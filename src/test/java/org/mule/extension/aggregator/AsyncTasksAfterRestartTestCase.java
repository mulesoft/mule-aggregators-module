/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;

import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.AggregatorsFeature.AGGREGATORS_EXTENSION;

import org.mule.extension.aggregator.internal.config.AggregatorManager;
import org.mule.extension.aggregator.internal.privileged.executor.AbstractAggregatorExecutor;
import org.mule.extension.aggregator.internal.privileged.executor.GroupBasedAggregatorOperationsExecutor;
import org.mule.extension.aggregator.internal.privileged.executor.SizeBasedAggregatorOperationsExecutor;
import org.mule.extension.aggregator.internal.privileged.executor.TimeBasedAggregatorOperationsExecutor;
import org.mule.extension.aggregator.internal.routes.AggregationCompleteRoute;
import org.mule.extension.aggregator.internal.source.AggregatorListener;
import org.mule.extension.aggregator.internal.storage.content.SimpleAggregatedContent;
import org.mule.extension.aggregator.internal.storage.info.GroupAggregatorSharedInformation;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.SimpleMemoryObjectStore;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.cluster.DefaultClusterService;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.operation.ImmutableProcessorChainExecutor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import org.springframework.test.util.ReflectionTestUtils;


@Feature(AGGREGATORS_EXTENSION)
public class AsyncTasksAfterRestartTestCase extends AbstractMuleContextTestCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncTasksAfterRestartTestCase.class);

  private static final String AGGREGATOR_COMPLETE_CALLBACK_CONTEXT_PARAM = "aggregationComplete";

  private SimpleMemoryObjectStore<Serializable> objectStore;

  private AggregatorManager aggregatorManager;

  private SourceCallback sourceCallback;

  private SourceCallbackContext sourceCallbackCtx;

  private AbstractAggregatorExecutor aggregatorExecutorRedeploy;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    final Map<String, Object> objects = new HashMap<>();

    final ConfigurationProperties configProperties = mock(ConfigurationProperties.class);
    when(configProperties.resolveStringProperty(Mockito.anyString())).thenReturn(empty());
    objects.put("resolver", configProperties);
    objects.put("_ClusterService", new DefaultClusterService());

    return objects;
  }

  @Before
  public void before() throws RegistrationException {
    objectStore = new SimpleMemoryObjectStore<>();
    aggregatorManager = ((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(AggregatorManager.class);

    sourceCallback = mock(SourceCallback.class);

    sourceCallbackCtx = mock(SourceCallbackContext.class);
    when(sourceCallback.createContext()).thenReturn(sourceCallbackCtx);
  }

  @After
  public void after() throws MuleException {
    if (aggregatorExecutorRedeploy != null) {
      stopIfNeeded(aggregatorExecutorRedeploy);
      disposeIfNeeded(aggregatorExecutorRedeploy, LOGGER);
    }
  }

  /**
   * Migrated from mule-ee-distributions
   * <p>
   * {@link https://github.commulesoft-emu/mule-ee-distributions/blob/eebf8f83189a0666bc5e36da849aa4c0c0e41fa1/tests/src/test/java/commulesoft-emu/mule/distributions/server/AggregatorsTestCase.java#L199}
   */
  @Test
  @Description("Async tasks are rescheduled after redeploy in timeBasedAggregator")
  public void timeBasedAggregatorAsyncTasksAfterRedeploy() throws Exception {
    final Map<String, Object> params = new HashMap<>();
    params.put("objectStore", objectStore);
    params.put("name", "aggregator");
    params.put("maxSize", -1);

    final TimeBasedAggregatorOperationsExecutor aggregatorExecutor = new TimeBasedAggregatorOperationsExecutor(params);

    initialiseIfNeeded(aggregatorExecutor, muleContext);
    startIfNeeded(aggregatorExecutor);

    final AggregatorListener aggregatorListener = mock(AggregatorListener.class);
    when(aggregatorListener.getCallback()).thenReturn(sourceCallback);
    aggregatorManager.registerListener("aggregator", aggregatorListener);

    final Map<String, Object> operationParams = new HashMap<>();
    operationParams.put("content", new TypedValue<>("testPayload", DataType.STRING));
    operationParams.put("period", RECEIVE_TIMEOUT);
    operationParams.put("periodUnit", MILLISECONDS);
    addItemToAggregation(aggregatorExecutor, operationParams);

    // Wait until just before the aggregation times out to do a restart...
    Thread.sleep(3000);
    shutdown(aggregatorExecutor);

    // ...then check that the pending aggregation timeout event is triggered after restarting on its due time
    aggregatorExecutorRedeploy = new TimeBasedAggregatorOperationsExecutor(params);
    startUp(aggregatorListener);

    assertAsyncAggregation(sourceCallback, sourceCallbackCtx);
  }

  /**
   * Migrated from mule-ee-distributions
   * <p>
   * {@link https://github.commulesoft-emu/mule-ee-distributions/blob/eebf8f83189a0666bc5e36da849aa4c0c0e41fa1/tests/src/test/java/commulesoft-emu/mule/distributions/server/AggregatorsTestCase.java#L205}
   */
  @Test
  @Description("Async tasks are rescheduled after redeploy in sizeBasedAggregator")
  public void sizeBasedAggregatorAsyncTasksAfterRedeploy() throws Exception {
    final Map<String, Object> params = new HashMap<>();
    params.put("objectStore", objectStore);
    params.put("name", "aggregator");
    params.put("maxSize", 2);

    final SizeBasedAggregatorOperationsExecutor aggregatorExecutor = new SizeBasedAggregatorOperationsExecutor(params);

    initialiseIfNeeded(aggregatorExecutor, muleContext);
    startIfNeeded(aggregatorExecutor);

    final AggregatorListener aggregatorListener = mock(AggregatorListener.class);
    when(aggregatorListener.getCallback()).thenReturn(sourceCallback);
    when(aggregatorListener.shouldIncludeTimedOutGroups()).thenReturn(true);
    aggregatorManager.registerListener("aggregator", aggregatorListener);

    final Map<String, Object> operationParams = new HashMap<>();
    operationParams.put("content", new TypedValue<>("testPayload", DataType.STRING));
    operationParams.put("timeout", RECEIVE_TIMEOUT);
    operationParams.put("timeoutUnit", MILLISECONDS);
    addItemToAggregation(aggregatorExecutor, operationParams);

    // Wait until just before the aggregation times out to do a restart...
    Thread.sleep(3000);
    shutdown(aggregatorExecutor);

    // ...then check that the pending aggregation timeout event is triggered after restarting on its due time
    aggregatorExecutorRedeploy = new SizeBasedAggregatorOperationsExecutor(params);
    startUp(aggregatorListener);

    assertAsyncAggregation(sourceCallback, sourceCallbackCtx);
  }

  @Test
  @Description("Avoid race condition in object store after redeploy in sizeBasedAggregator")
  public void sizeBasedAggregatorDoesNotPushToObjectStoreWhenNoChanges() throws Exception {
    final Map<String, Object> params = new HashMap<>();
    SimpleMemoryObjectStore mockedObjectStore = spy(SimpleMemoryObjectStore.class);
    params.put("objectStore", mockedObjectStore);
    params.put("name", "aggregator");
    params.put("maxSize", 2);

    final SizeBasedAggregatorOperationsExecutor aggregatorExecutor = new SizeBasedAggregatorOperationsExecutor(params);

    initialiseIfNeeded(aggregatorExecutor, muleContext);
    startIfNeeded(aggregatorExecutor);

    final AggregatorListener aggregatorListener = mock(AggregatorListener.class);
    when(aggregatorListener.getCallback()).thenReturn(sourceCallback);
    when(aggregatorListener.shouldIncludeTimedOutGroups()).thenReturn(true);
    aggregatorManager.registerListener("aggregator", aggregatorListener);

    verify(mockedObjectStore, never()).store(any(), any());

    final Map<String, Object> operationParams = new HashMap<>();
    operationParams.put("content", new TypedValue<>("testPayload", DataType.STRING));
    operationParams.put("timeout", RECEIVE_TIMEOUT);
    operationParams.put("timeoutUnit", MILLISECONDS);
    operationParams.put("sequenceNumber", 1);
    addItemToAggregation(aggregatorExecutor, operationParams);

    verify(mockedObjectStore).store(any(), any());

    shutdown(aggregatorExecutor);

    // ...then check that the pending aggregation timeout event is triggered after restarting on its due time
    aggregatorExecutorRedeploy = new SizeBasedAggregatorOperationsExecutor(params);
    startUp(aggregatorListener);

    assertAsyncAggregation(sourceCallback, sourceCallbackCtx);
  }

  @Test
  @Description("Avoid race condition in object store after redeploy in timeBasedAggregator")
  public void timeBasedAggregatorDoesNotPushToObjectStoreWhenNoChanges() throws Exception {
    final Map<String, Object> params = new HashMap<>();
    SimpleMemoryObjectStore mockedObjectStore = spy(SimpleMemoryObjectStore.class);
    params.put("objectStore", mockedObjectStore);
    params.put("name", "aggregator");
    params.put("maxSize", -1);

    final TimeBasedAggregatorOperationsExecutor aggregatorExecutor = new TimeBasedAggregatorOperationsExecutor(params);

    initialiseIfNeeded(aggregatorExecutor, muleContext);
    startIfNeeded(aggregatorExecutor);

    final AggregatorListener aggregatorListener = mock(AggregatorListener.class);
    when(aggregatorListener.getCallback()).thenReturn(sourceCallback);
    aggregatorManager.registerListener("aggregator", aggregatorListener);

    verify(mockedObjectStore, never()).store(any(), any());

    final Map<String, Object> operationParams = new HashMap<>();
    operationParams.put("content", new TypedValue<>("testPayload", DataType.STRING));
    operationParams.put("period", RECEIVE_TIMEOUT);
    operationParams.put("periodUnit", MILLISECONDS);
    addItemToAggregation(aggregatorExecutor, operationParams);

    verify(mockedObjectStore).store(any(), any());

    shutdown(aggregatorExecutor);

    // ...then check that the pending aggregation timeout event is triggered after restarting on its due time
    aggregatorExecutorRedeploy = new TimeBasedAggregatorOperationsExecutor(params);
    startUp(aggregatorListener);

    assertAsyncAggregation(sourceCallback, sourceCallbackCtx);
  }

  @Test
  @Description("Update messages pending aggregation in ObjectStore when upgrading to 1.0.3")
  public void groupBasedAggregatorCompatibilityWithPreviousVersion() throws Exception {
    final Map<String, Object> params = new HashMap<>();
    SimpleMemoryObjectStore mockedObjectStore = new SimpleMemoryObjectStore();
    GroupAggregatorSharedInformation sharedInformation = new GroupAggregatorSharedInformation();
    SimpleAggregatedContent simpleAggregatedContent = new SimpleAggregatedContent(3);
    Map<Integer, TypedValue> oldSequencedElements = new HashMap<>();
    oldSequencedElements.put(1, TypedValue.of("value1"));
    oldSequencedElements.put(2, TypedValue.of("value2"));

    ReflectionTestUtils.setField(simpleAggregatedContent, "sequencedElements", oldSequencedElements, Map.class);
    sharedInformation.setAggregatedContent("1", simpleAggregatedContent);
    String aggregatorKey = "AGGREGATORS:GroupBasedAggregator:aggregator";
    mockedObjectStore.store(aggregatorKey, sharedInformation);

    params.put("objectStore", mockedObjectStore);
    params.put("name", "aggregator");
    params.put("maxSize", 3);

    final GroupBasedAggregatorOperationsExecutor aggregatorExecutor = new GroupBasedAggregatorOperationsExecutor(params);


    initialiseIfNeeded(aggregatorExecutor, muleContext);
    startIfNeeded(aggregatorExecutor);

    final AggregatorListener aggregatorListener = mock(AggregatorListener.class);
    when(aggregatorListener.getCallback()).thenReturn(sourceCallback);
    when(aggregatorListener.shouldIncludeTimedOutGroups()).thenReturn(true);
    aggregatorManager.registerListener("aggregator", aggregatorListener);

    final Map<String, Object> operationParams = new HashMap<>();
    operationParams.put("content", new TypedValue<>("testPayload", DataType.STRING));
    operationParams.put("timeout", RECEIVE_TIMEOUT);
    operationParams.put("timeoutUnit", MILLISECONDS);
    operationParams.put("evictionTime", RECEIVE_TIMEOUT);
    operationParams.put("evictionTimeUnit", MILLISECONDS);
    operationParams.put("groupId", "1");
    operationParams.put("groupSize", 1);
    operationParams.put("aggregationComplete", mock(CompletionCallback.class));
    addItemToAggregation(aggregatorExecutor, operationParams);

    shutdown(aggregatorExecutor);

    // ...then check that the pending aggregation timeout event is triggered after restarting on its due time
    aggregatorExecutorRedeploy = new GroupBasedAggregatorOperationsExecutor(params);
    startUp(aggregatorListener);
  }

  private void addItemToAggregation(final AbstractAggregatorExecutor aggregatorExecutor,
                                    final Map<String, Object> operationParams)
      throws MuleException {
    addItemToAggregation(aggregatorExecutor, operationParams, null);
  }

  private void addItemToAggregation(final AbstractAggregatorExecutor aggregatorExecutor,
                                    final Map<String, Object> operationParams, Integer sequenceNumber)
      throws MuleException {
    final ExecutionContextAdapter executionCtx = mock(ExecutionContextAdapter.class);

    when(executionCtx.getParameters()).thenReturn(operationParams);
    when(executionCtx.getEvent()).thenReturn(testEvent());
    when(executionCtx.getVariable(COMPLETION_CALLBACK_CONTEXT_PARAM)).thenReturn(mock(CompletionCallback.class));

    AggregationCompleteRoute aggregationCompleteRoute = mock(AggregationCompleteRoute.class);
    ImmutableProcessorChainExecutor chain = mock(ImmutableProcessorChainExecutor.class);

    doAnswer(invocation -> {
      Consumer<Result> futureComplete = invocation.getArgumentAt(2, Consumer.class);
      futureComplete.accept(Result.builder().build());
      return null;
    }).when(chain).process(any(), any(), any(), any());

    when(aggregationCompleteRoute.getChain()).thenReturn(chain);
    when(executionCtx.getParameter(AGGREGATOR_COMPLETE_CALLBACK_CONTEXT_PARAM)).thenReturn(aggregationCompleteRoute);

    if (sequenceNumber != null) {
      ItemSequenceInfo mockItemSequenceInfo = ItemSequenceInfo.of(sequenceNumber);
      CoreEvent mockCoreEvent = mock(CoreEvent.class);
      when(mockCoreEvent.getItemSequenceInfo()).thenReturn(java.util.Optional.of(mockItemSequenceInfo));
      when(executionCtx.getEvent()).thenReturn(mockCoreEvent);
    }

    aggregatorExecutor.execute(executionCtx);
  }

  @Step("Simulate a shutdown of the runtime only affecting the relevant components")
  private void shutdown(final AbstractAggregatorExecutor aggregatorExecutor) throws MuleException {
    stopIfNeeded(aggregatorExecutor);
    stopIfNeeded(aggregatorManager);
    disposeIfNeeded(aggregatorExecutor, LOGGER);
  }

  @Step("Simulate a restart of the runtime only affecting the relevant components")
  private void startUp(final AggregatorListener aggregatorListener) throws InitialisationException, MuleException {
    initialiseIfNeeded(aggregatorExecutorRedeploy, muleContext);
    startIfNeeded(aggregatorManager);
    when(aggregatorListener.isStarted()).thenReturn(true);
    aggregatorManager.onContextStarted();
    startIfNeeded(aggregatorExecutorRedeploy);
  }

  private void assertAsyncAggregation(final SourceCallback sourceCallback, final SourceCallbackContext sourceCallbackCtx) {
    probe(RECEIVE_TIMEOUT, 100, () -> {
      verify(sourceCallback).handle(any(Result.class), eq(sourceCallbackCtx));
      return true;
    });
  }

}
