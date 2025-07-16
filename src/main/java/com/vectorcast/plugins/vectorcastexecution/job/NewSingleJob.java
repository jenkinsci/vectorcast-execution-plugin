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
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import hudson.model.Descriptor.FormException;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.apache.commons.io.IOUtils;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Create a new single job.
 */
public class NewSingleJob extends BaseJob {
  /**
   * Constructor.
   * @param request request object
   * @param response response object
   * @throws ServletException exception
   * @throws IOException exception
   * @throws ExternalResultsFileException exception
   * @throws BadOptionComboException exception
   */
  public NewSingleJob(final StaplerRequest request,
        final StaplerResponse response)
        throws ServletException, IOException, ExternalResultsFileException,
        BadOptionComboException {
    super(request, response);
  }
  /**
   * Gets the configruation for Windows.
   * @param pluginVersion plugin version of the running plugin while create
   * @param rptFmt Report Format (HTML/TXT]
   * @param htmlOrText html or text version of the reports
   * @param noGenExecReport don't generate execution report
   * @return String of configuration for Windows
   */
  private String getWindowsConfig(
        final String pluginVersion,
        final String rptFmt,
        final String htmlOrText,
        final String noGenExecReport)
        throws IOException {

    String win = ":: Created with vectorcast-execution plugin v"
      + pluginVersion + "\n\n"
      + "set VCAST_PROJECT_NAME=" + getManageProjectName() + "\n"
      + "set VCAST_PROJECT_BASENAME=" + getBaseName() + "\n"
      + getEnvironmentSetupWin() + "\n"
      + "set VCAST_EXECUTE_PREAMBLE_WIN=" + getExecutePreambleWin() + "\n"
      + getUseCILicensesWin() + "\n"
      + getAdditonalEnvVarsWindows() + "\n"
      + "set VCAST_WAIT_TIME=" + getWaitTime() + "\n"
      + "set VCAST_WAIT_LOOPS=" + getWaitLoops() + "\n"
      + "set VCAST_OPTION_USE_REPORTING="
      +     (getOptionUseReporting() ? "TRUE" : "FALSE") + "\n"
      + "set VCAST_rptFmt=" + rptFmt + "\n"
      + "set VCAST_HTML_OR_TEXT=" + htmlOrText + "\n"
      + "set VCAST_DONT_GENERATE_EXEC_RPT=" + noGenExecReport + "\n"
      + "set VCAST_USE_CBT=--incremental"
      + "\n\n";

    InputStream in = null;

    try {
        in = getBaselineWindowsSingleFile().openStream();
        win += IOUtils.toString(in, "UTF-8");
    } catch (IOException ex) {
        Logger.getLogger(NewSingleJob.class.getName())
            .log(Level.INFO, null, ex);
        win += "Missing baseline single job script for windows";
    } finally {
        if (in != null) {
            in.close();
        }
    }

    win += getEnvironmentTeardownWin() + "\n"
      + getPclpCommand() + "\n"
      + getSquoreCommand() + "\n";

    return win;
  }

  /**
   * Gets the configruation for Unix.
   * @param pluginVersion plugin version of the running plugin while create
   * @param rptFmt Report Format (HTML/TXT]
   * @param htmlOrText html or text version of the reports
   * @param noGenExecReport don't generate execution report
   * @return String of configuration for unix
   */
  private String getUnixConfig(
        final String pluginVersion,
        final String rptFmt,
        final String htmlOrText,
        final String noGenExecReport)
        throws IOException {

    String unix = "# Created with vectorcast-execution plugin v"
      + pluginVersion + "\n\n"
      + "VCAST_PROJECT_NAME=" + getManageProjectName() + "\n"
      + "VCAST_PROJECT_BASENAME=" + getBaseName() + "\n"
      + getEnvironmentSetupUnix() + "\n"
      + "VCAST_EXECUTE_PREAMBLE_LINUX=" + getExecutePreambleUnix() + "\n"
      + getUseCILicensesUnix() + "\n"
      + getAdditonalEnvVarsLinux() + "\n"
      + "VCAST_WAIT_TIME=" + getWaitTime() + "\n"
      + "VCAST_WAIT_LOOPS=" + getWaitLoops() + "\n"
      + "VCAST_OPTION_USE_REPORTING="
      +     (getOptionUseReporting() ? "1" : "0") + "\n"
      + "VCAST_rptFmt=" + rptFmt + "\n"
      + "VCAST_HTML_OR_TEXT=" + htmlOrText + "\n"
      + "VCAST_DONT_GENERATE_EXEC_RPT=" + noGenExecReport + "\n"
      + "VCAST_USE_CBT=--incremental"
      + "\n\n";

    InputStream in = null;

    try {
        in = getBaselineLinuxSingleFile().openStream();
        unix += IOUtils.toString(in, "UTF-8");
    } catch (IOException ex) {
        Logger.getLogger(NewSingleJob.class.getName()).
            log(Level.INFO, null, ex);
        unix += "Missing baseline single job script for windows";
    } finally {
        if (in != null) {
            in.close();
        }
    }

    unix += getEnvironmentTeardownUnix()
      + getPclpCommand() + "\n"
      + getSquoreCommand() + "\n";

    return unix;
  }

  /**
   * Builds up additional environment variables for linux.
   * @return String of additional environment variables for linux
   */
  private String getAdditonalEnvVarsLinux() {
    String addEnvVars = "";

    if (getUseStrictTestcaseImport()) {
      addEnvVars += "VCAST_USE_STRICT_IMPORT=1\n";
    } else {
      addEnvVars += "VCAST_USE_STRICT_IMPORT=0\n";
    }
    if (getUseRGW3()) {
      addEnvVars += "VCAST_USE_RGW3=1\n";
    } else {
      addEnvVars += "VCAST_USE_RGW3=0\n";
    }

    if (getUseLocalImportedResults()) {
      addEnvVars += "VCAST_USE_LOCAL_IMPORTED_RESULTS=1\n";
    } else {
      addEnvVars += "VCAST_USE_LOCAL_IMPORTED_RESULTS=0\n";
    }

    if (getUseExternalImportedResults()) {
      addEnvVars += "VCAST_USE_EXTERNAL_IMPORTED_RESULTS=1\n";
      addEnvVars += "VCAST_USE_EXTERNAL_FILENAME="
        + getExternalResultsFilename() + "\n";

    } else {
      addEnvVars += "VCAST_USE_EXTERNAL_IMPORTED_RESULTS=0\n";
    }

    if (getUseImportedResults()) {
      addEnvVars += "VCAST_USE_IMPORTED_RESULTS=1\n";
    } else {
      addEnvVars += "VCAST_USE_IMPORTED_RESULTS=0\n";
    }

    if (getUseCoveragePlugin()) {
      addEnvVars += "VCAST_USE_COVERAGE_PLUGIN=1\n";
    } else {
      addEnvVars += "VCAST_USE_COVERAGE_PLUGIN=0\n";
    }
    return addEnvVars;
  }

  /**
   * Builds up additional environment variables for windows.
   * @return String of additional environment variables for windows
   */
  private String getAdditonalEnvVarsWindows() {
    String addEnvVars = "";

    if (getUseStrictTestcaseImport()) {
      addEnvVars += "set VCAST_USE_STRICT_IMPORT=TRUE\n";
    } else {
      addEnvVars += "set VCAST_USE_STRICT_IMPORT=FALSE\n";
    }

    if (getUseRGW3()) {
      addEnvVars += "set VCAST_USE_RGW3=TRUE\n";
    } else {
      addEnvVars += "set VCAST_USE_RGW3=FALSE\n";
    }

    if (getUseLocalImportedResults()) {
      addEnvVars += "set VCAST_USE_LOCAL_IMPORTED_RESULTS=TRUE\n";
    } else {
      addEnvVars += "set VCAST_USE_LOCAL_IMPORTED_RESULTS=FALSE\n";
    }

    if (getUseExternalImportedResults()) {
      addEnvVars += "set VCAST_USE_EXTERNAL_IMPORTED_RESULTS=TRUE\n";
      addEnvVars += "set VCAST_USE_EXTERNAL_FILENAME="
        + getExternalResultsFilename() + "\n";
    } else {
      addEnvVars += "set VCAST_USE_EXTERNAL_IMPORTED_RESULTS=FALSE\n";
    }

    if (getUseImportedResults()) {
      addEnvVars += "set VCAST_USE_IMPORTED_RESULTS=TRUE\n";
    } else {
      addEnvVars += "set VCAST_USE_IMPORTED_RESULTS=FALSE\n";
    }

    if (getUseCoveragePlugin()) {
      addEnvVars += "set VCAST_USE_COVERAGE_PLUGIN=TRUE\n";
    } else {
      addEnvVars += "set VCAST_USE_COVERAGE_PLUGIN=FALSE\n";
    }

    return addEnvVars;
  }

  /**
   * Add build commands step to job.
   */
  private void addCommandSingleJob() throws IOException {
    String noGenExecReport = "";
    String htmlOrText = "";
    String rptFmt = "";
    if (!getOptionExecutionReport()) {
      noGenExecReport = " --dont-gen-exec-rpt";
    }
    if (getOptionHTMLBuildDesc().equalsIgnoreCase("HTML")) {
      htmlOrText = "html";
      rptFmt = "HTML";
    } else {
      htmlOrText = "txt";
      rptFmt = "TEXT";
    }

    String pluginVersion = VcastUtils.getVersion().orElse("Unknown");

    /*
     *  Windows config portion
     */
    String win = getWindowsConfig(pluginVersion, rptFmt,
        htmlOrText, noGenExecReport);

    /*
     *  Unix config portion
     */
    String unix = getUnixConfig(pluginVersion, rptFmt,
        htmlOrText, noGenExecReport);

    VectorCASTCommand command = new VectorCASTCommand(win, unix);
    if (!getTopProject().getBuildersList().add(command)) {
      throw new UnsupportedOperationException(
        "Failed to add VectorCASTCommand to Builders List"
      );
    }
  }
  /**
   * Add groovy script step to job.
   */
  private void addGroovyScriptSingleJob() throws IOException {

    InputStream in = null;

    String script = "";

    try {
        in = getBaselinePostBuildGroovyScript().openStream();
        script += IOUtils.toString(in, "UTF-8");
    } catch (IOException ex) {
        Logger.getLogger(NewSingleJob.class.getName())
            .log(Level.INFO, null, ex);
        script += "Missing baseline single job script for windows";
    } finally {
        if (in != null) {
            in.close();
        }
    }

    script = script.replace("@PROJECT_BASE@", getBaseName());

    SecureGroovyScript secureScript = null;

    try {
      secureScript =
          new SecureGroovyScript(
              script,
              false, /*sandbox*/
              null /*classpath*/
          );
    } catch (FormException ex) {
      Logger.getLogger(NewSingleJob.class.getName()).
        log(Level.INFO, null, ex);
    }
    GroovyPostbuildRecorder groovy =
        new GroovyPostbuildRecorder(
            secureScript,
            getOptionErrorLevel(), /*behaviour*/
            false  /*matrix parent*/
        );
    if (!getTopProject().getPublishersList().add(groovy)) {
      throw new UnsupportedOperationException(
        "Failed to add GroovyPostbuildRecorder to Publishers List"
      );
    }
  }
  /**
   * Create project.
   * @return project
   * @throws IOException exception
   * @throws JobAlreadyExistsException exception
   */
  @Override
  protected Project<?, ?> createProject()
        throws IOException, JobAlreadyExistsException {
    if (getBaseName().isEmpty()) {
      getResponse().sendError(HttpServletResponse.SC_NOT_MODIFIED,
        "No project name specified");
      return null;
    }
    String projectName = getBaseName() + ".vcast.single";
    if (getJobName() != null && !getJobName().isEmpty()) {
      projectName = getJobName();
    }
    if (getInstance().getJobNames().contains(projectName)) {
      throw new JobAlreadyExistsException(projectName);
    }
    Project<?, ?> project = getInstance()
        .createProject(FreeStyleProject.class, projectName);

    setProjectName(projectName);

    if (getNodeLabel() != null && !getNodeLabel().isEmpty()) {
      Label label = new LabelAtom(getNodeLabel());
      project.setAssignedLabel(label);
    }
    return project;
  }
  /**
   * Add build steps.
   * @throws IOException exception
   * @throws ServletException exception
   * @throws hudson.model.Descriptor.FormException exception
   */
  @Override
  @RequirePOST
  public void doCreate()
        throws IOException, ServletException, Descriptor.FormException {
    getTopProject().setDescription("Single job to run the manage project: "
        + getManageProjectName());

    // Build actions...
    if (getUseImportedResults() && getUseLocalImportedResults()) {
        addCopyResultsToImport(getTopProject());
    }
    addSetup(getTopProject());
    addCommandSingleJob();

    addArchiveArtifacts(getTopProject());

    // Post-build actions - only is using reporting
    if (getOptionUseReporting()) {
      addPCLintPlus(getTopProject());
      addJunit(getTopProject());
      if (getUseCoveragePlugin()) {
        addReferenceBuild(getTopProject());
        addJenkinsCoverage(getTopProject());
      } else {
        addVCCoverage(getTopProject());
      }
    }
    addGroovyScriptSingleJob();

    getTopProject().save();
  }

  /**
   * throw error if cleanupProject is called.
   * @throws UnsupportedOperationException cleanupProject Not supported
   */
  @Override
  protected void cleanupProject() {
     //To change body of generated methods, choose Tools | Templates.
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
