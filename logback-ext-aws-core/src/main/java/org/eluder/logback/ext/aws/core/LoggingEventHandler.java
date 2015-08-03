package org.eluder.logback.ext.aws.core;

/*
 * #[license]
 * logback-ext-aws-core
 * %%
 * Copyright (C) 2014 - 2015 Tapio Rautonen
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * %[license]
 */

import ch.qos.logback.core.spi.ContextAware;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.handlers.AsyncHandler;

import java.util.concurrent.CountDownLatch;

public class LoggingEventHandler<REQUEST extends AmazonWebServiceRequest, RESULT> implements AsyncHandler<REQUEST, RESULT> {

    private final ContextAware contextAware;
    private final CountDownLatch latch;
    private final String errorMessage;

    public LoggingEventHandler(ContextAware contextAware, CountDownLatch latch, String errorMessage) {
        this.contextAware = contextAware;
        this.latch = latch;
        this.errorMessage = errorMessage;
    }

    @Override
    public void onError(Exception exception) {
        contextAware.addWarn(errorMessage, exception);
        latch.countDown();
    }

    @Override
    public void onSuccess(REQUEST request, RESULT result) {
        latch.countDown();
    }
}
