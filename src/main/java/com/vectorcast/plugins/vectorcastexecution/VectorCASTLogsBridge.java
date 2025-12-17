package com.vectorcast.plugins.vectorcastexecution;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Bridge object returned from the {@code VectorCASTUtils} {@link org.jenkinsci.plugins.workflow.cps.GlobalVariable}.
 *
 * <p>This bridge exists to keep the Pipeline-facing API stable while allowing the implementation to live in
 * a Groovy source file bundled with the plugin. The key reason for this approach is to avoid invoking
 * Pipeline steps like {@code load} / {@code writeFile} from Java, which can behave unpredictably because
 * Java code is not CPS-transformed.</p>
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Reads the bundled Groovy source from {@link #DEFAULT_RESOURCE_PATH}.</li>
 *   <li>Compiles it using a {@link GroovyClassLoader}.</li>
 *   <li>Loads the class {@link #IMPL_CLASS_NAME} and instantiates it with the Pipeline {@link CpsScript}.</li>
 *   <li>Forwards method calls to that Groovy object.</li>
 * </ol>
 *
 * <p><b>Contract:</b> The bundled Groovy file must define the class
 * {@code com.vectorcast.plugins.vectorcastexecution.VectorCASTUtilsImpl} with a constructor
 * that accepts a single {@code script} argument (the Pipeline script), and must implement:</p>
 * <ul>
 *   <li>{@code checkLogsForErrors(VC, log)}</li>
 *   <li>{@code checkBuildLogForErrors(VC, logFile)}</li>
 * </ul>
 *
 * <p><b>Note:</b> This class does not currently support a repo/workspace override (e.g. loading a Groovy file
 * from {@code $WORKSPACE}). If/when you add that, do it from Groovy/CPS code, not from Java.</p>
 */
public class VectorCASTUtilsBridge implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Path (classpath resource) to the plugin-bundled Groovy implementation.
     *
     * <p>Place the file at:</p>
     * <pre>
     * src/main/resources/com/vectorcast/plugins/vectorcastexecution/default-scripts/VectorCASTUtils.groovy
     * </pre>
     */
    public static final String DEFAULT_RESOURCE_PATH =
            "/com/vectorcast/plugins/vectorcastexecution/default-scripts/VectorCASTUtils.groovy";

    /**
     * Fully qualified class name that must be defined in the bundled Groovy.
     */
    public static final String IMPL_CLASS_NAME =
            "com.vectorcast.plugins.vectorcastexecution.VectorCASTUtilsImpl";

    /**
     * The Pipeline script context (CPS). Used by the Groovy implementation to call Pipeline steps
     * (e.g., {@code sh}, {@code bat}, {@code writeFile}, {@code readFile}).
     */
    private final CpsScript script;

    /**
     * Cached Groovy implementation instance.
     *
     * <p>Marked {@code transient} so it can be recreated after Pipeline resume/restart/serialization.</p>
     */
    private transient GroovyObject delegate;

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script the Pipeline script context (never null)
     */
    public VectorCASTUtilsBridge(final CpsScript script) {
        this.script = Objects.requireNonNull(script, "script");
    }

    /**
     * Forwards to the Groovy implementation: {@code checkLogsForErrors(VC, log)}.
     *
     * @param VC  a user-defined map/object holding configuration (e.g. failure/unstable phrases)
     * @param log log text to scan
     * @return typically a 3-element list: {@code [foundKeywordsCsv, failureFlag, unstableFlag]}
     */
    public Object checkLogsForErrors(final Object VC, final String log) {
        return getDelegate().invokeMethod("checkLogsForErrors", new Object[]{VC, log});
    }

    /**
     * Forwards to the Groovy implementation: {@code checkBuildLogForErrors(VC, logFile)}.
     *
     * @param VC      a user-defined map/object holding configuration (e.g. failure/unstable phrases)
     * @param logFile path to the build log file (relative or absolute as your Groovy expects)
     * @return typically a 3-element list: {@code [foundKeywordsCsv, failureFlag, unstableFlag]}
     */
    public Object checkBuildLogForErrors(final Object VC, final String logFile) {
        return getDelegate().invokeMethod("checkBuildLogForErrors", new Object[]{VC, logFile});
    }

    /**
     * Lazily creates (or returns) the Groovy implementation instance.
     *
     * <p>This method compiles the bundled Groovy source and instantiates {@link #IMPL_CLASS_NAME}
     * with a single-argument constructor: {@code new VectorCASTUtilsImpl(script)}.</p>
     *
     * @return the Groovy implementation as a {@link GroovyObject}
     * @throws IllegalStateException if the bundled Groovy is missing, fails to compile, or does not define
     *                               the required class/constructor
     */
    private GroovyObject getDelegate() {
        if (delegate != null) {
            return delegate;
        }

        final String groovySource = readBundledGroovy(DEFAULT_RESOURCE_PATH);

        // Compile the Groovy source and load the implementation class.
        final GroovyClassLoader gcl = new GroovyClassLoader(getClass().getClassLoader());
        try {
            gcl.parseClass(groovySource, "VectorCASTUtils.groovy");

            final Class<?> implClass = gcl.loadClass(IMPL_CLASS_NAME);
            final Object impl = implClass.getConstructor(Object.class).newInstance(script);

            delegate = (GroovyObject) impl;
            return delegate;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to compile/instantiate " + IMPL_CLASS_NAME + " from " + DEFAULT_RESOURCE_PATH, e);
        }
    }

    /**
     * Reads a UTF-8 text resource from the plugin classpath.
     *
     * @param resourcePath classpath path (must start with {@code /})
     * @return the full text content of the resource
     * @throws IllegalStateException if the resource is missing or unreadable
     */
    private String readBundledGroovy(final String resourcePath) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        getClass().getResourceAsStream(resourcePath),
                        "Missing resource: " + resourcePath
                ),
                StandardCharsets.UTF_8
        ))) {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed reading bundled Groovy: " + resourcePath, e);
        }
    }
}
