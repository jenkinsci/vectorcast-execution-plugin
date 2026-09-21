package com.vectorcast.plugins.vectorcastexecution;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.scm.NullSCM;
import hudson.model.FreeStyleProject;
import io.jenkins.plugins.coverage.metrics.steps.CoverageRecorder;
import edu.hm.hafner.coverage.Metric;
import java.util.List;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/** Tests the folder action exposed by the VectorCAST menu. */
@WithJenkins
class VectorCASTFolderActionTest {

    @Test
    void exposesFolderMetadataAndRoutesToTheSupportedJobTypes(
            JenkinsRule rule) throws Exception {
        Folder folder = rule.jenkins.createProject(Folder.class, "vectorcast");
        VectorCASTFolderAction action = new VectorCASTFolderAction(folder);

        assertEquals("VectorCAST", action.getDisplayName());
        assertEquals("VectorCAST", action.getUrlName());
        assertEquals("vectorcast", action.getFolderName());
        assertEquals("vectorcast", action.getFolderFullName());
        assertSame(folder, action.getFolder());
        assertSame(action, action.getTarget());
        assertEquals("/plugin/vectorcast-execution/icons/vector_favicon_bw.png",
            action.getIconFileName());
        VectorCASTJobSingle single = assertInstanceOf(
            VectorCASTJobSingle.class,
            action.getDynamic("single-job", null, null));
        assertEquals("single-job", single.getUrlName());
        assertSame(folder, single.getFolder());
        assertInstanceOf(NullSCM.class, single.getTheScm());
        assertEquals("VectorCAST Single Job",
            single.getDescriptor().getDisplayName());

        VectorCASTJobPipeline pipeline = assertInstanceOf(
            VectorCASTJobPipeline.class,
            action.getDynamic("pipeline-job", null, null));
        assertEquals("pipeline-job", pipeline.getUrlName());
        assertSame(folder, pipeline.getFolder());
        assertInstanceOf(NullSCM.class, pipeline.getTheScm());
        assertEquals("VectorCAST Pipeline Job",
            pipeline.getDescriptor().getDisplayName());
        assertNull(action.getDynamic("unknown", null, null));

        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject blankExternalResults = new JSONObject();
        blankExternalResults.put("value", 2);
        JSONObject invalidSingleForm = new JSONObject();
        invalidSingleForm.put("manageProjectName", "project.vcm");
        invalidSingleForm.put("useImportedResults", true);
        invalidSingleForm.put("importedResults", blankExternalResults);
        when(request.getSubmittedForm()).thenReturn(invalidSingleForm);
        assertInstanceOf(HttpRedirect.class, single.doCreate(request, response));

        JSONObject conflictingPipelineForm = new JSONObject();
        conflictingPipelineForm.put("manageProjectName", "C:\\project.vcm");
        conflictingPipelineForm.put("scmSnippet", "git 'https://example.invalid/repo.git'");
        when(request.getSubmittedForm()).thenReturn(conflictingPipelineForm);
        assertInstanceOf(HttpRedirect.class,
            pipeline.doCreate(request, response));
        assertNotNull(pipeline.getScmException());
        assertEquals("git 'https://example.invalid/repo.git'",
            pipeline.getScmException().getScmSnippet());
        assertEquals("C:/project.vcm",
            pipeline.getScmException().getPathToManageProject());

        // Exercise successful controller submissions at both root and folder scope,
        // then ensure a duplicate submission preserves the original job.
        for (Folder destination : new Folder[]{null, folder}) {
            JSONObject form = new JSONObject();
            form.put("manageProjectName", "controller.vcm");
            form.put("jobName", "controller_single");
            form.put("useCoverageHistory", true);
            when(request.getSubmittedForm()).thenReturn(form);
            VectorCASTJobSingle singleController = new VectorCASTJobSingle(destination);
            assertFalse(singleController.doCreate(request, response) instanceof HttpRedirect);
            String singleName = singleController.getProjectName();
            Object singleItem = destination == null
                ? rule.jenkins.getItem(singleName) : destination.getItem(singleName);
            FreeStyleProject freestyle = assertInstanceOf(FreeStyleProject.class, singleItem);
            CoverageRecorder recorder = freestyle.getPublishersList().get(CoverageRecorder.class);
            assertNotNull(recorder);
            assertEquals(List.of(Metric.LINE, Metric.BRANCH), recorder.getQualityGates()
                .stream().map(gate -> gate.getMetric()).toList());
            assertInstanceOf(HttpRedirect.class, singleController.doCreate(request, response));
            assertNotNull(singleController.getException());
            assertEquals(freestyle.getFullName(), singleController.getException().getProject());
            assertSame(singleItem, destination == null
                ? rule.jenkins.getItem(singleName) : destination.getItem(singleName));

            form.put("jobName", "controller_pipeline");
            VectorCASTJobPipeline pipelineController = new VectorCASTJobPipeline(destination);
            assertFalse(pipelineController.doCreate(request, response) instanceof HttpRedirect);
            String pipelineName = pipelineController.getJob().getProjectName();
            Object pipelineItem = destination == null
                ? rule.jenkins.getItem(pipelineName) : destination.getItem(pipelineName);
            assertInstanceOf(WorkflowJob.class, pipelineItem);
            assertInstanceOf(HttpRedirect.class, pipelineController.doCreate(request, response));
            assertNotNull(pipelineController.getException());
            assertEquals(((WorkflowJob) pipelineItem).getFullName(),
                pipelineController.getException().getProject());
            assertSame(pipelineItem, destination == null
                ? rule.jenkins.getItem(pipelineName) : destination.getItem(pipelineName));
        }
    }
}
