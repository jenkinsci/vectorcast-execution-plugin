package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

/**
 * Pipeline global bridge for {@code VectorCASTAdditionalTools}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTAdditionalToolsBridge extends VectorCASTGroovyBridge {
    private static final long serialVersionUID = 1L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
            "/com/vectorcast/plugins/vectorcastexecution/default-scripts/VectorCASTAdditionalTools.groovy";

    /** Groovy implementation class defined by the bundled source. */
    private static final String IMPL_CLASS_NAME =
            "com.vectorcast.plugins.vectorcastexecution.VectorCASTAdditionalToolsImpl";

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script Pipeline CPS script context
     */
    public VectorCASTAdditionalToolsBridge(final CpsScript script) {
        super(script, DEFAULT_RESOURCE_PATH, IMPL_CLASS_NAME);
    }

    /**
     * Forwards to Groovy: {@code additionalTools(VC)}.
     */
    public Object additionalTools(final Object VC) {
        return getDelegate().invokeMethod("additionalTools", VC);
    }
}
