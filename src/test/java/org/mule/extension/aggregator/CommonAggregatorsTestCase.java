/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;

import static java.lang.Thread.sleep;
import static org.codehaus.plexus.util.IOUtil.toByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteExecutedNTimes;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteNthExecution;
import static org.mule.runtime.api.message.ItemSequenceInfo.of;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.context.annotation.Description;

public abstract class CommonAggregatorsTestCase extends MultipleOSAggregatorTestCase {

  @Test
  @Description("All attributes should be set and available inside a route")
  public void allAttributesAreSet() throws Exception {
    final String flowName = "attributesAreSet";
    final String randomString = "robin hood";
    flowRunner(flowName).withPayload(randomString).runNoVerify();
    sleep(1000); //Wait a little bit so that times differ.
    flowRunner(flowName).withPayload(randomString).run();
  }

  @Test
  @Description("Variables set in route should be propagated to outside aggregator")
  public void propagatingVariablesOnIncremental() throws Exception {
    final String flowName = "propagateVariables";
    final String variableKey = "internalVariable";
    final String variableValue = "stuff";
    Event event =
        flowRunner(flowName).withVariable("variableKey", variableKey).withVariable("variableValue", variableValue).run();
    assertThat(event.getVariables().get(variableKey).getValue(), Matchers.is(Matchers.equalTo(variableValue)));
  }

  @Test
  @Description("If the event arrives with a sequence number, then it's sorted")
  public void elementsWithSequenceNumberAreSorted() throws Exception {
    final String flowName = "sortedItems";
    flowRunner(flowName).withItemSequenceInfo(of(2, 2)).withPayload(2).run();
    flowRunner(flowName).withItemSequenceInfo(of(1, 2)).withPayload(1).run();
    flowRunner(flowName).withItemSequenceInfo(of(0, 2)).withPayload(0).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 1, 0, 1, 2);
  }

  @Test
  @Description("If elements with sequence number arrive mixed with elements without, then the sequence number ones will be ordered and the non sequence number be added last as they arrived")
  public void mixedElementsAreSorted() throws Exception {
    final String flowName = "sortedMixedItems";
    flowRunner(flowName).withPayload(5).run();
    flowRunner(flowName).withItemSequenceInfo(of(2, 2)).withPayload(2).run();
    flowRunner(flowName).withPayload(1).run();
    flowRunner(flowName).withPayload(2).run();
    flowRunner(flowName).withItemSequenceInfo(of(0, 2)).withPayload(0).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 1, 0, 2, 5, 1, 2);
  }

  @Test
  @Description("scheduled period aggregation is not executed after size aggregation")
  public void scheduledAggregationNotExecutedAfterSize() throws Exception {
    final String flowName = "scheduledAggregationNotExecuted";
    final String payload = "lrm";
    flowRunner(flowName).withPayload(payload).run();
    flowRunner(flowName).withPayload(payload).run();
    waitForAggregatorTask(0); //Wait a little bit to make sure the timeout aggregation was actually scheduled
    flowRunner(flowName).withPayload(payload).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);
    waitForAggregatorTask(100); //Wait to make sure timeout is never executed
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 1);
  }

  @Test
  @Description("BigContents are correctly serialized to the OS")
  public void bigContentAggregation() throws Exception {
    final String flowName = "aggregateMessageWithBigPayloadOnPersistentOS";
    final InputStream payload = Thread.currentThread().getContextClassLoader().getResourceAsStream(BIG_PAYLOAD_FILE_NAME);
    final byte[] payloadBytes = toByteArray(payload);
    flowRunner(flowName).run();
    Event resultEvent = flowRunner(flowName).run();
    List<TypedValue> aggregatedElements = (List<TypedValue>) ((TypedValue) resultEvent.getVariables().get("result")).getValue();
    assertThat(((Message) aggregatedElements.get(0).getValue()).getPayload().getValue(), is(equalTo(payloadBytes)));
  }

  @Test
  @Description("AggregatorListener receives attributes")
  public void listenerAttributes() throws Exception {
    flowRunner("listenerAttributes").run();
    ObjectStore currentObjectStore = objectStoreManager.getObjectStore(objectStore.getValue());
    //Let the listener be executed
    sleep(500);
    assertThat(((TypedValue<Map<String, Object>>) currentObjectStore.retrieve("onCompleteAttributes")).getValue().values(),
               not(hasItem(nullValue())));
    assertThat(((TypedValue<Map<String, Object>>) currentObjectStore.retrieve("onListenerAttributes")).getValue().values(),
               not(hasItem(nullValue())));
    assertThat(currentObjectStore.retrieve("onCompleteAttributes"),
               is(equalTo(currentObjectStore.retrieve("onListenerAttributes"))));
  }

  @Test
  @Description("AggregatorListener receives attributes when timeout")
  public void listenerAttributesWhenTimeout() throws Exception {
    flowRunner("listenerAttributesOnTimeout").run();
    //Let the listener be executed
    waitForAggregatorTask(200);
    ObjectStore currentObjectStore = objectStoreManager.getObjectStore(objectStore.getValue());
    assertThat(((TypedValue<Map<String, Object>>) currentObjectStore.retrieve("onListenerAttributes")).getValue().values(),
               not(hasItem(nullValue())));
    assertThat(((TypedValue<Map<String, Object>>) currentObjectStore.retrieve("onListenerAttributes")).getValue()
        .get("isAggregationComplete"),
               is(false));
  }

}

