/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.errors;

import static java.util.Collections.unmodifiableSet;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.GROUP_COMPLETED;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.GROUP_TIMED_OUT;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.NO_GROUP_ID;
import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.NO_GROUP_SIZE;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Declares errors thrown by {@link org.mule.extension.aggregator.internal.operations.GroupBasedAggregatorOperations}
 *
 * @since 1.0
 */
public class GroupBasedAggregatorErrorProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = new HashSet<>();
    errors.add(GROUP_COMPLETED);
    errors.add(GROUP_TIMED_OUT);
    errors.add(NO_GROUP_ID);
    errors.add(NO_GROUP_SIZE);
    return errors;
  }
}
