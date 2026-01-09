package com.vectorcast.plugins.vectorcastexecution;

import org.jenkinsci.plugins.workflow.cps.CpsScript;

import java.io.Serial;

/**
 * Pipeline global bridge for {@code VectorCASTSingleCheckout}.
 *
 * <p>Backed by a Groovy implementation bundled with the plugin.</p>
 */
public class VectorCASTSingleCheckoutBridge extends VectorCASTGroovyBridge {
    @Serial
    private static final long serialVersionUID = 5278922835694952077L;

    /** Bundled Groovy source resource. */
    private static final String DEFAULT_RESOURCE_PATH =
        "/com/vectorcast/plugins/vectorcastexecution/"
        + "default-scripts/VectorCASTSingleCheckout.groovy";

    /** Groovy implementation class defined by the bundled source. */
    private static final String IMPL_CLASS_NAME =
        "com.vectorcast.plugins.vectorcastexecution"
        + ".VectorCASTSingleCheckoutImpl";

    /**
     * Creates a new bridge for the given Pipeline execution.
     *
     * @param script Pipeline CPS script context
     */
    public VectorCASTSingleCheckoutBridge(final CpsScript script) {
        super(script, DEFAULT_RESOURCE_PATH, IMPL_CLASS_NAME);
    }

    /**
     * Forwards to Groovy: {@code updateForSingleCheckout(VC)}.
     * @param vc - global VectorCAST settings from Jenkinsfile
     * @return if the scripts were updated for single checkout
     */
    public boolean updateForSingleCheckout(final Object vc) {
        Object r = getDelegate().invokeMethod("updateForSingleCheckout",
                new Object[] {vc }
        );

        if (r == null) {
            return false; // or throw if you consider null a bug
        }
        if (r instanceof Boolean) {
            return (Boolean) r;
        }
        if (r instanceof CharSequence) {
            // just in case something returned "true"/"false"
            return Boolean.parseBoolean(r.toString());
        }
        // last-resort: Groovy truthiness (non-null => true)
        return true;
    }
}
