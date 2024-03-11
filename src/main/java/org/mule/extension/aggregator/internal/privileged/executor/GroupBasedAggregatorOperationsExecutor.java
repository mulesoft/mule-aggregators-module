/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.executor;

import static java.lang.String.format;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.AGGREGATOR_CONFIG;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.GROUP_COMPLETED;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.GROUP_TIMED_OUT;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.NO_GROUP_ID;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.NO_GROUP_SIZE;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.extension.aggregator.internal.parameter.GroupBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.privileged.CompletionCallbackWrapper;
import org.mule.extension.aggregator.internal.routes.AggregationCompleteRoute;
import org.mule.extension.aggregator.api.AggregationAttributes;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.extension.aggregator.internal.storage.content.AbstractAggregatedContent;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.extension.aggregator.internal.storage.content.SimpleAggregatedContent;
import org.mule.extension.aggregator.internal.storage.info.AggregatorSharedInformation;
import org.mule.extension.aggregator.internal.storage.info.GroupAggregatorSharedInformation;
import org.mule.extension.aggregator.internal.task.AsyncTask;
import org.mule.extension.aggregator.internal.task.SimpleAsyncTask;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;

/**
 * Custom executor for {@code groupBasedAggregator}.
 * <p/>
 * Class keeping all the logic for the aggregator defined in {@link org.mule.extension.aggregator.internal.operations.GroupBasedAggregatorOperations}
 * <p>
 * The reason why we have this custom executor is that unlike regular routers, we should be able to both, have the it
 * as void (the event out is the same as the event in) and propagate variables in case any is set inside a route.
 *
 * @since 1.0
 */
public class GroupBasedAggregatorOperationsExecutor extends AbstractAggregatorExecutor {

  private static final String AGGREGATOR_KEY = "GroupBasedAggregator";

  //Need to store this because we need to access the values onTimeout. Keep in mind that this can only be done
  //because expressions are not supported for this parameters. Otherwise, another solution should be used.
  private int lastConfiguredEvictionTime;
  private TimeUnit lastConfiguredEvictionTimeUnit;

  public GroupBasedAggregatorOperationsExecutor(Map<String, Object> params) {
    injectParameters(params);
  }

  public int getLastConfiguredEvictionTime() {
    return lastConfiguredEvictionTime;
  }

  public void setLastConfiguredEvictionTime(int lastConfiguredEvictionTime) {
    this.lastConfiguredEvictionTime = lastConfiguredEvictionTime;
  }

  public TimeUnit getLastConfiguredEvictionTimeUnit() {
    return lastConfiguredEvictionTimeUnit;
  }

  public void setLastConfiguredEvictionTimeUnit(TimeUnit lastConfiguredEvictionTimeUnit) {
    this.lastConfiguredEvictionTimeUnit = lastConfiguredEvictionTimeUnit;
  }

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    final ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;
    final CoreEvent event = context.getEvent();
    IncrementalAggregationRoute incrementalAggregationRoute = context.getParameter("incrementalAggregation");
    AggregationCompleteRoute aggregationCompleteRoute = context.getParameter("aggregationComplete");
    GroupBasedAggregatorParameterGroup parameters = createParameters(context.getParameters());
    Optional<ItemSequenceInfo> itemSequenceInfo = getItemSequenceInfo(executionContext);
    aggregate(parameters, incrementalAggregationRoute, aggregationCompleteRoute,
              new CompletionCallbackWrapper(context.getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM), event), itemSequenceInfo);
    return null;
  }

  private GroupBasedAggregatorParameterGroup createParameters(Map<String, Object> parameterMap) {
    GroupBasedAggregatorParameterGroup parameters = new GroupBasedAggregatorParameterGroup();
    parameters.setEvictionTime((Integer) parameterMap.get("evictionTime"));
    parameters.setEvictionTimeUnit((TimeUnit) parameterMap.get("evictionTimeUnit"));
    parameters.setGroupId((String) parameterMap.get("groupId"));
    parameters.setContent((TypedValue) parameterMap.get("content"));
    parameters.setGroupSize((Integer) parameterMap.get("groupSize"));
    parameters.setTimeout((Integer) parameterMap.get("timeout"));
    parameters.setTimeoutUnit((TimeUnit) parameterMap.get("timeoutUnit"));
    return parameters;
  }

  @Override
  String doGetAggregatorKey() {
    return AGGREGATOR_KEY;
  }

  private void aggregate(GroupBasedAggregatorParameterGroup aggregatorParameters,
                         IncrementalAggregationRoute incrementalAggregationRoute,
                         AggregationCompleteRoute onAggregationCompleteRoute,
                         CompletionCallbackWrapper completionCallback,
                         Optional<ItemSequenceInfo> itemSequenceInfo) {

    evaluateParameters(aggregatorParameters);

    lastConfiguredEvictionTime = aggregatorParameters.getEvictionTime();
    lastConfiguredEvictionTimeUnit = aggregatorParameters.getEvictionTimeUnit();

    CompletableFuture<Result<Object, Object>> future = new CompletableFuture<>();

    executeSynchronized(() -> {

      if (aggregatorParameters.isTimeoutSet()) {
        registerTimeoutIfNeeded(aggregatorParameters.getGroupId(), aggregatorParameters.getTimeout(),
                                aggregatorParameters.getTimeoutUnit());
      }

      AggregatedContent groupAggregatedContent =
          getOrCreateAggregatedContent(aggregatorParameters.getGroupId(), aggregatorParameters.getGroupSize());

      if (groupAggregatedContent.isComplete()) {
        throw new ModuleException(format("Trying to aggregate a new element to the group with id: %s ,but it's already complete",
                                         aggregatorParameters.getGroupId()),
                                  GROUP_COMPLETED);
      } else if (((SimpleAggregatedContent) groupAggregatedContent).isTimedOut()) {
        throw new ModuleException(format("Trying to aggregate a new element to the group with id: %s ,but it has already timed out",
                                         aggregatorParameters.getGroupId()),
                                  GROUP_TIMED_OUT);
      }

      addToStorage(groupAggregatedContent, aggregatorParameters.getContent(), itemSequenceInfo);

      if (groupAggregatedContent.isComplete()) {
        List<TypedValue> aggregatedElements = groupAggregatedContent.getAggregatedElements();
        notifyListenerOnComplete(aggregatedElements, getAttributes(aggregatorParameters.getGroupId(), groupAggregatedContent));
        handleGroupEviction(aggregatorParameters.getGroupId(), aggregatorParameters.getEvictionTime(),
                            aggregatorParameters.getEvictionTimeUnit());
        executeRouteWithAggregatedElements(onAggregationCompleteRoute, aggregatedElements,
                                           getAttributes(aggregatorParameters.getGroupId(), groupAggregatedContent),
                                           future);
        getSharedInfoLocalCopy().getRegisteredTimeoutAsyncAggregations().remove(aggregatorParameters.getGroupId());
      } else if (incrementalAggregationRoute != null) {
        executeRouteWithAggregatedElements(incrementalAggregationRoute, groupAggregatedContent.getAggregatedElements(),
                                           getAttributes(aggregatorParameters.getGroupId(), groupAggregatedContent),
                                           future);
      } else {
        future.complete(Result.builder().build());
      }
      return true;
    });

    finishExecution(future, completionCallback);
  }

  private void evaluateParameters(GroupBasedAggregatorParameterGroup parameterGroup) throws ModuleException {

    if (parameterGroup.getGroupId() == null) {
      throw new ModuleException("groupId expression resolves to null", NO_GROUP_ID);
    }
    if (parameterGroup.getGroupSize() == null) {
      throw new ModuleException("groupSize expression resolves to null", NO_GROUP_SIZE);
    } else {
      if (parameterGroup.getGroupSize() <= 0) {
        throw new ModuleException(format("groupSize should be bigger than 0, got: %d", parameterGroup.getGroupSize()),
                                  AGGREGATOR_CONFIG);
      }
    }

    //Any negative value should be allowed because it means that the group should never be evicted.
    //If the value is 0, it means evict immediately.
    if (parameterGroup.getEvictionTime() > 0) {
      evaluateConfiguredDelay("evictionTime", parameterGroup.getEvictionTime(), parameterGroup.getEvictionTimeUnit());
    }

    if (parameterGroup.isTimeoutSet()) {
      if (parameterGroup.getTimeout() <= 0) {
        throw new ModuleException(format("A configured timeout of %d is not valid. Value should be bigger than 0",
                                         parameterGroup.getTimeout()),
                                  AGGREGATOR_CONFIG);
      }
      evaluateConfiguredDelay("timeout", parameterGroup.getTimeout(), parameterGroup.getTimeoutUnit());
    }
  }

  private void handleGroupEviction(String groupId, int evictionTime, TimeUnit evictionUnit) {
    if (evictionTime == 0) { //Evict immediately
      evictGroup(groupId);
    } else if (evictionTime > 0) {
      registerGroupEvictionIfNeeded(groupId, evictionTime, evictionUnit);
    }
    //If eviction time is less than 0, then remember group forever.
  }

  private void evictGroup(String groupId) {
    getSharedInfoLocalCopy().removeAggregatedContent(groupId);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format("Group with id: %s evicted", groupId));
    }
  }

  private void onGroupEviction(String groupId) {
    evictGroup(groupId);
  }

  private void onTimeout(String groupId) {
    AggregatedContent groupStorage = getSharedInfoLocalCopy().getAggregatedContent(groupId);
    if (groupStorage != null) {
      List<TypedValue> elements = groupStorage.getAggregatedElements();
      ((SimpleAggregatedContent) groupStorage).setTimedOut();
      notifyListenerOnTimeout(elements, getAttributes(groupId, groupStorage));
      handleGroupEviction(groupId, lastConfiguredEvictionTime, lastConfiguredEvictionTimeUnit);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Group with id: %s timed out", groupId));
      }
    }
  }


  private AggregatedContent getOrCreateAggregatedContent(String groupId, int groupSize) {
    AggregatedContent aggregatedContent = getSharedInfoLocalCopy().getAggregatedContent(groupId);
    if (aggregatedContent == null) {
      aggregatedContent = new SimpleAggregatedContent(groupSize);
      getSharedInfoLocalCopy().setAggregatedContent(groupId, aggregatedContent);
    }
    if (((AbstractAggregatedContent) aggregatedContent).getMaxSize() != groupSize) {
      LOGGER.warn(format("Group size for groupId: %s is different from the first configured one. Was: %d, is: %d, using: %d",
                         groupId, ((AbstractAggregatedContent) aggregatedContent).getMaxSize(), groupSize,
                         ((AbstractAggregatedContent) aggregatedContent).getMaxSize()));
    }
    return aggregatedContent;
  }

  private AggregationAttributes getAttributes(String groupId, AggregatedContent aggregatedContent) {
    return new AggregationAttributes(groupId,
                                     aggregatedContent.getFirstValueArrivalTime(),
                                     aggregatedContent.getLastValueArrivalTime(),
                                     aggregatedContent.isComplete());

  }

  @Override
  GroupAggregatorSharedInformation getSharedInfoLocalCopy() throws ModuleException {
    return (GroupAggregatorSharedInformation) super.getSharedInfoLocalCopy();
  }

  @Override
  AggregatorSharedInformation createSharedInfo() {
    return new GroupAggregatorSharedInformation();
  }


  private void registerTimeoutIfNeeded(String groupId, int delay, TimeUnit unit) {
    if (getSharedInfoLocalCopy().shouldRegisterTimeout(groupId)) {
      AsyncTask task = new SimpleAsyncTask(delay, unit);
      task.setRegistered(getCurrentTime());
      getSharedInfoLocalCopy().registerTimeoutAsyncAggregation(groupId, task);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Registered timeout to be executed for groupId: %s in %d %s", groupId, delay, unit));
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Attempted to register timeout task for groupId: %s but it was already registered", groupId));
      }
    }
  }

  private void registerGroupEvictionIfNeeded(String groupId, int delay, TimeUnit unit) {
    if (getSharedInfoLocalCopy().shouldRegisterEviction(groupId)) {
      AsyncTask task = new SimpleAsyncTask(delay, unit);
      task.setRegistered(getCurrentTime());
      getSharedInfoLocalCopy().registerGroupEvictionTask(groupId, task);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Registered group eviction to be executed for groupId: %s in %d %s", groupId, delay, unit));
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Attempted to register group eviction for groupId: %s but it was already registered", groupId));
      }
    }
  }

  @Override
  boolean doScheduleRegisteredAsyncAggregations() {
    getSharedInfoLocalCopy().getRegisteredGroupEvictionTasks().forEach(this::scheduleGroupEvictionIfNeeded);
    getSharedInfoLocalCopy().getRegisteredTimeoutAsyncAggregations().forEach(this::scheduleTimeoutIfNeeded);
    return true;
  }

  @Override
  boolean doSetRegisteredAsyncAggregationsAsNotScheduled() {
    getSharedInfoLocalCopy().getRegisteredGroupEvictionTasks().forEach((key, value) -> value.setUnscheduled());
    getSharedInfoLocalCopy().getRegisteredTimeoutAsyncAggregations().forEach((key, value) -> value.setUnscheduled());
    return true;
  }

  private void scheduleGroupEvictionIfNeeded(String groupId, AsyncTask task) {
    if (!task.isScheduled()) {
      scheduleTask(task, () -> executeSynchronized(() -> {
        onGroupEviction(groupId);
        getSharedInfoLocalCopy().unregisterGroupEvictionTask(groupId);
        return true;
      }));
      task.setScheduled();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Scheduled group eviction for groupId: %s to be executed in %d %s", groupId, task.getDelay(),
                            task.getDelayTimeUnit()));
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Attempted to schedule a group eviction for groupId: %s, but it is already scheduled", groupId));
      }
    }
  }


  private void scheduleTimeoutIfNeeded(String groupId, AsyncTask task) {
    if (!task.isScheduled()) {
      scheduleTask(task, () -> executeSynchronized(() -> {
        if (getSharedInfoLocalCopy().getRegisteredTimeoutAsyncAggregations().get(groupId) != null
            //If the registered task changes prior to this execution, it means that the aggregation was completed by maxSize being reached, should not execute
            && task.getId()
                .equals(getSharedInfoLocalCopy().getRegisteredTimeoutAsyncAggregations().get(groupId).getId())) {
          onTimeout(groupId);
          getSharedInfoLocalCopy().unregisterTimeoutAsyncAggregation(groupId);
        }
        return true;
      }));
      task.setScheduled();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Scheduled timeout for groupId: %s to be executed in %d %s", groupId, task.getDelay(),
                            task.getDelayTimeUnit()));
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Attempted to schedule timeout for groupId: %s, but it is already scheduled", groupId));
      }
    }
  }

}
