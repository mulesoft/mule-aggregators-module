/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.aggregator.internal.privileged;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;

public class CompletionCallbackWrapper {

  private CompletionCallback delegate;
  private CoreEvent event;

  public CompletionCallbackWrapper(CompletionCallback delegate, CoreEvent event) {
    this.delegate = delegate;
    this.event = event;
  }

  public void success(Result<Object, Object> result) {
    if (result instanceof EventedResult) {
      delegate.success(result.copy().output(event.getMessage().getPayload()).build());
    } else {
      delegate.success(EventedResult.from(event));
    }

  }

  public void error(Throwable e) {
    delegate.error(e);
  }
}
