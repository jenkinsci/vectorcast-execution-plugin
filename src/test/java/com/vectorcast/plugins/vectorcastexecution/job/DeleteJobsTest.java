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
import com.vectorcast.plugins.vectorcastexecution.VectorCASTSetup;
import hudson.model.FreeStyleProject;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;
import static org.powermock.api.mockito.PowerMockito.when;

public class DeleteJobsTest {

    @Rule
    public JenkinsRule rule = new JenkinsRule();
    
    private static final String MANAGE_PROJECT = "/home/jenkins/vcast/project.vcm";

    @Test
    public void deleteTest() throws Exception {
        FreeStyleProject singleJobProject = rule.createFreeStyleProject("single.vcast");
        VectorCASTSetup vcSetup = new VectorCASTSetup("environmentSetupWin",
                "environmentSetupUnix",
                "executePreambleWin",
                "executePreambleUnix",
                "environmentTeardownWin",
                "environmentTeardownUnix",
                true,
                "optionErrorLevel",
                "optionHtmlBuildDesc",
                true,
                true,
                0L,
                0L,
                MANAGE_PROJECT,
                "jobName",
                "nodeLabel");
        singleJobProject.getBuildersList().add(vcSetup);
        
        MultiJobProject multiJobProject = rule.createProject(MultiJobProject.class, "multi.vcast");
        multiJobProject.getBuildersList().add(vcSetup);
        
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", MANAGE_PROJECT);
        when(request.getSubmittedForm()).thenReturn(jsonForm);

        FreeStyleProject jobToKeep = rule.createFreeStyleProject("jobToKeep");

        DeleteJobs deleteJobs = new DeleteJobs(request, response);
        deleteJobs.createJobList();
        Assert.assertEquals(2, deleteJobs.getJobsToDelete().size());
        Assert.assertEquals(3, rule.jenkins.getView("All").getAllItems().size());
        
        deleteJobs.doDelete();
        Assert.assertEquals(1, rule.jenkins.getView("All").getAllItems().size());
    }
}
