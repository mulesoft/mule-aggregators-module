/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.task;

import static java.util.OptionalLong.empty;
import static java.util.OptionalLong.of;

import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;


public class SimpleAsyncTask implements AsyncTask {

  private static final long NOT_SCHEDULED_TIMESTAMP = -1;

  private int delay;
  private TimeUnit delayUnit;
  private boolean scheduled;
  private long schedulingTimestamp;


  public SimpleAsyncTask(int delay, TimeUnit delayUnit) {
    this.delay = delay;
    this.delayUnit = delayUnit;
    this.scheduled = false;
    this.schedulingTimestamp = NOT_SCHEDULED_TIMESTAMP;
  }

  @Override
  public int getDelay() {
    return delay;
  }

  @Override
  public TimeUnit getDelayTimeUnit() {
    return delayUnit;
  }

  @Override
  public boolean isScheduled() {
    return scheduled;
  }

  @Override
  public OptionalLong getSchedulingTimestamp() {
    return schedulingTimestamp == NOT_SCHEDULED_TIMESTAMP ? empty() : of(schedulingTimestamp);
  }

  @Override
  public void setUnscheduled() {
    scheduled = false;
  }

  @Override
  public void setScheduled(long schedulingTimestamp) {
    scheduled = true;
    this.schedulingTimestamp = schedulingTimestamp;
  }

}
