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
import org.jvnet.hudson.test.JenkinsRule;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class VectorCASTCommandTest {
    
    @Test
    public void executesThePlatformAppropriateCommand(JenkinsRule rule)
            throws Exception {
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            FreeStyleProject project = rule.createFreeStyleProject();
            VectorCASTCommand command = new VectorCASTCommand(
                "echo \"Windows Command\"", "Unix Command");
            project.getBuildersList().add(command);
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            rule.assertBuildStatus(Result.SUCCESS, build);
            assertEquals("echo \"Windows Command\"",
                command.getWinCommand());
            assertEquals("Unix Command", command.getUnixCommand());
            assertTrue(command.getDescriptor().isApplicable(
                FreeStyleProject.class));

            FreeStyleProject failingProject = rule.createFreeStyleProject();
            failingProject.getBuildersList().add(new VectorCASTCommand(
                "exit /b 1", "Unix Command"));
            rule.assertBuildStatus(Result.FAILURE,
                failingProject.scheduleBuild2(0).get());
        } else {
            FreeStyleProject project = rule.createFreeStyleProject();
            VectorCASTCommand command = new VectorCASTCommand(
                "Windows Command", "echo \"Unix Command\"");
            project.getBuildersList().add(command);
            FreeStyleBuild build = project.scheduleBuild2(0).get();
            rule.assertBuildStatus(Result.SUCCESS, build);
            assertEquals("Windows Command", command.getWinCommand());
            assertEquals("echo \"Unix Command\"", command.getUnixCommand());
            assertTrue(command.getDescriptor().isApplicable(
                FreeStyleProject.class));

            FreeStyleProject failingProject = rule.createFreeStyleProject();
            failingProject.getBuildersList().add(new VectorCASTCommand(
                "Windows Command", "exit 1"));
            rule.assertBuildStatus(Result.FAILURE,
                failingProject.scheduleBuild2(0).get());
        }
    }
}
