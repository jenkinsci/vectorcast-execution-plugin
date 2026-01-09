package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serial;

/**
 * Pipeline global bridge for {@code VectorCASTExecution}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTExecutionBridge extends VectorCASTGroovyBridge {
    @Serial
    private static final long serialVersionUID = 1401312145551656152L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
        "/com/vectorcast/plugins/vectorcastexecution/"
        + "default-scripts/VectorCASTExecution.groovy";

    /** Groovy implementation class defined by the bundled source. */
    private static final String IMPL_CLASS_NAME =
        "com.vectorcast.plugins.vectorcastexecution.VectorCASTExecutionImpl";

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script Pipeline CPS script context
     */
    public VectorCASTExecutionBridge(final CpsScript script) {
        super(script, DEFAULT_RESOURCE_PATH, IMPL_CLASS_NAME);
    }

    /**
     * Forwards to Groovy: {@code getRunCommands(VC, commands)}.
     * @param vc - global VectorCAST settings from Jenkinsfile
     * @param commands to be executed
     * @return - a list of the run commands
     */
    public Object getRunCommands(final Object vc, final String commands) {
        return getDelegate().invokeMethod("getRunCommands",
                new Object[] {vc, commands});
    }
    /**
     * Forwards to Groovy: {@code getSetupManageProject(VC)}.
     * @param vc global VectorCAST settings from Jenkinsfile
     * @return - a list of the run commands
     */
    public Object getSetupManageProject(final Object vc) {
        return getDelegate().invokeMethod("getSetupManageProject", vc);
    }

    /**
     * Forwards to Groovy: {@code runUnitTestingParallel(VC)}.
     * @param vc - global VectorCAST settings from Jenkinsfile
     * @param inputString - line specifying environment
     * @return - a list of the run commands
     */
    public Object buildStepSpec(final Object vc, final String inputString) {
        return getDelegate().invokeMethod("buildStepSpec",
            new Object[] {vc, inputString }
        );
    }
}
