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
    /** Wait loops */
    private Long waitLoops;
    /** Wait time */
    private Long waitTime;
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
     * @param waitLoops wait loops
     * @param waitTime wait time
     * @param manageProjectName manage project name
     * @param jobName job name
     * @param nodeLabel node label
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
                           Long waitLoops,
                           Long waitTime,
                           String manageProjectName,
                           String jobName,
                           String nodeLabel) {
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
        this.usingSCM = false;
        this.scm = new NullSCM();
        this.waitLoops = waitLoops;
        this.waitTime = waitTime;
        this.manageProjectName = manageProjectName;
        this.jobName = jobName;
        this.nodeLabel = nodeLabel;
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
    /**
     * Perform the build step. Copy the scripts from the archive/directory to the workspace
     * @param build build
     * @param workspace workspace
     * @param launcher launcher
     * @param listener  listener
     */    
    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
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
                    // Ignore
                }
            }
        }
        
        // clean up old xml_data files
        
        logger.log(Level.INFO, "Cleaning up old xml_data files");
        File[] files = new File(workspace + "/xml_data/").listFiles();
        if (files != null)
        {
            for (File file : files) 
            {
                if (file.isFile()) 
                {
                    file.delete();
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
    private static final Logger logger = Logger.getLogger(VectorCASTSetup.class.getName());
}
