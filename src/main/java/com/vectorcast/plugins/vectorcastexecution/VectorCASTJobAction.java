/*
 * The MIT License
 * 
 * Copyright 2016 Vector Software, East Greenwich, Rhode Island USA
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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.vectorcast.plugins.vectorcastexecution;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.RootAction;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Add a new action that will add the VectorCAST create job
 */
@Extension
public final class VectorCASTJobAction implements RootAction, Describable<VectorCASTJobAction> {

    private static final Logger LOG = Logger.getLogger(VectorCASTJobAction.class.getName());
    private static final String JOBNAME = "VectorCAST Create Jobs from Manage Project";
    private static final String JOBCFG = "JobConfig.xml";
  
    private boolean exists = false;

    public boolean isExists() {
        checkForJobs();
        return exists;
    }
    
    /**
     * Check if the 'VectorCAST create job' already exists
     */
    private void checkForJobs() {
        Jenkins instance = Jenkins.getInstance();
        if (instance != null) {
            exists = false;
            Collection<String> jobs = instance.getJobNames();
            for (String job : jobs) {
                if (job.equals(JOBNAME)) {
                    exists = true;
                    break;
                }
            }
        }
    }

    public void doCreate(final StaplerRequest request, final StaplerResponse response) throws ServletException,
        IOException {
        Jenkins instance = Jenkins.getInstance();
        if (instance != null) {
            Collection<String> jobs = instance.getJobNames();
            boolean add = true;
            for (String job : jobs) {
                if (job.equals(JOBNAME)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                try {
                    LOG.log(Level.INFO, "Add " + JOBNAME);
                    InputStream is = VectorCASTJobAction.class.getClassLoader().getResourceAsStream(JOBCFG);
                    instance.createProjectFromXML(JOBNAME, is);
                } catch (IOException ex) {
                    Logger.getLogger(VectorCASTJobAction.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
      response.forwardToPreviousPage(request);
    }

    public String getRootUrl() {
        return Jenkins.getInstance().getRootUrl();
    }

    @Override
    public String getDisplayName() {
        return Messages.VectorCASTCommand_AddVCJob();
    }

    @Override
    public String getIconFileName() {
        return "/plugin/vectorcast-execution/icons/vector_favicon.png";
    }

    @Override
    public String getUrlName() {
        return "/createVCJob";
    }

    @Override
    public Descriptor<VectorCASTJobAction> getDescriptor() {
        Jenkins jenkins = Jenkins.getActiveInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins not running properly");
        }
        return jenkins.getDescriptorOrDie(getClass());
    }
  
    @Extension
    public static final class JobImportActionDescriptor extends Descriptor<VectorCASTJobAction> {

        @Override
        public String getDisplayName() { return ""; }

    }
}
