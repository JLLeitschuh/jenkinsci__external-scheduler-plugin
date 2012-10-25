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


import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

public final class PlannerProxy {

    private final static Logger LOGGER = Logger.getLogger(
            PlannerProxy.class.getName()
    );

    private static final String PREFIX = "/rest/hudsonQueue";
    private static final String TYPE = MediaType.APPLICATION_JSON;

    private static final QueueSerializerService serializator = new QueueSerializerService();
    private static final Client client = Client.create();

    private final URL serviceDestination;

    // Remote solving has started
    private boolean started = false;

    // Remote solving has stopped
    private boolean stopped = false;

    public PlannerProxy(final URL serviceDestination) {

        this.serviceDestination = serviceDestination;
    }

    public URL remoteUrl() {

        return serviceDestination;
    }

    public int score() {

        assumeStarted();

        final String score = getResource("/score").accept(TYPE).get(String.class);

        info("Getting score");
        return serializator.getScore(score);
    }

    public NodeAssignments solution() {

        assumeStarted();

        final String solution = getResource().accept(TYPE).get(String.class);

        info("Getting solution");
        return serializator.deserialize(solution);
    }

    public PlannerProxy queue(final StateProvider stateProvider, final NodeAssignments assignments) {

        if (assignments == null) throw new IllegalArgumentException("No assignments provided");

        final String queueString = serializator.serialize(stateProvider, assignments);
        final WebResource.Builder resource = getResource().type(TYPE);

        if (started) {

            info("Sending queue update");
            resource.put(queueString);
        } else {

            started = true;
            info("Starting remote planner");
            resource.post(queueString);
        }

        return this;
    }

    public PlannerProxy stop() {

        if (!started) return this;

        if (!stopped) {

            stopped = true;
            info("Stopping remote planner");
            getResource().delete();
        } else {

            error(String.format(
                    "Planner %s already stopped", serviceDestination.toString()
            ));
        }

        return this;
    }

    private WebResource getResource() {

        return getResource("");
    }

    private WebResource getResource(final String suffix) {

        final URL url = getUrl(suffix);

        return client.resource(url.toString());
    }

    private URL getUrl(final String url) {

        try {

            return new URL(serviceDestination, PREFIX + url);
        } catch (MalformedURLException ex) {

            throw new AssertionError(
                    serviceDestination.toString() + PREFIX + url + ": " + ex.getMessage()
            );
        }
    }

    private void assumeStarted() {

        if (!started) throw new IllegalStateException("Remote planner not started");
    }

    private void info(final String message) {

        LOGGER.info(message);
    }

    private void error(final String message) {

        LOGGER.severe(message);
    }

    protected void finalize() throws Throwable {

        // valid postcondition
        if (!started || stopped) return;

        error(String.format(
                "Planner %s was not stopped properlly", serviceDestination.toString()
        ));

        stop();
    }
}
