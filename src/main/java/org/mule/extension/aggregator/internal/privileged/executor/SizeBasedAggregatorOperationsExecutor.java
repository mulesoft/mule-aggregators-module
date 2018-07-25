/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.executor;

import static java.lang.String.format;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.AGGREGATOR_CONFIG;
import static org.mule.runtime.api.metadata.TypedValue.of;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.extension.aggregator.internal.parameter.SizeBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.privileged.CompletionCallbackWrapper;
import org.mule.extension.aggregator.internal.routes.AggregationCompleteRoute;
import org.mule.extension.aggregator.internal.routes.IncrementalAggregationRoute;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
    Optional<ItemSequenceInfo> itemSequenceInfo = getItemSequenceInfo(executionContext);
    aggregate(parameters, incrementalAggregationRoute, aggregationCompleteRoute,
              new CompletionCallbackWrapper(context.getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM), event), itemSequenceInfo);
    return null;
  }

  private SizeBasedAggregatorParameterGroup createParameters(Map<String, Object> parameterMap) {
    SizeBasedAggregatorParameterGroup parameters = new SizeBasedAggregatorParameterGroup();
    parameters.setContent((TypedValue) parameterMap.get("content"));
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
                         CompletionCallbackWrapper completionCallback,
                         Optional<ItemSequenceInfo> itemSequenceInfo) {


    evaluateParameters(aggregatorParameters);

    CompletableFuture<Result<Object, Object>> future = new CompletableFuture<>();

    //We should synchronize the access to the storage because if the group is released due to a timeout, we may get duplicates.
    executeSynchronized(() -> {

      AggregatedContent aggregatedContent = getAggregatedContent();

      if (aggregatorParameters.isTimeoutSet()) {
        registerAsyncAggregationIfNeeded(aggregatorParameters.getTimeout(), aggregatorParameters.getTimeoutUnit());
      }

      addToStorage(aggregatedContent, aggregatorParameters.getContent(), itemSequenceInfo);

      if (aggregatedContent.isComplete()) {
        notifyListenerOnComplete(aggregatedContent.getAggregatedElements(), getAggregationId());
        executeRouteWithAggregatedElements(onAggregationCompleteRoute, aggregatedContent.getAggregatedElements(),
                                           getAttributes(aggregatedContent), future);
        onCompleteAggregation();

      } else if (incrementalAggregationRoute != null) {
        executeRouteWithAggregatedElements(incrementalAggregationRoute, aggregatedContent.getAggregatedElements(),
                                           getAttributes(aggregatedContent), future);
      } else {
        future.complete(Result.builder().build());
      }
    });

    finishExecution(future, completionCallback);
  }

  private void evaluateParameters(SizeBasedAggregatorParameterGroup aggregatorParameters) {
    if (aggregatorParameters.isTimeoutSet()) {
      if (aggregatorParameters.getTimeout() <= 0) {
        throw new ModuleException(format("A configured timeout of %d is not valid. Value should be bigger than 0",
                                         aggregatorParameters.getTimeout()),
                                  AGGREGATOR_CONFIG);
      }
      evaluateConfiguredDelay("timeout", aggregatorParameters.getTimeout(), aggregatorParameters.getTimeoutUnit());
    }
    if (maxSize <= 0) {
      throw new ModuleException(format("maxSize should be bigger than 0, got: %d", maxSize), AGGREGATOR_CONFIG);
    }
  }

  @Override
  void onAsyncAggregationExecution() {
    onTimeout();
  }

  private void onTimeout() {
    notifyListenerOnTimeout(getAggregatedContent().getAggregatedElements(), getAggregationId());
    resetGroup();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Aggregation timed out");
    }
  }
}
