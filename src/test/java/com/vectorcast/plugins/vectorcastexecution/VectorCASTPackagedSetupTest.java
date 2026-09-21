package com.vectorcast.plugins.vectorcastexecution;

import hudson.EnvVars;
import com.vectorcast.plugins.vectorcastexecution.common.VcastUtils;
import hudson.FilePath;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import jenkins.tasks.SimpleBuildStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Loads the real setup class from a small JAR to exercise deployed packaging. */
class VectorCASTPackagedSetupTest {
    @TempDir
    Path temporary;

    @Test
    void copiesNestedPackagedScriptsAndRemovesOnlyTopLevelOldResults()
            throws Exception {
        Path workspace = temporary.resolve("workspace");
        Files.createDirectories(workspace.resolve("xml_data/nested"));
        Files.writeString(workspace.resolve("xml_data/old.xml"), "old result");
        Files.writeString(workspace.resolve("xml_data/nested/keep.xml"), "keep");
        runPackagedSetup(workspace, List.of("scripts/", "scripts/nested/",
            "scripts/nested/example.py", "unrelated.txt"));
        assertEquals("fixture contents", Files.readString(
            workspace.resolve("vc_scripts/nested/example.py")));
        assertFalse(Files.exists(workspace.resolve("vc_scripts/unrelated.txt")));
        assertFalse(Files.exists(workspace.resolve("xml_data/old.xml")));
        assertEquals("keep", Files.readString(
            workspace.resolve("xml_data/nested/keep.xml")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"scripts/../escaped.txt", "scripts/..\\escaped.txt"})
    void refusesTraversalEntries(String entry) throws Exception {
        Path workspace = temporary.resolve("workspace");
        Files.createDirectories(workspace);
        runPackagedSetup(workspace, List.of(entry));
        assertFalse(Files.exists(workspace.resolve("escaped.txt")));
        assertFalse(Files.exists(temporary.resolve("escaped.txt")));
        assertFalse(Files.exists(workspace.resolve("vc_scripts")));
    }

    private void runPackagedSetup(Path workspace, List<String> entries)
            throws Exception {
        String override = System.getenv("VCAST_VC_SCRIPTS");
        assumeTrue(override == null || override.isEmpty(),
            "Packaged-script tests require no script-directory override");
        String className = VectorCASTSetup.class.getName();
        String utilsName = VcastUtils.class.getName();
        Path jar = temporary.resolve("setup fixture.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Plugin-Version", "0.81-test");
        try (JarOutputStream output = new JarOutputStream(
                Files.newOutputStream(jar), manifest)) {
            for (String bundledClass : List.of(className, utilsName)) {
                String classEntry = bundledClass.replace('.', '/') + ".class";
                output.putNextEntry(new JarEntry(classEntry));
                try (InputStream source = VectorCASTSetup.class
                        .getResourceAsStream("/" + classEntry)) {
                    source.transferTo(output);
                }
                output.closeEntry();
            }
            for (String entry : entries) {
                output.putNextEntry(new JarEntry(entry));
                if (!entry.endsWith("/")) {
                    output.write("fixture contents".getBytes(StandardCharsets.UTF_8));
                }
                output.closeEntry();
            }
        }
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        // Isolate plugin classes under test; Jenkins types retain their identity.
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{jar.toUri().toURL()}, VectorCASTSetup.class.getClassLoader()) {
                @Override
                protected Class<?> loadClass(String name, boolean resolve)
                        throws ClassNotFoundException {
                    if (!className.equals(name) && !utilsName.equals(name)) {
                        return super.loadClass(name, resolve);
                    }
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = findClass(name);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            };
                StreamTaskListener listener = new StreamTaskListener(
                    log, StandardCharsets.UTF_8)) {
            Class<?> type = loader.loadClass(className);
            assertTrue(type.getProtectionDomain().getCodeSource()
                .getLocation().toString().endsWith("setup%20fixture.jar"));
            SimpleBuildStep setup = (SimpleBuildStep) type.getConstructor().newInstance();
            setup.perform(null, new FilePath(workspace.toFile()), new EnvVars(),
                null, listener);
        }
        assertTrue(log.toString(StandardCharsets.UTF_8)
            .contains("[VectorCAST Execution Version]: 0.81-test"));
    }
}
