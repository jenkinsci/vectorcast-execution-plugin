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
 * THE SOFTWARE.
 */
package com.vectorcast.plugins.vectorcastexecution.job;

import com.vectorcast.plugins.vectorcastexecution.common.VcastUtils;


import hudson.model.Descriptor;
import hudson.model.Project;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.StandardCopyOption;

import java.util.EnumSet;
import org.kohsuke.stapler.verb.POST;
import hudson.model.Item;
import hudson.security.AccessDeniedException3;
import hudson.security.Permission;
import jenkins.model.Jenkins;

/**
 * Create a new single job.
 */
public class NewPipelineJob extends BaseJob {

    /** shared artifact directory. */
    private String sharedArtifactDirectory;
    /** Pipeline SCM string - multi-line. */
    private String pipelineSCM = "";
    /** Single Checkout. */
    private boolean singleCheckout;
    /** Using change based testing. */
    private boolean useCBT;
    /** Use pipeline parameters. */
    private boolean useParameters;
    /** Post SCM checkout command. */
    private String postSCMCheckoutCommands;
    /** Environment setup script. */
    private String environmentSetup;
    /** Execute preamble. */
    private String executePreamble;
    /** Environment tear down. */
    private String environmentTeardown;

    /**
     * Constructor.
     *
     * @param request   request object
     * @param response  response object
     * @throws ServletException exception
     * @throws IOException      exception
     * @throws ScmConflictException      exception
     * @throws ExternalResultsFileException      exception
     * @throws BadOptionComboException exception
     */
    public NewPipelineJob(
            final StaplerRequest request,
            final StaplerResponse response)
            throws ServletException, IOException,
            ScmConflictException, ExternalResultsFileException,
            BadOptionComboException {
        super(request, response);

        JSONObject json = request.getSubmittedForm();

        sharedArtifactDirectory = json.optString("sharedArtifactDir", "");
        pipelineSCM = json.optString("scmSnippet", "").trim();

        singleCheckout = json.optBoolean("singleCheckout", false);

        // remove the win/linux options since there's no platform any more
        environmentSetup = json.optString("environmentSetup", null);
        executePreamble = json.optString("executePreamble", null);
        environmentTeardown = json.optString("environmentTeardown", null);
        postSCMCheckoutCommands =
            json.optString("postSCMCheckoutCommands", null);
        useCBT  = json.optBoolean("useCBT", true);
        useParameters  = json.optBoolean("useParameters", false);
        if (sharedArtifactDirectory.length() != 0) {
            sharedArtifactDirectory = "--workspace="
                + sharedArtifactDirectory.replace("\\", "/");
        }

        /* absoulte path and SCM checkout of manage project conflicts with
           the copy_build_dir.py ability to make LIS files relative path
        */
        String mpName = getManageProjectName();
        boolean absPath = false;

        if (mpName.startsWith("\\\\")) {
            absPath = true;
        }
        if (mpName.startsWith("/")) {
            absPath = true;
        }
        if (mpName.matches("[a-zA-Z]:.*")) {
            absPath = true;
        }

        if (!mpName.toLowerCase().endsWith(".vcm")) {
            mpName += ".vcm";
        }

        if (pipelineSCM.length() != 0 && absPath) {
            throw new ScmConflictException(pipelineSCM, mpName);
        }
    }

    /**
     * Create project.
     *
     * @return project
     * @throws IOException                   exception
     * @throws JobAlreadyExistsException     exception

     */
    @Override
    protected Project<?, ?> createProject()
        throws IOException, JobAlreadyExistsException {

        String projectName;

        if (getBaseName().isEmpty()) {
            getResponse().sendError(HttpServletResponse.SC_NOT_MODIFIED,
                "No project name specified");
            return null;
        }

        if (getJobName() != null && !getJobName().isEmpty()) {
            projectName = getJobName();
        } else {
            projectName = getBaseName() + ".vcast.pipeline";
        }

        // Remove all non-alphanumeric characters from the Jenkins Job name
        projectName = projectName.replaceAll("[^a-zA-Z0-9_]", "_");

        setProjectName(projectName);

        if (getInstance().getJobNames().contains(projectName)) {
            throw new JobAlreadyExistsException(projectName);
        }

        Logger.getLogger(NewPipelineJob.class.getName()).log(Level.INFO,
                "Pipeline Project Name: " + projectName,
                "Pipeline Project Name: " + projectName);
        return null;
    }

    /**
     * Add build steps.
     *
     * @throws IOException      exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     */
    @Override
    @RequirePOST
    public void doCreate()
            throws IOException, ServletException, Descriptor.FormException {

        // Get config.xml resource from jar and write it to temp
        File configFile = writeConfigFileWithFiles();

        try {

            // Modify XML to include generated pipeline script,
            // remove sandbox restriction
            String configPath = configFile.getAbsolutePath();
            DocumentBuilderFactory documentBuilderFactory =
                DocumentBuilderFactory.newInstance();

            DocumentBuilder documentBuilder =
                documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(configPath);

            Node scriptNode = document.getElementsByTagName("script").item(0);
            scriptNode.setTextContent(generateJenkinsfile());

            // Write DOM object to the file
            TransformerFactory transformerFactory =
                TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);

            StreamResult streamResult = new StreamResult(new File(configPath));
            transformer.transform(domSource, streamResult);

            InputStream xmlInput = new FileInputStream(configFile);

            try {
                /*
                 * hudson.model.Project Project proj = (Project) Fails with
                 * java.lang.ClassCastException:
                 *     org.jenkinsci.plugins.workflow.job.WorkflowJob
                 * cannot be cast to Project
                 */

                getInstance().createProjectFromXML(getProjectName(), xmlInput);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                xmlInput.close();
            }

        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransformerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
             e.printStackTrace();
       }

        if (!configFile.delete()) {
            throw new IOException("Unable to delete file: "
                + configFile.getAbsolutePath());
        }

    }

    /**
     * Create the Pipeline Jenkinsfile script.
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws JobAlreadyExistsException exception
     * @throws InvalidProjectFileException exception
     * @throws AccessDeniedException3 exception
     */
     @POST
     @Override
     public void create() throws
            IOException,
            ServletException,
            Descriptor.FormException,
            JobAlreadyExistsException,
            InvalidProjectFileException,
            AccessDeniedException3 {

        Jenkins instance = getInstance();

        if (!instance.hasPermission(Item.CREATE)
            || !instance.hasPermission(Item.CONFIGURE)) {
            throw new AccessDeniedException3(
                instance.getAuthentication2(),
                Permission.CREATE
            );
        }

        // Create the top-level project
        createProject();
        doCreate();
    }

    private static Path createTempFile(final Path tempDirChild)
            throws UncheckedIOException {
        try {
            if (tempDirChild.getFileSystem().supportedFileAttributeViews().
                contains("posix")) {
                // Explicit permissions setting is only required
                // on unix-like systems because
                // the temporary directory is shared between all users.
                // This is not necessary on Windows,
                // each user has their own temp directory
                final EnumSet<PosixFilePermission> posixFilePermissions =
                        EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE
                        );
                if (!Files.exists(tempDirChild)) {
                    Files.createFile(
                        tempDirChild,
                        PosixFilePermissions
                            .asFileAttribute(posixFilePermissions)
                        );
                } else {
                    Files.setPosixFilePermissions(
                    tempDirChild,
                    posixFilePermissions
                    ); // GOOD: Good has permissions `-rw-------`,
                       //or will throw an exception if this fails
                }
            } else if (!Files.exists(tempDirChild)) {
                // On Windows, we still need to create the directory,
                // when it doesn't already exist.
                Files.createDirectory(tempDirChild);
            }

            return tempDirChild.toAbsolutePath();
        } catch (IOException exception) {

            exception.printStackTrace();

            throw new UncheckedIOException("Failed to create temp file",
                exception);
        }
    }

    /**
     * Retrieves config.xml from the jar and writes it to the systems temp.
     * directory.
     *
     * @return File - temporary file
     * @throws UncheckedIOException
     */
    private File writeConfigFileWithFiles() throws IOException {

        InputStream in;
        Path configFile;

        if (useParameters) {
            in = getPipelineConfigParametersXML().openStream();
        } else {
            in = getPipelineConfigXML().openStream();
        }

        configFile = createTempFile(Paths.get("config_temp.xml"));

        try {
            Files.copy(in, configFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.getLogger(NewPipelineJob.class.getName())
                .log(Level.INFO, null, ex);
        } catch (UnsupportedOperationException ex) {
            Logger.getLogger(NewPipelineJob.class.getName())
                .log(Level.INFO, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(NewPipelineJob.class.getName())
                .log(Level.INFO, null, ex);
        }

        return configFile.toFile();
    }

    /**
     * Get pipelineSCM.
     * @return pipelineSCM String
     */
    protected String getPipelineSCM() {
        return this.pipelineSCM;
    }
    /**
     * Get getPostSCMCheckoutCommands.
     * @return postSCMCheckoutCommands String
     */
    protected String getPostSCMCheckoutCommands() {
        return this.postSCMCheckoutCommands;
    }

    /**
     * Get getUseParameters.
     * @return useParameters boolean
     */
    protected boolean getUseParameters() {
        return this.useParameters;
    }

    /**
     * Get getSingleCheckout.
     * @return singleCheckout boolean
     */
    protected boolean getSingleCheckout() {
        return this.singleCheckout;
    }

    /**
     * Get getEnvironmentSetup.
     * @return environmentSetup String
     */
    protected String getEnvironmentSetup() {
        return this.environmentSetup;
    }

    /**
     * Get getExecutePreamble.
     * @return executePreamble String
     */
    protected String getExecutePreamble() {
        return this.executePreamble;
    }

    /**
     * Get getExecutePreamble.
     * @return executePreamble String
     */
    protected String getEnvironmentTeardown() {
        return this.environmentTeardown;
    }

    /**
     * Get getSharedArtifactDirectory.
     * @return sharedArtifactDirectory string
     */
    protected String getSharedArtifactDir() {
        return this.sharedArtifactDirectory;
    }

    /**
     * Get getUseCBT.
     * @return getUseCBT boolean
     */
    protected boolean getUseCBT() {
        return this.useCBT;
    }

    /**
     * Corrects the input path to be all / based.
     *
     * @param in input string
     * @return String correct path.
     */
    private String correctPath(final String in) {
        return in.replace("\\", "/").replace("\"", "\\\"");
    }

    /**
     * Generates the <script> portion of the config.xml
     * which defines the pipeline for this pipeline job.
     *
     *
     * @return script portion of pipeline job.
     * @throws IOException
     */

    private String generateJenkinsfile() throws IOException {
        String setup = "";
        String preamble = "";
        String teardown = "";
        String postCheckoutCmds = "";

        // Doing once per MultiJobDetail similar to MultiJob plugin
        if ((executePreamble != null)
                && (!executePreamble.isEmpty())) {
            preamble = correctPath(executePreamble);
        }
        if ((environmentSetup != null)
                && (!environmentSetup.isEmpty())) {
            setup = correctPath(environmentSetup);
        }
        if ((environmentTeardown != null)
                && (!environmentTeardown.isEmpty())) {
            teardown = correctPath(environmentTeardown);
        }
        if ((postSCMCheckoutCommands != null)
                && (!postSCMCheckoutCommands.isEmpty())) {
            postCheckoutCmds = correctPath(postSCMCheckoutCommands);
        }
        String incremental = "\"\"";
        if (useCBT) {
            incremental = "\"--incremental\"";
        }

        String vcUseCi = "\"\"";

        if (getUseCILicenses()) {
            vcUseCi = "\"--ci\"";
        }

        String topOfJenkinsfile = " "
            + "// ===========================================================\n"
            + "// \n"
            + "// Auto-generated script by VectorCAST Execution Plug-in \n"
            + "// based on the information provided when creating the \n"
            + "//\n"
            + "//     VectorCAST > Pipeline job\n"
            + "//\n"
            + "// ===========================================================\n"
            + "\n"
            + "def VC_Manage_Project  = \'" + getManageProjectName() + "\'\n"
            + "def VC_EnvSetup        = '''" + setup + "'''\n"
            + "def VC_Build_Preamble  = \"" + preamble + "\"\n"
            + "def VC_EnvTeardown     = '''" + teardown + "'''\n"
            + "def scmStep () { " + pipelineSCM + " }\n"
            + "def VC_usingSCM = "
            + String.valueOf(pipelineSCM.length() != 0) + "\n"
            + "def VC_postScmStepsCmds = '''" + postCheckoutCmds + "'''\n"
            + "def VC_sharedArtifactDirectory = '''"
            + sharedArtifactDirectory + "'''\n"
            + "def VC_Agent_Label = '" + getNodeLabel() + "'\n"
            + "def VC_waitTime = '"  + getWaitTime() + "'\n"
            + "def VC_waitLoops = '" + getWaitLoops() + "'\n"
            + "def VC_maxParallel = " + getMaxParallel().toString() + "\n"
            + "def VC_useOneCheckoutDir = " + singleCheckout + "\n"
            + "def VC_UseCILicense = " + vcUseCi + "\n"
            + "def VC_useCBT = " + incremental + "\n"
            + "def VC_useCoveragePlugin = " + getUseCoveragePlugin() + "\n"
            + "def VC_createdWithVersion = '"
            + VcastUtils.getVersion().orElse("Unknown") + "'\n"
            + "def VC_usePCLintPlus = "
            + String.valueOf(getPclpCommand().length() != 0) + "\n"
            + "def VC_pclpCommand = '" + getPclpCommand() + "'\n"
            + "def VC_pclpResultsPattern = '" + getPclpResultsPattern() + "'\n"
            + "def VC_useSquore = "
            +   String.valueOf(getSquoreCommand().length() != 0) + "\n"
            + "def VC_squoreCommand = '''" + getSquoreCommand() + "'''\n"
            + "def VC_useCoverageHistory = " + getUseCoverageHistory() + "\n"
            + "def VC_useStrictImport = " + getUseStrictTestcaseImport() + "\n"
            + "def VC_useRGW3 = " + getUseRGW3() + "\n"
            + "def VC_useImportedResults = " + getUseImportedResults() + "\n"
            + "def VC_useLocalImportedResults = "
            + getUseLocalImportedResults() + "\n"
            + "def VC_useExternalImportedResults = "
            + getUseExternalImportedResults() + "\n"
            + "def VC_externalResultsFilename = \""
            + getExternalResultsFilename() + "\"\n"
            + "\n";

        String baseJenkinsfile = "";

        InputStream in = null;

        try {
            in = getBaselinePipelineGroovy().openStream();
            baseJenkinsfile = IOUtils.toString(in, "UTF-8");
        } catch (IOException ex) {
            Logger.getLogger(NewSingleJob.class.getName())
                .log(Level.INFO, null, ex);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (baseJenkinsfile == null) {
            baseJenkinsfile = "\n\n\n *** Errors reading the baseJenkinsfile..."
                + " check the Jenkins System Logs***\n\n";
        }

        return  topOfJenkinsfile + baseJenkinsfile;

    }


    /**
     * Cleans up the project - should not be called at this level.
     *
     */
    @Override
    protected void cleanupProject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
