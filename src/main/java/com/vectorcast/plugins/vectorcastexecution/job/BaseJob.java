/*
 * The MIT License
 *
 * Copyright 2024 Vector Software, East Greenwich, Rhode Island USA
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
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.copyartifact.StatusBuildSelector;
import hudson.tasks.ArtifactArchiver;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.io.FilenameUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.tasks.junit.JUnitResultArchiver;
import io.jenkins.plugins.analysis.warnings.PcLint;
import io.jenkins.plugins.analysis.core.steps.IssuesRecorder;
import io.jenkins.plugins.coverage.metrics.steps.CoverageRecorder;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTool.Parser;
import org.jenkinsci.plugins.credentialsbinding.impl.SecretBuildWrapper;
import org.jenkinsci.plugins.credentialsbinding.impl.UsernamePasswordMultiBinding;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;

/**
 * Base job management - create/delete/update
 */
abstract public class BaseJob {
    private String projectName;

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
    protected Project<?,?> topProject;
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
    /** Use CI license */
    private boolean useCILicenses;

    /** Use strict testcase import */
    private boolean useStrictTestcaseImport;
    
    /** Allow RGW3 test to be executed and exported  */
    private boolean useRGW3;
    
    /** Use coveagePlugin */
    private boolean useCoveragePlugin = true;

    /** Use imported results */
    private boolean useImportedResults = false;
    private boolean useLocalImportedResults = false;
    private boolean useExternalImportedResults = false;
    private String  externalResultsFilename;
    
    /** Use coverage history to control build status */
    private boolean useCoverageHistory;

    /** Using some form of SCM */
    private boolean usingSCM;
    /** The SCM being used */
    private SCM scm;
    /** Wait time */
    private Long waitTime;
    /** Wait loops */
    private Long waitLoops;
    /** Base Job name */
    private String jobName;
    /** Node label */
    private String nodeLabel;
    /** Maximum number of parallal jobs to queue up */
    private Long maxParallel;
    
    /** PC Lint Plus Command */
    private String pclpCommand;
    /** PC Lint Plus Path */
    private String pclpResultsPattern;
    /* Squore execution command */
    private String squoreCommand;

    /* TESTinsights Push information */
    private String TESTinsights_URL;
    private String TESTinsights_project;
    private String TESTinsights_credentials_id;
    private String TESTinsights_proxy;
    private String TESTinsights_SCM_URL;
    private String TESTinsights_SCM_Tech;

    /**
     * Constructor
     * @param request request object
     * @param response response object
     * @throws ServletException exception
     * @throws IOException exception
     * @throws ExternalResultsFileException exception
     */
    protected BaseJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException, ExternalResultsFileException {
        instance = Jenkins.get();
        this.request = request;
        this.response = response;
        JSONObject json = request.getSubmittedForm();
        
        if (json.toString().matches("\\w*")) {
            Logger.getLogger(BaseJob.class.getName()).log(Level.INFO, "JSONObject Submitted Form"+ json.toString());
        }

        manageProjectName = json.optString("manageProjectName");
        if (!manageProjectName.isEmpty()) {
            // Force unix style path to avoid problems later
            manageProjectName = manageProjectName.replace('\\','/');
            manageProjectName = manageProjectName.replaceAll("^[ \t]+|[ \t]+$", "");
            if (! manageProjectName.toLowerCase().endsWith(".vcm")) manageProjectName += ".vcm";
        }
        baseName = FilenameUtils.getBaseName(manageProjectName);

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

        if (jobName != null) {
            // Remove all non-alphanumeric characters from the Jenkins Job name
            jobName = jobName.replaceAll("[^a-zA-Z0-9_]","_");
        }

        nodeLabel = json.optString("nodeLabel", "");

        useCILicenses  = json.optBoolean("useCiLicense", false);
        useStrictTestcaseImport  = json.optBoolean("useStrictTestcaseImport", true);
        useRGW3  = json.optBoolean("useRGW3", false);
        useImportedResults  = json.optBoolean("useImportedResults", false);
        if (json.optInt("coverageDisplayOption", 0) == 0) {
            useCoveragePlugin = true;
        } else {
            useCoveragePlugin = false;
        }
        externalResultsFilename = "";

        if (useImportedResults) {
            JSONObject jsonImportResults  = json.optJSONObject("importedResults");
            
            if (jsonImportResults != null) {
                final long int_ext = jsonImportResults.optLong("value",0);
                
                if (int_ext == 1) {
                    useLocalImportedResults = true;
                    useExternalImportedResults = false;
                    externalResultsFilename = "";
                } else if (int_ext == 2) {
                    useLocalImportedResults = false;
                    useExternalImportedResults = true;
                    externalResultsFilename = jsonImportResults.optString("externalResultsFilename","");
                    if (externalResultsFilename.length() == 0) {
                        throw new ExternalResultsFileException();
                    }
                }
            }
        }
        useCoverageHistory = json.optBoolean("useCoverageHistory", false);
        maxParallel = json.optLong("maxParallel", 0);

        /* Additional Tools */
        pclpCommand = json.optString("pclpCommand", "").replace('\\','/');
        pclpResultsPattern = json.optString("pclpResultsPattern", "");
        squoreCommand = json.optString("squoreCommand", "").replace('\\','/');
        TESTinsights_URL = json.optString("TESTinsights_URL", "");
        TESTinsights_project = json.optString("TESTinsights_project", "");
        if (TESTinsights_project.length() == 0) {
                TESTinsights_project = "env.JOB_BASE_NAME";
        }
        TESTinsights_credentials_id = json.optString("TESTinsights_credentials_id", "");
        TESTinsights_proxy = json.optString("TESTinsights_proxy", "");
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
     * Get option to use CI licenses
     * @return true to use CI licenses, false to not
     */
    protected boolean getUseCILicenses() {
        return useCILicenses;
    }
    /**
     * Set option to use CI licenses
     * @param useCILicenses  true to use CI licenses, false to not
     */
    protected void setUseCILicenses(boolean useCILicenses) {
        this.useCILicenses = useCILicenses;
    }    
    /**
     * Get option to Use strict testcase import
     * @return true to Use strict testcase import, false to not
     */
    protected boolean getUseStrictTestcaseImport() {
        return useStrictTestcaseImport;
    }
    /**
     * Set option to Use strict testcase import
     * @param useStrictTestcaseImport  true to Use strict testcase import, false to not
     */
    protected void setUseStrictTestcaseImport(boolean useStrictTestcaseImport) {
        this.useStrictTestcaseImport = useStrictTestcaseImport;
    }    
    /**
     * Get option to Use RGW3 capabilities
     * @return true use RGW3 capabilities, false to not
     */
    protected boolean getUseRGW3() {
        return useRGW3;
    }
    /**
     * Set option to use RGW3 capabilities
     * @param useRGW3 true to allow RGW3 test cases to run and export
     */
    protected void setUseRGW3(boolean useRGW3) {
        this.useRGW3 = useRGW3;
    }    
    /**
     * Get option to use coverage plugin or vectorcast coverage plugin
     * @return true use coverage plugin or vectorcast coverage plugin
     */
    protected boolean getUseCoveragePlugin() {
        return useCoveragePlugin;
    }
    /**
     * Set option to use coverage plugin or vectorcast coverage plugin
     * @param useCoveragePlugin use coverage plugin or vectorcast coverage plugin
     */
    protected void setUseCoveragePlugin(boolean useCoveragePlugin) {
        this.useCoveragePlugin = useCoveragePlugin;
    }    
    /**
     * Get option to Use imported results
     * @return true to Use imported results, false to not
     */
    protected boolean getUseImportedResults() {
        return useImportedResults;
    }
    /**
     * Set option to Use imported results
     * @param useImportedResults true to Use imported results, false to not
     */
    protected void setUseImportedResults(boolean useImportedResults) {
        this.useImportedResults = useImportedResults;
    }    

    /**
     * Get option to Use local imported results
     * @return true to Use local imported results, false to not
     */
    protected boolean getUseLocalImportedResults() {
        return useLocalImportedResults;
    }
    /**
     * Set option to Use imported results
     * @param useLocalImportedResults true to Use local imported results, false to not
     */
    protected void setUseLocalImportedResults(boolean useLocalImportedResults) {
        this.useLocalImportedResults = useLocalImportedResults;
    }    

    /**
     * Get option to Use external imported results
     * @return true to Use external imported results, false to not
     */
    protected boolean getUseExternalImportedResults() {
        return useExternalImportedResults;
    }
    /**
     * Set option to Use imported results
     * @param useExternalImportedResults true to Use external imported results, false to not
     */
    protected void setUseExternalImportedResults(boolean useExternalImportedResults) {
        this.useExternalImportedResults = useExternalImportedResults;
    }    

    /**
     * Get option to Use as external result filename
     * @return string external result filename
     */
    protected String getExternalResultsFilename() {
        return externalResultsFilename;
    }
    /**
     * Set option to Use imported results
     * @param externalResultsFilename true to Use external imported results, false to not
     */
    protected void setExternalResultsFilename(String externalResultsFilename) {
        this.externalResultsFilename = externalResultsFilename;
    }    

    /**
     * Get option to Use coverage history to control build status
     * @return true to Use imported results, false to not
     */
    public boolean getUseCoverageHistory() {
        return useCoverageHistory;
    }
    /**
     * Set option to Use coverage history to control build status
     * @param useCoverageHistory true to Use imported results, false to not
     */
    protected void setUseCoverageHistory(boolean useCoverageHistory) {
        this.useCoverageHistory = useCoverageHistory;
    }    

    /**
     * Get for maxParallel to control maximum number of jobs to be queue at at any one point
     * @return MaxParallel integer number
     */
    protected Long getMaxParallel() {
        return maxParallel;
    }
    /**
     * Set option for maxParallel to control maximum number of jobs to be queue at at any one point
     * @param maxParallel Long number
     */
    protected void setMaxParallel(Long maxParallel) {
        this.maxParallel = maxParallel;
    }    
     /**
     * Get environment setup for windows
     * @return setup
     */
    protected String getUseCILicensesWin() {
        String ciEnvVars = "";
        
        if (useCILicenses) {
            ciEnvVars = "set VCAST_USING_HEADLESS_MODE=1\nset VCAST_USE_CI_LICENSES=1\n";
        }
        return ciEnvVars;
    }
    protected String getUseCILicensesUnix() {
        String ciEnvVars = "";
        
        if (useCILicenses) {
            ciEnvVars = "export VCAST_USING_HEADLESS_MODE=1\nexport VCAST_USE_CI_LICENSES=1\n";
        }
        return ciEnvVars;
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
    protected Project<?,?> getTopProject() {
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
     * Get pc-lint plus command
     * @return pc-lint plus command
     */
    protected String getPclpCommand() {
        return pclpCommand;
    }
    /**
     * Get pc-lint plus result pattern
     * @return pc-lint plus result pattern
     */
    protected String getPclpResultsPattern() {
        return pclpResultsPattern;
    }
    
    /**
     * Get command for running Squore
     * @return Squore command
     */
    protected String getSquoreCommand() {
        return squoreCommand;
    }    
    /**
     * Get URL for TESTinsights
     * @return TESTinsights URL
     */
    protected String getTESTinsights_URL() {
        return TESTinsights_URL;
    }    
    /**
     * Get Project for TESTinsights
     * @return TESTinsights Project
     */
    protected String getTESTinsights_project() {
        return TESTinsights_project;
    }    
    /**
     * Set Project for TESTinsights
     * @param TESTinsights_project  TESTinsights project name
     */
    protected void setTESTinsights_project(String TESTinsights_project) {
        this.TESTinsights_project = TESTinsights_project;
    }    
    /**
     * Get Proxy for TESTinsights
     * @return TESTinsights proxy
     */
    protected String getTESTinsights_proxy() {
        return TESTinsights_proxy;
    }    
    /**
     * Get Credentials for TESTinsights
     * @return TESTinsights Credentials
     */
    protected String getTESTinsights_credentials_id() {
        return TESTinsights_credentials_id;
    }        
    /**
     * Get SCM URL for TESTinsights
     * @return TESTinsights SCM URL
     */
    protected String getTESTinsights_SCM_URL() {
        return TESTinsights_SCM_URL;
    }    
    /**
     * Get SCM Technology TESTinsights
     * @return TESTinsights SCM Technology
     */
    protected String getTESTinsights_SCM_Tech() {
        return TESTinsights_SCM_Tech;
    }    
    /**
     * Set SCM URL for TESTinsights
     * @param TESTinsights_SCM_URL - String TESTinsights SCM URL
     */
     
    protected  void setTESTinsights_SCM_URL(String TESTinsights_SCM_URL) {
        this.TESTinsights_SCM_URL = TESTinsights_SCM_URL;
    }    
    /**
     * Set SCM Technology TESTinsights
     * @param TESTinsights_SCM_Tech - String TESTinsights SCM Techology (git or svn)
     */	 
    protected void setTESTinsights_SCM_Tech(String TESTinsights_SCM_Tech) {
        this.TESTinsights_SCM_Tech = TESTinsights_SCM_Tech;
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
     * Get the name of the project
     * @return the project name
     */
    public String getProjectName() {
        return projectName;
    }
    /**
     * Sets the name of the project
     * @param projectName - project name
     */
    public void setProjectName(final String projectName) {
        this.projectName = projectName;
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
    protected void addDeleteWorkspaceBeforeBuildStarts(Project<?,?> project) {
        if (optionClean) {
            PreBuildCleanup cleanup = new PreBuildCleanup(/*patterns*/null, true, /*cleanup param*/"", /*external delete*/"", /*disableDeferredWipeout*/ false);
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
            Logger.getLogger(BaseJob.class.getName()).log(Level.INFO, "Could not create topProject");
            return;
        }

        // Read the SCM setup
        scm = SCMS.parseSCM(request, topProject);
        if (scm == null) {
            scm = new NullSCM();
        }
        if (scm instanceof NullSCM) {
            usingSCM = false;
        } else {
            usingSCM = true;
            
            // for TESTinsights SCM connector
            String scmName = scm.getDescriptor().getDisplayName();
            if (scmName.equals("Git")) {
                TESTinsights_SCM_Tech = "git";
            } else if (scmName.equals("Subversion")) {
                TESTinsights_SCM_Tech = "svn";
            } else {
                TESTinsights_SCM_Tech = "";
            }
            Logger.getLogger(BaseJob.class.getName()).log(Level.INFO, "SCM Info: " + scmName);
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
    abstract protected Project<?,?> createProject() throws IOException, JobAlreadyExistsException;
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
    protected VectorCASTSetup addSetup(Project<?,?> project) throws IOException{
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
                                    useCILicenses,
                                    useStrictTestcaseImport,
                                    useRGW3,
                                    useImportedResults,
                                    useLocalImportedResults,
                                    useExternalImportedResults,
                                    externalResultsFilename,
                                    useCoverageHistory,
                                    waitLoops,
                                    waitTime,
                                    maxParallel,
                                    manageProjectName,
                                    jobName,
                                    nodeLabel,
                                    pclpCommand,
                                    pclpResultsPattern,
                                    squoreCommand,
                                    TESTinsights_URL,
                                    TESTinsights_project,
                                    TESTinsights_credentials_id,
                                    TESTinsights_proxy,
                                    TESTinsights_SCM_URL,
                                    TESTinsights_SCM_Tech);
                                    
        setup.setUsingSCM(usingSCM);
        setup.setSCM(scm);

        project.getBuildersList().add(setup);
        
        return setup;
    }
    /**
     * Add archive artifacts step
     * @param project project to add to
     */
    protected void addArchiveArtifacts(Project<?,?> project) {
        String pclpArchive = "";
        String TIArchive = "";
        
        if (pclpCommand.length() != 0) {
            pclpArchive = ", " + pclpResultsPattern;
        }
        if (TESTinsights_URL.length() != 0) {
            TIArchive = ", TESTinsights_Push.log";            
        }
        String addToolsArchive = pclpArchive + TIArchive;
        String defaultArchive = "**/*.html, xml_data/*.xml, unit_test_fail_count.txt, **/*.png, **/*.css, complete_build.log, *_results.vcr";
        
        ArtifactArchiver archiver = new ArtifactArchiver(defaultArchive + addToolsArchive);
        archiver.setExcludes("");
        archiver.setAllowEmptyArchive(false);
        project.getPublishersList().add(archiver);
    }

    /**
     * Add archive artifacts step
     * @param project project to add to
     */
    protected void addCopyResultsToImport(Project<?,?> project) {
        StatusBuildSelector selector = new StatusBuildSelector();
        CopyArtifact archiverCopier = new CopyArtifact(project.getName());
        archiverCopier.setParameters("");
        archiverCopier.setSelector(selector);
        archiverCopier.setFilter(baseName + "_results.vcr");
        archiverCopier.setTarget("");
        archiverCopier.setFlatten(false);
        archiverCopier.setOptional(true);
        
        project.getBuildersList().add(archiverCopier);
    }

    /**
     * Add JUnit rules step
     * @param project project to add step to
     */

    protected void addJunit(Project<?,?> project) {
        JUnitResultArchiver junit = new JUnitResultArchiver("**/test_results_*.xml");
        project.getPublishersList().add(junit);
    }
    /**
     * Add PC-Lint Plus step
     * @param project project to add step to do PC-Lint Plus
     */

    protected void addPCLintPlus(Project<?,?> project) {
        if (pclpCommand.length() != 0) {
            IssuesRecorder recorder = new IssuesRecorder();

            PcLint pcLintPlus = new PcLint();
            pcLintPlus.setPattern(pclpResultsPattern);
            pcLintPlus.setReportEncoding("UTF-8");
            pcLintPlus.setSkipSymbolicLinks(false);
            
            recorder.setTools(pcLintPlus);

            project.getPublishersList().add(recorder);
        }
    }
    /**
     * Add VectorCAST coverage reporting step
     * @param project project to add step to
     */
    protected void addVCCoverage(Project<?,?> project) {
        VectorCASTHealthReportThresholds healthReports = new VectorCASTHealthReportThresholds(0, 100, 0, 70, 0, 80, 0, 80, 0, 80, 0, 80);
        VectorCASTPublisher publisher = new VectorCASTPublisher();
        publisher.includes = "**/coverage_results_*.xml";
        publisher.healthReports = healthReports;
        publisher.setUseCoverageHistory(useCoverageHistory);
        project.getPublishersList().add(publisher);
    }
    /**
     * Add Jenkins coverage reporting step
     * @param project project to add step to
     */
    protected void addJenkinsCoverage(Project<?,?> project) {
        CoverageTool tool = new CoverageTool();
        tool.setParser(Parser.VECTORCAST);
        tool.setPattern("xml_data/cobertura/coverage_results*.xml");        
        List<CoverageTool> list = new ArrayList<CoverageTool>();
        list.add(tool);

        CoverageRecorder publisher = new CoverageRecorder();
        publisher.setTools(list);
        
        project.getPublishersList().add(publisher);
    }
    protected void addCredentialID(Project<?,?> project) {
        project.getBuildWrappersList().add(new SecretBuildWrapper(Collections.<MultiBinding<?>>singletonList(
                new UsernamePasswordMultiBinding("VC_TI_USR","VC_TI_PWS",TESTinsights_credentials_id))));
    }
}
