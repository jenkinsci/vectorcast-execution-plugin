package com.vectorcast.plugins.vectorcastexecution;

import com.jenkinsci.plugins.badge.action.BadgeAction;
import com.jenkinsci.plugins.badge.action.BadgeSummaryAction;
import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.markup.MarkupFormatter;
import hudson.util.StreamTaskListener;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Exercises report decoding and result policy without starting Jenkins. */
class VectorCASTPostBuildPolicyTest {
    @TempDir
    Path workspace;

    private MockedStatic<Jenkins> jenkinsAccess;

    @BeforeEach
    void provideTheBadgeTextFormatter() throws Exception {
        Jenkins jenkins = mock(Jenkins.class);
        MarkupFormatter formatter = mock(MarkupFormatter.class);
        when(jenkins.getMarkupFormatter()).thenReturn(formatter);
        when(formatter.translate(anyString())).thenAnswer(call -> call.getArgument(0));
        jenkinsAccess = mockStatic(Jenkins.class);
        jenkinsAccess.when(Jenkins::get).thenReturn(jenkins);
    }

    @AfterEach
    void releaseTheBadgeTextFormatter() {
        if (jenkinsAccess != null) {
            jenkinsAccess.close();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "'', '', UTF-8",
        "41, A, UTF-8",
        "efbbbf41, \ufeffA, UTF-8",
        "fffe4100, \ufeffA, UTF-16LE",
        "feff0041, \ufeffA, UTF-16BE",
        "efbca1, \uff21, UTF-8",
        "efbbbe, \ufefe, UTF-8",
        "ff41, \u41ff, UTF-16LE",
        "fe41, \u41fe, UTF-16LE",
        "ff, \u00ff, ISO-2022-KR",
        "fe, \u00fe, ISO-2022-KR"
    })
    void decodesMetricsOnlyReportsAndHandlesBomBoundaries(String hex,
            String expected, String charset) throws Exception {
        Files.write(workspace.resolve("demo_metrics_report.html_tmp"),
            HexFormat.of().parseHex(hex));
        Run<?, ?> run = mock(Run.class);
        when(run.getLogReader()).thenReturn(new StringReader("normal build\n"));
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        try (StreamTaskListener listener = new StreamTaskListener(log,
                StandardCharsets.UTF_8)) {
            new VectorCASTPostBuildPublisher("demo").perform(run,
                new FilePath(workspace.toFile()), null, listener);
        }
        ArgumentCaptor<Action> actions = ArgumentCaptor.forClass(Action.class);
        verify(run).addAction(actions.capture());
        BadgeSummaryAction summary = (BadgeSummaryAction) actions.getValue();
        assertEquals("<hr style=\"height:5px;border-width:0;"
            + "color:gray;background-color:gray\"> " + expected,
            summary.getText());
        assertTrue(log.toString(StandardCharsets.UTF_8)
            .contains("with charset: " + charset));
        verify(run, never()).setResult(any(Result.class));
        verify(run, never()).setDescription(any());
    }

    @Test
    void repeatedFailuresProduceOneStatusBadgeAndMissingReportsStayFailed()
            throws Exception {
        Run<?, ?> run = mock(Run.class);
        when(run.getLogReader()).thenReturn(new StringReader(
            "INCR_BUILD_FAILED\nINCR_BUILD_FAILED\n"));
        try (StreamTaskListener listener = new StreamTaskListener(
                new ByteArrayOutputStream(), StandardCharsets.UTF_8)) {
            new VectorCASTPostBuildPublisher("demo").perform(run,
                new FilePath(workspace.toFile()), null, listener);
        }
        verify(run).setResult(Result.FAILURE);
        verify(run, never()).setResult(Result.UNSTABLE);
        ArgumentCaptor<Action> actions = ArgumentCaptor.forClass(Action.class);
        verify(run, times(5)).addAction(actions.capture());
        assertEquals(1, actions.getAllValues().stream()
            .filter(BadgeAction.class::isInstance)
            .map(BadgeAction.class::cast)
            .filter(action -> "Build Error".equals(action.getText())).count());
        assertEquals(1, actions.getAllValues().stream()
            .filter(BadgeSummaryAction.class::isInstance)
            .map(BadgeSummaryAction.class::cast)
            .filter(action -> "General Failure".equals(action.getText())).count());
    }
}
