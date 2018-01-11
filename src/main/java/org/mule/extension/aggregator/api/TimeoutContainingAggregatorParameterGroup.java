/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.api;

import static java.lang.Integer.parseInt;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.TimeUnit;

/**
 * Extends {@code AggregatorParameterGroup adding timeout parameter}
 */
public class TimeoutContainingAggregatorParameterGroup extends AggregatorParameterGroup {

  /**
   * Maximum time to wait to complete a group.
   * <p/>
   * A value of 0 is not supported since the group would be timing out constantly
   * <p/>
   * The default value of -1 means wait forever
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = UNLIMITED_TIMEOUT)
  private Integer timeout;

  /**
   * The time unit in which the timeout is expressed.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "SECONDS")
  private TimeUnit timeoutUnit;

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  public TimeUnit getTimeoutUnit() {
    return timeoutUnit;
  }

  public void setTimeoutUnit(TimeUnit timeoutUnit) {
    this.timeoutUnit = timeoutUnit;
  }

  public boolean isTimeoutSet() {
    return timeout != parseInt(UNLIMITED_TIMEOUT);
  }

}
