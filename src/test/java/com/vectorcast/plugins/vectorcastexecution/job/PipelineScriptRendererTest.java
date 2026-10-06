package com.vectorcast.plugins.vectorcastexecution.job;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineScriptRendererTest {
    @Test
    void generatedJobsCreateIndexAfterReportsInWorkspaceRoot() throws Exception {
        for (String resourceName : new String[] {
                "/scripts/baselineSingleJobWindows.txt",
                "/scripts/baselineSingleJobLinux.txt",
                "/com/vectorcast/plugins/vectorcastexecution/default-scripts/VectorCASTMetrics.groovy"
        }) {
            try (InputStream resource = getClass().getResourceAsStream(resourceName)) {
                assertTrue(resource != null, resourceName + " must exist");
                String body = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(body.contains("vcast_exec.py"), resourceName);
                assertTrue(body.contains("--aggregate --metrics --fullstatus --fixup-reports"),
                    resourceName);
                if (resourceName.endsWith("VectorCASTMetrics.groovy")) {
                    assertTrue(body.contains("--noindex"), resourceName);
                    assertTrue(body.contains("VC.useCBT ? '--aggregate-rebuild' : ''"),
                        resourceName);
                    assertTrue(body.contains("create_index_html.py"), resourceName);
                    assertTrue(body.contains("--output-dir"), resourceName);
                    assertTrue(body.indexOf("create_index_html.py")
                        > body.indexOf("parallel_full_reports.py"), resourceName);
                } else {
                    assertFalse(body.contains("--noindex"), resourceName);
                }
            }
        }
    }

    @Test
    void generatedPipelineExecutesPerEnvironmentMetricsCommands() throws Exception {
        try (InputStream resource = getClass().getResourceAsStream(
                "/scripts/baseJenkinsfile.groovy")) {
            assertTrue(resource != null, "Pipeline body resource must exist");
            String body = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("runCommands(ret.cmds)"));
        }
    }

    @Test
    void environmentBuildUsesConfiguredSharedArtifactWorkspace() throws Exception {
        try (InputStream resource = getClass().getResourceAsStream(
                "/com/vectorcast/plugins/vectorcastexecution/default-scripts/VectorCASTExecution.groovy")) {
            assertTrue(resource != null, "Execution bridge resource must exist");
            String body = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(body.contains("${VC.sharedBldDir ?: ''} --level"));
        }
    }

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
        assertFalse(header.contains("VC_useCoveragePlugin"));
        assertFalse(header.contains("{{"));
    }
}
