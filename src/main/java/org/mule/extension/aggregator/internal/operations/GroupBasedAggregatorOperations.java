/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;

import org.mule.extension.aggregator.api.GroupBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.errors.GroupBasedAggregatorErrorProvider;
import org.mule.extension.aggregator.internal.routes.AggregationCompleteRoute;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;


/**
 * Operations defined for a Group Based aggregator.
 *
 * @since 1.0
 */
public class GroupBasedAggregatorOperations extends AbstractAggregatorOperations {

  /**
   * Aggregates a new event to the group and executes the routes and listeners if it corresponds.
   * </p>
   * If the event reaches the maxSize specified in {@code aggregatorParameters} for the group, 2 things will happen:
   * <ul>
   * <li>The elements in that group will be removed from the storage and the group will be marked as complete and every new element that arrives to that group will raise an exception</li>
   * <li>{@code onAggregationCompleteRoute} will be executed with the aggregated elements of that particular group</li>
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
   * If the group reaches a timeout, it will be marked as timedout and every new element that arrive for that group will raise an exception.
   * </p>
   * In the case of a timeout, the hooked listener will be executed only if it supports being called by timeout.
   * </p>
   * Once a particular group is marked as timedout or complete, a group eviction will be scheduled with a delay specified by the parameter.
   * On group eviction, the group will be reset and will be able to aggregate new elements.
   *
   * @param aggregatorParameters the parameters that configute the aggregator
   * @param incrementalAggregationRoute a route executed for every new event until the maxSize is reached
   * @param onAggregationCompleteRoute a route executed when maxSize is reached
   * @param completionCallback callback to be called when the router has finished
   * @throws ModuleException
   */
  @Alias("groupBasedAggregator")
  @Throws(GroupBasedAggregatorErrorProvider.class)
  public void aggregateByGroup(
          @ParameterGroup(name = "Aggregator config") GroupBasedAggregatorParameterGroup aggregatorParameters,
          @Alias("incrementalAggregation") @Optional IncrementalAggregationRoute incrementalAggregationRoute,
          @Alias("aggregationComplete") AggregationCompleteRoute onAggregationCompleteRoute,
          RouterCompletionCallback completionCallback)
      throws ModuleException {

    // implemented as privileged operation in GroupBasedAggregatorOperationsExecutor
  }

}
