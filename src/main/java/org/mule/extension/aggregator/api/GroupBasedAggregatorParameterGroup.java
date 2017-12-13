/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.api;


import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.ExpressionSupport.REQUIRED;
import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.TimeUnit;

/**
 * Parameters used to configure aggregators by group
 *
 * @since 1.0
 */
public class GroupBasedAggregatorParameterGroup extends TimeoutContainingAggregatorParameterGroup {

  /**
   * An expression that determines the aggregation group unique ID. This Id will be used to determine which events
   * must be aggregated together.
   */
  @Parameter
  @Expression(REQUIRED)
  @Optional(defaultValue = "#[correlationId]")
  private String groupId;

  /**
   * The size of the expected group to aggregate. All messages with the same correlation ID must have the same groupSize.
   * If not, only the first message groupSize will be considered and a warning will be logged.
   */
  @Parameter
  @Expression(SUPPORTED)
  //TODO: fix this!
  //@Optional(defaultValue = "#[groupCorrelation.groupSize]")
  @Optional(defaultValue = "10")
  //It should be integer instead of int to allow expression to resolve to null
  private Integer groupSize;

  /**
   * The time to remember a group ID once it was completed or timed out
   * </p>
   * 0 means, don't remember. -1 remember forever
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "180")
  private int evictionTime;

  /**
   * The unit for the evictionTime attribute
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "SECONDS")
  private TimeUnit evictionTimeUnit;

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public Integer getGroupSize() {
    return groupSize;
  }

  public void setGroupSize(int groupSize) {
    this.groupSize = groupSize;
  }

  public int getEvictionTime() {
    return evictionTime;
  }

  public void setEvictionTime(int evictionTime) {
    this.evictionTime = evictionTime;
  }

  public TimeUnit getEvictionTimeUnit() {
    return evictionTimeUnit;
  }

  public void setEvictionTimeUnit(TimeUnit evictionTimeUnit) {
    this.evictionTimeUnit = evictionTimeUnit;
  }
}
