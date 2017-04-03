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

import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Label;
import hudson.model.Project;
import hudson.model.labels.LabelAtom;
import hudson.plugins.copyartifact.BuildSelector;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.copyartifact.WorkspaceSelector;
import hudson.scm.SCM;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Create a new multi-job and associated build/execute and reporting jobs
 */
public class NewMultiJob extends BaseJob {
    /** Multi-job project name */
    private String multiProjectName;
    /** Manage file */
    private String manageFile;
    /** Parsed Manage project */
    private ManageProject manageProject = null;
    /** projects actually added */
    private List<String> projectsAdded = null;
    /** All projects needed */
    private List<String> projectsNeeded = null;
    /** Any existing projects */
    private List<String> projectsExisting = null;
    /**
     * Constructor
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException 
     */
    public NewMultiJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        super(request, response);
    }
    /**
     * Get projects added
     * @return projects added
     */
    public List<String> getProjectsAdded() {
        return projectsAdded;
    }
    /**
     * Get projects needed
     * @return projects needed
     */
    public List<String> getProjectsNeeded() {
        return projectsNeeded;
    }
    /**
     * Get existing projects
     * @return existing projects
     */
    public List<String> getProjectsExisting() {
        return projectsExisting;
    }
    /**
     * Create project
     * @return new top-level project
     * @throws IOException
     * @throws JobAlreadyExistsException 
     */
    @Override
    protected Project createProject() throws IOException, JobAlreadyExistsException {
        multiProjectName = getBaseName() + ".vcast_manage.multijob";
        if (getInstance().getJobNames().contains(multiProjectName)) {
            throw new JobAlreadyExistsException(multiProjectName);
        }
        return getInstance().createProject(MultiJobProject.class, multiProjectName);
    }
    /**
     * Cleanup project
     */
    @Override
    protected void cleanupProject() {
        List<Item> jobs = getInstance().getAllItems();
        for (Item job : jobs) {
            if (job.getFullName().equals(multiProjectName)) {
                try {
                    job.delete();
                } catch (IOException ex) {
                    Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    /**
     * Create multi-job top-level and sub-projects, updating if required.
     * @param update true to do an update rather than create only
     * @throws IOException
     * @throws ServletException
     * @throws hudson.model.Descriptor.FormException 
     */
    @Override
    protected void doCreate(boolean update) throws IOException, ServletException, Descriptor.FormException, InvalidProjectFileException {
        // Read the manage project file
        FileItem fileItem = getRequest().getFileItem("manageProject");
        if (fileItem == null) {
            return;
        }

        manageFile = fileItem.getString();
        manageProject = new ManageProject(manageFile);
        manageProject.parse();

        getTopProject().setDescription("Top-level multi job to run the manage project: " + getManageProjectName());
        Label label = new LabelAtom("master");
        getTopProject().setAssignedLabel(label);
        
        // Build actions...
        addSetup(getTopProject());
        // Add multi-job phases
        // Building...
        List<PhaseJobsConfig> phaseJobs = new ArrayList<>();
        
        projectsAdded = new ArrayList<>();
        projectsNeeded = new ArrayList<>();
        projectsExisting = new ArrayList<>();
        
        projectsAdded.add(getTopProject().getName());
        
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = getBaseName() + "_" + detail.getProjectName();
            projectsNeeded.add(name);
            PhaseJobsConfig phase = new PhaseJobsConfig(name, 
                    /*jobproperties*/"", 
                    /*currParams*/true, 
                    /*configs*/null, 
                    PhaseJobsConfig.KillPhaseOnJobResultCondition.NEVER, 
                    /*disablejob*/false, 
                    /*enableretrystrategy*/false, 
                    /*parsingrulespath*/null, 
                    /*retries*/0, 
                    /*enablecondition*/false, 
                    /*abort*/false, 
                    /*condition*/"", 
                    /*buildonly if scm changes*/false,
                    /*applycond if no scm changes*/false);
            phaseJobs.add(phase);
        }
        MultiJobBuilder multiJobBuilder = new MultiJobBuilder("Build, Execute and Report", phaseJobs, MultiJobBuilder.ContinuationCondition.COMPLETED);
        getTopProject().getBuildersList().add(multiJobBuilder);

        // Copy artifacts per building project
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = getBaseName() + "_" + detail.getProjectName();
            String tarFile = "";
            if (isUsingSCM()) {
                tarFile = ", " + getBaseName() + "_" + detail.getProjectName() + "_build.tar";
            }
            CopyArtifact copyArtifact = new CopyArtifact(name);
            copyArtifact.setOptional(true);
            copyArtifact.setFilter("**/*incremental_rebuild_report*," +
                                   "**/*_report.html, " +
                                   "xml_data/**" +
                                   tarFile);
            copyArtifact.setFingerprintArtifacts(false);
            BuildSelector bs = new WorkspaceSelector();
            copyArtifact.setSelector(bs);
            getTopProject().getBuildersList().add(copyArtifact);
        }
        addMultiJobBuildCommand();
                
        // Post-build actions if doing reporting
        if (getOptionUseReporting()) {
            addArchiveArtifacts(getTopProject());
            addXunit(getTopProject());
            addVCCoverage(getTopProject());
            addGroovyScriptMultiJob();
        }
        
        getTopProject().save();
        
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = getBaseName() + "_" + detail.getProjectName();
            createProjectPair(name, detail, update);
        }
    }
    /**
     * Add multi-job build command to top-level project
     */
    private void addMultiJobBuildCommand() {
    	String html_text = "";
        String report_format="";

        if (getOptionHtmlBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
            report_format = "HTML";
        } else {
            html_text = ".txt";
            report_format = "TEXT";
        }

        String win =
"set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
getEnvironmentSetupWin() + "\n";
        if (isUsingSCM()) {
            win +=
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\extract_build_dir.py\"\n";
        }
        if (getOptionUseReporting()) {
            win +=
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\incremental_build_report_aggregator.py\" --api 2 --rptfmt " + report_format + "\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --full-status=@PROJECT_BASE@_full_report.html\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --full-status > @PROJECT_BASE@_full_report.txt\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=aggregate   --output=\"@PROJECT_BASE@_aggregate_report.html\"\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=metrics     --output=\"@PROJECT_BASE@_metrics_report.html\"\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=environment --output=\"@PROJECT_BASE@_environment_report.html\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\gen-combined-cov.py\" \"@PROJECT_BASE@_aggregate_report.html\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\getTotals.py\" --api 2 @PROJECT_BASE@_full_report.txt\n";
        }
        win +=
getEnvironmentTeardownWin() + "\n";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@PROJECT_BASE@", getBaseName());

        String unix =
"export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
getEnvironmentSetupUnix() + "\n";
        if (isUsingSCM()) {
            unix +=
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/extract_build_dir.py\"\n";
        }
        if (getOptionUseReporting()) {
            unix +=
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/incremental_build_report_aggregator.py\" --api 2 --rptfmt " + report_format + "\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --full-status=@PROJECT_BASE@_full_report.html\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --full-status > @PROJECT_BASE@_full_report.txt\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=aggregate   --output=\"@PROJECT_BASE@_aggregate_report.html\"\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=metrics     --output=\"@PROJECT_BASE@_metrics_report.html\"\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=environment --output=\"@PROJECT_BASE@_environment_report.html\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/gen-combined-cov.py\" \"@PROJECT_BASE@_aggregate_report.html\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/getTotals.py\" --api 2 @PROJECT_BASE@_full_report.txt\n" +
"\n";
        }
        unix +=
getEnvironmentTeardownUnix() + "\n";
        unix = StringUtils.replace(unix, "@PROJECT@", getManageProjectName());
        unix = StringUtils.replace(unix, "@PROJECT_BASE@", getBaseName());
        
        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        getTopProject().getBuildersList().add(command);
    }
    /**
     * Add groovy script to top-level project
     */
    private void addGroovyScriptMultiJob() {
		String html_text = "";
		String html_newline = "";

        if (getOptionHtmlBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
            html_newline = "<br>";
        } else {
            html_text = ".txt";
            html_newline = "\\n";
        }
        String script =
"import hudson.FilePath\n" +
"\n" +
"FilePath fp_c = new FilePath(manager.build.getWorkspace(),'CombinedReport" + html_text + "')\n" +
"FilePath fp_f = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_full_report" + html_text + "')\n" +
"\n" +
"if (fp_c.exists() && fp_f.exists())\n" +
"{\n" +
"    manager.build.description = fp_c.readToString() + \"" + html_newline + "\" + fp_f.readToString()" +
"}\n" +
"else\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"General Failure\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.build.description = \"General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\"\n" +
"    manager.addBadge(\"warning.gif\", \"General Error\")\n" +
"}\n" +
"\n";
        script = StringUtils.replace(script, "@PROJECT_BASE@", getBaseName());

        SecureGroovyScript secureScript = new SecureGroovyScript(script, /*sandbox*/false, /*classpath*/null);
        GroovyPostbuildRecorder groovy = new GroovyPostbuildRecorder(secureScript, /*behaviour*/2, /*matrix parent*/false);
        getTopProject().getPublishersList().add(groovy);
    }
    /**
     * Create pair (execute and reporting) for given details
     * @param baseName project basename
     * @param detail detail
     * @param update update or new
     * @throws IOException 
     */
    private void createProjectPair(String baseName, MultiJobDetail detail, boolean update) throws IOException {
        // Building job
        if (!getInstance().getJobNames().contains(baseName)) {
            projectsAdded.add(baseName);
            FreeStyleProject p = getInstance().createProject(FreeStyleProject.class, baseName);
            if (p == null) {
                return;
            }
            SCM scm = getTopProject().getScm();
            p.setScm(scm);
            addDeleteWorkspaceBeforeBuildStarts(p);
            Label label = new LabelAtom(detail.getCompiler());
            p.setAssignedLabel(label);
            addSetup(p);
            addBuildCommands(p, detail, baseName);
            if (getOptionUseReporting()) {
                addReportingCommands(p, detail, baseName);
                addArchiveArtifacts(p);
                addXunit(p);
                addVCCoverage(p);
                addPostReportingGroovy(p);
                // RMK : TODO - fixup/combine groovy
            } else {
                addPostbuildGroovy(p, detail, baseName);
            }
            p.save();
        } else {
            projectsExisting.add(baseName);
        }
    }
    /**
     * Add groovy step to reporting project
     * @param project project to add to
     */
    private void addPostReportingGroovy(Project project) {
        String script = 
"if(manager.logContains(\".*py did not execute correctly.*\") || manager.logContains(\".*Traceback .most recent call last.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Jenkins Integration Script Failure\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"Jenkins Integration Script Failure\")\n" +
"}\n" +
"if (manager.logContains(\".*Failed to acquire lock on environment.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Failed to acquire lock on environment\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"Failed to acquire lock on environment\")\n" +
"}\n" +
"if (manager.logContains(\".*Environment Creation Failed.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Environment Creation Failed\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"Environment Creation Failed\")\n" +
"}\n" +
"if (manager.logContains(\".*FLEXlm Error.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"FLEXlm Error\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"FLEXlm Error\")\n" +
"}\n" +
"if (manager.logContains(\".*INCR_BUILD_FAILED.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Build Error\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"Build Error\")\n" +
"}\n" +
"if (manager.logContains(\".*NOT_LINKED.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Link Error\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"Link Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Preprocess Failed.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Preprocess Error\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"Preprocess Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Value Line Error - Command Ignored.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Test Case Import Error\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"Test Case Import Error\")\n" +
"}\n" +
"\n" +
"if(manager.logContains(\".*Abnormal Termination on Environment.*\")) \n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Abnormal Termination of at least one Environment\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.addBadge(\"warning.gif\", \"Abnormal Termination of at least one Environment\")\n" +
"}\n";
        
        SecureGroovyScript secureScript = new SecureGroovyScript(script, /*sandbox*/false, /*classpath*/null);
        GroovyPostbuildRecorder groovy = new GroovyPostbuildRecorder(secureScript, /*behaviour*/2, /*matrix parent*/false);
        project.getPublishersList().add(groovy);
    }
    /**
     * Add reporting commands step to project
     * @param project project to add to
     * @param detail job details
     * @param baseName project basename
     */
    private void addReportingCommands(Project project, MultiJobDetail detail, String baseName) {
        String noGenExecReport = "";
        if (!getOptionExecutionReport()) {
            noGenExecReport = " --dont-gen-exec-rpt";
        }
        
        String win =
getEnvironmentSetupWin() + "\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\generate-results.py\" --api 2 \"@PROJECT@\" --level @LEVEL@ -e @ENV@ " + noGenExecReport + "\n" +
getEnvironmentTeardownWin() + "\n" +
"";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@LEVEL@", detail.getLevel());
        win = StringUtils.replace(win, "@ENV@", detail.getEnvironment());
        
        String unix =
getEnvironmentSetupUnix() + "\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/generate-results.py\" --api 2 \"@PROJECT@\" --level @LEVEL@ -e @ENV@ " + noGenExecReport + "\n" +
getEnvironmentTeardownUnix() + "\n" +
"";
        unix = StringUtils.replace(unix, "@PROJECT@", getManageProjectName());
        unix = StringUtils.replace(unix, "@LEVEL@", detail.getLevel());
        unix = StringUtils.replace(unix, "@ENV@", detail.getEnvironment());

        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        project.getBuildersList().add(command);
    }
    /**
     * Add build commands step to project
     * @param project project to add to
     * @param detail job details
     * @param baseName project basename
     */
    private void addBuildCommands(Project project, MultiJobDetail detail, String baseName) {
        String report_format="";
        String html_text="";

        if (getOptionHtmlBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
            report_format = "HTML";
        } else {
            html_text = ".txt";
            report_format = "TEXT";
        }

        String win =
"set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
getEnvironmentSetupWin() + "\n";
        win +=
getExecutePreambleWin() +
" %VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --config VCAST_CUSTOM_REPORT_FORMAT=" + report_format + "\n" +
" %VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --level @LEVEL@ -e @ENV@ --build-execute --incremental --output @BASENAME@_manage_incremental_rebuild_report" + html_text + "\n" +
" %VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --config VCAST_CUSTOM_REPORT_FORMAT=HTML\n";

        if (isUsingSCM()) {
            win +=
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\copy_build_dir.py\" \"@PROJECT@\" @LEVEL@ @BASENAME@ @ENV@\n";
        }
        win +=
getEnvironmentTeardownWin() + "\n" +
"\n";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@LEVEL@", detail.getLevel());
        win = StringUtils.replace(win, "@ENV@", detail.getEnvironment());
        win = StringUtils.replace(win, "@BASENAME@", baseName);
        
        String unix = 
"export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
getEnvironmentSetupUnix() + "\n";
        unix +=
getExecutePreambleUnix() +
" $VECTORCAST_DIR/manage --project \"@PROJECT@\" --config VCAST_CUSTOM_REPORT_FORMAT=" + report_format + "\n" +
" $VECTORCAST_DIR/manage --project \"@PROJECT@\" --level @LEVEL@ -e @ENV@ --build-execute --incremental --output @BASENAME@_manage_incremental_rebuild_report" + html_text + "\n" +
 "$VECTORCAST_DIR/manage --project \"@PROJECT@\" --config VCAST_CUSTOM_REPORT_FORMAT=HTML\n";
       if (isUsingSCM()) {
            unix +=
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/copy_build_dir.py\" \"@PROJECT@\" @LEVEL@ @BASENAME@ @ENV@\n";
        }
        unix +=
getEnvironmentTeardownUnix() + "\n" +
"\n";
        unix = StringUtils.replace(unix, "@PROJECT@", getManageProjectName());
        unix = StringUtils.replace(unix, "@LEVEL@", detail.getLevel());
        unix = StringUtils.replace(unix, "@ENV@", detail.getEnvironment());
        unix = StringUtils.replace(unix, "@BASENAME@", baseName);

        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        project.getBuildersList().add(command);
    }
    /**
     * Add post-build groovy step to project
     * @param project project to add to
     * @param detail job details
     * @param baseName project basename
     */
    private void addPostbuildGroovy(Project project, MultiJobDetail detail, String baseName) {
        String setBuildStatus;
        String gif;
        String html_text;
        if (getOptionErrorLevel().equalsIgnoreCase("unstable")) {
            setBuildStatus = "    manager.buildUnstable()\n";
            gif = "\"warning.gif\"";
        } else {
            setBuildStatus = "    manager.buildFailure()\n";
            gif = "\"error.gif\"";
        }
        if (getOptionHtmlBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
        } else {
            html_text = ".txt";
        }
        String script =
"import hudson.FilePath\n" +
"\n" +
"if(manager.logContains(\".*py did not execute correctly.*\") || manager.logContains(\".*Traceback .most recent call last.*\"))\n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"Jenkins Integration Script Failure\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"Jenkins Integration Script Failure\")\n" +
"}\n" +
"if (manager.logContains(\".*Failed to acquire lock on environment.*\"))\n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"Failed to acquire lock on environment\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"Failed to acquire lock on environment\")\n" +
"}\n" +
"if (manager.logContains(\".*Environment Creation Failed.*\"))\n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"Environment Creation Failed\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"Environment Creation Failed\")\n" +
"}\n" +
"if (manager.logContains(\".*FLEXlm Error.*\"))\n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"FLEXlm Error\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"FLEXlm Error\")\n" +
"}\n" +
"if (manager.logContains(\".*INCR_BUILD_FAILED.*\"))\n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"Build Error\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"Build Error\")\n" +
"}\n" +
"if (manager.logContains(\".*NOT_LINKED.*\"))\n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"Link Error\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"Link Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Preprocess Failed.*\"))\n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"Preprocess Error\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"Preprocess Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Value Line Error - Command Ignored.*\"))\n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"Test Case Import Error\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"Test Case Import Error\")\n" +
"}\n" +
"\n" +
"if(manager.logContains(\".*Abnormal Termination on Environment.*\")) \n" +
"{\n" +
"    manager.createSummary(" + gif + ").appendText(\"Abnormal Termination of at least one Environment\", false, false, false, \"red\")\n" +
setBuildStatus +
"    manager.addBadge(" + gif + ", \"Abnormal Termination of at least one Environment\")\n" +
"}\n" +
"\n" +
"FilePath fp_i = new FilePath(manager.build.getWorkspace(),'@BASENAME@_manage_incremental_rebuild_report" + html_text + "')\n" +
"\n" +
"if (!fp_i.exists())\n" +
"    manager.build.description = \"General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\"\n" +
"\n" +
"      ";
        script = StringUtils.replace(script, "@BASENAME@", baseName);
        
        SecureGroovyScript secureScript = new SecureGroovyScript(script, /*sandbox*/false, /*classpath*/null);
        GroovyPostbuildRecorder groovy = new GroovyPostbuildRecorder(secureScript, /*behaviour*/2, /*matrix parent*/false);
        project.getPublishersList().add(groovy);
    }
}
