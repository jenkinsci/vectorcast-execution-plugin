package com.vectorcast.plugins.vectorcastexecution.job;

/** Typed values used to render the generated Pipeline header. */
public record PipelineJobConfiguration(
        String manageProjectName,
        String environmentSetup,
        String executePreamble,
        String environmentTeardown,
        String pipelineScm,
        String postScmCheckoutCommands,
        String sharedArtifactDirectory,
        String nodeLabel,
        long waitTime,
        long waitLoops,
        long maxParallel,
        boolean singleCheckout,
        boolean useCiLicense,
        boolean useCBT,
        String pluginVersion,
        String pclpCommand,
        String pclpResultsPattern,
        String squoreCommand,
        boolean useCoverageHistory,
        boolean useStrictImport,
        boolean useRGW3,
        boolean useImportedResults,
        boolean useLocalImportedResults,
        boolean useExternalImportedResults,
        String externalResultsFilename) {
}
