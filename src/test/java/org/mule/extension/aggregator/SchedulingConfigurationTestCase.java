/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;


import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class SchedulingConfigurationTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty schedulingTasksPeriod = new SystemProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY, "500");

  @Override
  protected String getConfigFile() {
    return "scheduling-config.xml";
  }

  @Test
  public void groupBasedAggregatorThrowsErrorWhenEvictionTimeIsLow() throws Exception {
    flowRunner("lowEvictionTime").run();
  }

  @Test
  public void groupBasedAggregatorThrowsErrorWhenTimeoutIsLow() throws Exception {
    flowRunner("lowTimeoutInGroup").run();
  }

  @Test
  public void sizeBasedAggregatorThrowsErrorWhenTimeoutIsLow() throws Exception {
    flowRunner("lowTimeoutInSize").run();
  }

  @Test
  public void timeBasedAggregatorThrowsErrorWhenPeriodIsLow() throws Exception {
    flowRunner("lowPeriod").run();
  }

}
