/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.executor;


import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_KEY;
import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.AGGREGATOR_CONFIG;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;
import org.mule.extension.aggregator.internal.config.AggregatorManager;
import org.mule.extension.aggregator.internal.privileged.CompletionCallbackWrapper;
import org.mule.extension.aggregator.internal.routes.AggregationAttributes;
import org.mule.extension.aggregator.internal.source.AggregatorListener;
import org.mule.extension.aggregator.internal.storage.info.AggregatorSharedInformation;
import org.mule.extension.aggregator.internal.task.AsyncTask;
import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.meta.model.operation.OperationModel;
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
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Custom abstract executor for aggregator operations.
 * <p>
 * The reason why we have this custom executor is that unlike regular routers, we should be able to both, have the router
 * as void (the event out is the same as the event in) and propagate variables in case any is set inside a route.
 *
 * @since 1.0
 */
public abstract class AbstractAggregatorExecutor
    implements ComponentExecutor<OperationModel>, Initialisable, Startable, Disposable {

  final Logger LOGGER = LoggerFactory.getLogger(getClass());
  private static final String AGGREGATORS_MODULE_KEY = "AGGREGATORS";
  private static final String DEFAULT_TASK_SCHEDULING_PERIOD = "1000";
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

  @Inject
  private ConfigurationProperties configProperties;


  private String objectStore;
  private String name;
  private Scheduler scheduler;
  private PrimaryNodeLifecycleNotificationListener notificationListener;
  private AggregatorSharedInformation sharedInfoLocalCopy;
  private LazyValue<ObjectStore<AggregatorSharedInformation>> storage;
  private boolean started = false;
  private long taskSchedulingPeriod = parseLong(DEFAULT_TASK_SCHEDULING_PERIOD);

  protected void injectParameters(Map<String, Object> parameters) {
    this.objectStore = (String) parameters.get("objectStore");
    this.name = (String) parameters.get("name");
  }

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
    if (clusterService.isPrimaryPollingInstance()) {
      if (!started) {
        setRegisteredTasksAsNotScheduled();
        scheduler = schedulerService.cpuLightScheduler();
        try {
          taskSchedulingPeriod = parseLong(configProperties.resolveStringProperty(TASK_SCHEDULING_PERIOD_KEY)
              .orElse(configProperties.resolveStringProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY)
                  .orElse(DEFAULT_TASK_SCHEDULING_PERIOD)));
        } catch (NumberFormatException e) {
          LOGGER.warn(format("Error trying to configure %s, the value could not be parsed to a long. Using default value: %d %s",
                             TASK_SCHEDULING_PERIOD_KEY, taskSchedulingPeriod, TASK_SCHEDULING_PERIOD_UNIT));
        }
        scheduler.scheduleAtFixedRate(this::scheduleRegisteredTasks, 0, taskSchedulingPeriod, TASK_SCHEDULING_PERIOD_UNIT);
        started = true;
      }
    }
  }

  @Override
  public void dispose() {
    if (scheduler != null) {
      scheduler.stop();
    }
  }

  void executeRouteWithAggregatedElements(Route route, List<TypedValue> elements, AggregationAttributes attributes,
                                          CompletionCallbackWrapper callback) {
    route.getChain().process(elements, attributes, callback::success, (e, r) -> callback.error(e));
  }

  private void scheduleRegisteredTasks() {
    executeSynchronized(this::doScheduleRegisteredTasks);
  }

  abstract void doScheduleRegisteredTasks();

  private void setRegisteredTasksAsNotScheduled() {
    executeSynchronized(this::doSetRegisteredTasksAsNotScheduled);
  }

  abstract void doSetRegisteredTasksAsNotScheduled();

  /**
   * When scheduling the {@param runnable}, we should compute the actual delay value to set to the scheduler giving that it will be different from the one set by the user.
   * <p/>
   * Since tasks will be scheduled once the periodic process that handles that is executed {@link #scheduleRegisteredTasks()}, we should account for the time waited
   * until that process execution takes place.
   * Also, if in a cluster, the task could've been already scheduled in another primary node that was disconnected, we should consider that offset as well.
   * <p/>
   * Every delay will be counted from the time the first event arrives to the aggregator.
   * <p/>
   * The offset from the previous schedule would be offset = now - previousSchedulingTimestamp.
   * The actual delay according to the time the first event arrived will be delay = configuredDelay - (now - firstEventArrivalTime)
   * So, the total time to wait would be actualDelay = delay - offset.
   * <p/>
   * The computation could cause the delay to be zero or negative, that should mean: execute immediately {@link java.util.concurrent.ScheduledExecutorService}  }
   *
   * @param task     the task pojo with information about the task to schedule
   * @param runnable the runnable to execute
   */
  void scheduleTask(AsyncTask task, Runnable runnable) {
    long now = getCurrentTime();
    long delay = task.getDelayTimeUnit().toMillis(task.getDelay()) - (now - task.getRegisteringTimestamp());
    if (task.getSchedulingTimestamp().isPresent()) {
      delay = delay - (getCurrentTime() - task.getSchedulingTimestamp().getAsLong());
    }
    scheduler.schedule(runnable, delay, MILLISECONDS);
  }

  void evaluateConfiguredDelay(String valueKey, int configuredDelay, TimeUnit timeUnit) throws ModuleException {
    long configuredDelayInMillis = timeUnit.toMillis(configuredDelay);
    if (configuredDelayInMillis < taskSchedulingPeriod) {
      throw new ModuleException(format("The configured %s : %d %s, is too small for the configured scheduling time period: %d %s. %s should be equal or bigger than the scheduling time period in order to accurately schedule it.%s Use %s global-config or %s SystemProperty to change it",
                                       valueKey,
                                       configuredDelay,
                                       timeUnit,
                                       taskSchedulingPeriod,
                                       TASK_SCHEDULING_PERIOD_UNIT,
                                       valueKey,
                                       lineSeparator(),
                                       TASK_SCHEDULING_PERIOD_KEY,
                                       TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY),
                                AGGREGATOR_CONFIG);
    }
  }

  void notifyListenerOnComplete(List<TypedValue> elements, String id) {
    getListenerAndExecute(listener -> executeListener(listener, elements, id));
  }

  void notifyListenerOnTimeout(List<TypedValue> elements, String id) {
    getListenerAndExecute(listener -> {
      if (listener.shouldIncludeTimedOutGroups()) {
        executeListener(listener, elements, id);
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

  private void executeListener(AggregatorListener listener, List<TypedValue> elements, String id) {
    if (listener.isStarted()) {
      SourceCallback callback = listener.getCallback();
      SourceCallbackContext context = callback.createContext();
      context.setCorrelationId(id);
      callback.handle(Result.<List<TypedValue>, AggregationAttributes>builder()
          .output(elements).build(), context);
    }
  }
}
