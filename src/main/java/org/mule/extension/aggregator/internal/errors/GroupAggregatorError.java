/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.errors;

import static java.util.Optional.ofNullable;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Optional;

/**
 * Errors for Aggregators
 *
 * @since 1.0
 */
public enum GroupAggregatorError implements ErrorTypeDefinition<GroupAggregatorError> {

  AGGREGATOR_CONFIG, GROUP_NOT_EVICTED, NO_GROUP_ID(AGGREGATOR_CONFIG), NO_GROUP_SIZE(AGGREGATOR_CONFIG), GROUP_COMPLETED(
      GROUP_NOT_EVICTED), GROUP_TIMED_OUT(GROUP_NOT_EVICTED);

  private ErrorTypeDefinition<? extends Enum<?>> parent;

  GroupAggregatorError(ErrorTypeDefinition<? extends Enum<?>> parent) {
    this.parent = parent;
  }

  GroupAggregatorError() {

  }

  @Override
  public Optional<ErrorTypeDefinition<? extends Enum<?>>> getParent() {
    return ofNullable(parent);
  }
}
