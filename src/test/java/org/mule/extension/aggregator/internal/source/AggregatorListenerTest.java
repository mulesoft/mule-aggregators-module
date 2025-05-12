/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.source;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mule.extension.aggregator.api.AggregationAttributes;
import org.mule.extension.aggregator.internal.config.AggregatorManager;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

public class AggregatorListenerTest {

  private AggregatorListener aggregatorListener;

  @Mock
  private AggregatorManager mockManager;

  @Mock
  private SourceCallback<Message, AggregationAttributes> mockCallback;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    aggregatorListener = new AggregatorListener();
  }

  @Test
  public void testDefaultValues() {
    assertNotNull(aggregatorListener.getStartLock());
    assertFalse(aggregatorListener.isIncludeTimedOutGroups());
    assertFalse(aggregatorListener.getStarted());
    assertNull(aggregatorListener.getAggregatorName());
    assertNull(aggregatorListener.getManager());
    assertNull(aggregatorListener.getSourceCallback());
  }

  @Test
  public void testSetAndGetManager() {
    aggregatorListener.setManager(mockManager);
    assertEquals(mockManager, aggregatorListener.getManager());
  }

  @Test
  public void testSetAndGetAggregatorName() {
    String name = "testAggregator";
    aggregatorListener.setAggregatorName(name);
    assertEquals(name, aggregatorListener.getAggregatorName());
  }

  @Test
  public void testSetAndGetIncludeTimedOutGroups() {
    aggregatorListener.setIncludeTimedOutGroups(true);
    assertTrue(aggregatorListener.isIncludeTimedOutGroups());

    aggregatorListener.setIncludeTimedOutGroups(false);
    assertFalse(aggregatorListener.isIncludeTimedOutGroups());
  }

  @Test
  public void testSetAndGetStarted() {
    aggregatorListener.setStarted(true);
    assertTrue(aggregatorListener.getStarted());

    aggregatorListener.setStarted(false);
    assertFalse(aggregatorListener.getStarted());
  }

  @Test
  public void testSetAndGetSourceCallback() {
    aggregatorListener.setSourceCallback(mockCallback);
    assertEquals(mockCallback, aggregatorListener.getSourceCallback());
  }
}
