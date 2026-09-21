/*
 * The MIT License
 *
 * Copyright 2026 Vector Informatik, GmbH
 */
package com.vectorcast.plugins.vectorcastexecution;

import com.jenkinsci.plugins.badge.action.BadgeAction;
import com.jenkinsci.plugins.badge.action.BadgeSummaryAction;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Applies the VectorCAST post-build result policy to a Freestyle build.
 *
 * <p>This replaces the generated Groovy Postbuild script. Keeping the policy
 * in plugin code avoids Script Security whole-script approval for each newly
 * generated Freestyle job.</p>
 */
public final class VectorCASTPostBuildPublisher extends Recorder
        implements SimpleBuildStep {

    /** Build-log rules preserved from the former post-build Groovy script. */
    private static final List<LogRule> LOG_RULES = List.of(
        new LogRule("INFO: File System Error", Result.UNSTABLE,
            "File System Error"),
        new LogRule("INFO: Problem parsing test results", Result.UNSTABLE,
            "Test Results Parse Error"),
        new LogRule("ERROR: Error accessing DataAPI for", Result.UNSTABLE,
            "VectorCAST DataAPI Error"),
        new LogRule("py did not execute correctly", Result.FAILURE,
            "Jenkins Integration Script Failure"),
        new LogRule("Traceback (most recent call last", Result.FAILURE,
            "Jenkins Integration Script Failure"),
        new LogRule("Failed to acquire lock on environment", Result.FAILURE,
            "Failed to acquire lock on environment"),
        new LogRule("Environment Creation Failed", Result.FAILURE,
            "Environment Creation Failed"),
        new LogRule("newer version of VectorCAST", Result.FAILURE,
            "Conflicting VectorCAST and VectorCAST Project versions"),
        new LogRule("FLEXlm Error", Result.FAILURE, "FLEXlm Error"),
        new LogRule("ERROR: Failed to obtain a license", Result.FAILURE,
            "FLEXlm Error"),
        new LogRule("Unable to obtain license", Result.FAILURE,
            "Unable to obtain license"),
        new LogRule("INCR_BUILD_FAILED", Result.FAILURE, "Build Error"),
        new LogRule("Environment was not successfully built", Result.FAILURE,
            "Build Error"),
        new LogRule("NOT_LINKED", Result.FAILURE, "Link Error"),
        new LogRule("Preprocess Failed", Result.FAILURE, "Preprocess Error"),
        new LogRule("Value Line Error - Command Ignored", Result.UNSTABLE,
            "Test Case Import Error"),
        new LogRule("(E) @LINE", Result.UNSTABLE,
            "Test Case Import Error"),
        new LogRule("Abnormal Termination on Environment", Result.FAILURE,
            "Abnormal Termination of at least one Environment")
    );

    /** Base name used by generated VectorCAST report fragments. */
    private final String projectBase;

    /** Summary icon used for successful report fragments. */
    private static final String REPORT_ICON = "icon-orange-square icon-xlg";

    /** Summary icon used for unstable result classifications. */
    private static final String WARNING_ICON = "icon-warning icon-xlg";

    /** Summary icon used for failed result classifications. */
    private static final String ERROR_ICON = "icon-error icon-xlg";

    /**
     * Creates the post-build publisher for a generated Freestyle job.
     *
     * @param inputProjectBase VectorCAST project base name
     */
    @DataBoundConstructor
    public VectorCASTPostBuildPublisher(final String inputProjectBase) {
        this.projectBase = inputProjectBase;
    }

    /**
     * Gets the VectorCAST project base name.
     *
     * @return project base name
     */
    public String getProjectBase() {
        return projectBase;
    }

    @Override
    public void perform(final Run<?, ?> run, final FilePath workspace,
            final Launcher launcher, final TaskListener listener)
            throws IOException, InterruptedException {
        Result detectedResult = findResult(run, listener);
        if (detectedResult != null) {
            run.setResult(detectedResult);
        }

        String reportSummary = readReportSummary(workspace, listener);
        // The Groovy Postbuild script added this summary unconditionally.
        // BadgeSummaryAction's seven-argument constructor is (id, icon, text,
        // cssClass, style, link, target), the same layout used by Pipeline's
        // addSummary step.
        run.addAction(new BadgeSummaryAction(null, REPORT_ICON, reportSummary,
            "", "", "", ""));

        if (!hasMainReport(workspace)) {
            String message = "General Failure, Incremental Build Report or "
                + "Full Report Not Present. Please see the console for more "
                + "information";
            listener.getLogger().println("[VectorCAST Post Build] " + message);
            run.addAction(new BadgeSummaryAction(null, ERROR_ICON,
                "General Failure", "", "", "", ""));
            run.addAction(new BadgeAction(null, ERROR_ICON, "General Error",
                "", "", "", ""));
            run.setDescription(message);
            if (detectedResult != Result.FAILURE) {
                run.setResult(Result.UNSTABLE);
            }
        }
    }

    /**
     * Reads the build log once and returns the worst matching result.
     *
     * @param run current build
     * @param listener build logger
     * @return failure, unstable, or null when no rule matches
     * @throws IOException if the build log cannot be read
     */
    private Result findResult(final Run<?, ?> run, final TaskListener listener)
            throws IOException {
        boolean unstable = false;
        boolean failure = false;
        boolean[] matched = new boolean[LOG_RULES.size()];

        try (BufferedReader reader = new BufferedReader(run.getLogReader())) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (int index = 0; index < LOG_RULES.size(); index++) {
                    LogRule rule = LOG_RULES.get(index);
                    if (!matched[index] && rule.matches(line)) {
                        matched[index] = true;
                        listener.getLogger().println("[VectorCAST Post Build] "
                            + rule.message());
                        addStatusActions(run, rule.result(), rule.message());
                        if (rule.result() == Result.FAILURE) {
                            failure = true;
                        } else {
                            unstable = true;
                        }
                    }
                }
            }
        }

        if (failure) {
            return Result.FAILURE;
        }
        return unstable ? Result.UNSTABLE : null;
    }

    /**
     * Checks for either report whose absence was treated as a general failure.
     *
     * @param workspace build workspace
     * @return true if the full or metrics report fragment exists
     * @throws IOException if the workspace cannot be queried
     * @throws InterruptedException if the agent operation is interrupted
     */
    private boolean hasMainReport(final FilePath workspace)
            throws IOException, InterruptedException {
        return workspace.child(projectBase + "_full_report.html_tmp").exists()
            || workspace.child(projectBase + "_metrics_report.html_tmp").exists();
    }

    /**
     * Recreates the report-fragment summary formerly rendered by Groovy
     * Postbuild on the build page.
     *
     * @param workspace build workspace
     * @param listener build logger
     * @return HTML summary content, or an empty string when no fragment exists
     * @throws IOException if a report fragment cannot be read
     * @throws InterruptedException if the agent operation is interrupted
     */
    private String readReportSummary(final FilePath workspace,
            final TaskListener listener) throws IOException, InterruptedException {
        List<FilePath> reports = new ArrayList<>();
        reports.add(workspace.child("coverage_diffs.html_tmp"));
        reports.add(workspace.child(projectBase + "_rebuild.html_tmp"));
        reports.add(workspace.child(projectBase + "_full_report.html_tmp"));
        reports.add(workspace.child(projectBase + "_metrics_report.html_tmp"));

        StringBuilder summary = new StringBuilder();
        for (FilePath report : reports) {
            if (report.exists()) {
                summary.append("<hr style=\"height:5px;border-width:0;"
                    + "color:gray;background-color:gray\"> ");
                summary.append(readWithFallback(report, listener));
            }
        }
        return summary.toString();
    }

    /**
     * Reads a report fragment using the former Groovy script's charset policy.
     *
     * @param report report fragment
     * @param listener build logger
     * @return decoded report text
     * @throws IOException if the report cannot be read
     * @throws InterruptedException if the agent operation is interrupted
     */
    private String readWithFallback(final FilePath report,
            final TaskListener listener) throws IOException, InterruptedException {
        byte[] bytes;
        try (InputStream stream = report.read()) {
            bytes = stream.readAllBytes();
        }

        for (Charset charset : reportCharsets(bytes)) {
            try {
                CharsetDecoder decoder = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
                CharBuffer decoded = decoder.decode(ByteBuffer.wrap(bytes));
                listener.getLogger().println("[VectorCAST Post Build] Decoded "
                    + report.getName() + " with charset: " + charset.name());
                return decoded.toString();
            } catch (CharacterCodingException ignored) {
                // Try the next legacy charset.
            }
        }

        Charset fallback = StandardCharsets.ISO_8859_1;
        listener.getLogger().println("[VectorCAST Post Build] Fall back decode "
            + report.getName() + " with charset: " + fallback.name());
        return new String(bytes, fallback);
    }

    /**
     * Returns the supported charsets, preferring a detected Unicode byte order
     * mark exactly as the former Groovy script did.
     *
     * @param bytes report contents
     * @return ordered decoding candidates
     */
    private List<Charset> reportCharsets(final byte[] bytes) {
        List<Charset> charsets = new ArrayList<>(List.of(
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_16LE,
            StandardCharsets.UTF_16BE,
            Charset.forName("GB18030"),
            Charset.forName("GBK"),
            Charset.forName("windows-31j"),
            Charset.forName("Shift_JIS"),
            Charset.forName("EUC-JP"),
            Charset.forName("ISO-2022-JP"),
            Charset.forName("MS949"),
            Charset.forName("x-windows-949"),
            Charset.forName("EUC-KR"),
            Charset.forName("ISO-2022-KR"),
            Charset.forName("windows-1252"),
            StandardCharsets.ISO_8859_1
        ));
        Charset bom = bomCharset(bytes);
        if (bom != null) {
            charsets.remove(bom);
            charsets.add(0, bom);
        }
        return charsets;
    }

    /**
     * Detects a supported Unicode byte order mark.
     *
     * @param bytes report contents
     * @return detected charset, or null when no supported mark is present
     */
    private Charset bomCharset(final byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xef
                && bytes[1] == (byte) 0xbb && bytes[2] == (byte) 0xbf) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xff
                && bytes[1] == (byte) 0xfe) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2 && bytes[0] == (byte) 0xfe
                && bytes[1] == (byte) 0xff) {
            return StandardCharsets.UTF_16BE;
        }
        return null;
    }

    /**
     * Adds both the build-page summary card and the build badge previously
     * supplied by Groovy Postbuild.
     *
     * @param run current build
     * @param result result classification
     * @param message user-visible classification
     */
    private void addStatusActions(final Run<?, ?> run, final Result result,
            final String message) {
        String icon = result == Result.FAILURE ? ERROR_ICON : WARNING_ICON;
        run.addAction(new BadgeSummaryAction(null, icon, message,
            "", "", "", ""));
        run.addAction(new BadgeAction(null, icon, message,
            "", "", "", ""));
    }

    /** Descriptor for the post-build publisher. */
    @Extension
    public static final class DescriptorImpl
            extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(
                final Class<? extends AbstractProject> projectType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Apply VectorCAST post-build result policy";
        }
    }

    /** Immutable build-log rule. */
    private record LogRule(String text, Result result, String message) {
        /**
         * Tests a single log line against the rule.
         *
         * @param line build-log line
         * @return true if the rule matches
         */
        boolean matches(final String line) {
            return line.contains(text);
        }
    }
}
