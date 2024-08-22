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
public class BadOptionComboException extends Exception {
    /** serial Version UID. */
    private static final long serialVersionUID = 4219732918348691554L;

    /** Name of option one. */
    private final String option1;

    /** Name of option two. */
    private final String option2;

    /**
     * Constructor.
     * @param opt1 option1 conflicts with
     * @param opt2 this option
     */
    public BadOptionComboException(final String opt1, final String opt2) {
        this.option1 = opt1;
        this.option2 = opt2;
    }
    /**
     * Get option one name.
     * @return option one name
     */
    public String getOption1() {
        return option1;
    }
    /**
     * Get option two name.
     * @return option two name
     */
    public String getOption2() {
        return option2;
    }
}
