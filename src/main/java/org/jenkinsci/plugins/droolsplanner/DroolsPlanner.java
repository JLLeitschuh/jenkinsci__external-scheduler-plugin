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
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author ogondza
 *
 */
public final class DroolsPlanner extends AbstractDescribableImpl<DroolsPlanner> {

    @Extension
    public static final class DescriptorImpl extends Descriptor<DroolsPlanner> {

        private String serverUrl;

        public DescriptorImpl() {

            load();
        }

        @Override
        public String getDisplayName() {

            return "Drools planner plugin";
        }

        public String getServerUrl() {

            return serverUrl;
        }

        @Override
        public boolean configure(
                final StaplerRequest req,
                final JSONObject formData
        ) throws FormException {

            this.serverUrl = formData.getString("serverUrl");

            save();
            return true;
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {

            URL url;
            try {

                url = new URL(serverUrl + "/hudsonQueue");
            } catch(MalformedURLException ex) {

                return FormValidation.error(ex, "It is not URL");
            }

            try {

                url.getContent();
            } catch (IOException ex) {

                return FormValidation.warning(ex, "Server seems down or it is not Drools planner server");
            }

            return FormValidation.ok("ok");
        }
    }
}
