package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serial;

/**
 * Pipeline global bridge for {@code VectorCASTHelpers}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTHelpersBridge extends VectorCASTGroovyBridge {
    @Serial
    private static final long serialVersionUID = -6365232646839418232L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
        "/com/vectorcast/plugins/vectorcastexecution"
        + "/default-scripts/VectorCASTHelpers.groovy";

    /** Groovy implementation class defined by the bundled source. */
    private static final String IMPL_CLASS_NAME =
            "com.vectorcast.plugins.vectorcastexecution.VectorCASTHelpersImpl";

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script Pipeline CPS script context
     */
    public VectorCASTHelpersBridge(final CpsScript script) {
        super(script, DEFAULT_RESOURCE_PATH, IMPL_CLASS_NAME);
    }

    /**
     * Forwards to Groovy: {@code fixUpName(name)}.
     * @param name  - name to be corrected
     * @return the fixed up name
     */
    public String fixUpName(final String name) {
         Object r = getDelegate().invokeMethod("fixUpName", name);
         return (r != null) ? r.toString() : "";
    }

    /**
     * Forwards to Groovy: {@code formatPath(inPath)}.
     * @param inPath path that needs formatting
     * @return path formatted properly
     */
    public String formatPath(final String inPath) {
        Object r = getDelegate().invokeMethod("formatPath", inPath);
        return (r != null) ? r.toString() : "";
    }

    /**
     * Forwards to Groovy: {@code getMpName(inputMpName)}.
     * @param inputMpName - default path VCproject name
     * @return just the manage project name
     */
    public String getMpName(final String inputMpName) {
        Object r = getDelegate().invokeMethod("getMpName", inputMpName);
        return (r != null) ? r.toString() : "";
    }
}
