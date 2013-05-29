package org.jenkinsci.plugins.restservicescheduler;

import org.jenkinsci.plugins.externalscheduler.ExternalScheduler;

import hudson.model.PeriodicWork;

/**
 * Update state periodically
 *
 * @author ogondza
 */
public class RemoteUpdater extends PeriodicWork {

    private final ExternalScheduler scheduler;

    /*package*/ public RemoteUpdater(final ExternalScheduler scheduler) {

        if (scheduler == null) throw new AssertionError("No scheduler");

        this.scheduler = scheduler;
    }

    @Override
    protected void doRun() throws Exception {

        scheduler.fetchSolution();
        scheduler.sendQueue();
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