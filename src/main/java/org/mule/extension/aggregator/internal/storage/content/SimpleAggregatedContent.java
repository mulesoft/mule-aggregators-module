/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.content;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Stores the aggregated content in memory until completion.
 *
 * @since 1.0
 */
public class SimpleAggregatedContent extends AbstractAggregatedContent {

  private static final long serialVersionUID = -229638907750317297L;
  private Map<Integer, List<TypedValue>> sequencedElements;
  private List<TypedValue> unsequencedElements;

  private SimpleAggregatedContent() {
    this.sequencedElements = new HashMap<>();
    this.unsequencedElements = new ArrayList<>();
  }

  public SimpleAggregatedContent(int maxSize) {
    this();
    this.maxSize = maxSize;
  }

  private void updateTimes(Long timeStamp) {
    if (firstElementArrivalTime == null) {
      firstElementArrivalTime = timeStamp;
    }
    lastElementArrivalTime = timeStamp;
  }

  @Override
  public void add(TypedValue newContent, Long timeStamp) {
    unsequencedElements.add(newContent);
    updateTimes(timeStamp);
  }

  @Override
  public void add(TypedValue newContent, Long timeStamp, int sequenceNumber) {
    if (sequencedElements.containsKey(sequenceNumber)) {
      sequencedElements.get(sequenceNumber).add(newContent);
    } else {
      List<TypedValue> newList = new ArrayList<>();
      newList.add(newContent);
      sequencedElements.put(sequenceNumber, newList);
    }

    //sequencedElements.put(sequenceNumber, newContent);
    updateTimes(timeStamp);
  }

  @Override
  public List<TypedValue> getAggregatedElements() {
    List<TypedValue> orderedElements = new ArrayList<>();
    if (sequencedElements.size() > 0) {
      orderedElements = sequencedElements.entrySet().stream().sorted(comparingInt(Map.Entry::getKey))
          .flatMap(val -> val.getValue().stream())
          .collect(toList());
    }
    orderedElements.addAll(unsequencedElements);
    return orderedElements;
  }


  public boolean isComplete() {
    return maxSize == sequencedElements.size() + unsequencedElements.size();
  }

  private static class Index implements Serializable {

    private static final long serialVersionUID = -8286760373914606346L;
    private Integer sequenceNumber = null;
    private int arrivalIndex = 0;

    public Index(int arrivalIndex) {
      this.arrivalIndex = arrivalIndex;
    }

    public Index(int arrivalIndex, int sequenceNumber) {
      this(arrivalIndex);
      this.sequenceNumber = sequenceNumber;
    }

    public Integer getSequenceNumber() {
      return sequenceNumber;
    }

    public int getArrivalIndex() {
      return arrivalIndex;
    }
  }

}
