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
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Create VectorCAST diagnostic job.
 */
@Extension
public class VectorCASTJobDiag extends JobBase {

    /** The request from jenkins. */
    private StaplerRequest jenkinsRequest;

    /** The response to jenkins. */
    private StaplerResponse jenkinsResponse;

    /** Name of diagnostic job. */
    public static final String PROJECT_NAME = "VectorCAST-Diagnostics";

    /**
     * Get the diagnostics job URL.
     * @return url
     */
    @Override
    public String getUrlName() {
        return "diag-job";
    }

    @Extension
    public static final class DescriptorImpl extends JobBaseDescriptor {
    }
    /**
     * Check if the diagnostics job exists.
     * @return true if it exists and false if not
     */
    public boolean isExists() {
        Jenkins instance = Jenkins.get();

        return instance.getJobNames().contains(PROJECT_NAME);
    }
    /**
     * Gets the configuration for a unix diagnostic job.
     * @return String for unix diagnostic job
     */
    private String getUnixCommand() {
        return "#!/bin/sh\n"
                + "\n"
                + "echo\n"
                + "echo\n"
                + "\n"
                + "RET_VAL=0\n"
                + "if [ \"$VECTORCAST_DIR\" = \"\" ] ; then\n"
                + "   echo\n"
                + "   echo VECTORCAST_DIR...ERROR\n"
                + "   echo \"   Environment Variable not set.  Either set "
                + "VECTORCAST_DIR in System Environment Variables or add "
                + "to Jenkins Configuration\"\n"
                + "   echo\n"
                + "   RET_VAL=$(($RET_VAL-1))\n"
                + "   exit $RET_VAL\n"
                + "else\n"
                + "   echo VECTORCAST_DIR...OKAY\n"
                + "   echo VECTORCAST_DIR = $VECTORCAST_DIR\n"
                + "fi\n"
                + "\n"
                + "if [ \"$JENKINS_URL\" = \"\" ] ; then\n"
                + "   echo\n"
                + "   echo JENKINS_URL...ERROR\n"
                + "   echo \"   Environment Variable not set.  Define the "
                + "URL in Jenkins configuration\"\n"
                + "   echo\n"
                + "   RET_VAL=$(($RET_VAL-1))\n"
                + "   exit $RET_VAL\n"
                + "else\n"
                + "   echo JENKINS_URL...OKAY\n"
                + "   echo JENKINS_URL = $JENKINS_URL\n"
                + "fi\n"
                + "\n"
                + "if [ \"$VECTOR_LICENSE_FILE\" = \"\" ] ; then\n"
                + "   if [ \"$LM_LICENSE_FILE\" = \"\" ]  ] ; then\n"
                + "      echo\n"
                + "      echo License Environment Variable...ERROR\n"
                + "      echo \"   Neither VECTOR_LICENSE_FILE nor "
                + "LM_LICENSE_FILE environment variable set...\"\n"
                + "      echo \"      ...Set either VECTOR_LICENSE_FILE or "
                + "LM_LICENSE_FILE in System Environment Variables or add "
                + "to Jenkins Configuration\"\n"
                + "      RET_VAL=$(($RET_VAL-1))\n"
                + "      exit $REV_VAL\n"
                + "   else\n"
                + "      echo License Environment Variable...OKAY\n"
                + "   fi \n"
                + "else \n"
                + "  echo License Environment Variable...OKAY\n"
                + "fi\n"
                + "\n"
                + "$VECTORCAST_DIR/manage --help > help.log\n"
                + "\n"
                + "if [ \"$?\" != \"0\" ] ; then\n"
                + "   echo\n"
                + "   echo VectorCAST Project License...ERROR\n"
                + "   echo    Error Starting VectorCAST Project.  Check "
                + "the log below for details\n"
                + "   echo\n"
                + "   cat help.log\n"
                + "   echo\n"
                + "   RET_VAL=$(($RET_VAL-1))\n"
                + "else\n"
                + "   echo VectorCAST Project License...OKAY\n"
                + "fi\n"
                + "\n"
                + "rm -rf help.log\n"
                + "\n"
                + "exit $RET_VAL";
    }

    /**
     * Gets the configuration for a windows diagnostic job.
     * @return String for windows diagnostic job
     */
    private String getWindowsCommand() {
        return "@echo off\n"
                + "\n"
                + "echo.\n"
                + "echo.\n"
                + "\n"
                + "set RET_VAL=0\n"
                + "if \"%VECTORCAST_DIR%\"==\"\" (\n"
                + "   echo.\n"
                + "   echo VECTORCAST_DIR...ERROR\n"
                + "   echo    Environment Variable not set.  Either set "
                + "VECTORCAST_DIR in System Environment Variables or add"
                + "to Jenkins Configuration\n"
                + "   echo.\n"
                + "   set /a RET_VAL=%RET_VAL%-1\n"
                + "   goto end\n"
                + ") else (\n"
                + "   echo VECTORCAST_DIR...OKAY\n"
                + "   echo VECTORCAST_DIR = %VECTORCAST_DIR%\n"
                + ")\n"
                + "\n"
                + "if \"%JENKINS_URL%\"==\"\" (\n"
                + "   echo.\n"
                + "   echo JENKINS_URL...ERROR\n"
                + "   echo    Environment Variable not set.  Define the URL"
                + " in Jenkins configuration\n"
                + "   echo.\n"
                + "   set /a RET_VAL=%RET_VAL%-1\n"
                + "   goto end\n"
                + ") else (\n"
                + "   echo JENKINS_URL...OKAY\n"
                + "   echo JENKINS_URL = %JENKINS_URL%\n"
                + ")\n"
                + "\n"
                + "if \"%VECTOR_LICENSE_FILE%\"==\"\" ( \n"
                + "   if \"%LM_LICENSE_FILE%\"==\"\" (\n"
                + "      echo.\n"
                + "      echo License Environment Variable...ERRRO\n"
                + "      echo    Neither VECTOR_LICENSE_FILE nor "
                + "LM_LICENSE_FILE environment variable set...\n"
                + "      echo       ...Set either VECTOR_LICENSE_FILE or "
                + "LM_LICENSE_FILE in System Environment Variables or add "
                + "to Jenkins Configuration\n"
                + "      echo.\n"
                + "      set /a RET_VAL=%RET_VAL%-1\n"
                + "      goto end\n"
                + "   ) else (\n"
                + "     echo License Environment Variable...OKAY\n"
                + "   )\n"
                + ") else (\n"
                + "   echo License Environment Variable...OKAY\n"
                + ")\n"
                + "\n"
                + "%VECTORCAST_DIR%\\manage --help > help.log\n"
                + "if \"%errorlevel%\"==\"1\" (\n"
                + "   echo.\n"
                + "   echo VectorCAST Project License...ERROR\n"
                + "   echo    Error Starting VectorCAST Project.  Check "
                + "the log below for details\n"
                + "   echo.\n"
                + "   type help.log\n"
                + "   echo.\n"
                + "   set /a RET_VAL=%RET_VAL%-1\n"
                + ") else (\n"
                + "   echo VectorCAST Project License...OKAY\n"
                + ")\n"
                + "\n"
                + ":end\n"
                + "\n"
                + "@echo off\n"
                + "\n"
                + "\n"
                + "EXIT /B %RET_VAL%";
    }

    /**
     * Get response.
     * @return response
     */
    protected StaplerResponse getResponse() {
        return jenkinsResponse;
    }

    /**
     * Get request.
     * @return request
     */
    protected StaplerRequest getRequest() {
        return jenkinsRequest;
    }

    /**
     * Create the diagnostics job.
     * @param request request objext
     * @param response response object
     * @return http response
     * @throws ServletException exception
     * @throws IOException exception
     * @throws hudson.model.Descriptor.FormException exception
     */
    @RequirePOST
    public HttpResponse doCreate(final StaplerRequest request,
            final StaplerResponse response) throws ServletException,
            IOException, Descriptor.FormException {
        Jenkins j = Jenkins.get();

        this.jenkinsRequest = request;
        this.jenkinsResponse = response;

        if (j != null && !j.getJobNames().contains(PROJECT_NAME)) {
            FreeStyleProject project = j.createProject(
                            FreeStyleProject.class, PROJECT_NAME);

            String winCommand = getWindowsCommand();
            String unixCommand = getUnixCommand();

            VectorCASTCommand cmd =
                    new VectorCASTCommand(winCommand, unixCommand);
            project.getBuildersList().add(cmd);
            project.save();
        }

        return new HttpRedirect("done");
    }
}

