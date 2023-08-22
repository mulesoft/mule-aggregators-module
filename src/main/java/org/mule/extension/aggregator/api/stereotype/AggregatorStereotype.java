/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.api.stereotype;

import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR_DEFINITION;

import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;

import java.util.Optional;

/**
 * {@link StereotypeDefinition} for a generic {@code AbstractAggregatorOperations}.
 *
 * @since 1.1, 1.0.2
 */
public class AggregatorStereotype implements StereotypeDefinition {

  @Override
  public String getName() {
    return "AGGREGATOR";
  }

  @Override
  public Optional<StereotypeDefinition> getParent() {
    return Optional.of(PROCESSOR_DEFINITION);
  }

}
