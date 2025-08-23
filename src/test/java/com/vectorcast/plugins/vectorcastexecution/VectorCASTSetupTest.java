package com.vectorcast.plugins.vectorcastexecution;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class VectorCASTSetupTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void copiesScriptsIntoWorkspace_andPrintsVersion() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        p.getBuildersList().add(new VectorCASTSetup());

        var b = r.buildAndAssertSuccess(p);

        FilePath ws = r.jenkins.getWorkspaceFor(p);
        assertNotNull(ws);
        // script root was created
        assertTrue("vc_scripts dir should exist", ws.child("vc_scripts").exists());
        // our test resource was copied
        assertTrue("baseJenkinsfile.groovy should be copied",
                ws.child("vc_scripts/baseJenkinsfile.groovy").exists());

        // version line is printed (dont assert the exact version string)
        r.assertLogContains("[VectorCAST Execution Version]:", b);
    }

    @Test
    public void configRoundTrip_preservesDefaults() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        VectorCASTSetup before = new VectorCASTSetup();
        p.getBuildersList().add(before);

        r.configRoundtrip(p);

        VectorCASTSetup after = p.getBuildersList().get(VectorCASTSetup.class);
        r.assertEqualDataBoundBeans(before, after);
    }
}
