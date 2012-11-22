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
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import hudson.model.AbstractCIBase;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.Queue.Item;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Node.class})
public class AbstractCiStateProviderTest {

    private AbstractCIBase jenkins;

    private Computer onlineComputer;
    private Computer offlineComputer;
    private Computer notAcceptingComputer;

    @Before
    public void setUp() {

        offlineComputer = mock(Computer.class);
        when(offlineComputer.isOffline()).thenReturn(true);
        when(offlineComputer.isAcceptingTasks()).thenReturn(true);

        onlineComputer = mock(Computer.class);
        when(onlineComputer.isOffline()).thenReturn(false);
        when(onlineComputer.isAcceptingTasks()).thenReturn(true);

        jenkins = mock(AbstractCIBase.class);
        when(jenkins.toComputer()).thenReturn(onlineComputer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void useNullBase() {

        new AbstractCiStateProvider(null);
    }

    @Test
    public void getOfflineJenkinsNodes() {

        when(jenkins.toComputer()).thenReturn(offlineComputer);

        final List<Node> nodes = new AbstractCiStateProvider(jenkins).getNodes();

        assertEquals(0, nodes.size());
    }

    @Test
    public void getNodes() {

        final Node slave = node();

        final Node noComputerSlave = node();
        when(noComputerSlave.toComputer()).thenReturn(null);

        final Node offlineSlave = node();
        when(offlineSlave.toComputer()).thenReturn(offlineComputer);

        final Node notAcceptingSlave = node();
        final Computer notAcceptingComputer = mock(Computer.class);
        when(notAcceptingComputer.isOffline()).thenReturn(false);
        when(notAcceptingComputer.isAcceptingTasks()).thenReturn(false);
        when(notAcceptingSlave.toComputer()).thenReturn(notAcceptingComputer);

        final Node nihilisticSlave = node();
        final Computer nihilisticComputer = mock(Computer.class);
        when(nihilisticComputer.isOffline()).thenReturn(true);
        when(nihilisticComputer.isAcceptingTasks()).thenReturn(false);
        when(nihilisticSlave.toComputer()).thenReturn(nihilisticComputer);


        usingNodes(jenkins, nihilisticSlave, offlineSlave, noComputerSlave, slave, notAcceptingSlave);

        final List<Node> nodes = new AbstractCiStateProvider(jenkins).getNodes();

        assertEquals(2, nodes.size());
        assertSame(slave, nodes.get(0));
        assertSame(jenkins, nodes.get(1));
    }

    private List<Node> usingNodes(
            final AbstractCIBase jenkins, final Node... slaves
    ) {

        final List<Node> slaveList = Arrays.asList(slaves);

        when(jenkins.getNodes()).thenReturn(slaveList);

        return slaveList;
    }

    @Test
    public void getQueue() {

        final Item firstItem = mock(Queue.Item.class);
        final Item secondItem = mock(Queue.Item.class);

        final Queue.Item[] inItems = usingQueue(jenkins, firstItem, secondItem);

        final List<? extends Queue.Item> items = new AbstractCiStateProvider(jenkins).getQueue();

        assertEquals(inItems.length, items.size());
        assertSame(inItems[0], items.get(0));
        assertSame(inItems[1], items.get(1));
    }

    private Queue.Item[] usingQueue(AbstractCIBase jenkins, final Queue.Item... items) {

        final Queue queue = mock(Queue.class);
        when(jenkins.getQueue()).thenReturn(queue);

        when(queue.getItems()).thenReturn(items);
        return items;
    }

    @Test
    public void delegateEqualsHashCode() {

        final Node slave1 = node();
        final Node slave2 = node();
        final Queue.Item item1 = mock(Queue.Item.class);
        final Queue.Item item2 = mock(Queue.Item.class);

        usingNodes(jenkins, slave1, slave2);
        usingQueue(jenkins, item1, item2);

        final StateProvider sp1 = new AbstractCiStateProvider(jenkins);
        final StateProvider sp2 = new AbstractCiStateProvider(jenkins);

        assertEquals(sp1.hashCode(), sp2.hashCode());
        assertEquals(sp1, sp2);
    }

    private Node node() {

        final Node node = mock(Node.class);

        when(node.toComputer()).thenReturn(onlineComputer);

        return node;
    }
}
