package com.vectorcast.plugins.vectorcastexecution;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.Item;
import hudson.model.Label;
import hudson.security.Permission;
import java.net.URL;
import java.util.List;
import jenkins.model.Jenkins;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.Page;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UI contract tests for the two VectorCAST job-creation forms. */
@WithJenkins
class JobCreationUiTest {
    private static final List<String> COMMON_CONTROLS = List.of(
        "manageProjectName", "nodeLabel", "jobName", "waitLoops", "waitTime",
        "useCiLicense", "useCoverageHistory", "useStrictTestcaseImport",
        "useRGW3", "useImportedResults", "importedResults",
        "externalResultsFilename", "pclpCommand", "pclpResultsPattern",
        "squoreCommand");

    @Test
    void rendersSharedAndJobSpecificControlsWithoutLegacyCoverageUi(
            final JenkinsRule rule) throws Exception {
        configureAuthorizedUser(rule);
        rule.createSlave(Label.get("Host_Test_Node_Linux"));
        rule.createSlave(Label.get("Host_Test_Node_Win"));
        var client = rule.createWebClient().login("devel");
        Folder folder = rule.jenkins.createProject(Folder.class, "ui-contract");

        for (FormContract contract : List.of(
                new FormContract("single-job", List.of(
                    "environmentSetupWin", "executePreambleWin",
                    "environmentTeardownWin", "environmentSetupUnix",
                    "executePreambleUnix", "environmentTeardownUnix",
                    "useCBT", "scm"),
                    List.of("optionClean", "optionUseReporting",
                        "optionErrorLevel", "optionHtmlBuildDesc",
                        "optionExecutionReport", "useParameters", "sharedArtifactDir",
                        "maxParallel", "environmentSetup", "scmSnippet",
                        "postSCMCheckoutCommands"), VectorCASTJobSingle.class),
                new FormContract("pipeline-job", List.of(
                    "useCBT", "useParameters", "sharedArtifactDir",
                    "maxParallel", "environmentSetup", "executePreamble",
                    "environmentTeardown", "singleCheckout", "scmSnippet",
                    "postSCMCheckoutCommands"),
                    List.of("environmentSetupWin", "environmentSetupUnix",
                        "optionClean", "optionUseReporting", "optionErrorLevel",
                        "optionHtmlBuildDesc", "optionExecutionReport", "scm"),
                    VectorCASTJobPipeline.class))) {
            HtmlPage page = client.goTo("job/" + folder.getName()
                + "/VectorCAST/" + contract.path() + "/");
            HtmlForm form = page.getFormByName("create");

            assertFalse(page.asNormalizedText().contains("Coverage Reporting"));
            assertFalse(page.asNormalizedText().contains("Use Jenkins Coverage Plugin"));
            assertControls(form, COMMON_CONTROLS, true);
            assertControls(form, contract.presentControls(), true);
            assertControls(form, contract.absentControls(), false);
            String autocompleteUrl = form.getInputByName("nodeLabel")
                .getAttribute("autocompleteurl");
            assertTrue(autocompleteUrl.endsWith("/descriptorByName/"
                + contract.jobType().getName() + "/autoCompleteNodeLabel"),
                () -> "Unexpected autocomplete URL: " + autocompleteUrl);
            Page candidates = client.getPage(new URL(page.getUrl(),
                autocompleteUrl + "?value=Host_"));
            String candidatesContent = candidates.getWebResponse()
                .getContentAsString();
            assertTrue(candidatesContent.contains("Host_Test_Node_Linux"));
            assertTrue(candidatesContent.contains("Host_Test_Node_Win"));
        }
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

    private void assertControls(final HtmlForm form,
            final List<String> names, final boolean expected) {
        for (String name : names) {
            boolean present = !form.getByXPath(".//*[@name='" + name + "']")
                .isEmpty();
            if (expected) {
                assertTrue(present, () -> "Missing form control: " + name);
            } else {
                assertFalse(present, () -> "Unexpected form control: " + name);
            }
        }
    }

    private record FormContract(String path, List<String> presentControls,
            List<String> absentControls, Class<?> jobType) {
    }
}
