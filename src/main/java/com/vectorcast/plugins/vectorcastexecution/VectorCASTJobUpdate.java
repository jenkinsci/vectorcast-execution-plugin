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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.vectorcast.plugins.vectorcastexecution;

import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.vectorcast.plugins.vectorcastexecution.job.InvalidProjectFileException;
import com.vectorcast.plugins.vectorcastexecution.job.JobAlreadyExistsException;
import com.vectorcast.plugins.vectorcastexecution.job.UpdateMultiJob;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.scm.NullSCM;
import jenkins.model.Jenkins;
import hudson.tasks.Builder;
import hudson.util.FormApply;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Update a set of multi-jobs
 */
@Extension
public class VectorCASTJobUpdate extends JobBase {
    /** Update multi-job object */
    private UpdateMultiJob job;
    /**
     * Get multi-job
     * @return multi-job
     */
    public UpdateMultiJob getJob() {
        return job;
    }
    /**
     * Get name of url for updating multi-job
     * @return url
     */
    @Override
    public String getUrlName() {
        return "job-update";
    }

    @Extension
    public static final class DescriptorImpl extends JobBaseDescriptor {
    }
    /**
     * Do the update
     * @param request request object
     * @param response response object
     * @return response
     * @throws ServletException exception
     * @throws IOException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws InterruptedException exception
     */
    @RequirePOST
    public HttpResponse doUpdate(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException, Descriptor.FormException, InterruptedException {
        job = new UpdateMultiJob(request, response, false);
        try {
            job.update();
            return new HttpRedirect("done");
        } catch (JobAlreadyExistsException ex) {
            // Can't happen when doing an update
            Logger.getLogger(VectorCASTJobUpdate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvalidProjectFileException ex) {
            return new HttpRedirect("invalid");
        }
        return FormApply.success(".");
    }
    /**
     * Do the update from saved details
     * @param request request objext
     * @param response response object
     * @return response
     * @throws ServletException exception
     * @throws IOException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws InterruptedException exception
     */
    @RequirePOST
    public HttpResponse doUpdateFromSaved(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException, Descriptor.FormException, InterruptedException {
        job = new UpdateMultiJob(request, response, true);
        String projectName = job.getMultiJobName();

        List<Item> jobs = Jenkins.getInstance().getAllItems();
        for (Item searchJob : jobs) {
            if (searchJob.getFullName().equalsIgnoreCase(projectName)) {
                // Found the multi-job, now get the VectorCASTSetup
                MultiJobProject project = (MultiJobProject)searchJob;
                for (Builder builder : project.getBuilders()) {
                    if (builder instanceof VectorCASTSetup) {
                        // Only 1 of them, so stop afterwards
                        VectorCASTSetup vcSetup = (VectorCASTSetup)builder;
                        // SCM doesn't seem to be saved so use definition from project
                        if (project.getScm() instanceof NullSCM) {
                            vcSetup.setUsingSCM(false);
                        } else {
                            vcSetup.setUsingSCM(true);
                        }
                        vcSetup.setSCM(project.getScm());
                        try {
                            job.useSavedData(vcSetup);
                            job.update();
                            return new HttpRedirect("done");
                        } catch (JobAlreadyExistsException ex) {
                            // Can't happen when doing an update
                            Logger.getLogger(VectorCASTJobUpdate.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InvalidProjectFileException ex) {
                            return new HttpRedirect("invalid");
                        }
                        return FormApply.success(".");
                    }
                }
                break;
            }
        }
        return new HttpRedirect("invalid");
    }
}
