/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.content;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.time.TimeSupplier;

import java.util.ArrayList;
import java.util.List;



/**
 * Stores the aggregated content in memory until completion.
 *
 * @since 1.0
 */
public class SimpleAggregatedContent extends AbstractAggregatedContent {

  private List<TypedValue> storage;

  private SimpleAggregatedContent() {
    this.storage = new ArrayList<>();
  }

  public SimpleAggregatedContent(int maxSize) {
    this();
    this.maxSize = maxSize;
  }

  @Override
  public void add(TypedValue newContent, Long timeStamp) {
    storage.add(newContent);
    if (firstElementArrivalTime == null) {
      firstElementArrivalTime = timeStamp;
    }
    lastElementArrivalTime = timeStamp;
  }

  @Override
  public List<TypedValue> getAggregatedElements() {
    return new ArrayList<>(this.storage);
  }

  public boolean isComplete() {
    return maxSize == storage.size();
  }

}
