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
    public void copiesScriptsIntoWorkspace_andPrintsVersion(JenkinsRule rule) throws Exception {
        FreeStyleProject p = rule.createFreeStyleProject();
        p.getBuildersList().add(new VectorCASTSetup());

        var b = rule.buildAndAssertSuccess(p);

        FilePath ws = rule.jenkins.getWorkspaceFor(p);
        assertNotNull(ws);
        // script root was created
        assertTrue(ws.child("vc_scripts").exists(), "vc_scripts dir should exist");
        // our test resource was copied
        assertTrue(ws.child("vc_scripts/baseJenkinsfile.groovy").exists(), "baseJenkinsfile.groovy should be copied");

        // version line is printed (dont assert the exact version string)
        rule.assertLogContains("[VectorCAST Execution Version]:", b);
    }

    @Test
    public void configRoundTrip_preservesDefaults(JenkinsRule rule) throws Exception {
        FreeStyleProject p = rule.createFreeStyleProject();
        VectorCASTSetup before = new VectorCASTSetup();
        p.getBuildersList().add(before);

        rule.configRoundtrip(p);

        VectorCASTSetup after = p.getBuildersList().get(VectorCASTSetup.class);
        rule.assertEqualDataBoundBeans(before, after);
    }
}
