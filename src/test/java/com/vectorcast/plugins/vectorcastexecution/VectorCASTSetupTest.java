package com.vectorcast.plugins.vectorcastexecution;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.jvnet.hudson.test.JenkinsRule;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class VectorCASTSetupTest {

    @Test
    public void copiesScriptsIntoWorkspacePrintsVersionAndRoundTripsConfig(
            JenkinsRule rule) throws Exception {
        FreeStyleProject p = rule.createFreeStyleProject();
        p.getBuildersList().add(new VectorCASTSetup());

        FilePath ws = rule.jenkins.getWorkspaceFor(p);
        assertNotNull(ws);
        ws.child("xml_data").mkdirs();
        FilePath staleXml = ws.child("xml_data/stale-result.xml");
        staleXml.write("<stale/>", "UTF-8");

        var b = rule.buildAndAssertSuccess(p);

        // script root was created
        assertTrue(ws.child("vc_scripts").exists(), "vc_scripts dir should exist");
        // our test resource was copied
        assertTrue(ws.child("vc_scripts/baseJenkinsfile.groovy").exists(), "baseJenkinsfile.groovy should be copied");
        assertFalse(staleXml.exists(), "stale XML results should be removed");

        // version line is printed (dont assert the exact version string)
        rule.assertLogContains("[VectorCAST Execution Version]:", b);

        FreeStyleProject roundTripProject = rule.createFreeStyleProject();
        VectorCASTSetup before = new VectorCASTSetup();
        roundTripProject.getBuildersList().add(before);
        assertTrue(before.getDescriptor().isApplicable(FreeStyleProject.class));

        rule.configRoundtrip(roundTripProject);

        VectorCASTSetup after = roundTripProject.getBuildersList()
            .get(VectorCASTSetup.class);
        rule.assertEqualDataBoundBeans(before, after);
    }
}
