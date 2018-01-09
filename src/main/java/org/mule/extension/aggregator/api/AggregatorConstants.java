/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.api;

import static org.mule.runtime.core.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

/**
 * Information related to aggregators
 *
 * @since 1.0
 */
public class AggregatorConstants {

  public static final String TASK_SCHEDULING_PERIOD_SYSTEM_PROPERTY_KEY = SYSTEM_PROPERTY_PREFIX + "schedulingDelay";

}
