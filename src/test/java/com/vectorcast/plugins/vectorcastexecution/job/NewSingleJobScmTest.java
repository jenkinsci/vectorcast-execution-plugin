package com.vectorcast.plugins.vectorcastexecution.job;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import hudson.security.Permission;
import java.util.List;
import jenkins.model.Jenkins;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlRadioButtonInput;
import org.htmlunit.html.HtmlSubmitInput;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises Jenkins' live SCM radio-list binding for a Freestyle job. */
@WithJenkins
class NewSingleJobScmTest {
    private static final String REPOSITORY_URL =
        "https://example.invalid/vectorcast.git";

    @Test
    void createsFreestyleJobWithGitScmFromTheRenderedForm(JenkinsRule rule)
            throws Exception {
        configureAuthorizedUser(rule);
        Folder folder = rule.jenkins.createProject(Folder.class, "scm-folder");
        var client = rule.createWebClient().login("devel");
        HtmlPage page = client.goTo("job/" + folder.getName()
            + "/VectorCAST/single-job/");
        HtmlForm form = page.getFormByName("create");

        input(form, "manageProjectName").setValueAttribute("project.vcm");
        selectGitRadio(form).setChecked(true);
        inputContaining(form, "url").setValueAttribute(REPOSITORY_URL);

        HtmlPage result = form.getElementsByTagName("input").stream()
            .filter(HtmlSubmitInput.class::isInstance)
            .map(HtmlSubmitInput.class::cast)
            .findFirst().orElseThrow().click();

        assertTrue(folder.getItem("project_vcast_single") != null,
            () -> "submission URL=" + result.getUrl() + "; folder items="
                + folder.getItems().stream().map(item -> item.getName())
                    .toList() + "; response=" + result.asNormalizedText());
        FreeStyleProject project = assertInstanceOf(FreeStyleProject.class,
            folder.getItem("project_vcast_single"));
        GitSCM scm = assertInstanceOf(GitSCM.class, project.getScm());
        assertEquals(REPOSITORY_URL,
            scm.getUserRemoteConfigs().get(0).getUrl());
        assertFalse(scm.getBranches().isEmpty());
    }

    private void configureAuthorizedUser(final JenkinsRule rule) {
        rule.jenkins.setSecurityRealm(rule.createDummySecurityRealm());
        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
        strategy.grant(Jenkins.READ).everywhere().to("devel");
        for (Permission permission : Item.PERMISSIONS.getPermissions()) {
            strategy.grant(permission).everywhere().to("devel");
        }
        rule.jenkins.setAuthorizationStrategy(strategy);
    }

    private HtmlRadioButtonInput selectGitRadio(final HtmlForm form) {
        List<HtmlRadioButtonInput> radios = form.getInputsByName("scm")
            .stream().filter(HtmlRadioButtonInput.class::isInstance)
            .map(HtmlRadioButtonInput.class::cast).toList();
        return radios.stream()
            .filter(radio -> radio.getParentNode().asNormalizedText()
                .toLowerCase().contains("git"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Git SCM radio was not rendered"));
    }

    private HtmlInput input(final HtmlForm form, final String name) {
        return form.getInputByName(name);
    }

    private HtmlInput inputContaining(final HtmlForm form, final String text) {
        return form.getElementsByTagName("input").stream()
            .filter(HtmlInput.class::isInstance)
            .map(HtmlInput.class::cast)
            .filter(input -> input.getNameAttribute().toLowerCase()
                .contains(text.toLowerCase()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No input containing " + text));
    }
}
