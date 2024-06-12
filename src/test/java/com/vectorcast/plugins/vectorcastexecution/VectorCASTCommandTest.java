/*
 * The MIT License
 *
 * Copyright 2024 Vector Informatik, GmbH.
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

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class VectorCASTCommandTest {
    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();
    
    @Test
    public void testOnWindows() throws Exception {
        // Only applies on Windows
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.getBuildersList().add(new VectorCASTCommand("echo \"Windows Command\"", "Unix Command"));
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            jenkins.assertBuildStatus(Result.SUCCESS, build);
        }
    }
    
    @Test
    public void testOnLinux() throws Exception {
        // Only applies on Windows
        if (System.getProperty("os.name").toLowerCase().indexOf("win") == -1) {
            FreeStyleProject project = jenkins.createFreeStyleProject();
            project.getBuildersList().add(new VectorCASTCommand("Windows Command", "echo \"Unix Command\""));
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            jenkins.assertBuildStatus(Result.SUCCESS, build);
        }
    }
}
