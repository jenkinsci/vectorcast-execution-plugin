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
    final long USE_LOCAL_IMPORTED_RESULTS = 1;
    final long USE_EXTERNAL_IMPORTED_RESULTS = 2;
    final String EXTERNAL_RESULT_FILENAME = "archivedResults/project.vcr";

    @Rule public JenkinsRule j = new JenkinsRule();
    private static final String PROJECTNAME = "project_vcast_pipeline";

    @BeforeEach
    void setUpStaticMocks() {
    }

    @AfterEach
    void tearDownStaticMocks() {
    }

    private NewPipelineJob setupTestBasic(JSONObject jsonForm) throws ServletException, IOException,
            ExternalResultsFileException, FormException, JobAlreadyExistsException,
            InvalidProjectFileException, Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        MockAuthorizationStrategy mockStrategy = new MockAuthorizationStrategy();
        mockStrategy.grant(Jenkins.READ).everywhere().to("devel");
        for (Permission p : Item.PERMISSIONS.getPermissions()) {
            mockStrategy.grant(p).everywhere().to("devel");
        }
        j.jenkins.setAuthorizationStrategy(mockStrategy);

        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);

        when(request.getSubmittedForm()).thenReturn(jsonForm);

        NewPipelineJob job = new NewPipelineJob(request, response);

        Assert.assertEquals("project", job.getBaseName());
        job.create();
        Assert.assertEquals(PROJECTNAME, job.getProjectName());

        // Pipeline Jobs have no "topProject"
        Assert.assertNull(job.getTopProject());

        return job;
    }

    private void checkImportedResults(NewPipelineJob job, long useLocalResults, Boolean useExternalResults, String externalResultsFilename) {
        if (useLocalResults == USE_LOCAL_IMPORTED_RESULTS) {
            Assert.assertTrue(job.getUseLocalImportedResults());
        }
        else if (useLocalResults == USE_EXTERNAL_IMPORTED_RESULTS) {
            Assert.assertFalse(job.getUseLocalImportedResults());
        }
        Assert.assertEquals(useExternalResults, job.getUseExternalImportedResults());
        Assert.assertEquals(externalResultsFilename, job.getExternalResultsFilename());
    }

    private void checkOptions (NewPipelineJob job,
                Boolean optionExecutionReport,
                Boolean optionUseReporting,
                Boolean useCiLicense,
                Boolean useStrictTestcaseImport,
                Boolean useRGW3,
                Boolean useImportedResults,
                Boolean useCoverageHistory) {

        Assert.assertEquals(optionExecutionReport, job.getOptionExecutionReport());
        Assert.assertEquals(optionUseReporting, job.getOptionUseReporting());
        Assert.assertEquals(useCiLicense, job.getUseCILicenses());
        Assert.assertEquals(useStrictTestcaseImport, job.getUseStrictTestcaseImport());
        Assert.assertEquals(useRGW3, job.getUseRGW3());
        Assert.assertEquals(useImportedResults, job.getUseImportedResults());
        Assert.assertEquals(useCoverageHistory, job.getUseCoverageHistory());
    }

    private void checkAdditionalTools (NewPipelineJob job,
            final String squoreCommand,
            final String pclpCommand,
            final String pclpResultsPattern,
            final String testInsightsUrl,
            final String tiProxy) {

        Assert.assertEquals(squoreCommand, job.getSquoreCommand());
        Assert.assertEquals(pclpCommand, job.getPclpCommand());
        Assert.assertEquals(pclpResultsPattern, job.getPclpResultsPattern());
        Assert.assertEquals(testInsightsUrl, job.getTestInsightsUrl());
        Assert.assertEquals(tiProxy, job.getTestInsightsProxy());
    }

    @Test
    public void testDefaults() throws Exception {
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("nodeLabel","Test_Node");

        NewPipelineJob job = setupTestBasic(jsonForm);

        Assert.assertEquals(true, job.getUseStrictTestcaseImport());
        Assert.assertEquals(true, job.getUseCoveragePlugin());
        Assert.assertEquals(false, job.getUseCILicenses());
        Assert.assertEquals(true, job.getUseCBT());
        Assert.assertEquals(false, job.getSingleCheckout());
        Assert.assertEquals(false, job.getUseParameters());
        Assert.assertEquals(false, job.getUseRGW3());
        Assert.assertEquals(false, job.getUseCoverageHistory());
        Assert.assertEquals("", job.getSharedArtifactDir());
        Assert.assertEquals("", job.getTestInsightsScmTech());
        Assert.assertNull(job.getEnvironmentSetup());
        Assert.assertNull(job.getExecutePreamble());
        Assert.assertNull(job.getEnvironmentTeardown());
        Assert.assertNull(job.getPostSCMCheckoutCommands());
        Assert.assertEquals("", job.getPipelineSCM());
        Assert.assertEquals(0, job.getMaxParallel().longValue());
    }

    @Test
    public void testAdditionalTools() throws Exception {

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", 1);  // VectorCAST Coverage Plugin
        jsonForm.put("useCoverageHistory", true);
        jsonForm.put("pclpCommand","call lint_my_code.bat");
        jsonForm.put("pclpResultsPattern","lint_results.xml");
        jsonForm.put("TESTinsights_URL","https://teamservices.vector.com/teamareas/pct");
        jsonForm.put("squoreCommand","hello squore test world");
        jsonForm.put("TESTinsights_proxy","TI Proxy 1234@localhost");
        
        NewPipelineJob job = setupTestBasic(jsonForm);
        checkAdditionalTools(job,
                "hello squore test world",
                "call lint_my_code.bat",
                "lint_results.xml",
                "https://teamservices.vector.com/teamareas/pct",
                "TI Proxy 1234@localhost");
    }

    @Test
    public void testCoveragePlugin() throws Exception {

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("coverageDisplayOption", 0);  // Jenkins Coverage Plugin

        NewPipelineJob job = setupTestBasic(jsonForm);

        Assert.assertEquals(true, job.getUseCoveragePlugin());
    }

    @Test
    public void testOptions() throws Exception {
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("nodeLabel","Test_Node");
        jsonForm.put("sharedArtifactDir","/home/jenkins/sharedArtifactDir");
        jsonForm.put("scmSnippet","git 'http://git.com'");
        jsonForm.put("environmentSetup","call setup.bat");
        jsonForm.put("executePreamble","wr_env.bat");
        jsonForm.put("environmentTeardown","close ports");
        jsonForm.put("postSCMCheckoutCommands","chmod a+wr -R *");
        jsonForm.put("coverageDisplayOption",1);
        jsonForm.put("maxParallel",10);

        NewPipelineJob job = setupTestBasic(jsonForm);

        Assert.assertEquals(true, job.getUseStrictTestcaseImport());
        Assert.assertEquals(false, job.getUseCILicenses());
        Assert.assertEquals(true, job.getUseCBT());
        Assert.assertEquals(false, job.getSingleCheckout());
        Assert.assertEquals(false, job.getUseParameters());
        Assert.assertEquals(false, job.getUseRGW3());
        Assert.assertEquals(false, job.getUseCoveragePlugin());
        Assert.assertEquals(false, job.getUseCoverageHistory());
        Assert.assertNotEquals(-1, job.getSharedArtifactDir().indexOf("/home/jenkins/sharedArtifactDir"));
        Assert.assertEquals("git", job.getTestInsightsScmTech());
        Assert.assertEquals("call setup.bat", job.getEnvironmentSetup());
        Assert.assertEquals("wr_env.bat", job.getExecutePreamble());
        Assert.assertEquals("close ports", job.getEnvironmentTeardown());
        Assert.assertEquals("chmod a+wr -R *", job.getPostSCMCheckoutCommands());
        Assert.assertEquals("git 'http://git.com'", job.getPipelineSCM());
        Assert.assertEquals(10, job.getMaxParallel().longValue());
        Assert.assertEquals(false, job.getUseCoveragePlugin());

    }

    @Test
    public void testLocalImportedResults() throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_LOCAL_IMPORTED_RESULTS);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("useImportedResults", true);
        jsonForm.put("importedResults", jsonImportResults);

        NewPipelineJob job = setupTestBasic(jsonForm);

        checkImportedResults(job, USE_LOCAL_IMPORTED_RESULTS, false, "");
    }

    @Test
    public void testExternalImportedResults() throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_EXTERNAL_IMPORTED_RESULTS);
        jsonImportResults.put("externalResultsFilename",EXTERNAL_RESULT_FILENAME);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("useImportedResults", true);
        jsonForm.put("importedResults", jsonImportResults);

        NewPipelineJob job = setupTestBasic(jsonForm);

        checkImportedResults(job, USE_EXTERNAL_IMPORTED_RESULTS, true, EXTERNAL_RESULT_FILENAME);
    }

    @Test
    public void testGitSCM() throws Exception {

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "project.vcm");
        jsonForm.put("TESTinsights_URL","https://teamservices.vector.com/teamareas/pct");
        jsonForm.put("scmSnippet","git 'http://git.com'");

        NewPipelineJob job = setupTestBasic(jsonForm);

        Assert.assertEquals("git", job.getTestInsightsScmTech());
    }

    @Test
    public void testSvnSCM() throws Exception {

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "project.vcm");
        jsonForm.put("TESTinsights_URL","https://teamservices.vector.com/teamareas/pct");
        jsonForm.put("scmSnippet","svn 'http://svn.com'");

        NewPipelineJob job = setupTestBasic(jsonForm);

        Assert.assertEquals("svn", job.getTestInsightsScmTech());
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

