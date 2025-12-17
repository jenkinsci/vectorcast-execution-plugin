package com.vectorcast.plugins.vectorcastexecution;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;

/**
 * Jenkins Pipeline {@link GlobalVariable} that exposes the {@code VectorCASTUtils} global in Pipeline scripts.
 *
 * <p>This class is intentionally small and stable: it only registers the global name and returns a
 * per-build delegate object.</p>
 *
 * <p>The goal is to keep the implementation customer-editable in Groovy, while still providing a
 * plugin-installed DSL entry point.</p>
 *
 * <h2>Usage in a Jenkinsfile</h2>
 * <pre>{@code
 * def (found, fail, unstable) = VectorCASTUtils.checkBuildLogForErrors("build.log")
 * }</pre>
 *
 * <p>The behavior of {@code VectorCASTUtils} is implemented by {@link VectorCASTUtilsBridge}, which loads
 * a Groovy implementation from the workspace (if present) or falls back to a plugin-bundled default.</p>
 */
@Extension
public class VectorCASTUtilsGlobal extends GlobalVariable {

    /**
     * Name of the global variable as seen by Pipeline scripts.
     */
    @Override
    public String getName() {
        return "VectorCASTUtils";
    }

    /**
     * Returns the per-build value bound to the {@code VectorCASTUtils} global.
     *
     * @param script the CPS-transformed Pipeline script for the current run
     * @return a delegate that implements the public utility functions
     */
    @Override
    public Object getValue(CpsScript script) {
        return new VectorCASTUtilsBridge(script);
    }
}
