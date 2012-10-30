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
import hudson.model.Node;
import hudson.model.Queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Node.class)
public class QueueSerializerServiceTest {

    private static final JsonSerializer SERIALIZER = new JsonSerializer();

    private final NodeMockFactory nodeFactory = new NodeMockFactory();

    private final List<Node> nodes = new ArrayList<Node>();

    @Test
    public void deserializeSingleItem() {

        final String json = "{\"solution\" : [ { \"id\" : 1, \"name\" : \"job@1\", \"node\" : \"vmg77-Win2k3-x86_64\" }," +
        		"{ \"id\" : 2, \"name\" : \"job@2\", \"node\" : \"not-assigned\" } ] }"
        ;

        final NodeAssignments assignments = SERIALIZER.extractAssignments(json);

        assertEquals(2, assignments.size());
        assertEquals("vmg77-Win2k3-x86_64", assignments.nodeName(1));
        assertEquals("not-assigned", assignments.nodeName(2));
    }

    @Test
    public void serializeSingleItem() {

        final String actual = SERIALIZER.buildQuery(
                new StateProviderMock(singleItem(), nodes),
                NodeAssignments.empty()
        );

        final String json = "{\"queue\":[{\"id\":2,\"priority\":50,\"inQueueSince\":3,\"name\":\"Single queue item\"," +
    		    "\"nodes\":[{\"name\":\"master\",\"executors\":2,\"freeExecutors\":1}],\"assigned\":null}]}"
        ;

        assertEquals(json, actual);
    }

    private List<Queue.Item> singleItem() {

        final List<Queue.Item> items = new ArrayList<Queue.Item>(1);
        final Set<Node> nodes = nodeFactory.set();

        nodes.add(nodeFactory.node("master", 2, 1));

        items.add(ItemMock.create(nodes, 2, "Single queue item", 3));

        return items;
    }

    @Test
    public void serializeSeveralItems() {

        final String actual = SERIALIZER.buildQuery(
                new StateProviderMock(severalItems(), nodes),
                NodeAssignments.builder()
                        .assign(4, "slave2")
                        .build()
        );

        final String json = "{\"queue\":[{\"id\":2,\"priority\":50,\"inQueueSince\":3,\"name\":\"Single queue item\"," +
                "\"nodes\":[{\"name\":\"master\",\"executors\":2,\"freeExecutors\":1}],\"assigned\":null}," +
                "{\"id\":4,\"priority\":70,\"inQueueSince\":5,\"name\":\"raven_eap\"," +
                "\"nodes\":[{\"name\":\"slave1\",\"executors\":7,\"freeExecutors\":7},{\"name\":\"slave2\",\"executors\":1,\"freeExecutors\":0}]" +
                ",\"assigned\":\"slave2\"}]}"
        ;

        assertEquals(json, actual);
    }

    private List<Queue.Item> severalItems() {

        final List<Queue.Item> items = new ArrayList<Queue.Item>(1);

        SortedSet<Node> nodes = nodeFactory.set();

        nodes.add(nodeFactory.node("master", 2, 1));

        items.add(ItemMock.create(nodes, 2, "Single queue item", 3));

        nodes = nodeFactory.set();

        nodes.add(nodeFactory.node("slave1", 7, 7));
        nodes.add(nodeFactory.node("slave2", 1, 0));

        items.add(ItemMock.create(nodes, 4, "raven_eap", 5));

        return items;
    }

    @Test
    public void serializeUnlabeledItem() {

        nodes.add(nodeFactory.node("slave_2:1", 2, 1));
        nodes.add(nodeFactory.node("slave_1:2", 1, 2));

        final String actual = SERIALIZER.buildQuery(
                new StateProviderMock(unlabeledItem(), nodes),
                NodeAssignments.empty()
        );

        final String json = "{\"queue\":[{\"id\":2,\"priority\":50,\"inQueueSince\":3,\"name\":\"Unlabeled item\"," +
                "\"nodes\":[{\"name\":\"slave_2:1\",\"executors\":2,\"freeExecutors\":1},{\"name\":\"slave_1:2\",\"executors\":1,\"freeExecutors\":2}],\"assigned\":null}]}"
        ;

        assertEquals(json, actual);
    }

    private List<Queue.Item> unlabeledItem() {

        final List<Queue.Item> items = new ArrayList<Queue.Item>(1);

        items.add(ItemMock.create(null, 2, "Unlabeled item", 3));

        return items;
    }
}
