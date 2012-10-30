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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import hudson.model.Node;
import hudson.model.Queue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Queue.BuildableItem.class)
public class DispatcherTest {

    @Mock private DroolsPlanner.DescriptorImpl descriptor;
    private Queue.BuildableItem item;

    private Dispatcher dispatcher;

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        dispatcher = new Dispatcher(descriptor);

        item = PowerMockito.mock(Queue.BuildableItem.class);
        Whitebox.setInternalState(item, "id", 42);
    }

    @Test
    public void canNotRun() {

        useSolution(NodeAssignments.empty());

        assertNotNull(dispatcher.canRun(item));
    }

    @Test
    public void canNotRunItem() {

        useSolution(NodeAssignments.builder().assign(41, "slave").build());

        assertNotNull(dispatcher.canRun(item));
    }

    @Test
    public void canRunItem() {

        useSolution(NodeAssignments.builder().assign(42, "slave").build());

        assertNull(dispatcher.canRun(item));
    }

    @Test
    public void canNotTake() {

        useSolution(NodeAssignments.empty());

        assertNotNull(dispatcher.canTake(
                node("dont care"), item
        ));
    }

    @Test
    public void canNotTakeItem() {

        useSolution(NodeAssignments.builder().assign(41, "slave").build());

        assertNotNull(dispatcher.canTake(
                node("slave"), item
        ));
    }

    @Test
    public void canTakeItem() {

        useSolution(NodeAssignments.builder().assign(42, "slave").build());

        assertNull(dispatcher.canTake(
                node("slave"), item
        ));
    }

    private Node node(final String name) {

        final Node node = Mockito.mock(Node.class);
        Mockito.when(node.getNodeName()).thenReturn(name);

        return node;
    }

    private void useSolution(final NodeAssignments assignments) {

        final Planner planner = Mockito.mock(Planner.class);
        Mockito.when(descriptor.getPlanner()).thenReturn(planner);

        Mockito.when(planner.solution()).thenReturn(assignments);
    }
}
