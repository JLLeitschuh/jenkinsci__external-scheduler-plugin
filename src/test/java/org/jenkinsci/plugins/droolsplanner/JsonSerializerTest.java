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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONStringer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Node.class, Computer.class, Queue.BuildableItem.class})
public class JsonSerializerTest {

    private static final JsonSerializer SERIALIZER = new JsonSerializer();

    private final NodeMockFactory nodeFactory = new NodeMockFactory();

    private final List<Node> nodes = new ArrayList<Node>();

    @Test
    public void deserializeScore() {

        assertThat(-1, equalTo(SERIALIZER.extractScore(getScoreMessage(-1))));
        assertThat(0, equalTo(SERIALIZER.extractScore(getScoreMessage(0))));
        assertThat(1, equalTo(SERIALIZER.extractScore(getScoreMessage(1))));
    }

    private String getScoreMessage(final long score) {

        final String message = new JSONStringer()
            .object()
            .key("score").value(score)
            .endObject()
            .toString()
        ;

        return message;
    }

    @Test
    public void deserializeSingleItem() {

        final String json = "{\"solution\" : [ { \"id\" : 1, \"name\" : \"job@1\", \"node\" : \"vmg77-Win2k3-x86_64\" }," +
        		"{ \"id\" : 2, \"name\" : \"job@2\", \"node\" : \"not-assigned\" } ] }"
        ;

        final NodeAssignments assignments = SERIALIZER.extractAssignments(json);

        assertEquals(2, assignments.size());
        assertEquals("vmg77-Win2k3-x86_64", assignments.nodeName(1));
        assertEquals(null, assignments.nodeName(2));
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

    private List<Queue.BuildableItem> singleItem() {

        final List<Queue.BuildableItem> items = ItemMock.list();
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
                "{\"id\":4,\"priority\":50,\"inQueueSince\":5,\"name\":\"raven_eap\"," +
                "\"nodes\":[{\"name\":\"slave1\",\"executors\":7,\"freeExecutors\":7},{\"name\":\"slave2\",\"executors\":1,\"freeExecutors\":0}]" +
                ",\"assigned\":\"slave2\"}]}"
        ;

        assertEquals(json, actual);
    }

    private List<Queue.BuildableItem> severalItems() {

        final List<Queue.BuildableItem> items = ItemMock.list();

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

    private List<Queue.BuildableItem> unlabeledItem() {

        final List<Queue.BuildableItem> items = ItemMock.list();

        items.add(ItemMock.create(null, 2, "Unlabeled item", 3));

        return items;
    }

    @Test
    public void testExclusiveNode() {

        final Node regular = nodeFactory.node("regular", 1, 1);
        final Node exclusive = mock(Node.class);
        when(exclusive.canTake(any(Queue.BuildableItem.class)))
            .thenReturn(new CauseOfBlockage() {

                @Override
                public String getShortDescription() {
                    return "COB";
                }
            })
        ;

        nodes.add(regular);
        nodes.add(exclusive);

        final Queue.BuildableItem item = ItemMock.create(
                new HashSet<Node>(nodes), 42, "item", 1
        );

        final JSONArray nodes = this
                .serialize(Arrays.asList(item), this.nodes, NodeAssignments.empty())
                .getJSONObject(0)
                .getJSONArray("nodes")
        ;

        assertEquals(1, nodes.size());

        final String nodeName = nodes.getJSONObject(0).getString("name");
        assertEquals("regular", nodeName);
    }

    @Test
    public void testUnassignOfflineNode() {

        final NodeAssignments solution = NodeAssignments.builder()
                .assign(1, "offline")
                .build()
        ;

        final Node offline = nodeFactory.node("offline", 1, 1);

        when(offline.toComputer().isOffline()).thenReturn(true);

        nodes.add(offline);

        final Queue.BuildableItem item = ItemMock.create(
                new HashSet<Node>(nodes), 1, "item", 1
        );

        final JSONObject assignedTo = serialize(Arrays.asList(item), this.nodes, solution)
                .getJSONObject(0)
                .getJSONObject("assigned")
        ;

        final String nodeName = assignedTo.isNullObject() ? null : assignedTo.toString();

        assertNull("Node assigned to " + nodeName, nodeName);
    }

    private JSONArray serialize(
            final List<Queue.BuildableItem> items,
            final List<Node> nodes,
            final NodeAssignments solution
    ) {

        final String actual = SERIALIZER.buildQuery(
                new StateProviderMock(items, nodes),
                solution
        );

        return JSONObject.fromObject(actual).getJSONArray("queue");
    }
}
