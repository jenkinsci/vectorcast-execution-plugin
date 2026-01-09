package com.vectorcast.plugins.vectorcastexecution;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.jspecify.annotations.NonNull;

/**
 * Jenkins Pipeline {@link GlobalVariable} that exposes the
 * {@code VectorCASTSingleCheckout} global in Pipeline scripts.
 *
 * <p>This class is intentionally small and stable:
 * it only registers the global name and returns a
 * per-build delegate object.</p>
 *
 * <p>The goal is to keep the implementation customer-editable
 * in Groovy, while still providing a
 * plugin-installed DSL entry point.</p>
 *
 * <h2>Usage in a Jenkinsfile</h2>
 * <pre>{@code
 * def (found, fail, unstable)
 *      = VectorCASTSingleCheckout.updateForSingleCheckout(VC)
 * }</pre>
 *
 * <p>The behavior of {@code VectorCASTSingleCheckout} is implemented
 * by {@link VectorCASTSingleCheckoutBridge}, which loads
 * a Groovy implementation from the workspace (if present)
 * or falls back to a plugin-bundled default.</p>
 */
@Extension
public class VectorCASTSingleCheckoutGlobal extends GlobalVariable {

    /**
     * Name of the global variable as seen by Pipeline scripts.
     * @return Name of groovy script name
     */
    @Override @NonNull
    public String getName() {
        return "VectorCASTSingleCheckout";
    }

    /**
     * Returns the per-build value bound to the
     *      {@code VectorCASTSingleCheckout} global.
     *
     * @param script the CPS-transformed Pipeline script for the current run
     * @return a delegate that implements the public utility functions
     */
    @Override @NonNull
    public Object getValue(@NonNull final CpsScript script) {
        return new VectorCASTSingleCheckoutBridge(script);
    }
}
