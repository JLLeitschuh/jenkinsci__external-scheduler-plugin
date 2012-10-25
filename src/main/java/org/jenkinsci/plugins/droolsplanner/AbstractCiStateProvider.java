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
import hudson.model.Node;
import hudson.model.Queue;

import java.util.ArrayList;
import java.util.List;

public class AbstractCiStateProvider implements StateProvider {

    private final AbstractCIBase base;

    public AbstractCiStateProvider(final AbstractCIBase base) {

        if (base == null) throw new IllegalArgumentException("Base is null");

        this.base = base;
    }

    public List<Node> getNodes() {

        final List<Node> nodes = new ArrayList<Node>();

        nodes.addAll(base.getNodes());
        nodes.add(base);

        return nodes;
    }

    public List<? extends Queue.Item> getQueue() {

        return base.getQueue().getBuildableItems();
    }

    @Override
    public boolean equals(final Object rhs) {

        if (this == rhs) return true;

        if (rhs == null) return false;

        if (!this.getClass().equals(rhs.getClass())) return false;

        final AbstractCiStateProvider otherProvider = (AbstractCiStateProvider) rhs;

        return base.equals(otherProvider.base);
    }

    @Override
    public int hashCode() {

        return base.hashCode() * 7 + 11;
    }
}
