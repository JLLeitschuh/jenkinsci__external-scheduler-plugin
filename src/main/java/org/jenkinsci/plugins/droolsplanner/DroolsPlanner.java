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
public final class DroolsPlanner extends AbstractDescribableImpl<DroolsPlanner> {

    private final static Logger LOGGER = Logger.getLogger(
            DroolsPlanner.class.getName()
    );

    private transient StateProvider stateProvider;

    private RestPlanner planner;
    private NodeAssignments solution;

    Long lastState = null;

    /**
     * Does Drools Based Queue Planner plugin intercept Jenkins queue scheduling
     */
    public boolean isActive() {

        return planner != null;
    }

    public NodeAssignments currentSolution() {

        if (!isActive()) return null;

        if (solution != null) return solution;

        return NodeAssignments.empty();
    }

    /**
     * Fetch remote solution
     *
     * @return New assignments
     */
    public NodeAssignments fetchSolution() {

        if (!isActive()) return null;

        return this.solution = planner.solution();
    }

    /**
     * Update Queue
     *
     * @return True if queue updated
     */
    public boolean sendQueue() {

        if (!isActive()) return false;

        final Long currentState = fingerprint(stateProvider(), currentSolution());

        if (currentState.equals(lastState)) return false;

        lastState = currentState;
        return planner.queue(stateProvider(), currentSolution());
    }

    private Long fingerprint(
            final StateProvider stateProvider,
            final NodeAssignments assignments
    ) {

        return new Long(
                stateProvider.hashCode() << 32 + assignments.hashCode()
        );
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
    public static class DescriptorImpl extends Descriptor<DroolsPlanner> {

        private transient RestPlanner planner;
        private static DroolsPlanner droolsPlanner = new DroolsPlanner ();

        private String serverUrl;

        public DescriptorImpl() {

            load();
            droolsPlanner.planner = getPlanner();
        }

        @Override
        public String getDisplayName() {

            return "Drools planner plugin";
        }

        public String getServerUrl() {

            return serverUrl;
        }

        private URL getUrl(final String serverUrl) {

            try {

                return new URL(serverUrl);
            } catch (MalformedURLException ex) {

                return null;
            }
        }

        public RestPlanner getPlanner() {

            if (planner != null) return planner;

            try {

                reloadPlanner();
            } catch (PlannerException ex) {

                // Thrown if and only if getting planner for invalid URL.
                // This exception was already handled upon configuration change.
            }

            return planner;
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

            try {

                reloadPlanner();
            } catch (PlannerException ex) {

                LOGGER.log(Level.INFO, "Drools queue planner not responding. Switching to default planner.", ex);
            }

            return true;
        }

        private void reloadPlanner() {

            if (serverUrl == null) return;

            final URL url = getUrl(serverUrl);

            if (url == null) return;

            if (planner != null) {

                // Do not create new planner for the same url
                if (url.equals(planner.remoteUrl())) return;

                // Stop the planner in case we are starting another
                planner.stop();

                // Erase the reference not to use stopped planner in case new
                // configuration will fail to initialize
                planner = null;
            }

            LOGGER.log(Level.INFO, "Attaching remote drools queue planner");
            droolsPlanner.planner = planner = new RestPlanner(url);
            droolsPlanner.planner.queue(
                    droolsPlanner.stateProvider(),
                    NodeAssignments.empty()
             );
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {

            try {

                final URL url = new URL(serverUrl);
                final RestPlanner restPlanner = new RestPlanner(url);

                return FormValidation.ok(restPlanner.name());
            } catch(MalformedURLException ex) {

                return FormValidation.error(ex, "It is not URL");
            } catch(PlannerException ex) {

                return FormValidation.warning(ex, "Server seems down or it is not a Drools planner server");
            }
        }

        /**
         * Register Dispatcher as a Jenkins extension
         * @return  Dispatcher instance
         */
        @Extension
        public static Dispatcher getDispatcher() {

            return new Dispatcher(droolsPlanner);
        }

        /**
         * Register RemoteUpdater as a Jenkins extension
         * @return  RemoteUpdater instance
         */
        @Extension
        public static RemoteUpdater getUpdater() {

            return new RemoteUpdater(droolsPlanner);
        }
    }
}
