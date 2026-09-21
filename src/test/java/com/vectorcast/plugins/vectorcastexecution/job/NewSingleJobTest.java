package com.vectorcast.plugins.vectorcastexecution.job;

import com.cloudbees.hudson.plugins.folder.Folder;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTPostBuildPublisher;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTSetup;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.plugins.ws_cleanup.PreBuildCleanup;
import hudson.security.Permission;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import hudson.tasks.junit.JUnitResultArchiver;
import hudson.plugins.copyartifact.CopyArtifact;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;
import org.mockito.Mockito;

import io.jenkins.plugins.coverage.metrics.steps.CoverageRecorder;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool.Parser;
import io.jenkins.plugins.forensics.reference.SimpleReferenceRecorder;
import java.util.List;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.Descriptor.FormException;
import hudson.scm.NullSCM;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class NewSingleJobTest {

    final String DEFAULT_ARTIFACT_LIST = "**/*.html, xml_data/**/*.xml,"
                + "unit_test_*.txt, **/*.png, **/*.css,"
                + "complete_build.log, *_results.vcr";

    final long USE_LOCAL_IMPORTED_RESULTS = 1;
    final long USE_EXTERNAL_IMPORTED_RESULTS = 2;
    final String EXTERNAL_RESULT_FILENAME = "archivedResults/project.vcr";

    private static final String PROJECTNAME = "project_vcast_single";

    private NewSingleJob setupTestBasic(JSONObject jsonForm, JenkinsRule rule) throws ServletException, IOException,
            ExternalResultsFileException, FormException, JobAlreadyExistsException,
            InvalidProjectFileException, Exception {
        return setupTestBasic(jsonForm, rule, "test_single", PROJECTNAME);
    }

    private NewSingleJob setupTestBasic(final JSONObject jsonForm,
                                      final JenkinsRule rule,
                                      final String folderName
        ) throws ServletException, IOException, ExternalResultsFileException,
            FormException, JobAlreadyExistsException,
            InvalidProjectFileException, Exception {
            return setupTestBasic(jsonForm, rule, folderName, PROJECTNAME);
        }

    private NewSingleJob setupTestBasic(final JSONObject jsonForm,
                                          final JenkinsRule rule,
                                          final String folderName,
                                          final String projectName
        ) throws ServletException, IOException, ExternalResultsFileException,
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
        NewSingleJob job = new NewSingleJob(request, response, folder);

        assertEquals("project", job.getBaseName());
        job.create();
        String requestedName = jsonForm.optString("jobName", "").trim();
        String expectedName = requestedName.isEmpty() ? projectName
            : requestedName.replaceAll("[^a-zA-Z0-9_]", "_");
        assertEquals(expectedName, job.getProjectName());
        assertNotNull(job.getTopProject());

        return job;
    }

    private void checkJunitPostBuildPublisher(
            DescribableList<Publisher, Descriptor<Publisher>> publisherList,
            int jUnitIndex, int postBuildIndex) {
        // Publisher 1- JUnitResultArchiver
        assertTrue(publisherList.get(jUnitIndex) instanceof JUnitResultArchiver);
        JUnitResultArchiver jUnit = (JUnitResultArchiver)publisherList.get(jUnitIndex);
        assertEquals("**/test_results_*.xml", jUnit.getTestResults());

        // Final publisher - native VectorCAST post-build result publisher.
        assertTrue(publisherList.get(postBuildIndex)
                instanceof VectorCASTPostBuildPublisher);
        VectorCASTPostBuildPublisher postBuild =
                (VectorCASTPostBuildPublisher) publisherList.get(postBuildIndex);
        assertEquals("project", postBuild.getProjectBase());

    }

    private void checkArchiverList(ArtifactArchiver archiver, String artifactsList) {
        String artifactsFromArchiver = archiver.getArtifacts();
        assertEquals(artifactsList,artifactsFromArchiver);
        assertFalse(archiver.getAllowEmptyArchive());
    }

    private void checkCoveragePlugin(DescribableList<Publisher,Descriptor<Publisher>> publisherList, int pubListIndex) {

        // Publisher 2 - CoverageRecorder
        assertTrue(publisherList.get(pubListIndex) instanceof CoverageRecorder);
        CoverageRecorder publisher = (CoverageRecorder) publisherList.get(pubListIndex);

        // CoverageRecorder > CoverageTool
        List<CoverageTool> coverageToolsList = publisher.getTools();
        assertEquals(1, coverageToolsList.size());
        assertTrue(coverageToolsList.get(0) instanceof CoverageTool);
        CoverageTool coverageTool = coverageToolsList.get(0);

        assertEquals("xml_data/cobertura/coverage_results*.xml", coverageTool.getPattern());
        assertEquals(Parser.VECTORCAST, coverageTool.getParser());
    }

    private void checkReferenceBuildPublisher(
            final DescribableList<Publisher, Descriptor<Publisher>> publisherList,
            final int publisherIndex) {
        assertTrue(publisherList.get(publisherIndex)
                instanceof SimpleReferenceRecorder);
    }

    private void checkBuildWrappers(NewSingleJob job, int builderSize){

        // Check build wrappers...
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList = job.getTopProject().getBuildWrappersList();
        assertEquals(builderSize, bldWrappersList.size());
        BuildWrapper wrapper = bldWrappersList.get(0);
        assertTrue(wrapper instanceof PreBuildCleanup);
        PreBuildCleanup cleanup = (PreBuildCleanup)wrapper;
        assertTrue(cleanup.getDeleteDirs());
    }

    private void checkBuildAction (NewSingleJob job, Boolean checkBuildAction) {
        // Check build actions...
        DescribableList<Builder,Descriptor<Builder>> bldrsList = job.getTopProject().getBuildersList();

        if (checkBuildAction) {
            assertEquals(3, bldrsList.size());
            assertTrue(bldrsList.get(0) instanceof CopyArtifact);
            assertTrue(bldrsList.get(1) instanceof VectorCASTSetup);
            assertTrue(bldrsList.get(2) instanceof VectorCASTCommand);
        } else {
            assertEquals(2, bldrsList.size());
            assertTrue(bldrsList.get(0) instanceof VectorCASTSetup);
            assertTrue(bldrsList.get(1) instanceof VectorCASTCommand);
        }
    }

    private void checkImportedResults(NewSingleJob job, long useLocalResults, Boolean useExternalResults, String externalResultsFilename) {
        if (useLocalResults == USE_LOCAL_IMPORTED_RESULTS) {
            assertTrue(job.getUseLocalImportedResults());
        }
        else if (useLocalResults == USE_EXTERNAL_IMPORTED_RESULTS) {
            assertFalse(job.getUseLocalImportedResults());
        }
        assertEquals(useExternalResults, job.getUseExternalImportedResults());
        assertEquals(externalResultsFilename, job.getExternalResultsFilename());
    }

    private void checkAdditionalTools (NewSingleJob job,
            final String squoreCommand,
            final String pclpCommand,
            final String pclpResultsPattern) {

        assertEquals(squoreCommand, job.getSquoreCommand());
        assertEquals(pclpCommand, job.getPclpCommand());
        assertEquals(pclpResultsPattern, job.getPclpResultsPattern());
    }

    private void checkOptions (NewSingleJob job,
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

    @Test
    public void testBasic(JenkinsRule rule) throws Exception {
        JSONObject jsonForm = new JSONObject();

        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("optionExecutionReport", true);
        jsonForm.put("useStrictTestcaseImport", true);

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        assertEquals(5, publisherList.size());

        // Publisher 0 - ArtifactArchiver
        assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);

        checkBuildWrappers(job, 1);
        checkBuildAction(job,false);
        checkArchiverList(archiver, DEFAULT_ARTIFACT_LIST);
        checkJunitPostBuildPublisher(publisherList, 1, 4);
        checkReferenceBuildPublisher(publisherList, 2);
        checkCoveragePlugin(publisherList, 3);
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

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        assertEquals(6, publisherList.size());

        // Publisher 0 - ArtifactArchiver
        assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);

        String addToolArtifacts = DEFAULT_ARTIFACT_LIST;
        addToolArtifacts += ", lint_results.xml";

        checkBuildWrappers(job, 1);
        checkBuildAction(job,false);
        checkArchiverList(archiver, addToolArtifacts);
        checkJunitPostBuildPublisher(publisherList, 2, 5);
        checkReferenceBuildPublisher(publisherList, 3);
        checkCoveragePlugin(publisherList, 4);
        checkAdditionalTools(job,
                "hello squore test world",
                "call lint_my_code.bat",
                "lint_results.xml");
    }

    @Test
    public void testCoveragePlugin(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();

        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("useCoverageHistory", false);
        jsonForm.put("pclpCommand","call lint_my_code.bat");
        jsonForm.put("pclpResultsPattern","lint_results.xml");

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        assertEquals(6, publisherList.size());

        // Publisher 0 - ArtifactArchiver
        assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);

        String addToolArtifacts = DEFAULT_ARTIFACT_LIST;
        addToolArtifacts += ", lint_results.xml";

        checkBuildWrappers(job, 1);
        checkBuildAction(job,false);
        checkArchiverList(archiver, addToolArtifacts);
        checkJunitPostBuildPublisher(publisherList, 2, 5);
        checkReferenceBuildPublisher(publisherList, 3);
        checkCoveragePlugin(publisherList, 4);
    }

    @Test
    public void testLocalImportedResults(JenkinsRule rule) throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_LOCAL_IMPORTED_RESULTS);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("useImportedResults", true);
        jsonForm.put("importedResults", jsonImportResults);

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        assertEquals(5, publisherList.size());

        // Publisher 0 - ArtifactArchiver
        assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);

        checkBuildWrappers(job, 1);
        checkBuildAction(job, true);
        checkArchiverList(archiver, DEFAULT_ARTIFACT_LIST);
        checkJunitPostBuildPublisher(publisherList, 1, 4);
        checkReferenceBuildPublisher(publisherList, 2);
        checkCoveragePlugin(publisherList, 3);
        checkImportedResults(job, USE_LOCAL_IMPORTED_RESULTS, false, "");
    }

    @Test
    public void testExternalImportedResults(JenkinsRule rule) throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_EXTERNAL_IMPORTED_RESULTS);
        jsonImportResults.put("externalResultsFilename",EXTERNAL_RESULT_FILENAME);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("useImportedResults", true);
        jsonForm.put("importedResults", jsonImportResults);

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        assertEquals(5, publisherList.size());

        // Publisher 0 - ArtifactArchiver
        assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);

        checkBuildWrappers(job, 1);
        checkBuildAction(job,false);
        checkArchiverList(archiver, DEFAULT_ARTIFACT_LIST);
        checkJunitPostBuildPublisher(publisherList, 1, 4);
        checkReferenceBuildPublisher(publisherList, 2);
        checkCoveragePlugin(publisherList, 3);
        checkImportedResults(job, USE_EXTERNAL_IMPORTED_RESULTS, true, EXTERNAL_RESULT_FILENAME);
    }

    @Test
    public void testDefaultOptions(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        checkOptions (job, true, true, false, true, false, false, false);
        assertEquals("built-in",
            job.getTopProject().getAssignedLabel().getName());
    }

    @Test
    public void testFalseOptions(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionExecutionReport", false);
        jsonForm.put("optionUseReporting", false);
        jsonForm.put("useCiLicense",false);
        jsonForm.put("useStrictTestcaseImport", false);
        jsonForm.put("useRGW3",false);
        jsonForm.put("useImportedResults", false);
        jsonForm.put("useCoverageHistory", false);

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        checkOptions (job, false, false, false, false, false, false, false);
    }

    @Test
    public void testTrueOptions(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionExecutionReport", true);
        jsonForm.put("optionUseReporting", true);
        jsonForm.put("useCiLicense",true);
        jsonForm.put("useStrictTestcaseImport", true);
        jsonForm.put("useRGW3",true);
        jsonForm.put("useImportedResults", true);
        jsonForm.put("useCoverageHistory", true);

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        checkOptions (job, true, true, true, true, true, true, true);
    }

    @Test
    public void customFreestyleOptionsAreAppliedToTheGeneratedJob(
            JenkinsRule rule) throws Exception {
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("jobName", "nightly_build/1");
        jsonForm.put("nodeLabel", "  windows-agent   ");
        jsonForm.put("environmentSetupWin", "call setup-win.bat");
        jsonForm.put("executePreambleWin", "call preamble-win.bat");
        jsonForm.put("environmentTeardownWin", "call teardown-win.bat");
        jsonForm.put("environmentSetupUnix", "source setup-unix.sh");
        jsonForm.put("executePreambleUnix", "source preamble-unix.sh");
        jsonForm.put("environmentTeardownUnix", "source teardown-unix.sh");
        jsonForm.put("optionUseReporting", false);
        jsonForm.put("optionErrorLevel", "failure");
        jsonForm.put("optionHtmlBuildDesc", "Text");
        jsonForm.put("optionExecutionReport", false);
        jsonForm.put("optionClean", false);
        jsonForm.put("waitTime", 30);
        jsonForm.put("waitLoops", 7);
        jsonForm.put("maxParallel", 8);
        jsonForm.put("useCiLicense", true);
        jsonForm.put("useStrictTestcaseImport", false);
        jsonForm.put("useRGW3", true);
        NewSingleJob job = setupTestBasic(jsonForm, rule);

        assertEquals("nightly_build_1", job.getProjectName());
        assertEquals("windows-agent",
            job.getTopProject().getAssignedLabel().getName());
        assertInstanceOf(NullSCM.class, job.getTopProject().getScm());
        assertFalse(job.isUsingScm());
        assertEquals(2, job.getTopProject().getBuildersList().size());
        assertTrue(job.getTopProject().getBuildWrappersList().isEmpty());
        assertEquals(2, job.getTopProject().getPublishersList().size());
        assertEquals(2, job.getOptionErrorLevel());
        assertEquals("Text", job.getOptionHTMLBuildDesc());
        assertEquals(30L, job.getWaitTime());
        assertEquals(7L, job.getWaitLoops());
        assertEquals(8L, job.getMaxParallel());

        VectorCASTCommand command = job.getTopProject().getBuildersList()
            .get(VectorCASTCommand.class);
        assertTrue(command.getWinCommand().contains("call setup-win.bat"));
        assertTrue(command.getWinCommand().contains("VCAST_WAIT_TIME=30"));
        assertTrue(command.getWinCommand().contains("VCAST_rptFmt=TEXT"));
        assertTrue(command.getWinCommand().contains("--dont-gen-exec-rpt"));
        assertTrue(command.getUnixCommand().contains("source setup-unix.sh"));
        assertTrue(command.getUnixCommand().contains("VCAST_WAIT_LOOPS=7"));
        assertTrue(command.getUnixCommand().contains("VCAST_USE_CI_LICENSES=1"));
        assertTrue(command.getUnixCommand().contains("VCAST_USE_STRICT_IMPORT=0"));
        assertTrue(command.getUnixCommand().contains("VCAST_USE_RGW3=1"));
    }

    @Test
    public void trimsIdentifiersPathsPatternsAndSelectorsButNotCommands(
            JenkinsRule rule) throws Exception {
        JSONObject importedResults = new JSONObject();
        importedResults.put("value", USE_EXTERNAL_IMPORTED_RESULTS);
        importedResults.put("externalResultsFilename",
            "  archivedResults\\project.vcr  ");
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "  /work/project  ");
        form.put("jobName", "  nightly  ");
        form.put("nodeLabel", "  agent-a  ");
        form.put("optionErrorLevel", "  failure  ");
        form.put("optionHtmlBuildDesc", "  Text  ");
        form.put("pclpResultsPattern", "  lint-results.xml  ");
        form.put("pclpCommand", "  run-lint --all  ");
        form.put("squoreCommand", "  run-squore --all  ");
        form.put("environmentSetupWin", "  call setup.bat  ");
        form.put("executePreambleWin", "  call preamble.bat  ");
        form.put("environmentTeardownWin", "  call teardown.bat  ");
        form.put("environmentSetupUnix", "  . ./setup.sh  ");
        form.put("executePreambleUnix", "  ./preamble.sh  ");
        form.put("environmentTeardownUnix", "  ./teardown.sh  ");
        form.put("useImportedResults", true);
        form.put("importedResults", importedResults);

        NewSingleJob job = setupTestBasic(form, rule, "trimmed-inputs", "nightly");

        assertEquals("/work/project.vcm", job.getManageProjectName());
        assertEquals("nightly", job.getProjectName());
        assertEquals("agent-a", job.getNodeLabel());
        assertEquals(2, job.getOptionErrorLevel());
        assertEquals("Text", job.getOptionHTMLBuildDesc());
        assertEquals("lint-results.xml", job.getPclpResultsPattern());
        assertEquals("archivedResults/project.vcr",
            job.getExternalResultsFilename());
        assertEquals("  run-lint --all  ", job.getPclpCommand());
        assertEquals("  run-squore --all  ", job.getSquoreCommand());
        assertEquals("  call setup.bat  ", job.getEnvironmentSetupWin());
        assertEquals("  call preamble.bat  ", job.getExecutePreambleWin());
        assertEquals("  call teardown.bat  ",
            job.getEnvironmentTeardownWin());
        assertEquals("  . ./setup.sh  ", job.getEnvironmentSetupUnix());
        assertEquals("  ./preamble.sh  ", job.getExecutePreambleUnix());
        assertEquals("  ./teardown.sh  ",
            job.getEnvironmentTeardownUnix());
    }

    @Test
    public void externalImportWithoutAFilenameIsRejected(JenkinsRule rule)
            throws Exception {
        JSONObject importedResults = new JSONObject();
        importedResults.put("value", USE_EXTERNAL_IMPORTED_RESULTS);
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        form.put("useImportedResults", true);
        form.put("importedResults", importedResults);

        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        when(request.getSubmittedForm()).thenReturn(form);
        Folder folder = rule.jenkins.createProject(Folder.class, "invalid-import");

        assertThrows(ExternalResultsFileException.class,
            () -> new NewSingleJob(request, response, folder));
    }

    @Test
    public void normalizesUnixWindowsAndUncManageProjectPaths(JenkinsRule rule)
            throws Exception {
        JSONObject unix = new JSONObject();
        unix.put("manageProjectName", "/work/project");
        NewSingleJob unixJob = newJobForPathTest(unix, rule, "unix-path");
        assertEquals("/work/project.vcm", unixJob.getManageProjectName());
        assertEquals("project", unixJob.getBaseName());

        JSONObject windows = new JSONObject();
        windows.put("manageProjectName", "C:\\work\\project.vcm");
        NewSingleJob windowsJob = newJobForPathTest(windows, rule,
            "windows-path");
        assertEquals("C:/work/project.vcm", windowsJob.getManageProjectName());

        JSONObject unc = new JSONObject();
        unc.put("manageProjectName", "\\\\server\\share\\project");
        NewSingleJob uncJob = newJobForPathTest(unc, rule, "unc-path");
        assertEquals("//server/share/project.vcm", uncJob.getManageProjectName());
    }

    /**
     * Absolute project locations are valid for a Freestyle job without SCM.
     * SCM jobs deliberately use a project file relative to the workspace; the
     * live SCM binding itself is exercised in {@link NewSingleJobScmTest}.
     */
    @Test
    public void createsFreestyleJobsForAbsolutePathsWithoutScm(
            JenkinsRule rule) throws Exception {
        JSONObject windows = new JSONObject();
        windows.put("manageProjectName", "C:\\work\\project.vcm");
        NewSingleJob windowsJob = setupTestBasic(windows, rule,
            "windows-absolute-path");
        assertEquals("C:/work/project.vcm",
            windowsJob.getManageProjectName());
        assertInstanceOf(NullSCM.class, windowsJob.getTopProject().getScm());

        JSONObject unc = new JSONObject();
        unc.put("manageProjectName", "\\\\server\\share\\project.vcm");
        NewSingleJob uncJob = setupTestBasic(unc, rule, "unc-absolute-path");
        assertEquals("//server/share/project.vcm", uncJob.getManageProjectName());
        assertInstanceOf(NullSCM.class, uncJob.getTopProject().getScm());
    }

    @Test
    public void emptyManageProjectNameIsRejectedBeforeCreatingAJob(
            JenkinsRule rule) throws Exception {
        JSONObject form = new JSONObject();
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        when(request.getSubmittedForm()).thenReturn(form);
        NewSingleJob job = new NewSingleJob(request, response,
            rule.jenkins.createProject(Folder.class, "empty-project"));

        assertNull(job.createProject());
        Mockito.verify(response).sendError(HttpServletResponse.SC_NOT_MODIFIED,
            "No project name specified");
    }

    @Test
    public void identifiesAnExistingGeneratedFreestyleName(JenkinsRule rule)
            throws Exception {
        JSONObject form = new JSONObject();
        form.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        NewSingleJob job = setupTestBasic(form, rule);

        assertThrows(JobAlreadyExistsException.class,
            () -> job.checkIfProjectExists(PROJECTNAME));
    }

    private NewSingleJob newJobForPathTest(final JSONObject form,
            final JenkinsRule rule, final String folderName) throws Exception {
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        when(request.getSubmittedForm()).thenReturn(form);
        return new NewSingleJob(request, response,
            rule.jenkins.createProject(Folder.class, folderName));
    }
    /*
     * Coverage inventory:
     * - NewSingleJobScmTest exercises real rendered-form SCM binding.
     * - customFreestyleOptionsAreAppliedToTheGeneratedJob covers job-name
     *   normalization, CI licensing, labels, Windows/Unix sections, and text
     *   report generation.
     * - The path tests cover absent extensions plus Unix, Windows, and UNC
     *   absolute paths without SCM. An SCM checkout uses a workspace-relative
     *   project path, so absolute-path plus SCM is intentionally not a
     *   generated-job scenario.
     * - NewSingleJob has no post-checkout field and no Groovy post-build
     *   behavior: it installs VectorCASTPostBuildPublisher natively.
     */

}
