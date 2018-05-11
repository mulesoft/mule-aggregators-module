/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;


import static java.lang.Thread.sleep;

import org.junit.Test;

public class DocAggregatorsTestCase extends AbstractAggregatorsTestCase {


  @Override
  protected String getConfigFile() {
    return "doc-aggregators-config.xml";
  }

  @Test
  public void test() throws Exception {
    runNTimes("main", 200, 0);
  }

  @Test
  public void test2() throws Exception {
    runNTimes("main2", 200, 0);
  }

  @Test
  public void test3() throws Exception {
    runNTimes("main3", 200, 1200);
  }

  @Test
  public void test4() throws Exception {
    runNTimes("main4", 1000, 0);
  }

  private void runNTimes(String flowName, int n, long delay) throws Exception {
    for (int i = 0; i < n; i++) {
      sleep(delay);
      flowRunner(flowName).withVariable("personName",i).withVariable("personNac",i % 3).run();
    }
  }


}
