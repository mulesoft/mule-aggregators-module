/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.munit;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AggregatorTestUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(AggregatorTestUtils.class);

  private AggregatorTestUtils() {
    // Nothing to do
  }

  public static Object restartAggregator(Object payload, Object aggregatorManager, ConfigurationComponentLocator locator,
                                         String aggregatorRoute) {
    LOGGER.debug(" >> registry: " + aggregatorManager.toString());
    final Component aggregator = locator.find(Location.builderFromStringRepresentation(aggregatorRoute).build()).get();
    LOGGER.debug(" >> registry: " + aggregator.toString());

    try {
      ((Stoppable) aggregator).stop();
      ((Stoppable) aggregatorManager).stop();
      ((Disposable) aggregator).dispose();
      ((Disposable) aggregatorManager).dispose();

      ((Initialisable) aggregatorManager).initialise();
      ((Initialisable) aggregator).initialise();
      ((Startable) aggregatorManager).start();
      ((Startable) aggregator).start();
    } catch (MuleException e) {
      LOGGER.error("Error restarting aggregator " + aggregatorRoute, e);
      throw new MuleRuntimeException(e);
    }

    return payload;
  }

}
