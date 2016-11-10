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
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.RootAction;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.ws_cleanup.PreBuildCleanup;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.jenkinsci.plugins.scriptsecurity.scripts.Language;
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval;
import org.jenkinsci.plugins.scriptsecurity.scripts.languages.GroovyLanguage;
import org.jenkinsci.plugins.xunit.XUnitPublisher;
import org.jenkinsci.plugins.xunit.types.CheckType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
public class NewMultiJobTest extends TestCase {
    @Mock
    private Jenkins mockJenkins;
    @Mock
    private MultiJobProject project;
    private DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList;
    private DescribableList<Builder,Descriptor<Builder>> bldrsList;
    private DescribableList<Publisher,Descriptor<Publisher>> publisherList;
    
    @Mock
    private FreeStyleProject project1;
    private static final String PROJECT1 = "project_VectorCAST_MinGW_C++_TestSuite_ORDER_BuildExecute";
    private DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList1;
    private DescribableList<Builder,Descriptor<Builder>> bldrsList1;
    private DescribableList<Publisher,Descriptor<Publisher>> publisherList1;
    @Mock
    private FreeStyleProject project2;
    private static final String PROJECT2 = "project_VectorCAST_MinGW_C++_TestSuite_ORDER_Reporting";
    private DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList2;
    private DescribableList<Builder,Descriptor<Builder>> bldrsList2;
    private DescribableList<Publisher,Descriptor<Publisher>> publisherList2;
    @Mock
    private FreeStyleProject project3;
    private static final String PROJECT3 = "project_VectorCAST_MinGW_C_TestSuite_ORDER_BuildExecute";
    private DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList3;
    private DescribableList<Builder,Descriptor<Builder>> bldrsList3;
    private DescribableList<Publisher,Descriptor<Publisher>> publisherList3;
    @Mock
    private FreeStyleProject project4;
    private static final String PROJECT4 = "project_VectorCAST_MinGW_C_TestSuite_ORDER_Reporting";
    private DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList4;
    private DescribableList<Builder,Descriptor<Builder>> bldrsList4;
    private DescribableList<Publisher,Descriptor<Publisher>> publisherList4;

    @Mock
    private ScriptApproval scriptApproval;
    @Mock
    private ExtensionList<RootAction> rootActionList;
    @Mock
    private ExtensionList<Language> langList;
    
    private static final String PROJECTNAME = "project.vcast_manage.multijob";
    private static final String PROJECTFILE = 
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
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(mockJenkins);
        
        when(mockJenkins.createProject(MultiJobProject.class, PROJECTNAME)).thenReturn(project);
        when(mockJenkins.createProject(FreeStyleProject.class, PROJECT1)).thenReturn(project1);
        when(mockJenkins.createProject(FreeStyleProject.class, PROJECT2)).thenReturn(project2);
        when(mockJenkins.createProject(FreeStyleProject.class, PROJECT3)).thenReturn(project3);
        when(mockJenkins.createProject(FreeStyleProject.class, PROJECT4)).thenReturn(project4);

        when(mockJenkins.getExtensionList(RootAction.class)).thenReturn(rootActionList);
        when(mockJenkins.getExtensionList(Language.class)).thenReturn(langList);
        
        GroovyLanguage groovy = new GroovyLanguage();
        when(langList.get(GroovyLanguage.class)).thenReturn(groovy);
        
        bldWrappersList = new DescribableList(project);
        when(project.getBuildWrappersList()).thenReturn(bldWrappersList);
        bldWrappersList1 = new DescribableList(project1);
        when(project1.getBuildWrappersList()).thenReturn(bldWrappersList1);
        bldWrappersList2 = new DescribableList(project2);
        when(project2.getBuildWrappersList()).thenReturn(bldWrappersList2);
        bldWrappersList3 = new DescribableList(project3);
        when(project3.getBuildWrappersList()).thenReturn(bldWrappersList3);
        bldWrappersList4 = new DescribableList(project4);
        when(project4.getBuildWrappersList()).thenReturn(bldWrappersList4);

        bldrsList = new DescribableList<>(project);
        when(project.getBuildersList()).thenReturn(bldrsList);
        bldrsList1 = new DescribableList<>(project1);
        when(project1.getBuildersList()).thenReturn(bldrsList1);
        bldrsList2 = new DescribableList<>(project2);
        when(project2.getBuildersList()).thenReturn(bldrsList2);
        bldrsList3 = new DescribableList<>(project3);
        when(project3.getBuildersList()).thenReturn(bldrsList3);
        bldrsList4 = new DescribableList<>(project4);
        when(project4.getBuildersList()).thenReturn(bldrsList4);

        publisherList = new DescribableList<>(project);
        when(project.getPublishersList()).thenReturn(publisherList);
        publisherList1 = new DescribableList<>(project1);
        when(project1.getPublishersList()).thenReturn(publisherList1);
        publisherList2 = new DescribableList<>(project2);
        when(project2.getPublishersList()).thenReturn(publisherList2);
        publisherList3 = new DescribableList<>(project3);
        when(project3.getPublishersList()).thenReturn(publisherList3);
        publisherList4 = new DescribableList<>(project4);
        when(project4.getPublishersList()).thenReturn(publisherList4);
        
        when(rootActionList.get(ScriptApproval.class)).thenReturn(scriptApproval);
        mockStatic(ScriptApproval.class);
        when(ScriptApproval.get()).thenReturn(scriptApproval);
    }

    @Test
    public void testBasic() throws Exception {
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        when(request.getSubmittedForm()).thenReturn(jsonForm);
        
        FileItem fileItem = Mockito.mock(FileItem.class);
        when(request.getFileItem("manageProject")).thenReturn(fileItem);
        when(fileItem.getString()).thenReturn(PROJECTFILE);

        NewMultiJob job = new NewMultiJob(request, response);
        Assert.assertEquals("project", job.getBaseName());
        job.create(false);
        Assert.assertTrue(job.getTopProject() != null);
        Assert.assertEquals(project, job.getTopProject());

        // Check build wrappers - main project...
        Assert.assertEquals(1, bldWrappersList.size());
        BuildWrapper wrapper = bldWrappersList.get(0);
        Assert.assertTrue(wrapper instanceof PreBuildCleanup);
        PreBuildCleanup cleanup = (PreBuildCleanup)wrapper;
        Assert.assertTrue(cleanup.getDeleteDirs());
        
        // Check build actions - main project...
        Assert.assertEquals(8, bldrsList.size());
        Assert.assertTrue(bldrsList.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(bldrsList.get(1) instanceof MultiJobBuilder);
        Assert.assertTrue(bldrsList.get(2) instanceof MultiJobBuilder);
        Assert.assertTrue(bldrsList.get(3) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(4) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(5) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(6) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(7) instanceof VectorCASTCommand);
        
        // Check publishers - main project...
        checkPublishers(publisherList);
        
        // Now check the additional build/report projects
        
        // Build/execute - project 1
        checkBuildExecuteSteps(bldrsList1);

        Assert.assertEquals(1, publisherList1.size());
        Assert.assertTrue(publisherList1.get(0) instanceof GroovyPostbuildRecorder);

        // Report - project 2
        checkBuildExecuteSteps(bldrsList2);
        checkPublishers(publisherList2);

        // Build/execute - project 3
        checkBuildExecuteSteps(bldrsList3);

        Assert.assertEquals(1, publisherList3.size());
        Assert.assertTrue(publisherList3.get(0) instanceof GroovyPostbuildRecorder);
        
        // Report - project 4
        checkBuildExecuteSteps(bldrsList4);
        checkPublishers(publisherList4);
    }
    
    @Test
    public void testNoReporting() throws Exception {
        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("option_use_reporting", false);
        when(request.getSubmittedForm()).thenReturn(jsonForm);
        
        FileItem fileItem = Mockito.mock(FileItem.class);
        when(request.getFileItem("manageProject")).thenReturn(fileItem);
        when(fileItem.getString()).thenReturn(PROJECTFILE);

        NewMultiJob job = new NewMultiJob(request, response);
        Assert.assertEquals("project", job.getBaseName());
        job.create(false);
        Assert.assertTrue(job.getTopProject() != null);
        Assert.assertEquals(project, job.getTopProject());

        // Check build wrappers - main project...
        Assert.assertEquals(1, bldWrappersList.size());
        BuildWrapper wrapper = bldWrappersList.get(0);
        Assert.assertTrue(wrapper instanceof PreBuildCleanup);
        PreBuildCleanup cleanup = (PreBuildCleanup)wrapper;
        Assert.assertTrue(cleanup.getDeleteDirs());
        
        // Check build actions - main project...
        Assert.assertEquals(5, bldrsList.size());
        Assert.assertTrue(bldrsList.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(bldrsList.get(1) instanceof MultiJobBuilder);
        Assert.assertTrue(bldrsList.get(2) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(3) instanceof CopyArtifact);
        Assert.assertTrue(bldrsList.get(4) instanceof VectorCASTCommand);
        
        // Check publishers - main project...
        Assert.assertEquals(0, publisherList.size());
        
        // Now check the additional build/report projects
        
        // Build/execute - project 1
        checkBuildExecuteSteps(bldrsList1);

        Assert.assertEquals(1, publisherList1.size());
        Assert.assertTrue(publisherList1.get(0) instanceof GroovyPostbuildRecorder);

        // Build/execute - project 3
        checkBuildExecuteSteps(bldrsList3);

        Assert.assertEquals(1, publisherList3.size());
        Assert.assertTrue(publisherList3.get(0) instanceof GroovyPostbuildRecorder);
    }
    
    private void checkBuildExecuteSteps(DescribableList<Builder,Descriptor<Builder>> list) {
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(list.get(1) instanceof VectorCASTCommand);
    }

    private void checkPublishers(DescribableList<Publisher,Descriptor<Publisher>> list) {
        Assert.assertEquals(4, list.size());
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(list.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)list.get(0);
        Assert.assertEquals("**/*",archiver.getArtifacts());
        Assert.assertFalse(archiver.getAllowEmptyArchive());
        // Publisher 1- XUnitPublisher
        Assert.assertTrue(list.get(1) instanceof XUnitPublisher);
        XUnitPublisher xUnit = (XUnitPublisher)list.get(1);
        Assert.assertArrayEquals(null, xUnit.getThresholds());
        Assert.assertTrue(xUnit.getTypes()[0] instanceof CheckType);
        CheckType checkType = (CheckType)xUnit.getTypes()[0];
        Assert.assertEquals("**/test_results_*.xml", checkType.getPattern());
        Assert.assertTrue(checkType.isSkipNoTestFiles());
        Assert.assertTrue(checkType.isDeleteOutputFiles());
        Assert.assertFalse(checkType.isFailIfNotNew());
        Assert.assertTrue(checkType.isStopProcessingIfError());
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
