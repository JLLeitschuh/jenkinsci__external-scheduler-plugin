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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;

import org.jenkinsci.plugins.externalscheduler.ExternalScheduler;
import org.jenkinsci.plugins.externalscheduler.RemoteUpdater;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ExternalScheduler.class)
public class RemoteUpdaterTest {

    private ExternalScheduler externalPlanner;

    private RemoteUpdater updater;

    @Before
    public void setUp() {

        externalPlanner = mock(ExternalScheduler.class);

        updater = new RemoteUpdater(externalPlanner);
    }

    @After
    public void tearDown() {

        verifyNoMoreInteractions(externalPlanner);
    }

    @Test(expected = AssertionError.class)
    public void doNotInstantiateWithoutDescriptor() {

        new RemoteUpdater(null);
    }

    @Test
    public void fetchAndSend() throws Exception {

        updater.doRun();

        verify(externalPlanner).sendQueue();
        verify(externalPlanner).fetchSolution();
    }
}
