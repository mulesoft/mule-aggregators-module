/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged.enricher;

import org.mule.extension.aggregator.internal.privileged.executor.TimeBasedAggregatorOperationsExecutor;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.property.ComponentExecutorModelProperty;


/**
 * Sets {@link TimeBasedAggregatorOperationsExecutor} as the executor of the {@code timeBasedAggregator} operation
 *
 * @since 1.0
 */
public class TimeBasedAggregatorOperationsEnricher implements DeclarationEnricher {

  public static final String OPERATION_NAME = "timeBasedAggregator";

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    extensionLoadingContext.getExtensionDeclarer().getDeclaration().getConstructs().stream()
        .filter(construct -> construct.getName().equals(OPERATION_NAME))
        .findFirst()
        .ifPresent(
                   construct -> construct
                       .addModelProperty(new ComponentExecutorModelProperty((model,
                                                                             params) -> new TimeBasedAggregatorOperationsExecutor(params))));
  }
}
