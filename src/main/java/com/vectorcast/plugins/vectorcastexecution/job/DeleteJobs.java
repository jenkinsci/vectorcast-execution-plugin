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
package com.vectorcast.plugins.vectorcastexecution.job;

import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Project;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Delete jobs
 */
public class DeleteJobs extends BaseJob {
    /** Jobs to delete */
    List<String> jobsToDelete = null;
    /**
     * Get jobs to delete
     * @return jobs to delete
     */
    public List<String> getJobsToDelete() {
        return jobsToDelete;
    }
    /**
     * Constructor
     * @param request request
     * @param response response
     * @throws ServletException
     * @throws IOException 
     */
    public DeleteJobs(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        super(request, response);
    }
    /**
     * Create job list
     */
    public void createJobList() {
        if (getBaseName().isEmpty()) {
            return;
        }
        jobsToDelete = new ArrayList<>();
        String baseName = getBaseName() + "_";
        String projName = getBaseName() + ".vcast_manage";
        String singleName = projName + ".singlejob";
        String multiName = projName + ".multijob";
        if (getInstance().getJobNames().contains(singleName)) {
            jobsToDelete.add(singleName);
        }
        if (getInstance().getJobNames().contains(multiName)) {
            jobsToDelete.add(multiName);
        }

        Collection<String> jobs = getInstance().getJobNames();
        for (String job : jobs) {
            if (job.startsWith(baseName)) {
                jobsToDelete.add(job);
            }
        }
    }
    /**
     * Delete the jobs
     * @throws IOException 
     */
    public void doDelete() throws IOException {
        if (getBaseName().isEmpty()) {
            return;
        }
        try {
            List<Item> jobs = getInstance().getAllItems();
            for (Item job : jobs) {
                if (jobsToDelete.contains(job.getFullName())) {
                    job.delete();
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DeleteJobs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @Override
    protected Project createProject() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    protected void doCreate(boolean update) throws IOException, ServletException, Descriptor.FormException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    protected void cleanupProject() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
