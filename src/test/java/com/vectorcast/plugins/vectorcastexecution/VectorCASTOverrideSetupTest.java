package com.vectorcast.plugins.vectorcastexecution;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.util.StreamTaskListener;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real process-environment override in an isolated Java process. */
class VectorCASTOverrideSetupTest {
    @TempDir
    Path temporary;

    @ParameterizedTest
    @ValueSource(strings = {"directory", "missing", "empty"})
    void honorsScriptOverrideWithoutChangingTheTestJvmEnvironment(String mode)
            throws Exception {
        Path source = temporary.resolve("custom scripts");
        if ("directory".equals(mode)) {
            Files.createDirectories(source.resolve("nested"));
            Files.writeString(source.resolve("nested/custom.py"), "custom script");
        }
        Path workspace = temporary.resolve("workspace");
        Files.createDirectories(workspace);
        List<String> args = new ArrayList<>();
        // Preserve JaCoCo instrumentation in the child when coverage is enabled.
        ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
            .filter(arg -> arg.startsWith("-javaagent:") && arg.contains("jacoco"))
            .forEach(args::add);
        args.add("-cp");
        args.add(System.getProperty("surefire.test.class.path",
            System.getProperty("java.class.path")));
        args.add(SetupProcess.class.getName());
        args.add(workspace.toString());
        Path arguments = temporary.resolve("java-arguments.txt");
        List<String> quoted = args.stream().map(arg -> "\""
            + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\"").toList();
        Files.write(arguments, quoted, StandardCharsets.UTF_8);
        Path output = temporary.resolve("setup-output.txt");
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path java = Path.of(System.getProperty("java.home"), "bin",
            windows ? "java.exe" : "java");
        ProcessBuilder builder = new ProcessBuilder(java.toString(), "@" + arguments);
        builder.environment().put("VCAST_VC_SCRIPTS",
            "empty".equals(mode) ? "" : source.toString());
        builder.redirectErrorStream(true).redirectOutput(output.toFile());
        Process process = builder.start();
        try {
            assertTrue(process.waitFor(45, TimeUnit.SECONDS), "setup process timed out");
            assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
        assertTrue(Files.isDirectory(workspace.resolve("vc_scripts")));
        if ("directory".equals(mode)) {
            assertEquals("custom script", Files.readString(
                workspace.resolve("vc_scripts/nested/custom.py")));
            assertFalse(Files.exists(workspace.resolve("vc_scripts/baseJenkinsfile.groovy")));
        } else if ("empty".equals(mode)) {
            assertTrue(Files.exists(workspace.resolve("vc_scripts/baseJenkinsfile.groovy")));
        } else {
            try (var files = Files.list(workspace.resolve("vc_scripts"))) {
                assertEquals(0, files.count());
            }
        }
    }

    /** Child entry point: only the real setup step runs in the altered environment. */
    public static final class SetupProcess {
        public static void main(String[] args) throws Exception {
            try (StreamTaskListener listener = new StreamTaskListener(
                    System.out, StandardCharsets.UTF_8)) {
                new VectorCASTSetup().perform(null, new FilePath(Path.of(args[0]).toFile()),
                    new EnvVars(), null, listener);
            }
        }
    }
}
