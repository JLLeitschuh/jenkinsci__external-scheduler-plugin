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
import hudson.model.Queue;
import hudson.model.Queue.BuildableItem;
import hudson.model.queue.QueueTaskDispatcher;
import hudson.model.queue.CauseOfBlockage;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author ogondza
 *
 */
public final class DroolsPlanner extends AbstractDescribableImpl<DroolsPlanner> {

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class Dispatcher extends QueueTaskDispatcher {

        private final DescriptorImpl descriptor;
        private NodeAssignments assignments = NodeAssignments.empty();
        private int lastState = -1;

        public Dispatcher() {

            descriptor = (DescriptorImpl) Jenkins
                    .getInstance()
                    .getDescriptor(DroolsPlanner.class)
            ;

            update();
        }

        public CauseOfBlockage canRun(Queue.Item item) {

            update();

            if (assignments.nodeName(item) == null) return new NotAssigned();

            return null;
        }

        public CauseOfBlockage canTake(Node node, BuildableItem item) {

            return new NotAssigned();
        }

        private void update() {

            final StateProvider stateProvider = descriptor.getStateProvider();
            final int currentState = stateProvider.hashCode();

            if (lastState == currentState) return;

            descriptor.getPlannerProxy().queue(stateProvider, assignments);
            assignments = descriptor.getPlannerProxy().solution();

            lastState = currentState;
        }

        public static final class NotAssigned extends CauseOfBlockage {

            @Override
            public String getShortDescription() {

                return "Drools Planner decided not at assign the job to any node";
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DroolsPlanner> {

        private transient PlannerProxy plannerProxy;
        private transient StateProvider stateProvider;

        private URL serverUrl;

        public DescriptorImpl() {

            load();
        }

        @Override
        public String getDisplayName() {

            return "Drools planner plugin";
        }

        public URL getServerUrl() {

            return serverUrl;
        }

        public PlannerProxy getPlannerProxy() {

            if (plannerProxy == null) {

                updatePlanner();
            }

            return plannerProxy;
        }

        public StateProvider getStateProvider() {

            if (stateProvider == null) {

                stateProvider = new AbstractCiStateProvider(Jenkins.getInstance());
            }

            return stateProvider;
        }

        @Override
        public boolean configure(
                final StaplerRequest req, final JSONObject formData
        ) throws FormException {

            try {

                serverUrl = new URL(formData.getString("serverUrl"));
            } catch (MalformedURLException ex) {

                throw new FormException(ex, "serverUrl");
            }

            save();

            updatePlanner();
            return true;
        }

        private void updatePlanner() {

            if (plannerProxy != null && serverUrl.equals(plannerProxy.remoteUrl())) return;

            plannerProxy = new PlannerProxy(serverUrl).queue(
                    getStateProvider(),
                    NodeAssignments.empty()
            );
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
