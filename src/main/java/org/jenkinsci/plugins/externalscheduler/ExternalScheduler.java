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
import hudson.util.DescribableList;

import java.io.IOException;
import java.util.List;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * @author ogondza
 */
public final class ExternalScheduler extends Plugin implements Describable<ExternalScheduler> {

    private static ExternalScheduler INSTANCE;

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
     * Get scheduler currently in use
     *
     * @return First configured scheduler of {@link DefaultScheduler} if there is none. Never null.
     */
    public Scheduler activeScheduler() {

        try {

            return getDescriptor().configuredProviders().get(0);
        } catch (IndexOutOfBoundsException ex) {

            return new DefaultScheduler();
        }
    }

    /**
     * Get external scheduler solution
     *
     * @return New assignments
     * @see Scheduler.solution()
     */
    public NodeAssignments currentSolution() {

        return activeScheduler().solution();
    }

    public DescriptorImpl getDescriptor() {

        return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ExternalScheduler> {

        private DescribableList<Scheduler, Scheduler.Descriptor> builders;

        public DescriptorImpl() {

            load();
        }

        @Override
        public boolean configure(
                final StaplerRequest req, final JSONObject formData
        ) throws FormException {

            final DescribableList<Scheduler, Scheduler.Descriptor> newBuilders = emptyProviders();
            try {

                newBuilders.rebuildHetero(req, formData, providerKinds(), "providers");
            } catch (final IOException ex) {

                throw new FormException("rebuildHetero failed", ex, "none");
            }

            builders = newBuilders;
            save();
            return true;
        }

        @Override
        public String getDisplayName() {

            return "Use custom scheduler implementation";
        }

        public List<Scheduler.Descriptor> providerKinds() {

            return Jenkins.getInstance().getDescriptorList(Scheduler.class);
        }

        public DescribableList<Scheduler, Scheduler.Descriptor> configuredProviders() {

            if (builders == null) {

                builders = emptyProviders();
            }

            return builders;
        }

        private DescribableList<Scheduler, Scheduler.Descriptor> emptyProviders() {

            return new DescribableList<Scheduler, Scheduler.Descriptor>(this);
        }
    }
}
