package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serial;
import java.util.List;

/**
 * Pipeline global bridge for {@code VectorCASTUtils}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTUtilsBridge extends VectorCASTGroovyBridge {
    @Serial
    private static final long serialVersionUID = -6365232646839418232L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
        "/com/vectorcast/plugins/vectorcastexecution"
        + "/default-scripts/VectorCASTUtils.groovy";

    /** Groovy implementation class defined by the bundled source. */
    private static final String IMPL_CLASS_NAME =
            "com.vectorcast.plugins.vectorcastexecution.VectorCASTUtilsImpl";

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script Pipeline CPS script context
     */
    public VectorCASTUtilsBridge(final CpsScript script) {
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
     * Forwards to Groovy: {@code getMpName(inputMpName)}.
     * @param inputMpName - default path VCproject name
     * @return just the manage project name
     */
    public String getMpName(final String inputMpName) {
        Object r = getDelegate().invokeMethod("getMpName", inputMpName);
        return (r != null) ? r.toString() : "";
    }
    
    /**
     * Forwards to Groovy: {@code getEnvironmentInfo(VC)}.
     * @param getJobsLog String returned from getjobs.py --type
     * @return List of jobs to create/run
     */
    public List<?> getEnvironmentInfo(final String getJobsLog) {

        Object r = getDelegate().invokeMethod("getEnvironmentInfo", getJobsLog);

        getScript().invokeMethod("echo", "returned from getDelegate()");

        if (r == null) {
            getScript().invokeMethod("echo",
                "getEnvironmentInfo returned null "
                + "Groovy impl did not return [UtEnvList, StEnvList])"
            );
            return null; // unreachable after error(), but keeps compiler happy
        }
        if (!(r instanceof List)) {
            getScript().invokeMethod("echo",
            "getEnvironmentInfo returned " + r.getClass() + " not a List");
            return null;
        }
        return (List<?>) r;
    }
}
