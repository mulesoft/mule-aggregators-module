/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.content;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.metadata.TypedValue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Stores the aggregated content in memory until completion.
 *
 * @since 1.0
 */
public class SimpleAggregatedContent extends AbstractAggregatedContent {

  private static final long serialVersionUID = -229638907750317297L;

  @Deprecated
  // TODO: fix this AMOD-5. This should be removed in the next major release.
  private Map<Integer, TypedValue> sequencedElements;

  @Deprecated
  // TODO: fix this AMOD-5. This should be removed in the next major release.
  private List<TypedValue> unsequencedElements;

  private Map<Index, TypedValue> indexedElements;

  private SimpleAggregatedContent() {
    this.sequencedElements = new HashMap<>();
    this.unsequencedElements = new ArrayList<>();
    this.indexedElements = new HashMap<>();
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
    indexedElements.put(lastArrivalIndex(null), newContent);
    updateTimes(timeStamp);
  }

  @Override
  public void add(TypedValue newContent, Long timeStamp, int sequenceNumber) {
    indexedElements.put(lastArrivalIndex(sequenceNumber), newContent);
    updateTimes(timeStamp);
  }

  @Override
  public List<TypedValue> getAggregatedElements() {
    if (indexedElements.isEmpty()) {
      return Collections.emptyList();
    }
    return indexedElements.entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
        .map(Map.Entry::getValue)
        .collect(toList());
  }

  public boolean isComplete() {
    return maxSize == indexedElements.size();
  }

  /**
   * This method does a custom deserialization after the default deserialization to initialise the indexed elements
   * because if a previous version was recovered this will not be initialized.
   * TODO: fix this AMOD-5. This should be removed in the next major release.
   */
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (Objects.isNull(indexedElements)) {
      indexedElements = new HashMap<>();
    }
  }

  /**
   * This method upgrades the sequenced elements to the new data structure for backward compatibility.
   * TODO: fix this AMOD-5. This should be removed in the next major release.
   */
  @Deprecated
  public void upgradeIfNeeded() {
    if (!Objects.isNull(sequencedElements) && !sequencedElements.isEmpty()) {
      for (Integer key : sequencedElements.keySet()) {
        indexedElements.put(lastArrivalIndex(key), sequencedElements.get(key));
      }
      sequencedElements.clear();
    }

    if (!Objects.isNull(unsequencedElements) && !unsequencedElements.isEmpty()) {
      for (TypedValue element : unsequencedElements) {
        indexedElements.put(lastArrivalIndex(null), element);
      }
      unsequencedElements.clear();
    }
  }

  private Index lastArrivalIndex(Integer key) {
    Index index = new Index(0, key);
    while (indexedElements.containsKey(index)) {
      index.forwardArrival();
    }
    return index;
  }

  private static class Index implements Serializable, Comparable {

    private static final long serialVersionUID = -8286760373914606346L;
    private Integer sequenceNumber = null;
    private int arrivalIndex = 0;

    public Index(int arrivalIndex, Integer sequenceNumber) {
      this.arrivalIndex = arrivalIndex;
      this.sequenceNumber = sequenceNumber;
    }

    public Integer getSequenceNumber() {
      return sequenceNumber;
    }

    public int getArrivalIndex() {
      return arrivalIndex;
    }

    /**
     * Compares this index with the specified index for order. Returns a negative integer, zero, or a positive integer
     * as this object is less than, equal to, or greater than the specified index. It is first compared by the sequence
     * number and then by the arrival number.
     *
     * @param o the index to compare to. It cannot be null.
     *
     * @return a negative integer, zero, or a positive integer as this index is less than, equal to, or greater than the
     * specified index.
     */
    @Override
    public int compareTo(Object o) {
      if (Objects.isNull(o) || !(o instanceof Index)) {
        throw new RuntimeException("The specified object is not an instance of Index");
      }
      Index otherIndex = (Index) o;
      int sequenceComparison = compareSequence(otherIndex);
      if (sequenceComparison != 0) {
        return sequenceComparison;
      }
      return Integer.compare(arrivalIndex, otherIndex.getArrivalIndex());
    }

    public void forwardArrival() {
      arrivalIndex++;
    }

    private int compareSequence(Index otherIndex) {
      if (Objects.isNull(sequenceNumber) && Objects.isNull(otherIndex.getSequenceNumber())) {
        return 0;
      }
      if (!Objects.isNull(sequenceNumber) && Objects.isNull(otherIndex.getSequenceNumber())) {
        return -1;
      }
      if (Objects.isNull(sequenceNumber) && !Objects.isNull(otherIndex.getSequenceNumber())) {
        return 1;
      }
      return sequenceNumber.compareTo(otherIndex.getSequenceNumber());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Index index = (Index) o;
      return arrivalIndex == index.arrivalIndex && Objects.equals(sequenceNumber, index.sequenceNumber);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sequenceNumber, arrivalIndex);
    }
  }
}
