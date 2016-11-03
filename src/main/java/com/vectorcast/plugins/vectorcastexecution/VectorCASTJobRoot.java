package com.vectorcast.plugins.vectorcastexecution;

import com.vectorcast.plugins.vectorcastexecution.JobBase;
import com.vectorcast.plugins.vectorcastexecution.Messages;
import hudson.Extension;
import hudson.model.RootAction;

import java.util.List;
import jenkins.model.Jenkins;

/**
 * Entry point to all the UI samples.
 * 
 * @author Kohsuke Kawaguchi
 */
@Extension
public class VectorCASTJobRoot implements RootAction/*, ModelObjectWithContextMenu*/ {
    public String getIconFileName() {
        // Only display if user has admin rights
        if (Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return "/plugin/vectorcast-execution/icons/vector_favicon.png";
        } else {
            return null;
        }
    }

    public String getDisplayName() {
        return Messages.VectorCASTCommand_AddVCJob();
    }

    public String getUrlName() {
        return "VectorCAST";
    }

    public JobBase getDynamic(String name) {
        for (JobBase ui : getAll())
            if (ui.getUrlName().equals(name))
                return ui;
        return null;
    }

    public List<JobBase> getAll() {
        return JobBase.all();
    }
}
