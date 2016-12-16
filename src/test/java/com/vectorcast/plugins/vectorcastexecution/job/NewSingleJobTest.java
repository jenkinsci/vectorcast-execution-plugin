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

import com.vectorcast.plugins.vectorcastcoverage.VectorCASTPublisher;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTSetup;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.RootAction;
import hudson.plugins.ws_cleanup.PreBuildCleanup;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import junit.framework.TestCase;
import net.sf.json.JSONObject;
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
public class NewSingleJobTest extends TestCase {
    @Mock
    private Jenkins mockJenkins;
    @Mock
    private FreeStyleProject project;
    private DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList;
    private DescribableList<Builder,Descriptor<Builder>> bldrsList;
    private DescribableList<Publisher,Descriptor<Publisher>> publisherList;
    @Mock
    private ScriptApproval scriptApproval;
    @Mock
    private ExtensionList<RootAction> rootActionList;
    @Mock
    private ExtensionList<Language> langList;
    
    private static final String PROJECTNAME = "project.vcast_manage.singlejob";
    
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(mockJenkins);
        
        when(mockJenkins.createProject(FreeStyleProject.class, PROJECTNAME)).thenReturn(project);
        when(mockJenkins.getExtensionList(RootAction.class)).thenReturn(rootActionList);
        when(mockJenkins.getExtensionList(Language.class)).thenReturn(langList);
        
        GroovyLanguage groovy = new GroovyLanguage();
        when(langList.get(GroovyLanguage.class)).thenReturn(groovy);
        
        bldWrappersList = new DescribableList(project);
        when(project.getBuildWrappersList()).thenReturn(bldWrappersList);
        bldrsList = new DescribableList<>(project);
        when(project.getBuildersList()).thenReturn(bldrsList);
        publisherList = new DescribableList<>(project);
        when(project.getPublishersList()).thenReturn(publisherList);
        
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
        jsonForm.put("option_clean", true);
        when(request.getSubmittedForm()).thenReturn(jsonForm);

        NewSingleJob job = new NewSingleJob(request, response);
        Assert.assertEquals("project", job.getBaseName());
        job.create(false);
        Assert.assertEquals(PROJECTNAME, job.getProjectName());
        Assert.assertTrue(job.getTopProject() != null);
        Assert.assertEquals(project, job.getTopProject());

        // Check build wrappers...
        Assert.assertEquals(1, bldWrappersList.size());
        BuildWrapper wrapper = bldWrappersList.get(0);
        Assert.assertTrue(wrapper instanceof PreBuildCleanup);
        PreBuildCleanup cleanup = (PreBuildCleanup)wrapper;
        Assert.assertTrue(cleanup.getDeleteDirs());
        
        // Check build actions...
        Assert.assertEquals(2, bldrsList.size());
        Assert.assertTrue(bldrsList.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(bldrsList.get(1) instanceof VectorCASTCommand);
        
        // Check publishers...
        Assert.assertEquals(4, publisherList.size());
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);
        Assert.assertEquals("*_report.html, " +
                            "execution/**, " +
                            "management/**, " +
                            "xml_data/**",
                            archiver.getArtifacts());
        Assert.assertFalse(archiver.getAllowEmptyArchive());
        // Publisher 1- XUnitPublisher
        Assert.assertTrue(publisherList.get(1) instanceof XUnitPublisher);
        XUnitPublisher xUnit = (XUnitPublisher)publisherList.get(1);
        Assert.assertArrayEquals(null, xUnit.getThresholds());
        Assert.assertTrue(xUnit.getTypes()[0] instanceof CheckType);
        CheckType checkType = (CheckType)xUnit.getTypes()[0];
        Assert.assertEquals("**/test_results_*.xml", checkType.getPattern());
        Assert.assertTrue(checkType.isSkipNoTestFiles());
        Assert.assertTrue(checkType.isDeleteOutputFiles());
        Assert.assertFalse(checkType.isFailIfNotNew());
        Assert.assertTrue(checkType.isStopProcessingIfError());
        // Publisher 2 - VectorCASTPublisher
        Assert.assertTrue(publisherList.get(2) instanceof VectorCASTPublisher);
        VectorCASTPublisher vcPublisher = (VectorCASTPublisher)publisherList.get(2);
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
        Assert.assertTrue(publisherList.get(3) instanceof GroovyPostbuildRecorder);
        GroovyPostbuildRecorder groovyScript = (GroovyPostbuildRecorder)publisherList.get(3);
        Assert.assertEquals(/*failure*/2, groovyScript.getBehavior());
    }

}
