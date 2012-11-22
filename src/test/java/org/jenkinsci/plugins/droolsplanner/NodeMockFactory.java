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

import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

class NodeMockFactory {

    /**
     * Use sorted set to simplify checking
     */
    public SortedSet<Node> set() {

        return new TreeSet<Node>(new Comparator<Node>() {

            public int compare(Node o1, Node o2) {

                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
    }

    public Node node(final String name, final int executors, final int freeExecutors) {

        final Computer computer = Mockito.mock(Computer.class);
        final Node node = PowerMockito.mock(Node.class);

        PowerMockito.when(node.getDisplayName()).thenReturn(name);
        PowerMockito.when(node.getSelfLabel()).thenReturn(new LabelAtom(name));
        PowerMockito.when(node.getNumExecutors()).thenReturn(executors);
        PowerMockito.when(node.toComputer()).thenReturn(computer);

        PowerMockito.when(computer.countIdle()).thenReturn(freeExecutors);

        return node;
    }
}
