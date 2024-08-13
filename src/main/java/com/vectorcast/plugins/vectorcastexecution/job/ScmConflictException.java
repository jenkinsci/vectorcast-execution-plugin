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
package com.vectorcast.plugins.vectorcastexecution.job;

/**
 * Exception raised if job being created already exists.
 */
public class ScmConflictException extends Exception {
    /** serial Version UID. */
    private static final long serialVersionUID = -1889207053537494684L;

    /** Name of project. */
    private final String scmSnippet;
    /** Path to project. */
    private final String pathToManageProject;
    /**
     * Constructor.
     * @param snip scm Snippet entered
     * @param path path to manage project
     */
    public ScmConflictException(final String snip, final String path) {
        this.scmSnippet = snip;
        this.pathToManageProject = path;
    }
    /**
     * Get scm snippet name.
     * @return scmSnippet name
     */
    public String getScmSnippet() {
        return scmSnippet;
    }

    /**
     * Get path to manage project name.
     * @return scmSnippet name
     */
    public String getPathToManageProject() {
        return pathToManageProject;
    }
}
