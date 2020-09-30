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
import com.vectorcast.plugins.vectorcastexecution.VectorCASTSetup;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
     * @param request request object
     * @param response response object
     * @param useSavedData use saved data true/false
     * @throws ServletException exception
     * @throws IOException exception
     */
    public NewMultiJob(final StaplerRequest request, final StaplerResponse response, boolean useSavedData) throws ServletException, IOException {
        super(request, response, useSavedData);
    }
    /**
     * Use Saved Data
     * @param savedData saved data to use
     */
    @Override
    public void useSavedData(VectorCASTSetup savedData) {
        super.useSavedData(savedData);
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
     * @throws IOException exception
     * @throws JobAlreadyExistsException exception
     */
    @Override
    protected Project createProject() throws IOException, JobAlreadyExistsException {
        multiProjectName = getBaseName() + ".vcast.multi";
        if (getJobName() != null && !getJobName().isEmpty()) {
            multiProjectName = getJobName();
        }
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
            if (job.getFullName().equalsIgnoreCase(multiProjectName)) {
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
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws InvalidProjectFileException exception
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
        String tmpLabel = getNodeLabel();
        if (tmpLabel == null || tmpLabel.isEmpty()) {
            tmpLabel = "master";
        }
        Label label = new LabelAtom(tmpLabel);
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
        
        String baseName = getBaseName();
        if (getJobName() != null && !getJobName().isEmpty()) {
            baseName = getJobName();
        }
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = baseName + "_" + detail.getProjectName();
            projectsNeeded.add(name);
            PhaseJobsConfig phase = null;
            try {
                // Orginal (pre MultiJob 1.30 and possibly earlier), use these
                // parameters
                Constructor ctor = PhaseJobsConfig.class.getConstructor(
                        /*name*/String.class,
                        /*jobproperties*/String.class,
                        /*currParams*/boolean.class,
                        /*configs*/List.class,
                        /*killPhaseOnJobResultCondition*/PhaseJobsConfig.KillPhaseOnJobResultCondition.class,
                        /*disablejob*/boolean.class,
                        /*enableretrystrategy*/boolean.class,
                        /*parsingrulespath*/String.class,
                        /*retries*/int.class,
                        /*enablecondition*/boolean.class,
                        /*abort*/boolean.class,
                        /*condition*/String.class,
                        /*buildonly if scm changes*/boolean.class,
                        /*applycond if no scm changes*/boolean.class);
            
                phase = (PhaseJobsConfig)ctor.newInstance(name, 
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
            } catch (NoSuchMethodException ex) {
                try {
                    // By MultiJob 1.30 there is a new parameter
                    Constructor ctor = PhaseJobsConfig.class.getConstructor(
                            /*jobAlias*/String.class,
                            /*name*/String.class,
                            /*jobproperties*/String.class,
                            /*currParams*/boolean.class,
                            /*configs*/List.class,
                            /*killPhaseOnJobResultCondition*/PhaseJobsConfig.KillPhaseOnJobResultCondition.class,
                            /*disablejob*/boolean.class,
                            /*enableretrystrategy*/boolean.class,
                            /*parsingrulespath*/String.class,
                            /*retries*/int.class,
                            /*enablecondition*/boolean.class,
                            /*abort*/boolean.class,
                            /*condition*/String.class,
                            /*buildonly if scm changes*/boolean.class,
                            /*applycond if no scm changes*/boolean.class);
                    phase = (PhaseJobsConfig)ctor.newInstance(name, 
                        /*jobAlias*/"",
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
                } catch (NoSuchMethodException ex1) {
                    Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (SecurityException ex1) {
                    Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (InstantiationException ex1) {
                    Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (IllegalAccessException ex1) {
                    Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (IllegalArgumentException ex1) {
                    Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex1);
                } catch (InvocationTargetException ex1) {
                    Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex1);
                }
            } catch (SecurityException ex) {
                Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvocationTargetException ex) {
                Logger.getLogger(NewMultiJob.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (phaseJobs != null) {
                phaseJobs.add(phase);
            }
        }
        MultiJobBuilder multiJobBuilder = new MultiJobBuilder("Build, Execute and Report", phaseJobs, MultiJobBuilder.ContinuationCondition.COMPLETED);
        getTopProject().getBuildersList().add(multiJobBuilder);

        // Copy artifacts per building project
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = baseName + "_" + detail.getProjectName();
            String tarFile = "";
            if (isUsingSCM()) {
                tarFile = ", " + getBaseName() + "_" + detail.getProjectName() + "_build.tar";
            }
            CopyArtifact copyArtifact = new CopyArtifact(name);
            copyArtifact.setOptional(true);
            copyArtifact.setFilter("**/*_rebuild*," +
                                   "execution/**, " +
                                   "management/**, " +
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
            addJunit(getTopProject());
            addVCCoverage(getTopProject());
            addGroovyScriptMultiJob();
        }
        
        getTopProject().save();
        
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = baseName + "_" + detail.getProjectName();
            createProjectPair(name, detail, update);
        }
    }
    /**
     * Add multi-job build command to top-level project
     */
    private void addMultiJobBuildCommand() {
    	String html_text = "";
        String report_format="";

        if (getOptionHTMLBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
            report_format = "HTML";
        } else {
            html_text = ".txt";
            report_format = "TEXT";
        }

        String win =
"set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"set VCAST_RPTS_SELF_CONTAINED=FALSE\n" +
getEnvironmentSetupWin() + "\n";
        if (isUsingSCM()) {
            win +=
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\extract_build_dir.py\"\n";
        }
        if (getOptionUseReporting()) {
            win +=
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\incremental_build_report_aggregator.py\" @PROJECT_BASE@ --rptfmt " + report_format + "\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --full-status=@PROJECT_BASE@_full_report.html\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=aggregate   --output=\\\"@PROJECT_BASE@_aggregate_report.html\\\"\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=metrics     --output=\\\"@PROJECT_BASE@_metrics_report.html\\\"\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=environment --output=\\\"@PROJECT_BASE@_environment_report.html\\\"\"\n" +
"\n";
        }
        win +=
getEnvironmentTeardownWin() + "\n";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@PROJECT_BASE@", getBaseName());

        String unix =
"export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"export VCAST_RPTS_SELF_CONTAINED=FALSE\n" +
getEnvironmentSetupUnix() + "\n";
        if (isUsingSCM()) {
            unix +=
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/extract_build_dir.py\"\n";
        }
        if (getOptionUseReporting()) {
            unix +=
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/incremental_build_report_aggregator.py\" @PROJECT_BASE@ --rptfmt " + report_format + "\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --full-status=@PROJECT_BASE@_full_report.html\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=aggregate   --output=\\\"@PROJECT_BASE@_aggregate_report.html\\\"\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=metrics     --output=\\\"@PROJECT_BASE@_metrics_report.html\\\"\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=environment --output=\\\"@PROJECT_BASE@_environment_report.html\\\"\"\n" +
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

        if (getOptionHTMLBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
            html_newline = "<br>";
        } else {
            html_text = ".txt";
            html_newline = "\\n";
        }
        String script =
"import hudson.FilePath\n" +
"\n" +
"FilePath fp_r = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_rebuild" + html_text + "')\n" +
"FilePath fp_f = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_full_report" + html_text + "')\n" +
"\n" +
"if (fp_r.exists() && fp_f.exists())\n" +
// Must put HTML in createSummary and not description. Description will be truncated
// and shown in Build history on left and cause corruption in the display, particularly
// if using 'anything-goes-formatter'
"{\n" +
"    manager.createSummary(\"monitor.png\").appendText(fp_r.readToString() + \"" + html_newline + "\" + fp_f.readToString(), false)\n" +
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
            addBuildCommands(p, detail, baseName, detail.getProjectName());
            if (getOptionUseReporting()) {
                addReportingCommands(p, detail, baseName);
                addArchiveArtifacts(p);
                addJunit(p);
                addVCCoverage(p);
                addPostReportingGroovy(p);
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
"Boolean buildFailed = false\n" +
"Boolean buildUnstable = false\n" +
"\n" +
"if(manager.logContains(\".*py did not execute correctly.*\") || manager.logContains(\".*Traceback .most recent call last.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Jenkins Integration Script Failure\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Jenkins Integration Script Failure\")\n" +
"}\n" +
"if (manager.logContains(\".*Failed to acquire lock on environment.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Failed to acquire lock on environment\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Failed to acquire lock on environment\")\n" +
"}\n" +
"if (manager.logContains(\".*Environment Creation Failed.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Environment Creation Failed\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Environment Creation Failed\")\n" +
"}\n" +
"if (manager.logContains(\".*FLEXlm Error.*\") || manager.logContains(\".*ERROR: Failed to obtain a license.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"FLEXlm Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"FLEXlm Error\")\n" +
"}\n" +
"if (manager.logContains(\".*INCR_BUILD_FAILED.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Build Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Build Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Environment was not successfully built.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Build Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Build Error\")\n" +
"}\n" +
"if (manager.logContains(\".*NOT_LINKED.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Link Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Link Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Preprocess Failed.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Preprocess Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Preprocess Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Value Line Error - Command Ignored.*\") || manager.logContains(\".*(E) @LINE:.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Test Case Import Error\", false, false, false, \"red\")\n" +
"    buildUnstable = true\n" +
"    manager.addBadge(\"warning.gif\", \"Test Case Import Error\")\n" +
"}\n" +
"\n" +
"if(manager.logContains(\".*Abnormal Termination on Environment.*\")) \n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Abnormal Termination of at least one Environment\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Abnormal Termination of at least one Environment\")\n" +
"}\n" +
"\n" +
"if (buildFailed && !buildUnstable)\n" +
"{\n" +
"    manager.buildFailure()\n" +
"}\n" +
"if (buildUnstable)\n" +
"{\n" +
"    manager.buildUnstable()\n" +
"}\n" +
"\n";

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
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\generate-results.py\" --junit --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + "  \"@PROJECT@\" --level @LEVEL@ -e @ENV@ " + noGenExecReport + "\n" +
getEnvironmentTeardownWin() + "\n" +
"";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@LEVEL@", detail.getLevel());
        win = StringUtils.replace(win, "@ENV@", detail.getEnvironment());
        
        String unix =
getEnvironmentSetupUnix() + "\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/generate-results.py\" --junit --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + "  \"@PROJECT@\" --level @LEVEL@ -e @ENV@ " + noGenExecReport + "\n" +
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
     * @param name project environment+compiler name (no manage project)
     */
    private void addBuildCommands(Project project, MultiJobDetail detail, String baseName, String name) {
        String report_format="";
        String html_text="";

        if (getOptionHTMLBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
            report_format = "HTML";
        } else {
            html_text = ".txt";
            report_format = "TEXT";
        }

        String win =
"set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"set VCAST_RPTS_SELF_CONTAINED=FALSE\n" +
getEnvironmentSetupWin() + "\n";
        win +=
getExecutePreambleWin() +
" %VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --config VCAST_CUSTOM_REPORT_FORMAT=" + report_format + "\"\n" +
" %VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --level @LEVEL@ -e @ENV@ --build-execute --incremental --output @NAME@_rebuild" + html_text + "\"\n" +
" %VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --config VCAST_CUSTOM_REPORT_FORMAT=HTML\"\n";

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
        win = StringUtils.replace(win, "@NAME@", name);
        
        String unix = 
"export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"export VCAST_RPTS_SELF_CONTAINED=FALSE\n" +
getEnvironmentSetupUnix() + "\n";
        unix +=
getExecutePreambleUnix() +
" $VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --config VCAST_CUSTOM_REPORT_FORMAT=" + report_format + "\"\n" +
" $VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --level @LEVEL@ -e @ENV@ --build-execute --incremental --output @NAME@_rebuild" + html_text + "\"\n" +
" $VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --config VCAST_CUSTOM_REPORT_FORMAT=HTML\"\n";
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
        unix = StringUtils.replace(unix, "@NAME@", name);

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
        String html_text;
        if (getOptionHTMLBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
        } else {
            html_text = ".txt";
        }
        String script =
"import hudson.FilePath\n" +
"\n" +
"Boolean buildFailed = false\n" +
"Boolean buildUnstable = false\n" +
"\n" +
"if(manager.logContains(\".*py did not execute correctly.*\") || manager.logContains(\".*Traceback .most recent call last.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Jenkins Integration Script Failure\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Jenkins Integration Script Failure\")\n" +
"}\n" +
"if (manager.logContains(\".*Failed to acquire lock on environment.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Failed to acquire lock on environment\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Failed to acquire lock on environment\")\n" +
"}\n" +
"if (manager.logContains(\".*Environment Creation Failed.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Environment Creation Failed\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Environment Creation Failed\")\n" +
"}\n" +
"if (manager.logContains(\".*FLEXlm Error.*\") || manager.logContains(\".*ERROR: Failed to obtain a license.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"FLEXlm Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"FLEXlm Error\")\n" +
"}\n" +
"if (manager.logContains(\".*INCR_BUILD_FAILED.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Build Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Build Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Environment was not successfully built.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Build Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Build Error\")\n" +
"}\n" +
"if (manager.logContains(\".*NOT_LINKED.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Link Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Link Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Preprocess Failed.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Preprocess Error\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Preprocess Error\")\n" +
"}\n" +
"if (manager.logContains(\".*Value Line Error - Command Ignored.*\") || manager.logContains(\".*(E) @LINE:.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Test Case Import Error\", false, false, false, \"red\")\n" +
"    buildUnstable = true\n" +
"    manager.addBadge(\"warning.gif\", \"Test Case Import Error\")\n" +
"}\n" +
"\n" +
"if(manager.logContains(\".*Abnormal Termination on Environment.*\")) \n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Abnormal Termination of at least one Environment\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Abnormal Termination of at least one Environment\")\n" +
"}\n" +
"\n" +
"if (buildFailed && !buildUnstable)\n" +
"{\n" +
"    manager.buildFailure()\n" +
"}\n" +
"if (buildUnstable)\n" +
"{\n" +
"    manager.buildUnstable()\n" +
"}\n" +
"\n" +
"FilePath fp_r = new FilePath(manager.build.getWorkspace(),'@NAME@_rebuild" + html_text + "')\n" +
"\n" +
"if (!fp_r.exists())\n" +
"{\n" +
"    manager.build.description = \"General Failure, Incremental Build Report Not Present. Please see the console for more information\"\n" +
"}\n" +
"\n";
        script = StringUtils.replace(script, "@BASENAME@", baseName);
        script = StringUtils.replace(script, "@NAME@", detail.getProjectName());
        
        SecureGroovyScript secureScript = new SecureGroovyScript(script, /*sandbox*/false, /*classpath*/null);
        GroovyPostbuildRecorder groovy = new GroovyPostbuildRecorder(secureScript, /*behaviour*/2, /*matrix parent*/false);
        project.getPublishersList().add(groovy);
    }
}
