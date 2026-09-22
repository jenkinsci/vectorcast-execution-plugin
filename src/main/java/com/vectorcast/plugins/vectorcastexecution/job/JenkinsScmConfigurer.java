package com.vectorcast.plugins.vectorcastexecution.job;

import hudson.model.Project;
import hudson.model.Descriptor;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.scm.SCMS;
import java.io.IOException;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest;

/** Jenkins-specific SCM form binding kept outside the job configuration model. */
final class JenkinsScmConfigurer {
    private JenkinsScmConfigurer() {
    }

    /** Parses and attaches the SCM configured by Jenkins' descriptor form. */
    static SCM configure(final StaplerRequest request,
            final Project<?, ?> project) throws ServletException, IOException,
            Descriptor.FormException {
        SCM scm = SCMS.parseSCM(request, project);
        if (scm == null) {
            scm = new NullSCM();
        }
        project.setScm(scm);
        return scm;
    }
}
