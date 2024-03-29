/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.executor;

import static java.lang.String.format;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.AGGREGATOR_CONFIG;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.extension.aggregator.internal.parameter.TimeBasedAggregatorParameterGroup;
import org.mule.extension.aggregator.internal.privileged.CompletionCallbackWrapper;
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
 * Custom executor for {@code timeBasedAggregator}.
 * <p/>
 * Class keeping all the logic for the aggregator defined in {@link org.mule.extension.aggregator.internal.operations.TimeBasedAggregatorOperations}
 * <p>
 * The reason why we have this custom executor is that unlike regular routers, we should be able to both, have the it
 * as void (the event out is the same as the event in) and propagate variables in case any is set inside a route.
 *
 * @since 1.0
 */
public class TimeBasedAggregatorOperationsExecutor extends SingleGroupAggregatorExecutor {

  private static final String AGGREGATOR_KEY = "TimeBasedAggregator";
  private int maxSize;

  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  public TimeBasedAggregatorOperationsExecutor(Map<String, Object> params) {
    injectParameters(params);
  }

  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> executionContext) {
    final ExecutionContextAdapter<OperationModel> context = (ExecutionContextAdapter<OperationModel>) executionContext;
    final CoreEvent event = context.getEvent();
    IncrementalAggregationRoute incrementalAggregationRoute = context.getParameter("incrementalAggregation");
    TimeBasedAggregatorParameterGroup parameters = createParameters(context.getParameters());
    Optional<ItemSequenceInfo> itemSequenceInfo = getItemSequenceInfo(executionContext);
    aggregate(parameters, incrementalAggregationRoute,
              new CompletionCallbackWrapper(context.getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM), event), itemSequenceInfo);
    return null;
  }

  private TimeBasedAggregatorParameterGroup createParameters(Map<String, Object> parameterMap) {
    TimeBasedAggregatorParameterGroup parameters = new TimeBasedAggregatorParameterGroup();
    parameters.setContent((TypedValue) parameterMap.get("content"));
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
                         CompletionCallbackWrapper completionCallback,
                         Optional<ItemSequenceInfo> itemSequenceInfo) {

    evaluateParameters(aggregatorParameters);

    CompletableFuture<Result<Object, Object>> future = new CompletableFuture<>();

    //We should synchronize the access to the storage to account for the situation when the period is completed while
    //executing a new event.
    executeSynchronized(() -> {

      registerAsyncAggregationIfNeeded(aggregatorParameters.getPeriod(), aggregatorParameters.getPeriodUnit());

      AggregatedContent aggregatedContent = getAggregatedContent();

      addToStorage(aggregatedContent, aggregatorParameters.getContent(), itemSequenceInfo);

      if (aggregatedContent.isComplete()) {
        notifyListenerOnComplete(aggregatedContent.getAggregatedElements(), getAttributes(getAggregatedContent()));
        onCompleteAggregation();
        future.complete(Result.builder().build());
      } else if (incrementalAggregationRoute != null) {
        executeRouteWithAggregatedElements(incrementalAggregationRoute, aggregatedContent.getAggregatedElements(),
                                           getAttributes(aggregatedContent), future);
      } else {
        future.complete(Result.builder().build());
      }
      return true;
    });

    finishExecution(future, completionCallback);
  }

  private void evaluateParameters(TimeBasedAggregatorParameterGroup parameterGroup) {

    if (parameterGroup.getPeriod() <= 0) {
      throw new ModuleException(format("A configured period of %d is not valid. Value should be bigger than 0",
                                       parameterGroup.getPeriod()),
                                AGGREGATOR_CONFIG);
    }

    evaluateConfiguredDelay("period", parameterGroup.getPeriod(), parameterGroup.getPeriodUnit());

    if (maxSize == 0) {
      throw new ModuleException("maxSize can't be 0", AGGREGATOR_CONFIG);
    }

  }

  @Override
  void onAsyncAggregationExecution() {
    getElementsAndNotifyListener();
  }

  private void getElementsAndNotifyListener() {
    notifyListenerOnComplete(getAggregatedContent().getAggregatedElements(), getAttributes(getAggregatedContent()));
    resetGroup();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Aggregation period complete");
    }
  }

}
