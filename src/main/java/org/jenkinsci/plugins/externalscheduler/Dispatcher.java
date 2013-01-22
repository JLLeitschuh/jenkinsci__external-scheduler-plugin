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
package org.jenkinsci.plugins.externalscheduler;

import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;

import java.util.logging.Logger;

/**
 * Assign jobs to particular nodes when Jenkins asks.
 *
 * @author ogondza
 */
public class Dispatcher extends QueueTaskDispatcher {

    private final static Logger LOGGER = Logger.getLogger(
            Dispatcher.class.getName()
    );

    private final ExternalPlanner planner;

    /*package*/ Dispatcher(final ExternalPlanner planner) {

        if (planner == null) throw new AssertionError("No planner");

        this.planner = planner;
    }

    public CauseOfBlockage canTake(final Node node, final BuildableItem item) {

        return assignToNode(node, item)
                ? null
                : notAssignedToNode(node, item)
        ;
    }

    private boolean assignToNode(final Node node, final BuildableItem item) {

        final NodeAssignments solution = planner.currentSolution();

        // Planner disabled
        if (solution == null) return true;

        final boolean assigned = node.getSelfLabel().toString().equals(solution.nodeName(item));

        logStatus(assigned, "assigning " + itemName(item) + " to " + node.getSelfLabel());

        return assigned;
    }

    private String itemName(final Queue.BuildableItem item) {

        return item.task.getDisplayName() + ":" + item.id;
    }

    private void logStatus(final boolean status, String message) {

        if (!status) {

            message = "not " + message;
        }

        LOGGER.info(message);
    }

    private CauseOfBlockage notAssignedToNode(final Node node, final BuildableItem item) {

        return new CauseOfBlockage() {

            @Override
            public String getShortDescription() {

                return String.format(
                        "External scheduler decided not to assign %s to %s", item.toString(), node.toString()
                );
            }
        };
    }
}