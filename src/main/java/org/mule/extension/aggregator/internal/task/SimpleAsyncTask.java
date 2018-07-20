/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.task;

import java.util.concurrent.TimeUnit;


public class SimpleAsyncTask implements AsyncTask {

  private static final long serialVersionUID = 7509203629409368845L;

  private int delay;
  private TimeUnit delayUnit;
  private boolean scheduled;
  private long registeringTimestamp;


  public SimpleAsyncTask(int delay, TimeUnit delayUnit) {
    this.delay = delay;
    this.delayUnit = delayUnit;
    this.scheduled = false;
  }

  @Override
  public void setRegistered(long timestamp) {
    this.registeringTimestamp = timestamp;
  }

  @Override
  public long getRegisteringTimestamp() {
    return registeringTimestamp;
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
  public void setUnscheduled() {
    scheduled = false;
  }

  @Override
  public void setScheduled() {
    scheduled = true;
  }

}
