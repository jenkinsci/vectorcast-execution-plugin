package com.vectorcast.plugins.vectorcastexecution;

import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import org.htmlunit.html.HtmlPage;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class VectorCASTJobRootTest {

    @Test
    public void loadsAsExtension_andHasUrl(JenkinsRule rule) {
        var list = rule.jenkins.getExtensionList(VectorCASTJobRoot.class);
        assertThat("extension should load", list, is(not(empty())));
        VectorCASTJobRoot action = list.get(0);
        assertThat(action.getUrlName(), is("VectorCAST"));
        assertThat(action.getDisplayName(), is("VectorCAST"));
        assertThat(action.getIconFileName(), is("/plugin/vectorcast-execution/icons/vector_favicon_bw.png"));
        action.getDynamic("VectorCAST");
    }

    @Test
    public void rendersIndexJelly(JenkinsRule rule) throws Exception {
        var wc = rule.createWebClient();
        HtmlPage page = wc.goTo("VectorCAST");           // same as "/my-action/"
        assertThat(page.getTitleText(), containsString("VectorCAST"));
        assertThat(page.asNormalizedText(), containsString("VectorCAST Jobs"));
    }
}
