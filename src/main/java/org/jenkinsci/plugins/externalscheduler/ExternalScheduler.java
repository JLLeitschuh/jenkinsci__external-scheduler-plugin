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
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.restservicescheduler.RemoteUpdater;
import org.jenkinsci.plugins.restservicescheduler.RestScheduler;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author ogondza
 */
public final class ExternalScheduler extends Plugin implements Describable<ExternalScheduler> {

    private static final Logger LOGGER = Logger.getLogger(
            ExternalScheduler.class.getName()
    );

    private static ExternalScheduler INSTANCE;

    private transient StateProvider stateProvider;

    private RestScheduler scheduler;

    private NodeAssignments solution;

    public ExternalScheduler() {

        INSTANCE = this;
    }

    /**
     * Register Dispatcher as a Jenkins extension
     */
    @Extension
    public static Dispatcher getDispatcher() {

        return new Dispatcher(INSTANCE);
    }

    /**
     * Register RemoteUpdater as a Jenkins extension
     */
    @Extension
    public static RemoteUpdater getUpdater() {

        return new RemoteUpdater(INSTANCE);
    }

    @Override
    public void postInitialize() throws Exception {

        super.postInitialize();

        attachScheduler();
    }

    @Override
    public void stop() throws Exception {

        super.stop();

        if (scheduler == null) return;

        scheduler.stop();
    }

    /**
     * Determine whether the Planner plugin intercept Jenkins queue scheduling
     */
    public boolean isActive() {

        return scheduler != null;
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

        NodeAssignments oldSolution = this.solution;

        try {

            solution = scheduler.solution();
        } catch (SchedulerException e) {

            solution = null;
        }

        if (queueUpdateNeeded(solution, oldSolution)) {

            stateProvider.updateQueue();
        }

        return solution;
    }

    private boolean queueUpdateNeeded(
            final NodeAssignments oldSolution, final NodeAssignments newSolution
    ) {

        return oldSolution == null
                ? newSolution != null
                : !oldSolution.equals(newSolution)
        ;
    }

    /**
     * Update Queue
     *
     * @return True if queue updated
     */
    public boolean sendQueue() {

        if (!isActive()) return false;

        try {

            return scheduler.queue(stateProvider(), currentSolution());
        } catch (SchedulerException ex) {

            solution = null;
            return false;
        }
    }

    private StateProvider stateProvider() {

        if (stateProvider == null) {

            stateProvider = new AbstractCiStateProvider(Jenkins.getInstance());
        }

        return stateProvider;
    }

    private void configurationUpdated() {

        attachScheduler();
    }

    private void detachScheduler(final SchedulerException ex) {

        LOGGER.log(Level.WARNING, "External scheduler not responding. Using default scheduler.", ex);
        this.scheduler = null;
    }

    /**
     * Get planner based on existing configuration or null when not possible
     */
    private void attachScheduler() {

        final URL url = getUrl(getDescriptor().getServerUrl());

        if (url == null) return;

        if (scheduler != null) {

            // Do not create new planner for the same url
            if (url.equals(scheduler.remoteUrl())) return;

            // Stop the planner in case we are starting another
            LOGGER.info("Stopping external scheduler: " + url.toString());

            try {

                scheduler.stop();
            } catch (SchedulerException ex) {

                detachScheduler(ex);
            }
        }

        LOGGER.info("Attaching external scheduler: " + url.toString());
        this.scheduler = createPlanner(url);
    }

    private RestScheduler createPlanner(final URL url) {

        try {

            final RestScheduler newPlanner = new RestScheduler(url);
            newPlanner.queue(stateProvider(), NodeAssignments.empty());

            return newPlanner;
        } catch (SchedulerException ex) {

            LOGGER.log(Level.WARNING, "External scheduler not responding. Using default scheduler.", ex);
            return null;
        }
    }

    private URL getUrl(final String serverUrl) {

        if (serverUrl == null) return null;

        try {

            return new URL(serverUrl);
        } catch (MalformedURLException ex) {

            return null;
        }
    }

    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ExternalScheduler> {

        private String serverUrl;

        public DescriptorImpl() {

            load();
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

            plugin().configurationUpdated();
            return true;
        }

        private ExternalScheduler plugin() {

            return (ExternalScheduler) getPlugin().getPlugin();
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {

            try {

                final URL url = new URL(serverUrl);
                final RestScheduler restPlanner = new RestScheduler(url);

                return FormValidation.ok(restPlanner.name());
            } catch(MalformedURLException ex) {

                return FormValidation.error(ex, "It is not URL");
            } catch(SchedulerException ex) {

                return FormValidation.warning(ex, "Server seems down or it is not an External scheduler");
            }
        }
    }
}
