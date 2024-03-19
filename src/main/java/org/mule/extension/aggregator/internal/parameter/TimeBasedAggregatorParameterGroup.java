/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.parameter;


import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.TimeUnit;

/**
 * Parameters used to configure aggregators by time.
 *
 * @since 1.0
 */
public class TimeBasedAggregatorParameterGroup extends AggregatorParameterGroup {

  /**
   * Indicates the period of time for holding messages in a group before releasing them
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private int period;

  /**
   * The time unit in which the period is expressed
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "SECONDS")
  private TimeUnit periodUnit;

  public Integer getPeriod() {
    return period;
  }

  public void setPeriod(Integer period) {
    this.period = period;
  }

  public TimeUnit getPeriodUnit() {
    return periodUnit;
  }

  public void setPeriodUnit(TimeUnit periodUnit) {
    this.periodUnit = periodUnit;
  }

  public void setPeriod(int period) {
    this.period = period;
  }
}
