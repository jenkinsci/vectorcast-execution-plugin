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

// import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.vectorcast.plugins.vectorcastexecution.job.InvalidProjectFileException;
import com.vectorcast.plugins.vectorcastexecution.job.JobAlreadyExistsException;
import com.vectorcast.plugins.vectorcastexecution.job.ScmConflictException;
import com.vectorcast.plugins.vectorcastexecution.job.NewPipelineJob;
import com.vectorcast.plugins.vectorcastexecution.job.PipelineNotSupportedException;

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
 * Create multiple jobs
 */
@Extension
public class VectorCASTJobPipeline extends JobBase {
    /** Job exists exception */
    private JobAlreadyExistsException exception;

    /** Job exists exception */
    private ScmConflictException scmException;
    
	/** project name */
	private String projectName;

    /** Pipeline job object */
    private NewPipelineJob job;
    /**
     * Get the pipeline job object
     * @return pipeline job
     */
    public NewPipelineJob getJob() {
        return job;
    }
    /**
     * Get the project name
     * @return project name
     */
    public String getProjectName() {
        return projectName;
    }
    /**
     * Get the job already exists exception
     * @return job already exists exception
     */
    public JobAlreadyExistsException getException() {
        return exception;
    }
    /**
     * Get the job already scm conflict exception
     * @return job already scm conflict exception
     */
    public ScmConflictException getScmException() {
        return scmException;
    }
    /**
     * URL for creating pipeline job
     * @return url
     */
    @Override
    public String getUrlName() {
        return "pipeline-job";
    }

    @Extension
    public static final class DescriptorImpl extends JobBaseDescriptor {
    }
    /**
     * Create pipeline job
     * @param request request objext
     * @param response response object
     * @return response
     * @throws ServletException exception
     * @throws IOException exception
     * @throws hudson.model.Descriptor.FormException exception
     */
    @RequirePOST
    public HttpResponse doCreate(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException, Descriptor.FormException {
        try {
            // Create Pipeline job
            job = new NewPipelineJob(request, response);
            job.create(false);
            projectName = job.getProjectName();
        	Logger.getLogger(VectorCASTJobPipeline.class.getName()).log(Level.SEVERE, "Pipeline Project Name: " + projectName, "Pipeline Project Name: " + projectName);
            return new HttpRedirect("created");
        } catch (ScmConflictException ex) {
            scmException = ex;
            return new HttpRedirect("conflict");
        }catch (JobAlreadyExistsException ex) {
            exception = ex;
            return new HttpRedirect("exists");
        } catch (InvalidProjectFileException ex) {
			// cannot happen on pipeline job as we don't read the project
            return new HttpRedirect("exists");
        } catch (PipelineNotSupportedException ex) {
        	Logger.getLogger(VectorCASTJobPipeline.class.getName()).log(Level.SEVERE, null, ex);
        	return new HttpRedirect("unsupported");
        }
    }
}
