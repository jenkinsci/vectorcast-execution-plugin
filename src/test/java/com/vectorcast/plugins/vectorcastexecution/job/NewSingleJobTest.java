package com.vectorcast.plugins.vectorcastexecution.job;

import com.vectorcast.plugins.vectorcastcoverage.VectorCASTPublisher;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
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
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

import io.jenkins.plugins.coverage.metrics.steps.CoverageRecorder;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool.Parser;
import java.util.List;
import java.io.IOException;
import javax.servlet.ServletException;


import hudson.model.Descriptor.FormException;

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

    /** Jenkins Coverage plugin selection. */
    private static final long USE_COVERAGE_PLUGIN = 1;

    /** VectorCAST Coverage plugin selection. */
    private static final long USE_VCC_PLUGIN = 2;

    private static final String PROJECTNAME = "project.vcast.single";

    private NewSingleJob setupTestBasic(JSONObject jsonForm, JenkinsRule rule) throws ServletException, IOException,
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

        NewSingleJob job = new NewSingleJob(request, response);

        assertEquals("project", job.getBaseName());
        job.create();
        assertEquals(PROJECTNAME, job.getProjectName());
        assertNotNull(job.getTopProject());

        return job;
    }

    private void checkJunitGroovy(DescribableList<Publisher,Descriptor<Publisher>> publisherList, int jUnitIndex, int groovyIndex) {
        // Publisher 1- JUnitResultArchiver
        assertTrue(publisherList.get(jUnitIndex) instanceof JUnitResultArchiver);
        JUnitResultArchiver jUnit = (JUnitResultArchiver)publisherList.get(jUnitIndex);
        assertEquals("**/test_results_*.xml", jUnit.getTestResults());

        // Publisher 5 - GroovyPostbuildRecorder
        assertTrue(publisherList.get(groovyIndex) instanceof GroovyPostbuildRecorder);
        GroovyPostbuildRecorder groovyScript = (GroovyPostbuildRecorder)publisherList.get(groovyIndex);
        assertEquals(/*unstable*/1, groovyScript.getBehavior());

    }

    private void checkArchiverList(ArtifactArchiver archiver, String artifactsList) {
        String artifactsFromArchiver = archiver.getArtifacts();
        assertEquals(artifactsList,artifactsFromArchiver);
        assertFalse(archiver.getAllowEmptyArchive());
    }

    private void checkVectorCASTPublisher(DescribableList<Publisher,Descriptor<Publisher>> publisherList, Boolean useCoverageHistory, int vcPubIndex) {
        // Publisher 2 - VectorCASTPublisher
        assertTrue(publisherList.get(vcPubIndex) instanceof VectorCASTPublisher);
        VectorCASTPublisher vcPublisher = (VectorCASTPublisher)publisherList.get(vcPubIndex);
        assertEquals("**/coverage_results_*.xml", vcPublisher.includes);
        assertEquals(useCoverageHistory, vcPublisher.getUseCoverageHistory());
        assertEquals("**/coverage_results_*.xml", vcPublisher.includes);
        assertEquals(80, vcPublisher.healthReports.getMaxBasisPath());
        assertEquals(0, vcPublisher.healthReports.getMinBasisPath());
        assertEquals(100, vcPublisher.healthReports.getMaxStatement());
        assertEquals(0, vcPublisher.healthReports.getMinStatement());
        assertEquals(70, vcPublisher.healthReports.getMaxBranch());
        assertEquals(0, vcPublisher.healthReports.getMinBranch());
        assertEquals(80, vcPublisher.healthReports.getMaxFunction());
        assertEquals(0, vcPublisher.healthReports.getMinFunction());
        assertEquals(80, vcPublisher.healthReports.getMaxFunctionCall());
        assertEquals(0, vcPublisher.healthReports.getMinFunctionCall());
        assertEquals(80, vcPublisher.healthReports.getMaxMCDC());
        assertEquals(0, vcPublisher.healthReports.getMinMCDC());
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

        JSONObject jsonCovDisplay  = new JSONObject();
        jsonCovDisplay.put("value", USE_VCC_PLUGIN);

        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", jsonCovDisplay);
        jsonForm.put("optionExecutionReport", true);
        jsonForm.put("useStrictTestcaseImport", true);

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        assertEquals(4, publisherList.size());

        // Publisher 0 - ArtifactArchiver
        assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);

        checkBuildWrappers(job, 1);
        checkBuildAction(job,false);
        checkArchiverList(archiver, DEFAULT_ARTIFACT_LIST);
        checkJunitGroovy(publisherList, 1, 3);
        checkVectorCASTPublisher(publisherList, false, 2);
    }

    @Test
    public void testAdditionalTools(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();
        JSONObject jsonCovDisplay  = new JSONObject();
        jsonCovDisplay.put("value", USE_VCC_PLUGIN);

        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", jsonCovDisplay);  // VectorCAST Coverage Plugin
        jsonForm.put("useCoverageHistory", true);
        jsonForm.put("pclpCommand","call lint_my_code.bat");
        jsonForm.put("pclpResultsPattern","lint_results.xml");
        jsonForm.put("squoreCommand","hello squore test world");

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        assertEquals(5, publisherList.size());

        // Publisher 0 - ArtifactArchiver
        assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);

        String addToolArtifacts = DEFAULT_ARTIFACT_LIST;
        addToolArtifacts += ", lint_results.xml";

        checkBuildWrappers(job, 1);
        checkBuildAction(job,false);
        checkArchiverList(archiver, addToolArtifacts);
        checkJunitGroovy(publisherList,2,4);
        checkVectorCASTPublisher(publisherList, true, 3);
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
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", jsonCovDisplay);  // Jenkins Coverage Plugin
        jsonForm.put("useCoverageHistory", false);  // VectorCAST Coverage Plugin
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
        checkJunitGroovy(publisherList, 2, 5);
        checkCoveragePlugin(publisherList, 4);
    }

    @Test
    public void testLocalImportedResults(JenkinsRule rule) throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_LOCAL_IMPORTED_RESULTS);

        JSONObject jsonCovDisplay  = new JSONObject();
        jsonCovDisplay.put("value", USE_COVERAGE_PLUGIN);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", jsonCovDisplay);
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
        checkJunitGroovy(publisherList, 1, 4);
        checkCoveragePlugin(publisherList, 3);
        checkImportedResults(job, USE_LOCAL_IMPORTED_RESULTS, false, "");
    }

    public void testExternalImportedResults(JenkinsRule rule) throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_EXTERNAL_IMPORTED_RESULTS);
        jsonImportResults.put("externalResultsFilename",EXTERNAL_RESULT_FILENAME);

        JSONObject jsonCovDisplay  = new JSONObject();
        jsonCovDisplay.put("value", USE_COVERAGE_PLUGIN);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", jsonCovDisplay);
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
        checkJunitGroovy(publisherList, 1, 4);
        checkCoveragePlugin(publisherList, 3);
        checkImportedResults(job, USE_EXTERNAL_IMPORTED_RESULTS, true, EXTERNAL_RESULT_FILENAME);
    }

    @Test
    public void testDefaultOptions(JenkinsRule rule) throws Exception {

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        checkOptions (job, true, true, false, true, false, false, false);
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

        // cant use Jenkins Coverage with useCoverageHistory
        JSONObject jsonCovDisplay  = new JSONObject();
        jsonCovDisplay.put("value", USE_VCC_PLUGIN);

        jsonForm.put("coverageDisplayOption", jsonCovDisplay);

        NewSingleJob job = setupTestBasic(jsonForm, rule);

        checkOptions (job, true, true, true, true, true, true, true);
    }

    /* TODO: Figure out how to add SCM to be parserd*/
    /* TODO: Specify Job name */
    /* TODO: Multiple jobs with same name */
    /* TODO: MPname set to none */
    /* TODO: MPname without .vcm */
    /* TODO: MPname on network driver abs path \\ */
    /* TODO: MPname on windows abs path */
    /* TODO: MPname on abs path and some SCM */
    /* TODO: use CI license */
    /* TODO: Unix env sections */
    /* TODO: Label not set */
    /* TODO: Windows env sections */
    /* TODO: Post checkout set to "" */
    /* TODO: htmlOrText set to text */
    /* TODO: use Imported results, extFname set to none*/
    /* TODO: different groovy script behaviors */

}
