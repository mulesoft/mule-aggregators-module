/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.operations;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.reference.ObjectStoreReference;

public abstract class AbstractAggregatorOperations {

  /**
   * An ObjectStore for storing shared information regarding aggregators. (Groups, GroupId, Etc)
   */
  @Parameter
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
