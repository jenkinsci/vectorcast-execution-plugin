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

import com.vectorcast.plugins.vectorcastexecution.job.InvalidProjectFileException;
import com.vectorcast.plugins.vectorcastexecution.job.JobAlreadyExistsException;
import com.vectorcast.plugins.vectorcastexecution.job.NewSingleJob;
import hudson.Extension;
import hudson.model.Descriptor;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Create single job
 */
@Extension
public class VectorCASTJobSingle extends JobBase {
    /** Job already exists exception */
    private JobAlreadyExistsException exception;
    /** Project name */
    private String projectName;
    /**
     * Get the project name
     * @return project name
     */
    public String getProjectName() {
        return projectName;
    }
    /**
     * Get job already exists exception
     * @return exception
     */
    public JobAlreadyExistsException getException() {
        return exception;
    }
    /**
     * Get url name for creating single job
     * @return url
     */
    @Override
    public String getUrlName() {
        return "single-job";
    }

    @Extension
    public static final class DescriptorImpl extends JobBaseDescriptor {
    }
    /**
     * Create the single job
     * @param request request object
     * @param response response object
     * @return response
     * @throws ServletException exception
     * @throws IOException exception
     * @throws hudson.model.Descriptor.FormException exception
     */
    @RequirePOST
    public HttpResponse doCreate(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException, Descriptor.FormException {
        // Create single-job
        NewSingleJob job = new NewSingleJob(request, response);
        try {
            job.create(false);
            projectName = job.getProjectName();
            return new HttpRedirect("created");
        } catch (JobAlreadyExistsException ex) {
            exception = ex;
            return new HttpRedirect("exists");
        } catch (InvalidProjectFileException ex) {
            // Can't happen for the single job
            Logger.getLogger(VectorCASTJobSingle.class.getName()).log(Level.SEVERE, null, ex);
            return new HttpRedirect("exists");
        }
    }
}
