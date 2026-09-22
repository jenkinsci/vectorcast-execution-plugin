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
import hudson.model.ItemGroup;
import hudson.model.Project;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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


import org.kohsuke.stapler.verb.POST;
import hudson.model.Item;
import hudson.security.AccessDeniedException3;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import com.cloudbees.hudson.plugins.folder.Folder;

/**
 * Create a new single job.
 */
public class NewPipelineJob extends BaseJob {

    /** Logger for Pipeline job creation. */
    private static final Logger LOGGER = Logger.getLogger(
        NewPipelineJob.class.getName());

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
     * @param inputFolder folder to create the jobs
     * @throws ServletException exception
     * @throws IOException      exception
     * @throws ScmConflictException      exception
     * @throws ExternalResultsFileException      exception
     * @throws BadOptionComboException exception
     */
    public NewPipelineJob(
            final StaplerRequest request,
            final StaplerResponse response,
            final Folder inputFolder)
            throws ServletException, IOException,
            ScmConflictException, ExternalResultsFileException,
            BadOptionComboException {
        this(request, response, inputFolder,
            JobFormData.from(request.getSubmittedForm()));
    }

    /** Creates a Pipeline job from form data already parsed by the action. */
    public NewPipelineJob(
            final StaplerRequest request,
            final StaplerResponse response,
            final Folder inputFolder,
            final JobFormData form)
            throws ServletException, IOException,
            ScmConflictException, ExternalResultsFileException,
            BadOptionComboException {
        super(request, response, inputFolder, form);

        sharedArtifactDirectory = form.text("sharedArtifactDir", "").trim();
        pipelineSCM = form.text("scmSnippet", "").trim();

        singleCheckout = form.flag("singleCheckout", false);

        // remove the win/linux options since there's no platform any more
        environmentSetup = form.text("environmentSetup", null);
        executePreamble = form.text("executePreamble", null);
        environmentTeardown = form.text("environmentTeardown", null);
        postSCMCheckoutCommands = form.text("postSCMCheckoutCommands", null);
        useCBT  = form.flag("useCBT", true);
        useParameters  = form.flag("useParameters", false);
        if (!sharedArtifactDirectory.isEmpty()) {
            sharedArtifactDirectory = "--workspace="
                + sharedArtifactDirectory.replace("\\", "/");
        }

        /* Absolute path and SCM checkout of manage project conflicts with
           the copy_build_dir.py ability to make LIS files relative path
        */
        String mpName = getManageProjectName();
        boolean absPath = isAbsoluteProjectPath(mpName);

        if (!pipelineSCM.isEmpty() && absPath) {
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

        String projectName = "";

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

        projectName = normalizeJobName(projectName);

        setProjectName(projectName);

        checkIfProjectExists(projectName);

        LOGGER.log(Level.INFO, "Pipeline Project Name: {0}", projectName);

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

        try (InputStream template = getPipelineConfigTemplate();
                ByteArrayOutputStream generatedXml = new ByteArrayOutputStream()) {
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(template);

            // Insert generated script
            Node scriptNode = document.getElementsByTagName("script").item(0);
            scriptNode.setTextContent(generateJenkinsfile());

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(
                    new DOMSource(document),
                    new StreamResult(generatedXml)
            );

            ItemGroup<?> parent = (getFolder() != null)
                    ? getFolder() : getInstance();

            LOGGER.log(Level.INFO, "Creating job {0} in parent {1}",
                new Object[]{getProjectName(), parent.getFullName()});

            try (InputStream xmlInput = new ByteArrayInputStream(
                    generatedXml.toByteArray())) {
                if (parent instanceof Folder) {
                    Folder currFolder = (Folder) parent;

                    currFolder.createProjectFromXML(getProjectName(), xmlInput);

                } else if (parent instanceof Jenkins) {
                    Jenkins.get().createProjectFromXML(getProjectName(), xmlInput);
                } else {
                    throw new IllegalStateException(
                        "Cannot create project in parent of type: "
                        + parent.getClass().getName()
                    );
                }
            }

        } catch (ParserConfigurationException | SAXException
                | TransformerException ex) {
            LOGGER.log(Level.SEVERE, "Unable to construct Pipeline job XML",
                ex);
            throw new IOException("Unable to construct Pipeline job XML", ex);
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

    /** Returns the packaged Pipeline XML template for the selected job type. */
    private InputStream getPipelineConfigTemplate() throws IOException {
        return (useParameters ? getPipelineConfigParametersXML()
            : getPipelineConfigXML()).openStream();
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
     * Format the multiline to either be the multiline or empty "".
     *
     * @param sInVar input string
     * @return String correct path.
     */
    private String getMultiLineString(final String sInVar) {
        String retStr = "";

        if (sInVar == null || sInVar.trim().isEmpty()) {
            retStr = "\"\"";
        } else {
            retStr = "'''" + sInVar + "'''";
        }

        return retStr;
    }
    /**
     * Generates the <script> portion of the config.xml
     * which defines the pipeline for this pipeline job.
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

        String topOfJenkinsfile =
            "// ===========================================================%n" +
            "//%n" +
            "// Auto-generated script by VectorCAST Execution Plug-in%n" +
            "// based on the information provided when creating the%n" +
            "//%n" +
            "//     VectorCAST > Pipeline job%n" +
            "//%n" +
            "// ===========================================================%n" +
            "%n" +
            "def VC_Manage_Project = '%s'%n" +
            "def VC_EnvSetup = %s%n" +
            "def VC_Build_Preamble = \"%s\"%n" +
            "def VC_EnvTeardown = %s%n" +
            "def scmStep () { %s }%n" +
            "def VC_usingSCM = %s%n" +
            "def VC_postScmStepsCmds = %s%n" +
            "def VC_sharedArtifactDirectory = \"%s\"%n" +
            "def VC_Agent_Label = '%s'%n" +
            "def VC_waitTime = '%s'%n" +
            "def VC_waitLoops = '%s'%n" +
            "def VC_maxParallel = %d%n" +
            "def VC_useOneCheckoutDir = %s%n" +
            "def VC_useCILicense = %s%n" +
            "def VC_useCBT = %s%n" +
            "def VC_useCoveragePlugin = %s%n" +
            "def VC_createdWithVersion = '%s'%n" +
            "def VC_usePCLintPlus = %s%n" +
            "def VC_pclpCommand = '%s'%n" +
            "def VC_pclpResultsPattern = '%s'%n" +
            "def VC_useSquore = %s%n" +
            "def VC_squoreCommand = %s%n" +
            "def VC_useCoverageHistory = %s%n" +
            "def VC_useStrictImport = %s%n" +
            "def VC_useRGW3 = %s%n" +
            "def VC_useImportedResults = %s%n" +
            "def VC_useLocalImportedResults = %s%n" +
            "def VC_useExternalImportedResults = %s%n" +
            "def VC_externalResultsFilename = \"%s\"%n";

            topOfJenkinsfile = topOfJenkinsfile.formatted(
                getManageProjectName(),
                getMultiLineString(setup),
                preamble,
                getMultiLineString(teardown),
                pipelineSCM,
                pipelineSCM.length() != 0,
                getMultiLineString(postCheckoutCmds),
                sharedArtifactDirectory,
                getNodeLabel(),
                getWaitTime(),
                getWaitLoops(),
                getMaxParallel(),
                singleCheckout,
                vcUseCi,
                incremental,
                true,
                VcastUtils.getVersion().orElse("Unknown"),
                getPclpCommand().length() != 0,
                getPclpCommand(),
                getPclpResultsPattern(),
                getSquoreCommand().length() != 0,
                getMultiLineString(getSquoreCommand()),
                getUseCoverageHistory(),
                getUseStrictTestcaseImport(),
                getUseRGW3(),
                getUseImportedResults(),
                getUseLocalImportedResults(),
                getUseExternalImportedResults(),
                getExternalResultsFilename()
            );

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
