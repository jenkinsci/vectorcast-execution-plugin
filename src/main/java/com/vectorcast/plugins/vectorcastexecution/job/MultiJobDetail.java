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
 * Details about a manage job
 */
public class MultiJobDetail {
    /** Project name */
    private String projName;
    /** Level */
    private String level;
    /** Environment */
    private String environment;
    /** Compiler */
    private String compiler;
    /**
     * Constructor
     * @param source source
     * @param platform platform
     * @param compiler compiler
     * @param testSuite testSuite
     * @param environment environment
     */
    public MultiJobDetail(String source, String platform, String compiler, String testSuite, String environment) {
        this.projName = compiler + "_" + testSuite + "_" + environment;
        if (source != null && platform != null) {
            this.level = source + "/" + platform + "/" + compiler + "/" + testSuite;
        } else {
            this.level = compiler + "/" + testSuite;
        }
        this.environment = environment;
        this.compiler = compiler;
    }
    /**
     * Get compiler
     * @return compiler
     */
    public String getCompiler() {
        return compiler;
    }
    /**
     * Get project name
     * @return project name
     */
    public String getProjectName() {
        return projName;
    }
    /**
     * Get level
     * @return level
     */
    public String getLevel() {
        return level;
    }
    /**
     * Get environment
     * @return environment
     */
    public String getEnvironment() {
        return environment;
    }
}
