/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.info;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.runtime.api.metadata.TypedValue;

public class GroupAggregatorSharedInformationTest {

  private GroupAggregatorSharedInformation info;

  @Before
  public void setUp() {
    info = new GroupAggregatorSharedInformation();
  }

  @Test
  public void testDefaultContentMapIsEmpty() {
    assertNotNull(info.getContentMap());
    assertTrue(info.getContentMap().isEmpty());
  }

  @Test
  public void testDefaultRegisteredEvictionsIsEmpty() {
    assertNotNull(info.getRegisteredEvictions());
    assertTrue(info.getRegisteredEvictions().isEmpty());
  }

  @Test
  public void testDefaultRegisteredTimeoutsIsEmpty() {
    assertNotNull(info.getRegisteredTimeouts());
    assertTrue(info.getRegisteredTimeouts().isEmpty());
  }

  @Test
  public void testSetAndGetContentMap() {
    Map<String, AggregatedContent> mockMap = new HashMap<>();
    AggregatedContent mockContent = new AggregatedContent() {

      @Override
      public boolean isComplete() {
        return false;
      }

      @Override
      public boolean upgradeIfNeeded() {
        return false;
      }

      @Override
      public void add(org.mule.runtime.api.metadata.TypedValue newElement, Long timeStamp) {}

      @Override
      public void add(TypedValue newElement, Long timestamp, int sequenceNumber) {

      }

      @Override
      public java.util.List<org.mule.runtime.api.metadata.TypedValue> getAggregatedElements() {
        return null;
      }

      @Override
      public Long getFirstValueArrivalTime() {
        return 0L;
      }

      @Override
      public Long getLastValueArrivalTime() {
        return 0L;
      }
    };
    mockMap.put("key1", mockContent);
    info.setContentMap(mockMap);
    assertEquals(mockMap, info.getContentMap());
  }
}
