package com.vectorcast.plugins.vectorcastexecution;

import com.vectorcast.plugins.vectorcastexecution.JobBase;
import hudson.model.Descriptor;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class JobBaseDescriptor extends Descriptor<JobBase> {
    @Override
    public String getDisplayName() {
        return clazz.getSimpleName();
    }
}
