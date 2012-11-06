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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertSame;
import hudson.model.Node;
import hudson.model.Queue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Node.class)
public class RestPlannerTest {

    private static final String SERVICE = "http://hudsonqueueplanning-ogondza.rhcloud.com/";

    private final NodeAssignments assignments = NodeAssignments.builder()
            .assign(2, "get_assigned_solution")
            .build()
    ;

    private final NodeMockFactory nodeFactory = new NodeMockFactory();

    private final List<Node> nodes = new ArrayList<Node>();

    private Planner pp;

    @Before
    public void setUp() throws MalformedURLException {

        pp = new RestPlanner(new URL(SERVICE));
    }

    @After
    public void tearDown() {

        pp.stop();
    }

    private Queue.Item getItem(final Set<Node> nodeSet) {

        return ItemMock.create(
                nodeSet, 2, "Single queue item", 3
        );
    }

    @Test
    public void getSolution() {

        final List<Queue.Item> items = ItemMock.list();
        items.add(getItem(getNodeSet("get_solution", 2, 1)));

        pp.queue(new StateProviderMock(items, nodes), assignments);

        validateScore(pp.score());

        NodeAssignments assignments = pp.solution();
        assertThat(assignments.nodeName(1), nullValue());
        assertThat(assignments.nodeName(2), notNullValue());
    }

    @Test
    public void getAssignedSolution() {

        final List<Queue.Item> items = ItemMock.list();
        items.add(getItem(getNodeSet("get_assigned_solution", 2, 1)));

        pp.queue(new StateProviderMock(items, nodes), assignments);

        validateScore(pp.score());

        NodeAssignments assignments = pp.solution();
        assertThat(assignments.nodeName(1), nullValue());
        assertThat(assignments.nodeName(2), equalTo("get_assigned_solution"));
    }

    /**
     * Test updating the queue several times and picking up the solution
     */
    @Test
    public void getReassignedSolution() {

        getSolution();
        getAssignedSolution();
        getSolution();
        getAssignedSolution();
    }

    @Test(expected = IllegalStateException.class)
    public void getScoreFromNotStarted() {

        pp.score();
    }

    @Test(expected = IllegalStateException.class)
    public void getSolutionFromNotStarted() {

        pp.solution();
    }

    @Test(expected = IllegalStateException.class)
    public void getScoreFromStopped() {

        pp.stop();
        pp.score();
    }

    @Test(expected = IllegalStateException.class)
    public void getSolutionFromStopped() {

        pp.stop();
        pp.solution();
    }

    @Test(expected = IllegalStateException.class)
    public void sendQueueToStopped() {

        pp.stop();
        pp.queue(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getInstanctWithoutUrl() {

        new RestPlanner(null);
    }

    @Test
    public void getUrl() throws MalformedURLException {

        final URL url = new URL(SERVICE);
        final Planner planner = new RestPlanner(url);

        assertSame(url, planner.remoteUrl());
    }

    private SortedSet<Node> getNodeSet(String name, int executors, int freeexecutors) {

        final SortedSet<Node> set = nodeFactory.set();
        set.add(nodeFactory.node(name, executors, freeexecutors));

        return set;
    }

    private void validateScore(final int score) {

        assertThat(score, greaterThanOrEqualTo(0));
        assertThat(score, lessThanOrEqualTo(1));
    }
}
