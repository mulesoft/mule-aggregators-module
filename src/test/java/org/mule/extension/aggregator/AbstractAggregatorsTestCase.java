/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;

import static java.lang.Thread.sleep;
import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY;
import static org.mule.functional.util.FlowExecutionLogger.resetLogsMap;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.tck.junit4.rule.SystemProperty;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractAggregatorsTestCase extends MuleArtifactFunctionalTestCase {

  static final String AGGREGATION_COMPLETE_ROUTE_KEY = "aggregationComplete";
  static final String INCREMENTAL_AGGREGATION_ROUTE_KEY = "incrementalAggregation";
  static final String LISTENER_ROUTE_KEY = "listenerCalled";

  private static final int TASK_SCHEDULING_PERIOD = 100;

  @Inject
  ObjectStoreManager objectStoreManager;

  @Rule
  public SystemProperty schedulingTasksPeriod =
      new SystemProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY, Integer.toString(TASK_SCHEDULING_PERIOD));

  @Before
  public void reset() {
    resetLogsMap();
  }

  protected void waitForAggregatorTask(int configuredScheduledTime) throws Exception {
    sleep(TASK_SCHEDULING_PERIOD + configuredScheduledTime);
  }

}
