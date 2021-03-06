/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteExecutedNTimes;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteNeverExecuted;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteNthExecution;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.core.api.event.CoreEvent;

import org.junit.Test;
import org.springframework.context.annotation.Description;

public class TimeBasedAggregatorsTestCase extends CommonAggregatorsTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"time-based-aggregators-config.xml", "common-aggregators-config.xml"};
  }

  @Test
  @Description("Incremental aggregation route should be called")
  public void incrementalAggregationRouteIsCalled() throws Exception {
    final String flowName = "incrementalAggregationRoute";

    flowRunner(flowName).withPayload(1).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1, 1);

    flowRunner(flowName).withPayload(2).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 2);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 2, 1, 2);
  }

  @Test
  @Description("Registered listener should be triggered when period is completed")
  public void listenerIsCalledOnTimeout() throws Exception {
    final String flowName = "aggregatorWithSmallPeriod";
    //Check that the listener is never called because the time is computed from the moment the first event is received
    assertRouteNeverExecuted(LISTENER_ROUTE_KEY);
    flowRunner(flowName).withPayload(1).run();
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 1);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 1, 1);

  }

  @Test
  @Description("Registered listener should be triggered when maxSize is reached before period")
  public void listenerIsCalledWhenMaxSizeIsReached() throws Exception {
    final String flowName = "aggregatorWithMaxSize";

    flowRunner(flowName).withPayload(1).run();
    assertRouteNeverExecuted(LISTENER_ROUTE_KEY);

    flowRunner(flowName).withPayload(2).run();
    assertRouteNeverExecuted(LISTENER_ROUTE_KEY);

    flowRunner(flowName).withPayload(3).run();
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 1);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 1, 1, 2, 3);
  }

  @Test
  @Description("The message after aggregator should be the same as before")
  public void elementAfterAggregator() throws Exception {
    final String flowName = "beforeAndAfterAggregator";
    final String randomMessage = "This is the best extension ever!";

    CoreEvent event = flowRunner(flowName).withPayload(1).run();
    assertThat(event.getMessage().getPayload().getValue(), is(equalTo(1)));

    event = flowRunner(flowName).withPayload(randomMessage).run();
    assertThat(event.getMessage().getPayload().getValue(), is(equalTo(randomMessage)));
  }

  @Test
  @Description("The message after aggregator should be the same as before even if incremental aggregarion route is executed")
  public void elementAfterAggregatorExecutingIncrementalAggregation() throws Exception {
    final String flowName = "incrementalAggregationRoute";

    CoreEvent event = flowRunner(flowName).withPayload(1).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1);
    assertThat(event.getMessage().getPayload().getValue(), is(equalTo(1)));

    event = flowRunner(flowName).withPayload(2).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 2);
    assertThat(event.getMessage().getPayload().getValue(), is(equalTo(2)));
  }

  @Test
  @Description("Every incremental aggregation attribute groupId and the listener correlationId should be the same")
  public void sameIdForIncrementalAndListener() throws Exception {
    final String flowName = "idCheck";
    final String idPlaceholderKey = "id";
    Event event = flowRunner(flowName).runNoVerify();
    flowRunner(flowName).withVariable(idPlaceholderKey, event.getVariables().get(idPlaceholderKey)).run();
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
