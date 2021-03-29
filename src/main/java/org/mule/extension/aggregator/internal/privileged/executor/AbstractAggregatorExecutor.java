/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.executor;


import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_KEY;
import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.AGGREGATOR_CONFIG;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.OBJECT_STORE_ACCESS;
import static org.mule.runtime.api.message.Message.builder;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_STORE_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;

import org.mule.extension.aggregator.api.AggregationAttributes;
import org.mule.extension.aggregator.internal.config.AggregatorManager;
import org.mule.extension.aggregator.internal.privileged.CompletionCallbackWrapper;
import org.mule.extension.aggregator.internal.source.AggregatorListener;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.extension.aggregator.internal.storage.info.AggregatorSharedInformation;
import org.mule.extension.aggregator.internal.task.AsyncTask;
import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.lifecycle.PrimaryNodeLifecycleNotificationListener;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
    implements ComponentExecutor<OperationModel>, Lifecycle {

  final Logger LOGGER = LoggerFactory.getLogger(getClass());
  private static final String AGGREGATORS_MODULE_KEY = "AGGREGATORS";

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
  private TransformationService transformationService;

  private ObjectStore<AggregatorSharedInformation> objectStore;
  private String name;
  private Scheduler scheduler;
  private PrimaryNodeLifecycleNotificationListener notificationListener;
  private AggregatorSharedInformation sharedInfoLocalCopy;
  private LazyValue<ObjectStore<AggregatorSharedInformation>> storage;

  //When clustered, only the primaryNode should have this flag in true so that when another node changes to primary
  //all the logic in the start() method is executed.
  private boolean started = false;

  private final Object stoppingLock = new Object();
  private boolean shouldSynchronizeToOS = true;

  protected void injectParameters(Map<String, Object> parameters) {
    this.objectStore = (ObjectStore<AggregatorSharedInformation>) parameters.get("objectStore");
    this.name = (String) parameters.get("name");
  }

  //TODO: This is a little bit of a hack since the SDK already supports injecting the event correlation info if declared
  //as an operation parameter.And we can extract the itemSequenceInfo from it.But since we are bypassing that behaviour in order to be able to propagate variables
  //we must get the item sequence info from the event ourselves.
  Optional<ItemSequenceInfo> getItemSequenceInfo(ExecutionContext context) {
    CoreEvent event = ((ExecutionContextAdapter) context).getEvent();
    return event.getItemSequenceInfo();
  }

  private Object consumingStream(Object element) {
    if (element instanceof InputStream) {
      return transformationService.transform(element, DataType.INPUT_STREAM, DataType.BYTE_ARRAY);
    } else if (element instanceof CursorStreamProvider) {
      return transformationService.transform(element, DataType.CURSOR_STREAM_PROVIDER, DataType.BYTE_ARRAY);
    } else {
      return element;
    }
  }

  private TypedValue consumingStreams(TypedValue element) {
    Object elementValue = element.getValue();
    MediaType elementMediaType = element.getDataType().getMediaType();
    Object consumedValue;
    if (elementValue instanceof Message) {
      consumedValue = builder((Message) elementValue)
          .payload(consumingStreams(((Message) elementValue).getPayload()))
          .attributes(consumingStreams(((Message) elementValue).getAttributes()))
          .build();
    } else {
      consumedValue = consumingStream(elementValue);
    }
    return new TypedValue(consumedValue, DataType.builder().fromObject(consumedValue).mediaType(elementMediaType).build());
  }

  void addToStorage(AggregatedContent aggregatedContent, TypedValue aggregatedElement,
                    Optional<ItemSequenceInfo> itemSequenceInfo) {
    TypedValue aggregatedElementTypedValue = consumingStreams(aggregatedElement);
    if (itemSequenceInfo.isPresent()) {
      aggregatedContent.add(aggregatedElementTypedValue, getCurrentTime(), itemSequenceInfo.get().getPosition());
    } else {
      aggregatedContent.add(aggregatedElementTypedValue, getCurrentTime());
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    //TODO: fix this MULE-9480
    initialiseIfNeeded(aggregatorManager);
    aggregatorManager.registerAggregator(name, this::scheduleRegisteredAsyncAggregations);
    storage = new LazyValue<>(this::getConfiguredObjectStore);
    notificationListener = new PrimaryNodeLifecycleNotificationListener(this, notificationListenerRegistry);
    notificationListener.register();
  }

  ObjectStore getConfiguredObjectStore() {
    if (objectStore == null) {
      return objectStoreManager.getDefaultPartition();
    } else {
      return objectStore;
    }
  }

  @Override
  public void start() throws MuleException {
    if (clusterService.isPrimaryPollingInstance()) {
      if (!started) {
        startIfNeeded(objectStore);
        setRegisteredAsyncAggregationsAsNotScheduled();
        if (getStorage().isPersistent()) {
          scheduler = schedulerService.ioScheduler(SchedulerConfig.config().withShutdownTimeout(0, MILLISECONDS));
        } else {
          scheduler = schedulerService.cpuLightScheduler(SchedulerConfig.config().withShutdownTimeout(0, MILLISECONDS));
        }
        started = true;
      }
    }
  }

  @Override
  public void stop() throws MuleException {
    synchronized (stoppingLock) {
      shouldSynchronizeToOS = false;
      started = false;
      if (scheduler != null) {
        //Tasks will not execute because of the stoppingLock that we acquired so there is no point in letting them finish.
        scheduler.stop();
        scheduler = null;
      }
    }
  }

  @Override
  public void dispose() {
    //EE-6218: need to check scheduler again because of a bug in cluster
    if (scheduler != null) {
      scheduler.stop();
    }
  }

  void executeRouteWithAggregatedElements(Route route, List<TypedValue> elements, AggregationAttributes attributes,
                                          CompletableFuture<Result<Object, Object>> future) {
    route.getChain().process(elements, attributes, future::complete, (e, r) -> future.completeExceptionally(e));
  }

  void finishExecution(CompletableFuture<Result<Object, Object>> future, CompletionCallbackWrapper completionCallback) {
    try {
      completionCallback.success(future.get());
    } catch (ExecutionException e) {
      completionCallback.error(e.getCause());
    } catch (InterruptedException e) {
      completionCallback.error(e);
    }
  }

  private void scheduleRegisteredAsyncAggregations() {
    executeSynchronized(this::doScheduleRegisteredAsyncAggregations);
  }

  abstract void doScheduleRegisteredAsyncAggregations();

  private void setRegisteredAsyncAggregationsAsNotScheduled() {
    executeSynchronized(this::doSetRegisteredAsyncAggregationsAsNotScheduled);
  }

  abstract void doSetRegisteredAsyncAggregationsAsNotScheduled();

  /**
   * When scheduling the {@param runnable}, we should compute the actual delay value to set to the scheduler giving that it will be different from the one set by the user.
   * <p/>
   * Since tasks will be scheduled once the periodic process that handles that is executed {@link #scheduleRegisteredAsyncAggregations()}, we should account for the time waited
   * until that process execution takes place.
   * <p/>
   * Every delay will be counted from the time the first event arrives to the aggregator.
   * <p/>
   * The actual delay according to the time the first event arrived will be delay = configuredDelay - (now - firstEventArrivalTime)
   * <p/>
   * The computation could cause the delay to be zero or negative, that should mean: execute immediately {@link java.util.concurrent.ScheduledExecutorService}  }
   *
   * @param task     the task pojo with information about the task to schedule
   * @param runnable the runnable to execute
   */
  void scheduleTask(AsyncTask task, Runnable runnable) {
    long now = getCurrentTime();
    long configuredDelay = task.getDelayTimeUnit().toMillis(task.getDelay());
    long delay = configuredDelay - (now - task.getRegisteringTimestamp());
    scheduler.schedule(runnable, delay, MILLISECONDS);
  }

  void evaluateConfiguredDelay(String valueKey, int configuredDelay, TimeUnit timeUnit) throws ModuleException {
    long configuredDelayInMillis = timeUnit.toMillis(configuredDelay);
    if (configuredDelayInMillis < aggregatorManager.getTaskSchedulingPeriodInMillis()) {
      throw new ModuleException(format("The configured %s : %d %s, is too small for the configured scheduling time period: %d MILLISECONDS. %s should be equal or bigger than the scheduling time period in order to accurately schedule it.%s Use %s global-config or %s SystemProperty to change it",
                                       valueKey,
                                       configuredDelay,
                                       timeUnit,
                                       aggregatorManager.getTaskSchedulingPeriodInMillis(),
                                       valueKey,
                                       lineSeparator(),
                                       TASK_SCHEDULING_PERIOD_KEY,
                                       TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY),
                                AGGREGATOR_CONFIG);
    }
  }

  void notifyListenerOnComplete(List<TypedValue> elements, AggregationAttributes aggregationAttributes) {
    getListenerAndExecute(listener -> executeListener(listener, elements, aggregationAttributes));
  }

  void notifyListenerOnTimeout(List<TypedValue> elements, AggregationAttributes aggregationAttributes) {
    getListenerAndExecute(listener -> {
      if (listener.shouldIncludeTimedOutGroups()) {
        executeListener(listener, elements, aggregationAttributes);
      }
    });
  }

  void executeSynchronized(Runnable task) {
    synchronized (stoppingLock) {
      if (shouldSynchronizeToOS) {
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
      throw new ModuleException("Found error when trying to access ObjectStore", OBJECT_STORE_ACCESS, e);
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
      throw new ModuleException("Found error when trying to access ObjectStore", OBJECT_STORE_ACCESS, e);
    }
  }

  private void getListenerAndExecute(Consumer<AggregatorListener> task) {
    aggregatorManager.getListener(this.name).ifPresent(task);
  }

  private void executeListener(AggregatorListener listener, List<TypedValue> elements,
                               AggregationAttributes aggregationAttributes) {
    if (listener.isStarted()) {
      SourceCallback callback = listener.getCallback();
      SourceCallbackContext context = callback.createContext();
      context.setCorrelationId(aggregationAttributes.getAggregationId());
      callback.handle(Result.<List<TypedValue>, AggregationAttributes>builder()
          .output(elements).attributes(aggregationAttributes).build(), context);
    }
  }

}
