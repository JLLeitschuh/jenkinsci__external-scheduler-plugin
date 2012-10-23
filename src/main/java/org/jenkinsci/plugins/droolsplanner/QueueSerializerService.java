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

import hudson.model.Node;
import hudson.model.Queue;

import java.util.List;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;


public final class QueueSerializerService {

    public NodeAssignements deserialize(final String queue) {

        final JSONArray items = JSONObject.fromObject(queue).getJSONArray("solution");

        final NodeAssignements.Builder builder = NodeAssignements.builder();
        for (final Object o: items) {

            final JSONObject item = (JSONObject) o;

            builder.assign(item.getInt("id"), item.getString("node"));
        }

        return builder.build();
    }

    public String serialize(final List<Queue.Item> queue, final NodeAssignements assignments) {

        final Serializer serializer = new Serializer(new JSONStringer(), assignments);

        return serializer.run(queue).toString();
    }

    private static final class Serializer {

        private final JSONStringer builder;
        private final NodeAssignements assignments;

        public Serializer(final JSONStringer builder, final NodeAssignements nodeAssignements) {

            this.builder = builder;
            this.assignments = nodeAssignements;
        }

        public JSONBuilder run(final List<Queue.Item> queue) {

            builder.object().key("queue").array();

            queue(queue);

            return builder.endArray().endObject();
        }

        private void queue(final List<Queue.Item> queue) {

            if (queue == null) return;

            for (final Queue.Item item : queue) {

                builder.object();

                builder.key("id").value(item.id);
                builder.key("priority").value(priority(item));
                builder.key("inQueueSince").value(item.getInQueueSince());
                builder.key("name").value(item.task.getDisplayName());

                builder.key("nodes").array();

                nodes(item);

                builder.endArray();

                builder.key("assigned").value(assignments.taskNodeName(item.id));

                builder.endObject();
            }
        }

        private int priority(final Queue.Item item) {

            final String jobName = item.task.getDisplayName().toLowerCase();

            if (jobName.contains("eap") || jobName.contains("brms") || jobName.contains("jdg")) return 70;

            return 50;
        }

        private void nodes(final Queue.Item item) {

            final Set<Node> nodes = item.getAssignedLabel().getNodes();
            for (final Node node: nodes) {

                builder.object();

                builder.key("name").value(node.getDisplayName());
                builder.key("executors").value(node.getNumExecutors());
                builder.key("freeExecutors").value(node.toComputer().countIdle());

                builder.endObject();
            }
        }
    }
}
