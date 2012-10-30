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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.MockitoAnnotations.initMocks;
import hudson.model.AbstractCIBase;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.Item;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class AbstractCiStateProviderTest {

    @Mock AbstractCIBase jenkins;

    @Before
    public void setUp() {

        initMocks(this);
    }

    @Test(expected = IllegalArgumentException.class)
    public void useNullBase() {

        new AbstractCiStateProvider(null);
    }

    @Test
    public void getNodes() {

        final Node slave = Mockito.mock(Node.class);
        usingNodes(jenkins, slave);

        final List<Node> nodes = new AbstractCiStateProvider(jenkins).getNodes();

        assertEquals(2, nodes.size());
        assertSame(slave, nodes.get(0));
        assertSame(jenkins, nodes.get(1));
    }

    private List<Node> usingNodes(
            final AbstractCIBase jenkins, final Node... slaves
    ) {

        final List<Node> slaveList = Arrays.asList(slaves);

        Mockito.when(jenkins.getNodes()).thenReturn(slaveList);

        return slaveList;
    }

    @Test
    public void getQueue() {

        final Item firstItem = Mockito.mock(Queue.Item.class);
        final Item secondItem = Mockito.mock(Queue.Item.class);

        final Queue.Item[] inItems = usingQueue(jenkins, firstItem, secondItem);

        final List<? extends Queue.Item> items = new AbstractCiStateProvider(jenkins).getQueue();

        assertEquals(inItems.length, items.size());
        assertSame(inItems[0], items.get(0));
        assertSame(inItems[1], items.get(1));
    }

    private Queue.Item[] usingQueue(AbstractCIBase jenkins, final Queue.Item... items) {

        final Queue queue = Mockito.mock(Queue.class);
        Mockito.when(jenkins.getQueue()).thenReturn(queue);

        Mockito.when(queue.getItems()).thenReturn(items);
        return items;
    }

    @Test
    public void delegateEqualsHashCode() {

        final Node slave1 = Mockito.mock(Node.class);
        final Node slave2 = Mockito.mock(Node.class);
        final Queue.Item item1 = Mockito.mock(Queue.Item.class);
        final Queue.Item item2 = Mockito.mock(Queue.Item.class);

        usingNodes(jenkins, slave1, slave2);
        usingQueue(jenkins, item1, item2);

        final StateProvider sp1 = new AbstractCiStateProvider(jenkins);
        final StateProvider sp2 = new AbstractCiStateProvider(jenkins);

        assertEquals(sp1.hashCode(), sp2.hashCode());
        assertEquals(sp1, sp2);
    }
}
