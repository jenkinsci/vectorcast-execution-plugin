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
import io.jenkins.plugins.coverage.metrics.steps.CoverageQualityGate;
import io.jenkins.plugins.coverage.metrics.model.Baseline;
import io.jenkins.plugins.util.QualityGate.QualityGateCriticality;
import edu.hm.hafner.coverage.Metric;
import io.jenkins.plugins.forensics.reference.SimpleReferenceRecorder;
import org.jenkinsci.plugins.credentialsbinding.impl.SecretBuildWrapper;
import org.jenkinsci.plugins.credentialsbinding.impl.UsernamePasswordMultiBinding;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import java.net.URL;


/**
 * Base job management - create/delete/update.
 */
public abstract class BaseJob {
    /** Coverage Delta threshold. */
    private static final float COVERAGE_THRESHOLD = -0.001f;

    /** VectorCAST Coverage plugin selection. */
    private static final long USE_VCC_PLUGIN = 2;

    /** Zero percent indicator. */
    private static final int ZERO_PERCENT = 0;
    /** Seventy percent indicator. */
    private static  final int SEVENTY_PERCENT = 70;
    /** Eighty percent indicator. */
    private static  final int EIGHTY_PERCENT = 80;
    /** 100% indicator. */
    private static  final int ONE_HUNDREAD_PERCENT = 100;
    /** Maximum string length. */
    private static  final int MAX_STRING_LEN = 1000;
    /** Default wait time. */
    private static  final int DEFAULT_WAIT_TIME = 30;
    /** Default number of wait loops. */
    private static  final int DEFAULT_WAIT_LOOP = 1;

    /** Project name. */
    private String projectName;
    /** Jenkins instance. */
    private Jenkins instance;
    /** Request. */
    private StaplerRequest request;
    /** Response. */
    private StaplerResponse response;
    /** Manage project name. */
    private String manageProjectName;
    /** Base name generated from the manage project name. */
    private String baseName;
    /** Top-level project. */
    private Project<?, ?> topProject;
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
    private int optionErrorLevel;
    /** Use HTML in build description. */
    private String optionHtmlBuildDesc;
    /** Generate execution report. */
    private boolean optionExecutionReport;
    /** Clean workspace. */
    private boolean optionClean;
    /** Use CI license. */
    private boolean useCILicenses;

    /** Use strict testcase import. */
    private boolean useStrictTestcaseImport;

    /** Allow RGW3 test to be executed and exported. */
    private boolean useRGW3;

    /** Use coveagePlugin. */
    private boolean useCoveragePlugin;

    /** Use imported results. */
    private boolean useImportedResults = false;
    /** Use local/artifact imported results. */
    private boolean useLocalImportedResults = false;
    /** Use external file for imported results. */
    private boolean useExternalImportedResults = false;
    /** Filename for external results. */
    private String  externalResultsFilename;

    /** Use coverage history to control build status. */
    private boolean useCoverageHistory;

    /** Using some form of SCM. */
    private boolean usingScm;
    /** The SCM being used. */
    private SCM scm;
    /** Wait time. */
    private Long waitTime;
    /** Wait loops. */
    private Long waitLoops;
    /** Base Job name. */
    private String jobName;
    /** Node label. */
    private String nodeLabel;
    /** Maximum number of parallal jobs to queue up. */
    private Long maxParallel;

    /** PC Lint Plus Command. */
    private String pclpCommand;
    /** PC Lint Plus Path. */
    private String pclpResultsPattern;
    /** Squore execution command. */
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
     * Constructor.
     * @param req request object
     * @param resp response object
     * @throws ServletException exception
     * @throws IOException exception
     * @throws ExternalResultsFileException exception
     * @throws IllegalArgumentException exception
     * @throws BadOptionComboException exception
     */
    protected BaseJob(final StaplerRequest req,
            final StaplerResponse resp)
            throws ServletException, IOException,
            ExternalResultsFileException, IllegalArgumentException,
            BadOptionComboException {

        instance = Jenkins.get();
        request = req;
        response = resp;
        JSONObject json = request.getSubmittedForm();

        manageProjectName = json.optString("manageProjectName");
        if (manageProjectName.length() > MAX_STRING_LEN) {
            throw new IllegalArgumentException(
                "manageProjectName too long > 1000"
            );
        }

        if (!manageProjectName.isEmpty()) {
            // Force unix style path to avoid problems later
            manageProjectName = manageProjectName.replace('\\', '/');
            manageProjectName = manageProjectName.trim();
            if (!manageProjectName.toLowerCase().endsWith(".vcm")) {
                manageProjectName += ".vcm";
            }
        }
        baseName = FilenameUtils.getBaseName(manageProjectName);

        environmentSetupWin = json.optString("environmentSetupWin");
        executePreambleWin = json.optString("executePreambleWin");
        environmentTeardownWin = json.optString("environmentTeardownWin");

        environmentSetupUnix = json.optString("environmentSetupUnix");
        executePreambleUnix = json.optString("executePreambleUnix");
        environmentTeardownUnix = json.optString("environmentTeardownUnix");

        optionUseReporting = json.optBoolean("optionUseReporting", true);
        String errLevel = json.optString("optionErrorLevel", "unstable");
        if (errLevel.equals("nothing")) {
            optionErrorLevel = 0;
        } else if (errLevel.equals("unstable")) {
            optionErrorLevel = 1;
        } else if (errLevel.equals("failure")) {
            optionErrorLevel = 2;
        }

        optionHtmlBuildDesc = json.optString("optionHtmlBuildDesc", "HTML");
        optionExecutionReport = json.optBoolean("optionExecutionReport", true);
        optionClean = json.optBoolean("optionClean", false);

        waitTime = json.optLong("waitTime", DEFAULT_WAIT_TIME);
        waitLoops = json.optLong("waitLoops", DEFAULT_WAIT_LOOP);

        jobName = json.optString("jobName", null);

        if (jobName != null) {
            // Remove all non-alphanumeric characters from the Jenkins Job name
            jobName = jobName.replaceAll("[^a-zA-Z0-9_]", "_");
        }

        nodeLabel = json.optString("nodeLabel", "");

        useCILicenses  = json.optBoolean("useCiLicense", false);
        useStrictTestcaseImport  = json
            .optBoolean("useStrictTestcaseImport", true);
        useRGW3  = json.optBoolean("useRGW3", false);
        useImportedResults  = json.optBoolean("useImportedResults", false);


        /* since Coverage is a radio button, we need to unpack it */
        JSONObject jsonCovPlugin = json.optJSONObject("coverageDisplayOption");

        /* If there's something specified, check which one to use */
        if (jsonCovPlugin != null) {
            final long whichPlugin =
                jsonCovPlugin.optLong("value", USE_VCC_PLUGIN);
            if (whichPlugin == USE_VCC_PLUGIN) {
                useCoveragePlugin = false;
            } else {
                useCoveragePlugin = true;
            }
        } else {
            /* If there's nothing specified, use VCC */
            useCoveragePlugin = false;
        }

        externalResultsFilename = "";

        if (useImportedResults) {
            JSONObject jsonImpRes = json.optJSONObject("importedResults");

            if (jsonImpRes != null) {
                final long intExt = jsonImpRes.optLong("value", 0);

                if (intExt == 1) {
                    useLocalImportedResults = true;
                    useExternalImportedResults = false;
                    externalResultsFilename = "";
                } else if (intExt == 2) {
                    useLocalImportedResults = false;
                    useExternalImportedResults = true;
                    externalResultsFilename = jsonImpRes
                        .optString("externalResultsFilename", "");
                    externalResultsFilename =
                        externalResultsFilename.replace('\\', '/');
                    if (externalResultsFilename.length() == 0) {
                        throw new ExternalResultsFileException();
                    }
                }
            }
        }
        useCoverageHistory = json.optBoolean("useCoverageHistory", false);
        maxParallel = json.optLong("maxParallel", 0);

        /* Additional Tools */
        pclpCommand = json.optString("pclpCommand", "").replace('\\', '/');
        pclpResultsPattern = json.optString("pclpResultsPattern", "");
        squoreCommand = json.optString("squoreCommand", "").replace('\\', '/');
        testInsightsUrl = json.optString("TESTinsights_URL", "");
        testInsightsProject = json.optString("TESTinsights_project", "");
        if (testInsightsProject.length() == 0) {
                testInsightsProject = "env.JOB_BASE_NAME";
        }
        testInsightsCredentialsId =
            json.optString("TESTinsights_credentials_id", "");
        testInsightsProxy = json.optString("TESTinsights_proxy", "");

    }

    /**
     * Using some form of SCM.
     * @return true or false
     */
    protected boolean isUsingScm() {
        return usingScm;
    }
    /**
     * Set using some form of SCM.
     * @param useScm true/false
     */
    protected void setUsingSCM(final boolean useScm) {
        this.usingScm = useScm;
    }
    /**
     * Get environment setup for windows.
     * @return setup
     */
    protected String getEnvironmentSetupWin() {
        return environmentSetupWin;
    }

    /**
     * Get execute preamble for windows.
     * @return preamble
     */
    protected String getExecutePreambleWin() {
        return executePreambleWin;
    }
    /**
     * Get environment tear down for windows.
     * @return environment tear down for windows
     */
    protected String getEnvironmentTeardownWin() {
        return environmentTeardownWin;
    }
    /**
     * Get environment setup for unix.
     * @return environment setup
     */
    protected String getEnvironmentSetupUnix() {
        return environmentSetupUnix;
    }
    /**
     * Get execute preamble for unix.
     * @return preamble
     */
    protected String getExecutePreambleUnix() {
        return executePreambleUnix;
    }
    /**
     * Get environment tear down for unix.
     * @return teardown
     */
    protected String getEnvironmentTeardownUnix() {
        return environmentTeardownUnix;
    }
    /**
     * Get use Jenkins reporting option.
     * @return true to use, false to not
     */
    protected boolean getOptionUseReporting() {
        return optionUseReporting;
    }
    /**
     * Get error level.
     * @return int 0 - Do nothing, 1 - Unstable, 2 - Error
     */
    protected int getOptionErrorLevel() {
        return optionErrorLevel;
    }
    /**
     * Use HTML Build Description.
     * @return HTML or TEXT
     */
    protected String getOptionHTMLBuildDesc() {
        return optionHtmlBuildDesc;
    }
    /**
     * Use execution report.
     * @return true to use, false to not
     */
    protected boolean getOptionExecutionReport() {
        return optionExecutionReport;
    }
    /**
     * Get option to clean workspace before build.
     * @return true to clean, false to not
     */
    protected boolean getOptionClean() {
        return optionClean;
    }
    /**
     * Get option to use CI licenses.
     * @return true to use CI licenses, false to not
     */
    protected boolean getUseCILicenses() {
        return useCILicenses;
    }
    /**
     * Get option to Use strict testcase import.
     * @return true to Use strict testcase import, false to not
     */
    protected boolean getUseStrictTestcaseImport() {
        return useStrictTestcaseImport;
    }
    /**
     * Get option to Use RGW3 capabilities.
     * @return true use RGW3 capabilities, false to not
     */
    protected boolean getUseRGW3() {
        return useRGW3;
    }
    /**
     * Get option to use coverage plugin or vectorcast coverage plugin.
     * @return true use coverage plugin or vectorcast coverage plugin
     */
    protected boolean getUseCoveragePlugin() {
        return useCoveragePlugin;
    }
    /**
     * Get option to Use imported results.
     * @return true to Use imported results, false to not
     */
    protected boolean getUseImportedResults() {
        return useImportedResults;
    }

    /**
     * Get option to Use local imported results.
     * @return true to Use local imported results, false to not
     */
    protected boolean getUseLocalImportedResults() {
        return useLocalImportedResults;
    }
    /**
     * Get option to Use external imported results.
     * @return true to Use external imported results, false to not
     */
    protected boolean getUseExternalImportedResults() {
        return useExternalImportedResults;
    }

    /**
     * Get option to Use as external result filename.
     * @return string external result filename
     */
    protected String getExternalResultsFilename() {
        return externalResultsFilename;
    }
    /**
     * Get option to Use coverage history to control build status.
     * @return true to Use imported results, false to not
     */
    public boolean getUseCoverageHistory() {
        return useCoverageHistory;
    }
    /**
     * Get for maxParallel to control maximum number of
     * jobs to be queue at at any one point.
     * @return MaxParallel integer number
     */
    protected Long getMaxParallel() {
        return maxParallel;
    }
     /**
     * Get use CI license for Linux.
     * @return String command to set
     */
    protected String getUseCILicensesWin() {
        String ciEnvVars = "";

        if (useCILicenses) {
            ciEnvVars = "set VCAST_USING_HEADLESS_MODE=1\n"
                + "set VCAST_USE_CI_LICENSES=1\n";
        }
        return ciEnvVars;
    }
     /**
     * Get use CI license for Linux.
     * @return String command to set
     */
    protected String getUseCILicensesUnix() {
        String ciEnvVars = "";

        if (useCILicenses) {
            ciEnvVars = "export VCAST_USING_HEADLESS_MODE=1\n"
                + "export VCAST_USE_CI_LICENSES=1\n";
        }
        return ciEnvVars;
    }
   /**
     * Get the time to wait between retries.
     * @return number of seconds
     */
    protected Long getWaitTime() {
        return waitTime;
    }
    /**
     * Get the number of wait loops.
     * @return number of iterations
     */
    protected Long getWaitLoops() {
        return waitLoops;
    }
    /**
     * Get the user-specified job name.
     * @return job name (null for use default)
     */
    protected String getJobName() {
        return jobName;
    }
    /**
     * Get top-level project.
     * @return project
     */
    protected Project<?, ?> getTopProject() {
        return topProject;
    }
    /**
     * Get manage project name.
     * @return manage project name
     */
    protected String getManageProjectName() {
        return manageProjectName;
    }
    /**
     * Get base name of manage project.
     * @return base name
     */
    protected String getBaseName() {
        return baseName;
    }
    /**
     * Get node label.
     * @return node label
     */
    protected String getNodeLabel() {
        return nodeLabel;
    }
    /**
     * Get pc-lint plus command.
     * @return pc-lint plus command
     */
    protected String getPclpCommand() {
        return pclpCommand;
    }
    /**
     * Get pc-lint plus result pattern.
     * @return pc-lint plus result pattern
     */
    protected String getPclpResultsPattern() {
        return pclpResultsPattern;
    }

    /**
     * Get command for running Squore.
     * @return Squore command
     */
    protected String getSquoreCommand() {
        return squoreCommand;
    }
    /**
     * Get URL for TESTinsights.
     * @return TESTinsights URL
     */
    protected String getTestInsightsUrl() {
        return testInsightsUrl;
    }
    /**
     * Get Project for TESTinsights.
     * @return TESTinsights Project
     */
    protected String getTestInsightsProject() {
        return testInsightsProject;
    }
    /**
     * set Project for TESTinsights.
     * @param project TESTinsights Project
     */
    protected void setTestInsightsProject(final String project) {
        this.testInsightsProject = project;
    }
    /**
     * Get Proxy for TESTinsights.
     * @return proxy TESTinsights proxy
     */
    protected String getTestInsightsProxy() {
        return testInsightsProxy;
    }
    /**
     * Get Credentials for TESTinsights.
     * @return TESTinsights Credentials
     */
    protected String getTestInsightsCredentialsId() {
        return testInsightsCredentialsId;
    }
    /**
     * Set SCM URL for TESTinsights.
     * @param url TESTinsights SCM URL
     */
    protected void setTestInsightsScmUrl(final String url) {
        this.testInsightsScmUrl = url;
    }
    /**
     * Get SCM URL for TESTinsights.
     * @return TESTinsights SCM URL
     */
    protected String getTestInsightsScmUrl() {
        return testInsightsScmUrl;
    }
    /**
     * Get SCM Technology TESTinsights.
     * @return TESTinsights SCM Technology
     */
    protected String getTestInsightsScmTech() {
        return testInsightsScmTech;
    }
    /**
     * Set SCM Technology TESTinsights.
     * @param tech TESTinsights SCM Technology
     */
    protected void setTestInsightsScmTech(final String tech) {
        this.testInsightsScmTech = tech;
    }
    /**
     * Get request.
     * @return request
     */
    protected StaplerRequest getRequest() {
        return request;
    }
    /**
     * Get Jenkins instance.
     * @return Jenkins instance
     */
    protected Jenkins getInstance() {
        return instance;
    }
    /**
     * Get the name of the project.
     * @return the project name
     */
    public String getProjectName() {
        return projectName;
    }
    /**
     * Sets the name of the project.
     * @param pName - project name
     */
    public void setProjectName(final String pName) {
        this.projectName = pName;
    }
    /**
     * Get response.
     * @return response
     */
    protected StaplerResponse getResponse() {
        return response;
    }
    /**
     * Add the delete workspace before build starts option.
     * @param project project to add to
     */
    protected void addDelWSBeforeBuild(final Project<?, ?> project) {
        if (optionClean) {
            PreBuildCleanup cleanup = new PreBuildCleanup(
                null,    /*patterns*/
                true,
                "",      /*cleanup param*/
                "",      /*external delete*/
                false /*disableDeferredWipeout*/
            );
            project.getBuildWrappersList().add(cleanup);
        }
    }
    /**
     * Create the job(s).
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws JobAlreadyExistsException exception
     * @throws InvalidProjectFileException exception
     */
    public void create()
            throws IOException, ServletException,
            Descriptor.FormException,
            JobAlreadyExistsException,
            InvalidProjectFileException {
        // Create the top-level project
        topProject = createProject();
        if (topProject == null) {
            return;
        }

        // Read the SCM setup
        scm = SCMS.parseSCM(request, topProject);
        if (scm == null) {
            scm = new NullSCM();
        }
        if (scm instanceof NullSCM) {
            usingScm = false;
        } else {
            usingScm = true;

            // for TESTinsights SCM connector
            String scmName = scm.getDescriptor().getDisplayName();
            if (scmName.equals("Git")) {
                testInsightsScmTech = "git";
            } else if (scmName.equals("Subversion")) {
                testInsightsScmTech = "svn";
            } else {
                testInsightsScmTech = "";
            }
        }
        topProject.setScm(scm);

        addDelWSBeforeBuild(topProject);

        try {
            doCreate();
        } catch (InvalidProjectFileException ex) {
            cleanupProject();
            throw ex;
        }
    }
    /**
     * Create top-level project.
     * @return created project
     * @throws IOException exception
     * @throws JobAlreadyExistsException exception
     */
    protected abstract Project<?, ?> createProject()
            throws IOException, JobAlreadyExistsException;
    /**
     * Cleanup top-level project, as in delete.
     */
    protected abstract void cleanupProject();
    /**
     * Do create of project details.
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws InvalidProjectFileException exception
     */
    protected abstract void doCreate()
        throws IOException,
        ServletException,
        Descriptor.FormException,
        InvalidProjectFileException;

    /**
     * Add the VectorCAST setup step to copy the python scripts to.
     * the workspace
     * @param project project
     * @return the setup build step
     */
    protected VectorCASTSetup addSetup(final Project<?, ?> project)
            throws IOException {
        VectorCASTSetup setup = new VectorCASTSetup();

        project.getBuildersList().add(setup);

        return setup;
    }
    /**
     * Add archive artifacts step.
     * @param project project to add to
     */
    protected void addArchiveArtifacts(final Project<?, ?> project) {
        String pclpArchive = "";
        String tiArchive = "";

        if (pclpCommand.length() != 0) {
            pclpArchive = ", " + pclpResultsPattern;
        }
        if (testInsightsUrl.length() != 0) {
            tiArchive = ", TESTinsights_Push.log";
        }
        String addToolsArchive = pclpArchive + tiArchive;
        String defaultArchive = "**/*.html, xml_data/**/*.xml,"
                + "unit_test_*.txt, **/*.png, **/*.css,"
                + "complete_build.log, *_results.vcr";

        ArtifactArchiver archiver =
                new ArtifactArchiver(defaultArchive + addToolsArchive);
        archiver.setExcludes("");
        archiver.setAllowEmptyArchive(false);
        project.getPublishersList().add(archiver);
    }

    /**
     * Add archive artifacts step.
     * @param project project to add to
     */
    protected void addCopyResultsToImport(final Project<?, ?> project) {
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
     * Add JUnit rules step.
     * @param project project to add step to
     */

    protected void addJunit(final Project<?, ?> project) {
        JUnitResultArchiver junit =
            new JUnitResultArchiver("**/test_results_*.xml");
        project.getPublishersList().add(junit);
    }
    /**
     * Add PC-Lint Plus step.
     * @param project project to add step to do PC-Lint Plus
     */

    protected void addPCLintPlus(final Project<?, ?> project) {
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
     * Add VectorCAST coverage reporting step.
     * @param project project to add step to
     */
    protected void addVCCoverage(final Project<?, ?> project) {
        VectorCASTHealthReportThresholds healthReports =
                new VectorCASTHealthReportThresholds(
                    ZERO_PERCENT, ONE_HUNDREAD_PERCENT,
                    ZERO_PERCENT, SEVENTY_PERCENT,
                    ZERO_PERCENT, EIGHTY_PERCENT,
                    ZERO_PERCENT, EIGHTY_PERCENT,
                    ZERO_PERCENT, EIGHTY_PERCENT,
                    ZERO_PERCENT, EIGHTY_PERCENT);
        VectorCASTPublisher publisher = new VectorCASTPublisher();
        publisher.includes = "**/coverage_results_*.xml";
        publisher.healthReports = healthReports;
        publisher.setUseCoverageHistory(useCoverageHistory);
        project.getPublishersList().add(publisher);
    }
    /**
     * Add Jenkins coverage reporting step.
     * @param project project to add step to
     */
    protected void addReferenceBuild(final Project<?, ?> project) {
        SimpleReferenceRecorder refRec = new SimpleReferenceRecorder();

        project.getPublishersList().add(refRec);
    }

    /**
     * Add Jenkins coverage reporting step.
     * @param project project to add step to
     */
    protected void addJenkinsCoverage(final Project<?, ?> project) {

        List<CoverageQualityGate> qualityGates = null;

        CoverageTool tool = new CoverageTool();
        tool.setParser(Parser.VECTORCAST);
        tool.setPattern("xml_data/cobertura/coverage_results*.xml");

        if (getUseCoverageHistory()) {
            CoverageQualityGate statement =
                new CoverageQualityGate(Metric.LINE);
            statement.setBaseline(Baseline.PROJECT_DELTA);
            statement.setCriticality(QualityGateCriticality.ERROR);
            statement.setThreshold(COVERAGE_THRESHOLD);

            CoverageQualityGate branch = new CoverageQualityGate(Metric.BRANCH);
            branch.setBaseline(Baseline.PROJECT_DELTA);
            branch.setCriticality(QualityGateCriticality.ERROR);
            branch.setThreshold(COVERAGE_THRESHOLD);

            qualityGates = new ArrayList<CoverageQualityGate>();
            qualityGates.add(statement);
            qualityGates.add(branch);

        }

        //tool.setQualityGate();
        List<CoverageTool> list = new ArrayList<CoverageTool>();
        list.add(tool);

        CoverageRecorder publisher = new CoverageRecorder();
        publisher.setTools(list);

        if (qualityGates != null) {
            publisher.setQualityGates(qualityGates);
        }

        project.getPublishersList().add(publisher);
    }
    /**
     * Add credentials for coverage reporting step.
     * @param project project to add step to
     */
    protected void addCredentialID(final Project<?, ?> project) {
        project.getBuildWrappersList().add(
            new SecretBuildWrapper(Collections.<MultiBinding<?>>singletonList(
            new UsernamePasswordMultiBinding("VC_TI_USR", "VC_TI_PWS",
                testInsightsCredentialsId))));
    }

    /**
     * Call to get baseline windows single job file.
     * @return URL for baseline file
     */
    protected URL getBaselineWindowsSingleFile() {
        // GOOD: The call is always made on an object of the same type.
        return BaseJob.class.
            getResource("/scripts/baselineSingleJobWindows.txt");
    }
    /**
     * Call to get baseline linux single job file.
     * @return URL for baseline file
     */
    protected URL getBaselineLinuxSingleFile() {
        // GOOD: The call is always made on an object of the same type.
        return BaseJob.class.getResource("/scripts/baselineSingleJobLinux.txt");
    }
    /**
     * Call to get baseline post-build groovy job file.
     * @return URL for baseline file
     */
    protected URL getBaselinePostBuildGroovyScript() {
        // GOOD: The call is always made on an object of the same type.
        return BaseJob.class.getResource("/scripts/baselinePostBuild.groovy");
    }
    /**
     * Call to get baseline config.xml with parameters for pipeline job .
     * @return URL for baseline file
     */
    protected URL getPipelineConfigParametersXML() {
        // GOOD: The call is always made on an object of the same type.
        return BaseJob.class.getResource("/scripts/config_parameters.xml");
    }
    /**
     * Call to get baseline config.xml for pipeline job .
     * @return URL for baseline file
     */
    protected URL getPipelineConfigXML() {
        // GOOD: The call is always made on an object of the same type.
        return BaseJob.class.getResource("/scripts/config.xml");
    }
    /**
     * Call to get baseline groovy script for pipeline job.
     * @return URL for baseline file
     */
    protected URL getBaselinePipelineGroovy() {
        // GOOD: The call is always made on an object of the same type.
        return BaseJob.class.getResource("/scripts/baseJenkinsfile.groovy");
    }
}
