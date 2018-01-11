/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged;

import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.property.ComponentExecutorModelProperty;

public class SizeBasedAggregatorOperationsEnricher implements DeclarationEnricher {

  public static final String OPERATION_NAME = "all3";

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    extensionLoadingContext.getExtensionDeclarer().getDeclaration().getOperations().stream()
        .filter(operation -> operation.getName().equals(OPERATION_NAME))
        .findFirst()
        .ifPresent(
                   operation -> operation
                       .addModelProperty(new ComponentExecutorModelProperty((model,
                                                                             params) -> new GroupBasedAggregatorOperationsExecutor())));

  }
}
