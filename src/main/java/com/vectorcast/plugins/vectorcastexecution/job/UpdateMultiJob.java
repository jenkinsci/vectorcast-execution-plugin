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
     * @param request request object
     * @param response response object
     * @param useSavedData use saved data true/false
     * @throws ServletException exception
     * @throws IOException exception
     */
    public UpdateMultiJob(final StaplerRequest request, final StaplerResponse response, boolean useSavedData) throws ServletException, IOException {
        super(request, response, useSavedData);
    }
    /**
     * Get the multi job name
     * @return multi-job name
     */
    public String getMultiJobName() {
        String projectName = getBaseName() + ".vcast.multi";
        return projectName;
    }
    /**
     * Do update
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws InterruptedException exception
     * @throws JobAlreadyExistsException exception
     * @throws InvalidProjectFileException exception
     */
    public void update() throws IOException, ServletException, Descriptor.FormException, InterruptedException, JobAlreadyExistsException, InvalidProjectFileException {
        deleted = new ArrayList<>();
        String projectName = getMultiJobName();
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
     * @return newly created project
     * @throws IOException exception
     */
    @Override
    protected Project createProject() throws IOException {
        String projectName = getBaseName() + ".vcast.multi";
        return getInstance().createProject(MultiJobProject.class, projectName);
    }
    /**
     * Delete job
     * @param jobName job to delete
     * @throws IOException exception
     */
    private void deleteJob(String jobName) throws IOException {
        if (getBaseName().isEmpty()) {
            return;
        }
        try {
            List<Item> jobs = getInstance().getAllItems();
            for (Item job : jobs) {
                if (job.getFullName().equalsIgnoreCase(jobName)) {
                    job.delete();
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(DeleteJobs.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
