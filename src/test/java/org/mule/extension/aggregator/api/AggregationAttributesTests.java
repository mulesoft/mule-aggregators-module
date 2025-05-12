/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.api;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

public class AggregationAttributesTests {

  private AggregationAttributes attributes;
  private static final String TEST_AGGREGATION_ID = "test-id-123";
  private static final long TEST_FIRST_ARRIVAL_TIME = 1000L;
  private static final long TEST_LAST_ARRIVAL_TIME = 2000L;

  @Before
  public void setUp() {
    attributes = new AggregationAttributes();
  }

  @Test
  public void testDefaultConstructor() {
    assertNotNull(attributes);
    assertFalse(attributes.isAggregationComplete());
    assertFalse(attributes.getIsAggregationComplete());
  }

  @Test
  public void testParameterizedConstructor() {
    attributes = new AggregationAttributes(TEST_AGGREGATION_ID,
                                           TEST_FIRST_ARRIVAL_TIME,
                                           TEST_LAST_ARRIVAL_TIME,
                                           true);

    assertEquals(TEST_AGGREGATION_ID, attributes.getAggregationId());
    assertEquals(new Date(TEST_FIRST_ARRIVAL_TIME), attributes.getFirstItemArrivalTime());
    assertEquals(new Date(TEST_LAST_ARRIVAL_TIME), attributes.getLastItemArrivalTime());
    assertTrue(attributes.isAggregationComplete());
    assertTrue(attributes.getIsAggregationComplete());
  }

  @Test
  public void testSetAndGetAggregationId() {
    attributes.setAggregationId(TEST_AGGREGATION_ID);
    assertEquals(TEST_AGGREGATION_ID, attributes.getAggregationId());
  }

  @Test
  public void testSetAndGetFirstItemArrivalTime() {
    attributes.setFirstItemArrivalTime(TEST_FIRST_ARRIVAL_TIME);
    assertEquals(new Date(TEST_FIRST_ARRIVAL_TIME), attributes.getFirstItemArrivalTime());
  }

  @Test
  public void testSetAndGetLastItemArrivalTime() {
    attributes.setLastItemArrivalTime(TEST_LAST_ARRIVAL_TIME);
    assertEquals(new Date(TEST_LAST_ARRIVAL_TIME), attributes.getLastItemArrivalTime());
  }

  @Test
  public void testSetAggregationComplete() {
    attributes.setAggregationComplete();
    assertTrue(attributes.isAggregationComplete());
    assertTrue(attributes.getIsAggregationComplete());
  }

  @Test
  public void testSetAggregationCompleteWithBoolean() {
    attributes.setAggregationComplete(true);
    assertTrue(attributes.isAggregationComplete());
    assertTrue(attributes.getIsAggregationComplete());

    attributes.setAggregationComplete(false);
    assertFalse(attributes.isAggregationComplete());
    assertFalse(attributes.getIsAggregationComplete());
  }

  @Test
  public void testGetIsAggregationComplete() {
    attributes.setAggregationComplete(true);
    assertTrue(attributes.getIsAggregationComplete());

    attributes.setAggregationComplete(false);
    assertFalse(attributes.getIsAggregationComplete());
  }
}
