package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

/**
 * Pipeline global bridge for {@code VectorCASTLogs}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTLogsBridge extends VectorCASTGroovyBridge {
    private static final long serialVersionUID = 1L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
            "/com/vectorcast/plugins/vectorcastexecution/default-scripts/VectorCASTLogs.groovy";

    /** Groovy implementation class defined by the bundled source. */
    private static final String IMPL_CLASS_NAME =
            "com.vectorcast.plugins.vectorcastexecution.VectorCASTLogsImpl";

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script Pipeline CPS script context
     */
    public VectorCASTLogsBridge(final CpsScript script) {
        super(script, DEFAULT_RESOURCE_PATH, IMPL_CLASS_NAME);
    }

    /**
     * Forwards to Groovy: {@code checkLogsForErrors(VC, log)}.
     */
    public Object checkLogsForErrors(final Object VC, final String log) {
        return getDelegate().invokeMethod("checkLogsForErrors", new Object[]{VC, log});
    }

    /**
     * Forwards to Groovy: {@code checkBuildLogForErrors(VC, logFile)}.
     */
    public Object checkBuildLogForErrors(final Object VC, final String logFile) {
        return getDelegate().invokeMethod("checkBuildLogForErrors", new Object[]{VC, logFile});
    }
}
