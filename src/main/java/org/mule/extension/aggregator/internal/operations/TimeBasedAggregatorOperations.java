/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.aggregator.internal.errors.AggregatorErrorProvider;
import org.mule.extension.aggregator.internal.parameter.TimeBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;

public class TimeBasedAggregatorOperations extends SingleGroupAggregatorOperations {

  private static final String UNLIMITED_SIZE = "-1";

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
  @Optional(defaultValue = UNLIMITED_SIZE)
  private int maxSize;

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
  @Throws(AggregatorErrorProvider.class)
  public void aggregateByTime(
                              @ParameterGroup(
                                  name = "Aggregator config") TimeBasedAggregatorParameterGroup aggregatorParameters,
                              @Alias("incrementalAggregation") @Optional IncrementalAggregationRoute incrementalAggregationRoute,
                              RouterCompletionCallback completionCallback) {

    // implemented as privileged operation in TimeBasedAggregatorOperationsExecutor
  }

}
