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

import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Node.Mode;
import hudson.model.Queue;
import hudson.model.labels.LabelAtom;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

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

        final Computer computer = mock(Computer.class);
        final Node node = mock(Node.class);

        when(node.getDisplayName()).thenReturn(name);
        when(node.getSelfLabel()).thenReturn(new LabelAtom(name));
        when(node.getNumExecutors()).thenReturn(executors);
        when(node.toComputer()).thenReturn(computer);
        when(node.canTake(any(Queue.BuildableItem.class))).thenReturn(null);
        when(node.getMode()).thenReturn(Mode.NORMAL);

        when(computer.countIdle()).thenReturn(freeExecutors);
        when(computer.isOffline()).thenReturn(false);
        when(computer.isOnline()).thenCallRealMethod();
        when(computer.isAcceptingTasks()).thenReturn(true);

        return node;
    }
}
