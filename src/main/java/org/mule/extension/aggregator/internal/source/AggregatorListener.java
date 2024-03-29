/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.source;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

import org.mule.extension.aggregator.api.AggregationAttributes;
import org.mule.extension.aggregator.api.stereotype.AggregatorStereotype;
import org.mule.extension.aggregator.internal.config.AggregatorManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import javax.inject.Inject;

/**
 * Source for listening to events triggered by aggregator
 *
 * @since 1.0
 */
public class AggregatorListener extends Source<Message, AggregationAttributes> {

  private final Object startLock = new Object();

  @Inject
  private AggregatorManager manager;

  @Parameter
  @Expression(NOT_SUPPORTED)
  @AllowedStereotypes({AggregatorStereotype.class})
  private String aggregatorName;

  @Parameter
  @Expression(NOT_SUPPORTED)
  @Optional(defaultValue = "false")
  private boolean includeTimedOutGroups;

  private Boolean started = false;
  private SourceCallback<Message, AggregationAttributes> sourceCallback;

  public Object getStartLock() {
    return startLock;
  }

  public AggregatorManager getManager() {
    return manager;
  }

  public void setManager(AggregatorManager manager) {
    this.manager = manager;
  }

  public String getAggregatorName() {
    return aggregatorName;
  }

  public void setAggregatorName(String aggregatorName) {
    this.aggregatorName = aggregatorName;
  }

  public boolean isIncludeTimedOutGroups() {
    return includeTimedOutGroups;
  }

  public void setIncludeTimedOutGroups(boolean includeTimedOutGroups) {
    this.includeTimedOutGroups = includeTimedOutGroups;
  }

  public Boolean getStarted() {
    return started;
  }

  public void setStarted(Boolean started) {
    this.started = started;
  }

  public SourceCallback<Message, AggregationAttributes> getSourceCallback() {
    return sourceCallback;
  }

  public void setSourceCallback(SourceCallback<Message, AggregationAttributes> sourceCallback) {
    this.sourceCallback = sourceCallback;
  }

  @Override
  public void onStart(SourceCallback<Message, AggregationAttributes> sourceCallback) throws MuleException {
    synchronized (startLock) {
      this.sourceCallback = sourceCallback;
      manager.registerListener(aggregatorName, this);
      started = true;
    }
  }

  @Override
  public void onStop() {
    synchronized (startLock) {
      started = false;
      manager.unregisterListener(aggregatorName, this);
    }
  }

  public boolean isStarted() {
    synchronized (startLock) {
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
