/*
 * The MIT License
 *
 * Copyright 2020 Vector Software, East Greenwich, Rhode Island USA
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
package com.vectorcast.plugins.vectorcastexecution.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.PluginWrapper;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import jenkins.model.Jenkins;

import hudson.model.Label;
import hudson.model.AutoCompletionCandidates;

import com.vectorcast.plugins.vectorcastexecution.Messages;

/** Utility class for VectorCAST. */
public class VcastUtils {

    /** Logger for plugin diagnostics. */
    private static final Logger LOGGER = Logger.getLogger(VcastUtils.class
        .getName());

    /** Plugin short name used by Jenkins' plugin manager. */
    private static final String PLUGIN_SHORT_NAME = "vectorcast-execution";

    /** Jenkins version where the monochrome icon became appropriate. */
    private static final int MONOCHROME_ICON_MAJOR = 2;

    /** Jenkins version where the monochrome icon became appropriate. */
    private static final int MONOCHROME_ICON_MINOR = 361;

    /** Permission of current view. */
    private static volatile Permission viewPermission;

   /**
     * Get the current view permissions.
     * @return Permission for current view
     */
    public static synchronized Permission getViewPermission() {
        if (viewPermission == null) {
            PermissionGroup group = new PermissionGroup(
                VcastUtils.class,
                Messages._VectorCASTRootAction_PermissionGroup()
            );

            viewPermission = new Permission(
                group,
                "View",
                Messages._VectorCASTRootAction_ViewPermissionDescription(),
                Jenkins.ADMINISTER,
                true,
                new PermissionScope[]{PermissionScope.JENKINS}
            );
        }
        return viewPermission;
    }

    /**
     * Gets the version of the plugins.
     * @return Optional returns the version
     */
    public static Optional<String> getVersion() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            PluginWrapper plugin = instance.getPluginManager().getPlugin(
                PLUGIN_SHORT_NAME);
            if (plugin != null) {
                return Optional.of(plugin.getVersion());
            }
        }

        String implementationVersion = VcastUtils.class.getPackage()
            .getImplementationVersion();
        if (implementationVersion != null) {
            return Optional.of(implementationVersion);
        }

        URL source = VcastUtils.class.getProtectionDomain().getCodeSource()
            .getLocation();
        if (!"file".equals(source.getProtocol())
                || !source.getPath().toLowerCase().endsWith(".jar")) {
            return Optional.empty();
        }
        try (JarFile archive = new JarFile(new File(source.toURI()))) {
            return Optional.ofNullable(archive.getManifest()
                .getMainAttributes().getValue("Plugin-Version"));
        } catch (IOException | URISyntaxException ex) {
            LOGGER.log(Level.FINE, "Unable to read VectorCAST plugin version",
                ex);
            return Optional.empty();
        }
    }

    /**
     * Returns the icon appropriate for the current Jenkins UI and user.
     *
     * @return icon resource path, or {@code null} when the action is hidden
     */
    public static String getVectorCASTIconFileName() {
        if (!Jenkins.get().hasPermission(getViewPermission())) {
            return null;
        }
        return supportsMonochromeIcons(Jenkins.VERSION)
            ? "/plugin/vectorcast-execution/icons/vector_favicon_bw.png"
            : "/plugin/vectorcast-execution/icons/vector_favicon.png";
    }

    private static boolean supportsMonochromeIcons(final String version) {
        String[] components = version.split("\\.");
        try {
            int major = Integer.parseInt(components[0]);
            int minor = Integer.parseInt(components[1]);
            return major > MONOCHROME_ICON_MAJOR
                || (major == MONOCHROME_ICON_MAJOR
                && minor >= MONOCHROME_ICON_MINOR);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            LOGGER.log(Level.FINE, "Unable to parse Jenkins version: {0}",
                version);
            return false;
        }
    }

    /**
     * Default constructor for subclasses.
     * @throws UnsupportedOperationException is called
     */
    protected VcastUtils() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }

    /**
     * Update the potential labels to be used.
     * @param value @QueryParameter String
     * @return AutoCompletionCandidates with the list of the potential
     *         node matches
     */
    public static AutoCompletionCandidates completeNodeLabel(
            final String value) {

        AutoCompletionCandidates c = new AutoCompletionCandidates();

        for (Label l : Jenkins.get().getLabels()) {
            if (l.getName().startsWith(value)) {               
                c.add(l.getName());
            }
        }
        return c;
    }

}
