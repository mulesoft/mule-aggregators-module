/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;


import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.aggregator.api.SizeBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.errors.SizeBasedAggregatorErrorProvider;
import org.mule.extension.aggregator.internal.routes.AggregationCompleteRoute;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;


/**
 * Operations defined for a Size Based Aggregator.
 *
 * @since 1.0
 */
public class SizeBasedAggregatorOperations extends SingleGroupAggregatorOperations {

  /**
   * Maximum size for the aggregation to be before releasing it.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private int maxSize;

  /**
   * Aggregates a new event and executes the routes and listeners if it corresponds.
   * </p>
   * If the event reaches the maxSize specified in {@code aggregatorParameters}, 2 things will happen:
   * <ul>
   * <li>The elements in the storage will be removed and the next event will belong to the new aggregation</li>
   * <li>{@code onAggregationCompleteRoute} will be executed with the aggregated elements</li>
   * </ul>
   * Additionally, if there is a listener registered to this aggregator, it's callback will be executed with the same set
   * of elements
   * </p>
   * If the route {@code incrementalAggregationRoute} is not null, and the maxSize was not reached,
   * then it's chain will be executed with all aggregated events, including the last one.
   * </p>
   * The aggregator can also have a timeout defined. In that case, an scheduled task with that timeout as delay
   * will be registered for execution. The time will be computed from the time in which the first element arrives and
   * no extra tasks will be scheduled if there is another one waiting to be executed.
   * </p>
   * In the case of a timeout, the hooked listener will be executed only if it supports being called by timeout.
   *
   * @param aggregatorParameters the parameters that define the aggregator behaviour
   * @param incrementalAggregationRoute the route executed for every new event, if present
   * @param onAggregationCompleteRoute the route executed when the group is complete
   * @param completionCallback router callback
   */
  @Alias("sizeBasedAggregator")
  @Throws(SizeBasedAggregatorErrorProvider.class)
  public void aggregateBySize(
                              @ParameterGroup(
                                  name = "Aggregator config") SizeBasedAggregatorParameterGroup aggregatorParameters,
                              @Alias("incrementalAggregation") @Optional IncrementalAggregationRoute incrementalAggregationRoute,
                              @Alias("aggregationComplete") AggregationCompleteRoute onAggregationCompleteRoute,
                              RouterCompletionCallback completionCallback) {

    // implemented as privileged operation in SizeBasedAggregatorOperationsExecutor

  }

}
