package com.vectorcast.plugins.vectorcastexecution.job;

import net.sf.json.JSONObject;

/** Parsed, immutable values from a VectorCAST job creation form. */
public record JobCreationRequest(
        String manageProjectName,
        String jobName,
        String nodeLabel,
        String environmentSetupWin,
        String executePreambleWin,
        String environmentTeardownWin,
        String environmentSetupUnix,
        String executePreambleUnix,
        String environmentTeardownUnix,
        boolean optionUseReporting,
        int optionErrorLevel,
        String optionHtmlBuildDesc,
        boolean optionExecutionReport,
        boolean optionClean,
        long waitTime,
        long waitLoops,
        boolean useCiLicenses,
        boolean useStrictTestcaseImport,
        boolean useRGW3,
        boolean useImportedResults,
        boolean useLocalImportedResults,
        boolean useExternalImportedResults,
        String externalResultsFilename,
        boolean useCoverageHistory,
        long maxParallel,
        String pclpCommand,
        String pclpResultsPattern,
        String squoreCommand,
        boolean useCBT,
        String sharedArtifactDirectory,
        String pipelineSCM,
        boolean singleCheckout,
        String environmentSetup,
        String executePreamble,
        String environmentTeardown,
        String postSCMCheckoutCommands,
        boolean useParameters) {

    /** Converts Stapler's submitted JSON into normalized creation values. */
    public static JobCreationRequest parse(final JSONObject json)
            throws ExternalResultsFileException {
        JobFormData form = JobFormData.from(json);
        String manageProject = form.text("manageProjectName", "");
        if (manageProject.length() > 1000) {
            throw new IllegalArgumentException("manageProjectName too long > 1000");
        }
        manageProject = normalizeManageProjectName(manageProject);

        String name = form.text("jobName", null);
        if (name != null) {
            name = normalizeJobName(name);
        }
        String errorLevel = form.text("optionErrorLevel", "unstable").trim();
        int errorResult = switch (errorLevel) {
            case "failure" -> 2;
            case "unstable" -> 1;
            default -> 0;
        };

        boolean imported = form.flag("useImportedResults", false);
        JobFormData importedSection = form.section("importedResults");
        long importSource = imported ? importedSection.number("value", 0) : 0;
        boolean local = importSource == 1;
        boolean external = importSource == 2;
        String externalFile = "";
        if (external) {
            externalFile = importedSection.text("externalResultsFilename", "")
                .trim().replace('\\', '/');
            if (externalFile.isEmpty()) {
                throw new ExternalResultsFileException();
            }
        }

        String sharedDirectory = form.text("sharedArtifactDir", "").trim();
        if (!sharedDirectory.isEmpty()) {
            sharedDirectory = "--workspace="
                + sharedDirectory.replace('\\', '/');
        }

        return new JobCreationRequest(
            manageProject, name, form.text("nodeLabel", "").trim(),
            form.text("environmentSetupWin", ""),
            form.text("executePreambleWin", ""),
            form.text("environmentTeardownWin", ""),
            form.text("environmentSetupUnix", ""),
            form.text("executePreambleUnix", ""),
            form.text("environmentTeardownUnix", ""),
            form.flag("optionUseReporting", true), errorResult,
            form.text("optionHtmlBuildDesc", "HTML").trim(),
            form.flag("optionExecutionReport", true),
            form.flag("optionClean", false),
            form.number("waitTime", 30), form.number("waitLoops", 1),
            form.flag("useCiLicense", false),
            form.flag("useStrictTestcaseImport", true),
            form.flag("useRGW3", false), imported, local, external,
            externalFile, form.flag("useCoverageHistory", false),
            form.number("maxParallel", 0),
            form.text("pclpCommand", "").replace('\\', '/'),
            form.text("pclpResultsPattern", "").trim(),
            form.text("squoreCommand", "").replace('\\', '/'),
            form.flag("useCBT", true), sharedDirectory,
            form.text("scmSnippet", "").trim(),
            form.flag("singleCheckout", false),
            form.text("environmentSetup", null),
            form.text("executePreamble", null),
            form.text("environmentTeardown", null),
            form.text("postSCMCheckoutCommands", null),
            form.flag("useParameters", false));
    }

    static String normalizeJobName(final String input) {
        return input.trim().replaceAll("[^a-zA-Z0-9_]", "_");
    }

    static String normalizeManageProjectName(final String input) {
        if (input.isEmpty()) {
            return input;
        }
        String normalized = input.replace('\\', '/').trim();
        return normalized.toLowerCase().endsWith(".vcm")
            ? normalized : normalized + ".vcm";
    }
}
