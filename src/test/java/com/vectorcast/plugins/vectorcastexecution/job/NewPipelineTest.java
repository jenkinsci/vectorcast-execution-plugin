package com.vectorcast.plugins.vectorcastexecution.job;

import com.vectorcast.plugins.vectorcastexecution.job.ScmConflictException;
import com.vectorcast.plugins.vectorcastexecution.job.ExternalResultsFileException;
import com.vectorcast.plugins.vectorcastexecution.job.JobAlreadyExistsException;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.Permission;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.SingleFileSCM;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;
import org.mockito.Mockito;
import hudson.model.Descriptor.FormException;

import static org.mockito.Mockito.when;

public class NewPipelineTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @BeforeEach
    void setUpStaticMocks() {
    }

    @AfterEach
    void tearDownStaticMocks() {
    }

    @Test public void customizeWorkspaceWithFile() 
            throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        MockAuthorizationStrategy mockStrategy = new MockAuthorizationStrategy();
        mockStrategy.grant(Jenkins.READ).everywhere().to("devel");
        for (Permission p : Item.PERMISSIONS.getPermissions()) {
            mockStrategy.grant(p).everywhere().to("devel");
        }
        j.jenkins.setAuthorizationStrategy(mockStrategy);

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
        Assert.assertEquals("project_vcast_pipeline", job.getProjectName());
    }
}
