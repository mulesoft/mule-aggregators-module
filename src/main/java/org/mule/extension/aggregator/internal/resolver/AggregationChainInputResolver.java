/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.resolver;

import org.mule.extension.aggregator.api.AggregationAttributes;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.metadata.message.api.MessageMetadataTypeBuilder;
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

import java.util.function.Function;

/**
 * A {@link ChainInputTypeResolver} which wraps the payload type into an array of the same type.
 * <p>
 * Attributes are always of {@link AggregationAttributes} type.
 */
public class AggregationChainInputResolver implements ChainInputTypeResolver {

  @Override
  public String getCategoryName() {
    return "AGGREGATOR";
  }

  @Override
  public String getResolverName() {
    return "AGGREGATOR_CHAIN_INPUT";
  }

  @Override
  public MessageMetadataType getChainInputMetadataType(ChainInputMetadataContext context) {
    MessageMetadataType messageMetadataType = context.getInputMessageMetadataType();
    MessageMetadataTypeBuilder chainMessageMetadataTypeBuilder = MessageMetadataType.builder();

    chainMessageMetadataTypeBuilder.attributes(context.getTypeLoader().load(AggregationAttributes.class));
    messageMetadataType.getPayloadType()
        .map(getAggregatorMapper(context))
        .ifPresent(chainMessageMetadataTypeBuilder::payload);

    return chainMessageMetadataTypeBuilder.build();
  }

  private Function<MetadataType, ArrayType> getAggregatorMapper(ChainInputMetadataContext context) {
    return recordType -> context.getTypeBuilder().arrayType().of(recordType).build();
  }
}
