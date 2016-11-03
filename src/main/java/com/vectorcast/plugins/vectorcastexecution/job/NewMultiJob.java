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

import com.tikal.jenkins.plugins.multijob.MultiJobBuild;
import com.tikal.jenkins.plugins.multijob.MultiJobBuilder;
import com.tikal.jenkins.plugins.multijob.MultiJobProject;
import com.tikal.jenkins.plugins.multijob.PhaseJobsConfig;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.Project;
import hudson.model.labels.LabelAtom;
import hudson.plugins.copyartifact.BuildSelector;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.plugins.copyartifact.WorkspaceSelector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 */
public class NewMultiJob extends BaseJob {
    
    private static final Logger LOG = Logger.getLogger(NewMultiJob.class.getName());

    private String manageFile;
    private ManageProject manageProject = null;

    public NewMultiJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        super(request, response);
    }

    @Override
    protected Project createProject() throws IOException {
        String projectName = getBaseName() + ".vcast_manage.multijob";
        return getInstance().createProject(MultiJobProject.class, projectName);
    }

    @Override
    protected void doCreate() throws IOException, ServletException, Descriptor.FormException {
        // Read the manage project file
        manageFile = getRequest().getFileItem("manageProject").getString();
        manageProject = new ManageProject(manageFile);
        manageProject.parse();

        if (manageProject == null) {
            return;
        }
        getTopProject().setDescription("Top-level multi job to run the manage project: " + getManageProjectName());
        Label label = new LabelAtom("master");
        getTopProject().setAssignedLabel(label);
        
        // Build actions...
        addSetup(getTopProject());
        // Add multi-job phases
        // Building...
        List<PhaseJobsConfig> phaseJobs = new ArrayList<>();
        
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = getBaseName() + "_" + detail.getProjectName() + "_BuildExecute";
            PhaseJobsConfig phase = new PhaseJobsConfig(name, /*jobproperties*/"", /*currParams*/true, /*configs*/null, PhaseJobsConfig.KillPhaseOnJobResultCondition.NEVER, /*disablejob*/false, /*enableretrystrategy*/false, /*parsingrulespath*/null, /*retries*/0, /*enablecondition*/false, /*abort*/false, /*condition*/"", /*buildonly if scm changes*/false);
            phaseJobs.add(phase);
        }
        MultiJobBuilder multiJobBuilder = new MultiJobBuilder("Build-Execute-Phase", phaseJobs, MultiJobBuilder.ContinuationCondition.COMPLETED);
        getTopProject().getBuildersList().add(multiJobBuilder);
        // Reporting...
        phaseJobs = new ArrayList<>();
        
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = getBaseName() + "_" + detail.getProjectName() + "_Reporting";
            PhaseJobsConfig phase = new PhaseJobsConfig(name, /*jobproperties*/"", /*currParams*/true, /*configs*/null, PhaseJobsConfig.KillPhaseOnJobResultCondition.NEVER, /*disablejob*/false, /*enableretrystrategy*/false, /*parsingrulespath*/null, /*retries*/0, /*enablecondition*/false, /*abort*/false, /*condition*/"", /*buildonly if scm changes*/false);
            phaseJobs.add(phase);
        }
        multiJobBuilder = new MultiJobBuilder("Reporting-Phase", phaseJobs, MultiJobBuilder.ContinuationCondition.COMPLETED);
        getTopProject().getBuildersList().add(multiJobBuilder);
        // Copy artifacts per building project
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = getBaseName() + "_" + detail.getProjectName() + "_BuildExecute";
            CopyArtifact copyArtifact = new CopyArtifact(name);
            copyArtifact.setOptional(true);
            copyArtifact.setFilter("**/*manage_incremental_rebuild_report.html");
            copyArtifact.setFingerprintArtifacts(false);
            BuildSelector bs = new WorkspaceSelector();
            copyArtifact.setSelector(bs);
            getTopProject().getBuildersList().add(copyArtifact);
        }
        // Copy artifacts per reporting project
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = getBaseName() + "_" + detail.getProjectName() + "_Reporting";
            CopyArtifact copyArtifact = new CopyArtifact(name);
            copyArtifact.setOptional(true);
            copyArtifact.setFilter("**/*");
            copyArtifact.setFingerprintArtifacts(false);
            BuildSelector bs = new WorkspaceSelector();
            copyArtifact.setSelector(bs);
            getTopProject().getBuildersList().add(copyArtifact);
        }
        addMultiJobBuildCommand();
                
        // Post-build actions
        addArchiveArtifacts(getTopProject());
        addXunit(getTopProject());
        addVCCoverage(getTopProject());
        addGroovyScriptMultiJob();
        
        getTopProject().save();
        
        for (MultiJobDetail detail : manageProject.getJobs()) {
            String name = getBaseName() + "_" + detail.getProjectName();
            createProjectPair(name, detail);
        }
    }
    private void addMultiJobBuildCommand() {
        String win =
"%VECTORCAST_DIR%\\vpython %WORKSPACE%\\vc_scripts\\incremental_build_report_aggregator.py --api 2 \n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=aggregate  \n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=metrics     \n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=environment \n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --full-status=@PROJECT_BASE@_full_report.html\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --full-status > @PROJECT_BASE@_full_report.txt\n" +
"%VECTORCAST_DIR%\\vpython %WORKSPACE%\\vc_scripts\\getTotals.py --api 2 @PROJECT_BASE@_full_report.txt\n" +
"           ";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@PROJECT_BASE@", getBaseName());

        String unix =
"$VECTORCAST_DIR/vpython $WORKSPACE/vc_scripts/incremental_build_report_aggregator.py --api 2 \n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=aggregate  \n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=metrics     \n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=environment\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --full-status=@PROJECT_BASE@_full_report.html\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --full-status > @PROJECT_BASE@_full_report.txt\n" +
"$VECTORCAST_DIR/vpython $WORKSPACE/vc_scripts/getTotals.py --api 2 @PROJECT_BASE@_full_report.txt\n" +
"\n" +
"          ";
        unix = StringUtils.replace(unix, "@PROJECT@", getManageProjectName());
        unix = StringUtils.replace(unix, "@PROJECT_BASE@", getBaseName());
        
        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        getTopProject().getBuildersList().add(command);
    }
    private void addGroovyScriptMultiJob() {
        String script =
"import hudson.FilePath\n" +
"\n" +
"FilePath fp_c = new FilePath(manager.build.getWorkspace(),'CombinedReport.html')\n" +
"FilePath fp_f = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_full_report.html')\n" +
"\n" +
"if (fp_c.exists() && fp_f.exists())\n" +
"{\n" +
"    manager.build.description = \"Full Status Report\"\n" +
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
    private void createProjectPair(String baseName, MultiJobDetail detail) throws IOException {
        // Building job
        FreeStyleProject p = getInstance().createProject(FreeStyleProject.class, baseName + "_BuildExecute");
        p.setScm(getTopProject().getScm());
        addDeleteWorkspaceBeforeBuildStarts(p);
        Label label = new LabelAtom(detail.getCompiler());
        p.setAssignedLabel(label);
        addSetup(p);
        addBuildCommands(p, detail, baseName);
        addPostbuildGroovy(p, detail, baseName);
        p.save();

        // Reporting job
        p = getInstance().createProject(FreeStyleProject.class, baseName + "_Reporting");
        p.setScm(getTopProject().getScm());
        addDeleteWorkspaceBeforeBuildStarts(p);
        p.setAssignedLabel(label);
        addSetup(p);
        addReportingCommands(p, detail, baseName);
        addArchiveArtifacts(p);
        addXunit(p);
        addVCCoverage(p);
        addPostReportingGroovy(p);
        p.save();
    }
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
    private void addReportingCommands(Project project, MultiJobDetail detail, String baseName) {
        String win = "%VECTORCAST_DIR%\\vpython %WORKSPACE%\\vc_scripts\\generate-results.py --api 2 \"@PROJECT@\" --level @LEVEL@ -e @ENV@ \n" +
"      ";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@LEVEL@", detail.getLevel());
        win = StringUtils.replace(win, "@ENV@", detail.getEnvironment());
        
        String unix = "$VECTORCAST_DIR/vpython $WORKSPACE/vc_scripts/generate-results.py --api 2 \"@PROJECT@\" --level @LEVEL@ -e @ENV@ \n" +
"      ";
        unix = StringUtils.replace(unix, "@PROJECT@", getManageProjectName());
        unix = StringUtils.replace(unix, "@LEVEL@", detail.getLevel());
        unix = StringUtils.replace(unix, "@ENV@", detail.getEnvironment());

        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        project.getBuildersList().add(command);
    }
    private void addBuildCommands(Project project, MultiJobDetail detail, String baseName) {
        String win = 
"set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --level @LEVEL@ -e @ENV@ --build-execute --incremental --output @BASENAME@_manage_incremental_rebuild_report.html\n" +
"\n";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@LEVEL@", detail.getLevel());
        win = StringUtils.replace(win, "@ENV@", detail.getEnvironment());
        win = StringUtils.replace(win, "@BASENAME@", baseName);
        
        String unix = 
"export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --level @LEVEL@ -e @ENV@ --build-execute --incremental --output @BASENAME@_manage_incremental_rebuild_report.html\n" +
"\n";
        unix = StringUtils.replace(unix, "@PROJECT@", getManageProjectName());
        unix = StringUtils.replace(unix, "@LEVEL@", detail.getLevel());
        unix = StringUtils.replace(unix, "@ENV@", detail.getEnvironment());
        unix = StringUtils.replace(unix, "@BASENAME@", baseName);

        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        project.getBuildersList().add(command);
    }
    private void addPostbuildGroovy(Project project, MultiJobDetail detail, String baseName) {
        String script = 
"import hudson.FilePath\n" +
"\n" +
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
"}\n" +
"\n" +
"FilePath fp_i = new FilePath(manager.build.getWorkspace(),'@BASENAME@_manage_incremental_rebuild_report.html')\n" +
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
