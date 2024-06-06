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

import hudson.Extension;
import hudson.model.RootAction;

import java.util.List;
import jenkins.model.Jenkins;
import java.util.logging.Level;
import java.util.logging.Logger;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;

import com.vectorcast.plugins.vectorcastexecution.common.VcastUtils;

/**
 * Top level of VectorCAST job control
 */
@Extension
public class VectorCASTJobRoot implements RootAction {
    
	public static final PermissionGroup PERMISSIONS_GROUP = new PermissionGroup(
			VectorCASTJobRoot.class,Messages._VectorCASTRootAction_PermissionGroup());
    private static final PermissionScope scope[] = {PermissionScope.JENKINS};
 	public static final Permission VIEW = new Permission(PERMISSIONS_GROUP,
			"View", Messages._VectorCASTRootAction_ViewPermissionDescription(),
            Jenkins.ADMINISTER, true, scope);
            
            
   /**
     * Get the icon to use.
     * @return icon to use or null if user does not have permissions
     */
    public String getIconFileName() {

        Boolean permission = false;
        if (Jenkins.get().hasPermission(VIEW)) {
            permission =  true;
        }
            
        // Always display - assume that Jenkins permission checking
        // will catch and report any permissions issues
        if (permission) {
            String iconName;
            String jenkinsVersion = Jenkins.getInstance().VERSION;
            String version[] = jenkinsVersion.split("\\.");
            Integer major, minor;
            Boolean colorIcon = true;

            try {
                major = Integer.parseInt(version[0]);
                minor = Integer.parseInt(version[1]);
                if  ((major >= 2) && (minor >=361)) {
                    colorIcon = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            if (colorIcon) {
                iconName = "/plugin/vectorcast-execution/icons/vector_favicon.png";
            } else {
                iconName = "/plugin/vectorcast-execution/icons/vector_favicon_bw.png";
            }
            
            return iconName;
        } else {
            return null;
        }
    }
    /**
     * Display name for the top-level action/menu-item
     * @return display name
     */
    @Override
    public String getDisplayName() {
        return Messages.VectorCASTCommand_AddVCJob();
    }
    /**
     * Get name of top-level action/url
     * @return url
     */
    @Override
    public String getUrlName() {
        return "VectorCAST";
    }
    /**
     * Default version.
     * @return version
     */
    public String getVersion() {
        return VcastUtils.getVersion().orElse( "Error - Could not determine version" );
    }
    /**
     * Get dynamic 'job' - used by Stapler
     * @param name name to find
     * @return dynamic job
     */
    public JobBase getDynamic(String name) {
        for (JobBase ui : getAll()) {
            if (ui != null) {
                String urlName = ui.getUrlName();
                if (urlName != null) {
                    if (urlName.equals(name)) {
                        return ui;
                    }
                }
            }
        }
        return null;
    }
    /**
     * Get all actions associated with this URL
     * @return list of actions
     */
    public List<JobBase> getAll() {
        return JobBase.all();
    }
}
