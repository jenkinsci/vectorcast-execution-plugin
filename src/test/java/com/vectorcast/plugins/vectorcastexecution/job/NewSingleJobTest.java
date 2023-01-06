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
import junit.framework.TestCase;
import net.sf.json.JSONObject;
import hudson.tasks.junit.JUnitResultArchiver;
import org.junit.Assert;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class NewSingleJobTest extends TestCase {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    private static final String PROJECTNAME = "project.vcast.single";

    @Test
    public void testBasic() throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        MockAuthorizationStrategy mockStrategy = new MockAuthorizationStrategy();
        mockStrategy.grant(Jenkins.READ).everywhere().to("devel");
        for (Permission p : Item.PERMISSIONS.getPermissions()) {
            mockStrategy.grant(p).everywhere().to("devel");
        }
        r.jenkins.setAuthorizationStrategy(mockStrategy);

        StaplerRequest request = Mockito.mock(StaplerRequest.class);
        StaplerResponse response = Mockito.mock(StaplerResponse.class);
        JSONObject jsonForm = new JSONObject();
        jsonForm.put("manageProjectName", "/home/jenkins/vcast/project.vcm");
        jsonForm.put("optionClean", true);
        when(request.getSubmittedForm()).thenReturn(jsonForm);

        NewSingleJob job = new NewSingleJob(request, response);
        Assert.assertEquals("project", job.getBaseName());
        job.create(false);
        Assert.assertEquals(PROJECTNAME, job.getProjectName());
        Assert.assertTrue(job.getTopProject() != null);

        // Check build wrappers...
        DescribableList<BuildWrapper, Descriptor<BuildWrapper>> bldWrappersList = job.getTopProject().getBuildWrappersList();
        Assert.assertEquals(1, bldWrappersList.size());
        BuildWrapper wrapper = bldWrappersList.get(0);
        Assert.assertTrue(wrapper instanceof PreBuildCleanup);
        PreBuildCleanup cleanup = (PreBuildCleanup)wrapper;
        Assert.assertTrue(cleanup.getDeleteDirs());

        // Check build actions...
        DescribableList<Builder,Descriptor<Builder>> bldrsList = job.getTopProject().getBuildersList();
        Assert.assertEquals(2, bldrsList.size());
        Assert.assertTrue(bldrsList.get(0) instanceof VectorCASTSetup);
        Assert.assertTrue(bldrsList.get(1) instanceof VectorCASTCommand);

        // Check publishers...
        DescribableList<Publisher,Descriptor<Publisher>> publisherList = job.getTopProject().getPublishersList();
        Assert.assertEquals(4, publisherList.size());
        // Publisher 0 - ArtifactArchiver
        Assert.assertTrue(publisherList.get(0) instanceof ArtifactArchiver);
        ArtifactArchiver archiver = (ArtifactArchiver)publisherList.get(0);
        Assert.assertEquals("**/*.html, xml_data/*.xml, unit_test_fail_count.txt, **/*.png, **/*.css, complete_build.log",archiver.getArtifacts());
        Assert.assertFalse(archiver.getAllowEmptyArchive());
        // Publisher 1- JUnitResultArchiver
        Assert.assertTrue(publisherList.get(1) instanceof JUnitResultArchiver);
        JUnitResultArchiver jUnit = (JUnitResultArchiver)publisherList.get(1);
        Assert.assertEquals("**/test_results_*.xml", jUnit.getTestResults());
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
