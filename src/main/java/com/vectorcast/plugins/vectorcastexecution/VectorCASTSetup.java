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
import java.io.FileInputStream;
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
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.EnvVars;

/**
 * VectorCAST setup build action.
 */
public class VectorCASTSetup extends Builder implements SimpleBuildStep {
    /** script directory. */
    private static final String SCRIPT_DIR = "/scripts/";

    /** Environment setup for windows. */
    private String environmentSetupWin;
    /** Environment setup for unix. */
    private String environmentSetupUnix;
    /** Execute preamble for windows. */
    private String executePreambleWin;
    /** Execute preamble for unix. */
    private String executePreambleUnix;
    /** Environment tear down for windows. */
    private String environmentTeardownWin;
    /** Environment tear down for unix. */
    private String environmentTeardownUnix;
    /** Use Jenkins reporting. */
    private boolean optionUseReporting;
    /** What error-level to use. */
    private String optionErrorLevel;
    /** Use HTML in build description. */
    private String optionHtmlBuildDesc;
    /** Generate execution report. */
    private boolean optionExecutionReport;
    /** Clean workspace. */
    private boolean optionClean;
    /** Use CI License. */
    private boolean useCILicenses;
    /** Use strict testcase import. */
    private boolean useStrictTestcaseImport;
    /** Use RGW3. */
    private boolean useRGW3;
    /** Use imported results. */
    private boolean useImportedResults = false;
    /** Use local/artifact imported results. */
    private boolean useLocalImportedResults = false;
    /** Use external file for imported results. */
    private boolean useExternalImportedResults = false;
    /** Filename for external results. */
    private String  externalResultsFilename;
    /** Use the coverage plugin. */
    private boolean useCoveragePlugin = true;
    /** Use coverage history to control build status. */
    private boolean useCoverageHistory;
    /** Wait loops. */
    private Long waitLoops;
    /** Wait time. */
    private Long waitTime;
    /** Maximum number of parallal jobs to queue up. */
    private Long maxParallel;
    /** Using some form of SCM. */
    private boolean usingSCM;
    /** SCM if using. */
    private SCM scm;
    /** Manage project name. */
    private String manageProjectName;
    /** Base Job name. */
    private String jobName;
    /** Node label. */
    private String nodeLabel;
    /** PC Lint Plus Command. */
    private String pclpCommand = "";
    /** PC Lint Plus Path. */
    private String pclpResultsPattern;
    /** PC Lint Plus Path. */
    private String squoreCommand;

    /** TESTinsights URL information. */
    private String testInsightsUrl;
    /** TESTinsights Project information. */
    private String testInsightsProject;
    /** TESTinsights credentials information. */
    private String testInsightsCredentialsId;
    /** TESTinsights Proxy information. */
    private String testInsightsProxy;
    /** TESTinsights SCM information. */
    private String testInsightsScmUrl;
    /** TESTinsights SCM Tech information. */
    private String testInsightsScmTech;


    /**
     * Get the number of wait loops to do.
     * @return number of loops
     */
    public Long getWaitLoops() {
        return waitLoops;
    }
    /**
     * Set the number of wait loops.
     * @param loops number of loops
     */
    public void setWaitLoops(final Long loops) {
        this.waitLoops = loops;
    }
    /**
     * Get the wait time for license retries.
     * @return the wait time
     */
    public Long getWaitTime() {
        return waitTime;
    }
    /**
     * Set the wait time for license retries.
     * @param time the wait time
     */
    public void setWaitTime(final Long time) {
        this.waitTime = time;
    }
    /**
     * Get for maxParallel to control maximum number of
     * jobs to be queue at at any one point.
     * @return maxParallel Long number
     */
    public Long getMaxParallel() {
        return maxParallel;
    }
    /**
     * Set option for maxParallel to control maximum number of
     * jobs to be queue at at any one point.
     * @param max Long number
     */
    public void setMaxParallel(final Long max) {
        this.maxParallel = max;
    }
    /**
     * Get environment for windows setup.
     * @return environment setup
     */
    public String getEnvironmentSetupWin() {
        return environmentSetupWin;
    }
    /**
     * Set environment setup for windows.
     * @param winSetup environment setup
     */
    public void setEnvironmentSetupWin(final String winSetup) {
        this.environmentSetupWin = winSetup;
    }
    /**
     * Get environment setup for unix.
     * @return environment setup
     */
    public String getEnvironmentSetupUnix() {
        return environmentSetupUnix;
    }
    /**
     * Set environment setup for unix.
     * @param setupUnix environment setup
     */
    public void setEnvironmentSetupUnix(final String setupUnix) {
        this.environmentSetupUnix = setupUnix;
    }
    /**
     * Get execute preamble for windows.
     * @return execute preamble
     */
    public String getExecutePreambleWin() {
        return executePreambleWin;
    }
    /**
     * Set execute preamble for windows.
     * @param preamble execute preamble
     */
    public void setExecutePreambleWin(final String preamble) {
        this.executePreambleWin = preamble;
    }
    /**
     * Get execute preamble for unix.
     * @return execute preamble
     */
    public String getExecutePreambleUnix() {
        return executePreambleUnix;
    }
    /**
     * Set execute preamble for unix.
     * @param preamble execute preamble
     */
    public void setExecutePreambleUnix(final String preamble) {
        this.executePreambleUnix = preamble;
    }
    /**
     * Get environment teardown for windows.
     * @return environment teardown
     */
    public String getEnvironmentTeardownWin() {
        return environmentTeardownWin;
    }
    /**
     * Set environment teardown for windows.
     * @param teardown environment teardown
     */
    public void setEnvironmentTeardownWin(final String teardown) {
        this.environmentTeardownWin = teardown;
    }
    /**
     * Get environment teardown for unix.
     * @return environment teardown
     */
    public String getEnvironmentTeardownUnix() {
        return environmentTeardownUnix;
    }
    /**
     * Set environment teardown for unix.
     * @param teardown environment teardown
     */
    public void setEnvironmentTeardownUnix(final String teardown) {
        this.environmentTeardownUnix = teardown;
    }
    /**
     * Get option to use reporting.
     * @return true/false
     */
    public boolean getOptionUseReporting() {
        return optionUseReporting;
    }
    /**
     * Set option to use reporting.
     * @param reporting true/false
     */
    public void setOptionUseReporting(final boolean reporting) {
        this.optionUseReporting = reporting;
    }
    /**
     * Get option error level.
     * @return error level
     */
    public String getOptionErrorLevel() {
        return optionErrorLevel;
    }
    /**
     * Set option error level.
     * @param error error level
     */
    public void setOptionErrorLevel(final String error) {
        this.optionErrorLevel = error;
    }
    /**
     * Get option for HTML Build Description.
     * @return "HTML" or "TEXT"
     */
    public String getOptionHtmlBuildDesc() {
        return optionHtmlBuildDesc;
    }
    /**
     * Set option for HTML build description.
     * @param desc HTML or TEXT
     */
    public void setOptionHtmlBuildDesc(final String desc) {
        this.optionHtmlBuildDesc = desc;
    }
    /**
     * Get option for execution report.
     * @return true/false
     */
    public boolean getOptionExecutionReport() {
        return optionExecutionReport;
    }
    /**
     * Set option for execution report.
     * @param report true/false
     */
    public void setOptionExecutionReport(final boolean report) {
        this.optionExecutionReport = report;
    }
    /**
     * Get option for cleaning workspace.
     * @return true/false
     */
    public boolean getOptionClean() {
        return optionClean;
    }
    /**
     * Set option for cleaning workspace.
     * @param clean true/false
     */
    public void setOptionClean(final boolean clean) {
        this.optionClean = clean;
    }
    /**
     * Get option to use CI licenses.
     * @return true to use CI licenses, false to not
     */
    public boolean getUseCILicenses() {
        return useCILicenses;
    }
    /**
     * Set option to use CI licenses.
     * @param ci  true to use CI licenses, false to not
     */
    public void setUseCILicenses(final boolean ci) {
        this.useCILicenses = ci;
    }
    /**
     * Get option to Use strict testcase import.
     * @return true to Use strict testcase import, false to not
     */
    public boolean getUseStrictTestcaseImport() {
        return useStrictTestcaseImport;
    }
    /**
     * Set option to Use strict testcase import.
     * @param strict  true to Use strict testcase import, false to not
     */
    public void setUseStrictTestcaseImport(final boolean strict) {
        this.useStrictTestcaseImport = strict;
    }

    /**
     * Get option to Use RGW3 capabilities.
     * @return true use RGW3 capabilities, false to not
     */
    public boolean getUseRGW3() {
        return useRGW3;
    }
    /**
     * Set option to use RGW3 capabilities.
     * @param rgw3 true to allow RGW3 test cases to run and export
     */
    public void setUseRGW3(final boolean rgw3) {
        this.useRGW3 = rgw3;
    }
    /**
     * Get option to use coverage plugin or vectorcast coverage plugin.
     * @return true use coverage plugin or vectorcast coverage plugin
     */
    public boolean getUseCoveragePlugin() {
        return useCoveragePlugin;
    }
    /**
     * Set option to use coverage plugin or vectorcast coverage plugin.
     * @param useCov use coverage plugin or vectorcast coverage plugin
     */
    public void setUseCoveragePlugin(final boolean useCov) {
        this.useCoveragePlugin = useCov;
    }
    /**
     * Get option to Use imported results.
     * @return true to Use imported results, false to not
     */
    public boolean getUseImportedResults() {
        return useImportedResults;
    }
    /**
     * Set option to Use imported results.
     * @param useImport true to Use imported results, false to not
     */
    public void setUseImportedResults(final boolean useImport) {
        this.useImportedResults = useImport;
    }

    /**
     * Get option to Use local imported results.
     * @return true to Use local imported results, false to not
     */
    public boolean getUseLocalImportedResults() {
        return useLocalImportedResults;
    }
    /**
     * Set option to Use imported results.
     * @param useLocal true to Use local imported results, false to not
     */
    public void setUseLocalImportedResults(final boolean useLocal) {
        this.useLocalImportedResults = useLocal;
    }

    /**
     * Get option to Use external imported results.
     * @return true to Use external imported results, false to not
     */
    public boolean getUseExternalImportedResults() {
        return useExternalImportedResults;
    }
    /**
     * Set option to Use imported results.
     * @param useExt true to Use external imported results, false to not
     */
    public void setUseExternalImportedResults(final boolean useExt) {
        this.useExternalImportedResults = useExt;
    }

      /**
     * Get option to Use as external result filename.
     * @return string external result filename
     */
    public String getExternalResultsFilename() {
        return externalResultsFilename;
    }
    /**
     * Set option to Use imported results.
     * @param extFname true to Use external imported results, false to not
     */
    public void setExternalResultsFilename(final String extFname) {
        this.externalResultsFilename = extFname;
    }

    /**
     * Get option to Use coverage history to control build status.
     * @return true to Use imported results, false to not
     */
    public boolean getUseCoverageHistory() {
        return useCoverageHistory;
    }
    /**
     * Set option to Use coverage history to control build status.
     * @param useCovHis true to Use imported results, false to not
     */
    public void setUseCoverageHistory(final boolean useCovHis) {
        this.useCoverageHistory = useCovHis;
    }
    /**
     * Get using SCM.
     * @return true/false
     */
    public boolean getUsingSCM() {
        return usingSCM;
    }
    /**
     * Set using SCM (true yes, false no).
     * @param useScm true/false
     */
    public void setUsingSCM(final boolean useScm) {
        this.usingSCM = useScm;
    }
    /**
     * Get the SCM to use.
     * @return SCM
     */
    public SCM getSCM() {
        return scm;
    }
    /**
     * Set the SCM being used.
     * @param inScm SCM
     */
    public void setSCM(final SCM inScm) {
        this.scm = inScm;
    }
    /**
     * Get the Manage project file/name.
     * @return Manage project name
     */
    public String getManageProjectName() {
        return manageProjectName;
    }
    /**
     * Set the Manage project file/name.
     * @param mpName Manage project name
     */
    public void setManageProjectName(final String mpName) {
        this.manageProjectName = mpName;
    }
    /**
     * Get the job name.
     * @return job name
     */
    public String getJobName() {
        return jobName;
    }
    /**
     * Set the job name.
     * @param name job name
     */
    public void setJobName(final String name) {
        this.jobName = name;
    }
    /**
     * Get the node label.
     * @return node label
     */
    public String getNodeLabel() {
        return nodeLabel;
    }
    /**
     * Set the node label.
     * @param label node label
     */
    public void setNodeLabel(final String label) {
        this.nodeLabel = label;
    }
    /**
     * Get pc-lint plus command.
     * @return pc-lint plus command
     */
    public String getPclpCommand() {
        return pclpCommand;
    }
    /**
     * Get pc-lint plus command.
     * @param pclpCmd - Pc Lint Plus Command
     */
    public void setPclpCommand(final String pclpCmd) {
        this.pclpCommand = pclpCmd;
    }
    /**
     * Get using pc-lint plus command.
     * @return true/false if we have a PC Lint Command
     */
    public boolean getUsingPCLP() {
        return pclpCommand.length() != 0;
    }
    /**
     * Get pc-lint plus result pattern.
     * @return pc-lint plus result pattern
     */
    public String getPclpResultsPattern() {
        return pclpResultsPattern;
    }
    /**
     * Get pc-lint plus result pattern.
     * @param pclcResult - PC Lint Result pattern
     */
    public void setPclpResultsPattern(final String pclcResult) {
        this.pclpResultsPattern = pclcResult;
    }

    /**
     * Get using getUsingPCLP command.
     * @return true/false if we have a squoreCommand
     */
    public boolean getUsingSquoreCommand() {
        return squoreCommand.length() != 0;
    }

    /**
     * Get Squore command.
     * @return Squore command
     */
    public String getSquoreCommand() {
        return squoreCommand;
    }

    /**
     * Set Squore command.
     * @param cmd - Squore Command
     */
    public void setSquoreCommand(final String cmd) {
        this.squoreCommand = cmd;
    }

    /**
     * Get URL for TESTinsights.
     * @return TESTinsights URL
     */
    public String getTestInsightsUrl() {
        return testInsightsUrl;
    }
    /**
     * Set URL for TESTinsights.
     * @param url - TESTinsights URL
     */
    public void setTestInsightsUrl(final String url) {
        this.testInsightsUrl = url;
    }
    /**
     * Get Project for TESTinsights.
     * @return TESTinsights Project
     */
    public String gettestInsightsProject() {
        return testInsightsProject;
    }
    /**
     * Set Project for TESTinsights.
     * @param  tiProj - Project for TESTinsights
     */
    public void settestInsightsProject(final String tiProj) {
        this.testInsightsProject = tiProj;
    }
    /**
     * Get Proxy for TESTinsights.
     * @return TESTinsights proxy
     */
    public String gettestInsightsProxy() {
        return testInsightsProxy;
    }
    /**
     * Set Proxy for TESTinsights.
     * @param tiProxy TESTinsights proxy
     */
    public void settestInsightsProxy(final String tiProxy) {
        this.testInsightsProxy = tiProxy;
    }
    /**
     * Get Credentials ID for TESTinsights.
     * @return TESTinsights Credentials
     */
    public String getTestInsightsCredentialsId() {
        return testInsightsCredentialsId;
    }
    /**
     * Set Credentials ID for TESTinsights.
     * @param tiCred - Credentials ID for TESTinsights
     */
    public void settestInsightsCredentialsId(final String tiCred) {
        this.testInsightsCredentialsId = tiCred;
    }
    /**
     * Get SCM URL for TESTinsights.
     * @return TESTinsights SCM URL
     */
    public String gettestInsightsScmUrl() {
        return testInsightsScmUrl;
    }
    /**
     * Get SCM Technology TESTinsights.
     * @return String TESTinsights SCM Technology
     */
    public String getTestInsightsScmTech() {
        return testInsightsScmTech;
    }
    /**
     * Set SCM URL for TESTinsights.
     * @param tiScmUrl - URL for TESTinsights
     */
    public void settestInsightsScmUrl(final String tiScmUrl) {
        this.testInsightsScmUrl = tiScmUrl;
    }
    /**
     * Set SCM Technology TESTinsights.
     * @param tech - SCM Technology TESTinsights (git or svn)
     */

    public void setTestInsightsScmTech(final String tech) {
        this.testInsightsScmTech = tech;
    }
    /**
     * Create setup step.
     * @param environmentSetupWinLocal environment setup for windows
     * @param environmentSetupUnixLocal environment setup for unix
     * @param executePreambleWinLocal execute preamble for windows
     * @param executePreambleUnixLocal execute preamble for unix
     * @param environmentTeardownWinLocal environment teardown for windows
     * @param environmentTeardownUnixLocal environment teardown for unix
     * @param optionUseReportingLocal use reporting
     * @param optionErrorLevelLocal error level
     * @param optionHtmlBuildDescLocal HTML Build description
     * @param optionExecutionReportLocal execution report
     * @param optionCleanLocal clean
     * @param useCILicensesLocal use CI licenses
     * @param useStrictTestcaseImportLocal Use strict testcase import
     * @param useRGW3Local Use RGW3 capabilities
     * @param useImportedResultsLocal use imported results
     * @param useLocalImportedResultsLocal use local imported results
     * @param useExternalImportedResultsLocal use extern imported results
     * @param externalResultsFilenameLocal use extern result filename
     * @param useCoverageHistoryLocal use imported results
     * @param waitLoopsLocal wait loops
     * @param waitTimeLocal wait time
     * @param maxParallelLocal maximum number of jobs to queue in parallel
     * @param manageProjectNameLocal manage project name
     * @param jobNameLocal job name
     * @param nodeLabelLocal node label
     * @param pclpCommandLocal PC Lint Plus command
     * @param pclpResultsPatternLocal PC Lint Plus result patter
     * @param squoreCommandLocal Squore command
     * @param testInsightsUrlLocal URL for TESTinsights
     * @param testInsightsProjectLocal Project for for TESTinsights
     * @param testInsightsCredentialsIdLocal Credentials for for TESTinsights
     * @param testInsightsProxyLocal Proxy for for TESTinsights
     * @param testInsightsScmUrlLocal SCM URL for for TESTinsights
     * @param testInsightsScmTechLocal SCM technology for for TESTinsights
     */
    @DataBoundConstructor
    @SuppressWarnings("checkstyle:ParameterNumber")
    public VectorCASTSetup(final String environmentSetupWinLocal,
                           final String environmentSetupUnixLocal,
                           final String executePreambleWinLocal,
                           final String executePreambleUnixLocal,
                           final String environmentTeardownWinLocal,
                           final String environmentTeardownUnixLocal,
                           final boolean optionUseReportingLocal,
                           final String optionErrorLevelLocal,
                           final String optionHtmlBuildDescLocal,
                           final boolean optionExecutionReportLocal,
                           final boolean optionCleanLocal,
                           final boolean useCILicensesLocal,
                           final boolean useStrictTestcaseImportLocal,
                           final boolean useRGW3Local,
                           final boolean useImportedResultsLocal,
                           final boolean useLocalImportedResultsLocal,
                           final boolean useExternalImportedResultsLocal,
                           final String  externalResultsFilenameLocal,
                           final boolean useCoverageHistoryLocal,
                           final Long waitLoopsLocal,
                           final Long waitTimeLocal,
                           final Long maxParallelLocal,
                           final String manageProjectNameLocal,
                           final String jobNameLocal,
                           final String nodeLabelLocal,
                           final String pclpCommandLocal,
                           final String pclpResultsPatternLocal,
                           final String squoreCommandLocal,
                           final String testInsightsUrlLocal,
                           final String testInsightsProjectLocal,
                           final String testInsightsCredentialsIdLocal,
                           final String testInsightsProxyLocal,
                           final String testInsightsScmUrlLocal,
                           final String testInsightsScmTechLocal) {
        this.environmentSetupWin = environmentSetupWinLocal;
        this.environmentSetupUnix = environmentSetupUnixLocal;
        this.executePreambleWin = executePreambleWinLocal;
        this.executePreambleUnix = executePreambleUnixLocal;
        this.environmentTeardownWin = environmentTeardownWinLocal;
        this.environmentTeardownUnix = environmentTeardownUnixLocal;
        this.optionUseReporting = optionUseReportingLocal;
        this.optionErrorLevel = optionErrorLevelLocal;
        this.optionHtmlBuildDesc = optionHtmlBuildDescLocal;
        this.optionExecutionReport = optionExecutionReportLocal;
        this.optionClean = optionCleanLocal;
        this.useCILicenses = useCILicensesLocal;
        this.useStrictTestcaseImport = useStrictTestcaseImportLocal;
        this.useRGW3 = useRGW3Local;
        this.useImportedResults = useImportedResultsLocal;
        this.useLocalImportedResults = useLocalImportedResultsLocal;
        this.useExternalImportedResults = useExternalImportedResultsLocal;
        this.externalResultsFilename = externalResultsFilenameLocal;
        this.useCoverageHistory = useCoverageHistoryLocal;
        this.usingSCM = false;
        this.scm = new NullSCM();
        this.waitLoops = waitLoopsLocal;
        this.waitTime = waitTimeLocal;
        this.maxParallel = maxParallelLocal;
        this.manageProjectName = manageProjectNameLocal;
        this.jobName = jobNameLocal;
        this.nodeLabel = nodeLabelLocal;
        this.pclpCommand = pclpCommandLocal;
        this.pclpResultsPattern = pclpResultsPatternLocal;
        this.squoreCommand = squoreCommandLocal;
        this.testInsightsUrl = testInsightsUrlLocal;
        this.testInsightsProject = testInsightsProjectLocal;
        this.testInsightsCredentialsId = testInsightsCredentialsIdLocal;
        this.testInsightsProxy = testInsightsProxyLocal;
        this.testInsightsScmUrl = testInsightsScmUrlLocal;
        this.testInsightsScmTech = testInsightsScmTechLocal;
    }
    /**
     * Copy the files in a directory recursively to the job workspace.
     * This is used when the source is NOT a jar file.
     *
     * @param scriptDir directory to process
     * @param base base
     * @param destDir destination directory
     * @param directDir direct dir to copy
     * @throws IOException exception
     * @throws InterruptedException exception
     */
    private void processDir(final File scriptDir,
            final String base,
            final FilePath destDir,
            final Boolean directDir) throws IOException, InterruptedException {
        destDir.mkdirs();
        File[] files = scriptDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {

            if (file.isDirectory()) {
                FilePath newDest = new FilePath(destDir, file.getName());
                processDir(file,
                    base + "/" + file.getName(),
                    newDest, directDir);
            } else {
                if (directDir) {
                    // change the copy mechanism
                    // to copy file to remote node
                    File newFile = new File(destDir
                        + File.separator
                        + file.getName());
                    File inFile = new File(scriptDir
                        + File.separator
                        + file.getName());

                    FilePath dest = new FilePath(destDir, newFile.getName());
                    InputStream is = null;
                    try {
                        is = new FileInputStream(inFile);
                        dest.copyFrom(is);
                    } finally {
                        if (is != null) {
                            is.close();
                        }
                    }
                } else {
                    FilePath newFile = new FilePath(destDir, file.getName());
                    try (InputStream is = VectorCASTSetup.class.
                            getResourceAsStream(SCRIPT_DIR
                                + base
                                + "/"
                                + file.getName())) {
                        newFile.copyFrom(is);
                    }
                }
            }
        }
    }

    private void printVersion(final PrintStream logger) {
        logger.println("[VectorCAST Execution Version]: "
            + VcastUtils.getVersion().
            orElse("Error - Could not determine version"));
    }

    /**
     * Perform the build step. Copy the scripts from the
     * archive/directory to the workspace.
     * @param build build
     * @param workspace workspace
     * @param env environment variables
     * @param launcher launcher
     * @param listener  listener
     * @throws IOException exception
     */
    @Override
    public void perform(final Run<?, ?> build, final FilePath workspace,
            final EnvVars env, final Launcher launcher,
            final TaskListener listener) throws IOException {

        final int initPathLen = 8;
        FilePath destScriptDir = new FilePath(workspace, "vc_scripts");
        JarFile jFile = null;
        try {
            String path = null;
            String overridePath = System.getenv("VCAST_VC_SCRIPTS");
            String extraScriptPath = SCRIPT_DIR;
            Boolean directDir = false;
            if (overridePath != null && !overridePath.isEmpty()) {
                path = overridePath;
                extraScriptPath = "";
                directDir = true;
                String msg = " "
                    + "VectorCAST - overriding vc_scripts. Copying from '"
                    + path + "'";
                Logger.
                    getLogger(
                        VectorCASTSetup.class.getName()).log(Level.ALL, msg);
            } else {
                path = VectorCASTSetup.class.getProtectionDomain().
                    getCodeSource().getLocation().getPath();
                path = URLDecoder.decode(path, "utf-8");
            }
            File testPath = new File(path);
            printVersion(listener.getLogger());
            if (testPath.isFile()) {
                // Have jar file...
                jFile = new JarFile(testPath);
                Enumeration<JarEntry> entries = jFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("scripts")) {
                        String fileOrDir =
                            entry.getName().substring(initPathLen);
                        FilePath dest = new FilePath(destScriptDir, fileOrDir);
                        if (entry.getName().endsWith("/")) {
                            // Directory, create destination
                            dest.mkdirs();
                        } else {
                            // File, copy it
                            InputStream is = VectorCASTSetup.class.
                                getResourceAsStream("/" + entry.getName());
                            dest.copyFrom(is);
                        }
                    }
                }
            } else {
                // Have directory
                File scriptDir = new File(path + extraScriptPath);
                processDir(scriptDir, "./", destScriptDir, directDir);
            }
        } catch (IOException ex) {
            Logger.getLogger(VectorCASTSetup.class.getName()).
                log(Level.INFO, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(VectorCASTSetup.class.getName()).
                log(Level.INFO, null, ex);
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
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.delete()) {
                    throw new IOException("Unable to delete file: "
                        + file.getAbsolutePath());
                }
            }
        }
    }
    /**
     * getDescriptor for {@link VectorCASTSetup}.
     * @return DescriptorImpl descriptor
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    /**
     * Descriptor for {@link VectorCASTSetup}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }
        /**
         * Override call to isApplicable.
         * @param aClass AbstractProject Project Class
         * @return boolean always true
         * call load() in the constructor.
         */
        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(
            final Class<? extends AbstractProject> aClass) {
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

    /**
    * Convert current setup to a string to print.
    * @return the display name
    */
   @Override
   public String toString() {
        String string = "\nVectorCASTSetup: \n"
                + "\t environmentSetupUnix: " + environmentSetupUnix + "\n"
                + "\t executePreambleWin: " + executePreambleWin + "\n"
                + "\t executePreambleUnix: " + executePreambleUnix + "\n"
                + "\t environmentTeardownWin: " + environmentTeardownWin + "\n"
                + "\t environmentTeardownUnix: " + environmentTeardownUnix
                + "\n"
                + "\t optionUseReporting: " + optionUseReporting + "\n"
                + "\t optionErrorLevel: " + optionErrorLevel + "\n"
                + "\t optionHtmlBuildDesc: " + optionHtmlBuildDesc + "\n"
                + "\t optionHtmlBuildDesc: " + optionHtmlBuildDesc + "\n"
                + "\t optionExecutionReport: " + optionExecutionReport + "\n"
                + "\t optionClean: " + optionClean + "\n"
                + "\t useCILicenses: " + useCILicenses + "\n"
                + "\t useStrictTestcaseImport: " + useStrictTestcaseImport
                + "\n"
                + "\t useRGW3: " + useRGW3 + "\n"
                + "\t useImportedResults: " + useImportedResults + "\n"
                + "\t useLocalImportedResults: " + useLocalImportedResults
                + "\n"
                + "\t useExternalImportedResults: " + useExternalImportedResults
                + "\n"
                + "\t externalResultsFilename: " + externalResultsFilename
                + "\n"
                + "\t useCoverageHistory: " + useCoverageHistory + "\n"
                + "\t usingSCM: " + usingSCM + "\n"
                + "\t scm: " + scm + "\n"
                + "\t waitLoops: " + waitLoops + "\n"
                + "\t waitTime: " + waitTime + "\n"
                + "\t maxParallel: " + maxParallel + "\n"
                + "\t manageProjectName: " + manageProjectName + "\n"
                + "\t jobName: " + jobName + "\n"
                + "\t nodeLabel: " + nodeLabel + "\n"
                + "\t pclpCommand: " + pclpCommand + "\n"
                + "\t pclpResultsPattern: " + pclpResultsPattern + "\n"
                + "\t squoreCommand: " + squoreCommand + "\n"
                + "\t testInsightsUrl: " + testInsightsUrl + "\n"
                + "\t testInsightsProject: " + testInsightsProject + "\n"
                + "\t testInsightsCredentialsId: " + testInsightsCredentialsId
                + "\n"
                + "\t testInsightsProxy: " + testInsightsProxy + "\n"
                + "\t testInsightsScmUrl: " + testInsightsScmUrl + "\n"
                + "\t testInsightsScmTech: " + testInsightsScmTech + "\n";
        return string;
    }
}
