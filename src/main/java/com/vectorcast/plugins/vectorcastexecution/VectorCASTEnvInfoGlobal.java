package com.vectorcast.plugins.vectorcastexecution;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

/**
 * Jenkins Pipeline {@link GlobalVariable} that
 *    exposes the {@code VectorCASTEnvInfo} global in Pipeline scripts.
 *
 * <p>This class is intentionally small and stable:
 *    it only registers the global name and returns a
 *    per-build delegate object.</p>
 *
 * <p>The goal is to keep the implementation customer-editable
 *    in Groovy, while still providing a
 *    plugin-installed DSL entry point.</p>
 *
 * <h2>Usage in a Jenkinsfile</h2>
 * <pre>{@code
 * def (found, fail, unstable) =
 *    VectorCASTEnvInfo.checkBuildLogForErrors("build.log")
 * }</pre>
 *
 * <p>The behavior of {@code VectorCASTEnvInfo} is implemented
 *    by {@link VectorCASTEnvInfoBridge}, which loads
 *    a Groovy implementation from the workspace (if present) or
 *    falls back to a plugin-bundled default.</p>
 */
@Extension
public class VectorCASTEnvInfoGlobal extends GlobalVariable {

    /**
     * Name of the global variable as seen by Pipeline scripts.
     */
    @Override @NonNull
    public String getName() {
        return "VectorCASTEnvInfo";
    }

    /**
     * Returns the per-build value bound
     *    to the {@code VectorCASTEnvInfo} global.
     *
     * @param script the CPS-transformed Pipeline script for the current run
     * @return a delegate that implements the public utility functions
     */
    @Override @NonNull
    public Object getValue(@NonNull final CpsScript script) {
        return new VectorCASTEnvInfoBridge(script);
    }
}
