/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import org.springframework.context.annotation.Description;

public class ConfigurationTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty schedulingTasksPeriod = new SystemProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY, "500");

  @Override
  protected String getConfigFile() {
    return "aggregators-configurations-config.xml";
  }

  @Test
  @Description("An eviction time less than the configured scheduling period should raise exception")
  public void groupBasedAggregatorThrowsErrorWhenEvictionTimeIsLow() throws Exception {
    flowRunner("lowEvictionTime").run();
  }

  @Test
  @Description("A timeout time less than the configured scheduling period should raise exception")
  public void groupBasedAggregatorThrowsErrorWhenTimeoutIsLow() throws Exception {
    flowRunner("lowTimeoutInGroup").run();
  }

  @Test
  @Description("A timeout time less than the configured scheduling period should raise exception")
  public void sizeBasedAggregatorThrowsErrorWhenTimeoutIsLow() throws Exception {
    flowRunner("lowTimeoutInSize").run();
  }

  @Test
  @Description("A period less than the configured scheduling period should raise exception")
  public void timeBasedAggregatorThrowsErrorWhenPeriodIsLow() throws Exception {
    flowRunner("lowPeriod").run();
  }

  @Test
  @Description("A timeout of 0 should raise exception for size or group based aggregators")
  public void zeroTimeout() throws Exception {
    flowRunner("zeroTimeoutGroup").run();
    flowRunner("zeroTimeoutSize").run();
  }

  @Test
  @Description("A period of 0 should raise an exception for time based aggregator")
  public void zeroPeriod() throws Exception {
    flowRunner("zeroPeriod").run();
  }

  @Test
  @Description("A timeout less than 0 should raise exception for size or group based aggregators")
  public void negativeTimeout() throws Exception {
    flowRunner("negativeTimeoutGroup").run();
    flowRunner("negativeTimeoutSize").run();
  }

  @Test
  @Description("A period less than 0 should raise an exception for time based aggregator")
  public void negativePeriod() throws Exception {
    flowRunner("negativePeriod").run();
  }

  @Test
  @Description("A maxSize of 0 is not supported for Size or Time based aggregators")
  public void zeroMaxSize() throws Exception {
    flowRunner("zeroMaxSizeSizeBased").run();
    flowRunner("zeroMaxSizeTimeBased").run();
  }

  @Test
  @Description("A groupSize of 0 is not supported for GroupBasedAggrgators")
  public void zeroGroupSize() throws Exception {
    flowRunner("zeroGroupSize").run();
  }

  @Test
  @Description("A maxSize of less than 0 is not supported for Size based aggregators")
  public void negativeMaxSize() throws Exception {
    flowRunner("negativeMaxSizeSizeBased").run();
  }

  @Test
  @Description("A groupSize of less than 0 is not supported for GroupBasedAggrgators")
  public void negativeGroupSize() throws Exception {
    flowRunner("negativeGroupSize").run();
  }



}
