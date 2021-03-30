/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.storage.info;

import java.io.Serializable;

public interface AggregatorSharedInformation extends Serializable {

  // TODO: fix this AMOD-5. This should be removed in the next major release.
  /**
   * This method upgrades the sequenced elements to the new data structure for backward compatibility.
   */
  @Deprecated
  void upgradeIfNeeded();
}
