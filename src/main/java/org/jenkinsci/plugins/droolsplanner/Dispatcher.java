/*
 * The MIT License
 *
 * Copyright (c) 2012 Red Hat, Inc.
 *
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
 */
package org.jenkinsci.plugins.droolsplanner;

import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;

/**
 * Assign jobs to particular nodes when Jenkins asks.
 *
 * @author ogondza
 */
public class Dispatcher extends QueueTaskDispatcher {

    private final DroolsPlanner.DescriptorImpl descriptor;

    /*package*/ Dispatcher(final DroolsPlanner.DescriptorImpl descriptor) {

        if (descriptor == null) throw new AssertionError("No descriptor");

        this.descriptor = descriptor;
    }

    public CauseOfBlockage canRun(final Queue.Item item) {

        return (getNodeName(item) == null)
                ? NOT_ASSIGNED
                : null
        ;
    }

    public CauseOfBlockage canTake(final Node node, final BuildableItem item) {

        return (node.getNodeName().equals(getNodeName(item)))
                ? null
                : NOT_ASSIGNED
        ;
    }

    private String getNodeName(final Queue.Item item) {

        return descriptor.getPlanner().solution().nodeName(item);
    }

    private static final CauseOfBlockage NOT_ASSIGNED = new CauseOfBlockage () {

        @Override
        public String getShortDescription() {

            return "Drools Planner decided not at assign the job to any node";
        }
    };
}