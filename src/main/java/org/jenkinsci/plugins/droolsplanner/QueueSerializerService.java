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

import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

public final class QueueSerializerService {

    public int getScore(final String score) {

        return JSONObject.fromObject(score).getInt("score");
    }

    public NodeAssignments deserialize(final String solution) {

        final JSONArray items = JSONObject.fromObject(solution).getJSONArray("solution");

        final NodeAssignments.Builder builder = NodeAssignments.builder();
        for (final Object o: items) {

            final JSONObject item = (JSONObject) o;

            builder.assign(item.getInt("id"), item.getString("node"));
        }

        return builder.build();
    }

    public String serialize(
            final StateProvider stateProvider,
            final NodeAssignments assignments
    ) {

        final Serializer serializer = new Serializer(new JSONStringer(), assignments, stateProvider);

        return serializer.run().toString();
    }

    private static final class Serializer {

        private final JSONStringer builder;
        private final NodeAssignments assignments;
        private final StateProvider stateProvider;

        public Serializer(
                final JSONStringer builder,
                final NodeAssignments nodeAssignements,
                final StateProvider stateProvider
        ) {

            this.builder = builder;
            this.assignments = nodeAssignements;
            this.stateProvider = stateProvider;
        }

        public JSONBuilder run() {

            builder.object().key("queue").array();

            queue(stateProvider);

            return builder.endArray().endObject();
        }

        private void queue(final StateProvider stateProvider) {

            final List<? extends Queue.Item> queue = stateProvider.getQueue();

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

                builder.key("assigned").value(assignments.nodeName(item));

                builder.endObject();
            }
        }

        private int priority(final Queue.Item item) {

            final String jobName = item.task.getDisplayName().toLowerCase();

            if (jobName.contains("eap") || jobName.contains("brms") || jobName.contains("jdg")) return 70;

            return 50;
        }

        private void nodes(final Queue.Item item) {

            for (final Node node: getUsableNodes(item)) {

                builder.object();

                builder.key("name").value(node.getNodeName());
                builder.key("executors").value(node.getNumExecutors());
                builder.key("freeExecutors").value(node.toComputer().countIdle());

                builder.endObject();
            }
        }

        private Collection<Node> getUsableNodes(final Queue.Item item) {

            final Label label = item.getAssignedLabel();

            if (label != null) {

                final Set<Node> nodes = label.getNodes();
                if (nodes != null && !nodes.isEmpty()) return nodes;
            }

            return stateProvider.getNodes();
        }
    }
}
