package com.vectorcast.plugins.vectorcastexecution.job;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Project;
import hudson.scm.NullSCM;
import hudson.scm.SCMS;
import hudson.security.ACL;
import hudson.security.AccessDeniedException3;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Tests validation and creation failure policy without starting Jenkins. */
class BaseJobPolicyTest {
    @ParameterizedTest
    @CsvSource({"nothing, 0", "unstable, 1", "failure, 2", "unknown, 0"})
    void mapsErrorLevels(String level, int expected) throws Exception {
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "project.vcm");
        form.put("optionErrorLevel", " " + level + " ");
        try (MockedStatic<Jenkins> access = controller(true, true)) {
            TestJob job = new TestJob(request(form));
            assertEquals(expected, job.getOptionErrorLevel());
        }
    }

    @Test
    void rejectsOversizedProjectPathsBeforeCreatingAJob() throws Exception {
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "x".repeat(1001));
        StaplerRequest request = request(form);
        try (MockedStatic<Jenkins> access = controller(true, true)) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new TestJob(request));
            assertEquals("manageProjectName too long > 1000", error.getMessage());
        }
    }

    @ParameterizedTest
    @CsvSource({"false, true", "true, false", "false, false"})
    void requiresBothCreateAndConfigurePermissions(boolean create, boolean configure)
            throws Exception {
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "project.vcm");
        try (MockedStatic<Jenkins> access = controller(create, configure)) {
            TestJob single = new TestJob(request(form));
            assertThrows(AccessDeniedException3.class, single::create);
            assertFalse(single.created);
            NewPipelineJob pipeline = new NewPipelineJob(request(form),
                mock(StaplerResponse.class), null);
            assertThrows(AccessDeniedException3.class, pipeline::create);
        }
    }

    @Test
    void stopsWhenNoProjectWasCreated() throws Exception {
        try (MockedStatic<Jenkins> access = controller(true, true)) {
            TestJob job = new TestJob(request(new JSONObject()));
            job.create();
            assertTrue(job.created);
            assertFalse(job.configured);
            assertNull(job.getTopProject());
        }
    }

    @Test
    void suppliesNullScmAndCleansUpAfterAnInvalidProject() throws Exception {
        try (MockedStatic<Jenkins> access = controller(true, true);
                MockedStatic<SCMS> scms = mockStatic(SCMS.class)) {
            TestJob job = new TestJob(request(new JSONObject()));
            job.project = mock(FreeStyleProject.class);
            job.invalid = new InvalidProjectFileException();
            InvalidProjectFileException error = assertThrows(
                InvalidProjectFileException.class, job::create);
            assertSame(job.invalid, error);
            assertTrue(job.cleaned);
            assertTrue(job.configured);
            assertFalse(job.isUsingScm());
            verify(job.project).setScm(any(NullSCM.class));
        }
    }

    private static StaplerRequest request(JSONObject form) throws Exception {
        StaplerRequest request = mock(StaplerRequest.class);
        when(request.getSubmittedForm()).thenReturn(form);
        return request;
    }

    private static MockedStatic<Jenkins> controller(boolean create, boolean configure) {
        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.hasPermission(Item.CREATE)).thenReturn(create);
        when(jenkins.hasPermission(Item.CONFIGURE)).thenReturn(configure);
        MockedStatic<Jenkins> access = mockStatic(Jenkins.class);
        access.when(Jenkins::get).thenReturn(jenkins);
        access.when(Jenkins::getAuthentication2).thenReturn(ACL.SYSTEM2);
        return access;
    }

    private static final class TestJob extends BaseJob {
        private Project<?, ?> project;
        private boolean created;
        private boolean configured;
        private boolean cleaned;
        private InvalidProjectFileException invalid;

        private TestJob(StaplerRequest request) throws Exception {
            super(request, mock(StaplerResponse.class), null);
        }

        @Override
        protected Project<?, ?> createProject() {
            created = true;
            return project;
        }

        @Override
        protected void doCreate() throws InvalidProjectFileException {
            configured = true;
            if (invalid != null) {
                throw invalid;
            }
        }

        @Override
        protected void cleanupProject() {
            cleaned = true;
        }
    }
}
