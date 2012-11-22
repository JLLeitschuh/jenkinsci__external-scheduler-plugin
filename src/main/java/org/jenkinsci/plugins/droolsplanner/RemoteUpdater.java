package org.jenkinsci.plugins.droolsplanner;

import hudson.model.PeriodicWork;

/**
 * Update state periodically
 *
 * @author ogondza
 */
public class RemoteUpdater extends PeriodicWork {

    private final DroolsPlanner planner;

    /*package*/ public RemoteUpdater(final DroolsPlanner planner) {

        if (planner == null) throw new AssertionError("No planner");

        this.planner = planner;
    }

    @Override
    protected void doRun() throws Exception {

        final boolean updated = planner.sendQueue();

        if (!updated) {

            planner.fetchSolution();
        }
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