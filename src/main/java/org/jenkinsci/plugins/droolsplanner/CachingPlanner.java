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

import java.net.URL;

public class CachingPlanner implements Planner {

    private final Planner planner;

    private Long remoteState;
    private Integer remoteScore;
    private NodeAssignments remoteSolution;

    public CachingPlanner(final Planner planner) {

        this.planner = planner;
        reset();
    }

    private void reset() {

        remoteState = null;
        remoteScore = null;
        remoteSolution = null;
    }

    public Planner stop() {

        return planner.stop();
    }

    public URL remoteUrl() {

        return planner.remoteUrl();
    }

    public int score() {

        if (remoteScore == null) {

            remoteScore = new Integer(planner.score());
        }

        return remoteScore.intValue();
    }

    public NodeAssignments solution() {

        if (remoteSolution == null) {

            remoteSolution = planner.solution();
        }

        return remoteSolution;
    }

    public Planner queue(
            final StateProvider stateProvider,
            final NodeAssignments assignments
    ) {

        final Long arriving = fingerprint(stateProvider, assignments);

        if (!arriving.equals(remoteState)) {

            planner.queue(stateProvider, assignments);
            reset();
            remoteState = arriving;
        }

        return this;
    }

    private Long fingerprint(
            final StateProvider stateProvider,
            final NodeAssignments assignments
    ) {

        return new Long(
                stateProvider.hashCode() << 32 + assignments.hashCode()
        );
    }
}
