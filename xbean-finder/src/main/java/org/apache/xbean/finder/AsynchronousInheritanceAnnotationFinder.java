/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xbean.finder;

import org.apache.xbean.finder.archive.Archive;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

// designed to trigger asynchronism from a single thread
public class AsynchronousInheritanceAnnotationFinder extends AnnotationFinder {
    private volatile ExecutorService executor = null;
    private volatile CountDownLatch subclassesLatch = null;
    private volatile CountDownLatch implementationsLatch = null;

    public AsynchronousInheritanceAnnotationFinder(final Archive archive, final boolean checkRuntimeAnnotation) {
        super(archive, checkRuntimeAnnotation);
    }

    public AsynchronousInheritanceAnnotationFinder(final Archive archive) {
        super(archive);
    }

    public void destroy() {
        executor.shutdownNow();
    }

    @Override // should be called from main thread
    public AnnotationFinder enableFindImplementations() {
        if (implementationsLatch == null) {
            enableFindSubclasses();

            implementationsLatch = new CountDownLatch(1);
            executor().submit(new Runnable() {
                public void run() {
                    try {
                        subclassesLatch.await();
                        AsynchronousInheritanceAnnotationFinder.super.enableFindImplementations();
                        implementationsLatch.countDown();
                    } catch (final InterruptedException e) {
                        // no-op
                    }
                }
            });
        }
        return this;
    }

    @Override  // should be called from main thread
    public AnnotationFinder enableFindSubclasses() {
        if (subclassesLatch == null) {
            subclassesLatch = new CountDownLatch(1);
            executor().submit(new Runnable() {
                public void run() {
                    AsynchronousInheritanceAnnotationFinder.super.enableFindSubclasses();
                    subclassesLatch.countDown();
                }
            });
        }
        return this;
    }

    @Override  // should be called from main thread
    public <T> List<Class<? extends T>> findSubclasses(final Class<T> clazz) {
        if (subclassesLatch == null) {
            enableFindSubclasses();
        }
        join(subclassesLatch);
        return super.findSubclasses(clazz);
    }

    @Override  // should be called from main thread
    public <T> List<Class<? extends T>> findImplementations(final Class<T> clazz) {
        if (implementationsLatch == null) {
            enableFindImplementations();
        }
        join(implementationsLatch);
        return super.findImplementations(clazz);
    }

    private ExecutorService executor() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
        }
        return executor;
    }

    private void join(final CountDownLatch latch) {
        if (latch != null) {
            try {
                latch.await();
            } catch (final InterruptedException e) {
                // no-op
            }
        }
    }

    protected static class DaemonThreadFactory implements ThreadFactory {
        private final String name = "xbean-finder-" + hashCode();
        private final ThreadGroup group;
        private final AtomicInteger ids = new AtomicInteger(0);

        protected DaemonThreadFactory() {
            final SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                group = securityManager.getThreadGroup();
            } else {
                group = Thread.currentThread().getThreadGroup();
            }
        }

        // @Override
        public Thread newThread(final Runnable runnable) {
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(AnnotationFinder.class.getClassLoader());
            try {
                final Thread thread = new Thread(group, runnable, name + " - " + ids.incrementAndGet());
                if (!thread.isDaemon()) {
                    thread.setDaemon(true);
                }
                if (thread.getPriority() != Thread.NORM_PRIORITY) {
                    thread.setPriority(Thread.NORM_PRIORITY);
                }
                return thread;
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
        }
    }
}
