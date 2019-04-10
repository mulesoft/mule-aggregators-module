/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.extension.aggregator.api.stereotype.AggregatorStereotype;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.annotation.param.reference.ObjectStoreReference;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;

@Stereotype(AggregatorStereotype.class)
public abstract class AbstractAggregatorOperations {

  /**
   * An ObjectStore for storing shared information regarding aggregators. (Groups, GroupId, Etc)
   */
  @Parameter
  @Placement(tab = ADVANCED_TAB)
  @DisplayName(value = "Object Store (*)")
  @Summary(
      value = "In order to create an Object Store configuration, an Object Store implementation needs to be present in the project. An option is to add the Mule Object Store Module provided by MuleSoft")
  @Expression(NOT_SUPPORTED)
  @Optional
  @ObjectStoreReference
  private ObjectStore objectStore;


  /**
   * A name for the aggregator to be referenced later.
   */
  @Parameter
  @Expression(NOT_SUPPORTED)
  private String name;

}
