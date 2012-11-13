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

import hudson.model.AbstractCIBase;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulate Jenkins state
 *
 * @author ogondza
 */
public class AbstractCiStateProvider implements StateProvider {

    private final AbstractCIBase base;

    public AbstractCiStateProvider(final AbstractCIBase base) {

        if (base == null) throw new IllegalArgumentException("Base is null");

        this.base = base;
    }

    /**
     * @return List of online Nodes
     */
    public List<Node> getNodes() {

        final List<Node> nodes = new ArrayList<Node>();

        for (final Node nodeCadidate: base.getNodes()) {

            if (nodeReady(nodeCadidate)) {

                nodes.add(nodeCadidate);
            }
        }

        if (nodeReady(base)) {

            nodes.add(base);
        }


        return nodes;
    }

    private boolean nodeReady(final Node node) {

        final Computer computer = node.toComputer();
        if (computer == null) return false;

        return !computer.isOffline() && computer.isAcceptingTasks();
    }

    /**
     * @return List of queued item to be scheduled
     */
    public List<? extends Queue.Item> getQueue() {

        return Arrays.asList(base.getQueue().getItems());
    }

    @Override
    public boolean equals(final Object rhs) {

        if (rhs == null) return false;

        if (this == rhs) return true;

        if (!this.getClass().equals(rhs.getClass())) return false;

        final AbstractCiStateProvider otherProvider = (AbstractCiStateProvider) rhs;

        if (this.base == otherProvider.base) return true;

        return getNodes().equals(otherProvider.getNodes())
                && getQueue().equals(otherProvider.getQueue())
        ;
    }

    @Override
    public int hashCode() {

        return 31 * getNodes().hashCode() + getQueue().hashCode() + 7;
    }
}
