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

/**
 * Translate objects to JSON and back
 *
 * @author ogondza
 */
public final class JsonSerializer {

    /**
     * Extract int from score message
     *
     * @param score JSON score
     * @return Integral score
     */
    public int extractScore(final String score) {

        return JSONObject.fromObject(score).getInt("score");
    }

    /**
     * Extract assignments from solution message
     *
     * @param solution JSON solution
     * @return Updated assignments
     */
    public NodeAssignments extractAssignments(final String solution) {

        final JSONArray items = JSONObject.fromObject(solution).getJSONArray("solution");

        final NodeAssignments.Builder builder = NodeAssignments.builder();
        for (final Object o: items) {

            final JSONObject item = (JSONObject) o;

            builder.assign(item.getInt("id"), item.getString("node"));
        }

        return builder.build();
    }

    /**
     * Serialize current state as JSON
     *
     * @param stateProvider Current state
     * @param assignments Latest assignments
     * @return JSON query
     */
    public String buildQuery(
            final StateProvider stateProvider, final NodeAssignments assignments
    ) {

        final Serializer serializer = new Serializer(new JSONStringer(), assignments, stateProvider);

        return serializer.getJson().toString();
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

            if (builder == null) throw new AssertionError("builder is null");
            if (nodeAssignements == null) throw new AssertionError("nodeAssignments is null");
            if (stateProvider == null) throw new AssertionError("stateProvider is null");

            this.builder = builder;
            this.assignments = nodeAssignements;
            this.stateProvider = stateProvider;
        }

        public JSONBuilder getJson() {

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

            return 50;
        }

        private void nodes(final Queue.Item item) {

            for (final Node node: getUsableNodes(item)) {

                final Computer computer = node.toComputer();
                final int freeExecutors = (computer == null)
                        ? 0
                        : computer.countIdle()
                ;

                builder.object();

                builder.key("name").value(getName(node));
                builder.key("executors").value(node.getNumExecutors());
                builder.key("freeExecutors").value(freeExecutors);

                builder.endObject();
            }
        }


        private Collection<Node> getUsableNodes(final Queue.Item item) {

            final Label label = item.getAssignedLabel();

            if (label != null) {

                final Set<Node> nodes = label.getNodes();
                if (nodes != null) return nodes;
            }

            return stateProvider.getNodes();
        }

        private String getName(final Node node) {

            return node.getSelfLabel().toString();
        }
    }
}
