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

import hudson.Extension;
import hudson.model.Queue;
import hudson.model.queue.QueueSorter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Drools planner sorter
 *
 * @author ogondza
 */
@Extension
public class Sorter extends QueueSorter {

    private static final Comparator<Queue.Item> comparator = new Comparator<Queue.Item> () {

        public int compare(final Queue.Item lhs, final Queue.Item rhs) {

            if (lhs.task == rhs.task) return 0;
            if (lhs.task == null) return -1;
            if (rhs.task == null) return 1;

            return lhs.task.getDisplayName().compareTo(rhs.task.getDisplayName());
        }
    };

    @Override
    public void sortBuildableItems(List<Queue.BuildableItem> items) {

        Collections.sort(items, comparator);
    }
}
