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

    private static final String PROJECTNAME = "project_vcast_pipeline";

    private static final String FOLDERNAME = "test_pipeline_folder";
    private NewPipelineJob setupTestBasic(JSONObject jsonForm, JenkinsRule rule) throws ServletException, IOException,
            ExternalResultsFileException, FormException, JobAlreadyExistsException,
            InvalidProjectFileException, Exception {
        return setupTestBasic(jsonForm, rule, FOLDERNAME);
    }

    private NewPipelineJob setupTestBasic(final JSONObject jsonForm,
                                      final JenkinsRule rule, final String folderName)
        throws ServletException, IOException, ExternalResultsFileException,
        FormException, JobAlreadyExistsException,
        InvalidProjectFileException, Exception {
            return setupTestBasic(jsonForm, rule, folderName, PROJECTNAME);
        }

    private NewPipelineJob setupTestBasic(final JSONObject jsonForm,
                                          final JenkinsRule rule,
                                          final String folderName,
                                          final String projectName)
            throws ServletException, IOException, ExternalResultsFileException,
            FormException, JobAlreadyExistsException,
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

        Folder folder = rule.jenkins.createProject(Folder.class, folderName);

        NewPipelineJob job = new NewPipelineJob(request, response, folder);

        assertEquals("project", job.getBaseName());
        job.create();
        assertEquals(projectName, job.getProjectName());
        assertEquals(folderName, job.getFolder().getName());

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
        jsonForm.put("nodeLabel","  Test_Node   ");

        NewPipelineJob job = setupTestBasic(jsonForm, rule);

        assertEquals(true, job.getUseStrictTestcaseImport());
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
        String generatedConfig = ((hudson.model.AbstractItem) job.getFolder()
            .getItem(job.getProjectName())).getConfigFile().asString();
        assertTrue(generatedConfig.contains(
            "def VC_Manage_Project = '/home/jenkins/vcast/project.vcm'"));
        assertTrue(generatedConfig.contains("def VC_usingSCM = false"));
        assertTrue(generatedConfig.contains("def VC_useCBT = \"--incremental\""));
        assertTrue(generatedConfig.contains("def VC_useStrictImport = true"));
    }

    @Test
    public void testAdditionalTools(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();

        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
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
    public void testOptions(JenkinsRule rule) throws Exception {
        JSONObject jsonForm = new JSONObject();

        jsonForm.put("manageProjectName", "project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("nodeLabel","  Test_Node   ");
        jsonForm.put("sharedArtifactDir","/home/jenkins/sharedArtifactDir");
        jsonForm.put("scmSnippet","git 'http://git.com'");
        jsonForm.put("environmentSetup","call setup.bat");
        jsonForm.put("executePreamble","wr_env.bat");
        jsonForm.put("environmentTeardown","close ports");
        jsonForm.put("postSCMCheckoutCommands","chmod a+wr -R *");
        jsonForm.put("maxParallel",10);

        NewPipelineJob job = setupTestBasic(jsonForm, rule);

        assertEquals(true, job.getUseStrictTestcaseImport());
        assertEquals(false, job.getUseCILicenses());
        assertEquals(true, job.getUseCBT());
        assertEquals(false, job.getSingleCheckout());
        assertEquals(false, job.getUseParameters());
        assertEquals(false, job.getUseRGW3());
        assertEquals(false, job.getUseCoverageHistory());
        assertEquals("Test_Node", job.getNodeLabel());
        assertNotEquals(-1, job.getSharedArtifactDir().indexOf("/home/jenkins/sharedArtifactDir"));
        assertEquals("call setup.bat", job.getEnvironmentSetup());
        assertEquals("wr_env.bat", job.getExecutePreamble());
        assertEquals("close ports", job.getEnvironmentTeardown());
        assertEquals("chmod a+wr -R *", job.getPostSCMCheckoutCommands());
        assertEquals("git 'http://git.com'", job.getPipelineSCM());
        assertEquals(10, job.getMaxParallel().longValue());
        String generatedConfig = ((hudson.model.AbstractItem) job.getFolder()
            .getItem(job.getProjectName())).getConfigFile().asString();
        assertTrue(generatedConfig.contains(
            "def scmStep () { git 'http://git.com' }"));
        assertTrue(generatedConfig.contains("def VC_usingSCM = true"));
        assertTrue(generatedConfig.contains("recordCoverage tools:"));
        assertFalse(generatedConfig.contains("VC_useCoveragePlugin"));
        assertFalse(generatedConfig.contains("VC_Healthy_Target"));
        assertFalse(generatedConfig.contains("VC_Use_Threshold"));
        assertFalse(generatedConfig.contains("useCoverPlgin:"));
        assertTrue(generatedConfig.contains(
            "def VC_sharedArtifactDirectory = \"--workspace=/home/jenkins/sharedArtifactDir\""));
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
        String generatedConfig = ((hudson.model.AbstractItem) job.getFolder()
            .getItem(job.getProjectName())).getConfigFile().asString();
        assertTrue(generatedConfig.contains("def VC_useImportedResults = true"));
        assertTrue(generatedConfig.contains("def VC_useLocalImportedResults = true"));
        assertTrue(generatedConfig.contains("def VC_useExternalImportedResults = false"));
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
        String generatedConfig = ((hudson.model.AbstractItem) job.getFolder()
            .getItem(job.getProjectName())).getConfigFile().asString();
        assertTrue(generatedConfig.contains("def VC_useImportedResults = true"));
        assertTrue(generatedConfig.contains("def VC_useLocalImportedResults = false"));
        assertTrue(generatedConfig.contains("def VC_useExternalImportedResults = true"));
        assertTrue(generatedConfig.contains(
            "def VC_externalResultsFilename = \"archivedResults/project.vcr\""));
    }

    @Test
    public void parameterizedPipelineUsesNamedJobAndOptionalFlags(
            JenkinsRule rule) throws Exception {
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "  project  ");
        form.put("jobName", "  nightly_pipeline  ");
        form.put("nodeLabel", "  linux-agent  ");
        form.put("sharedArtifactDir", "  C:\\shared\\artifacts  ");
        form.put("scmSnippet", "  git 'https://example.invalid/project.git'  ");
        form.put("useParameters", true);
        form.put("useCBT", false);
        form.put("useCiLicense", true);
        form.put("environmentSetup", "");
        form.put("executePreamble", "");
        form.put("environmentTeardown", "");
        form.put("postSCMCheckoutCommands", "");

        NewPipelineJob job = setupTestBasic(form, rule,
            "parameterized-pipeline", "nightly_pipeline");

        assertEquals("project.vcm", job.getManageProjectName());
        assertEquals("nightly_pipeline", job.getProjectName());
        assertEquals("linux-agent", job.getNodeLabel());
        assertEquals("--workspace=C:/shared/artifacts",
            job.getSharedArtifactDir());
        assertEquals("git 'https://example.invalid/project.git'",
            job.getPipelineSCM());
        assertTrue(job.getUseParameters());
        assertFalse(job.getUseCBT());
        assertTrue(job.getUseCILicenses());
        assertEquals("", job.getEnvironmentSetup());
        assertEquals("", job.getExecutePreamble());
        assertEquals("", job.getEnvironmentTeardown());
        assertEquals("", job.getPostSCMCheckoutCommands());

        hudson.model.AbstractItem created = assertInstanceOf(
            hudson.model.AbstractItem.class,
            job.getFolder().getItem(job.getProjectName()));
        String generatedConfig = created.getConfigFile().asString();
        assertTrue(generatedConfig
            .contains("hudson.model.StringParameterDefinition"));
        assertTrue(generatedConfig
            .contains("def VC_Manage_Project = 'project.vcm'"));
        assertTrue(generatedConfig.contains("def VC_useCBT = \"\""));
        assertTrue(generatedConfig
            .contains("def VC_useCILicense = \"--ci\""));
        assertTrue(generatedConfig.contains(
            "def scmStep () { git 'https://example.invalid/project.git' }"));
    }

    @Test
    public void duplicatePipelineNameIsRejected(JenkinsRule rule)
            throws Exception {
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "project.vcm");
        NewPipelineJob existing = setupTestBasic(form, rule,
            "duplicate-pipeline");

        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        when(request.getSubmittedForm()).thenReturn(form);
        NewPipelineJob duplicate = new NewPipelineJob(request, response,
            existing.getFolder());

        assertThrows(JobAlreadyExistsException.class, duplicate::createProject);
    }

    @Test
    public void normalizesAbsolutePathsWithoutScmAndRejectsThemWithScm(
            JenkinsRule rule) throws Exception {
        StaplerResponse response = Mockito.mock(StaplerResponse.class);

        JSONObject windows = new JSONObject();
        windows.put("manageProjectName", "  C:\\work\\project  ");
        StaplerRequest windowsRequest = Mockito.mock(StaplerRequest.class);
        when(windowsRequest.getSubmittedForm()).thenReturn(windows);
        NewPipelineJob windowsJob = new NewPipelineJob(windowsRequest, response,
            rule.jenkins.createProject(Folder.class, "windows-pipeline"));
        assertEquals("C:/work/project.vcm", windowsJob.getManageProjectName());

        JSONObject unc = new JSONObject();
        unc.put("manageProjectName", "\\\\server\\share\\project.vcm");
        StaplerRequest uncRequest = Mockito.mock(StaplerRequest.class);
        when(uncRequest.getSubmittedForm()).thenReturn(unc);
        NewPipelineJob uncJob = new NewPipelineJob(uncRequest, response,
            rule.jenkins.createProject(Folder.class, "unc-pipeline"));
        assertEquals("//server/share/project.vcm", uncJob.getManageProjectName());

        JSONObject conflicting = new JSONObject();
        conflicting.put("manageProjectName", "C:\\work\\project.vcm");
        conflicting.put("scmSnippet", "git 'https://example.invalid/project.git'");
        StaplerRequest conflictRequest = Mockito.mock(StaplerRequest.class);
        when(conflictRequest.getSubmittedForm()).thenReturn(conflicting);
        assertThrows(ScmConflictException.class,
            () -> new NewPipelineJob(conflictRequest, response,
                rule.jenkins.createProject(Folder.class, "scm-conflict")));
    }

    @Test
    public void externalImportWithoutFilenameIsRejected(JenkinsRule rule)
            throws Exception {
        JSONObject importedResults = new JSONObject();
        importedResults.put("value", USE_EXTERNAL_IMPORTED_RESULTS);
        importedResults.put("externalResultsFilename", "   ");
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "project.vcm");
        form.put("useImportedResults", true);
        form.put("importedResults", importedResults);
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        when(request.getSubmittedForm()).thenReturn(form);

        assertThrows(ExternalResultsFileException.class,
            () -> new NewPipelineJob(request, response,
                rule.jenkins.createProject(Folder.class, "blank-external")));
    }

    @Test
    public void preservesWhitespaceInPipelineScriptBodies(JenkinsRule rule)
            throws Exception {
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "project.vcm");
        form.put("environmentSetup", "  source setup.sh  ");
        form.put("executePreamble", "  ./preamble.sh  ");
        form.put("environmentTeardown", "  ./teardown.sh  ");
        form.put("postSCMCheckoutCommands", "  git submodule update  ");
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        when(request.getSubmittedForm()).thenReturn(form);

        NewPipelineJob job = new NewPipelineJob(request, response,
            rule.jenkins.createProject(Folder.class, "script-whitespace"));

        assertEquals("  source setup.sh  ", job.getEnvironmentSetup());
        assertEquals("  ./preamble.sh  ", job.getExecutePreamble());
        assertEquals("  ./teardown.sh  ", job.getEnvironmentTeardown());
        assertEquals("  git submodule update  ",
            job.getPostSCMCheckoutCommands());
    }


}
