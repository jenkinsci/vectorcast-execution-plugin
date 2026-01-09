package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serial;
import java.util.List;

/**
 * Pipeline global bridge for {@code VectorCASTMetrics}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTMetricsBridge extends VectorCASTGroovyBridge {
    @Serial
    private static final long serialVersionUID = -6333641713155901055L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
        "/com/vectorcast/plugins/vectorcastexecution"
        + "/default-scripts/VectorCASTMetrics.groovy";

    /** Groovy implementation class defined by the bundled source. */
    private static final String IMPL_CLASS_NAME =
        "com.vectorcast.plugins.vectorcastexecution.VectorCASTMetricsImpl";

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script Pipeline CPS script context
     */
    public VectorCASTMetricsBridge(final CpsScript script) {
        super(script, DEFAULT_RESOURCE_PATH, IMPL_CLASS_NAME);
    }

    /**
     * Forwards to Groovy: {@code getMetricsEnvCmds(VC)}.
     * @param vc - global VectorCAST settings from Jenkinsfile
     * @param envDesc string to describe the env
     * @return list of commands to run
     */
    public Object getMetricsEnvCmds(final Object vc, final String envDesc) {
        return getDelegate().invokeMethod("getMetricsEnvCmds",
            new Object[] {vc, envDesc }
        );
    }

    /**
     * Forwards to Groovy: {@code getMetricsCmds(VC, extraResultOptions)}.
     * @param vc - global VectorCAST settings from Jenkinsfile
     * @param extraResultOptions string list of extra options for
     *                           report generation
     * @return list of commands to run
     */
    public Object getMetricsCmds(final Object vc,
                                 final List<?> extraResultOptions) {
        return getDelegate().invokeMethod(
                "getMetricsCmds", new Object[] {vc, extraResultOptions }
        );
    }
}
