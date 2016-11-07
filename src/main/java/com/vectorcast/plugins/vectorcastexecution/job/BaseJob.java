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

import com.vectorcast.plugins.vectorcastcoverage.VectorCASTHealthReportThresholds;
import com.vectorcast.plugins.vectorcastcoverage.VectorCASTPublisher;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTSetup;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.plugins.ws_cleanup.PreBuildCleanup;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.scm.SCMS;
import hudson.tasks.ArtifactArchiver;
import java.io.IOException;
import java.util.logging.Logger;
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
 */
abstract public class BaseJob {
    
    private static final Logger LOG = Logger.getLogger(BaseJob.class.getName());
    
    private Jenkins instance;
    private StaplerRequest request;
    private StaplerResponse response;
    private String manageProjectName;
    private String baseName;
    private Project topProject;
    private String environmentSetupWin;
    private String environmentSetupUnix;
    private String executePreambleWin;
    private String executePreambleUnix;
    private String environmentTeardownWin;
    private String environmentTeardownUnix;
    private boolean option_use_reporting;
    private String option_error_level;
    private boolean option_execution_report;
    
    protected BaseJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        instance = Jenkins.getInstance();
        this.request = request;
        this.response = response;

        JSONObject json = request.getSubmittedForm();
        
        manageProjectName = json.optString("manageProjectName");
        baseName = FilenameUtils.getBaseName(manageProjectName);
        
        environmentSetupWin = json.optString("environment_setup_win");
        executePreambleWin = json.optString("execute_preamble_win");
        environmentTeardownWin = json.optString("environment_teardown_win");

        environmentSetupUnix = json.optString("environment_setup_unix");
        executePreambleUnix = json.optString("execute_preamble_unix");
        environmentTeardownUnix = json.optString("environment_teardown_unix");
        
        option_use_reporting = json.optBoolean("option_use_reporting", true);
        option_error_level = json.optString("option_error_level", "Unstable");
        option_execution_report = json.optBoolean("option_execution_report", true);

    }
    
    protected String getEnvironmentSetupWin() {
        return environmentSetupWin;
    }
    protected String getExecutePreambleWin() {
        return executePreambleWin;
    }
    protected String getEnvironmentTeardownWin() {
        return environmentTeardownWin;
    }
    protected String getEnvironmentSetupUnix() {
        return environmentSetupUnix;
    }
    protected String getExecutePreambleUnix() {
        return executePreambleUnix;
    }
    protected String getEnvironmentTeardownUnix() {
        return environmentTeardownUnix;
    }
    protected boolean getOptionUseReporting() {
        return option_use_reporting;
    }
    protected String getOptionErrorLevel() {
        return option_error_level;
    }
    protected boolean getOptionExecutionReport() {
        return option_execution_report;
    }
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
    protected StaplerResponse getResponse() {
        return response;
    }
    protected void addDeleteWorkspaceBeforeBuildStarts(Project project) {
        PreBuildCleanup cleanup = new PreBuildCleanup(/*patterns*/null, true, /*cleanup param*/"", /*external delete*/"");
        project.getBuildWrappersList().add(cleanup);
    }
    public void create(boolean update) throws IOException, ServletException, Descriptor.FormException {
        // Create the top-level project
        topProject = createProject();
        if (topProject == null) {
            return;
        }

        // Read the SCM setup
        SCM scm = SCMS.parseSCM(request, topProject);
        if (scm == null) {
            scm = new NullSCM();
        }
        topProject.setScm(scm);

        addDeleteWorkspaceBeforeBuildStarts(topProject);

        doCreate(update);
    }
    abstract protected Project createProject() throws IOException;
    abstract protected void doCreate(boolean update) throws IOException, ServletException, Descriptor.FormException ;
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
