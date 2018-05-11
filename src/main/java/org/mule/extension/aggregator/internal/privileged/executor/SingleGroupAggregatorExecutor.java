/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.executor;


import static java.lang.String.format;
import static org.mule.runtime.core.api.util.UUID.getUUID;
import org.mule.extension.aggregator.api.AggregationAttributes;
import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.extension.aggregator.internal.storage.content.SimpleAggregatedContent;
import org.mule.extension.aggregator.internal.storage.info.AggregatorSharedInformation;
import org.mule.extension.aggregator.internal.storage.info.SimpleAggregatorSharedInformation;
import org.mule.extension.aggregator.internal.task.AsyncTask;
import org.mule.extension.aggregator.internal.task.SimpleAsyncTask;
import org.mule.runtime.extension.api.exception.ModuleException;

import java.util.concurrent.TimeUnit;

/**
 * Custom abstract executor for aggregators with a single aggregation group.
 * <p/>
 * The reason why we have this custom executor is that unlike regular routers, we should be able to both, have the it
 * as void (the event out is the same as the event in) and propagate variables in case any is set inside a route.
 *
 * @since 1.0
 */
public abstract class SingleGroupAggregatorExecutor extends AbstractAggregatorExecutor {

  private int groupSize;

  void setGroupSize(int groupSize) {
    this.groupSize = groupSize;
  }

  void resetGroup() {
    getSharedInfoLocalCopy().setAggregatedContent(new SimpleAggregatedContent(groupSize));
    getSharedInfoLocalCopy().setAggregationId(getUUID());
  }

  String getAggregationId() {
    String id = getSharedInfoLocalCopy().getAggregationId();
    if (id == null) {
      id = getUUID();
      getSharedInfoLocalCopy().setAggregationId(id);
    }
    return id;
  }

  AggregationAttributes getAttributes(AggregatedContent aggregatedContent) {
    return new AggregationAttributes(getAggregationId(),
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
      task.setRegistered(getCurrentTime());
      getSharedInfoLocalCopy().registerTask(task);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(format("Registered task to be executed in %d %s", delay, unit));
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Attempted to register task but it was already registered");
      }
    }
  }

  @Override
  void doScheduleRegisteredTasks() {
    AsyncTask task = getSharedInfoLocalCopy().getRegisteredTask();
    if (task != null) {
      if (!task.isScheduled()) {
        scheduleTask(task, () -> executeSynchronized(() -> {
          onTaskExecution();
          getSharedInfoLocalCopy().unregisterTask();
        }));
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(format("Scheduled task to be executed in %d %s", task.getDelay(), task.getDelayTimeUnit()));
        }
        task.setScheduled(getCurrentTime());
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Attempted to schedule task but it was already scheduled");
        }
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
