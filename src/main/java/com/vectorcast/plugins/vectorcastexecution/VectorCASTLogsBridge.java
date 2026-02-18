package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serial;
import java.util.Map;

/**
 * Pipeline global bridge for {@code VectorCASTLogs}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTLogsBridge extends VectorCASTGroovyBridge {
    @Serial
    private static final long serialVersionUID = -5605681200041687890L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
        "/com/vectorcast/plugins/vectorcastexecution"
        + "/default-scripts/VectorCASTLogs.groovy";

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
     * Forwards to Groovy: {@code checkBuildLogPlan(VC, inputs)}.
     * @param vc global VectorCAST settings from Jenkinsfile
     * @param inputs for generating the checks
     * @return object commands and results
     */
    public Object checkBuildLogPlan(final Object vc, final Map<?, ?> inputs) {
        return getDelegate().invokeMethod(
                "checkBuildLogPlan",
                new Object[] {vc, inputs }
        );
    }
}
