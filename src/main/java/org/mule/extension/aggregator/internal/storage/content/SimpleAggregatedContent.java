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
import java.util.Collections;
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

  @Deprecated
  // TODO: fix this AMOD-5. This should be removed in the next major release.
  private Map<Integer, TypedValue> sequencedElements;

  @Deprecated
  // TODO: fix this AMOD-5. This should be removed in the next major release.
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
    SequencedElement sequencedElement;
    if (sequencedElements.containsKey(sequenceNumber)) {
      sequencedElement = (SequencedElement) sequencedElements.get(sequenceNumber).getValue();
    } else {
      sequencedElement = new SequencedElement();
    }
    sequencedElement.add(newContent);
    sequencedElements.put(sequenceNumber, TypedValue.of(sequencedElement));

    updateTimes(timeStamp);
  }

  @Override
  public List<TypedValue> getAggregatedElements() {
    List<TypedValue> orderedElements = new ArrayList<>();
    if (sequencedElements.size() > 0) {
      orderedElements = sequencedElements.entrySet().stream().sorted(comparingInt(Map.Entry::getKey))
          .flatMap(val -> ((SequencedElement) val.getValue().getValue()).get().stream())
          .collect(toList());
    }
    orderedElements.addAll(unsequencedElements);
    return orderedElements;
  }

  public boolean isComplete() {
    Integer sequencedElementsSize = sequencedElements.values().stream().map(v -> ((SequencedElement) v.getValue()).size())
        .reduce(0, Integer::sum);
    return maxSize == unsequencedElements.size() + sequencedElementsSize;
  }

  /**
   * This method upgrades the sequenced elements to the new data structure for backward compatibility.
   * TODO: fix this AMOD-5. This should be removed in the next major release.
   */
  @Deprecated
  public void upgradeIfNeeded() {
    if (sequencedElements.isEmpty()) {
      return;
    }

    // TODO: fix this AMOD-5. For sequenced elements use the class Index instead of using the SequencedElement class to
    //  wrap list inside TypeValues.
    boolean isUpgraded = sequencedElements.values().stream().anyMatch(v -> v.getValue() instanceof SequencedElement);
    if (!isUpgraded) {
      Map<Integer, TypedValue> indexedElements = new HashMap<>();
      for (Integer key : sequencedElements.keySet()) {
        SequencedElement sequencedElement = new SequencedElement();
        sequencedElement.add(sequencedElements.get(key));
        indexedElements.put(key, TypedValue.of(sequencedElement));
      }
      sequencedElements.clear();
      sequencedElements = indexedElements;
    }
  }

  /**
   * Inner class to save a list instead of a single element when aggregating elements by sequence number.
   * TODO: fix this AMOD-5. This should be removed in the next major release.
   */
  @Deprecated
  private static class SequencedElement implements Serializable {

    private static final long serialVersionUID = -5278813073775940619L;
    private List<TypedValue> aggregatedElements;

    SequencedElement() {
      aggregatedElements = new ArrayList<>();
    }

    void add(TypedValue value) {
      aggregatedElements.add(value);
    }

    List<TypedValue> get() {
      return Collections.unmodifiableList(aggregatedElements);
    }

    int size() {
      return aggregatedElements.size();
    }
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
