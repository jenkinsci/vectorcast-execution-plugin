/*
 * The MIT License
 *
 * Copyright 2016 rmk.
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

import com.vectorcast.plugins.vectorcastcoverage.VectorCASTHealthReportThresholds;
import com.vectorcast.plugins.vectorcastcoverage.VectorCASTPublisher;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTSetup;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.plugins.ws_cleanup.PreBuildCleanup;
import hudson.tasks.ArtifactArchiver;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.lib.dtkit.type.TestType;
import org.jenkinsci.plugins.xunit.XUnitPublisher;
import org.jenkinsci.plugins.xunit.threshold.XUnitThreshold;
import org.jenkinsci.plugins.xunit.types.CheckType;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author rmk
 */
abstract public class BaseJob {
    
    private Jenkins instance;
    private StaplerRequest request;
    private StaplerResponse response;
    private String manageProjectName;
    private String baseName;
//    private FreeStyleProject project;
    private Project topProject;
    
    protected BaseJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        instance = Jenkins.getInstance();
        this.request = request;
        this.response = response;

        JSONObject json = request.getSubmittedForm();
        
        manageProjectName = json.optString("manageProjectName");
        baseName = FilenameUtils.getBaseName(manageProjectName);
    }
    
    abstract protected void processManageProject() throws ServletException, IOException;

//    protected FreeStyleProject getProject() {
    protected Project getTopProject() {
        return topProject;
    }
    protected String getManageProjectName() {
        return manageProjectName;
    }
    protected String getBaseName() {
        return baseName;
    }
    protected StaplerRequest getRequest() {
        return request;
    }
    protected Jenkins getInstance() {
        return instance;
    }
    protected void addDeleteWorkspaceBeforeBuildStarts(Project project) {
        PreBuildCleanup cleanup = new PreBuildCleanup(/*patterns*/null, true, /*cleanup param*/"", /*external delete*/"");
        project.getBuildWrappersList().add(cleanup);
    }
    public void create() throws IOException, ServletException, Descriptor.FormException {
        // Create the top-level project
        topProject = createProject();
        
        // Read the SCM setup
        topProject.doConfigSubmit(request, response);
        
        addDeleteWorkspaceBeforeBuildStarts(topProject);

        processManageProject();

        doCreate();
    }
    abstract protected Project createProject() throws IOException;
    abstract protected void doCreate() throws IOException, ServletException, Descriptor.FormException ;
    /**
     * Add the VectorCAST setup step to copy the python scripts to
     * the workspace
     * @param project project
     */
    protected void addSetup(Project project) {
        VectorCASTSetup setup = new VectorCASTSetup();
        project.getBuildersList().add(setup);
    }
    protected void addArchiveArtifacts(Project project) {
        ArtifactArchiver archiver = new ArtifactArchiver(/*artifacts*/"**/*", /*excludes*/"", /*latest only*/false, /*allow empty archive*/false);
        project.getPublishersList().add(archiver);
    }
    
    protected void addXunit(Project project) {
        XUnitThreshold[] thresholds = null;
        CheckType checkType = new CheckType("**/test_results_*.xml", /*skipNoTestFiles*/true, /*failIfNotNew*/false, /*deleteOpFiles*/true, /*StopProcIfErrot*/true);
        TestType[] testTypes = new TestType[1];
        testTypes[0] = checkType;
        XUnitPublisher xunit = new XUnitPublisher(testTypes, thresholds);
        project.getPublishersList().add(xunit);
    }
    
    protected void addVCCoverage(Project project) {
        VectorCASTHealthReportThresholds healthReports = new VectorCASTHealthReportThresholds(0, 100, 0, 70, 0, 80, 0, 80, 0, 80, 0, 80);
        VectorCASTPublisher publisher = new VectorCASTPublisher();
        publisher.includes = "**/coverage_results_*.xml";
        publisher.healthReports = healthReports;
        project.getPublishersList().add(publisher);
    }
    
}
