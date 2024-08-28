/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.resolver;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.sdk.api.metadata.resolving.OutputStaticTypeResolver;

/**
 * An {@link OutputStaticTypeResolver} to indicate the output is in fact Void.
 * <p>
 * This is because the {@link RouterCompletionCallback} is a {@link CompletionCallback} of {@link Object} type.
 * <p>
 * We can't change to use a {@link CompletionCallback} with the right generics because of compatibility with Runtimes before 4.7.
 */
public class AggregationOutputResolver extends OutputStaticTypeResolver {

  private static final MetadataType VOID_TYPE = create(JAVA).voidType().build();

  @Override
  public String getCategoryName() {
    return "AGGREGATOR";
  }

  @Override
  public String getResolverName() {
    return "AGGREGATOR_OUTPUT";
  }

  @Override
  public MetadataType getStaticMetadata() {
    return VOID_TYPE;
  }
}
