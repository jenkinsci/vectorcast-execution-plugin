package com.vectorcast.plugins.vectorcastexecution.job;

import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import java.net.URL;
import java.nio.file.Path;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Documents current behavior when a packaged job template cannot be read. */
class JobResourceFailureTest {
    @TempDir
    Path temporary;

    @Test
    @WithJenkins
    void documentsUnreadableTemplateBehaviorInGeneratedJobs(JenkinsRule rule)
            throws Exception {
        URL missing = temporary.resolve("missing-template").toUri().toURL();
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "project.vcm");
        StaplerRequest request = mock(StaplerRequest.class);
        StaplerResponse response = mock(StaplerResponse.class);
        when(request.getSubmittedForm()).thenReturn(form);
        NewSingleJob single = new NewSingleJob(request, response, null) {
            @Override
            protected URL getBaselineWindowsSingleFile() {
                return missing;
            }

            @Override
            protected URL getBaselineLinuxSingleFile() {
                return missing;
            }
        };
        single.create();
        VectorCASTCommand command = single.getTopProject().getBuildersList()
            .get(VectorCASTCommand.class);
        assertTrue(command.getWinCommand()
            .contains("Missing baseline single job script for Windows"));
        assertTrue(command.getUnixCommand()
            .contains("Missing baseline single job script for Linux"));

        NewPipelineJob pipeline = new NewPipelineJob(request, response, null) {
            @Override
            protected URL getBaselinePipelineGroovy() {
                return missing;
            }
        };
        pipeline.create();
        WorkflowJob job = assertInstanceOf(WorkflowJob.class,
            rule.jenkins.getItem(pipeline.getProjectName()));
        CpsFlowDefinition definition = assertInstanceOf(CpsFlowDefinition.class,
            job.getDefinition());
        assertTrue(definition.getScript().contains("def VC_Manage_Project = 'project.vcm'"));
        assertFalse(definition.getScript().contains("def VC_Healthy_Target"));
        // Current defect: the catch leaves an empty string, so the null-only
        // fallback does not add the intended error diagnostic.
        assertFalse(definition.getScript().contains("Errors reading the baseJenkinsfile"));
    }
}
