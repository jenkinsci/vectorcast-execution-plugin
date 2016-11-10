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
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * VectorCAST setup build action
 */
public class VectorCASTSetup extends Builder implements SimpleBuildStep {
    /** script directory */
    private static final String SCRIPT_DIR = "/scripts/";
    /**
     * Create a VectorCAST setup step
     */
    @DataBoundConstructor
    public VectorCASTSetup() {
    }
    /**
     * Copy the files in a directory recursively to the job workspace.
     * This is used when the source is NOT a jar file
     * 
     * @param dir directory to process
     * @param base base
     * @param destDir destination directory
     * @throws IOException
     * @throws InterruptedException 
     */
    private void processDir(File dir, String base, FilePath destDir) throws IOException, InterruptedException {
        destDir.mkdirs();
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                FilePath newDest = new FilePath(destDir, file.getName());
                processDir(file, base + "/" + file.getName(), newDest);
            } else {
                FilePath newFile = new FilePath(destDir, file.getName());
                try (InputStream is = VectorCASTSetup.class.getResourceAsStream(SCRIPT_DIR + base + "/" + file.getName())) {
                    newFile.copyFrom(is);
                }
            }
        }
    }
    /**
     * Perform the build step. Copy the scripts from the archive/directory to the workspace
     * @param build
     * @param workspace
     * @param launcher
     * @param listener 
     */    
    @Override
    public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) {
        FilePath destScriptDir = new FilePath(workspace, "vc_scripts");
        JarFile jFile = null;
        try {
            String path = VectorCASTSetup.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            File testPath = new File(path);
            if (testPath.isFile()) {
                // Have jar file...
                jFile = new JarFile(VectorCASTSetup.class.getProtectionDomain().getCodeSource().getLocation().getPath());
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
                File scriptDir = new File(path + SCRIPT_DIR);
                processDir(scriptDir, "./", destScriptDir);
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
}
