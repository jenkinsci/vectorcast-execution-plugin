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

import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import java.io.IOException;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Create a multi-job update job
 */
public class NewMultiJobUpdate extends BaseJob {
    /** Username for connecting back to Jenkins */
    private String username;
    /** Password for connecting back to Jenkins */
    private String password;
    /**
     * Constructor
     * @param request request object
     * @param response response object
     * @throws ServletException exception
     * @throws IOException exception
     */
    public NewMultiJobUpdate(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        super(request, response, false);
        JSONObject json = request.getSubmittedForm();
        username = json.optString("userName");
        password = json.optString("password");
    }
    /**
     * Create new top-level project
     * @return top-level project
     * @throws IOException exception
     * @throws JobAlreadyExistsException exception
     */
    @Override
    protected Project createProject() throws IOException, JobAlreadyExistsException {
        String projectName = getBaseName() + ".vcast.updatemulti";
        if (getInstance().getJobNames().contains(projectName)) {
            throw new JobAlreadyExistsException(projectName);
        }
        return getInstance().createProject(FreeStyleProject.class, projectName);
    }

    @Override
    protected void cleanupProject() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void doCreate(boolean update) throws IOException, ServletException, Descriptor.FormException, InvalidProjectFileException {
        getTopProject().setDescription("Update job to update the manage project: " + getManageProjectName());

        // Build actions...
        addSetup(getTopProject());

        addScriptCall();
        
        getTopProject().save();
    }
    /**
     * Add script to call back to Jenkins
     */
    private void addScriptCall() {
        String args = " --projfile " + getManageProjectName();
        args += " --projname " + getBaseName();
//        args += " --verbose";
        if (!username.isEmpty()) {
            args += " --user " + username;
            args += " --password " + password;
        }

        String win =
"vpython \"%WORKSPACE%\\vc_scripts\\UpdateMultiJob.py\" ";
        win += " --url %JENKINS_URL%";
        win += args;
        win += "\n";

        String unix =
"vpython \"$WORKSPACE/vc_scripts/UpdateMultiJob.py\" ";
        unix += " --url $JENKINS_URL";
        unix += args;
        unix += "\n";

        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        getTopProject().getBuildersList().add(command);
    }
}
