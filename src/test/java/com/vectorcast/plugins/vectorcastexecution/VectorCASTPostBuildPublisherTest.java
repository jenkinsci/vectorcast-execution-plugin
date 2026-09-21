/*
 * The MIT License
 *
 * Copyright 2026 Vector Informatik, GmbH
 */
package com.vectorcast.plugins.vectorcastexecution;

import com.jenkinsci.plugins.badge.action.BadgeAction;
import com.jenkinsci.plugins.badge.action.BadgeSummaryAction;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the native replacement for the generated Groovy Postbuild script. */
@WithJenkins
class VectorCASTPostBuildPublisherTest {

    @Test
    void createsSummaryFromTheFourFreestyleReportFragments(JenkinsRule rule)
            throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getBuildersList().add(new ReportWriter());
        project.getPublishersList().add(new VectorCASTPostBuildPublisher("demo"));

        FreeStyleBuild build = rule.buildAndAssertSuccess(project);

        List<BadgeSummaryAction> summaries = build.getActions().stream()
            .filter(BadgeSummaryAction.class::isInstance)
            .map(BadgeSummaryAction.class::cast)
            .toList();
        assertEquals(1, summaries.size());
        String summary = summaries.get(0).getText();
        assertTrue(summary.contains("coverage report"));
        assertTrue(summary.contains("incremental rebuild"));
        assertTrue(summary.contains("full report"));
        assertTrue(summary.contains("metrics report"));
        assertTrue(summary.contains("height:5px"));
    }

    @Test
    void preservesFailureClassificationAndStatusBadge(JenkinsRule rule)
            throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getBuildersList().add(new ReportWriter());
        project.getBuildersList().add(new LogWriter("INCR_BUILD_FAILED"));
        project.getPublishersList().add(new VectorCASTPostBuildPublisher("demo"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        rule.assertBuildStatus(Result.FAILURE, build);
        assertTrue(hasSummary(build, "Build Error"));
        assertTrue(hasBadge(build, "Build Error"));
    }

    @Test
    void marksBuildUnstableForARecoverableLogRule(JenkinsRule rule)
            throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getBuildersList().add(new ReportWriter());
        project.getBuildersList().add(new LogWriter(
            "INFO: Problem parsing test results"));
        project.getPublishersList().add(new VectorCASTPostBuildPublisher("demo"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        rule.assertBuildStatus(Result.UNSTABLE, build);
        assertTrue(hasSummary(build, "Test Results Parse Error"));
        assertTrue(hasBadge(build, "Test Results Parse Error"));
    }

    @Test
    void failureTakesPrecedenceOverAnUnstableLogRule(JenkinsRule rule)
            throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getBuildersList().add(new ReportWriter());
        project.getBuildersList().add(new LogWriter(
            "INFO: File System Error\nINCR_BUILD_FAILED"));
        project.getPublishersList().add(new VectorCASTPostBuildPublisher("demo"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        rule.assertBuildStatus(Result.FAILURE, build);
        assertTrue(hasSummary(build, "File System Error"));
        assertTrue(hasSummary(build, "Build Error"));
    }

    @Test
    void decodesUtf16ReportFragmentsWithABom(JenkinsRule rule)
            throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getBuildersList().add(new Utf16ReportWriter());
        project.getPublishersList().add(new VectorCASTPostBuildPublisher("demo"));

        FreeStyleBuild build = rule.buildAndAssertSuccess(project);

        BadgeSummaryAction summary = build.getActions(BadgeSummaryAction.class)
            .get(0);
        assertTrue(summary.getText().contains("UTF-16 full report"));
    }

    @Test
    void marksBuildUnstableWhenFullAndMetricsReportsAreBothAbsent(
            JenkinsRule rule) throws Exception {
        FreeStyleProject project = rule.createFreeStyleProject();
        project.getPublishersList().add(new VectorCASTPostBuildPublisher("demo"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();

        rule.assertBuildStatus(Result.UNSTABLE, build);
        assertEquals("General Failure, Incremental Build Report or Full Report "
            + "Not Present. Please see the console for more information",
            build.getDescription());
        assertTrue(hasSummary(build, "General Failure"));
        assertTrue(hasBadge(build, "General Error"));
    }

    private boolean hasSummary(final Run<?, ?> run, final String text) {
        return run.getActions(BadgeSummaryAction.class).stream()
            .anyMatch(action -> text.equals(action.getText()));
    }

    private boolean hasBadge(final Run<?, ?> run, final String text) {
        return run.getActions(BadgeAction.class).stream()
            .anyMatch(action -> text.equals(action.getText()));
    }

    /** Writes exactly the four report fragments supported by Freestyle jobs. */
    private static final class ReportWriter extends Builder {
        @Override
        public boolean perform(final AbstractBuild<?, ?> build,
                final Launcher launcher, final BuildListener listener)
                throws InterruptedException, IOException {
            FilePath workspace = build.getWorkspace();
            workspace.child("coverage_diffs.html_tmp").write("coverage report",
                "UTF-8");
            workspace.child("demo_rebuild.html_tmp").write("incremental rebuild",
                "UTF-8");
            workspace.child("demo_full_report.html_tmp").write("full report",
                "UTF-8");
            workspace.child("demo_metrics_report.html_tmp").write("metrics report",
                "UTF-8");
            return true;
        }
    }

    /** Writes a single log line for post-build log-rule tests. */
    private static final class LogWriter extends Builder {
        private final String line;

        LogWriter(final String inputLine) {
            this.line = inputLine;
        }

        @Override
        public boolean perform(final AbstractBuild<?, ?> build,
                final Launcher launcher, final BuildListener listener) {
            listener.getLogger().println(line);
            return true;
        }
    }

    /** Writes a UTF-16LE full report, including its byte-order mark. */
    private static final class Utf16ReportWriter extends Builder {
        @Override
        public boolean perform(final AbstractBuild<?, ?> build,
                final Launcher launcher, final BuildListener listener)
                throws InterruptedException, IOException {
            build.getWorkspace().child("demo_full_report.html_tmp")
                .write("\ufeffUTF-16 full report", "UTF-16LE");
            return true;
        }
    }
}
