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
 * Base bridge for exposing a Pipeline global backed by a Groovy implementation bundled with the plugin.
 *
 * <p>This bridge avoids calling Pipeline steps (e.g. {@code load}, {@code writeFile}) from Java because Java
 * code is not CPS-transformed. Instead, it compiles a Groovy source resource and instantiates a known class.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>The bundled Groovy resource must be readable from {@code resourcePath}.</li>
 *   <li>The bundled Groovy must define {@code implClassName}.</li>
 *   <li>The impl class must have a 1-arg constructor accepting the Pipeline {@link CpsScript} (as {@code Object}).</li>
 * </ul>
 *
 * <p>Subclasses typically only define constants for the Groovy resource path and implementation class name,
 * then provide typed wrapper methods that forward to the Groovy delegate.</p>
 */
public abstract class VectorCASTGroovyBridge implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Pipeline script context (CPS). Passed to the Groovy implementation. */
    private final CpsScript script;

    /** Classpath resource containing the Groovy implementation. Must start with '/'. */
    private final String resourcePath;

    /** Fully-qualified class name implemented by the Groovy source. */
    private final String implClassName;

    /** Cached Groovy implementation instance (recreated after Pipeline resume). */
    private transient GroovyObject delegate;

    /**
     * Creates a bridge for a specific Groovy resource + implementation class.
     *
     * @param script Pipeline {@link CpsScript} context (non-null)
     * @param resourcePath classpath resource path to Groovy source (non-null, begins with '/')
     * @param implClassName fully-qualified Groovy implementation class name (non-null)
     */
    protected VectorCASTGroovyBridge(final CpsScript script,
                                     final String resourcePath,
                                     final String implClassName) {
        this.script = Objects.requireNonNull(script, "script");
        this.resourcePath = Objects.requireNonNull(resourcePath, "resourcePath");
        this.implClassName = Objects.requireNonNull(implClassName, "implClassName");
    }

    /** Getter for pipeline script. */
    protected final CpsScript getScript() {
        return script;
    }

    /**
     * Gets (and lazily creates) the Groovy delegate implementation object.
     *
     * @return instantiated Groovy object implementing the requested API
     * @throws IllegalStateException if the resource is missing, fails to compile, or the impl class cannot be created
     */
    protected GroovyObject getDelegate() {
        if (delegate != null) {
            return delegate;
        }

        final String groovySource = readBundledGroovy(resourcePath);
        final String scriptName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);

        final GroovyClassLoader gcl = new GroovyClassLoader(getClass().getClassLoader());
        try {
            // Compile the source so the impl class becomes loadable
            gcl.parseClass(groovySource, scriptName);

            final Class<?> implClass = gcl.loadClass(implClassName);
            final Object impl = implClass.getConstructor(Object.class).newInstance(script);

            delegate = (GroovyObject) impl;
            return delegate;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to compile/instantiate " + implClassName + " from " + resourcePath, e);
        }
    }

    /**
     * Reads a UTF-8 text resource from the plugin classpath.
     *
     * @param resourcePath classpath resource path beginning with '/'
     * @return full resource content as text
     */
    private String readBundledGroovy(final String resourcePath) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(resourcePath),
                        "Missing resource: " + resourcePath),
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
