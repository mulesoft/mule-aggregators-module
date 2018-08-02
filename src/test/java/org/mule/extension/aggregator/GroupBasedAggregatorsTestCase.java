/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteExecutedNTimes;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteNeverExecuted;
import static org.mule.functional.util.FlowExecutionLogger.assertRouteNthExecution;

import org.mule.runtime.api.event.Event;
import org.mule.tck.junit4.FlakyTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.context.annotation.Description;

/**
 * Tests for Group based aggregators
 */
@FlakyTest(times = 10)
public class GroupBasedAggregatorsTestCase extends CommonAggregatorsTestCase {

  public static final String GROUP_ID_VARIABLE_KEY = "gid";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"group-based-aggregators-config.xml", "common-aggregators-config.xml"};
  }


  @Test
  @Description("Assert that group is never complete due to groupId being different for every message")
  public void groupNeverCompletesWithDifferentGroupId() throws Exception {
    final String flowName = "aggregationCompleteRoute";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(1).run();
    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 2).withPayload(2).run();
    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);
  }

  @Test
  @Description("Check that aggregationComplete route is called when group is completed")
  public void aggregationCompleteIsCalledForGroup() throws Exception {
    final String flowName = "aggregationCompleteRoute";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(1).run();
    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);
    assertRouteNeverExecuted(LISTENER_ROUTE_KEY);
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 2).withPayload(2).run();
    assertRouteNeverExecuted(AGGREGATION_COMPLETE_ROUTE_KEY);
    assertRouteNeverExecuted(LISTENER_ROUTE_KEY);
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(2).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 1, 1, 2);
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 1);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 1, 1, 2);
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 2).withPayload(1).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 2);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 2, 2, 1);
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 2);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 2, 2, 1);
  }

  @Test
  @Description("incrementalAggregation route is called with the aggregated elements of the last received groupId")
  public void incrementalAggregationWithDifferentId() throws Exception {
    final String flowName = "incrementalAggregationRoute";
    final String randomString = "shhh!";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(1).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 1, 1);

    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 2).withPayload(2).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 2);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 2, 2);

    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(randomString).run();
    assertRouteExecutedNTimes(INCREMENTAL_AGGREGATION_ROUTE_KEY, 3);
    assertRouteNthExecution(INCREMENTAL_AGGREGATION_ROUTE_KEY, 3, 1, randomString);
  }

  @Test
  @Description("When timeout is complete, the listener is notified with the aggregated elements")
  public void listenerOnTimeout() throws Exception {
    final String flowName = "shortTimeoutAggregator";
    flowRunner(flowName).withPayload(1).run();
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 1);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 1, 1);
  }


  @Test
  @Description("The hooked listener is not called on timeout if attribute is not set")
  public void listenerOnTimeoutNotCalledIfAttributeNotSet() throws Exception {
    final String flowName = "shortTimeoutAggregator2";
    flowRunner(flowName).withPayload(1).run();
    assertRouteNeverExecuted(LISTENER_ROUTE_KEY);
  }


  @Test
  @Description("Scheduled timeout executed after group is evicted does nothing. For that scenario to take place, the group should be completed")
  public void timeoutOnEvictedGroup() throws Exception {
    final String flowName = "shortTimeoutAndEvictionTime";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(1).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 1, 1);

    sleep(100); //Let the group be evicted

    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(2).run();
    //No exception will be thrown because when the timeout should have been executed, the group was already evicted.
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 2);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 2, 2);
  }

  @Test
  @Description("Check that the groups get evicted after group timeout is completed. If group was not evicted, an error should have been thrown")
  public void groupsAreEvictedAfterEvictionTimeout() throws Exception {
    final String flowName = "shortEvictionTime";

    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(1).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 1);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 1, 1);

    sleep(100); //Let the group be evicted

    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(1).run();
    assertRouteExecutedNTimes(AGGREGATION_COMPLETE_ROUTE_KEY, 2);
    assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, 2, 1);
  }

  @Test
  @Description("Completed not evicted groups should raise exception if new element arrives")
  public void completedGroupsThrowException() throws Exception {
    final String flowName = "groupCompleteExceptionThrowingFlow";
    final String randomString = "there is nothing to see here";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(randomString).runNoVerify();
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(randomString).run();
  }

  @Test
  @Description("Timed out not evicted groups should raise exception if new element arrives")
  public void timedOutGroupsThrowException() throws Exception {
    final String flowName = "groupTimedOutExceptionThrowingFlow";
    final String randomString = "this is not random at all, is it?";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(randomString).runNoVerify();

    sleep(100); //Let the group execute timeout

    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, 1).withPayload(randomString).run();
  }

  @Test
  @Description("Exception is thrown if can't resolve groupId expression")
  public void invalidGroupIdExpressionThrowsException() throws Exception {
    final String flowName = "noGroupIdExceptionThrowingFlow";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, null).withPayload(1).run();
  }

  @Test
  @Description("Exception is thrown if can't resolve groupSize expression")
  public void invalidGroupSizeExpressionThrowsException() throws Exception {
    final String flowName = "noGroupSizeExceptionThrowingFlow";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, null).withPayload(1).run();
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
    assertThat(event.getVariables().get(variableKey).getValue(), is(equalTo(variableValue)));
  }

  @Test
  @Description("Every incremental aggregation should have the same groupId")
  public void sameIdForIncrementalAndComplete() throws Exception {
    final String flowName = "onIncrementalIdCheck";
    final String groupId = "theID";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, groupId).run();
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, groupId).run();
  }

  @Test
  @Description("Every incremental aggregation should have the same groupId, the same for complete and listener")
  public void sameIdForIncrementalCompleteAndListener() throws Exception {
    final String flowName = "onCompleteAndListenerIdCheck";
    final String groupId = "theID";
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, groupId).runNoVerify();
    flowRunner(flowName).withVariable(GROUP_ID_VARIABLE_KEY, groupId).run();
    assertRouteExecutedNTimes(LISTENER_ROUTE_KEY, 1);
    assertRouteNthExecution(LISTENER_ROUTE_KEY, 1, groupId);
  }

  @Description("Eviction time of 0 means evict immediately")
  public void evictGroupImmediately() throws Exception {
    final String flowName = "evictImmediately";
    for (int i = 1; i < 5; i++) {
      flowRunner(flowName).withPayload(i).run();
      assertRouteNthExecution(AGGREGATION_COMPLETE_ROUTE_KEY, i, i);
    }
  }

}
