/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;

import static org.mule.runtime.core.api.util.UUID.getUUID;

import org.mule.extension.aggregator.internal.routes.AggregatorAttributes;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.extension.aggregator.internal.storage.content.SimpleAggregatedContent;
import org.mule.extension.aggregator.internal.storage.info.AggregatorSharedInformation;
import org.mule.extension.aggregator.internal.storage.info.SimpleAggregatorSharedInformation;
import org.mule.extension.aggregator.internal.task.AsyncTask;
import org.mule.extension.aggregator.internal.task.SimpleAsyncTask;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.util.concurrent.TimeUnit;


/**
 * Aggregator Operations for aggregators with only 1 group at a time.
 *
 * @since 1.0
 */
public abstract class SingleGroupAggregatorOperations extends AbstractAggregatorOperations {

  private static final String TASKS_ID = "tasks";

  private String groupId = getUUID();
  private int groupSize;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
  }

  void setGroupSize(int groupSize) {
    this.groupSize = groupSize;
  }

  void resetGroup() {
    getSharedInfoLocalCopy().setAggregatedContent(new SimpleAggregatedContent(groupSize));
    groupId = getUUID();
  }

  AggregatorAttributes getAttributes(AggregatedContent aggregatedContent) {
    return new AggregatorAttributes(groupId,
                                    aggregatedContent.getFirstValueArrivalTime(),
                                    aggregatedContent.getLastValueArrivalTime(),
                                    aggregatedContent.isComplete());
  }

  AggregatedContent getAggregatedContent() {
    AggregatedContent aggregatedContent = getSharedInfoLocalCopy().getAggregatedContent();
    if (aggregatedContent == null) {
      aggregatedContent = new SimpleAggregatedContent(groupSize);
      getSharedInfoLocalCopy().setAggregatedContent(aggregatedContent);
    }
    return aggregatedContent;
  }

  void registerTaskIfNeeded(int delay, TimeUnit unit) {
    if (getSharedInfoLocalCopy().shouldRegisterNextTask()) {
      AsyncTask task = new SimpleAsyncTask(delay, unit);
      getSharedInfoLocalCopy().registerTask(task);
    }
  }

  @Override
  void doScheduleRegisteredTasks() {
    AsyncTask task = getSharedInfoLocalCopy().getRegisteredTask();
      if (task != null) {
        if (!task.isScheduled()) {
          scheduleTask(task.getDelay(), task.getDelayTimeUnit(), () -> {
            onTaskExecution();
            getSharedInfoLocalCopy().unregisterTask();
          });
        }
        task.setScheduled(getCurrentTime());
      }
  }

  @Override
  void doSetRegisteredTasksAsNotScheduled() {
      AsyncTask task = getSharedInfoLocalCopy().getRegisteredTask();
      if (task != null) {
        task.setUnscheduled();
      }
  }

  abstract void onTaskExecution();

  @Override
  SimpleAggregatorSharedInformation getSharedInfoLocalCopy() throws ModuleException {
    return (SimpleAggregatorSharedInformation) super.getSharedInfoLocalCopy();
  }

  @Override
  AggregatorSharedInformation createSharedInfo() {
    return new SimpleAggregatorSharedInformation();
  }


}
