package com.vectorcast.plugins.vectorcastexecution.common;

import hudson.model.AutoCompletionCandidates;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTJobSingle;
import com.vectorcast.plugins.vectorcastexecution.VectorCASTJobPipeline;
import hudson.model.Label;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.Exception;

/** Tests shared VectorCAST utility behavior exposed to Jenkins views. */
class VcastUtilsTest {

    @Test
    void createsAndCachesTheViewPermission() {
        Permission permission = VcastUtils.getViewPermission();

        assertEquals("View", permission.name);
        assertSame(Jenkins.ADMINISTER, permission.impliedBy);
        assertSame(permission, VcastUtils.getViewPermission());
    }

    @Test
    @WithJenkins
    void completesOnlyMatchingJenkinsNodeLabels(JenkinsRule rule) throws java.lang.Exception {
        rule.createSlave(Label.get("vectorcast-linux"));
        rule.createSlave(Label.get("vectorcast-windows"));
        rule.createSlave(Label.get("unrelated-agent"));

        AutoCompletionCandidates candidates =
            VcastUtils.completeNodeLabel("vectorcast-");

        assertTrue(candidates.getValues().contains("vectorcast-linux"));
        assertTrue(candidates.getValues().contains("vectorcast-windows"));
        assertFalse(candidates.getValues().contains("unrelated-agent"));
        assertEquals(candidates.getValues(), rule.jenkins.getDescriptorByType(
            VectorCASTJobSingle.DescriptorImpl.class)
            .doAutoCompleteNodeLabel("vectorcast-").getValues());
        assertEquals(candidates.getValues(), rule.jenkins.getDescriptorByType(
            VectorCASTJobPipeline.DescriptorImpl.class)
            .doAutoCompleteNodeLabel("vectorcast-").getValues());
    }
}
