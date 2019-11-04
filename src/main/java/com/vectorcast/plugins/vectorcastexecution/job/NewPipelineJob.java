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

import hudson.model.Descriptor;
import hudson.model.Project;
import net.sf.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.io.IOUtils;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.net.URLDecoder;
import java.util.Enumeration;
import hudson.FilePath;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;


/**
 * Create a new single job
 */
public class NewPipelineJob extends BaseJob {
	/** project name */
	private String projectName;

	/** node label to determine where Jenkins runs the jobs */
	private String nodeLabel = null;

	/** used to determine if this version of Jenkins supports pipeline jobs */
	private boolean supported = false;

	private Project topProject;
    
    private String sharedArtifactDirectory;
    
    private String pipelineSCM = "";
    
    private String debugJSON;

    /** Environment setup script */
    private String environmentSetup;
    /** Execute preamble */
    private String executePreamble;
    /** Environment tear down*/
    private String environmentTeardown;
	/**
	 * Constructor
	 * 
	 * @param request   request object
	 * @param response  response object
	 * @throws ServletException exception
	 * @throws IOException      exception
	 * @throws PipelineNotSupportedException      exception
	 * @throws UnsupportedOperationException      exception
	 * @throws ScmConflictException      exception
	 */
	public NewPipelineJob(final StaplerRequest request, final StaplerResponse response)
			throws ServletException, IOException, PipelineNotSupportedException, UnsupportedOperationException, ScmConflictException {
		super(request, response, false);

		JSONObject json = request.getSubmittedForm();
        debugJSON = json.toString();
        
		nodeLabel = json.optString("nodeLabel");
		if (nodeLabel == null || nodeLabel.isEmpty()) {
			nodeLabel = "master";
        }

        sharedArtifactDirectory = json.optString("sharedArtifactDir",null);
        pipelineSCM = json.optString("scmSnippet","").trim();
        
        // remove the win/linux options since there's no platform any more 
        environmentSetup = json.optString("environmentSetup", null);
        executePreamble = json.optString("executePreamble", null);
        environmentTeardown = json.optString("environmentTeardown", null);
        
        if (sharedArtifactDirectory != null) {
            sharedArtifactDirectory = "--workspace="+sharedArtifactDirectory.replace("\\","/");
        } else {
            sharedArtifactDirectory = "";
        }
       
        /* absoulte path and SCM checkout of manage project conflicts with 
           the copy_build_dir.py ability to make LIS files relative path 
        */
        String MPName = this.getManageProjectName();
        Boolean absPath = false;
        
        if (MPName.startsWith("\\\\"))   absPath = true;
        if (MPName.startsWith("/"))      absPath = true;
        if (MPName.matches("[a-zA-Z]:.*")) absPath = true;
        
        Logger.getLogger(NewPipelineJob.class.getName()).log(Level.SEVERE, "MPName: " + MPName + "   scmSnippet: " + pipelineSCM,  "MPName: " + MPName + "   scmSnippet: " + pipelineSCM);

        if (pipelineSCM.length() != 0 && absPath) {
            throw new ScmConflictException(pipelineSCM, MPName);
        }

		// Get version of Jenkins to determine if pipelines are supported
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec("java -jar jenkins.war --version");
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
		int majorVersion = 0;

		try {
			majorVersion = Integer.parseInt(stdInput.readLine().substring(0, 1));
		} catch (NumberFormatException e) {
			// Prior to version 1.649, the version information contained text which
			// will cause the parse to fail. This means this Jenkins install is too
			// old to support pipeline jobs.
			supported = false;
		}
		if (majorVersion < 2) {
			supported = false;
		} else {
			supported = true;
		}
		// Pipeline is being created by the createProjectFromXML method in doCreate().
		// This returns a TopLevelItem, not a Project, getTopProject() is null.
		// Setting name and returning null.
		if (!supported) {
			throw new PipelineNotSupportedException();
		}
	}

	/**
	 * Get the name of the project
	 * 
	 * @return the project name
	 */
	public String getProjectName() {
        Logger.getLogger(NewPipelineJob.class.getName()).log(Level.SEVERE, "Pipeline Project Name: " + projectName, "Pipeline Project Name: " + projectName);
        
		return projectName;
	}

	/**
	 * Create project
	 * 
	 * @return project
	 * @throws IOException                   exception
	 * @throws JobAlreadyExistsException     exception
     
	 */
	@Override
	protected Project createProject() throws IOException, JobAlreadyExistsException {


        if (getBaseName().isEmpty()) {
            getResponse().sendError(HttpServletResponse.SC_NOT_MODIFIED, "No project name specified");
            return null;
        }

		projectName = getBaseName() + ".vcast.pipeline";
        
        if (getJobName() != null && !getJobName().isEmpty()) {
            projectName = getJobName();
        }

        if (getInstance().getJobNames().contains(projectName)) {
            throw new JobAlreadyExistsException(projectName);
        }
            
        Logger.getLogger(NewPipelineJob.class.getName()).log(Level.SEVERE, "Pipeline Project Name: " + projectName, "Pipeline Project Name: " + projectName);
		return null;
    }

	/**
	 * Add build steps
	 * 
	 * @param update true to update, false to not
	 * @throws IOException      exception
	 * @throws ServletException exception
	 * @throws hudson.model.Descriptor.FormException exception
	 */
	@Override
	public void doCreate(boolean update) throws IOException, ServletException, Descriptor.FormException {
        
		// Get config.xml resource from jar and write it to temp
		File configFile = writeConfigFile();

		try {

			// Modify XML to include generated pipeline script, remove sandbox restriction
			String configPath = configFile.getAbsolutePath();
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(configPath);

			Node scriptNode = document.getElementsByTagName("script").item(0);
			scriptNode.setTextContent(generateJenkinsfile());

			Node sandboxNode = document.getElementsByTagName("sandbox").item(0);
			sandboxNode.setTextContent("false");

			// Write DOM object to the file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource domSource = new DOMSource(document);

			StreamResult streamResult = new StreamResult(new File(configPath));
			transformer.transform(domSource, streamResult);

			InputStream xmlInput = new FileInputStream(configFile);

			/**
			 * hudson.model.Project Project proj = (Project) Fails with
			 * java.lang.ClassCastException: org.jenkinsci.plugins.workflow.job.WorkflowJob
			 * cannot be cast to Project
			 */

			getInstance().createProjectFromXML(this.projectName, xmlInput);

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (java.lang.IllegalArgumentException e) {
 			e.printStackTrace();
       }

		configFile.delete();
	}

    /**
     * Create the Pipeline Jenkinsfile script
     * @throws IOException exception
     * @throws ServletException exception
     * @throws hudson.model.Descriptor.FormException exception
     * @throws JobAlreadyExistsException exception
     * @throws InvalidProjectFileException exception
     */    
     public void create(boolean update) throws IOException, ServletException, Descriptor.FormException, 
        JobAlreadyExistsException, InvalidProjectFileException {
            
            // Create the top-level project
            topProject = createProject();   
            doCreate(update);
        }
    
	/**
	 * Retrieves config.xml from the jar and writes it to the systems temp
	 * directory.
	 * 
	 * @return
	 * @throws IOException
	 */
	private File writeConfigFile() throws IOException {

		InputStream in = getClass().getResourceAsStream("/scripts/config.xml");
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		File configFile = File.createTempFile("config_temp", ".xml");

		FileWriter fw = null;
		BufferedWriter bw = null;

		try {
			// write out to temp file
			fw = new FileWriter(configFile);
			bw = new BufferedWriter(fw);
			if ((fw != null) && (bw != null)) {
				String line = null;
				while ((line = br.readLine()) != null) {
					bw.write(line + "\n");
				}
			}
		} finally {
			// cleanup
			bw.close();
			br.close();
			in.close();
			fw.close();
		}

		return configFile.getAbsoluteFile();
	}

    /**
     * Get getSharedArtifactDirectory
     * @return sharedArtifactDirectory string
     */
    protected String getSharedArtifactDirectory() {
        return this.sharedArtifactDirectory;
    }

    /**
     * Get BaseJenkinsfile
     * @return BaseJenkinsfile string
     */
    private String getBaseJenkinsfile() throws IOException {
        String result = null;
        InputStream in = getClass().getResourceAsStream("/scripts/baseJenkinsfile.groovy");

        try {
            result = IOUtils.toString(in);
        } catch (IOException ex) {
            Logger.getLogger(NewPipelineJob.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
            in.close();
		}

        return result;
    }
	/**
	 * Generates the <script> portion of the config.xml which defines the pipeline.
	 * for this pipeline job.
	 * 
	 * 
	 * @return script portion of pipeline job.
	 * @throws IOException
	 */
     
    private String generateJenkinsfile() throws IOException {
		String setup = "";
		String preamble = "";
		String teardown = "";

		// Doing once per MultiJobDetail similar to MultiJob plugin
        if ((executePreamble != null) && (!executePreamble.isEmpty())) {
            preamble = executePreamble.replace("\\","/");
        }
        if ((environmentSetup != null) && (!environmentSetup.isEmpty())) {
            setup = environmentSetup.replace("\\","/");
        }
        if ((environmentTeardown != null) && (!environmentTeardown.isEmpty())) {
            teardown = environmentTeardown.replace("\\","/");
        }
        
        String topOfJenkinsfile = "// ===============================================================\n" + 
            "// \n" +  
            "// Auto-generated script by VectorCAST Execution Plug-in \n" +  
            "// based on the information provided when creating the \n" +  
            "//\n" +  
            "//     VectorCAST > Pipeline job\n" +  
            "//\n" +  
            "// ===============================================================\n" +  
            "\n" +  
            "VC_Manage_Project     = \"" + this.getManageProjectName() + "\"\n" + 
            "VC_EnvSetup        = \"\"\"" + setup + "\"\"\"\n" + 
            "VC_Build_Preamble  = \"" + preamble + "\"\n" + 
            "VC_EnvTeardown     = \"\"\"" + teardown + "\"\"\"\n" + 
            "def scmStep () { " + pipelineSCM + " }\n" + 
            "VC_usingSCM = " + String.valueOf(pipelineSCM.length() != 0) + "\n" + 
            "VC_sharedArtifactDirectory = \"\"\"" + sharedArtifactDirectory + "\"\"\"\n" +  
            "VC_Agent_Label = '" + nodeLabel + "'\n" +  
            "VC_waitTime = '"  + getWaitTime() + "'\n" +  
            "VC_waitLoops = '" + getWaitLoops() + "'\n" +  
            "\n" +  
            "\n" +  
            "/* DEBUG JSON REPSONSE: \n" + debugJSON + "\n*/"+
            "\n" +  
            "\n" + 
            "";
            
        String baseJenkinsfile = getBaseJenkinsfile();
        
        if (baseJenkinsfile == null) {
            baseJenkinsfile = "\n\n\n *** Errors reading the baseJenkinsfile...check the Jenkins System Logs***\n\n";
        }
        
        return  topOfJenkinsfile + baseJenkinsfile;

    }
   
	@Override
	protected void cleanupProject() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
    

}
