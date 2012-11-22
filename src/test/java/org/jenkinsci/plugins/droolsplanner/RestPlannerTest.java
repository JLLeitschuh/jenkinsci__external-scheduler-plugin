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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

@RunWith(PowerMockRunner.class)
@PrepareForTest(URL.class)
public class RestPlannerTest {

    private URL serviceUrl;
    private Client client;

    private RestPlanner pp;

    @Before
    public void setUp() throws MalformedURLException, InterruptedException {

        client = mock(Client.class);

        serviceUrl = mock(URL.class);
        when(serviceUrl.toString()).thenReturn("Fake url");

        useMeaningFullInfo();

        pp = new RestPlanner(serviceUrl, client);
    }

    private void useMeaningFullInfo() {

        WebResource r = mock(WebResource.class);
        WebResource.Builder rb = mock(WebResource.Builder.class);
        when(r.accept(MediaType.TEXT_PLAIN)).thenReturn(rb);

        when(client.resource(Mockito.endsWith("/info"))).thenReturn(r);

        when(rb.get(String.class)).thenReturn("hudson-queue-planning : Planner Mock on URL planer.mock.localhost");
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

    @Test(expected = IllegalArgumentException.class)
    public void getInstanctWithoutUrl() {

        new RestPlanner(null);
    }

    @Test
    public void getUrl() throws MalformedURLException {

        assertSame(serviceUrl, pp.remoteUrl());
    }

    @Test
    public void checkName() {

        assertEquals( "Planner Mock", pp.name() );
    }
}
