/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.executor;

import static org.mule.runtime.api.metadata.TypedValue.of;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.extension.aggregator.api.SizeBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.privileged.CompletionCallbackWrapper;
import org.mule.extension.aggregator.internal.routes.AggregationCompleteRoute;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Publisher;

/**
 * Custom executor for {@code sizeBasedAggregator}.
 * <p/>
 * Class keeping all the logic for the aggregator defined in {@link org.mule.extension.aggregator.internal.operations.SizeBasedAggregatorOperations}
 * <p>
 * The reason why we have this custom executor is that unlike regular routers, we should be able to both, have the it
 * as void (the event out is the same as the event in) and propagate variables in case any is set inside a route.
 *
 * @since 1.0
 */
public class SizeBasedAggregatorOperationsExecutor extends SingleGroupAggregatorExecutor {

  private static final String AGGREGATOR_KEY = "SizeBasedAggregator";

  private int maxSize;

  public SizeBasedAggregatorOperationsExecutor(Map<String, Object> params) {
    injectParameters(params);
  }

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    final ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;
    final CoreEvent event = context.getEvent();
    IncrementalAggregationRoute incrementalAggregationRoute = context.getParameter("incrementalAggregation");
    AggregationCompleteRoute aggregationCompleteRoute = context.getParameter("aggregationComplete");
    SizeBasedAggregatorParameterGroup parameters = createParameters(context.getParameters());
    aggregate(parameters, incrementalAggregationRoute, aggregationCompleteRoute,
              new CompletionCallbackWrapper(context.getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM), event));
    return null;
  }

  private SizeBasedAggregatorParameterGroup createParameters(Map<String, Object> parameterMap) {
    SizeBasedAggregatorParameterGroup parameters = new SizeBasedAggregatorParameterGroup();
    parameters.setContent(parameterMap.get("content"));
    parameters.setTimeout((Integer) parameterMap.get("timeout"));
    parameters.setTimeoutUnit((TimeUnit) parameterMap.get("timeoutUnit"));
    return parameters;
  }

  @Override
  protected void injectParameters(Map<String, Object> parameters) {
    super.injectParameters(parameters);
    maxSize = (Integer) parameters.get("maxSize");
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    setGroupSize(maxSize);
  }

  @Override
  String doGetAggregatorKey() {
    return AGGREGATOR_KEY;
  }

  private void aggregate(SizeBasedAggregatorParameterGroup aggregatorParameters,
                         IncrementalAggregationRoute incrementalAggregationRoute,
                         AggregationCompleteRoute onAggregationCompleteRoute,
                         CompletionCallbackWrapper completionCallback) {

    if (aggregatorParameters.isTimeoutSet()) {
      evaluateConfiguredDelay("timeout", aggregatorParameters.getTimeout(), aggregatorParameters.getTimeoutUnit());
    }

    //We should synchronize the access to the storage because if the group is released due to a timeout, we may get duplicates.
    executeSynchronized(() -> {


      AggregatedContent aggregatedContent = getAggregatedContent();

      if (aggregatorParameters.isTimeoutSet()) {
        registerTaskIfNeeded(aggregatorParameters.getTimeout(), aggregatorParameters.getTimeoutUnit());
      }

      aggregatedContent.add(of(aggregatorParameters.getContent()), getCurrentTime());

      if (aggregatedContent.isComplete()) {
        notifyListenerOnComplete(aggregatedContent.getAggregatedElements());
        executeRouteWithAggregatedElements(onAggregationCompleteRoute, aggregatedContent.getAggregatedElements(),
                                           getAttributes(aggregatedContent), completionCallback);
        resetGroup();
      } else if (incrementalAggregationRoute != null) {
        executeRouteWithAggregatedElements(incrementalAggregationRoute, aggregatedContent.getAggregatedElements(),
                                           getAttributes(aggregatedContent), completionCallback);
      } else {
        completionCallback.success(Result.builder().build());
      }
    });
  }

  @Override
  void onTaskExecution() {
    onTimeout();
  }

  private void onTimeout() {
    executeSynchronized(() -> {
      notifyListenerOnTimeout(getAggregatedContent().getAggregatedElements());
      resetGroup();
    });
  }
}
