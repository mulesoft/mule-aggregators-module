/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator;


import static java.util.stream.Collectors.toList;
import static org.codehaus.plexus.util.IOUtil.toByteArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

public class AggregatorStreamingTestCase extends MultipleOSAggregatorTestCase {

  private static final String TEST_FILE_NAME = "little_payload";
  private static final MediaType CUSTOM_MEDIA_TYPE = MediaType.create("custom", "custom");

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"common-aggregators-config.xml", "aggregators-streaming-config.xml"};
  }

  private InputStream getInputStreamPayload() {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(TEST_FILE_NAME);
  }

  private List<TypedValue> processFlowCombo(String mainFlow, String referencedFlow) throws Exception {
    flowRunner(mainFlow).withPayload(getInputStreamPayload()).withVariable("flowName", referencedFlow)
        .withMediaType(CUSTOM_MEDIA_TYPE).run();
    Event event = flowRunner(mainFlow).withPayload(getInputStreamPayload()).withVariable("flowName", referencedFlow)
        .withMediaType(CUSTOM_MEDIA_TYPE).run();
    List<TypedValue> aggregatedElements = (List<TypedValue>) event.getVariables().get("result").getValue();
    assertThat(aggregatedElements, hasSize(2));
    return aggregatedElements;
  }

  private List<TypedValue> processInputStream(String flowName) throws Exception {
    return processFlowCombo("mainFlow", flowName);
  }

  private List<TypedValue> processCursorProvider(String flowName) throws Exception {
    return processFlowCombo("aggregateFromFile", flowName);
  }

  @Test
  public void inputStreamAsMessage() throws Exception {
    final byte[] payloadBytes = toByteArray(getInputStreamPayload());
    List<TypedValue> aggregatedElements = processInputStream("aggregateMessage");
    List<Object> payloads =
        aggregatedElements.stream().map(message -> ((Message) message.getValue()).getPayload().getValue()).collect(toList());
    List<MediaType> mediaTypes = aggregatedElements.stream()
        .map(message -> ((Message) message.getValue()).getPayload().getDataType().getMediaType()).collect(toList());
    assertThat(mediaTypes, everyItem(is(equalTo(CUSTOM_MEDIA_TYPE))));
    assertThat(payloads, everyItem(is(equalTo(payloadBytes))));
  }

  @Test
  public void inputStreamAsPayload() throws Exception {
    final byte[] payloadBytes = toByteArray(getInputStreamPayload());
    List<TypedValue> aggregatedElements = processInputStream("aggregatePayload");
    List<Object> payloads = aggregatedElements.stream().map(payload -> payload.getValue()).collect(toList());
    List<MediaType> mediaTypes =
        aggregatedElements.stream().map(payload -> payload.getDataType().getMediaType()).collect(toList());
    assertThat(mediaTypes, everyItem(is(equalTo(CUSTOM_MEDIA_TYPE))));
    assertThat(payloads, everyItem(is(equalTo(payloadBytes))));
  }

  @Test
  public void cursorProviderAsMessage() throws Exception {
    final byte[] payloadBytes = toByteArray(getInputStreamPayload());
    List<TypedValue> aggregatedElements = processCursorProvider("aggregateMessage");
    List<Object> payloads =
        aggregatedElements.stream().map(message -> ((Message) message.getValue()).getPayload().getValue()).collect(toList());
    List<MediaType> mediaTypes = aggregatedElements.stream()
        .map(message -> ((Message) message.getValue()).getPayload().getDataType().getMediaType()).collect(toList());
    assertThat(mediaTypes, everyItem(is(equalTo(CUSTOM_MEDIA_TYPE))));
    assertThat(payloads, everyItem(is(equalTo(payloadBytes))));
  }

  @Test
  public void cursorProviderAsPayload() throws Exception {
    final byte[] payloadBytes = toByteArray(getInputStreamPayload());
    List<TypedValue> aggregatedElements = processCursorProvider("aggregatePayload");
    List<Object> payloads = aggregatedElements.stream().map(payload -> payload.getValue()).collect(toList());
    List<MediaType> mediaTypes =
        aggregatedElements.stream().map(payload -> payload.getDataType().getMediaType()).collect(toList());
    assertThat(mediaTypes, everyItem(is(equalTo(CUSTOM_MEDIA_TYPE))));
    assertThat(payloads, everyItem(is(equalTo(payloadBytes))));
  }

}
