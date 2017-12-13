/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.info;


import org.mule.extension.aggregator.internal.storage.content.AggregatedContent;
import org.mule.extension.aggregator.internal.task.AsyncTask;

import java.util.HashMap;
import java.util.Map;

public class GroupAggregatorSharedInformation implements AggregatorSharedInformation {

  private Map<String, AggregatedContent> contentMap = new HashMap<>();
  private Map<String, AsyncTask> registeredEvictions = new HashMap<>();
  private Map<String, AsyncTask> registeredTimeouts = new HashMap<>();

  public AggregatedContent getAggregatedContent(String groupId) {
    return contentMap.get(groupId);
  }

  public void setAggregatedContent(String groupId, AggregatedContent content) {
    contentMap.put(groupId, content);
  }

  public void removeAggregatedContent(String groupId) {
    contentMap.remove(groupId);
  }

  public boolean shouldRegisterEviction(String groupId) {
    return registeredEvictions.get(groupId) == null;
  }

  public void registerGroupEvictionTask(String groupId, AsyncTask groupEvictionTask) {
    registeredEvictions.put(groupId, groupEvictionTask);
  }

  public void unregisterGroupEvictionTask(String groupId) {
    registeredEvictions.remove(groupId);
  }

  public Map<String, AsyncTask> getRegisteredGroupEvictionTasks() {
    return registeredEvictions;
  }

  public boolean shouldRegisterTimeout(String groupId) {
    return registeredTimeouts.get(groupId) == null;
  }

  public void registerTimeoutTask(String groupId, AsyncTask timeoutTask) {
    registeredTimeouts.put(groupId, timeoutTask);
  }

  public void unregisterTimeoutTask(String groupId) {
    registeredTimeouts.remove(groupId);
  }

  public Map<String, AsyncTask> getRegisteredTimeoutTasks() {
    return registeredTimeouts;
  }
}
