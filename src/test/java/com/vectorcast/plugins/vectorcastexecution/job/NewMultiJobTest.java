/*
 * The MIT License
 *
 * Copyright 2016 Vector Software, East Greenwich, Rhode Island USA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.vectorcast.plugins.vectorcastexecution.job;

import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.vectorcast.plugins.vectorcastcoverage.VectorCASTPublisher;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTSetup;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.ws_cleanup.PreBuildCleanup;
import hudson.security.Permission;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import hudson.tasks.junit.JUnitResultArchiver;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mockito;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
public class NewMultiJobTest extends TestCase {
    @Rule
    public JenkinsRule r = new JenkinsRule();
    
    private static final String PROJECTNAME = "project.vcast.multi";
    private static final String PROJECT1 = "project_VectorCAST_MinGW_C++_TestSuite_ORDER";
    private static final String PROJECT2 = "project_VectorCAST_MinGW_C_TestSuite_ORDER";
    
    private static final String PROJECTFILE14 = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<project version=\"14\">\n" +
"  <config>\n" +
"    <saved-logs>5</saved-logs>\n" +
"    <build-directory-naming-strategy>COMPRESSED</build-directory-naming-strategy>\n" +
"  </config>\n" +
"  <project-id>1473776183</project-id>\n" +
"  <factor-config-options>0</factor-config-options>\n" +
"  <environment name=\"ORDER\" type=\"UNIT\">\n" +
"    <language>1</language>\n" +
"    <is-monitored>0</is-monitored>\n" +
"    <config>\n" +
"      <original-environment-directory>jenkinsDemo/build/1481489187</original-environment-directory>\n" +
"    </config>\n" +
"  </environment>\n" +
"  <environment name=\"ORDERS\" type=\"UNIT\">\n" +
"    <language>1</language>\n" +
"    <is-monitored>0</is-monitored>\n" +
"    <config>\n" +
"      <original-environment-directory>jenkinsDemo/build/2187318026</original-environment-directory>\n" +
"    </config>\n" +
"  </environment>\n" +
"  <group name=\"Group\">\n" +
"    <environment name=\"ORDER\"/>\n" +
"  </group>\n" +
"  <source-collection name=\"Source\">\n" +
"    <platform name=\"Windows\">\n" +
"      <compiler>\n" +
"        <compiler>\n" +
"          <name>VectorCAST_MinGW_C</name>\n" +
"          <config>\n" +
"            <config>\n" +
"              <key>C_COMPILER_TAG</key>\n" +
"              <value>BUILTIN_MINGW_45_C</value>\n" +
"            </config>\n" +
"          </config>\n" +
"        </compiler>\n" +
"        <testsuite name=\"TestSuite\">\n" +
"          <group name=\"Group\"/>\n" +
"        </testsuite>\n" +
"      </compiler>\n" +
"      <compiler>\n" +
"        <compiler>\n" +
"          <name>VectorCAST_MinGW_C++</name>\n" +
"          <config>\n" +
"            <config>\n" +
"              <key>C_COMPILER_TAG</key>\n" +
"              <value>BUILTIN_MINGW_45_CPP</value>\n" +
"            </config>\n" +
"          </config>\n" +
"        </compiler>\n" +
"        <testsuite name=\"TestSuite\">\n" +
"          <group name=\"Group\"/>\n" +
"        </testsuite>\n" +
"      </compiler>\n" +
"    </platform>\n" +
"  </source-collection>\n" +
"</project>";


    private static final String PROJECTFILE17_645 = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<project version=\"17\">\n" +
"  <config>\n" +
"    <saved-logs>5</saved-logs>\n" +
"    <build-directory-naming-strategy>COMPRESSED</build-directory-naming-strategy>\n" +
"  </config>\n" +
"  <project-id>1473776183</project-id>\n" +
"  <factor-config-options>0</factor-config-options>\n" +
"  <environment name=\"ORDER\" type=\"UNIT\">\n" +
"    <language>1</language>\n" +
"    <is-monitored>0</is-monitored>\n" +
"    <config>\n" +
"      <original-environment-directory>jenkinsDemo/build/1481489187</original-environment-directory>\n" +
"    </config>\n" +
"  </environment>\n" +
"  <environment name=\"ORDERS\" type=\"UNIT\">\n" +
"    <language>1</language>\n" +
"    <is-monitored>0</is-monitored>\n" +
"    <config>\n" +
"      <original-environment-directory>jenkinsDemo/build/2187318026</original-environment-directory>\n" +
"    </config>\n" +
"  </environment>\n" +
"  <group name=\"Group\">\n" +
"    <environment name=\"ORDER\"/>\n" +
"  </group>\n" +
"  <source-collection name=\"Source\">\n" +
"    <platform name=\"Windows\">\n" +
"      <compiler>\n" +
"        <compiler>\n" +
"          <name>VectorCAST_MinGW_C</name>\n" +
"          <config>\n" +
"            <config>\n" +
"              <key>C_COMPILER_TAG</key>\n" +
"              <value>BUILTIN_MINGW_45_C</value>\n" +
"            </config>\n" +
"          </config>\n" +
"        </compiler>\n" +
"        <testsuite name=\"TestSuite\">\n" +
"          <group name=\"Group\"/>\n" +
"        </testsuite>\n" +
"      </compiler>\n" +
"      <compiler>\n" +
"        <compiler>\n" +
"          <name>VectorCAST_MinGW_C++</name>\n" +
"          <config>\n" +
"            <config>\n" +
"              <key>C_COMPILER_TAG</key>\n" +
"              <value>BUILTIN_MINGW_45_CPP</value>\n" +
"            </config>\n" +
"          </config>\n" +
"        </compiler>\n" +
"        <testsuite name=\"TestSuite\">\n" +
"          <group name=\"Group\"/>\n" +
"        </testsuite>\n" +
"      </compiler>\n" +
"    </platform>\n" +
"  </source-collection>\n" +
"</project>";
    
    private static final String PROJECTFILE17 = 
"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
"<project version=\"17\">\n" +
"  <config>\n" +
"    <saved-logs>5</saved-logs>\n" +
"    <build-directory-naming-strategy>COMPRESSED</build-directory-naming-strategy>\n" +
"  </config>\n" +
"  <project-id>1473776183</project-id>\n" +
"  <factor-config-options>0</factor-config-options>\n" +
"  <environment name=\"ORDER\" type=\"UNIT\">\n" +
"    <language>1</language>\n" +
"    <is-monitored>0</is-monitored>\n" +
"    <config>\n" +
"      <original-environment-directory>jenkinsDemo/build/1481489187</original-environment-directory>\n" +
"    </config>\n" +
"  </environment>\n" +
"  <environment name=\"ORDERS\" type=\"UNIT\">\n" +
"    <language>1</language>\n" +
"    <is-monitored>0</is-monitored>\n" +
"    <config>\n" +
"      <original-environment-directory>jenkinsDemo/build/2187318026</original-environment-directory>\n" +
"    </config>\n" +
"  </environment>\n" +
"  <group name=\"Group\">\n" +
"    <environment name=\"ORDER\"/>\n" +
"  </group>\n" +
"      <compiler>\n" +
"        <compiler>\n" +
"          <name>VectorCAST_MinGW_C</name>\n" +
"          <config>\n" +
"            <config>\n" +
"              <key>C_COMPILER_TAG</key>\n" +
"              <value>BUILTIN_MINGW_45_C</value>\n" +
"            </config>\n" +
"          </config>\n" +
"        </compiler>\n" +
"        <testsuite name=\"TestSuite\">\n" +
"          <group name=\"Group\"/>\n" +
"        </testsuite>\n" +
"      </compiler>\n" +
"      <compiler>\n" +
"        <compiler>\n" +
"          <name>VectorCAST_MinGW_C++</name>\n" +
"          <config>\n" +
"            <config>\n" +
"              <key>C_COMPILER_TAG</key>\n" +
"              <value>BUILTIN_MINGW_45_CPP</value>\n" +
"            </config>\n" +
"          </config>\n" +
"        </compiler>\n" +
"        <testsuite name=\"TestSuite\">\n" +
"          <group name=\"Group\"/>\n" +
"        </testsuite>\n" +
"      </compiler>\n" +
"</project>";
    
    @Before
    @Override    public void setUp() throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy mockStrategy = new MockAuthorizationStrategy();
        mockStrategy.grant(Jenkins.READ).everywhere().to("devel");
        for (Permission p : Item.PERMISSIONS.getPermissions()) {
            mockStrategy.grant(p).everywhere().to("devel");
        }
        r.jenkins.setAuthorizationStrategy(mockStrategy);
    }

    public void basicCommon(String projectFile) throws Exception {
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        when(request.getSubmittedForm()).thenReturn(jsonForm);
        
        FileItem fileItem = Mockito.mock(FileItem.class);
        when(request.getFileItem("manageProject")).thenReturn(fileItem);
        when(fileItem.getString()).thenReturn(projectFile);

        NewMultiJob job = new NewMultiJob(request, response, false);
        Assert.assertEquals("project", job.getBaseName());
        job.create(false);
        Assert.assertTrue(job.getTopProject() != null);

        // Check build wrappers - main project...
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList = job.getTopProject().getBuildWrappersList();
        Assert.assertEquals(1, bldWrappersList.size());
        BuildWrapper wrapper = bldWrappersList.get(0);
        Assert.assertTrue(wrapper instanceof PreBuildCleanup);
        PreBuildCleanup cleanup = (PreBuildCleanup)wrapper;
        Assert.assertTrue(cleanup.getDeleteDirs());
        
        // Check build actions - main project...
        DescribableList<Builder,Descriptor<Builder>> bldrsList = job.getTopProject().getBuildersList();
        Assert.assertEquals(5, bldrsList.size());
        Assert.assertTrue(bldrsList.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(bldrsList.get(1) instanceof MultiJobBuilder);
        Assert.assertTrue(bldrsList.get(2) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(3) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(4) instanceof VectorCASTCommand);
        
        // Check publishers - main project...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        checkPublishers(publisherList);
        
        // Now check the additional build/report projects

        MultiJobProject project = (MultiJobProject)job.getTopProject();
        List<FreeStyleProject> projects = project.getParent().getAllItems(FreeStyleProject.class);
        Assert.assertEquals(2, projects.size());
        FreeStyleProject project1 = ((FreeStyleProject)projects.toArray()[0]);
        Assert.assertEquals(PROJECT1, project1.getName());
        FreeStyleProject project2 = ((FreeStyleProject)projects.toArray()[1]);
        Assert.assertEquals(PROJECT2, project2.getName());

        // Build/execute - project 1
        bldrsList = project1.getBuildersList();
        checkBuildExecuteSteps3(bldrsList);

        publisherList = project1.getPublishersList();
        checkPublishers(publisherList);

        // Build/execute - project 2
        bldrsList = project2.getBuildersList();
        checkBuildExecuteSteps3(bldrsList);

        publisherList = project2.getPublishersList();
        checkPublishers(publisherList);
    }

    @Test
    public void testBasic14() throws Exception {
        // Test using version 14 Manage project with 4 levels
        basicCommon(PROJECTFILE14);
    }
    
    @Test
    public void testBasic17_645() throws Exception {
        // Test using version 17 Manage project with only 2 levels
        basicCommon(PROJECTFILE17_645);
    }
    
    @Test
    public void testBasic17() throws Exception {
        // Test using version 17 Manage project with only 2 levels
        basicCommon(PROJECTFILE17);
    }
    
    @Test
    public void testNoReporting() throws Exception {
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionUseReporting", false);
        jsonForm.put("optionClean", false);
        when(request.getSubmittedForm()).thenReturn(jsonForm);
        
        FileItem fileItem = Mockito.mock(FileItem.class);
        when(request.getFileItem("manageProject")).thenReturn(fileItem);
        when(fileItem.getString()).thenReturn(PROJECTFILE14);

        NewMultiJob job = new NewMultiJob(request, response, false);
        Assert.assertEquals("project", job.getBaseName());
        job.create(false);
        Assert.assertTrue(job.getTopProject() != null);

        // Check build wrappers - main project...
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList = job.getTopProject().getBuildWrappersList();
        Assert.assertEquals(0, bldWrappersList.size());
        // No cleanup
        
        // Check build actions - main project...
        DescribableList<Builder,Descriptor<Builder>> bldrsList = job.getTopProject().getBuildersList();
        Assert.assertEquals(5, bldrsList.size());
        Assert.assertTrue(bldrsList.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(bldrsList.get(1) instanceof MultiJobBuilder);
        Assert.assertTrue(bldrsList.get(2) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(3) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(4) instanceof VectorCASTCommand);
        
        // Check publishers - main project...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        Assert.assertEquals(0, publisherList.size());
        
        // Now check the additional build/report projects

        MultiJobProject project = (MultiJobProject)job.getTopProject();
        List<FreeStyleProject> projects = project.getParent().getAllItems(FreeStyleProject.class);
        Assert.assertEquals(2, projects.size());
        FreeStyleProject project1 = ((FreeStyleProject)projects.toArray()[0]);
        Assert.assertEquals(PROJECT1, project1.getName());
        FreeStyleProject project2 = ((FreeStyleProject)projects.toArray()[1]);
        Assert.assertEquals(PROJECT2, project2.getName());
        
        // Build/execute - project 1
        bldrsList = project1.getBuildersList();
        checkBuildExecuteSteps2(bldrsList);

        publisherList = project1.getPublishersList();
        Assert.assertEquals(1, publisherList.size());
        Assert.assertTrue(publisherList.get(0) instanceof GroovyPostbuildRecorder);

        // Build/execute - project 2
        bldrsList = project2.getBuildersList();
        checkBuildExecuteSteps2(bldrsList);

        publisherList = project2.getPublishersList();
        Assert.assertEquals(1, publisherList.size());
        Assert.assertTrue(publisherList.get(0) instanceof GroovyPostbuildRecorder);
    }
    
    private void checkBuildExecuteSteps3(DescribableList<Builder,Descriptor<Builder>> list) {
        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(list.get(1) instanceof VectorCASTCommand);
        Assert.assertTrue(list.get(2) instanceof VectorCASTCommand);
    }

    private void checkBuildExecuteSteps2(DescribableList<Builder,Descriptor<Builder>> list) {
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(list.get(1) instanceof VectorCASTCommand);
    }

    private void checkPublishers(DescribableList<Publisher,Descriptor<Publisher>> list) {
        Assert.assertEquals(4, list.size());
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(list.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)list.get(0);
        Assert.assertEquals("*_rebuild*," +
                             "*_report.html, " +
                            "execution/**, " +
                            "management/**, " +
                            "xml_data/**",
                            archiver.getArtifacts());
        Assert.assertFalse(archiver.getAllowEmptyArchive());
        // Publisher 1- JUnitResultArchiver
        Assert.assertTrue(list.get(1) instanceof JUnitResultArchiver);
        JUnitResultArchiver jUnit = (JUnitResultArchiver)list.get(1);
        Assert.assertEquals("**/test_results_*.xml", jUnit.getTestResults());
        // Publisher 2 - VectorCASTPublisher
        Assert.assertTrue(list.get(2) instanceof VectorCASTPublisher);
        VectorCASTPublisher vcPublisher = (VectorCASTPublisher)list.get(2);
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
        // Publisher 3 - GroovyPostbuildRecorder
        Assert.assertTrue(list.get(3) instanceof GroovyPostbuildRecorder);
        GroovyPostbuildRecorder groovyScript = (GroovyPostbuildRecorder)list.get(3);
        Assert.assertEquals(/*failure*/2, groovyScript.getBehavior());
    }
}
