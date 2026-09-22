package com.vectorcast.plugins.vectorcastexecution.job;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Renders the stable, plugin-owned header of a generated Pipeline script. */
public final class PipelineScriptRenderer {
    private static final String TEMPLATE = "pipeline-header.groovy.template";

    private PipelineScriptRenderer() {
    }

    /** Renders a Pipeline header using named placeholders. */
    public static String render(final PipelineJobConfiguration config)
            throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("manageProject", groovySingle(config.manageProjectName()));
        values.put("environmentSetup", groovyString(config.environmentSetup()));
        values.put("buildPreamble", groovyString(config.executePreamble()));
        values.put("environmentTeardown", groovyString(config.environmentTeardown()));
        values.put("scmStep", config.pipelineScm());
        values.put("usingScm", Boolean.toString(!config.pipelineScm().isEmpty()));
        values.put("postScmCheckout", groovyString(
            config.postScmCheckoutCommands()));
        values.put("sharedArtifactDirectory", groovyString(
            config.sharedArtifactDirectory()));
        values.put("agentLabel", groovySingle(config.nodeLabel()));
        values.put("waitTime", groovySingle(Long.toString(config.waitTime())));
        values.put("waitLoops", groovySingle(Long.toString(config.waitLoops())));
        values.put("maxParallel", Long.toString(config.maxParallel()));
        values.put("singleCheckout", Boolean.toString(config.singleCheckout()));
        values.put("ciLicense", groovyString(config.useCiLicense() ? "--ci" : ""));
        values.put("cbt", groovyString(config.useCBT() ? "--incremental" : ""));
        values.put("pluginVersion", groovySingle(config.pluginVersion()));
        values.put("pclpEnabled", Boolean.toString(!config.pclpCommand().isEmpty()));
        values.put("pclpCommand", groovySingle(config.pclpCommand()));
        values.put("pclpResultsPattern", groovySingle(config.pclpResultsPattern()));
        values.put("squoreEnabled", Boolean.toString(!config.squoreCommand().isEmpty()));
        values.put("squoreCommand", groovyString(config.squoreCommand()));
        values.put("coverageHistory", Boolean.toString(config.useCoverageHistory()));
        values.put("strictImport", Boolean.toString(config.useStrictImport()));
        values.put("rgw3", Boolean.toString(config.useRGW3()));
        values.put("importedResults", Boolean.toString(config.useImportedResults()));
        values.put("localImportedResults", Boolean.toString(
            config.useLocalImportedResults()));
        values.put("externalImportedResults", Boolean.toString(
            config.useExternalImportedResults()));
        values.put("externalResultsFilename", groovyString(
            config.externalResultsFilename()));

        String rendered = readTemplate();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}",
                entry.getValue());
        }
        if (rendered.contains("{{")) {
            throw new IOException("Pipeline header template has an unresolved placeholder");
        }
        return rendered;
    }

    private static String readTemplate() throws IOException {
        try (InputStream input = PipelineScriptRenderer.class
                .getResourceAsStream(TEMPLATE)) {
            if (input == null) {
                throw new IOException("Missing Pipeline header template: " + TEMPLATE);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String groovySingle(final String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static String groovyString(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\")
            .replace("\"", "\\\"").replace("$", "\\$")
            .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}
