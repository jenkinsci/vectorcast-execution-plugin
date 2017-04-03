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
 * Base job management - create/delete/update
 */
abstract public class BaseJob {
    /** Jenkins instance */    
    private Jenkins instance;
    /** Request */
    private StaplerRequest request;
    /** Response */
    private StaplerResponse response;
    /** Manage project name */
    private String manageProjectName;
    /** Base name generated from the manage project name */
    private String baseName;
    /** Top-level project */
    private Project topProject;
    /** Environment setup for windows */
    private String environmentSetupWin;
    /** Environment setup for unix */
    private String environmentSetupUnix;
    /** Execute preamble for windows */
    private String executePreambleWin;
    /** Execute preamble for unix */
    private String executePreambleUnix;
    /** Environment tear down for windows */
    private String environmentTeardownWin;
    /** Environment tear down for unix */
    private String environmentTeardownUnix;
    /** Use Jenkins reporting */
    private boolean option_use_reporting;
    /** What error-level to use */
    private String option_error_level;
    /** Use HTML in build description */
    private String option_html_build_desc;
    /** Generate execution report */
    private boolean option_execution_report;
    /** Clean workspace */
    private boolean option_clean_workspace;
    /** Using some form of SCM */
    private boolean usingSCM;
    /**
     * Constructor
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException 
     */
    protected BaseJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        instance = Jenkins.getInstance();
        this.request = request;
        this.response = response;
        this.usingSCM = false;

        JSONObject json = request.getSubmittedForm();
        
        manageProjectName = json.optString("manageProjectName");
        if (!manageProjectName.isEmpty()) {
            // Force unix style path to avoid problems later
            manageProjectName = manageProjectName.replace('\\','/');
        }
        baseName = FilenameUtils.getBaseName(manageProjectName);
        
        environmentSetupWin = json.optString("environment_setup_win");
        executePreambleWin = json.optString("execute_preamble_win");
        environmentTeardownWin = json.optString("environment_teardown_win");

        environmentSetupUnix = json.optString("environment_setup_unix");
        executePreambleUnix = json.optString("execute_preamble_unix");
        environmentTeardownUnix = json.optString("environment_teardown_unix");
        
        option_use_reporting = json.optBoolean("option_use_reporting", true);
        option_error_level = json.optString("option_error_level", "Unstable");
        option_html_build_desc = json.optString("option_html_build_desc", "HTML");
        option_execution_report = json.optBoolean("option_execution_report", true);
        option_clean_workspace = json.optBoolean("option_clean", false);
    }
    /**
     * Using some form of SCM
     * @return true or false
     */
    protected boolean isUsingSCM() {
        return usingSCM;
    }
    /**
     * Get environment setup for windows
     * @return setup
     */
    protected String getEnvironmentSetupWin() {
        return environmentSetupWin;
    }
    /**
     * Get execute preamble for windows
     * @return preamble
     */
    protected String getExecutePreambleWin() {
        return executePreambleWin;
    }
    /**
     * Get environment tear down for windows
     * @return 
     */
    protected String getEnvironmentTeardownWin() {
        return environmentTeardownWin;
    }
    /**
     * Get environment setup for unix
     * @return environment setup
     */
    protected String getEnvironmentSetupUnix() {
        return environmentSetupUnix;
    }
    /**
     * Get execute preamble for unix
     * @return preamble
     */
    protected String getExecutePreambleUnix() {
        return executePreambleUnix;
    }
    /**
     * Get environment tear down for unix
     * @return teardown
     */
    protected String getEnvironmentTeardownUnix() {
        return environmentTeardownUnix;
    }
    /**
     * Get use Jenkins reporting option
     * @return true to use, false to not
     */
    protected boolean getOptionUseReporting() {
        return option_use_reporting;
    }
    /**
     * Get error level
     * @return Unstable or Failure
     */
    protected String getOptionErrorLevel() {
        return option_error_level;
    }
    /**
     * Use HTML Build Description
     * @return HTML or TEXT
     */
    protected String getOptionHtmlBuildDesc() {
        return option_html_build_desc;
    }
    /**
     * Use execution report
     * @return true to use, false to not
     */
    protected boolean getOptionExecutionReport() {
        return option_execution_report;
    }
    /**
     * Get top-level project
     * @return project
     */
    protected Project getTopProject() {
        return topProject;
    }
    /**
     * Get manage project name
     * @return manage project name
     */
    protected String getManageProjectName() {
        return manageProjectName;
    }
    /**
     * Get base name of manage project
     * @return base name
     */
    protected String getBaseName() {
        return baseName;
    }
    /**
     * Get request
     * @return request
     */
    protected StaplerRequest getRequest() {
        return request;
    }
    /**
     * Get Jenkins instance
     * @return Jenkins instance
     */
    protected Jenkins getInstance() {
        return instance;
    }
    /**
     * Get response
     * @return response
     */
    protected StaplerResponse getResponse() {
        return response;
    }
    /**
     * Add the delete workspace before build starts option
     * @param project project to add to
     */
    protected void addDeleteWorkspaceBeforeBuildStarts(Project project) {
        if (option_clean_workspace) {
            PreBuildCleanup cleanup = new PreBuildCleanup(/*patterns*/null, true, /*cleanup param*/"", /*external delete*/"");
            project.getBuildWrappersList().add(cleanup);
        }
    }
    /**
     * Create the job(s)
     * @param update
     * @throws IOException
     * @throws ServletException
     * @throws hudson.model.Descriptor.FormException
     * @throws JobAlreadyExistsException 
     */
    public void create(boolean update) throws IOException, ServletException, Descriptor.FormException, JobAlreadyExistsException, InvalidProjectFileException {
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
        if (scm instanceof NullSCM) {
            usingSCM = false;
        } else {
            usingSCM = true;
        }

        addDeleteWorkspaceBeforeBuildStarts(topProject);

        try {
            doCreate(update);
        } catch (InvalidProjectFileException ex) {
            cleanupProject();
            throw ex;
        }
    }
    /**
     * Create top-level project
     * @return created project
     * @throws IOException
     * @throws JobAlreadyExistsException 
     */
    abstract protected Project createProject() throws IOException, JobAlreadyExistsException;
    /**
     * Cleanup top-level project, as in delete
     */
    abstract protected void cleanupProject();
    /**
     * Do create of project details
     * @param update true if doing an update rather than a create
     * @throws IOException
     * @throws ServletException
     * @throws hudson.model.Descriptor.FormException 
     */
    abstract protected void doCreate(boolean update) throws IOException, ServletException, Descriptor.FormException, InvalidProjectFileException ;
    /**
     * Add the VectorCAST setup step to copy the python scripts to
     * the workspace
     * @param project project
     */
    protected void addSetup(Project project) {
        VectorCASTSetup setup = new VectorCASTSetup();
        project.getBuildersList().add(setup);
    }
    /**
     * Add archive artifacts step
     * @param project project to add to
     */
    protected void addArchiveArtifacts(Project project) {
        ArtifactArchiver archiver = new ArtifactArchiver(
                /*artifacts*/"*incremental_rebuild_report*," +
                             "*_report.html, " +
                             "execution/**, " +
                             "management/**, " +
                             "xml_data/**",
                /*excludes*/"",
                /*latest only*/false,
                /*allow empty archive*/false);
        project.getPublishersList().add(archiver);
    }
    /**
     * Add XUnit rules step
     * @param project project to add step to
     */
    protected void addXunit(Project project) {
        XUnitThreshold[] thresholds = null;
        CheckType checkType = new CheckType("**/test_results_*.xml", /*skipNoTestFiles*/true, /*failIfNotNew*/false, /*deleteOpFiles*/true, /*StopProcIfErrot*/true);
        TestType[] testTypes = new TestType[1];
        testTypes[0] = checkType;
        XUnitPublisher xunit = new XUnitPublisher(testTypes, thresholds);
        project.getPublishersList().add(xunit);
    }
    /**
     * Add VectorCAST coverage reporting step
     * @param project project to add step to
     */
    protected void addVCCoverage(Project project) {
        VectorCASTHealthReportThresholds healthReports = new VectorCASTHealthReportThresholds(0, 100, 0, 70, 0, 80, 0, 80, 0, 80, 0, 80);
        VectorCASTPublisher publisher = new VectorCASTPublisher();
        publisher.includes = "**/coverage_results_*.xml";
        publisher.healthReports = healthReports;
        project.getPublishersList().add(publisher);
    }
}
