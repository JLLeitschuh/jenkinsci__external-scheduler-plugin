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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class CachingPlannerTest {

    @Mock private Planner planner;
    @Mock private StateProvider jenkins;

    private CachingPlanner cachingPlanner;

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);

        cachingPlanner = new CachingPlanner(planner);
    }

    private NodeAssignments emptyAssignments() {

        return NodeAssignments.empty();
    }

    @Test
    public void stop() {

        cachingPlanner.stop();

        Mockito.verify(planner).stop();
        Mockito.verifyNoMoreInteractions(planner);
    }

    @Test
    public void remoteUrl() {

        cachingPlanner.remoteUrl();

        Mockito.verify(planner).remoteUrl();
        Mockito.verifyNoMoreInteractions(planner);
    }

    public int score() {

        return planner.score();
    }

    public NodeAssignments solution() {

        return planner.solution();
    }

    @Test
    public void doNotResendQueue() {

        final StateProvider sp = Mockito.mock(StateProvider.class);
        final NodeAssignments na = NodeAssignments.empty();

        cachingPlanner.queue(sp, na);
        cachingPlanner.queue(sp, na);

        Mockito.verify(planner).queue(sp, na);
        Mockito.verifyNoMoreInteractions(planner);
    }

    @Test
    public void updateQueueOnChange () {

        final StateProvider hudson = Mockito.mock(StateProvider.class);

        final NodeAssignments notEmpty = NodeAssignments.builder().assign(42, "42").build();

        cachingPlanner.queue(hudson, emptyAssignments());
        cachingPlanner.queue(hudson, emptyAssignments());
        cachingPlanner.queue(jenkins, emptyAssignments());
        cachingPlanner.queue(jenkins, emptyAssignments());
        cachingPlanner.queue(jenkins, notEmpty);
        cachingPlanner.queue(jenkins, notEmpty);
        cachingPlanner.queue(hudson, notEmpty);
        cachingPlanner.queue(hudson, notEmpty);

        Mockito.verify(planner).queue(hudson, emptyAssignments());
        Mockito.verify(planner).queue(jenkins, emptyAssignments());
        Mockito.verify(planner).queue(jenkins, notEmpty);
        Mockito.verify(planner).queue(hudson, notEmpty);
        Mockito.verifyNoMoreInteractions(planner);
    }

    @Test
    public void doNotRefetchScore() {

        cachingPlanner.score();
        cachingPlanner.score();

        Mockito.verify(planner).score();
        Mockito.verifyNoMoreInteractions(planner);
    }

    @Test
    public void doNotRefetchSolution() {

        Mockito.when(planner.solution()).thenReturn(NodeAssignments.empty());

        cachingPlanner.solution();
        cachingPlanner.solution();

        Mockito.verify(planner).solution();
        Mockito.verifyNoMoreInteractions(planner);
    }

    @Test
    public void updateScore() {

        cachingPlanner.score();
        cachingPlanner.queue(jenkins, emptyAssignments());
        cachingPlanner.score();

        Mockito.verify(planner, Mockito.times(2)).score();
        Mockito.verify(planner).queue(jenkins, emptyAssignments());
        Mockito.verifyNoMoreInteractions(planner);
    }

    @Test
    public void updateSolution() {

        cachingPlanner.solution();
        cachingPlanner.queue(jenkins, emptyAssignments());
        cachingPlanner.solution();

        Mockito.verify(planner, Mockito.times(2)).solution();
        Mockito.verify(planner).queue(jenkins, emptyAssignments());
        Mockito.verifyNoMoreInteractions(planner);
    }
}
