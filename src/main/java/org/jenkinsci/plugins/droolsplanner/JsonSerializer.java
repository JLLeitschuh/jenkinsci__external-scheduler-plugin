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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private static final String NOT_ASSIGNED = "not-assigned";

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

            builder.assign(
                    item.getInt("id"),
                    deserilizeNodeName(item.getString("node"))
            );
        }

        return builder.build();
    }

    private String deserilizeNodeName(final String jsonNodeName) {

        return NOT_ASSIGNED.equals(jsonNodeName)
                ? null
                : jsonNodeName
        ;
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

            queue();

            return builder.endArray().endObject();
        }

        private void queue() {

            for (final Queue.BuildableItem item : stateProvider.getQueue()) {

                builder.object();

                builder.key("id").value(item.id);
                builder.key("priority").value(priority(item));

                builder.key("inQueueSince").value(item.getInQueueSince());

                builder.key("name").value(item.task.getDisplayName());

                builder.key("nodes");

                final List<Node> usableNodes = assignableNodes(item);
                nodes(item, usableNodes);

                builder.key("assigned").value(assignedNode(item, usableNodes));

                builder.endObject();
            }
        }

        private int priority(final Queue.BuildableItem item) {

            return 50;
        }

        private void nodes(final Queue.BuildableItem item, final Collection<Node> nodes) {

            builder.array();

            for (final Node node: nodes) {

                builder.object();

                builder.key("name").value(getName(node));

                builder.key("executors").value(node.getNumExecutors());

                final int freeExecutors = node.toComputer().countIdle();
                builder.key("freeExecutors").value(freeExecutors);

                builder.endObject();
            }

            builder.endArray();
        }

        private List<Node> assignableNodes(final Queue.BuildableItem item) {

            final Label label = item.getAssignedLabel();

            final Collection<Node> nodeCandidates = (label != null && label.getNodes() != null)
                    ? label.getNodes()
                    : stateProvider.getNodes()
            ;

            final List<Node> nodes = new ArrayList<Node>(nodeCandidates.size());
            for(final Node node: nodeCandidates) {

                if (nodeApplicable(item, node)) {

                    nodes.add(node);
                }
            }

            return nodes;
        }

        private boolean nodeApplicable(Queue.BuildableItem item, final Node node) {

            return isOnline(node) && node.canTake(item) == null;
        }

        private boolean isOnline(final Node node) {

            final Computer computer = node.toComputer();
            return computer != null && !computer.isOffline() && computer.isAcceptingTasks();
        }

        private String assignedNode(final Queue.BuildableItem item, final List<Node> nodes) {

            final String assignedTo = assignments.nodeName(item);

            for (final Node node: nodes) {

                if (getName(node).equals(assignedTo)) return assignedTo;
            }

            // currently assigned node is no longer assignable
            return null;
        }

        private String getName(final Node node) {

            return node.getSelfLabel().toString();
        }
    }
}
