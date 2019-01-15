/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class MigrationToolTestCase extends AbstractAggregatorsTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"migration-tool-config.xml"};
  }

  @Test
  public void executionWaitsUntilAggregationIsComplete() throws Exception {
    Event event = flowRunner("splitAggregateFlow").withPayload(new Integer[] {1, 2, 3}).run();
    List<TypedValue<Integer>> aggregatedElements = (List) event.getMessage().getPayload().getValue();
    assertThat(aggregatedElements.get(0).getValue(), equalTo(1));
    assertThat(aggregatedElements.get(1).getValue(), equalTo(2));
    assertThat(aggregatedElements.get(2).getValue(), equalTo(3));
  }

  @Test
  public void correlationIdRemains() throws Exception {
    final String sourceCorrelationId = "correlationId";
    Event event =
        flowRunner("splitAggregateFlow").withPayload(new Integer[] {1, 2, 3}).withSourceCorrelationId(sourceCorrelationId).run();
    assertThat(event.getCorrelationId(), equalTo(sourceCorrelationId));
  }

  @Test
  public void timeoutNoFailOnTimeout() throws Exception {
    Event event =
        flowRunner("splitAggregateWithDelayFlow").withVariable("failOnTimeout", false).withPayload(new Integer[] {1, 2, 3}).run();
    assertThat(((TypedValue) (((List) event.getMessage().getPayload().getValue()).get(0))).getValue(), equalTo(1));
  }

  @Test
  public void timeoutFailOnTimeout() throws Exception {
    Event event =
        flowRunner("splitAggregateWithDelayFlow").withVariable("failOnTimeout", true).withPayload(new Integer[] {1, 2, 3}).run();
    assertThat(event.getMessage().getPayload().getValue(), equalTo("OK"));
  }

}
