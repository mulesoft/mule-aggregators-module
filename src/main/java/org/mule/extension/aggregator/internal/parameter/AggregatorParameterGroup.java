/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.parameter;

import static org.mule.runtime.api.meta.ExpressionSupport.SUPPORTED;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Common aggregator parameters
 *
 * @since 1.0
 */
public class AggregatorParameterGroup {

  static final String UNLIMITED_TIMEOUT = "-1";

  /**
   * The expression to get the value of the content to be aggregated.
   * <p/>
   * Have in mind that once the routes are triggered, the only variables available are the ones that last
   * invocation only.
   */
  @Parameter
  @Expression(SUPPORTED)
  @Content
  @Optional(defaultValue = "#[message]")
  private TypedValue<Object> content;

  public TypedValue getContent() {
    return content;
  }

  public void setContent(TypedValue content) {
    this.content = content;
  }

}
