package com.vectorcast.plugins.vectorcastexecution;

import com.cloudbees.hudson.plugins.folder.Folder;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VectorCASTJobRootTest {

    /** Jenkins rule for testing. */
    @Rule
    public JenkinsRule r = new JenkinsRule();

    /**
     * Ensure the global RootAction extension loads.
     */
    @Test
    public void loadsGlobalRootAction() {
        boolean hasRootAction = r.jenkins.getExtensionList(hudson.model.RootAction.class).stream()
            .anyMatch(a -> a instanceof VectorCASTJobRoot);

        assertThat("VectorCASTJobRoot should be registered as a RootAction", hasRootAction, is(true));
    }

    /**
     * Ensure a folder gets a VectorCASTFolderAction attached by the factory.
     * @throws Exception 
     */
    @Test
    public void folderGetsVectorCASTAction() throws Exception {
        Folder f = r.jenkins.createProject(Folder.class, "myFolder");

        System.out.println("Folder actions:");
        f.getAllActions().forEach(a -> System.out.println(" - " + a.getClass()));

        boolean hasFolderAction = f.getAllActions().stream()
            .anyMatch(a -> a instanceof VectorCASTFolderAction);

        assertThat("Folder should contain a VectorCASTFolderAction", hasFolderAction, is(true));
    }

    /**
     * Ensure the folder VectorCAST page renders correctly (index.jelly).
     * @throws Exception 
     */
    @Test
    public void folderVectorCASTPageRenders() throws Exception {
        Folder f = r.jenkins.createProject(Folder.class, "myFolder");

        // Try to load the VectorCAST folder action page
        r.createWebClient().goTo("job/myFolder/VectorCAST/");
    }
}
