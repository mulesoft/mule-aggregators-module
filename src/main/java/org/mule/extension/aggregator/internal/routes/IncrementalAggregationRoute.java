/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.routes;

import org.mule.runtime.extension.api.runtime.route.Route;

/**
 * Aggregation route to be executed whenever a new event arrives to the aggregator but the condition for the group to be
 * complete is not yet met.
 *
 * @since 1.0
 */
public class IncrementalAggregationRoute extends Route {

}
