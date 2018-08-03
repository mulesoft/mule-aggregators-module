/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteExecutedNTimes;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteNeverExecuted;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteNthExecution;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.FlakyTest;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.context.annotation.Description;

@FlakyTest(times = 10)
public class SizeBasedAggregatorTestCase extends CommonAggregatorsTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"size-based-aggregators-config.xml", "common-aggregators-config.xml"};
  }

  @Test
  @Description("Aggregator works with only aggregation-complete route defined")
  public void noIncrementalAggregationRoute() throws Exception {
    final String flowName = "noIncrementalAggregationRoute";
    flowRunner(flowName).withPayload(1).run();
    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);

    flowRunner(flowName).withPayload(2).run();
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 1, 1, 2);

    flowRunner(flowName).withPayload(3).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);

    flowRunner(flowName).withPayload(4).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 2);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 2, 3, 4);

  }

  @Test
  @Description("incremental-aggregation route is called after every event, except when aggregation is complete")
  public void incrementalAggregationRoute() throws Exception {
    final String flowName = "incrementalAggregationRoute";

    flowRunner(flowName).withPayload(1).run();
    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1, 1);

    flowRunner(flowName).withPayload(2).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1);
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 1, 1, 2);
  }

  @Test
  @Description("incremental-aggregarion route is called every time with all the aggregated elements until the last execution")
  public void incrementalAggregationRouteCalledMultipleTimes() throws Exception {
    final String flowName = "incrementalAggregationRouteCalledMultipleTimes";

    flowRunner(flowName).withPayload(1).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1, 1);

    flowRunner(flowName).withPayload(2).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 2);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 2, 1, 2);

    flowRunner(flowName).withPayload(3).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 3);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 3, 1, 2, 3);

    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);

    flowRunner(flowName).withPayload(4).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 3);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 1, 1, 2, 3, 4);

  }

  @Test
  @Description("Hooked listener is called when aggregation is complete")
  public void listenerCalledOnComplete() throws Exception {
    noIncrementalAggregationRoute();
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 2);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 1, 1, 2);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 2, 3, 4);
  }

  @Test
  @Description("Listener is not called due to timeout if flag is not set")
  public void listenerNotCalledOnTimeoutIfAttributeNotSet() throws Exception {
    final String flowName = "timeoutAggregator1";
    flowRunner(flowName).withPayload(1).run();
    waitForAggregatorTask(100);
    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);
    assertRouteNeverExecuted(LISTENER_ROUTE_KEY);
  }

  @Test
  @Description("Listener is called due to timeout if flag is set")
  public void listenerCalledOnTimeoutIfAttributeSet() throws Exception {
    final String flowName = "timeoutAggregator2";
    flowRunner(flowName).withPayload(1).run();
    waitForAggregatorTask(100);
    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 1);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 1, 1);
  }

  @Test
  @Description("Message after aggregator is not modified even it incremental-aggregation route is executed")
  public void messageBeforeAndAfterAggregatorWithIncrementalAggregation() throws Exception {
    final String flowName = "incrementalAggregationRouteCalledMultipleTimes";
    final String randomMessage = "No, seriously. This is da bomb!";

    CoreEvent event = flowRunner(flowName).withPayload(1).run();
    assertThat(event.getMessage().getPayload().getValue(), is(equalTo(1)));

    event = flowRunner(flowName).withPayload(randomMessage).run();
    assertThat(event.getMessage().getPayload().getValue(), is(equalTo(randomMessage)));

    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 2);

  }

  @Test
  @Description("Message after aggregator is not modified even it aggregation-complete route is executed")
  public void messageBeforeAndAfterAggregatorWithAggregationComplete() throws Exception {
    final String flowName = "noIncrementalAggregationRoute";

    CoreEvent event = flowRunner(flowName).withPayload(1).run();
    assertThat(event.getMessage().getPayload().getValue(), is(equalTo(1)));

    event = flowRunner(flowName).withPayload(2).run();
    assertThat(event.getMessage().getPayload().getValue(), is(equalTo(2)));

    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);
  }

  @Test
  @Description("Variables set in route should be propagated to outside aggregator")
  public void propagatingVariablesOnComplete() throws Exception {
    final String flowName = "propagateVariables";
    final String variableKey = "internalVariable";
    final String variableValue = "stuff";
    Event event = flowRunner(flowName).run();
    assertThat(event.getVariables(), not(hasKey(variableKey)));
    event = flowRunner(flowName).withVariable("variableKey", variableKey).withVariable("variableValue", variableValue).run();
    assertThat(event.getVariables().get(variableKey).getValue(), Matchers.is(Matchers.equalTo(variableValue)));
  }

  @Test
  @Description("Every incremental aggregation should have the same groupId")
  public void sameIdForIncrementalAndComplete() throws Exception {
    final String flowName = "onIncrementalIdCheck";
    final String idPlaceholderKey = "id";
    Event event = flowRunner(flowName).runNoVerify();
    flowRunner(flowName).withVariable(idPlaceholderKey, event.getVariables().get(idPlaceholderKey)).run();
    flowRunner(flowName).withVariable(idPlaceholderKey, event.getVariables().get(idPlaceholderKey)).run();
  }

  @Test
  @Description("Every incremental aggregation should have the same groupId, the same for complete and listener")
  public void sameIdForIncrementalCompleteAndListener() throws Exception {
    final String flowName = "onCompleteAndListenerIdCheck";
    final String idPlaceholderKey = "id";
    Event event = flowRunner(flowName).runNoVerify();
    flowRunner(flowName).withVariable(idPlaceholderKey, event.getVariables().get(idPlaceholderKey)).run();
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 1);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 1, event.getVariables().get(idPlaceholderKey).getValue());
  }

  @Test
  @Description("once a group is completed, the groupId should be different")
  public void groupIdChangesAfterComplete() throws Exception {
    final String flowName = "idChangeAfterComplete";
    final String idPlaceholderKey = "id";
    Event event = flowRunner(flowName).runNoVerify();
    flowRunner(flowName).runNoVerify();
    flowRunner(flowName).withVariable(idPlaceholderKey, event.getVariables().get(idPlaceholderKey)).run();
  }

}
