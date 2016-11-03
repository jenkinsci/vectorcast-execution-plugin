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

import com.vectorcast.plugins.vectorcastexecution.VectorCASTCommand;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 */
public class NewSingleJob extends BaseJob {

    private static final Logger LOG = Logger.getLogger(NewSingleJob.class.getName());

    
    public NewSingleJob(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException {
        super(request, response);
    }
    
    private void addCommandSingleJob() {
        
        String win = 
"set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --status\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --release-locks\n" +
" %VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --build-execute --incremental --output \"@PROJECT_BASE@_manage_incremental_rebuild_report.html\" \n" +
"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\generate-results.py\" --api 2 \"@PROJECT@\" \n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=aggregate  \n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=metrics     \n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --create-report=environment \n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --full-status=\"@PROJECT_BASE@_full_report.html\"\n" +
"%VECTORCAST_DIR%\\manage --project \"@PROJECT@\" --full-status > \"@PROJECT_BASE@_full_report.txt\"\n" +
"%VECTORCAST_DIR%\\vpython \"%WORKSPACE%\\vc_scripts\\getTotals.py\" --api 2 \"@PROJECT_BASE@_full_report.txt\"";
        win = StringUtils.replace(win, "@PROJECT@", getManageProjectName());
        win = StringUtils.replace(win, "@PROJECT_BASE@", getBaseName());
        
        String unix = 
"export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --status \n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --release-locks \n" +
" $VECTORCAST_DIR/manage --project \"@PROJECT@\" --build-execute --incremental --output \"@PROJECT_BASE@_manage_incremental_rebuild_report.html\"\n" +
"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/generate-results.py\" --api 2 \"@PROJECT@\" \n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=aggregate   --output=\"@PROJECT_BASE@_aggregate_report.html\"\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=metrics     --output=\"@PROJECT_BASE@_metrics_report.html\"\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --create-report=environment --output=\"@PROJECT_BASE@_environment_report.html\"\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --full-status=\"@PROJECT_BASE@_full_report.html\"\n" +
"$VECTORCAST_DIR/manage --project \"@PROJECT@\" --full-status > \"@PROJECT_BASE@_full_report.txt\"\n" +
"$VECTORCAST_DIR/vpython \"$WORKSPACE/vc_scripts/getTotals.py\" --api 2 \"@PROJECT_BASE@_full_report.txt\"";
        unix = StringUtils.replace(unix, "@PROJECT@", getManageProjectName());
        unix = StringUtils.replace(unix, "@PROJECT_BASE@", getBaseName());
        
        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        getTopProject().getBuildersList().add(command);
    }

    private void addGroovyScriptSingleJob() {
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
"FilePath fp_i = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_manage_incremental_rebuild_report.html')\n" +
"FilePath fp_f = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_full_report.html')\n" +
"if (fp_i.exists() && fp_f.exists())\n" +
"{\n" +
"    manager.build.description = \"Full Status Report\"\n" +
"}\n" +
"else\n" +
"{\n" +
"    manager.createSummary(\"warning.gif\").appendText(\"General Failure\", false, false, false, \"red\")\n" +
"    manager.buildUnstable()\n" +
"    manager.build.description = \"General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\"\n" +
"    manager.addBadge(\"warning.gif\", \"General Error\")\n" +
"}";
        script = StringUtils.replace(script, "@PROJECT_BASE@", getBaseName());
        script = StringUtils.replace(script, "@PROJECT@", getManageProjectName());
        
        SecureGroovyScript secureScript = new SecureGroovyScript(script, /*sandbox*/false, /*classpath*/null);
        GroovyPostbuildRecorder groovy = new GroovyPostbuildRecorder(secureScript, /*behaviour*/2, /*matrix parent*/false);
        getTopProject().getPublishersList().add(groovy);
    }
    @Override
    protected Project createProject() throws IOException {
        String projectName = getBaseName() + ".vcast_manage.singlejob";
        if (getInstance().getJobNames().contains(projectName)) {
            getResponse().sendError(HttpServletResponse.SC_NOT_MODIFIED, "Project already exists");
            return null;
        }
        return getInstance().createProject(FreeStyleProject.class, projectName);
    }
    @Override
    public void doCreate() throws IOException, ServletException, Descriptor.FormException {
        getTopProject().setDescription("Single job to run the manage project: " + getManageProjectName());

        // Build actions...
        addSetup(getTopProject());
        addCommandSingleJob();
                
        // Post-build actions
        addArchiveArtifacts(getTopProject());
        addXunit(getTopProject());
        addVCCoverage(getTopProject());
        addGroovyScriptSingleJob();
        
        getTopProject().save();
    }
}
