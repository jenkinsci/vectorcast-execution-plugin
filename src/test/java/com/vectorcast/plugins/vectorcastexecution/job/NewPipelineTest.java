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
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.SingleFileSCM;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import hudson.model.Descriptor.FormException;

import static org.mockito.Mockito.when;
import org.mockito.Mockito;
import com.cloudbees.hudson.plugins.folder.Folder;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class NewPipelineTest {
    final long USE_LOCAL_IMPORTED_RESULTS = 1;
    final long USE_EXTERNAL_IMPORTED_RESULTS = 2;
    final String EXTERNAL_RESULT_FILENAME = "archivedResults/project.vcr";

    /** Jenkins Coverage plugin selection. */
    private static final long USE_COVERAGE_PLUGIN = 1;

    /** VectorCAST Coverage plugin selection. */
    private static final long USE_VCC_PLUGIN = 2;

    private static final String PROJECTNAME = "project_vcast_pipeline";

    private static final String FOLDERNAME = "test_pipeline_folder";
    private NewPipelineJob setupTestBasic(JSONObject jsonForm, JenkinsRule rule) throws ServletException, IOException,
            ExternalResultsFileException, FormException, JobAlreadyExistsException,
            InvalidProjectFileException, Exception {
        rule.jenkins.setSecurityRealm(rule.createDummySecurityRealm());
        MockAuthorizationStrategy mockStrategy = new MockAuthorizationStrategy();
        mockStrategy.grant(Jenkins.READ).everywhere().to("devel");
        for (Permission p : Item.PERMISSIONS.getPermissions()) {
            mockStrategy.grant(p).everywhere().to("devel");
        }
        rule.jenkins.setAuthorizationStrategy(mockStrategy);

        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);

        when(request.getSubmittedForm()).thenReturn(jsonForm);

        Folder folder = rule.jenkins.createProject(Folder.class, FOLDERNAME);

        NewPipelineJob job = new NewPipelineJob(request, response, folder);

        assertEquals("project", job.getBaseName());
        job.create();
        assertEquals(PROJECTNAME, job.getProjectName());
        assertEquals(FOLDERNAME, job.getFolder().getName());

        // Pipeline Jobs have no "topProject"
        assertNull(job.getTopProject());

        return job;
    }

    private void checkImportedResults(NewPipelineJob job, long useLocalResults, Boolean useExternalResults, String externalResultsFilename) {
        if (useLocalResults == USE_LOCAL_IMPORTED_RESULTS) {
            assertTrue(job.getUseLocalImportedResults());
        }
        else if (useLocalResults == USE_EXTERNAL_IMPORTED_RESULTS) {
            assertFalse(job.getUseLocalImportedResults());
        }
        assertEquals(useExternalResults, job.getUseExternalImportedResults());
        assertEquals(externalResultsFilename, job.getExternalResultsFilename());
    }

    private void checkOptions (NewPipelineJob job,
                Boolean optionExecutionReport,
                Boolean optionUseReporting,
                Boolean useCiLicense,
                Boolean useStrictTestcaseImport,
                Boolean useRGW3,
                Boolean useImportedResults,
                Boolean useCoverageHistory) {

        assertEquals(optionExecutionReport, job.getOptionExecutionReport());
        assertEquals(optionUseReporting, job.getOptionUseReporting());
        assertEquals(useCiLicense, job.getUseCILicenses());
        assertEquals(useStrictTestcaseImport, job.getUseStrictTestcaseImport());
        assertEquals(useRGW3, job.getUseRGW3());
        assertEquals(useImportedResults, job.getUseImportedResults());
        assertEquals(useCoverageHistory, job.getUseCoverageHistory());
    }

    private void checkAdditionalTools (NewPipelineJob job,
            final String squoreCommand,
            final String pclpCommand,
            final String pclpResultsPattern) {

        assertEquals(squoreCommand, job.getSquoreCommand());
        assertEquals(pclpCommand, job.getPclpCommand());
        assertEquals(pclpResultsPattern, job.getPclpResultsPattern());
    }

    @Test
    public void testDefaults(JenkinsRule rule) throws Exception {
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("nodeLabel","Test_Node");

        NewPipelineJob job = setupTestBasic(jsonForm, rule);

        assertEquals(true, job.getUseStrictTestcaseImport());
        assertEquals(false, job.getUseCoveragePlugin());
        assertEquals(false, job.getUseCILicenses());
        assertEquals(true, job.getUseCBT());
        assertEquals(false, job.getSingleCheckout());
        assertEquals(false, job.getUseParameters());
        assertEquals(false, job.getUseRGW3());
        assertEquals(false, job.getUseCoverageHistory());
        assertEquals("", job.getSharedArtifactDir());
        assertNull(job.getEnvironmentSetup());
        assertNull(job.getExecutePreamble());
        assertNull(job.getEnvironmentTeardown());
        assertNull(job.getPostSCMCheckoutCommands());
        assertEquals("", job.getPipelineSCM());
        assertEquals(0, job.getMaxParallel().longValue());
    }

    @Test
    public void testAdditionalTools(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();

        JSONObject jsonCovDisplay  = new JSONObject();
        jsonCovDisplay.put("value", USE_COVERAGE_PLUGIN);

        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", jsonCovDisplay);  // VectorCAST Coverage Plugin
        jsonForm.put("useCoverageHistory", true);
        jsonForm.put("pclpCommand","call lint_my_code.bat");
        jsonForm.put("pclpResultsPattern","lint_results.xml");
        jsonForm.put("squoreCommand","hello squore test world");
        
        NewPipelineJob job = setupTestBasic(jsonForm, rule);
        checkAdditionalTools(job,
                "hello squore test world",
                "call lint_my_code.bat",
                "lint_results.xml");
    }

    @Test
    public void testCoveragePlugin(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();

        JSONObject jsonCovDisplay  = new JSONObject();
        jsonCovDisplay.put("value", USE_COVERAGE_PLUGIN);

        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("coverageDisplayOption", jsonCovDisplay);  // Jenkins Coverage Plugin

        NewPipelineJob job = setupTestBasic(jsonForm, rule);

        assertEquals(true, job.getUseCoveragePlugin());
    }

    @Test
    public void testOptions(JenkinsRule rule) throws Exception {
        JSONObject jsonForm = new JSONObject();

        JSONObject jsonCovDisplay  = new JSONObject();
        jsonCovDisplay.put("value", USE_VCC_PLUGIN);

        jsonForm.put("manageProjectName", "project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("nodeLabel","Test_Node");
        jsonForm.put("sharedArtifactDir","/home/jenkins/sharedArtifactDir");
        jsonForm.put("scmSnippet","git 'http://git.com'");
        jsonForm.put("environmentSetup","call setup.bat");
        jsonForm.put("executePreamble","wr_env.bat");
        jsonForm.put("environmentTeardown","close ports");
        jsonForm.put("postSCMCheckoutCommands","chmod a+wr -R *");
        jsonForm.put("coverageDisplayOption",jsonCovDisplay);
        jsonForm.put("maxParallel",10);

        NewPipelineJob job = setupTestBasic(jsonForm, rule);

        assertEquals(true, job.getUseStrictTestcaseImport());
        assertEquals(false, job.getUseCILicenses());
        assertEquals(true, job.getUseCBT());
        assertEquals(false, job.getSingleCheckout());
        assertEquals(false, job.getUseParameters());
        assertEquals(false, job.getUseRGW3());
        assertEquals(false, job.getUseCoveragePlugin());
        assertEquals(false, job.getUseCoverageHistory());
        assertNotEquals(-1, job.getSharedArtifactDir().indexOf("/home/jenkins/sharedArtifactDir"));
        assertEquals("call setup.bat", job.getEnvironmentSetup());
        assertEquals("wr_env.bat", job.getExecutePreamble());
        assertEquals("close ports", job.getEnvironmentTeardown());
        assertEquals("chmod a+wr -R *", job.getPostSCMCheckoutCommands());
        assertEquals("git 'http://git.com'", job.getPipelineSCM());
        assertEquals(10, job.getMaxParallel().longValue());
        assertEquals(false, job.getUseCoveragePlugin());

    }

    @Test
    public void testLocalImportedResults(JenkinsRule rule) throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_LOCAL_IMPORTED_RESULTS);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("useImportedResults", true);
        jsonForm.put("importedResults", jsonImportResults);

        NewPipelineJob job = setupTestBasic(jsonForm, rule);

        checkImportedResults(job, USE_LOCAL_IMPORTED_RESULTS, false, "");
    }

    @Test
    public void testExternalImportedResults(JenkinsRule rule) throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_EXTERNAL_IMPORTED_RESULTS);
        jsonImportResults.put("externalResultsFilename",EXTERNAL_RESULT_FILENAME);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("useImportedResults", true);
        jsonForm.put("importedResults", jsonImportResults);

        NewPipelineJob job = setupTestBasic(jsonForm, rule);

        checkImportedResults(job, USE_EXTERNAL_IMPORTED_RESULTS, true, EXTERNAL_RESULT_FILENAME);
    }

    
    /* TODO: Use Parameters */
    /* TODO: Specify Job name */
    /* TODO: Multiple jobs with same name */
    /* TODO: MPname without .vcm */
    /* TODO: MPname on network driver abs path \\ */
    /* TODO: MPname on windows abs path */
    /* TODO: MPname on abs path and some SCM */
    /* TODO: use CBT */
    /* TODO: use CI license */
    /* TODO: env sections and post checkout set to "" */
    
    
}

