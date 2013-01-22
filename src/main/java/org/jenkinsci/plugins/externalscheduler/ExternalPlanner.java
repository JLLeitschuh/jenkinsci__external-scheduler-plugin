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
package org.jenkinsci.plugins.externalscheduler;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author ogondza
 */
public final class ExternalPlanner extends AbstractDescribableImpl<ExternalPlanner> {

    private final static Logger LOGGER = Logger.getLogger(
            ExternalPlanner.class.getName()
    );

    private transient StateProvider stateProvider;

    private RestPlanner planner;
    private NodeAssignments solution;

    Long lastState = null;

    /**
     * Determine whether the Planner plugin intercept Jenkins queue scheduling
     */
    public boolean assumeActive() {

        if (planner != null) return true;

        // Get descriptor in case it was not instantiated yet to have planner injected
        getDescriptor();
        if (planner != null) return true;

        LOGGER.info("External scheduler not active");
        return false;
    }

    public NodeAssignments currentSolution() {

        if (!assumeActive()) return null;

        if (solution != null) return solution;

        return NodeAssignments.empty();
    }

    /**
     * Fetch remote solution
     *
     * @return New assignments
     */
    public NodeAssignments fetchSolution() {

        if (!assumeActive()) return null;

        NodeAssignments oldSolution = this.solution;

        solution = planner.solution();

        if (!solution.equals(oldSolution)) {

            stateProvider.updateQueue();
        }

        return solution;
    }

    /**
     * Update Queue
     *
     * @return True if queue updated
     */
    public boolean sendQueue() {

        if (!assumeActive()) return false;

        return planner.queue(stateProvider(), currentSolution());
    }

    private StateProvider stateProvider() {

        if (stateProvider == null) {

            stateProvider = new AbstractCiStateProvider(Jenkins.getInstance());
        }

        return stateProvider;
    }

    @Override
    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ExternalPlanner> {

        private static ExternalPlanner externalPlanner = new ExternalPlanner();

        private String serverUrl;

        public DescriptorImpl() {

            load();
            externalPlanner.planner = getPlanner(null);
        }

        /**
         * Get planner based on existing configuration or null when not possible
         */
        private RestPlanner getPlanner(final RestPlanner planner) {

            try {

                return reloadPlanner(planner);
            } catch (PlannerException ex) {

                LOGGER.log(Level.INFO, "External scheduler not responding. Using default scheduler.", ex);
                return null;
            }
        }

        private RestPlanner reloadPlanner(final RestPlanner planner) {

            if (serverUrl == null) return null;

            final URL url = getUrl(serverUrl);

            if (url == null) return null;

            if (planner != null) {

                // Do not create new planner for the same url
                if (url.equals(planner.remoteUrl())) return null;

                // Stop the planner in case we are starting another
                planner.stop();
            }

            LOGGER.log(Level.INFO, "Attaching external scheduler.");
            final RestPlanner newPlanner = new RestPlanner(url);
            newPlanner.queue(
                    externalPlanner.stateProvider(),
                    NodeAssignments.empty()
            );

            return newPlanner;
        }

        @Override
        public String getDisplayName() {

            return "External scheduler plugin";
        }

        public String getServerUrl() {

            return serverUrl;
        }

        @Override
        public boolean configure(
                final StaplerRequest req, final JSONObject formData
        ) throws FormException {

            serverUrl = formData.getString("serverUrl");

            if (!serverUrl.endsWith("/")) {

                serverUrl += "/";
            }

            save();

            externalPlanner.planner = getPlanner(externalPlanner.planner);

            return true;
        }

        private URL getUrl(final String serverUrl) {

            try {

                return new URL(serverUrl);
            } catch (MalformedURLException ex) {

                return null;
            }
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {

            try {

                final URL url = new URL(serverUrl);
                final RestPlanner restPlanner = new RestPlanner(url);

                return FormValidation.ok(restPlanner.name());
            } catch(MalformedURLException ex) {

                return FormValidation.error(ex, "It is not URL");
            } catch(PlannerException ex) {

                return FormValidation.warning(ex, "Server seems down or it is not an External scheduler");
            }
        }

        /**
         * Register Dispatcher as a Jenkins extension
         */
        @Extension
        public static Dispatcher getDispatcher() {

            return new Dispatcher(externalPlanner);
        }

        /**
         * Register RemoteUpdater as a Jenkins extension
         */
        @Extension
        public static RemoteUpdater getUpdater() {

            return new RemoteUpdater(externalPlanner);
        }
    }
}
