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

/**
 *
 * @author ogondza
 */
public interface Planner {

    /**
     * Get planner URL
     * @return Associated URL
     */
    URL remoteUrl();

    /**
     * Get planner score
     * @return score
     */
    Score score();

    /**
     * Get planner solution
     * @return New assignments
     */
    NodeAssignments solution();

    /**
     * Put new state into planner
     * @param stateProvider Jenkins state
     * @param assignments Current assignments
     * @return updated or not
     */
    boolean queue(
            final StateProvider stateProvider,
            final NodeAssignments assignments
    );

    /**
     * Stop planner
     * @return self
     */
    Planner stop();
}