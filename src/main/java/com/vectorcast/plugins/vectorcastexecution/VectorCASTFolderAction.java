/*
 * The MIT License
 *
 * Copyright 2025 Vector Informatik, GmbH.
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

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.model.Action;
import com.vectorcast.plugins.vectorcastexecution.common.VcastUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

 
/**
 * VectorCASTFolderAction implements actions.
 */

public class VectorCASTFolderAction implements Action {


    /** Folder information. */
    private final Folder folder;

    /** Constructor with folder as input.
     * @param inputFolder input folder
     */
    public VectorCASTFolderAction(final Folder inputFolder) {
        this.folder = inputFolder;
    }

    /** Gets the icon name.
     * @return String of Icon
     */
    @Override
    public String getIconFileName() {
        return VcastUtils.getVectorCASTIconFileName();
    }

    /**
     * Display name for the folder-level action/menu-item.
     * @return display name
     */
    @Override
    public String getDisplayName() {
        return "VectorCAST";
    }

    /**
     * Get name of top-level action/url.
     * @return url
     */
    @Override
    public String getUrlName() {
        return "VectorCAST";
    }
    /**
     * Get internal folder variable.
     * @return folder
     */
    public Folder getFolder() {
        return folder;
    }

    /**
     * Return Target "this" for render.
     * @return this
     */
    public Object getTarget() {
        return this;  // allows index.jelly to render
    }

    /**
     * Get full folder name variable.
     * @return full folder name
     */
    public String getFolderFullName() {
        return folder != null ? folder.getFullName() : "";
    }

    /**
     * Get version of plugin.
     * @return version of the plugin
     */
    public String getVersion() {
        return VcastUtils.getVersion().orElse("Unknown");
    }

    /**
     * Get folder name variable.
     * @return folder name
     */
    public String getFolderName() {
        return folder != null ? folder.getName() : "";
    }

    /** Get dynamic job based on folder and type.
     * @param token type of job
     * @param req stapler request
     * @param rsp stapler response
     * @return newly created job
     */
    public Object getDynamic(final String token,
            final StaplerRequest req, final StaplerResponse rsp) {

        if ("single-job".equals(token)) {
            return new VectorCASTJobSingle(folder);
        }
        if ("pipeline-job".equals(token)) {
            return new VectorCASTJobPipeline(folder);
        }
        return null;
    }
}
