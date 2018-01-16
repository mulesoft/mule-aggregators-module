/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.extension.aggregator.GroupBasedAggregatorsTestCase.GROUP_ID_VARIABLE_KEY;
import org.mule.extension.aggregator.internal.storage.info.GroupAggregatorSharedInformation;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.store.ObjectStoreManager;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;

public class CustomObjectStoreTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  ObjectStoreManager osManager;

  @Override
  protected String getConfigFile() {
    return "custom-object-store-config.xml";
  }

  @Test
  public void customObjectStore() throws Exception {
    final String defaultOSFlow = "defaultObjectStoreAggregator";
    final String defaultOSID = "defaultObjectStoreGroup";
    final String defaultPayload = "default";

    final String globalOSFlow = "globalObjectStoreAggregator";
    final String globalOS = "globalObjectStore";
    final String globalOSID = "globalObjectStoreGroup";
    final String globalPayload = "global";

    final String privateOSFlow = "privateObjectStoreAggregator";
    final String privateOS = "privateObjectStore";
    final String privateOSID = "privateObjectStoreGroup";
    final String privatePayload = "private";

    flowRunner(defaultOSFlow).withVariable(GROUP_ID_VARIABLE_KEY, defaultOSID).withPayload(defaultPayload).run();
    flowRunner(globalOSFlow).withVariable(GROUP_ID_VARIABLE_KEY, globalOSID).withPayload(globalPayload).run();
    flowRunner(privateOSFlow).withVariable(GROUP_ID_VARIABLE_KEY,privateOSID).withPayload(privatePayload).run();

    Map<String, GroupAggregatorSharedInformation> defaultInfoMap =
        (Map<String, GroupAggregatorSharedInformation>) osManager.getDefaultPartition().retrieveAll();
    Map<String, GroupAggregatorSharedInformation> globalInfoMap =
        (Map<String, GroupAggregatorSharedInformation>) osManager.getObjectStore(globalOS).retrieveAll();
    Map<String,GroupAggregatorSharedInformation> privateInfoMap = (Map<String,GroupAggregatorSharedInformation>)osManager.getObjectStore(privateOS).retrieveAll();

    assertThat(defaultInfoMap.size(), is(1));
    assertThat(globalInfoMap.size(), is(1));
    assertThat(privateInfoMap.size(), is(1));

  }


}
