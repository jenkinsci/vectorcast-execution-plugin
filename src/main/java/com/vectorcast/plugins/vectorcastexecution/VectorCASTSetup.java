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
package com.vectorcast.plugins.vectorcastexecution;

import com.vectorcast.plugins.vectorcastexecution.common.VcastUtils;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * VectorCAST setup build action
 */
public class VectorCASTSetup extends Builder implements SimpleBuildStep {
    /** script directory */
    private static final String SCRIPT_DIR = "/scripts/";

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
    /** Use CI License */
    private boolean useCILicenses;
    /** Use strict testcase import */
    private boolean useStrictTestcaseImport;
    /** Use RGW3 */
    private boolean useRGW3;
    /** Use imported results */
    private boolean useImportedResults = false;
    private boolean useLocalImportedResults = false;
    private boolean useExternalImportedResults = false;
    private boolean useCoveragePlugin = true;
    private String  externalResultsFilename;

    /** Use coverage history to control build status */
    private boolean useCoverageHistory;
    /** Wait loops */
    private Long waitLoops;
    /** Wait time */
    private Long waitTime;
    /** Maximum number of parallal jobs to queue up */
    private Long maxParallel;
    /** Using some form of SCM */
    private boolean usingSCM;
    /** SCM if using */
    private SCM scm;
    /** Manage project name */
    private String manageProjectName;
    /** Base Job name */
    private String jobName;
    /** Node label */
    private String nodeLabel;
    /** PC Lint Plus Command */
    private String pclpCommand = "";
    /** PC Lint Plus Path */
    private String pclpResultsPattern;
    /** PC Lint Plus Path */
    private String squoreCommand;
    
    /** TESTinsights Push information **/
    private String TESTinsights_URL;
    private String TESTinsights_project;
    private String TESTinsights_credentials_id;
    private String TESTinsights_proxy;
    private String TESTinsights_SCM_URL;
    private String TESTinsights_SCM_Tech;

    /**
     * Get the number of wait loops to do
     * @return number of loops
     */
    public Long getWaitLoops() {
        return waitLoops;
    }
    /**
     * Set the number of wait loops
     * @param waitLoops number of loops
     */
    public void setWaitLoops(Long waitLoops) {
        this.waitLoops = waitLoops;
    }
    /**
     * Get the wait time for license retries
     * @return the wait time
     */
    public Long getWaitTime() {
        return waitTime;
    }
    /**
     * Set the wait time for license retries
     * @param waitTime the wait time
     */
    public void setWaitTime(Long waitTime) {
        this.waitTime = waitTime;
    }
    /**
     * Get for maxParallel to control maximum number of jobs to be queue at at any one point
     * @return maxParallel Long number
     */
    public Long getMaxParallel() {
        return maxParallel;
    }
    /**
     * Set option for maxParallel to control maximum number of jobs to be queue at at any one point
     * @param maxParallel Long number
     */
    public void setMaxParallel(Long maxParallel) {
        this.maxParallel = maxParallel;
    }
    /**
     * Get environment for windows setup
     * @return environment setup
     */
    public String getEnvironmentSetupWin() {
        return environmentSetupWin;
    }
    /**
     * Set environment setup for windows
     * @param environmentSetupWin environment setup
     */
    public void setEnvironmentSetupWin(String environmentSetupWin) {
        this.environmentSetupWin = environmentSetupWin;
    }
    /**
     * Get environment setup for unix
     * @return environment setup
     */
    public String getEnvironmentSetupUnix() {
        return environmentSetupUnix;
    }
    /**
     * Set environment setup for unix
     * @param environmentSetupUnix environment setup
     */
    public void setEnvironmentSetupUnix(String environmentSetupUnix) {
        this.environmentSetupUnix = environmentSetupUnix;
    }
    /**
     * Get execute preamble for windows
     * @return execute preamble
     */
    public String getExecutePreambleWin() {
        return executePreambleWin;
    }
    /**
     * Set execute preamble for windows
     * @param executePreambleWin execute preamble
     */
    public void setExecutePreambleWin(String executePreambleWin) {
        this.executePreambleWin = executePreambleWin;
    }
    /**
     * Get execute preamble for unix
     * @return execute preamble
     */
    public String getExecutePreambleUnix() {
        return executePreambleUnix;
    }
    /**
     * Set execute preamble for unix
     * @param executePreambleUnix execute preamble
     */
    public void setExecutePreambleUnix(String executePreambleUnix) {
        this.executePreambleUnix = executePreambleUnix;
    }
    /**
     * Get environment teardown for windows
     * @return environment teardown
     */
    public String getEnvironmentTeardownWin() {
        return environmentTeardownWin;
    }
    /**
     * Set environment teardown for windows
     * @param environmentTeardownWin environment teardown
     */
    public void setEnvironmentTeardownWin(String environmentTeardownWin) {
        this.environmentTeardownWin = environmentTeardownWin;
    }
    /**
     * Get environment teardown for unix
     * @return environment teardown
     */
    public String getEnvironmentTeardownUnix() {
        return environmentTeardownUnix;
    }
    /**
     * Set environment teardown for unix
     * @param environmentTeardownUnix environment teardown
     */
    public void setEnvironmentTeardownUnix(String environmentTeardownUnix) {
        this.environmentTeardownUnix = environmentTeardownUnix;
    }
    /**
     * Get option to use reporting
     * @return true/false
     */
    public boolean getOptionUseReporting() {
        return optionUseReporting;
    }
    /**
     * Set option to use reporting
     * @param optionUseReporting true/false
     */
    public void setOptionUseReporting(boolean optionUseReporting) {
        this.optionUseReporting = optionUseReporting;
    }
    /**
     * Get option error level
     * @return error level
     */
    public String getOptionErrorLevel() {
        return optionErrorLevel;
    }
    /**
     * Set option error level
     * @param optionErrorLevel error level
     */
    public void setOptionErrorLevel(String optionErrorLevel) {
        this.optionErrorLevel = optionErrorLevel;
    }
    /**
     * Get option for HTML Build Description
     * @return "HTML" or "TEXT"
     */
    public String getOptionHtmlBuildDesc() {
        return optionHtmlBuildDesc;
    }
    /**
     * Set option for HTML build description     * 
     * @param optionHtmlBuildDesc HTML or TEXT
     */
    public void setOptionHtmlBuildDesc(String optionHtmlBuildDesc) {
        this.optionHtmlBuildDesc = optionHtmlBuildDesc;
    }
    /**
     * Get option for execution report
     * @return true/false
     */
    public boolean getOptionExecutionReport() {
        return optionExecutionReport;
    }
    /**
     * Set option for execution report
     * @param optionExecutionReport true/false
     */
    public void setOptionExecutionReport(boolean optionExecutionReport) {
        this.optionExecutionReport = optionExecutionReport;
    }
    /**
     * Get option for cleaning workspace
     * @return true/false
     */
    public boolean getOptionClean() {
        return optionClean;
    }
    /**
     * Set option for cleaning workspace
     * @param optionClean true/false
     */
    public void setOptionClean(boolean optionClean) {
        this.optionClean = optionClean;
    }
    /**
     * Get option to use CI licenses
     * @return true to use CI licenses, false to not
     */
    public boolean getUseCILicenses() {
        return useCILicenses;
    }
    /**
     * Set option to use CI licenses
     * @param useCILicenses  true to use CI licenses, false to not
     */
    public void setUseCILicenses(boolean useCILicenses) {
        this.useCILicenses = useCILicenses;
    }    
    /**
     * Get option to Use strict testcase import
     * @return true to Use strict testcase import, false to not
     */
    public boolean getUseStrictTestcaseImport() {
        return useStrictTestcaseImport;
    }
    /**
     * Set option to Use strict testcase import
     * @param useStrictTestcaseImport  true to Use strict testcase import, false to not
     */
    public void setUseStrictTestcaseImport(boolean useStrictTestcaseImport) {
        this.useStrictTestcaseImport = useStrictTestcaseImport;
    }    
    
    /**
     * Get option to Use RGW3 capabilities
     * @return true use RGW3 capabilities, false to not
     */
    public boolean getUseRGW3() {
        return useRGW3;
    }
    /**
     * Set option to use RGW3 capabilities
     * @param useRGW3 true to allow RGW3 test cases to run and export
     */
    public void setUseRGW3(boolean useRGW3) {
        this.useRGW3 = useRGW3;
    }    
    /**
     * Get option to use coverage plugin or vectorcast coverage plugin
     * @return true use coverage plugin or vectorcast coverage plugin
     */
    public boolean getUseCoveragePlugin() {
        return useCoveragePlugin;
    }
    /**
     * Set option to use coverage plugin or vectorcast coverage plugin
     * @param useCoveragePlugin use coverage plugin or vectorcast coverage plugin
     */
    public void setUseCoveragePlugin(boolean useCoveragePlugin) {
        this.useCoveragePlugin = useCoveragePlugin;
    }    
    /**
     * Get option to Use imported results
     * @return true to Use imported results, false to not
     */
    public boolean getUseImportedResults() {
        return useImportedResults;
    }
    /**
     * Set option to Use imported results
     * @param useImportedResults true to Use imported results, false to not
     */
    public void setUseImportedResults(boolean useImportedResults) {
        this.useImportedResults = useImportedResults;
    }   
    
    /**
     * Get option to Use local imported results
     * @return true to Use local imported results, false to not
     */
    public boolean getUseLocalImportedResults() {
        return useLocalImportedResults;
    }
    /**
     * Set option to Use imported results
     * @param useLocalImportedResults true to Use local imported results, false to not
     */
    public void setUseLocalImportedResults(boolean useLocalImportedResults) {
        this.useLocalImportedResults = useLocalImportedResults;
    }    

    /**
     * Get option to Use external imported results
     * @return true to Use external imported results, false to not
     */
    public boolean getUseExternalImportedResults() {
        return useExternalImportedResults;
    }
    /**
     * Set option to Use imported results
     * @param useExternalImportedResults true to Use external imported results, false to not
     */
    public void setUseExternalImportedResults(boolean useExternalImportedResults) {
        this.useExternalImportedResults = useExternalImportedResults;
    }    

      /**
     * Get option to Use as external result filename
     * @return string external result filename
     */
    public String getExternalResultsFilename() {
        return externalResultsFilename;
    }
    /**
     * Set option to Use imported results
     * @param externalResultsFilename true to Use external imported results, false to not
     */
    public void setExternalResultsFilename(String externalResultsFilename) {
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
    public void setUseCoverageHistory(boolean useCoverageHistory) {
        this.useCoverageHistory = useCoverageHistory;
    }    
    /**
     * Get using SCM
     * @return true/false
     */
    public boolean getUsingSCM() {
        return usingSCM;
    }
    /**
     * Set using SCM (true yes, false no)
     * @param usingSCM true/false
     */
    public void setUsingSCM(boolean usingSCM) {
        this.usingSCM = usingSCM;
    }
    /**
     * Get the SCM to use
     * @return SCM
     */
    public SCM getSCM() {
        return scm;
    }
    /**
     * Set the SCM being used
     * @param scm SCM
     */
    public void setSCM(SCM scm) {
        this.scm = scm;
    }
    /**
     * Get the Manage project file/name
     * @return Manage project name
     */
    public String getManageProjectName() {
        return manageProjectName;
    }
    /**
     * Set the Manage project file/name
     * @param manageProjectName Manage project name
     */
    public void setManageProjectName(String manageProjectName) {
        this.manageProjectName = manageProjectName;
    }
    /**
     * Get the job name
     * @return job name
     */
    public String getJobName() {
        return jobName;
    }
    /**
     * Set the job name
     * @param jobName job name
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
    /**
     * Get the node label
     * @return node label
     */
    public String getNodeLabel() {
        return nodeLabel;
    }
    /**
     * Set the node label
     * @param nodeLabel node label
     */
    public void setNodeLabel(String nodeLabel) {
        this.nodeLabel = nodeLabel;
    }
    /**
     * Get pc-lint plus command
     * @return pc-lint plus command
     */
    public String getPclpCommand() {
        return pclpCommand;
    }
    /**
     * Get pc-lint plus command
     * @param pclpCommand - Pc Lint Plus Command
     */
    public void setPclpCommand(String pclpCommand) {
        this.pclpCommand = pclpCommand;
    }
    /**
     * Get using pc-lint plus command
     * @return true/false if we have a PC Lint Command
     */
    public boolean getUsingPCLP() {
        return pclpCommand.length() != 0;
    }
    /**
     * Get pc-lint plus result pattern
     * @return pc-lint plus result pattern
     */
    public String getPclpResultsPattern() {
        return pclpResultsPattern;
    }
    /**
     * Get pc-lint plus result pattern
     * @param pclpResultsPattern - PC Lint Result pattern
     */
    public void setPclpResultsPattern(String pclpResultsPattern) {
        this.pclpResultsPattern = pclpResultsPattern;
    }
    
    /**
     * Get using getUsingPCLP command
     * @return true/false if we have a squoreCommand
     */
    public boolean getUsingSquoreCommand() {
        return squoreCommand.length() != 0;
    }

    /**
     * Get Squore command
     * @return Squore command
     */
    public String getSquoreCommand() {
        return squoreCommand;
    }

    /**
     * Set Squore command
     * @param squoreCommand - Squore Command
     */
    public void setSquoreCommand(String squoreCommand) {
        this.squoreCommand = squoreCommand;
    }

    /**
     * Get URL for TESTinsights
     * @return TESTinsights URL
     */
    public String getTESTinsights_URL() {
        return TESTinsights_URL;
    }    
    /**
     * Set URL for TESTinsights
     * @param TESTinsights_URL - TESTinsights URL
     */
    public void setTESTinsights_URL(String TESTinsights_URL) {
        this.TESTinsights_URL = TESTinsights_URL;
    }    
    /**
     * Get Project for TESTinsights
     * @return TESTinsights Project
     */
    public String getTESTinsights_project() {
        return TESTinsights_project;
    }    
    /**
     * Set Project for TESTinsights
     * @param  TESTinsights_project - Project for TESTinsights
     */
    public void setTESTinsights_project(String TESTinsights_project) {
        this.TESTinsights_project = TESTinsights_project;
    }    
    /**
     * Get Proxy for TESTinsights
     * @return TESTinsights proxy
     */
    public String getTESTinsights_proxy() {
        return TESTinsights_proxy;
    }    
    /**
     * Set Proxy for TESTinsights
     * @param TESTinsights_proxy TESTinsights proxy
     */
    public void setTESTinsights_proxy(String TESTinsights_proxy) {
        this.TESTinsights_proxy = TESTinsights_proxy;
    }    
    /**
     * Get Credentials ID for TESTinsights
     * @return TESTinsights Credentials
     */
    public String getTESTinsights_credentials_id() {
        return TESTinsights_credentials_id;
    }        
    /**
     * Set Credentials ID for TESTinsights
     * @param TESTinsights_credentials_id - Credentials ID for TESTinsights
     */
    public void setTESTinsights_credentials_id(String TESTinsights_credentials_id) {
        this.TESTinsights_credentials_id = TESTinsights_credentials_id;
    }    
    /**
     * Get SCM URL for TESTinsights
     * @return TESTinsights SCM URL
     */
    public String getTESTinsights_SCM_URL() {
        return TESTinsights_SCM_URL;
    }    
    /**
     * Get SCM Technology TESTinsights
     * @return TESTinsights SCM Technology
     */
    public String getTESTinsights_SCM_Tech() {
        return TESTinsights_SCM_Tech;
    }    
    /**
     * Set SCM URL for TESTinsights
     * @param TESTinsights_SCM_URL - URL for TESTinsights
     */
    public void setTESTinsights_SCM_URL(String TESTinsights_SCM_URL) {
        this.TESTinsights_SCM_URL = TESTinsights_SCM_URL;
    }    
    /**
     * Set SCM Technology TESTinsights
     * @param TESTinsights_SCM_Tech - SCM Technology TESTinsights (git or svn)
     */

    public void setTESTinsights_SCM_Tech(String TESTinsights_SCM_Tech) {
        this.TESTinsights_SCM_Tech = TESTinsights_SCM_Tech;
    }    
    /**
     * Create setup step
     * @param environmentSetupWin environment setup for windows
     * @param environmentSetupUnix environment setup for unix
     * @param executePreambleWin execute preamble for windows
     * @param executePreambleUnix execute preamble for unix
     * @param environmentTeardownWin environment teardown for windows
     * @param environmentTeardownUnix environment teardown for unix
     * @param optionUseReporting use reporting
     * @param optionErrorLevel error level
     * @param optionHtmlBuildDesc HTML Build description
     * @param optionExecutionReport execution report
     * @param optionClean clean
     * @param useCILicenses use CI licenses
     * @param useStrictTestcaseImport Use strict testcase import
     * @param useRGW3 Use RGW3 capabilities
     * @param useImportedResults use imported results
     * @param useLocalImportedResults use local imported results
     * @param useExternalImportedResults use extern imported results
     * @param externalResultsFilename use extern result filename
     * @param useCoverageHistory use imported results
     * @param waitLoops wait loops
     * @param waitTime wait time
     * @param maxParallel maximum number of jobs to queue in parallel
     * @param manageProjectName manage project name
     * @param jobName job name
     * @param nodeLabel node label
     * @param pclpCommand PC Lint Plus command
     * @param pclpResultsPattern PC Lint Plus result patter
     * @param squoreCommand Squore command
     * @param TESTinsights_URL URL for TESTinsights
     * @param TESTinsights_project Project for for TESTinsights
     * @param TESTinsights_credentials_id Credentials for for TESTinsights
     * @param TESTinsights_proxy Proxy for for TESTinsights
     * @param TESTinsights_SCM_URL SCM URL for for TESTinsights
     * @param TESTinsights_SCM_Tech SCM technology for for TESTinsights
     */
    @DataBoundConstructor
    public VectorCASTSetup(String environmentSetupWin,
                           String environmentSetupUnix,
                           String executePreambleWin,
                           String executePreambleUnix,
                           String environmentTeardownWin,
                           String environmentTeardownUnix,
                           boolean optionUseReporting,
                           String optionErrorLevel,
                           String optionHtmlBuildDesc,
                           boolean optionExecutionReport,
                           boolean optionClean,
                           boolean useCILicenses,
                           boolean useStrictTestcaseImport,
                           boolean useRGW3,
                           boolean useImportedResults,
                           boolean useLocalImportedResults,
                           boolean useExternalImportedResults,
                           String  externalResultsFilename,
                           boolean useCoverageHistory,
                           Long waitLoops,
                           Long waitTime,
                           Long maxParallel,
                           String manageProjectName,
                           String jobName,
                           String nodeLabel,
                           String pclpCommand,
                           String pclpResultsPattern,
                           String squoreCommand,
                           String TESTinsights_URL,
                           String TESTinsights_project,
                           String TESTinsights_credentials_id,
                           String TESTinsights_proxy,
                           String TESTinsights_SCM_URL,
                           String TESTinsights_SCM_Tech) {
        this.environmentSetupWin = environmentSetupWin;
        this.environmentSetupUnix = environmentSetupUnix;
        this.executePreambleWin = executePreambleWin;
        this.executePreambleUnix = executePreambleUnix;
        this.environmentTeardownWin = environmentTeardownWin;
        this.environmentTeardownUnix = environmentTeardownUnix;
        this.optionUseReporting = optionUseReporting;
        this.optionErrorLevel = optionErrorLevel;
        this.optionHtmlBuildDesc = optionHtmlBuildDesc;
        this.optionExecutionReport = optionExecutionReport;
        this.optionClean = optionClean;
        this.useCILicenses = useCILicenses;
        this.useStrictTestcaseImport = useStrictTestcaseImport;
        this.useRGW3 = useRGW3;
        this.useImportedResults = useImportedResults;
        this.useLocalImportedResults = useLocalImportedResults;
        this.useExternalImportedResults = useExternalImportedResults;
        this.externalResultsFilename = externalResultsFilename;
        this.useCoverageHistory = useCoverageHistory;
        this.usingSCM = false;
        this.scm = new NullSCM();
        this.waitLoops = waitLoops;
        this.waitTime = waitTime;
        this.maxParallel = maxParallel;
        this.manageProjectName = manageProjectName;
        this.jobName = jobName;
        this.nodeLabel = nodeLabel;
        this.pclpCommand = pclpCommand;
        this.pclpResultsPattern = pclpResultsPattern;
        this.squoreCommand = squoreCommand;
        this.TESTinsights_URL = TESTinsights_URL;
        this.TESTinsights_project = TESTinsights_project;
        this.TESTinsights_credentials_id = TESTinsights_credentials_id;
        this.TESTinsights_proxy = TESTinsights_proxy;
        this.TESTinsights_SCM_URL = TESTinsights_SCM_URL;
        this.TESTinsights_SCM_Tech = TESTinsights_SCM_Tech;
    }
    /**
     * Copy the files in a directory recursively to the job workspace.
     * This is used when the source is NOT a jar file
     * 
     * @param dir directory to process
     * @param base base
     * @param destDir destination directory
     * @throws IOException exception
     * @throws InterruptedException exception
     */
    private void processDir(File dir, String base, FilePath destDir, Boolean directDir) throws IOException, InterruptedException {
        destDir.mkdirs();
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                FilePath newDest = new FilePath(destDir, file.getName());
                processDir(file, base + "/" + file.getName(), newDest, directDir);
            } else {
                if (directDir) {
                    File newFile = new File(destDir + File.separator + file.getName());
                    FileUtils.copyFile(file, newFile);
                } else {
                    FilePath newFile = new FilePath(destDir, file.getName());
                    try (InputStream is = VectorCASTSetup.class.getResourceAsStream(SCRIPT_DIR + base + "/" + file.getName())) {
                        newFile.copyFrom(is);
                    }
                }
            }
        }
    }

    private void printVersion( PrintStream logger )
    {
	logger.println( "[VectorCAST Execution Version]: " + VcastUtils.getVersion().orElse( "Error - Could not determine version" ) );
    }

    /**
     * Perform the build step. Copy the scripts from the archive/directory to the workspace
     * @param build build
     * @param workspace workspace
     * @param launcher launcher
     * @param listener  listener
	 * @throws IOException      exception
     */    
    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException {
        FilePath destScriptDir = new FilePath(workspace, "vc_scripts");
        JarFile jFile = null;
        try {
            String path = null;
            String override_path = System.getenv("VCAST_VC_SCRIPTS");
            String extra_script_path = SCRIPT_DIR;
            Boolean directDir = false;
            if (override_path != null && !override_path.isEmpty()) {
                path = override_path;
                extra_script_path = "";
                directDir = true;
                String msg = "VectorCAST - overriding vc_scripts. Copying from '" + path + "'";
                Logger.getLogger(VectorCASTSetup.class.getName()).log(Level.ALL, msg);
            } else {
                path = VectorCASTSetup.class.getProtectionDomain().getCodeSource().getLocation().getPath();
                path = URLDecoder.decode(path, "utf-8");
            }
            File testPath = new File(path);
            printVersion( listener.getLogger() );
            if (testPath.isFile()) {
                // Have jar file...
                jFile = new JarFile(testPath);
                Enumeration<JarEntry> entries = jFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("scripts")) {
                        String fileOrDir = entry.getName().substring(8); // length of scripts/
                        FilePath dest = new FilePath(destScriptDir, fileOrDir);
                        if (entry.getName().endsWith("/")) {
                            // Directory, create destination
                            dest.mkdirs();
                        } else {
                            // File, copy it
                            InputStream is = VectorCASTSetup.class.getResourceAsStream("/" + entry.getName());
                            dest.copyFrom(is);
                        }
                    }
                }
            } else {
                // Have directory
                File scriptDir = new File(path + extra_script_path);
                processDir(scriptDir, "./", destScriptDir, directDir);
            }
        } catch (IOException ex) {
            Logger.getLogger(VectorCASTSetup.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(VectorCASTSetup.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (jFile != null) {
                try {
                    jFile.close();
                } catch (IOException ex) {
                    assert true;
                }
            }
        }
        
        // clean up old xml_data files
        
        File[] files = new File(workspace + "/xml_data/").listFiles();
        if (files != null)
        {
            for (File file : files) 
            {
                if (file.isFile() && !file.delete()) {
                    throw new IOException("Unable to delete file: " + file.getAbsolutePath());   
                }
            }
        }
    }
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    /**
     * Descriptor for {@link VectorCASTSetup}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }
        /**
         * This human readable name is used in the configuration screen.
         * @return the display name
         */
        @Override
        public String getDisplayName() {
            return Messages.VectorCASTSetup_DisplayName();
        }
    }
    
    @Override
    public String toString() {
    	
    	String string = "\nVectorCASTSetup: \n"
    			+ "\t environmentSetupUnix: " + environmentSetupUnix +  "\n"
    			+ "\t executePreambleWin: " + executePreambleWin +  "\n"
    			+ "\t executePreambleUnix: " + executePreambleUnix +  "\n"
    			+ "\t environmentTeardownWin: " + environmentTeardownWin +  "\n"
    			+ "\t environmentTeardownUnix: " + environmentTeardownUnix +  "\n"
    			+ "\t optionUseReporting: " + optionUseReporting +  "\n"
    			+ "\t optionErrorLevel: " + optionErrorLevel +  "\n"
    			+ "\t optionHtmlBuildDesc: " + optionHtmlBuildDesc +  "\n"
    			+ "\t optionHtmlBuildDesc: " + optionHtmlBuildDesc +  "\n"
    			+ "\t optionExecutionReport: " + optionExecutionReport +  "\n"
    			+ "\t optionClean: " + optionClean +  "\n"
    			+ "\t useCILicenses: " + useCILicenses +  "\n"
    			+ "\t useStrictTestcaseImport: " + useStrictTestcaseImport +  "\n"
    			+ "\t useRGW3: " + useRGW3 +  "\n"
    			+ "\t useImportedResults: " + useImportedResults +  "\n"
    			+ "\t useLocalImportedResults: " + useLocalImportedResults +  "\n"
    			+ "\t useExternalImportedResults: " + useExternalImportedResults +  "\n"
    			+ "\t externalResultsFilename: " + externalResultsFilename +  "\n"
    			+ "\t useCoverageHistory: " + useCoverageHistory +  "\n"
    			+ "\t usingSCM: " + usingSCM +  "\n"
    			+ "\t scm: " + scm +  "\n"
    			+ "\t waitLoops: " + waitLoops +  "\n"
    			+ "\t waitTime: " + waitTime +  "\n"
    			+ "\t maxParallel: " + maxParallel +  "\n"
    			+ "\t manageProjectName: " + manageProjectName +  "\n"
    			+ "\t jobName: " + jobName +  "\n"
    			+ "\t nodeLabel: " + nodeLabel +  "\n"
    			+ "\t pclpCommand: " + pclpCommand +  "\n"
    			+ "\t pclpResultsPattern: " + pclpResultsPattern +  "\n"
    			+ "\t squoreCommand: " + squoreCommand +  "\n"
    			+ "\t TESTinsights_URL: " + TESTinsights_URL +  "\n"
    			+ "\t TESTinsights_project: " + TESTinsights_project +  "\n"
    			+ "\t TESTinsights_credentials_id: " + TESTinsights_credentials_id +  "\n"
    			+ "\t TESTinsights_proxy: " + TESTinsights_proxy +  "\n"
    			+ "\t TESTinsights_SCM_URL: " + TESTinsights_SCM_URL +  "\n"
    			+ "\t TESTinsights_SCM_Tech: " + TESTinsights_SCM_Tech +  "\n";
    	return string;
    }
}
