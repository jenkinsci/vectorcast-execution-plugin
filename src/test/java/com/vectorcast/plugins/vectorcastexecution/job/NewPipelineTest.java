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

import hudson.model.Item;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
@PowerMockIgnore("jdk.internal.reflect.*")

public class NewPipelineTest extends TestCase {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    private static final String PROJECTNAME = "project_vcast_pipeline";

    @Test
    public void testBasic() throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy mockStrategy = new MockAuthorizationStrategy();
        mockStrategy.grant(Jenkins.READ).everywhere().to("devel");
        for (Permission p : Item.PERMISSIONS.getPermissions()) {
            mockStrategy.grant(p).everywhere().to("devel");
        }
        r.jenkins.setAuthorizationStrategy(mockStrategy);

        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("nodeLabel","Test_Node");
        when(request.getSubmittedForm()).thenReturn(jsonForm);

        NewPipelineJob job = new NewPipelineJob(request, response);
        Assert.assertEquals("project", job.getBaseName());
        job.create(false);
        Assert.assertEquals(PROJECTNAME, job.getProjectName());
        //Assert.assertTrue(job.getTopProject() != null);
    }
}
