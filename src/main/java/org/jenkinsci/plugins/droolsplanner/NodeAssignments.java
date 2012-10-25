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

import hudson.model.Queue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to represent Task to Node assignment
 *
 * @author ogondza
 */
public final class NodeAssignments {

    private final Map<Integer, String> assignments;

    public static NodeAssignments.Builder builder() {

        return new Builder();
    }

    public static NodeAssignments empty() {

        return builder().build();
    }

    public static final class Builder {

        final Map<Integer, String> assignments = new HashMap<Integer, String>();

        public NodeAssignments.Builder assign(final int id, final String nodeName) {

            assignments.put(id, nodeName);
            return this;
        }

        public NodeAssignments build() {

            return new NodeAssignments(this);
        }
    }

    public NodeAssignments(final NodeAssignments.Builder builder) {

        this.assignments = Collections.unmodifiableMap(builder.assignments);
    }

    public String nodeName(final Queue.Item task) {

        return nodeName(task.id);
    }

    public String nodeName(final int taskId) {

        return assignments.get(taskId);
    }

    public int size() {

        return assignments.size();
    }
}