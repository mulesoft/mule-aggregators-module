/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;


import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;
import org.mule.extension.aggregator.internal.routes.AggregatorAttributes;
import org.mule.extension.aggregator.internal.config.AggregatorManager;
import org.mule.extension.aggregator.internal.source.AggregatorListener;
import org.mule.extension.aggregator.internal.storage.info.AggregatorSharedInformation;
import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.lifecycle.PrimaryNodeLifecycleNotificationListener;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.reference.ObjectStoreReference;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Route;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

public abstract class AbstractAggregatorOperations implements Initialisable, Startable, Disposable {

  public static final String TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY = SYSTEM_PROPERTY_PREFIX + "schedulingDelay";

  private static final String AGGREGATORS_MODULE_KEY = "AGGREGATORS";
  private static final int TASK_SCHEDULING_PERIOD = 1000;
  private static final TimeUnit TASK_SCHEDULING_PERIOD_UNIT = MILLISECONDS;

  @Inject
  @Named(OBJECT_STORE_MANAGER)
  private ObjectStoreManager objectStoreManager;

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private AggregatorManager aggregatorManager;

  @Inject
  private LockFactory lockFactory;

  @Inject
  private TimeSupplier timeSupplier;

  @Inject
  private NotificationListenerRegistry notificationListenerRegistry;

  @Inject
  private ClusterService clusterService;

  /**
   * An ObjectStore for storing shared information regarding aggregators. (Groups, GroupId, Etc)
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional
  @ObjectStoreReference
  private String objectStore;


  /**
   * A name for the aggregator to be referenced later.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private String name;

  private Scheduler scheduler;
  private PrimaryNodeLifecycleNotificationListener notificationListener;
  private AggregatorSharedInformation sharedInfoLocalCopy;
  private LazyValue<ObjectStore<AggregatorSharedInformation>> storage;
  private boolean started = false;



  @Override
  public void initialise() throws InitialisationException {
    //TODO: fix this MULE-9480
    initialiseIfNeeded(aggregatorManager);
    aggregatorManager.registerAggregator(name);
    storage = new LazyValue<ObjectStore<AggregatorSharedInformation>>(this::getConfiguredObjectStore);
    notificationListener = new PrimaryNodeLifecycleNotificationListener(this, notificationListenerRegistry);
    notificationListener.register();
  }

  ObjectStore getConfiguredObjectStore() {
    if (objectStore == null) {
      return objectStoreManager.getDefaultPartition();
    } else {
      return objectStoreManager.getObjectStore(objectStore);
    }
  }

  @Override
  public void start() throws MuleException {
    if (!started) {
      if (clusterService.isPrimaryPollingInstance()) {
        scheduler = schedulerService.cpuLightScheduler();
        String configuredPeriodString = getProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY);
        int configuredPeriod = configuredPeriodString == null ? TASK_SCHEDULING_PERIOD : parseInt(configuredPeriodString);
        scheduler.scheduleAtFixedRate(this::scheduleRegisteredTasks, 0, configuredPeriod, TASK_SCHEDULING_PERIOD_UNIT);
      }
      started = true;
    }
  }

  @Override
  public void dispose() {
    if (scheduler != null) {
      scheduler.stop();
    }
  }

  void executeRouteWithAggregatedElements(Route route, List<TypedValue> elements, AggregatorAttributes attributes,
                                          VoidCompletionCallback callback) {
    route.getChain().process(elements, attributes, r -> callback.success(), (e, r) -> callback.error(e));
  }

  abstract void scheduleRegisteredTasks();

  void scheduleTask(int delay, TimeUnit unit, Runnable task) {
    scheduler.schedule(task, delay, unit);
  }

  void notifyListenerOnComplete(List<TypedValue> elements) {
    getListenerAndExecute(listener -> executeListener(listener, elements));
  }

  void notifyListenerOnTimeout(List<TypedValue> elements) {
    getListenerAndExecute(listener -> {
      if (listener.shouldIncludeTimedOutGroups()) {
        executeListener(listener, elements);
      }
    });
  }

  synchronized void executeSynchronized(Runnable task) {
    Lock lock = lockFactory.createLock(getAggregatorKey());
    lock.lock();
    try {
      pullSharedInfo();
      task.run();
      pushSharedInfo();
    } finally {
      lock.unlock();
    }
  }

  private String getAggregatorKey() {
    return format("%s:%s:%s", AGGREGATORS_MODULE_KEY, doGetAggregatorKey(), name);
  }

  abstract String doGetAggregatorKey();

  abstract AggregatorSharedInformation createSharedInfo();

  Long getCurrentTime() {
    return timeSupplier.get();
  }

  AggregatorSharedInformation getSharedInfoLocalCopy() {
    return sharedInfoLocalCopy;
  }

  private ObjectStore<AggregatorSharedInformation> getStorage() {
    return storage.get();
  }

  private void pullSharedInfo() throws ModuleException {
    String aggregatorKey = getAggregatorKey();
    try {
      if (getStorage().contains(aggregatorKey)) {
        sharedInfoLocalCopy = getStorage().retrieve(getAggregatorKey());
      } else {
        sharedInfoLocalCopy = createSharedInfo();
      }
    } catch (ObjectStoreException e) {
      throw new ModuleException("Found error when trying to access ObjectStore", ANY, e);
    }
  }

  private void pushSharedInfo() throws ModuleException {
    String aggregatorKey = getAggregatorKey();
    try {
      if (getStorage().contains(aggregatorKey)) {
        getStorage().remove(aggregatorKey);
      }
      getStorage().store(aggregatorKey, sharedInfoLocalCopy);
    } catch (ObjectStoreException e) {
      throw new ModuleException("Found error when trying to access ObjectStore", ANY, e);
    }
  }

  private void getListenerAndExecute(Consumer<AggregatorListener> task) {
    aggregatorManager.getListener(this.name).ifPresent(task);
  }

  private void executeListener(AggregatorListener listener, List<TypedValue> elements) {
    if (listener.isStarted()) {
      listener.getCallback().handle(Result.<List<TypedValue>, AggregatorAttributes>builder()
          .output(elements).build());
    }
  }

}
