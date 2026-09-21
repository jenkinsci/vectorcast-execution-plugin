package com.vectorcast.plugins.vectorcastexecution;

import groovy.lang.GroovyObject;
import java.util.List;
import java.util.Map;
import org.jenkinsci.plugins.workflow.cps.CpsScript;
import org.jenkinsci.plugins.workflow.cps.GlobalVariable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Tests the Java-to-bundled-Groovy bridge contract used by Pipeline globals. */
class VectorCASTGroovyBridgeTest {

    @Test
    void allProductionBridgesLoadAndCacheTheirBundledGroovyImplementations() {
        CpsScript script = mock(CpsScript.class);
        List<VectorCASTGroovyBridge> bridges = List.of(
            new VectorCASTExecutionBridge(script),
            new VectorCASTLogsBridge(script),
            new VectorCASTMetricsBridge(script),
            new VectorCASTSingleCheckoutBridge(script),
            new VectorCASTUtilsBridge(script)
        );

        for (VectorCASTGroovyBridge bridge : bridges) {
            GroovyObject delegate = bridge.getDelegate();
            assertNotNull(delegate);
            assertSame(delegate, bridge.getDelegate());
        }
    }

    @Test
    void utilsBridgeForwardsCallsToTheBundledImplementation() {
        VectorCASTUtilsBridge bridge = new VectorCASTUtilsBridge(
            mock(CpsScript.class));

        assertEquals("nightly_build_1", bridge.fixUpName("nightly/build 1"));
        assertEquals("project", bridge.getMpName("folder/project.vcm"));
        assertEquals("project", bridge.getMpName("folder/project"));
    }

    @Test
    void missingGroovyResourceProducesAClearFailure() {
        VectorCASTGroovyBridge bridge = new TestBridge(mock(CpsScript.class),
            "/missing/VectorCAST.groovy", "missing.Implementation");

        IllegalStateException error = assertThrows(IllegalStateException.class,
            bridge::getDelegate);
        assertTrue(error.getMessage().contains("Missing resource"));
    }

    @Test
    void pipelineGlobalsExposeTheirDocumentedNamesAndBridgeTypes()
            throws Exception {
        CpsScript script = mock(CpsScript.class);
        List<GlobalVariable> globals = List.of(
            new VectorCASTExecutionGlobal(),
            new VectorCASTLogsGlobal(),
            new VectorCASTMetricsGlobal(),
            new VectorCASTSingleCheckoutGlobal(),
            new VectorCASTUtilsGlobal()
        );

        assertEquals(List.of(
            "VectorCASTExecution", "VectorCASTLogs", "VectorCASTMetrics",
            "VectorCASTSingleCheckout", "VectorCASTUtils"
        ), globals.stream().map(GlobalVariable::getName).toList());
        assertInstanceOf(VectorCASTExecutionBridge.class,
            globals.get(0).getValue(script));
        assertInstanceOf(VectorCASTLogsBridge.class,
            globals.get(1).getValue(script));
        assertInstanceOf(VectorCASTMetricsBridge.class,
            globals.get(2).getValue(script));
        assertInstanceOf(VectorCASTSingleCheckoutBridge.class,
            globals.get(3).getValue(script));
        assertInstanceOf(VectorCASTUtilsBridge.class,
            globals.get(4).getValue(script));
    }

    @Test
    void executionMetricsAndLogsBridgesForwardAllPublicMethods() {
        Object expected = new Object();
        GroovyObject delegate = delegateReturning(expected);

        TestExecutionBridge execution = new TestExecutionBridge(delegate);
        assertSame(expected, execution.getRunCommands(Map.of(), "commands"));
        assertSame(expected, execution.getSetupManageProject(Map.of()));
        assertSame(expected, execution.buildStepSpec(Map.of(), "environment"));

        TestMetricsBridge metrics = new TestMetricsBridge(delegate);
        assertSame(expected, metrics.getMetricsEnvCmds(Map.of(), "environment"));
        assertSame(expected, metrics.getMetricsCmds(Map.of(), List.of("--xml")));

        TestLogsBridge logs = new TestLogsBridge(delegate);
        assertSame(expected, logs.checkBuildLogPlan(Map.of(), Map.of()));
    }

    @Test
    void utilsBridgeForwardsEnvironmentInformation() {
        CpsScript script = mock(CpsScript.class);
        List<String> expected = List.of("unit", "system");
        TestUtilsBridge bridge = new TestUtilsBridge(script,
            delegateReturning(expected));

        assertSame(expected, bridge.getEnvironmentInfo("getjobs output"));
    }

    @Test
    void singleCheckoutNormalizesDelegateReturnValuesToBooleans() {
        assertFalse(new TestSingleCheckoutBridge(delegateReturning(null))
            .updateForSingleCheckout(Map.of()));
        assertTrue(new TestSingleCheckoutBridge(delegateReturning(true))
            .updateForSingleCheckout(Map.of()));
        assertFalse(new TestSingleCheckoutBridge(delegateReturning("false"))
            .updateForSingleCheckout(Map.of()));
        assertTrue(new TestSingleCheckoutBridge(delegateReturning("true"))
            .updateForSingleCheckout(Map.of()));
        assertTrue(new TestSingleCheckoutBridge(delegateReturning(new Object()))
            .updateForSingleCheckout(Map.of()));
    }

    @Test
    void utilsBridgeHandlesMissingAndUnexpectedDelegateValues() {
        CpsScript script = mock(CpsScript.class);
        TestUtilsBridge missing = new TestUtilsBridge(script, delegateReturning(null));
        assertEquals("", missing.fixUpName("name"));
        assertEquals("", missing.getMpName("project.vcm"));
        assertNull(missing.getEnvironmentInfo("output"));
        assertNull(new TestUtilsBridge(script, delegateReturning("not a list"))
            .getEnvironmentInfo("output"));
        TestUtilsBridge numeric = new TestUtilsBridge(script, delegateReturning(42));
        assertEquals("42", numeric.fixUpName("name"));
        assertEquals("42", numeric.getMpName("project.vcm"));
    }

    @Test
    void unavailableImplementationPreservesTheClassLoadingCause() {
        String resource = "/com/vectorcast/plugins/vectorcastexecution"
            + "/default-scripts/VectorCASTUtils.groovy";
        TestBridge bridge = new TestBridge(mock(CpsScript.class), resource,
            "missing.Implementation");
        IllegalStateException error = assertThrows(IllegalStateException.class,
            bridge::getDelegate);
        assertTrue(error.getMessage().contains(resource));
        assertTrue(error.getMessage().contains("missing.Implementation"));
        assertInstanceOf(ClassNotFoundException.class, error.getCause());
    }

    private static GroovyObject delegateReturning(final Object value) {
        GroovyObject delegate = mock(GroovyObject.class);
        when(delegate.invokeMethod(anyString(), any())).thenReturn(value);
        return delegate;
    }

    /** Test-only concrete bridge for exercising resource-load failures. */
    private static final class TestBridge extends VectorCASTGroovyBridge {
        private TestBridge(final CpsScript script, final String resourcePath,
                final String implementationClass) {
            super(script, resourcePath, implementationClass);
        }
    }

    private static final class TestExecutionBridge
            extends VectorCASTExecutionBridge {
        private final GroovyObject delegate;

        private TestExecutionBridge(final GroovyObject inputDelegate) {
            super(mock(CpsScript.class));
            delegate = inputDelegate;
        }

        @Override
        protected GroovyObject getDelegate() {
            return delegate;
        }
    }

    private static final class TestLogsBridge extends VectorCASTLogsBridge {
        private final GroovyObject delegate;

        private TestLogsBridge(final GroovyObject inputDelegate) {
            super(mock(CpsScript.class));
            delegate = inputDelegate;
        }

        @Override
        protected GroovyObject getDelegate() {
            return delegate;
        }
    }

    private static final class TestMetricsBridge extends VectorCASTMetricsBridge {
        private final GroovyObject delegate;

        private TestMetricsBridge(final GroovyObject inputDelegate) {
            super(mock(CpsScript.class));
            delegate = inputDelegate;
        }

        @Override
        protected GroovyObject getDelegate() {
            return delegate;
        }
    }

    private static final class TestSingleCheckoutBridge
            extends VectorCASTSingleCheckoutBridge {
        private final GroovyObject delegate;

        private TestSingleCheckoutBridge(final GroovyObject inputDelegate) {
            super(mock(CpsScript.class));
            delegate = inputDelegate;
        }

        @Override
        protected GroovyObject getDelegate() {
            return delegate;
        }
    }

    private static final class TestUtilsBridge extends VectorCASTUtilsBridge {
        private final GroovyObject delegate;

        private TestUtilsBridge(final CpsScript script,
                final GroovyObject inputDelegate) {
            super(script);
            delegate = inputDelegate;
        }

        @Override
        protected GroovyObject getDelegate() {
            return delegate;
        }
    }
}
