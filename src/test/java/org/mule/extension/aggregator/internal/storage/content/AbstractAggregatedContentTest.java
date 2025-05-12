/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.content;

import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AbstractAggregatedContentTest {

  private TestAggregatedContent aggregatedContent;

  @Before
  public void setUp() {
    aggregatedContent = new TestAggregatedContent();
  }

  @Test
  public void testInitialMaxSize() {
    assertEquals(-1, aggregatedContent.getMaxSize());
  }

  @Test
  public void testSetAndGetMaxSize() {
    aggregatedContent.setMaxSize(5);
    assertEquals(5, aggregatedContent.getMaxSize());
  }

  @Test
  public void testSetAndIsTimedOut() {
    aggregatedContent.setTimedOut(true);
    assertTrue(aggregatedContent.isTimedOut());

    aggregatedContent.setTimedOut(false);
    assertFalse(aggregatedContent.isTimedOut());
  }

  @Test
  public void testDeprecatedSetTimedOut() {
    aggregatedContent.setTimedOut(); // deprecated version
    assertTrue(aggregatedContent.isTimedOut());
  }

  @Test
  public void testSetAndGetFirstElementArrivalTime() {
    Long time = System.currentTimeMillis();
    aggregatedContent.setFirstElementArrivalTime(time);
    assertEquals(time, aggregatedContent.getFirstElementArrivalTime());
    assertEquals(time, aggregatedContent.getFirstValueArrivalTime()); // deprecated
  }

  @Test
  public void testSetAndGetLastElementArrivalTime() {
    Long time = System.currentTimeMillis();
    aggregatedContent.setLastElementArrivalTime(time);
    assertEquals(time, aggregatedContent.getLastElementArrivalTime());
    assertEquals(time, aggregatedContent.getLastValueArrivalTime()); // deprecated
  }

  @Test
  public void testAddAndGetAggregatedElements() {
    TypedValue<String> val1 = new TypedValue<>("A", null);
    TypedValue<String> val2 = new TypedValue<>("B", null);

    aggregatedContent.add(val1, 1000L);
    aggregatedContent.add(val2, 2000L);

    List<TypedValue> list = aggregatedContent.getAggregatedElements();
    assertEquals(2, list.size());
    assertEquals("A", list.get(0).getValue());
    assertEquals("B", list.get(1).getValue());
  }

  @Test
  public void testIsComplete() {
    assertFalse(aggregatedContent.isComplete());
    aggregatedContent.setComplete(true);
    assertTrue(aggregatedContent.isComplete());
  }

  private static class TestAggregatedContent extends AbstractAggregatedContent {

    private static final long serialVersionUID = 1L;

    private final List<TypedValue> elements = new ArrayList<>();
    private boolean complete = false;

    @Override
    public boolean isComplete() {
      return complete;
    }

    @Override
    public boolean upgradeIfNeeded() {
      return false;
    }

    public void setComplete(boolean complete) {
      this.complete = complete;
    }

    @Override
    public void add(TypedValue newElement, Long timeStamp) {
      elements.add(newElement);
      if (getFirstElementArrivalTime() == null) {
        setFirstElementArrivalTime(timeStamp);
      }
      setLastElementArrivalTime(timeStamp);
    }

    @Override
    public void add(TypedValue newElement, Long timestamp, int sequenceNumber) {

    }

    @Override
    public List<TypedValue> getAggregatedElements() {
      return elements;
    }
  }
}
