/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.info;


import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.extension.aggregator.internal.task.AsyncTask;


public class SimpleAggregatorSharedInformation implements AggregatorSharedInformation {

  private static final long serialVersionUID = 2720335740399722498L;
  private AggregatedContent content;
  private AsyncTask asyncTask;

  public AggregatedContent getAggregatedContent() {
    return content;
  }

  public void setAggregatedContent(AggregatedContent content) {
    this.content = content;
  }

  public boolean shouldRegisterNextTask() {
    return asyncTask == null;
  }

  public void registerTask(AsyncTask task) {
    this.asyncTask = task;
  }

  public void unregisterTask() {
    this.asyncTask = null;
  }

  public AsyncTask getRegisteredTask() {
    return asyncTask;
  }
}
