/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.content;

import org.mule.runtime.api.metadata.TypedValue;

import java.io.Serializable;
import java.util.List;


public interface AggregatedContent extends Serializable {

  /**
   * Add a new element to the storage
   *
   * @param newElement the element to be added
   * @param timestamp the moment when the element was added
   */
  public void add(TypedValue newElement, Long timestamp);

  /**
   * Add a new element to the storage
   *
   * @param newElement the element to be added
   * @param timestamp the moment when the element was added
   * @param sequenceNumber the position of the new element inside content being aggregated
   */
  public void add(TypedValue newElement, Long timestamp, int sequenceNumber);

  /**
   * Gets the aggregated elements from the storage
   *
   * @return all the elements in the storage
   */
  public List<TypedValue> getAggregatedElements();

  /**
   * Get a timestamp representing when the first value was aggregated
   *
   * @return a timestamp with the time of the first aggregated value
   */
  public Long getFirstValueArrivalTime();

  /**
   * Get a timestamp with the time of the last aggregated element
   *
   * @return a timestamp with the time of the last aggregated element
   */
  public Long getLastValueArrivalTime();

  /**
   * Get a boolean indicating if the number of elements is equal to the maximum allowed
   *
   * @return true if the group is complete, false otherwise
   */
  public boolean isComplete();

  // TODO: fix this AMOD-5. This should be removed in the next major release.
  /**
   * This method upgrades the sequenced elements to the new data structure for backward compatibility.
   *
   * @return true if the upgrade was made, false otherwise.
   */
  @Deprecated
  public boolean upgradeIfNeeded();

}
