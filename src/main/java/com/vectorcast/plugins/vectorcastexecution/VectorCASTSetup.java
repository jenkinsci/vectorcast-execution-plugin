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

import com.vectorcast.plugins.vectorcastexecution.common.VcastUtils;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.EnvVars;

/**
 * VectorCAST setup build action.
 */
public class VectorCASTSetup extends Builder implements SimpleBuildStep {
    /** script directory. */
    private static final String SCRIPT_DIR = "/scripts/";


    /**
     * Default constructor.
     *
     */

    @DataBoundConstructor
    public VectorCASTSetup() {
    }

    /**
     * Copy the files in a directory recursively to the job workspace.
     * This is used when the source is NOT a jar file.
     *
     * @param scriptDir directory to process
     * @param base base
     * @param destDir destination directory
     * @param directDir direct dir to copy
     * @throws IOException exception
     * @throws InterruptedException exception
     */

    private void processDir(final File scriptDir,
            final String base,
            final FilePath destDir,
            final Boolean directDir) throws IOException, InterruptedException {

        destDir.mkdirs();
        File[] files = scriptDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {

            if (file.isDirectory()) {
                FilePath newDest = new FilePath(destDir, file.getName());
                processDir(file,
                    base + "/" + file.getName(),
                    newDest, directDir);
            } else {
                if (directDir) {

                    // change the copy mechanism
                    // to copy file to remote node
                    File newFile = new File(destDir
                        + File.separator
                        + file.getName());
                    File inFile = new File(scriptDir
                        + File.separator
                        + file.getName());

                    FilePath dest = new FilePath(destDir, newFile.getName());
                    InputStream is = null;
                    try {
                        is = new FileInputStream(inFile);
                        dest.copyFrom(is);
                    } finally {

                        if (is != null) {
                            is.close();
                        }
                    }
                } else {
                    FilePath newFile = new FilePath(destDir, file.getName());
                    try (InputStream is = VectorCASTSetup.class.
                            getResourceAsStream(SCRIPT_DIR
                                + base
                                + "/"
                                + file.getName())) {
                        newFile.copyFrom(is);
                    }
                }
            }
        }
    }

    /**
     * Prints the version of the plugin that's being used.
     * @param logger - where to log the info
     */
    private void printVersion(final PrintStream logger) {
        logger.println("[VectorCAST Execution Version]: "
            + VcastUtils.getVersion().
            orElse("Error - Could not determine version"));
    }

    /**
     * Perform the build step. Copy the scripts from the
     * archive/directory to the workspace.
     * @param build build
     * @param workspace workspace
     * @param env environment variables
     * @param launcher launcher
     * @param listener  listener
     * @throws IOException exception
     */
    @Override
    public void perform(final Run<?, ?> build, final FilePath workspace,
            final EnvVars env, final Launcher launcher,
            final TaskListener listener) throws IOException {

        final int initPathLen = 8;
        FilePath destScriptDir = new FilePath(workspace, "vc_scripts");
        JarFile jFile = null;
        try {
            String path = null;
            String overridePath = System.getenv("VCAST_VC_SCRIPTS");
            String extraScriptPath = SCRIPT_DIR;
            Boolean directDir = false;
            if (overridePath != null && !overridePath.isEmpty()) {
                path = overridePath;
                extraScriptPath = "";
                directDir = true;
                String msg = " "
                    + "VectorCAST - overriding vc_scripts. Copying from '"
                    + path + "'";
                Logger.
                    getLogger(
                        VectorCASTSetup.class.getName()).log(Level.ALL, msg);
            } else {
                path = VectorCASTSetup.class.getProtectionDomain().
                    getCodeSource().getLocation().getPath();
                path = URLDecoder.decode(path, "utf-8");
            }
            File testPath = new File(path);
            printVersion(listener.getLogger());
            if (testPath.isFile()) {
                // Have jar file...
                jFile = new JarFile(testPath);
                Enumeration<JarEntry> entries = jFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith("scripts")) {

                        String fileOrDir =
                            entry.getName().substring(initPathLen);

                        /* check to solve jenkins security scanner */
                        File destDir  = new File(destScriptDir.getName());
                        File destFile = new File(destDir, fileOrDir);
                        if (!destFile.toPath().normalize().
                                startsWith(destDir.toPath())) {
                            throw new IOException("Bad entry in scripts.jar: "
                                + entry.getName());
                        }

                        FilePath dest = new FilePath(destScriptDir, fileOrDir);
                        if (entry.getName().endsWith("/")) {
                            // Directory, create destination
                            dest.mkdirs();
                        } else {

                            String destString = "/" + fileOrDir;
                            /* check to solve jenkins security scanner */
                            destDir  = new File(destScriptDir.getName());
                            destFile = new File(destDir, destString);
                            if (!destFile.toPath().normalize().
                                    startsWith(destDir.toPath())) {
                                throw new IOException(""
                                    + "Bad entry in scripts.jar: "
                                    + entry.getName());
                            }

                            // File, copy it
                            InputStream is = VectorCASTSetup.class.
                                getResourceAsStream("/" + entry.getName());
                            dest.copyFrom(is);
                        }
                    }
                }
            } else {
                // Have directory
                File scriptDir = new File(path + extraScriptPath);
                processDir(scriptDir, "./", destScriptDir, directDir);
            }
        } catch (IOException ex) {
            Logger.getLogger(VectorCASTSetup.class.getName()).
                log(Level.INFO, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(VectorCASTSetup.class.getName()).
                log(Level.INFO, null, ex);
        } finally {
            if (jFile != null) {
                try {
                    jFile.close();
                } catch (IOException ex) {
                    assert true;
                }
            }
        }

        // clean up old xml_data files

        File[] files = new File(workspace + "/xml_data/").listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && !file.delete()) {
                    throw new IOException("Unable to delete file: "
                        + file.getAbsolutePath());
                }
            }
        }
    }
    /**
     * getDescriptor for {@link VectorCASTSetup}.
     * @return DescriptorImpl descriptor
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    /**
     * Descriptor for {@link VectorCASTSetup}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Builder> {
        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }
        /**
         * Override call to isApplicable.
         * @param aClass AbstractProject Project Class
         * @return boolean always true
         * call load() in the constructor.
         */
        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(
            final Class<? extends AbstractProject> aClass) {
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
