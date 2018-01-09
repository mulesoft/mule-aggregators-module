/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;

import static java.lang.Thread.sleep;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.metadata.TypedValue.of;
import org.mule.extension.aggregator.api.TimeBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.errors.TimeBasedAggregatorErrorProvider;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.process.VoidCompletionCallback;


public class TimeBasedAggregatorOperations extends SingleGroupAggregatorOperations {

  private static final String UNLIMITED_TIMEOUT = "-1";
  private static final String AGGREGATOR_KEY = "TimeBasedAggregator";

  /**
   * Defines the a maximum size for the aggregation. If the size is reached before the period configured is completed
   * then, an aggregation will be triggered.
   * <p/>
   * A value of 0 should not be accepted.
   * <p/>
   * A value of -1 indicates unbounded (No aggregation triggered by size)
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = UNLIMITED_TIMEOUT)
  private int maxSize;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    setGroupSize(maxSize);
  }

  @Override
  String doGetAggregatorKey() {
    return AGGREGATOR_KEY;
  }

  /**
   * This aggregator aggregates elements until it reaches the specified time period.
   * </p>
   * The period to be taken into account will start being computed from the time the first event occurs. Once that aggregation
   * is released, the timer will not start until the next event.
   * </p>
   * The aggregator also allows an {@code incrementalAggregationRoute} route
   * that will be executed every time a new event arrives, unless maxSize is set.
   * If that is the case, the {@code inclrementalAggregation} route will be executed every time except when
   * the size of the aggregated elements is equal to maxSize. In that moment, a listener's callback(if any) is executed.
   *
   * @param aggregatorParameters Define the behaviour of the aggregator
   * @param incrementalAggregationRoute Route to be called with every new event
   * @param completionCallback Callback to notify route completion
   */
  @Alias("timeBasedAggregator")
  @Throws(TimeBasedAggregatorErrorProvider.class)
  public void aggregateByTime(
                              @ParameterGroup(
                                  name = "timeBasedAggregatorParameters") TimeBasedAggregatorParameterGroup aggregatorParameters,
                              @Alias("incrementalAggregation") @Optional IncrementalAggregationRoute incrementalAggregationRoute,
                              VoidCompletionCallback completionCallback) {

    evaluateConfiguredDelay("period", aggregatorParameters.getPeriod(), aggregatorParameters.getPeriodUnit());

    //We should synchronize the access to the storage to account for the situation when the period is completed while
    //executing a new event.
    executeSynchronized(() -> {

      registerTaskIfNeeded(aggregatorParameters.getPeriod(), aggregatorParameters.getPeriodUnit());

      AggregatedContent aggregatedContent = getAggregatedContent();

      aggregatedContent.add(of(aggregatorParameters.getContent()), getCurrentTime());

      if (aggregatedContent.isComplete()) {
        notifyListenerOnComplete(aggregatedContent.getAggregatedElements());
        resetGroup();
        completionCallback.success();
      } else if (incrementalAggregationRoute != null) {
        executeRouteWithAggregatedElements(incrementalAggregationRoute, aggregatedContent.getAggregatedElements(),
                                           getAttributes(aggregatedContent), completionCallback);
      } else {
        completionCallback.success();
      }
    });
  }

  @Override
  void onTaskExecution() {
    getElementsAndNotifyListener();
  }

  private void getElementsAndNotifyListener() {
    executeSynchronized(() -> {
      notifyListenerOnComplete(getAggregatedContent().getAggregatedElements());
      resetGroup();
    });
  }

}
