/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.errors;


import static org.mule.extension.aggregator.internal.errors.AggregatorError.AGGREGATOR_CONFIG;
import static org.mule.extension.aggregator.internal.errors.AggregatorError.OBJECT_STORE_ACCESS;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

public class AggregatorErrorProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = new HashSet<>();
    errors.add(AGGREGATOR_CONFIG);
    errors.add(OBJECT_STORE_ACCESS);
    return errors;
  }
}
