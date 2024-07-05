/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal;

import org.mule.extension.aggregator.internal.errors.AggregatorError;
import org.mule.extension.aggregator.internal.operations.GroupBasedAggregatorOperations;
import org.mule.extension.aggregator.internal.operations.SizeBasedAggregatorOperations;
import org.mule.extension.aggregator.internal.operations.TimeBasedAggregatorOperations;
import org.mule.extension.aggregator.internal.privileged.enricher.GroupBasedAggregatorOperationsEnricher;
import org.mule.extension.aggregator.internal.privileged.enricher.SizeBasedAggregatorOperationsEnricher;
import org.mule.extension.aggregator.internal.privileged.enricher.TimeBasedAggregatorOperationsEnricher;
import org.mule.extension.aggregator.internal.source.AggregatorListener;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.privileged.DeclarationEnrichers;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

import org.mule.sdk.api.annotation.JavaVersionSupport;



@Extension(name = "Aggregators")
@Operations({TimeBasedAggregatorOperations.class, SizeBasedAggregatorOperations.class, GroupBasedAggregatorOperations.class})
@Sources(AggregatorListener.class)
@ErrorTypes(AggregatorError.class)
@DeclarationEnrichers({GroupBasedAggregatorOperationsEnricher.class, SizeBasedAggregatorOperationsEnricher.class,
    TimeBasedAggregatorOperationsEnricher.class})
@Xml(prefix = "aggregators")
@JavaVersionSupport({JAVA_8, JAVA_11, JAVA_17})
public class AggregatorsExtension {

}
