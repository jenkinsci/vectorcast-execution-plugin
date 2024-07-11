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

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BatchFile;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Shell;
import hudson.EnvVars;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class allows a command script to be specified for both Linux and Windows
 * and the build step will test and run the correct command
 */
public class VectorCASTCommand extends Builder implements SimpleBuildStep {

    private final String winCommand;
    private final String unixCommand;

    /**
     * Get the windows variant of the command
     * @return windows command
     */
    public final String getWinCommand() {
        return winCommand;
    }

    /**
     * Get the Unix variant of the command
     * @return unix command
     */
    public final String getUnixCommand() {
        return unixCommand;
    }
    
    /**
     * Create a VectorCAST command
     * @param winCommand the windows variant of the command
     * @param unixCommand the unix variant of the command
     */
    @DataBoundConstructor
    public VectorCASTCommand(String winCommand, String unixCommand) {
        this.winCommand = winCommand;
        this.unixCommand = unixCommand;
    }
    
    @Override
    public void perform(Run<?,?> build, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) {
        // Windows check and run batch command
        //
        // Get the windows batch command and run it if this node is Windows
        //
        if (!launcher.isUnix()) {
            String windowsCmd = getWinCommand();
            BatchFile batchFile = new BatchFile(windowsCmd);
            try {
                if (!batchFile.perform((AbstractBuild)build, launcher, (BuildListener)listener)) {
                    build.setResult(Result.FAILURE);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(VectorCASTCommand.class.getName()).log(Level.SEVERE, null, ex);
                build.setResult(Result.FAILURE);
            }
        }

        //
        // Linux check and batch command
        //        
        // Get the Linux/Unix batch command and run it if this node is not Windows
        //
        if (launcher.isUnix()) {
            String unixCmd = getUnixCommand();
            Shell shell = new Shell(unixCmd);
            try {
                if (!shell.perform((AbstractBuild)build, launcher, (BuildListener)listener)) {
                    build.setResult(Result.FAILURE);
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(VectorCASTCommand.class.getName()).log(Level.SEVERE, null, ex);
                build.setResult(Result.FAILURE);
            }
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link VectorCASTCommand}. Used as a singleton.
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
        @SuppressWarnings("rawtypes")
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
            return Messages.VectorCASTCommand_DisplayName();
        }
    }

}

