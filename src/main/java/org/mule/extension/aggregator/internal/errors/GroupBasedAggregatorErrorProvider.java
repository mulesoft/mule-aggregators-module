/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.errors;

import static org.mule.extension.aggregator.internal.errors.AggregatorError.GROUP_COMPLETED;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.GROUP_TIMED_OUT;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.NO_GROUP_ID;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.NO_GROUP_SIZE;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Set;

/**
 * Declares errors thrown by {@link org.mule.extension.aggregator.internal.operations.GroupBasedAggregatorOperations}
 *
 * @since 1.0
 */
public class GroupBasedAggregatorErrorProvider extends AggregatorErrorProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = super.getErrorTypes();
    errors.add(GROUP_COMPLETED);
    errors.add(GROUP_TIMED_OUT);
    errors.add(NO_GROUP_ID);
    errors.add(NO_GROUP_SIZE);
    return errors;
  }
}
