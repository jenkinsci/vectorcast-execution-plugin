package com.vectorcast.plugins.vectorcastexecution;

import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import org.htmlunit.html.HtmlPage;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VectorCASTJobRootTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void loadsAsExtension_andHasUrl() {
        var list = r.jenkins.getExtensionList(VectorCASTJobRoot.class);
        assertThat("extension should load", list, is(not(empty())));
        VectorCASTJobRoot action = list.get(0);
        assertThat(action.getUrlName(), is("VectorCAST"));
        assertThat(action.getDisplayName(), is("VectorCAST"));
        assertThat(action.getIconFileName(), is("/plugin/vectorcast-execution/icons/vector_favicon_bw.png"));
        action.getDynamic("VectorCAST");
    }

    @Test
    public void rendersIndexJelly() throws Exception {
        var wc = r.createWebClient();
        HtmlPage page = wc.goTo("VectorCAST");           // same as "/my-action/"
        assertThat(page.getTitleText(), containsString("VectorCAST"));
        assertThat(page.asNormalizedText(), containsString("VectorCAST Jobs"));
    }
}
