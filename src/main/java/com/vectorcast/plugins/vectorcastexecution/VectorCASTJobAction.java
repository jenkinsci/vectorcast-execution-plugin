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
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.vectorcast.plugins.vectorcastexecution;

import com.vectorcast.plugins.vectorcastcoverage.VectorCASTHealthReportThresholds;
import com.vectorcast.plugins.vectorcastcoverage.VectorCASTPublisher;
import com.vectorcast.plugins.vectorcastcoverage.portlet.bean.VectorCASTCoverageResultSummary;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.RootAction;
import hudson.plugins.ws_cleanup.PreBuildCleanup;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.tasks.ArtifactArchiver;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.lib.dtkit.type.TestType;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.xunit.XUnitPublisher;
import org.jenkinsci.plugins.xunit.threshold.XUnitThreshold;
import org.jenkinsci.plugins.xunit.types.CheckType;
import org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildRecorder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Add a new action that will add the VectorCAST create job
 */
@Extension
public final class VectorCASTJobAction /*extends DummyCreateProject*/ implements RootAction, Describable<VectorCASTJobAction> {

    private static final Logger LOG = Logger.getLogger(VectorCASTJobAction.class.getName());
    private static final String JOBNAME = "VectorCAST Create Jobs from Manage Project";
    private static final String JOBCFG = "vc-job-config.xml";
  
    private boolean exists = false;

    private SCM scm;
    
    public VectorCASTJobAction() throws IOException {
        scm = new NullSCM();
    }
    public SCM getTheScm() {
        return scm;
    }
    public void setTheScm(SCM scm) {
        this.scm = scm;
    }

    public boolean isExists() {
//        checkForJobs();
//        return exists;
        return false;
    }
    
    /**
     * Check if the 'VectorCAST create job' already exists
     */
    private void checkForJobs() {
        Jenkins instance = Jenkins.getInstance();
        if (instance != null) {
            exists = false;
            Collection<String> jobs = instance.getJobNames();
            for (String job : jobs) {
                if (job.equals(JOBNAME)) {
                    exists = true;
                    break;
                }
            }
        }
    }

    private void addSetup(FreeStyleProject project) {
        VectorCASTSetup setup = new VectorCASTSetup();
        project.getBuildersList().add(setup);
    }
    private void addCommand(FreeStyleProject project) {
        String win = "set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"%VECTORCAST_DIR%\\manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --status\n" +
"%VECTORCAST_DIR%\\manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --release-locks\n" +
" %VECTORCAST_DIR%\\manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --build-execute --incremental --output jenkinsDemo_manage_incremental_rebuild_report.html \n" +
"\n" +
"  \n" +
"%VECTORCAST_DIR%\\vpython %WORKSPACE%\\vc_scripts\\generate-results.py --api 2 C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo \n" +
"%VECTORCAST_DIR%\\manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --create-report=aggregate  \n" +
"%VECTORCAST_DIR%\\manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --create-report=metrics     \n" +
"%VECTORCAST_DIR%\\manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --create-report=environment \n" +
"%VECTORCAST_DIR%\\manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --full-status=jenkinsDemo_full_report.html\n" +
"%VECTORCAST_DIR%\\manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --full-status > jenkinsDemo_full_report.txt\n" +
"%VECTORCAST_DIR%\\vpython %WORKSPACE%\\vc_scripts\\getTotals.py --api 2 jenkinsDemo_full_report.txt";
        String unix = "export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\n" +
"$VECTORCAST_DIR/manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --status \n" +
"$VECTORCAST_DIR/manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --release-locks \n" +
" $VECTORCAST_DIR/manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --build-execute --incremental --output jenkinsDemo_manage_incremental_rebuild_report.html\n" +
"\n" +
"  \n" +
"$VECTORCAST_DIR/vpython $WORKSPACE/vc_scripts/generate-results.py --api 2 C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo \n" +
"$VECTORCAST_DIR/manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --create-report=aggregate   --output=jenkinsDemo_aggregate_report.html\n" +
"$VECTORCAST_DIR/manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --create-report=metrics     --output=jenkinsDemo_metrics_report.html\n" +
"$VECTORCAST_DIR/manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --create-report=environment --output=jenkinsDemo_environment_report.html\n" +
"$VECTORCAST_DIR/manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --full-status=jenkinsDemo_full_report.html\n" +
"$VECTORCAST_DIR/manage --project C:\\Work\\Jenkins_VC\\vcast\\jenkinsDemo --full-status > jenkinsDemo_full_report.txt\n" +
"$VECTORCAST_DIR/vpython $WORKSPACE/vc_scripts/getTotals.py --api 2 jenkinsDemo_full_report.txt";
        VectorCASTCommand command = new VectorCASTCommand(win, unix);
        project.getBuildersList().add(command);
    }

    private void addArchiveArtifacts(FreeStyleProject project) {
        ArtifactArchiver archiver = new ArtifactArchiver(/*artifacts*/"**/*", /*excludes*/"", /*latest only*/false, /*allow empty archive*/false);
        project.getPublishersList().add(archiver);
    }
    
    private void addXunit(FreeStyleProject project) {
        XUnitThreshold[] thresholds = null;
        CheckType checkType = new CheckType("**/test_results_*.xml", /*skipNoTestFiles*/true, /*failIfNotNew*/true, /*deleteOpFiles*/true, /*StopProcIfErrot*/true);
        TestType[] testTypes = new TestType[1];
        testTypes[0] = checkType;
        XUnitPublisher xunit = new XUnitPublisher(testTypes, thresholds);
        project.getPublishersList().add(xunit);
    }
    
    private void addVCCoverage(FreeStyleProject project) {
        VectorCASTHealthReportThresholds healthReports = new VectorCASTHealthReportThresholds(0, 100, 0, 70, 0, 80, 0, 80, 0, 80, 0, 80);
        VectorCASTPublisher publisher = new VectorCASTPublisher();
        publisher.includes = "**/coverage_results_*.xml";
        publisher.healthReports = healthReports;
        project.getPublishersList().add(publisher);
    }
    
    private void addGroovyScript(FreeStyleProject project) {
        String script = "import hudson.FilePath\n" +
" \n" +
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
"FilePath fp_i = new FilePath(manager.build.getWorkspace(),'jenkinsDemo_manage_incremental_rebuild_report.html')\n" +
"FilePath fp_f = new FilePath(manager.build.getWorkspace(),'jenkinsDemo_full_report.html')\n" +
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
        SecureGroovyScript secureScript = new SecureGroovyScript(script, /*sandbox*/false, /*classpath*/null);
        GroovyPostbuildRecorder groovy = new GroovyPostbuildRecorder(secureScript, /*behaviour*/2, /*matrix parent*/false);
        project.getPublishersList().add(groovy);
    }
    
    public void doCreate(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException, Descriptor.FormException {
        Jenkins instance = Jenkins.getInstance();

        // Read the manage project file
        String file = IOUtils.toString(request.getFileItem("manageProject").getInputStream());

        String projectName = "single.job";
        // Create the top-level (or for single job, the only) project        
        FreeStyleProject topProject;
        topProject = instance.createProject(FreeStyleProject.class, projectName);
        // Read the SCM setup
        topProject.doConfigSubmit(request, response);
        
        PreBuildCleanup cleanup = new PreBuildCleanup(/*patterns*/null, true, /*cleanup param*/"", /*external delete*/"");
        topProject.getBuildWrappersList().add(cleanup);

        // Build actions...
        addSetup(topProject);
        addCommand(topProject);
                
        // Post-build actions
        addArchiveArtifacts(topProject);
        addXunit(topProject);
        addVCCoverage(topProject);
        addGroovyScript(topProject);
        
        topProject.save();
        
//        AbstractProject project;
//        project = instance.createProject(FreeStyleProject.class, "Dummy Job2");
//        project.setScm(topProject.getScm());
//        project.save();
    }
    
    /**
     * Old - no longer used
     */
    public void doCreateOLD(final StaplerRequest request, final StaplerResponse response) throws ServletException, IOException, Descriptor.FormException {
        Jenkins instance = Jenkins.getInstance();
        
        if (instance != null) {
            Collection<String> jobs = instance.getJobNames();
            boolean add = true;
            for (String job : jobs) {
                if (job.equals(JOBNAME)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                try {
                    LOG.log(Level.INFO, "Add " + JOBNAME);
                    InputStream is = VectorCASTJobAction.class.getResourceAsStream("/" + JOBCFG);
                    if (is == null) {
                        LOG.log(Level.SEVERE, "Error creating job, corrupt plugin/installation");
                    } else {
                        instance.createProjectFromXML(JOBNAME, is);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(VectorCASTJobAction.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
      response.forwardToPreviousPage(request);
    }

    public String getRootUrl() {
        return Jenkins.getInstance().getRootUrl();
    }

    @Override
    public String getDisplayName() {
        return Messages.VectorCASTCommand_AddVCJob();
    }

    @Override
    public String getIconFileName() {
        // Only display if user has admin rights
        if (Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return "/plugin/vectorcast-execution/icons/vector_favicon.png";
        } else {
            return null;
        }
    }

    @Override
    public String getUrlName() {
        return "/createVCJob";
    }

    @Override
    public Descriptor<VectorCASTJobAction> getDescriptor() {
        Jenkins jenkins = Jenkins.getActiveInstance();
        if (jenkins == null) {
            throw new IllegalStateException("Jenkins not running properly");
        }
        return jenkins.getDescriptorOrDie(getClass());
    }
  
    @Extension
    public static final class JobImportActionDescriptor extends Descriptor<VectorCASTJobAction> {

        @Override
        public String getDisplayName() { return ""; }

    }
}
