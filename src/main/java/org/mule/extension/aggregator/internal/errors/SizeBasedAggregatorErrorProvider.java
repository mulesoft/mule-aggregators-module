package org.mule.extension.aggregator.internal.errors;

import static org.mule.extension.aggregator.internal.errors.GroupAggregatorError.AGGREGATOR_CONFIG;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Declares errors thrown by {@link org.mule.extension.aggregator.internal.operations.SizeBasedAggregatorOperations}
 *
 * @since 1.0
 */
public class SizeBasedAggregatorErrorProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    Set<ErrorTypeDefinition> errors = new HashSet<>();
    errors.add(AGGREGATOR_CONFIG);
    return errors;
  }
}
