package org.jenkinsci.plugins.droolsplanner;

import hudson.model.PeriodicWork;

import org.jenkinsci.plugins.droolsplanner.DroolsPlanner.DescriptorImpl;

/**
 * Update state periodically
 *
 * @author ogondza
 */
public class RemoteUpdater extends PeriodicWork {

    private final DescriptorImpl descriptor;

    /*package*/ public RemoteUpdater(final DescriptorImpl descriptor) {

        if (descriptor == null) throw new AssertionError("No descriptor");

        this.descriptor = descriptor;
    }

    @Override
    protected void doRun() throws Exception {

        final StateProvider stateProvider = descriptor.getStateProvider();

        final Planner planner = descriptor.getPlanner();

//        if (planner == null) return;

        planner.queue(stateProvider, planner.solution());
    }

    @Override
    public long getInitialDelay() {

        return 0;
    }

    @Override
    public long getRecurrencePeriod() {

        return 5 * 1000;
    }
}