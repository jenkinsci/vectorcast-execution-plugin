package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serial;
import java.util.List;

/**
 * Pipeline global bridge for {@code VectorCASTEnvInfo}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTEnvInfoBridge extends VectorCASTGroovyBridge {
    @Serial
    private static final long serialVersionUID = -6521891005541848533L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
        "/com/vectorcast/plugins/vectorcastexecution"
        + "/default-scripts/VectorCASTEnvInfo.groovy";

    /** Groovy implementation class defined by the bundled source. */
    private static final String IMPL_CLASS_NAME =
            "com.vectorcast.plugins.vectorcastexecution.VectorCASTEnvInfoImpl";

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script Pipeline CPS script context
     */
    public VectorCASTEnvInfoBridge(final CpsScript script) {
        super(script, DEFAULT_RESOURCE_PATH, IMPL_CLASS_NAME);
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
