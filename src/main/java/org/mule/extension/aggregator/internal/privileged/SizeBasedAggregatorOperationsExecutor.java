/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged;

import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.extension.aggregator.api.SizeBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.operations.SizeBasedAggregatorOperations;
import org.mule.extension.aggregator.internal.routes.AggregationCompleteRoute;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;

public class SizeBasedAggregatorOperationsExecutor extends SizeBasedAggregatorOperations
    implements ComponentExecutor<OperationModel> {

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    final ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;
    final CoreEvent event = context.getEvent();
    IncrementalAggregationRoute incrementalAggregationRoute = context.getParameter("incrementalAggregation");
    AggregationCompleteRoute aggregationCompleteRoute = context.getParameter("aggregationComplete");
    setParameters(context.getParameters());
    SizeBasedAggregatorParameterGroup parameters = createParameters(context.getParameters());
    aggregate(parameters, incrementalAggregationRoute, aggregationCompleteRoute,
              new AggregatorCompletionCallback(context.getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM), event));
    return null;
  }

  private SizeBasedAggregatorParameterGroup createParameters(Map<String, Object> parameterMap) {
    SizeBasedAggregatorParameterGroup parameters = new SizeBasedAggregatorParameterGroup();
    parameters.setContent(parameterMap.get("content"));
    parameters.setTimeout((Integer) parameterMap.get("timeout"));
    parameters.setTimeoutUnit((TimeUnit) parameterMap.get("timeoutUnit"));
    return parameters;
  }
}
