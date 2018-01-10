package org.mule.extension.aggregator;


import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

public class SchedulingConfigurationTestCase extends MuleArtifactFunctionalTestCase {


  @Rule
  public SystemProperty schedulingTasksPeriod = new SystemProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY, "1000");





}
