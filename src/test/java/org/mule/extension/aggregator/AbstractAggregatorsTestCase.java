/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY;
import static org.mule.functional.util.FlowExecutionLogger.resetLogsMap;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.event.Event;
import org.mule.tck.junit4.rule.SystemProperty;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Description;

public abstract class AbstractAggregatorsTestCase extends MuleArtifactFunctionalTestCase {


  static final String AGGREGATION_COMPLETE_ROUTE_KEY = "aggregationComplete";
  static final String INCREMENTAL_AGGREGATION_ROUTE_KEY = "incrementalAggregation";
  static final String LISTENER_ROUTE_KEY = "listenerCalled";

  @Rule
  public SystemProperty schedulingTasksPeriod = new SystemProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY, "500");

  @Before
  public void reset() {
    resetLogsMap();
  }


  @Test
  @Description("All attributes should be set and available inside a route")
  public void allAttributesAreSet() throws Exception {
    final String flowName = "attributesAreSet";
    final String randomString = "robin hood";
    flowRunner(flowName).withPayload(randomString).runNoVerify();
    sleep(100); //Wait a little bit so that times differ.
    flowRunner(flowName).withPayload(randomString).run();
  }


  @Test
  @Description("Variables set in route should be propagated to outside aggregator")
  public void propagatingVariablesOnIncremental() throws Exception {
    final String flowName = "propagateVariables";
    final String variableKey = "internalVariable";
    final String variableValue = "stuff";
    Event event = flowRunner(flowName).withVariable("variableKey", variableKey).withVariable("variableValue", variableValue).run();
    assertThat(event.getVariables().get(variableKey).getValue(), Matchers.is(Matchers.equalTo(variableValue)));
  }


}
