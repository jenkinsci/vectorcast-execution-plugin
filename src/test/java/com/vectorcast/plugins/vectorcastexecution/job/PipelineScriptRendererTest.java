package com.vectorcast.plugins.vectorcastexecution.job;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineScriptRendererTest {
    @Test
    void rendersNamedValuesWithGroovySafeStringLiterals() throws Exception {
        PipelineJobConfiguration configuration = new PipelineJobConfiguration(
            "project's.vcm", "source setup.sh\nexport MODE=$MODE",
            "run \"preamble\"", "cleanup", "git 'https://example.invalid'",
            "git submodule update", "--workspace=C:/shared", "linux-agent",
            30, 2, 4, false, true, false, "0.81-SNAPSHOT",
            "lint", "lint.xml", "squore", true, true, false, true,
            false, true, "external.vcr");

        String header = PipelineScriptRenderer.render(configuration);

        assertTrue(header.contains("def VC_Manage_Project = 'project\\'s.vcm'"));
        assertTrue(header.contains("def VC_EnvSetup = \"source setup.sh\\n"
            + "export MODE=\\$MODE\""));
        assertTrue(header.contains("def VC_useCBT = \"\""));
        assertTrue(header.contains("def VC_useCILicense = \"--ci\""));
        assertTrue(header.contains("def scmStep () { git 'https://example.invalid' }"));
        assertFalse(header.contains("{{"));
    }
}
