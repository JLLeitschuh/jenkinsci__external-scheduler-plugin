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

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author ogondza
 *
 */
public final class DroolsPlanner extends AbstractDescribableImpl<DroolsPlanner> {



    /**
     * Class to represent Task assignment
     *
     * @author ogondza
     */
    public static final class NodeAssignements {

        private final Map<Integer, String> assignments;

        public NodeAssignements(final Map<Integer, String> assignments) {

            this.assignments = Collections.unmodifiableMap(assignments);
        }
    }

    /**
     * Class to represent possible assignment to the particular Node
     *
     * @author ogondza
     */
    public static final class NodeAssignment {

        private static final String NOT_ASSIGNED = "not assigned";
        private final Node node;

        public static NodeAssignment fromString(final List<Node> existingNodes, final String name) {

            if (name == null) throw new IllegalArgumentException("No name");

            if (name.equals(NOT_ASSIGNED)) return new NodeAssignment(null);

            for (final Node node: existingNodes) {

                if (name.equals(node.getDisplayName())) return new NodeAssignment(node);
            }

            throw new AssertionError("unknown node");
        }

        public NodeAssignment(final Node node) {

            this.node = node;
        }

        public boolean isAssigned() {

            return node != null;
        }

        public String toString() {

            return node == null
                    ? NOT_ASSIGNED
                    : node.getDisplayName()
            ;
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DroolsPlanner> {

        private String serverUrl;

        public DescriptorImpl() {

            load();
        }

        @Override
        public String getDisplayName() {

            return "Drools planner plugin";
        }

        public String getServerUrl() {

            return serverUrl;
        }

        @Override
        public boolean configure(
                final StaplerRequest req,
                final JSONObject formData
        ) throws FormException {

            this.serverUrl = formData.getString("serverUrl");

            save();
            return true;
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {

            URL url;
            try {

                url = new URL(serverUrl + "/hudsonQueue");
            } catch(MalformedURLException ex) {

                return FormValidation.error(ex, "It is not URL");
            }

            try {

                url.getContent();
            } catch (IOException ex) {

                return FormValidation.warning(ex, "Server seems down or it is not Drools planner server");
            }

            return FormValidation.ok("ok");
        }
    }
}
