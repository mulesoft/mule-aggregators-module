/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.resolver;

import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_METADATA_AVAILABLE;

import org.mule.extension.aggregator.api.AggregationAttributes;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.sdk.api.metadata.MetadataContext;
import org.mule.sdk.api.metadata.resolving.AttributesStaticTypeResolver;
import org.mule.sdk.api.metadata.resolving.AttributesTypeResolver;

/**
 * An {@link AttributesTypeResolver} to indicate the attributes are of type {@link AggregationAttributes}.
 * <p>
 * This is because the {@link RouterCompletionCallback} is a {@link CompletionCallback} of {@link Object} type.
 * <p>
 * We can't change to use a {@link CompletionCallback} with the right generics because of compatibility with Runtimes before 4.7.
 * @implNote This can't be a {@link AttributesStaticTypeResolver} because of an issue of duplicate {@link ClassInformationAnnotation}.
 */
public class AggregationAttributesResolver implements AttributesTypeResolver<Object> {

  @Override
  public String getCategoryName() {
    return "AGGREGATOR";
  }

  @Override
  public String getResolverName() {
    return "AGGREGATOR_ATTRIBUTES";
  }

  @Override
  public MetadataType getAttributesType(MetadataContext metadataContext, Object key) throws MetadataResolvingException {
    MetadataType aggregationAttributesType = metadataContext.getTypeLoader().load(AggregationAttributes.class);
    MetadataType originalAttributesType = metadataContext.getRouterOutputMetadataContext()
        .orElseThrow(this::getMissingRouterOutputMetadataContextException)
        .getRouterInputMessageType().get()
        .getAttributesType().orElse(metadataContext.getTypeBuilder().voidType().build());
    return metadataContext.getTypeBuilder()
        .unionType()
        .of(aggregationAttributesType)
        .of(originalAttributesType)
        .build();
  }

  private MetadataResolvingException getMissingRouterOutputMetadataContextException() {
    return new MetadataResolvingException("Missing required propagation metadata", NO_DYNAMIC_METADATA_AVAILABLE);
  }
}
