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

  private static final long serialVersionUID = 2720335740399722498L;
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

  // TODO: fix this AMOD-5. This should be removed in the next major release.
  /**
   * This method upgrades the sequenced elements to the new data structure for backward compatibility.
   */
  @Deprecated
  @Override
  public boolean upgradeIfNeeded() {
    if (!Objects.isNull(this.content)) {
      return content.upgradeIfNeeded();
    }
    return false;
  }

  public AggregatedContent getContent() {
    return content;
  }

  public void setContent(AggregatedContent content) {
    this.content = content;
  }

  public AsyncTask getAsyncAggregationTask() {
    return asyncAggregationTask;
  }

  public void setAsyncAggregationTask(AsyncTask asyncAggregationTask) {
    this.asyncAggregationTask = asyncAggregationTask;
  }
}
