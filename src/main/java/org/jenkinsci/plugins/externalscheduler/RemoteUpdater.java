package org.jenkinsci.plugins.externalscheduler;

import hudson.model.PeriodicWork;

/**
 * Update state periodically
 *
 * @author ogondza
 */
public class RemoteUpdater extends PeriodicWork {

    private final ExternalPlanner planner;

    /*package*/ public RemoteUpdater(final ExternalPlanner planner) {

        if (planner == null) throw new AssertionError("No planner");

        this.planner = planner;
    }

    @Override
    protected void doRun() throws Exception {

        planner.fetchSolution();
        planner.sendQueue();
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