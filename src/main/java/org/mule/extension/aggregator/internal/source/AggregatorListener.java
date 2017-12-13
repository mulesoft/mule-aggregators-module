/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.source;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.aggregator.internal.routes.AggregatorAttributes;
import org.mule.extension.aggregator.internal.config.AggregatorManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import javax.inject.Inject;

/**
 * Source for listening to events triggered by aggregator
 *
 * @since 1.0
 */
public class AggregatorListener extends Source<Message, AggregatorAttributes> {

  @Inject
  private AggregatorManager manager;

  @Parameter
  @Expression(NOT_SUPPORTED)
  private String aggregatorName;

  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "false")
  private boolean includeTimedOutGroups;

  private Boolean started = false;
  private SourceCallback<Message, AggregatorAttributes> sourceCallback;

  @Override
  public void onStart(SourceCallback<Message, AggregatorAttributes> sourceCallback) throws MuleException {
    synchronized (started) {
      this.sourceCallback = sourceCallback;
      manager.registerListener(aggregatorName, this);
      started = true;
    }
  }

  @Override
  public void onStop() {
    synchronized (started) {
      started = false;
    }
  }

  public boolean isStarted() {
    synchronized (started) {
      return started;
    }
  }

  public SourceCallback getCallback() {
    return sourceCallback;
  }

  public boolean shouldIncludeTimedOutGroups() {
    return includeTimedOutGroups;
  }



}
