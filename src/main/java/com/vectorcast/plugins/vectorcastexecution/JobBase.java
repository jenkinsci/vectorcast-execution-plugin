/*
 * The MIT License
 *
 * Copyright 2016 Vector Software, East Greenwich, Rhode Island USA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.vectorcast.plugins.vectorcastexecution;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Action;
import hudson.model.Describable;
import hudson.scm.NullSCM;
import hudson.scm.SCM;

import jenkins.model.Jenkins;

/**
 * Base job
 */
public abstract class JobBase implements ExtensionPoint, Action, Describable<JobBase> {
    /** SCM to use initially */
    private SCM scm;
    /**
     * Default Constructor
     */
    public JobBase() {
        scm = new NullSCM();
    }
    /**
     * Get the SCM
     * @return the SCM
     */
    public SCM getTheScm() {
        return scm;
    }
    /**
     * Set the SCM object
     * @param scm new SCM
     */
    public void setTheScm(SCM scm) {
        this.scm = scm;
    }
    /**
     * Default icon name
     * @return icon name
     */
    @Override
    public String getIconFileName() {
        return "/plugin/vectorcast-execution/icons/vector_favicon.png";
    }
    /**
     * Default URL name
     * @return url name
     */
    @Override
    public String getUrlName() {
        return getClass().getSimpleName();
    }
    /**
     * Default display name.
     * @return name
     */
    @Override
    public String getDisplayName() {
        return getClass().getSimpleName();
    }
    /**
     * Default descriptor
     * @return descriptor
     */
    @Override
    public JobBaseDescriptor getDescriptor() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        } else {
            return (JobBaseDescriptor) instance.getDescriptorOrDie(getClass());
        }
    }
    /**
     * Returns all the registered {@link JobBase}s.
     * @return all extensions based on JobBase
     */
    public static ExtensionList<JobBase> all() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            return null;
        } else {
            return instance.getExtensionList(JobBase.class);
        }
    }
}
