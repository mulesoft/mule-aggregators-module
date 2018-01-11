/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;

import static java.lang.String.format;
import static java.lang.Thread.sleep;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.GROUP_COMPLETED;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.GROUP_TIMED_OUT;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.NO_GROUP_ID;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.NO_GROUP_SIZE;
import static org.mule.runtime.api.metadata.TypedValue.of;
import org.mule.extension.aggregator.api.GroupBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.errors.GroupBasedAggregatorErrorProvider;
import org.mule.extension.aggregator.internal.routes.AggregationCompleteRoute;
import org.mule.extension.aggregator.internal.routes.AggregatorAttributes;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.extension.aggregator.internal.storage.content.SimpleAggregatedContent;
import org.mule.extension.aggregator.internal.storage.info.AggregatorSharedInformation;
import org.mule.extension.aggregator.internal.storage.info.GroupAggregatorSharedInformation;
import org.mule.extension.aggregator.internal.task.AsyncTask;
import org.mule.extension.aggregator.internal.task.SimpleAsyncTask;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;

import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Operations defined for a Group Based aggregator.
 *
 * @since 1.0
 */
public class GroupBasedAggregatorOperations extends AbstractAggregatorOperations {

  private static final String AGGREGATOR_KEY = "GroupBasedAggregator";

  @Override
  String doGetAggregatorKey() {
    return AGGREGATOR_KEY;
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
  }

  @Alias("groupBasedAggregator")
  @Throws(GroupBasedAggregatorErrorProvider.class)
  public void aggregateByGroup(
                               @ParameterGroup(
                                   name = "groupBasedAggregatorParameterGroup") GroupBasedAggregatorParameterGroup aggregatorParameters,
                               @Alias("incrementalAggregation") @Optional IncrementalAggregationRoute incrementalAggregationRoute,
                               @Alias("aggregationComplete") AggregationCompleteRoute onAggregationCompleteRoute,
                               RouterCompletionCallback completionCallback)
      throws ModuleException {

    //evaluateParameters(aggregatorParameters);
    //
    //executeSynchronized(() -> {
    //
    //  if (aggregatorParameters.isTimeoutSet()) {
    //    registerTimeoutIfNeeded(aggregatorParameters.getGroupId(), aggregatorParameters.getTimeout(),
    //                            aggregatorParameters.getTimeoutUnit());
    //  }
    //
    //  AggregatedContent groupAggregatedContent =
    //      getOrCreateAggregatedContent(aggregatorParameters.getGroupId(), aggregatorParameters.getGroupSize());
    //
    //  if (groupAggregatedContent.isComplete()) {
    //    throw new ModuleException(format("Trying to aggregate a new element to the group with id: %s ,but it's already complete",
    //                                     aggregatorParameters.getGroupId()),
    //                              GROUP_COMPLETED);
    //  } else if (((SimpleAggregatedContent) groupAggregatedContent).isTimedOut()) {
    //    throw new ModuleException(format("Trying to aggregate a new element to the group with id: %s ,but it has already timed out",
    //                                     aggregatorParameters.getGroupId()),
    //                              GROUP_TIMED_OUT);
    //  }
    //
    //  groupAggregatedContent.add(of(aggregatorParameters.getContent()), getCurrentTime());
    //
    //  if (groupAggregatedContent.isComplete()) {
    //    List<TypedValue> aggregatedElements = groupAggregatedContent.getAggregatedElements();
    //    notifyListenerOnComplete(aggregatedElements);
    //    registerGroupEvictionIfNeeded(aggregatorParameters.getGroupId(), aggregatorParameters.getEvictionTime(),
    //                                  aggregatorParameters.getEvictionTimeUnit());
    //    executeRouteWithAggregatedElements(onAggregationCompleteRoute, aggregatedElements,
    //                                       getAttributes(aggregatorParameters.getGroupId(), groupAggregatedContent),
    //                                       completionCallback);
    //  } else if (incrementalAggregationRoute != null) {
    //    executeRouteWithAggregatedElements(incrementalAggregationRoute, groupAggregatedContent.getAggregatedElements(),
    //                                       getAttributes(aggregatorParameters.getGroupId(), groupAggregatedContent),
    //                                       completionCallback);
    //  } else {
    //    completionCallback.success();
    //  }
    //});
  }

  protected void aggregate(GroupBasedAggregatorParameterGroup aggregatorParameters,
                           IncrementalAggregationRoute incrementalAggregationRoute,
                           AggregationCompleteRoute onAggregationCompleteRoute,
                           RouterCompletionCallback completionCallback) {

    evaluateParameters(aggregatorParameters);

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

      groupAggregatedContent.add(of(aggregatorParameters.getContent()), getCurrentTime());

      if (groupAggregatedContent.isComplete()) {
        List<TypedValue> aggregatedElements = groupAggregatedContent.getAggregatedElements();
        notifyListenerOnComplete(aggregatedElements);
        registerGroupEvictionIfNeeded(aggregatorParameters.getGroupId(), aggregatorParameters.getEvictionTime(),
                                      aggregatorParameters.getEvictionTimeUnit());
        executeRouteWithAggregatedElements(onAggregationCompleteRoute, aggregatedElements,
                                           getAttributes(aggregatorParameters.getGroupId(), groupAggregatedContent),
                                           completionCallback);
      } else if (incrementalAggregationRoute != null) {
        executeRouteWithAggregatedElements(incrementalAggregationRoute, groupAggregatedContent.getAggregatedElements(),
                                           getAttributes(aggregatorParameters.getGroupId(), groupAggregatedContent),
                                           completionCallback);
      } else {
        completionCallback.success(Result.builder().build());
      }
    });
  }

  private void evaluateParameters(GroupBasedAggregatorParameterGroup parameterGroup) throws ModuleException {
    if (parameterGroup.getGroupId() == null) {
      throw new ModuleException("groupId expression resolves to null", NO_GROUP_ID);
    }
    if (parameterGroup.getGroupSize() == null) {
      throw new ModuleException("groupSize expression resolves to null", NO_GROUP_SIZE);
    }
    evaluateConfiguredDelay("evictionTime", parameterGroup.getEvictionTime(), parameterGroup.getEvictionTimeUnit());
    if (parameterGroup.isTimeoutSet()) {
      evaluateConfiguredDelay("timeout", parameterGroup.getTimeout(), parameterGroup.getTimeoutUnit());
    }
  }

  private void onGroupEviction(String groupId) {
    executeSynchronized(() -> getSharedInfoLocalCopy().removeAggregatedContent(groupId));
  }

  private void onTimeout(String groupId) {
    executeSynchronized(() -> {
      AggregatedContent groupStorage = getSharedInfoLocalCopy().getAggregatedContent(groupId);
      if (groupStorage != null) {
        List<TypedValue> elements = groupStorage.getAggregatedElements();
        ((SimpleAggregatedContent) groupStorage).setTimedOut();
        notifyListenerOnTimeout(elements);
      }
    });
  }


  private AggregatedContent getOrCreateAggregatedContent(String groupId, int groupSize) {
    AggregatedContent aggregatedContent = getSharedInfoLocalCopy().getAggregatedContent(groupId);
    if (aggregatedContent == null) {
      aggregatedContent = new SimpleAggregatedContent(groupSize);
      getSharedInfoLocalCopy().setAggregatedContent(groupId, aggregatedContent);
    }
    //TODO:LOG EXCEPTION IF GROUP SIZE DOES NOT MATCH
    return aggregatedContent;
  }

  private AggregatorAttributes getAttributes(String groupId, AggregatedContent aggregatedContent) {
    return new AggregatorAttributes(groupId,
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
      getSharedInfoLocalCopy().registerTimeoutTask(groupId, task);
    }
  }

  private void registerGroupEvictionIfNeeded(String groupId, int delay, TimeUnit unit) {
    if (getSharedInfoLocalCopy().shouldRegisterEviction(groupId)) {
      AsyncTask task = new SimpleAsyncTask(delay, unit);
      task.setRegistered(getCurrentTime());
      getSharedInfoLocalCopy().registerGroupEvictionTask(groupId, task);
    }
  }

  @Override
  void doScheduleRegisteredTasks() {
    getSharedInfoLocalCopy().getRegisteredGroupEvictionTasks().forEach(this::scheduleGroupEvictionIfNeeded);
    getSharedInfoLocalCopy().getRegisteredTimeoutTasks().forEach(this::scheduleTimeoutIfNeeded);
  }

  @Override
  void doSetRegisteredTasksAsNotScheduled() {
    getSharedInfoLocalCopy().getRegisteredGroupEvictionTasks().forEach((key, value) -> value.setUnscheduled());
    getSharedInfoLocalCopy().getRegisteredTimeoutTasks().forEach((key, value) -> value.setUnscheduled());
  }

  private void scheduleGroupEvictionIfNeeded(String groupId, AsyncTask task) {
    if (!task.isScheduled()) {
      scheduleTask(task, () -> {
        onGroupEviction(groupId);
        getSharedInfoLocalCopy().unregisterGroupEvictionTask(groupId);
      });
      task.setScheduled(getCurrentTime());
    }
  }


  private void scheduleTimeoutIfNeeded(String groupId, AsyncTask task) {
    if (!task.isScheduled()) {
      scheduleTask(task, () -> {
        onTimeout(groupId);
        getSharedInfoLocalCopy().unregisterTimeoutTask(groupId);
      });
      task.setScheduled(getCurrentTime());
    }
  }


}
