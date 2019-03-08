/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.task;


import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * POJO to store information about tasks to be scheduled
 *
 * @since 1.0
 */
public interface AsyncTask extends Serializable {

  /**
   * Returns the time to wait until executing this task
   *
   * @return time to wait for the task to be executed
   */
  int getDelay();


  /**
   * Returns the time unit of the delay time
   *
   * @return time unit of the delay time
   */
  TimeUnit getDelayTimeUnit();

  /**
   * Sets the task as registered
   *
   * @param timestamp the timestamp of the moment when the task was registered
   */
  void setRegistered(long timestamp);

  /**
   * @return the timestamp for when the task was registered
   */
  long getRegisteringTimestamp();


  /**
   * Returns true if the task was already scheduled.
   *
   * @return a boolean if the task was scheduled for execution
   */
  boolean isScheduled();


  /**
   * Sets the task as scheduled
   */
  void setScheduled();


  /**
   * Sets the task as not scheduled
   */
  void setUnscheduled();

  /**
   * return the task id
   */
  String getId();

}
