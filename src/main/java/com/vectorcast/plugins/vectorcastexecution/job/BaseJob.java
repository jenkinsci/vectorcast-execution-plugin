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
//import org.jenkinsci.plugins.xunit.XUnitPublisher;
//import org.jenkinsci.plugins.xunit.threshold.XUnitThreshold;
//import org.jenkinsci.plugins.xunit.types.CheckType;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.tasks.junit.JUnitResultArchiver;
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
    private boolean optionUseReporting;
    /** What error-level to use */
    private String optionErrorLevel;
    /** Use HTML in build description */
    private String optionHtmlBuildDesc;
    /** Generate execution report */
    private boolean optionExecutionReport;
    /** Clean workspace */
    private boolean optionClean;
    /** Using some form of SCM */
    private boolean usingSCM;
    /** The SCM being used */
    private SCM scm;
    /** Use saved data or not */
    private boolean useSavedData;
    /** Wait time */
    private Long waitTime;
    /** Wait loops */
    private Long waitLoops;
    /** Base Job name */
    private String jobName;
    /** Node label */
    private String nodeLabel;
    /**
     * Constructor
     * @param request request object
     * @param response response object
     * @param useSavedData use saved data true/false
     * @throws ServletException exception
     * @throws IOException exception
     */
    protected BaseJob(final StaplerRequest request, final StaplerResponse response, boolean useSavedData) throws ServletException, IOException {
        instance = Jenkins.getInstance();
        this.request = request;
        this.response = response;
        JSONObject json = request.getSubmittedForm();

        manageProjectName = json.optString("manageProjectName");
        if (!manageProjectName.isEmpty()) {
            // Force unix style path to avoid problems later
            manageProjectName = manageProjectName.replace('\\','/');
        }
        baseName = FilenameUtils.getBaseName(manageProjectName);

        this.useSavedData = useSavedData;
        if (useSavedData) {
            // Data will be set later
        } else {
            this.usingSCM = false;

            environmentSetupWin = json.optString("environmentSetupWin");
            executePreambleWin = json.optString("executePreambleWin");
            environmentTeardownWin = json.optString("environmentTeardownWin");

            environmentSetupUnix = json.optString("environmentSetupUnix");
            executePreambleUnix = json.optString("executePreambleUnix");
            environmentTeardownUnix = json.optString("environmentTeardownUnix");

            optionUseReporting = json.optBoolean("optionUseReporting", true);
            optionErrorLevel = json.optString("optionErrorLevel", "Unstable");
            optionHtmlBuildDesc = json.optString("optionHtmlBuildDesc", "HTML");
            optionExecutionReport = json.optBoolean("optionExecutionReport", true);
            optionClean = json.optBoolean("optionClean", false);

            waitTime = json.optLong("waitTime", 5);
            waitLoops = json.optLong("waitLoops", 2);
            
            jobName = json.optString("jobName", null);
            nodeLabel = json.optString("nodeLabel", "");
        }
    }
    /**
     * Use Saved Data
     * @param savedData saved data to use
     */
    public void useSavedData(VectorCASTSetup savedData) {
        environmentSetupWin = savedData.getEnvironmentSetupWin();
        executePreambleWin = savedData.getExecutePreambleWin();
        environmentTeardownWin = savedData.getEnvironmentTeardownWin();

        environmentSetupUnix = savedData.getEnvironmentSetupUnix();
        executePreambleUnix = savedData.getExecutePreambleUnix();
        environmentTeardownUnix = savedData.getEnvironmentTeardownUnix();

        optionUseReporting = savedData.getOptionUseReporting();
        optionErrorLevel = savedData.getOptionErrorLevel();
        optionHtmlBuildDesc = savedData.getOptionHtmlBuildDesc();
        optionExecutionReport = savedData.getOptionExecutionReport();
        optionClean = savedData.getOptionClean();

        usingSCM = savedData.getUsingSCM();
        scm = savedData.getSCM();

        waitTime = savedData.getWaitTime();
        waitLoops = savedData.getWaitLoops();
        jobName = savedData.getJobName();
        nodeLabel = savedData.getNodeLabel();
    }
    /**
     * Using some form of SCM
     * @return true or false
     */
    protected boolean isUsingSCM() {
        return usingSCM;
    }
    /**
     * Set using some form of SCM
     * @param usingSCM true/false
     */
    protected void setUsingSCM(boolean usingSCM) {
        this.usingSCM = usingSCM;
    }
    /**
     * Get environment setup for windows
     * @return setup
     */
    protected String getEnvironmentSetupWin() {
        return environmentSetupWin;
    }
    /**
     * Set environment setup for windows
     * @param environmentSetupWin windows environment setup
     */
    protected void setEnvironmentSetupWin(String environmentSetupWin) {
        this.environmentSetupWin = environmentSetupWin;
    }
    /**
     * Get execute preamble for windows
     * @return preamble
     */
    protected String getExecutePreambleWin() {
        return executePreambleWin;
    }
    /**
     * Set execute preamble for windows
     * @param executePreambleWin execute preamble for windows
     */
    protected void setExecutePreambleWin(String executePreambleWin) {
        this.executePreambleWin = executePreambleWin;
    }
    /**
     * Get environment tear down for windows
     * @return environment tear down for windows
     */
    protected String getEnvironmentTeardownWin() {
        return environmentTeardownWin;
    }
    /**
     * Set environment tear down for windows
     * @param environmentTeardownWin environment tear down for windows
     */
    protected void setEnvironmentTeardownWin(String environmentTeardownWin) {
        this.environmentTeardownWin = environmentTeardownWin;
    }
    /**
     * Get environment setup for unix
     * @return environment setup
     */
    protected String getEnvironmentSetupUnix() {
        return environmentSetupUnix;
    }
    /**
     * Set environment setup for unix
     * @param environmentSetupUnix environment setup for unix
     */
    protected void setEnvironmentSetupUnix(String environmentSetupUnix) {
        this.environmentSetupUnix = environmentSetupUnix;
    }
    /**
     * Get execute preamble for unix
     * @return preamble
     */
    protected String getExecutePreambleUnix() {
        return executePreambleUnix;
    }
    /**
     * Set execute preamble for unix
     * @param executePreambleUnix execute preamble for unix
     */
    protected void setExecutePreambleUnix(String executePreambleUnix) {
        this.executePreambleUnix = executePreambleUnix;
    }
    /**
     * Get environment tear down for unix
     * @return teardown
     */
    protected String getEnvironmentTeardownUnix() {
        return environmentTeardownUnix;
    }
    /**
     * Set environment teardown for unix
     * @param environmentTeardownUnix environment tear down for unix
     */
    protected void setEnvironmentTeardownUnix(String environmentTeardownUnix) {
        this.environmentTeardownUnix = environmentTeardownUnix;
    }
    /**
     * Get use Jenkins reporting option
     * @return true to use, false to not
     */
    protected boolean getOptionUseReporting() {
        return optionUseReporting;
    }
    /**
     * Set use Jenkins reporting option
     * @param optionUseReporting true to use, false to not
     */
    protected void setOptionUseReporting(boolean optionUseReporting) {
        this.optionUseReporting = optionUseReporting;
    }
    /**
     * Get error level
     * @return Unstable or Failure
     */
    protected String getOptionErrorLevel() {
        return optionErrorLevel;
    }
    /**
     * Set option error level
     * @param optionErrorLevel Unstable or Failure
     */
    protected void setOptionErrorLevel(String optionErrorLevel) {
        this.optionErrorLevel = optionErrorLevel;
    }
    /**
     * Use HTML Build Description
     * @return HTML or TEXT
     */
    protected String getOptionHTMLBuildDesc() {
        return optionHtmlBuildDesc;
    }
    /**
     * Set use HTML Build description
     * @param optionHtmlBuildDesc HTML build description
     */
    protected void setOptionHTMLBuildDesc(String optionHtmlBuildDesc) {
        this.optionHtmlBuildDesc = optionHtmlBuildDesc;
    }
    /**
     * Use execution report
     * @return true to use, false to not
     */
    protected boolean getOptionExecutionReport() {
        return optionExecutionReport;
    }
    /**
     * Set use execution report
     * @param optionExecutionReport true to use, false to not
     */
    protected void setOptionExecutionReport(boolean optionExecutionReport) {
        this.optionExecutionReport = optionExecutionReport;
    }
    /**
     * Get option to clean workspace before build
     * @return true to clean, false to not
     */
    protected boolean getOptionClean() {
        return optionClean;
    }
    /**
     * Set option to clean workspace before build
     * @param optionClean  true to clean, false to not
     */
    protected void setOptionClean(boolean optionClean) {
        this.optionClean = optionClean;
    }
    /**
     * Get the time to wait between retries
     * @return number of seconds
     */
    protected Long getWaitTime() {
        return waitTime;
    }
    /**
     * Get the number of wait loops
     * @return number of iterations
     */
    protected Long getWaitLoops() {
        return waitLoops;
    }
    /**
     * Get the user-specified job name
     * @return job name (null for use default)
     */
    protected String getJobName() {
        return jobName;
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
     * Get node label
     * @return node label
     */
    protected String getNodeLabel() {
        return nodeLabel;
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
        if (optionClean) {
            PreBuildCleanup cleanup = new PreBuildCleanup(/*patterns*/null, true, /*cleanup param*/"", /*external delete*/"");
            project.getBuildWrappersList().add(cleanup);
        }
    }
    /**
     * Create the job(s)
     * @param update true/false
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws JobAlreadyExistsException exception
     * @throws InvalidProjectFileException exception
     */
    public void create(boolean update) throws IOException, ServletException, Descriptor.FormException, JobAlreadyExistsException, InvalidProjectFileException {
        // Create the top-level project
        topProject = createProject();
        if (topProject == null) {
            return;
        }

        if (!useSavedData) {
            // Read the SCM setup
            scm = SCMS.parseSCM(request, topProject);
            if (scm == null) {
                scm = new NullSCM();
            }
            if (scm instanceof NullSCM) {
                usingSCM = false;
            } else {
                usingSCM = true;
            }
        }
        topProject.setScm(scm);

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
     * @throws IOException exception
     * @throws JobAlreadyExistsException exception
     */
    abstract protected Project createProject() throws IOException, JobAlreadyExistsException;
    /**
     * Cleanup top-level project, as in delete
     */
    abstract protected void cleanupProject();
    /**
     * Do create of project details
     * @param update true if doing an update rather than a create
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws InvalidProjectFileException exception
     */
    abstract protected void doCreate(boolean update) throws IOException, ServletException, Descriptor.FormException, InvalidProjectFileException ;
    /**
     * Add the VectorCAST setup step to copy the python scripts to
     * the workspace
     * @param project project
     * @return the setup build step
     */
    protected VectorCASTSetup addSetup(Project project) {
        VectorCASTSetup setup = 
                new VectorCASTSetup(environmentSetupWin,
                                    environmentSetupUnix,
                                    executePreambleWin,
                                    executePreambleUnix,
                                    environmentTeardownWin,
                                    environmentTeardownUnix,
                                    optionUseReporting,
                                    optionErrorLevel,
                                    optionHtmlBuildDesc,
                                    optionExecutionReport,
                                    optionClean,
                                    waitLoops,
                                    waitTime,
                                    manageProjectName,
                                    jobName,
                                    nodeLabel);
        setup.setUsingSCM(usingSCM);
        setup.setSCM(scm);

        project.getBuildersList().add(setup);
        return setup;
    }
    /**
     * Add archive artifacts step
     * @param project project to add to
     */
    protected void addArchiveArtifacts(Project project) {
        ArtifactArchiver archiver = new ArtifactArchiver(
                /*artifacts*/"*_rebuild*," +
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
     * Add JUnit rules step
     * @param project project to add step to
     */

    protected void addJunit(Project project) {
        JUnitResultArchiver junit = new JUnitResultArchiver("**/test_results_*.xml");
        project.getPublishersList().add(junit);
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
