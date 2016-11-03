package com.vectorcast.plugins.vectorcastexecution;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Action;
import hudson.model.Describable;
import hudson.scm.NullSCM;
import hudson.scm.SCM;

import java.util.logging.Logger;

import jenkins.model.Jenkins;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class JobBase implements ExtensionPoint, Action, Describable<JobBase> {

    private SCM scm;
    public JobBase() {
        scm = new NullSCM();
    }
    public SCM getTheScm() {
        return scm;
    }
    public void setTheScm(SCM scm) {
        this.scm = scm;
    }
    @Override
    public String getIconFileName() {
        return "/plugin/vectorcast-execution/icons/vector_favicon.png";
    }

    public String getUrlName() {
        return getClass().getSimpleName();
    }

    /**
     * Default display name.
     */
    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    public JobBaseDescriptor getDescriptor() {
        return (JobBaseDescriptor) Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Returns all the registered {@link JobBase}s.
     */
    public static ExtensionList<JobBase> all() {
        return Jenkins.getInstance().getExtensionList(JobBase.class);
    }

}
