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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

public class NewSingleJobTest {

    final String DEFAULT_ARTIFACT_LIST = "**/*.html, xml_data/*.xml, unit_test_fail_count.txt, **/*.png, **/*.css, complete_build.log, *_results.vcr";
    final long USE_LOCAL_IMPORTED_RESULTS = 1;
    final long USE_EXTERNAL_IMPORTED_RESULTS = 2;
    final String EXTERNAL_RESULT_FILENAME = "archivedResults/project.vcr";

    @Rule 
    public JenkinsRule j = new JenkinsRule();
    private static final String PROJECTNAME = "project.vcast.single";

    @BeforeEach
    void setUpStaticMocks() {
    }

    @AfterEach
    void tearDownStaticMocks() {
    }

    private NewSingleJob setupTestBasic(JSONObject jsonForm) throws ServletException, IOException, 
            ExternalResultsFileException, FormException, JobAlreadyExistsException,
            InvalidProjectFileException {
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

        NewSingleJob job = new NewSingleJob(request, response);
        
        Assert.assertEquals("project", job.getBaseName());
        job.create(false);
        Assert.assertEquals(PROJECTNAME, job.getProjectName());
        Assert.assertNotNull(job.getTopProject());

        return job;
    }
    
    private void checkJunitGroovy(DescribableList<Publisher,Descriptor<Publisher>> publisherList, int jUnitIndex, int groovyIndex) {
        // Publisher 1- JUnitResultArchiver
        Assert.assertTrue(publisherList.get(jUnitIndex) instanceof JUnitResultArchiver);
        JUnitResultArchiver jUnit = (JUnitResultArchiver)publisherList.get(jUnitIndex);
        Assert.assertEquals("**/test_results_*.xml", jUnit.getTestResults());
        
        // Publisher 3 - GroovyPostbuildRecorder
        Assert.assertTrue(publisherList.get(groovyIndex) instanceof GroovyPostbuildRecorder);
        GroovyPostbuildRecorder groovyScript = (GroovyPostbuildRecorder)publisherList.get(groovyIndex);
        Assert.assertEquals(/*failure*/2, groovyScript.getBehavior());    

    }
    
    private void checkArchiverList(ArtifactArchiver archiver, String artifactsList) {
        String artifactsFromArchiver = archiver.getArtifacts();
        Assert.assertEquals(artifactsList,artifactsFromArchiver);
        Assert.assertFalse(archiver.getAllowEmptyArchive());
    }
    
    private void checkVectorCASTPublisher(DescribableList<Publisher,Descriptor<Publisher>> publisherList, Boolean useCoverageHistory, int vcPubIndex) {
        // Publisher 2 - VectorCASTPublisher
        Assert.assertTrue(publisherList.get(vcPubIndex) instanceof VectorCASTPublisher);
        VectorCASTPublisher vcPublisher = (VectorCASTPublisher)publisherList.get(vcPubIndex);
        Assert.assertEquals("**/coverage_results_*.xml", vcPublisher.includes);
        Assert.assertEquals(useCoverageHistory, vcPublisher.getUseCoverageHistory());
        Assert.assertEquals("**/coverage_results_*.xml", vcPublisher.includes);
        Assert.assertEquals(80, vcPublisher.healthReports.getMaxBasisPath());
        Assert.assertEquals(0, vcPublisher.healthReports.getMinBasisPath());
        Assert.assertEquals(100, vcPublisher.healthReports.getMaxStatement());
        Assert.assertEquals(0, vcPublisher.healthReports.getMinStatement());
        Assert.assertEquals(70, vcPublisher.healthReports.getMaxBranch());
        Assert.assertEquals(0, vcPublisher.healthReports.getMinBranch());
        Assert.assertEquals(80, vcPublisher.healthReports.getMaxFunction());
        Assert.assertEquals(0, vcPublisher.healthReports.getMinFunction());
        Assert.assertEquals(80, vcPublisher.healthReports.getMaxFunctionCall());
        Assert.assertEquals(0, vcPublisher.healthReports.getMinFunctionCall());
        Assert.assertEquals(80, vcPublisher.healthReports.getMaxMCDC());
        Assert.assertEquals(0, vcPublisher.healthReports.getMinMCDC());
    }
    
    private void checkCoveragePlugin(DescribableList<Publisher,Descriptor<Publisher>> publisherList, int pubListIndex) {
        
        // Publisher 2 - CoverageRecorder
        Assert.assertTrue(publisherList.get(pubListIndex) instanceof CoverageRecorder);
        CoverageRecorder publisher = (CoverageRecorder) publisherList.get(pubListIndex);
        
        // CoverageRecorder > CoverageTool
        List<CoverageTool> coverageToolsList = publisher.getTools();
        Assert.assertEquals(1, coverageToolsList.size());
        Assert.assertTrue(coverageToolsList.get(0) instanceof CoverageTool);
        CoverageTool coverageTool = (CoverageTool)coverageToolsList.get(0);

        Assert.assertEquals("xml_data/cobertura/coverage_results*.xml", coverageTool.getPattern());
        Assert.assertEquals(Parser.VECTORCAST, coverageTool.getParser());        
    }
    
    private void checkBuildWrappers(NewSingleJob job, int builderSize){
        
        // Check build wrappers...
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList = job.getTopProject().getBuildWrappersList();
        Assert.assertEquals(builderSize, bldWrappersList.size());
        BuildWrapper wrapper = bldWrappersList.get(0);
        Assert.assertTrue(wrapper instanceof PreBuildCleanup);
        PreBuildCleanup cleanup = (PreBuildCleanup)wrapper;
        Assert.assertTrue(cleanup.getDeleteDirs());
    }
 
    private void checkBuildAction (NewSingleJob job) {
        // Check build actions...
        DescribableList<Builder,Descriptor<Builder>> bldrsList = job.getTopProject().getBuildersList();
        Assert.assertEquals(3, bldrsList.size());
        Assert.assertTrue(bldrsList.get(0) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(1) instanceof VectorCASTSetup);
        Assert.assertTrue(bldrsList.get(2) instanceof VectorCASTCommand);
    }

    private void checkImportedResults(NewSingleJob job, long useLocalResults, Boolean useExternalResults, String externalResultsFilename) {
        if (useLocalResults == USE_LOCAL_IMPORTED_RESULTS) {
            Assert.assertTrue(job.getUseLocalImportedResults());
        }
        else if (useLocalResults == USE_EXTERNAL_IMPORTED_RESULTS) {
            Assert.assertFalse(job.getUseLocalImportedResults());
        }
        Assert.assertEquals(useExternalResults, job.getUseExternalImportedResults());
        Assert.assertEquals(externalResultsFilename, job.getExternalResultsFilename());
    }

    @Test 
    public void testBasic() throws Exception {
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", 1);

        NewSingleJob job = setupTestBasic(jsonForm);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        Assert.assertEquals(4, publisherList.size());
        
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);
        
        checkBuildWrappers(job, 1);
        checkBuildAction(job);
        checkArchiverList(archiver, DEFAULT_ARTIFACT_LIST);
        checkJunitGroovy(publisherList, 1, 3);
        checkVectorCASTPublisher(publisherList, false, 2);
        
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

        NewSingleJob job = setupTestBasic(jsonForm);
                
        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        Assert.assertEquals(5, publisherList.size());
        
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);
        
        String addToolArtifacts = DEFAULT_ARTIFACT_LIST;
        addToolArtifacts += ", lint_results.xml";
        addToolArtifacts += ", TESTinsights_Push.log";
        
        checkBuildWrappers(job, 2);
        checkBuildAction(job);
        checkArchiverList(archiver, addToolArtifacts);
        checkJunitGroovy(publisherList,2,4);
        checkVectorCASTPublisher(publisherList, true, 3);
    }

    @Test 
    public void testCoveragePlugin() throws Exception {
        
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", 0);  // Jenkins Coverage Plugin
        jsonForm.put("useCoverageHistory", false);  // VectorCAST Coverage Plugin
        jsonForm.put("pclpCommand","call lint_my_code.bat");
        jsonForm.put("pclpResultsPattern","lint_results.xml");
        jsonForm.put("TESTinsights_URL","https://teamservices.vector.com/teamareas/pct");

        NewSingleJob job = setupTestBasic(jsonForm);
                
        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        Assert.assertEquals(5, publisherList.size());
        
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);
        
        String addToolArtifacts = DEFAULT_ARTIFACT_LIST;
        addToolArtifacts += ", lint_results.xml";
        addToolArtifacts += ", TESTinsights_Push.log";
        
        checkBuildWrappers(job, 2);
        checkBuildAction(job);
        checkArchiverList(archiver, addToolArtifacts);
        checkJunitGroovy(publisherList, 2, 4);
        checkCoveragePlugin(publisherList, 3);
    }
    
    @Test 
    public void testLocalImportedResults() throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_LOCAL_IMPORTED_RESULTS);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", 0);
        jsonForm.put("useImportedResults", true);
        jsonForm.put("importedResults", jsonImportResults);

        NewSingleJob job = setupTestBasic(jsonForm);
                
        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        Assert.assertEquals(4, publisherList.size());
        
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);

        checkBuildWrappers(job, 1);
        checkBuildAction(job);
        checkArchiverList(archiver, DEFAULT_ARTIFACT_LIST);
        checkJunitGroovy(publisherList, 1, 3);
        checkCoveragePlugin(publisherList, 2);
        checkImportedResults(job, USE_LOCAL_IMPORTED_RESULTS, false, "");
    }
    
    @Test 
    public void testExternalImportedResults() throws Exception {

        JSONObject jsonImportResults  = new JSONObject();
        jsonImportResults.put("value", USE_EXTERNAL_IMPORTED_RESULTS);
        jsonImportResults.put("externalResultsFilename",EXTERNAL_RESULT_FILENAME);

        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        jsonForm.put("coverageDisplayOption", 0);
        jsonForm.put("useImportedResults", true);
        jsonForm.put("importedResults", jsonImportResults);

        NewSingleJob job = setupTestBasic(jsonForm);
                
        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        Assert.assertEquals(4, publisherList.size());
        
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);


        checkBuildWrappers(job, 1);
        checkBuildAction(job);
        checkArchiverList(archiver, DEFAULT_ARTIFACT_LIST);
        checkJunitGroovy(publisherList, 1, 3);
        checkCoveragePlugin(publisherList, 2);
        checkImportedResults(job, USE_EXTERNAL_IMPORTED_RESULTS, true, EXTERNAL_RESULT_FILENAME);
    }
}

