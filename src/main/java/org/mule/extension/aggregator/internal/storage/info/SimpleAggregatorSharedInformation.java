/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.info;


import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.extension.aggregator.internal.task.AsyncTask;

import java.util.Objects;


public class SimpleAggregatorSharedInformation implements AggregatorSharedInformation {

  private static final long serialVersionUID = -6875380625504335868L;
  private AggregatedContent content;
  private String AggregationId;
  private AsyncTask asyncAggregationTask;

  public AggregatedContent getAggregatedContent() {
    return content;
  }

  public void setAggregatedContent(AggregatedContent content) {
    this.content = content;
  }

  public boolean shouldRegisterNextAsyncAggregation() {
    return asyncAggregationTask == null;
  }

  public void registerAsyncAggregationTask(AsyncTask task) {
    this.asyncAggregationTask = task;
  }

  public void unregisterAsyncAggregationTask() {
    this.asyncAggregationTask = null;
  }

  public AsyncTask getRegisteredAsyncAggregationTask() {
    return asyncAggregationTask;
  }

  public String getAggregationId() {
    return AggregationId;
  }

  public void setAggregationId(String aggregationId) {
    AggregationId = aggregationId;
  }

  /**
   * This method upgrades the sequenced elements to the new data structure for backward compatibility.
   * TODO: fix this AMOD-5. This should be removed in the next major release.
   */
  @Deprecated
  @Override
  public void upgradeIfNeeded() {
    if (!Objects.isNull(this.content)) {
      content.upgradeIfNeeded();
    }
  }
}
