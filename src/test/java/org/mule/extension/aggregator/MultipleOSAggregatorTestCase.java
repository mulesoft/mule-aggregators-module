/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static junit.framework.TestCase.fail;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Rule;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public abstract class MultipleOSAggregatorTestCase extends AbstractAggregatorsTestCase {

  protected static final String BIG_PAYLOAD_FILE_NAME = "big_payload";

  @Rule
  public SystemProperty workingDir = new SystemProperty("workingDir", getWorkingDir());

  @Parameterized.Parameter(0)
  public String type;

  @Parameterized.Parameter(1)
  @Rule
  public SystemProperty objectStore;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"PERSISTENT", new SystemProperty("objectStore", "aggregatorsPersistentObjectStore")},
        {"IN_MEMORY", new SystemProperty("objectStore", "aggregatorsInMemoryObjectStore")}
    });
  }

  private static String getWorkingDir() {
    URI resourceUri = null;
    try {
      resourceUri = currentThread().getContextClassLoader().getResource(BIG_PAYLOAD_FILE_NAME).toURI();
    } catch (URISyntaxException e) {
      fail();
    }
    return new File(resourceUri).getParentFile().getAbsolutePath();
  }

}
