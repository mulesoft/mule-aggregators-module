/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.task;


import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public interface AsyncTask extends Serializable {

  /**
   * Returns the time to wait until executing this task
   *
   * @return time to wait for the task to be executed
   */
  public int getDelay();


  /**
   * Returns the time unit of the delay time
   *
   * @return time unit of the delay time
   */
  public TimeUnit getDelayTimeUnit();


  /**
   * Returns true if the task was already scheduled.
   *
   * @return a boolean if the task was scheduled for execution
   */
  public boolean isScheduled();


  public void setScheduled(long timestamp);


  /**
   * Returns a value representing the time the task was schedules. If {@code isScheduled()} is false,
   * -1 will be returned
   *
   * @return the time for the scheduled task.
   */
  public long getSchedulingTimestamp();

}
