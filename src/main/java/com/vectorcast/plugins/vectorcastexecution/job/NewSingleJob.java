/*
 * The MIT License
 *
 * Copyright 2016 Vector Software, East Greenwich, Rhode Island USA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the f"Software"), to deal
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

import com.vectorcast.plugins.vectorcastexecution.common.VcastUtils;

import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Project;
import hudson.model.labels.LabelAtom;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Create a new single job
 */
public class NewSingleJob extends BaseJob {
    /** project name */
    private String projectName;
    /**
     * Constructor
     * @param request request object
     * @param response response object
     * @throws ServletException exception
     * @throws IOException exception
     */
    public NewSingleJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        super(request, response, false);
    }
    /**
     * Get the name of the project
     * @return the project name
     */
    public String getProjectName() {
        return projectName;
    }
    /**
     * Add build commands step to job
     */
    private void addCommandSingleJob() {
        String noGenExecReport = "";
        String html_text = "";
        String report_format="";
        if (!getOptionExecutionReport()) {
            noGenExecReport = " --dont-gen-exec-rpt";
        }
        if (getOptionHTMLBuildDesc().equalsIgnoreCase("HTML")) {
            html_text = ".html";
            report_format = "HTML";
        } else {
            html_text = ".txt";
            report_format = "TEXT";
        }
        String pclpCommandString = "";
        String squoreCommandString_win = "";
        String squoreCommandString_unix = "";
        String TESTinsightsSCMconnect_win = "\n";
        String TESTinsightsSCMconnect_unix = "\n";
        String TESTinsightsCommandString_win = "";
        String TESTinsightsCommandString_unix = "";
        String scmInfoCommand_win = "";
        String scmInfoCommand_unix = "";
        
        if (getPclpCommand().length() != 0) {
            pclpCommandString = getPclpCommand() + "\n";
        }            
        if (getSquoreCommand().length() != 0) {
            squoreCommandString_win = "%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\generate_squore_results.py\" \"@PROJECT@\"";
            squoreCommandString_win += "\n" + getSquoreCommand() + "\n";
            squoreCommandString_unix = "$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/generate_squore_results.py\" \"@PROJECT@\"";
            squoreCommandString_unix += "\n" + getSquoreCommand() + "\n";
        }            
        if (getTESTinsights_URL().length() != 0) {
            boolean setupConnect = false;
            if (isUsingSCM()) {
                if (getTESTinsights_SCM_Tech() == "git") {
                    scmInfoCommand_win = "git config remote.origin.url > scm_url.tmp\n" +
                        "set /p SCM_URL= < scm_url.tmp\n" +
                        "git rev-parse HEAD > scm_rev.tmp\n" +
                        "set /p SCM_REV= < scm_rev.tmp\n";
                    scmInfoCommand_unix = "SCM_URL=`git config remote.origin.url`\n" +
                        "SCM_REV=`git rev-parse HEAD`\n";
                    setupConnect = true;
                                            
                }
                if (getTESTinsights_SCM_Tech() == "svn") {
                    scmInfoCommand_win = "svn info --show-item=url --no-newline > scm_url.tmp\n" +
                        "set /p SCM_URL= < scm_url.tmp\n" +
                        "git svn info --show-item revision > scm_rev.tmp\n" +
                        "set /p SCM_REV= < scm_rev.tmp\n";
                    scmInfoCommand_unix = "SCM_URL=`svn info --show-item=url --no-newline`\n" +
                        "SCM_REV=`svn info --show-item revision`\n";
                    setupConnect = true;
                }
                if (setupConnect) {
                    TESTinsightsSCMconnect_win = " --vc-project-local-path=%WORKSPACE%/\"@PROJECT@\" --vc-project-scm-path=%SCM_URL%/\"@PROJECT@\" --src-local-path=%WORKSPACE% --src-scm-path=%SCM_URL%/ --vc-project-scm-technology=" + getTESTinsights_SCM_Tech() + " --src-scm-technology=" + getTESTinsights_SCM_Tech() + " --vc-project-scm-revision=%SCM_REV% --src-scm-revision %SCM_REV% --versioned\n";
                    TESTinsightsSCMconnect_unix = " --vc-project-local-path=$WORKSPACE/\"@PROJECT@\" --vc-project-scm-path=$SCM_URL/\"@PROJECT@\" --src-local-path=$WORKSPACE --src-scm-path=$SCM_URL/ --vc-project-scm-technology=" + getTESTinsights_SCM_Tech() + " --src-scm-technology=" + getTESTinsights_SCM_Tech() + " --vc-project-scm-revision=$SCM_REV --src-scm-revision $SCM_REV --versioned\n";
                }
            }
            if (setupConnect) {
                TESTinsightsCommandString_win  = scmInfoCommand_win;
                TESTinsightsCommandString_unix = scmInfoCommand_unix;
            }
            TESTinsightsCommandString_win  += "testinsights_connector --api " + getTESTinsights_URL() + " --user %VC_TI_USR%  --pass %VC_TI_PWS% --action PUSH --project " + getTESTinsights_project() + " --test-object %BUILD_NUMBER% --vc-project \"@PROJECT@\" --proxy " + getTESTinsights_proxy() + " --log TESTinsight_Push.log " + TESTinsightsSCMconnect_win;
            TESTinsightsCommandString_unix += "testinsights_connector --api " + getTESTinsights_URL() + " --user $VC_TI_USR   --pass $VC_TI_PWS  --action PUSH --project " + getTESTinsights_project() + " --test-object $BUILD_NUMBER --vc-project \"@PROJECT@\" --proxy " + getTESTinsights_proxy() + " --log TESTinsight_Push.log " + TESTinsightsSCMconnect_unix;
        }            
        String pluginVersion = VcastUtils.getVersion().orElse( "Unknown" );    
        String win = 
"rem Created with vectorcast-execution plugin v" + pluginVersion + "\n\n" +
getEnvironmentSetupWin() + "\n" +
"set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"set VCAST_NO_FILE_TRUNCATION=1\n" +
"set VCAST_RPTS_SELF_CONTAINED=FALSE\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --status\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --force --release-locks\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --config VCAST_CUSTOM_REPORT_FORMAT=" + report_format + "\"\n" +
getExecutePreambleWin() +
"del command.log > nul 2>&1\n"+
" %VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --build-execute --incremental --output \\\"@PROJECT_BASE@_rebuild" + html_text + "\\\" \"\n" +
"copy command.log complete_build.log\n"+
"copy \"@PROJECT_BASE@_rebuild" + html_text + "\" \"@PROJECT_BASE@_rebuild" + html_text + "_tmp\"\n"+
"\n" +
pclpCommandString + squoreCommandString_win + TESTinsightsCommandString_win;
        if (getOptionUseReporting()) {
            win +=
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --config VCAST_CUSTOM_REPORT_FORMAT=HTML\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\generate-results.py\" --junit --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " \"@PROJECT@\" " + noGenExecReport + " --buildlog complete_build.log\n" +

"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\full_report_no_toc.py\" \"@PROJECT@\" \n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\fixup_reports.py\" \"@PROJECT_BASE@_rebuild" + html_text + "_tmp\"\n" +

"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --full-status=\\\"@PROJECT_BASE@_full_report.html\\\"\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=aggregate   --output=\\\"@PROJECT_BASE@_aggregate_report.html\\\"\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=metrics     --output=\\\"@PROJECT_BASE@_metrics_report.html\\\"\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=environment --output=\\\"@PROJECT_BASE@_environment_report.html\\\"\"\n";
        }
        win += getEnvironmentTeardownWin() + "\n";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@PROJECT_BASE@", getBaseName());

        String unix = 
"##Created with vectorcast-execution plugin v" + pluginVersion + "\n\n" +
getEnvironmentSetupUnix() + "\n" +
"export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"export VCAST_NO_FILE_TRUNCATION=1\n" +
"export VCAST_RPTS_SELF_CONTAINED=FALSE\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --status \"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --force --release-locks \"\n" +
getExecutePreambleUnix() +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --config VCAST_CUSTOM_REPORT_FORMAT=" + report_format + "\"\n" +
"rm -f command.log\n"+
" $VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --build-execute --incremental  --output \\\"@PROJECT_BASE@_rebuild" + html_text + "\\\" \"\n" +
"cp -p command.log complete_build.log\n"+
"cp -p \"@PROJECT_BASE@_rebuild" + html_text + "\" \"@PROJECT_BASE@_rebuild" + html_text + "_tmp\"\n"+
pclpCommandString + squoreCommandString_unix + TESTinsightsCommandString_unix;

if (getOptionUseReporting()) {
            unix +=
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --config VCAST_CUSTOM_REPORT_FORMAT=HTML\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/generate-results.py\" --junit --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " \"@PROJECT@\" " + noGenExecReport + " --buildlog complete_build.log\n" +

"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/full_report_no_toc.py\" \"@PROJECT@\" \n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/fixup_reports.py\" \"@PROJECT_BASE@_rebuild" + html_text + "_tmp\"\n" +

"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=aggregate   --output=\\\"@PROJECT_BASE@_aggregate_report.html\\\"\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=metrics     --output=\\\"@PROJECT_BASE@_metrics_report.html\\\"\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/managewait.py\" --wait_time " + getWaitTime() + " --wait_loops " + getWaitLoops() + " --command_line \"--project \\\"@PROJECT@\\\" --create-report=environment --output=\\\"@PROJECT_BASE@_environment_report.html\\\"\"\n";
        }
        unix += getEnvironmentTeardownUnix() + "\n";
        unix = StringUtils.replace(unix, "@PROJECT@", getManageProjectName());
        unix = StringUtils.replace(unix, "@PROJECT_BASE@", getBaseName());

        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        getTopProject().getBuildersList().add(command);
    }
    /**
     * Add groovy script step to job
     */
    private void addGroovyScriptSingleJob() {
        String html_text;
        String html_newline;
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
"Boolean buildFailed = false\n" +
"Boolean buildUnstable = false\n" +
"\n" +
"if(manager.logContains(\".*INFO: File System Error.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"File System Error\", false, false, false, \"red\")\n" +
"    buildUnstable = true\n" +
"    manager.addBadge(\"warning.gif\", \"File System Error\")\n" +
"}\n" +
"if(manager.logContains(\".*INFO: Problem parsing test results.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"Test Results Parse Error\", false, false, false, \"red\")\n" +
"    buildUnstable = true\n" +
"    manager.addBadge(\"warning.gif\", \"Test Results Parse Error\")\n" +
"}\n" +
"if(manager.logContains(\".*ERROR: Error accessing DataAPI for.*\"))\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"DataAPI Error\", false, false, false, \"red\")\n" +
"    buildUnstable = true\n" +
"    manager.addBadge(\"warning.gif\", \"VectorCAST DataAPI Error\")\n" +
"}\n" +
"else if(manager.logContains(\".*py did not execute correctly.*\") || manager.logContains(\".*Traceback .most recent call last.*\"))\n" +
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
"if (manager.logContains(\".*Error with Test Case Management Report.*\"))\n" +
"{\n" +
"    manager.createSummary(\"error.gif\").appendText(\"Error with Test Case Management Report of at least one Environment\", false, false, false, \"red\")\n" +
"    buildFailed = true\n" +
"    manager.addBadge(\"error.gif\", \"Error with Test Case Management Report of at least one Environment\")\n" +
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
"FilePath fp_i = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_rebuild" + html_text + "_tmp')\n" +
"FilePath fp_f = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_full_report" + html_text + "_tmp')\n" +
"if (fp_i.exists() && fp_f.exists())\n" +
"{\n" +
// Must put HTML in createSummary and not description. Description will be truncated
// and shown in Build history on left and cause corruption in the display, particularly
// if using 'anything-goes-formatter'
"    manager.createSummary(\"monitor.png\").appendText(fp_i.readToString() + \"" + html_newline + "\" + fp_f.readToString(), false)\n" +
"}\n" +
"else\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"General Failure\", false, false, false, \"red\")\n" +
"    buildUnstable = true\n" +
"    manager.build.description = \"General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\"\n" +
"    manager.addBadge(\"warning.gif\", \"General Error\")\n" +
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
        script = StringUtils.replace(script, "@PROJECT_BASE@", getBaseName());
        script = StringUtils.replace(script, "@PROJECT@", getManageProjectName());
        
        SecureGroovyScript secureScript = new SecureGroovyScript(script, /*sandbox*/false, /*classpath*/null);
        GroovyPostbuildRecorder groovy = new GroovyPostbuildRecorder(secureScript, /*behaviour*/2, /*matrix parent*/false);
        getTopProject().getPublishersList().add(groovy);
    }
    /**
     * Create project
     * @return project
     * @throws IOException exception
     * @throws JobAlreadyExistsException exception
     */
    @Override
    protected Project createProject() throws IOException, JobAlreadyExistsException {
        if (getBaseName().isEmpty()) {
            getResponse().sendError(HttpServletResponse.SC_NOT_MODIFIED, "No project name specified");
            return null;
        }
        projectName = getBaseName() + ".vcast.single";
        if (getJobName() != null && !getJobName().isEmpty()) {
            projectName = getJobName();
        }
        if (getInstance().getJobNames().contains(projectName)) {
            throw new JobAlreadyExistsException(projectName);
        }
        Project project = getInstance().createProject(FreeStyleProject.class, projectName);
        if (getNodeLabel() != null && !getNodeLabel().isEmpty()) {
            Label label = new LabelAtom(getNodeLabel());
            project.setAssignedLabel(label);
        }
        return project;
    }
    /**
     * Add build steps
     * @param update true to update, false to not
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     */
    @Override
    public void doCreate(boolean update) throws IOException, ServletException, Descriptor.FormException {
        getTopProject().setDescription("Single job to run the manage project: " + getManageProjectName());

        // Build actions...
        addSetup(getTopProject());
        addCommandSingleJob();
                
        // Post-build actions - only is using reporting
        if (getOptionUseReporting()) {
            addArchiveArtifacts(getTopProject());
            addPCLintPlus(getTopProject());
            addJunit(getTopProject());
            addVCCoverage(getTopProject());
            addGroovyScriptSingleJob();
            if (getTESTinsights_URL().length() != 0) {
                addCredentialID(getTopProject());
            }
        }
        
        getTopProject().save();
    }
    @Override
    protected void cleanupProject() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
