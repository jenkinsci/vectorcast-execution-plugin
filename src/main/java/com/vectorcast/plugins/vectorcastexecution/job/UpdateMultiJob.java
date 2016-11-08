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

import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Project;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Update a multi-job project.
 * Basically
 *     re-create top-level multi-job (delete then create)
 *     delete any projects no longer needed.
 *     add any new projects now required
 */
public class UpdateMultiJob extends NewMultiJob {
    /** Deleted jobs */
    private List<String> deleted = null;
    /**
     * Get the list of deleted jobs
     * @return the deleted jobs
     */
    public List<String> getDeleted() {
        return deleted;
    }
    /**
     * Constructor
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException 
     */
    public UpdateMultiJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        super(request, response);
    }
    /**
     * Do update
     * @throws IOException
     * @throws ServletException
     * @throws hudson.model.Descriptor.FormException
     * @throws InterruptedException
     * @throws JobAlreadyExistsException 
     */
    public void update() throws IOException, ServletException, Descriptor.FormException, InterruptedException, JobAlreadyExistsException {
        deleted = new ArrayList<>();
        String projectName = getBaseName() + ".vcast_manage.multijob";
        // Delete existing multijob
        deleteJob(projectName);
        // Create all other projects
        create(true);
        // Now remove any (now) redundant project
        List<Item> jobs = getInstance().getAllItems();
        for (Item job : jobs) {
            // Delete any jobs not part of the multi-job set just added
            if (!getProjectsNeeded().contains(job.getFullName()) &&
                job.getFullName().startsWith(getBaseName() + "_")) {
                deleted.add(job.getFullName());
                job.delete();
            }
        }
    }
    /**
     * Create new top-level project
     * @return
     * @throws IOException 
     */
    @Override
    protected Project createProject() throws IOException {
        String projectName = getBaseName() + ".vcast_manage.multijob";
        return getInstance().createProject(MultiJobProject.class, projectName);
    }
    /**
     * Delete job
     * @param jobName job to delete
     * @throws IOException 
     */
    private void deleteJob(String jobName) throws IOException {
        if (getBaseName().isEmpty()) {
            return;
        }
        try {
            List<Item> jobs = getInstance().getAllItems();
            for (Item job : jobs) {
                if (job.getFullName().equals(jobName)) {
                    job.delete();
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DeleteJobs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
