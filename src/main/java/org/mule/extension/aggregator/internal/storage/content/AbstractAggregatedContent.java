/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.content;

import org.mule.extension.aggregator.internal.util.ExcludeFromGeneratedCoverage;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.List;

public abstract class AbstractAggregatedContent implements AggregatedContent {

  private static final long serialVersionUID = 8840464071317299342L;
  int maxSize = -1;
  boolean timedOut;
  Long firstElementArrivalTime;
  Long lastElementArrivalTime;

  abstract public boolean isComplete();

  abstract public void add(TypedValue newElement, Long timeStamp);

  abstract public List<TypedValue> getAggregatedElements();

  public int getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  public boolean isTimedOut() {
    return timedOut;
  }

  @Deprecated
  @ExcludeFromGeneratedCoverage
  public void setTimedOut() {
    this.timedOut = true;
  }

  @Deprecated
  public Long getFirstValueArrivalTime() {
    return firstElementArrivalTime;
  }

  @Deprecated
  public Long getLastValueArrivalTime() {
    return lastElementArrivalTime;
  }

  public void setTimedOut(boolean timedOut) {
    this.timedOut = timedOut;
  }

  public Long getFirstElementArrivalTime() {
    return firstElementArrivalTime;
  }

  public void setFirstElementArrivalTime(Long firstElementArrivalTime) {
    this.firstElementArrivalTime = firstElementArrivalTime;
  }

  public Long getLastElementArrivalTime() {
    return lastElementArrivalTime;
  }

  public void setLastElementArrivalTime(Long lastElementArrivalTime) {
    this.lastElementArrivalTime = lastElementArrivalTime;
  }
}
