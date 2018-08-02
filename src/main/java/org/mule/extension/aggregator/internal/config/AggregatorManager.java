/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.config;

import static java.lang.Long.parseLong;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_KEY;
import static org.mule.extension.aggregator.api.AggregatorConstants.TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_STARTED;
import org.mule.extension.aggregator.internal.source.AggregatorListener;
import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.lifecycle.PrimaryNodeLifecycleNotificationListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls the registration of the aggregators and listeners for proper binding between them.
 *
 * @since 1.0
 */
public class AggregatorManager implements Lifecycle {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());
  private static final String DEFAULT_TASK_SCHEDULING_PERIOD = "1000";

  private Map<String, AggregatorListener> registeredListeners;
  private Map<String, Runnable> availableAggregators;

  private final Object registeredAggregatorsModificationLock = new Object();

  private boolean initialized = false;
  private long taskSchedulingPeriod = parseLong(DEFAULT_TASK_SCHEDULING_PERIOD);

  private PrimaryNodeLifecycleNotificationListener notificationListener;
  //Need to add this to wait until aggregatorListeners are started before calling their callbacks
  private MuleContextNotificationListener<MuleContextNotification> contextStartListener;

  private Scheduler scheduler;

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private NotificationListenerRegistry notificationListenerRegistry;

  @Inject
  private ClusterService clusterService;

  @Inject
  private ConfigurationProperties configProperties;

  @Override
  public void initialise() throws InitialisationException {
    if (!initialized) {
      registeredListeners = new HashMap<>();
      availableAggregators = new HashMap<>();
      notificationListener = new PrimaryNodeLifecycleNotificationListener(this, notificationListenerRegistry);
      notificationListener.register();
      contextStartListener = new MuleContextNotificationListener<MuleContextNotification>() {

        @Override
        public void onNotification(MuleContextNotification notification) {
          if (clusterService.isPrimaryPollingInstance()) {
            if ("CONTEXT_STARTED".equals(notification.getAction().getIdentifier())) {
              notificationListenerRegistry.unregisterListener(this);
              contextStartListener = null;
              scheduler.scheduleAtFixedRate(AggregatorManager.this::syncAggregators, 0L, taskSchedulingPeriod, MILLISECONDS);
            }
          }
        }
      };
      notificationListenerRegistry.registerListener(contextStartListener);
      initialized = true;
    }
  }

  @Override
  public void start() throws MuleException {
    if (clusterService.isPrimaryPollingInstance()) {
      scheduler = schedulerService.cpuIntensiveScheduler();
      try {
        taskSchedulingPeriod = parseLong(configProperties.resolveStringProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY)
            .orElse(configProperties.resolveStringProperty(TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY)
                .orElse(DEFAULT_TASK_SCHEDULING_PERIOD)));
      } catch (NumberFormatException e) {
        LOGGER.warn(format("Error trying to configure %s, the value could not be parsed to a long. Using default value: %d %s",
                           TASK_SCHEDULING_PERIOD_KEY, taskSchedulingPeriod, MILLISECONDS));
      }
    }
  }

  @Override
  public void stop() throws MuleException {
    if (scheduler != null) {
      scheduler.stop();
      scheduler = null;
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

  private void syncAggregators() {
    synchronized (registeredAggregatorsModificationLock) {
      for (Runnable runnable : availableAggregators.values()) {
        runnable.run();
      }
    }
  }

  /**
   * Registers a new aggregator to keep track of the available ones and check valid listener registrations.
   *
   * @param aggregatorName the name of the aggregator registered
   */
  public void registerAggregator(String aggregatorName, Runnable synchronizingTask) {
    synchronized (registeredAggregatorsModificationLock) {
      availableAggregators.put(aggregatorName, synchronizingTask);
    }
  }

  /**
   * Registers a unique listener to an already registered aggregator
   *
   * @param aggregatorName the name of the aggregator to register to
   * @param listener the listener to be called when needed
   * @throws MuleException
   */
  public void registerListener(String aggregatorName, AggregatorListener listener) throws MuleRuntimeException {
    if (!availableAggregators.keySet().contains(aggregatorName)) {
      throw new MuleRuntimeException(createStaticMessage("Listener is attempting to register to aggregator: %s ,but it does not exist",
                                                         aggregatorName));
    }
    if (registeredListeners.containsKey(aggregatorName)) {
      throw new MuleRuntimeException(createStaticMessage("Aggregator %s already has a listener", aggregatorName));
    }
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

  /**
   * Returns the period used for scheduling registered aggregator tasks in milliseconds.
   *
   * @return the configured period in milliseconds
   */
  public long getTaskSchedulingPeriodInMillis() {
    return taskSchedulingPeriod;
  }


}
