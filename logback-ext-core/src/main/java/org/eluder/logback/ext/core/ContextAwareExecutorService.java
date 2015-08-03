package org.eluder.logback.ext.core;

/*
 * #[license]
 * logback-ext-core
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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ContextAwareExecutorService implements ExecutorService {

    private final ContextAware contextAware;

    public ContextAwareExecutorService(ContextAware contextAware) {
        this.contextAware = contextAware;
    }

    @Override
    public void shutdown() {
        contextAware.getContext().getExecutorService().shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return contextAware.getContext().getExecutorService().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return contextAware.getContext().getExecutorService().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return contextAware.getContext().getExecutorService().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return contextAware.getContext().getExecutorService().awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return contextAware.getContext().getExecutorService().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return contextAware.getContext().getExecutorService().submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return contextAware.getContext().getExecutorService().submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return contextAware.getContext().getExecutorService().invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return contextAware.getContext().getExecutorService().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return contextAware.getContext().getExecutorService().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return contextAware.getContext().getExecutorService().invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        contextAware.getContext().getExecutorService().execute(command);
    }
}
