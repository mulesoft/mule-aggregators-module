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

public class TimeBasedAggregatorOprationsEnricher implements DeclarationEnricher {

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
