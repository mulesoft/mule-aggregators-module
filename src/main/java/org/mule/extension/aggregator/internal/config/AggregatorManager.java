/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.config;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.extension.aggregator.internal.source.AggregatorListener;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Controls the registration of the aggregators and listeners for proper binding between them.
 *
 * @since 1.0
 */
public class AggregatorManager implements Initialisable, Disposable {

  private Map<String, AggregatorListener> registeredListeners;
  private Set<String> availableAggregators;
  private boolean initialized = false;

  @Override
  public void initialise() throws InitialisationException {
    if (!initialized) {
      registeredListeners = new HashMap<>();
      availableAggregators = new HashSet<>();
      initialized = true;
    }
  }

  @Override
  public void dispose() {
    if (initialized) {
      registeredListeners = null;
      availableAggregators = null;
      initialized = false;
    }
  }

  /**
   * Registers a new aggregator to keep track of the available ones and check valid listener registrations.
   *
   * @param aggregatorName the name of the aggregator registered
   */
  public void registerAggregator(String aggregatorName) {
    availableAggregators.add(aggregatorName);
  }

  /**
   * Registers a unique listener to an already registered aggregator
   *
   * @param aggregatorName the name of the aggregator to register to
   * @param listener the listener to be called when needed
   * @throws MuleException
   */
  public void registerListener(String aggregatorName, AggregatorListener listener) throws MuleException {
    //TODO:CHECK IF MULEEXCEPTION IS THE BEST OPTION
    //TODO:ADD THIS CHECK
    //if (!availableAggregators.contains(aggregatorName)) {
    //  throw new MuleRuntimeException(createStaticMessage("Listener is attempting to register to aggregator: %s ,but it does not exist",
    //                                                     aggregatorName));
    //}
    //if (registeredListeners.containsKey(aggregatorName)) {
    //  throw new MuleRuntimeException(createStaticMessage("Aggregator %s already has a listener", aggregatorName));
    //}
    registeredListeners.put(aggregatorName, listener);
  }

  /**
   * Get the listener registered to the aggregator with {@param aggregatorName}
   * <p/>
   * If the aggregator does not have any listener registered to it an {@link Optional#empty()} will be returned
   *
   * @return An optional with the listener registered
   */
  public Optional<AggregatorListener> getListener(String aggregatorName) {
    return ofNullable(registeredListeners.get(aggregatorName));
  }


}
