package com.vectorcast.plugins.vectorcastexecution;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests both agent platforms and command failures without launching a shell. */
class VectorCASTCommandPolicyTest {
    @ParameterizedTest
    @CsvSource({"true, success", "false, success", "true, failure",
        "false, failure", "true, interrupted", "false, interrupted"})
    void selectsAgentPlatformAndPropagatesCommandOutcome(boolean unix,
            String outcome) throws Exception {
        AbstractBuild<?, ?> build = mock(FreeStyleBuild.class);
        Launcher launcher = mock(Launcher.class);
        BuildListener listener = mock(BuildListener.class);
        when(launcher.isUnix()).thenReturn(unix);
        try (MockedConstruction<Shell> shells = mockConstruction(Shell.class,
                (shell, context) -> {
                    assertEquals("unix command", context.arguments().get(0));
                    if ("interrupted".equals(outcome)) {
                        when(shell.perform(build, launcher, listener))
                            .thenThrow(new InterruptedException("test interruption"));
                    } else {
                        when(shell.perform(build, launcher, listener))
                            .thenReturn("success".equals(outcome));
                    }
                });
                MockedConstruction<BatchFile> batches = mockConstruction(
                    BatchFile.class, (batch, context) -> {
                        assertEquals("windows command", context.arguments().get(0));
                        if ("interrupted".equals(outcome)) {
                            when(batch.perform(build, launcher, listener))
                                .thenThrow(new InterruptedException("test interruption"));
                        } else {
                            when(batch.perform(build, launcher, listener))
                                .thenReturn("success".equals(outcome));
                        }
                    })) {
            new VectorCASTCommand("windows command", "unix command")
                .perform(build, null, launcher, listener);
            assertEquals(unix ? 1 : 0, shells.constructed().size());
            assertEquals(unix ? 0 : 1, batches.constructed().size());
            if (unix) {
                verify(shells.constructed().get(0)).perform(build, launcher, listener);
            } else {
                verify(batches.constructed().get(0)).perform(build, launcher, listener);
            }
            if ("success".equals(outcome)) {
                verify(build, never()).setResult(any(Result.class));
            } else {
                verify(build).setResult(Result.FAILURE);
            }
        }
    }
}
