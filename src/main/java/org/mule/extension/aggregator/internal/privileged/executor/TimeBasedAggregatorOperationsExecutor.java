/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.executor;

import static org.mule.runtime.api.metadata.TypedValue.of;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.extension.aggregator.api.TimeBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.privileged.CompletionCallbackWrapper;
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

public class TimeBasedAggregatorOperationsExecutor extends SingleGroupAggregatorExecutor {

  private static final String AGGREGATOR_KEY = "TimeBasedAggregator";
  private int maxSize;

  public TimeBasedAggregatorOperationsExecutor(Map<String, Object> params) {
    injectParameters(params);
  }

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    final ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;
    final CoreEvent event = context.getEvent();
    IncrementalAggregationRoute incrementalAggregationRoute = context.getParameter("incrementalAggregation");
    TimeBasedAggregatorParameterGroup parameters = createParameters(context.getParameters());
    aggregate(parameters, incrementalAggregationRoute,
              new CompletionCallbackWrapper(context.getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM), event));
    return null;
  }

  private TimeBasedAggregatorParameterGroup createParameters(Map<String, Object> parameterMap) {
    TimeBasedAggregatorParameterGroup parameters = new TimeBasedAggregatorParameterGroup();
    parameters.setContent(parameterMap.get("content"));
    parameters.setPeriod((Integer) parameterMap.get("period"));
    parameters.setPeriodUnit((TimeUnit) parameterMap.get("periodUnit"));
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


  private void aggregate(TimeBasedAggregatorParameterGroup aggregatorParameters,
                         IncrementalAggregationRoute incrementalAggregationRoute,
                         CompletionCallbackWrapper completionCallback) {

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
        completionCallback.success(Result.builder().build());
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
    getElementsAndNotifyListener();
  }

  private void getElementsAndNotifyListener() {
    executeSynchronized(() -> {
      notifyListenerOnComplete(getAggregatedContent().getAggregatedElements());
      resetGroup();
    });
  }

}
