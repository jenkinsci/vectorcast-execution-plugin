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

	/** Location to store generated Pipeline artifacts */
	private String sharedLoc = null;

	private Project topProject;

	/**
	 * Constructor
	 * 
	 * @param request   request object
	 * @param response  response object
	 * @param savedData using saved data
	 * @throws ServletException exception
	 * @throws IOException      exception
	 * @throws PipelineNotSupportedException      exception
	 */
	public NewPipelineJob(final StaplerRequest request, final StaplerResponse response, boolean savedData)
			throws ServletException, IOException, PipelineNotSupportedException {
		super(request, response, false);

		JSONObject json = request.getSubmittedForm();
		nodeLabel = json.optString("nodeLabel");
		if (nodeLabel == null || nodeLabel.isEmpty()) {
			nodeLabel = "vector";
		}

		String ws = json.optString("workspace");
		if (ws != null) {
			sharedLoc = ws;
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

		projectName = getBaseName() + ".vcast.pipeline";
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
			scriptNode.setTextContent(generatePipeScript());

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
	 * Generates the <script> portion of the config.xml which defines the pipeline.
	 * for this pipeline job.
	 * 
	 * GENERATES ENVIRONMENT REPORTS AS WELL
	 * 
	 * @return script portion of pipeline job.
	 * @throws IOException
	 */
	private String generatePipeScript() throws IOException {

		String setup = null;
		String preamble = null;
		String teardown = null;

		// Doing once per MultiJobDetail similar to MultiJob plugin
		if (SystemUtils.IS_OS_WINDOWS) {
			if ((getExecutePreambleWin() != null) && (!getExecutePreambleWin().isEmpty())) {
				preamble = "\"" + getExecutePreambleWin() + "\"\n";
			}
			if ((getEnvironmentSetupWin() != null) && (!getEnvironmentSetupWin().isEmpty())) {
				setup = "\"" + getEnvironmentSetupWin() + "\"\n";
			}
			if ((getEnvironmentTeardownWin() != null) && (!getEnvironmentTeardownWin().isEmpty())) {
				teardown = "\"" + getEnvironmentTeardownWin() + "\"\n";
			}
		} else {
			if ((getExecutePreambleUnix() != null) && (!getExecutePreambleUnix().isEmpty())) {
				preamble = "\"" + getExecutePreambleUnix() + "\"\n";
			}
			if ((getEnvironmentSetupUnix() != null) && (!getEnvironmentSetupUnix().isEmpty())) {
				setup = "\"" + getEnvironmentSetupUnix() + "\"\n";
			}
			if ((getEnvironmentTeardownUnix() != null) && (!getEnvironmentTeardownUnix().isEmpty())) {
				teardown = "\"" + getEnvironmentTeardownUnix() + "\"\n";
			}
		}

		// Build the pipeline script to insert in config.xml file
		String script = 
			"import com.vectorcast.plugins.vectorcastexecution.job.ManageProject;\n"
			+ "import com.vectorcast.plugins.vectorcastexecution.job.MultiJobDetail;\n"
			+ "import com.vectorcast.plugins.vectorcastexecution.VectorCASTSetup;\n"
			+ "import java.io.IOException;\n\n" + "import hudson.FilePath;\n" + "import java.util.Map;\n"
			+ "import java.util.Map.Entry;\n\n"

			+ "def tasks = [:]\n" + "vcm_file = \"" + this.getManageProjectName() + "\"\n"
			+ "env_rebuild_cmd = 'VECTORCAST_DIR/manage.exe --project VCMFILE --level COMPILER -e ENVIRONMENT --build-execute --incremental --output REPORTLOC/ENVIRONMENT/ENVIRONMENT_rebuild.html'\n"
			+ "generate_cmd = 'VECTORCAST_DIR/vpython \\\"WORKSPACE/vc_scripts/generate-results.py\\\" VCMFILE --level COMPILER -e ENVIRONMENT'\n"
			+ "manage_cmd = 'VECTORCAST_DIR/manage.exe -p VCMFILE --level COMPILER -e ENVIRONMENT --clicast-args report custom MAnagement \\\"REPORTLOC/ENVIRONMENT/ENVIRONMENT_manage_report.html\\\"'\n"
			+ "metrics_cmd = 'VECTORCAST_DIR/manage.exe -p VCMFILE --level COMPILER -e ENVIRONMENT --clicast-args report custom MEtrics \\\"REPORTLOC/ENVIRONMENT/ENVIRONMENT_metrics_report.html\\\"' \n"
			+ "aggregate_cmd = 'VECTORCAST_DIR/manage.exe -p VCMFILE --level COMPILER -e ENVIRONMENT --clicast-args report custom COverage'\n"
			+ "full_cmd = 'VECTORCAST_DIR/manage.exe -p VCMFILE --level COMPILER -e ENVIRONMENT --clicast-args report custom FULl \\\"REPORTLOC/ENVIRONMENT/ENVIRONMENT_full_report.html\\\"'\n\n";

		String reportsLoc = null;
		String projReportsLoc = null;
		String buildDirLoc = null;
		// String artifactsLoc = null; // Force for testing, make field optional in UI
		String projLoc = buildDirLoc = FilenameUtils.removeExtension(this.getManageProjectName());

		String artifactsLoc = this.getWorkspace();
		if (artifactsLoc == null) {
			// Determine location of the project and use it as artifact location
			File f = new File(this.getManageProjectName());
			buildDirLoc = FilenameUtils.removeExtension(this.getManageProjectName()) + "/build";
			artifactsLoc = f.getParent().replace("\\", "/");
		} else {
			// set build directory location to set the workspace
			buildDirLoc = artifactsLoc + "/build";
		}

		reportsLoc = artifactsLoc + "/reports"; // Project/reports
		projReportsLoc = reportsLoc + "/Global"; // reports/Global

		script = script + "projLoc = \"" + projLoc + "\"\n" + "artifactsdir = \"" + artifactsLoc + "\"\n"
			+ "reportdir = \"" + reportsLoc + "\"\n" + "projreportdir = \"" + projReportsLoc + "\"\n"
			+ "buildLoc = \"" + buildDirLoc + "\"\n\n"

			// Project level commands
			+ "print_cmd = 'export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE'\n"
			+ "proj_full_status_html_cmd = 'VECTORCAST_DIR/manage --project VCMFILE --full-status=\"PROJECTREPORTDIR/Project_full_report.html\"'\n"
			+ "proj_full_status_txt_cmd = 'VECTORCAST_DIR/manage --project VCMFILE --full-status > \"PROJECTREPORTDIR/Project_full_report.txt\"'\n"
			+ "proj_aggregate_cmd = 'VECTORCAST_DIR/manage --project VCMFILE --create-report=aggregate --output=\"PROJECTREPORTDIR/Project_aggregate_report.html\"'\n"
			+ "proj_metrics_cmd = 'VECTORCAST_DIR/manage --project VCMFILE --create-report=metrics --output=\"PROJECTREPORTDIR/Project_metrics_report.html\"'\n"
			+ "proj_env_cmd = 'VECTORCAST_DIR/manage --project VCMFILE --create-report=environment --output=\"PROJECTREPORTDIR/Project_environment_report.html\"'\n"
			+ "proj_combined_cov_cmd = 'VECTORCAST_DIR/vpython \"WORKSPACE/vc_scripts/gen-combined-cov.py\" \"PROJECTREPORTDIR/Project_aggregate_report.html\"'\n"
			+ "proj_totals_cmd = 'VECTORCAST_DIR/vpython \"WORKSPACE\"/vc_scripts/getTotals.py \"PROJECTREPORTDIR/Project_full_report.txt\"'\n"
			+ "aggregator_cmd = 'VECTORCAST_DIR/vpython \"REPORTLOC/vc_scripts/incremental_build_report_aggregator.py\" --rptfmt HTML'\n\n"
			+ "copy_rebuild_cmd = \"cp \\\"REPORTLOC/ENVIRONMENT/ENVIRONMENT_rebuild.html\\\" \\\"${reportdir}/temp\\\"\"\n"
			+ "boolean windows = false;\n\n"

			+ "vc_dir = \"\"\n\n" + "Map<String,List> jobs_map\n" + "String ws;\n\n"

			+ "def execute(String cmd) {\n" + "\t if (!isUnix()) {\n" + "\t\t bat cmd\n" + "\t } else {\n"
			+ "\t\t sh \"${cmd}\"\n" + "\t }\n" + "}\n\n"

			+ "def create_jobs(){\n" + "\t ws_cmd = 'C:/VCAST/manage --project \"" + this.getManageProjectName()
			+ "\" --set-workspace=\"" + buildDirLoc + "\"'\n" + "\t execute(ws_cmd)\n\n"

			+ "\t\t\t\t\t\t sh \"mkdir ${reportdir}/temp\"\n\n"

			+ "\t proj = new ManageProject(\"" + this.getManageProjectName() + "\");\n";

		script = script + "\t try {\n" + "\t\t proj.parseFromPipeline()\n" + "\t } catch (Exception e) {\n"
			+ "\t\t println \"Caught an exception parsing the file: \" _ e.getMessage() \n" + "\t }\n"

			+ "\t jobs_map = new HashMap<>()\n" + "\t jobs = proj.getJobs();\n" + "\t jobs.each {\n"
			+ "\t\t MultiJobDetail job = it;\n" + "\t\t comp = it.getCompiler()\n"
			+ "\t\t suite = it.getTestSuite()\n" + "\t\t env = it.getEnvironment()\n\n"

			+ "\t\t copy_cmd = copy_rebuild_cmd.replace(\"REPORTLOC\", \"${reportdir}\")\n"
			+ "\t\t copy_cmd = copy_cmd.replace(\"COMPILER\", comp)\n"
			+ "\t\t copy_cmd = copy_cmd.replace(\"TESTSUITE\", suite)\n"
			+ "\t\t copy_cmd = copy_cmd.replace(\"ENVIRONMENT\", env)\n"

			+ "\t\t if (!fileExists(\"${reportdir}/${env}\")) {\n" + "\t\t\t sh \"mkdir ${reportdir}/${env}\"\n" // TODO - add dos equivalent																														
			+ "\t\t }\n\n"

			+ "\t\t job_name = \"${comp}_\" + \"${suite}_\" + \"${env}\"\n\n"
			
			+ "\t\t env_build_cmd = env_rebuild_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
			+ "\t\t env_build_cmd = env_build_cmd.replace(\"COMPILER\", comp)\n"
			+ "\t\t env_build_cmd = env_build_cmd.replace(\"TESTSUITE\", suite)\n"
			+ "\t\t env_build_cmd = env_build_cmd.replace(\"ENVIRONMENT\", env)\n"
			+ "\t\t env_build_cmd = env_build_cmd.replace(\"REPORTLOC\", \"${reportdir}\")\n"
			+ "\t\t env_build_cmd = env_build_cmd.replace(\"JOBNAME\", \"${job_name}\")\n\n"

			+ "\t\t gen_cmd = generate_cmd.replace(\"ENVIRONMENT\", env)\n"
			+ "\t\t gen_cmd = gen_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
			+ "\t\t gen_cmd = gen_cmd.replace(\"COMPILER\", comp)\n"
			+ "\t\t gen_cmd = gen_cmd.replace(\"TESTSUITE\", suite)\n"
			+ "\t\t gen_cmd = gen_cmd.replace(\"WORKSPACE\", \"${artifactsdir}\")\n\n"

			+ "\t\t man_cmd = manage_cmd.replace(\"ENVIRONMENT\", env)\n"
			+ "\t\t man_cmd = man_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
			+ "\t\t man_cmd = man_cmd.replace(\"COMPILER\", comp)\n"
			+ "\t\t man_cmd = man_cmd.replace(\"REPORTLOC\", \"${reportdir}\")\n\n"

			+ "\t\t met_cmd = metrics_cmd.replace(\"ENVIRONMENT\", env)\n"
			+ "\t\t met_cmd = met_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
			+ "\t\t met_cmd = met_cmd.replace(\"COMPILER\", comp)\n"
			+ "\t\t met_cmd = met_cmd.replace(\"REPORTLOC\", \"${reportdir}\")\n\n"

			+ "\t\t agg_cmd = aggregate_cmd.replace(\"ENVIRONMENT\", env)\n"
			+ "\t\t agg_cmd = agg_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
			+ "\t\t agg_cmd = agg_cmd.replace(\"COMPILER\", comp)\n"
			+ "\t\t agg_cmd = agg_cmd.replace(\"REPORTLOC\", \"${reportdir}\")\n\n"

			+ "\t\t f_cmd = full_cmd.replace(\"ENVIRONMENT\", env)\n"
			+ "\t\t f_cmd = f_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
			+ "\t\t f_cmd = f_cmd.replace(\"COMPILER\", comp)\n"
			+ "\t\t f_cmd = f_cmd.replace(\"REPORTLOC\", \"${reportdir}\")\n\n"

			+ "\t\t List cmds = new ArrayList();\n\n"

			+ "\t\t setup_cmd = \"\"\n" // TODO - only add if not empty
			+ "\t\t cmds.add(setup_cmd)\n\n"

			+ "\t\t preamble_cmd = \"\" \n" + "\t\t cmds.add(preamble_cmd)\n" + "\t\t cmds.add(env_build_cmd)\n"
			+ "\t\t cmds.add(gen_cmd)\n" + "\t\t cmds.add(man_cmd)\n" + "\t\t cmds.add(met_cmd)\n"
			+ "\t\t cmds.add(f_cmd)\n" + "\t\t cmds.add(copy_cmd)\n\n"

			+ "\t\t teardown_cmd = \"\"\n" + "\t\t jobs_map.put(job_name, cmds);\n" + "\t }\n"
			+ "\t  return jobs_map\n" + " }\n\n"

			// Start of pipeline
			+ "pipeline {\n\n"
			/**
			 * Using master to run the pipeline itself frees up an agent to run the jobs,
			 * resulting in significantly faster run times.
			 */
			+ "\t agent {label 'master'}\n\n" + "\t stages {\n\n" + "\t\t stage('Determine Environments'){\n\n"

			+ "\t\t\t steps {\n" + "\t\t\t\t script {\n" + "\t\t\t\t\t if (!fileExists(\"${artifactsdir}\")) {\n"
			+ "\t\t\t\t\t\t sh \"mkdir ${artifactsdir}\"\n" + "\t\t\t\t\t }\n\n"

			+ "\t\t\t\t\t dest = new File(\"${artifactsdir}\")\n" + "\t\t\t\t\t VectorCASTSetup.copyFiles(dest)\n\n"

			+ "\t\t\t\t\t if (!isUnix()) {\n" + "\t\t\t\t\t\t windows = true;\n" + "\t\t\t\t\t }\n"
			+ "\t\t\t\t\t jobs_map = create_jobs()\n" + "\t\t\t\t\t }\n" + "\t\t\t\t }\n" + "\t\t\t }\n\n"

			// Execute stage
			+ "\t\t stage('Execute  Environments') {\n" + "\t\t\t steps {\n" + "\t\t\t\t script {\n\n"

			+ "\t\t\t\t\t jobs_map.each { entry ->\n" + "\t\t\t\t\t\t name = entry.getKey()\n\n"

			+ "\t\t\t\t\t\t tasks[entry.getKey()] = {\n" + "\t\t\t\t\t\t\t ArrayList cmds = entry.getValue();\n\n";

		// TODO - in the future, restrict jobs based on compiler (job_name)
		// node(name) {
		if (nodeLabel != null) {
			script = script + "\t\t\t\t\t\t\t node('" + this.nodeLabel + "'){\n";
		} else {
			script = script + "\t\t\t\t\t\t\t node('vector'){\n";
		}

		script = script +
			"\t\t\t\t\t\t\t\t copyArtifacts filter: '**/*_rebuild*, execution/**/ management/**, xml_data/**, tar_tarFile', optional: true, projectName: '"
			+ this.getBaseName() + ".vcast.pipeline', selector: workspace()\n"
			+ "\t\t\t\t\t\t\t\t ws = \"${workspace}\".replace(\"\\\\\", \"/\")\n"
			+ "\t\t\t\t\t\t\t\t dest = new File(ws);\n" + "\t\t\t\t\t\t\t\t VectorCASTSetup.copyFiles(dest)\n\n"

			+ "\t\t\t\t\t\t\t\t vc_dir = sh (\n" // TODO - dos equivalent command
			+ "\t\t\t\t\t\t\t\t\t script: 'echo $VECTORCAST_DIR',\n" + "\t\t\t\t\t\t\t\t\t returnStdout: true \n"
			+ "\t\t\t\t\t\t\t\t\t ).trim() \n"
			+ "\t\t\t\t\t\t\t\t vc_dir = vc_dir.replaceAll(\"\\\\\\\\\", \"/\")\n\n"

			+ "\t\t\t\t\t\t\t\t for (String command : cmds) { \n"
			+ "\t\t\t\t\t\t\t\t\t command = command.replaceAll(\"VECTORCAST_DIR\", \"${vc_dir}\")\n"
			+ "\t\t\t\t\t\t\t\t\t command = command.replaceAll(\"WORKSPACE\", \"${ws}\")\n"
			+ "\t\t\t\t\t\t\t\t\t execute(command)\n" + "\t\t\t\t\t\t\t }\n" + "\t\t\t\t\t\t }\n" + "\t\t\t\t\t }\n"
			+ "\t\t\t\t }\n"
			+ "\t\t\t\t parallel tasks\n" + "\t\t\t\t }\n" + "\t\t\t }\n" + "\t\t }\n\n"

			+ "\t\t stage ('Create Reports') {\n" + "\t\t\t steps {\n" + "\t\t\t\t script {\n\n";

		// default to node label 'vector'
		if (nodeLabel != null) {
			script = script + "\t\t\t\t\t node('" + this.nodeLabel + "'){\n";
		} else {
			script = script + "\t\t\t\t\t node('vector'){\n";
		}

		script = script + "\t\t\t\t\t\t if (isUnix()){\n"
				+ "\t\t\t\t\t\t\t print_cmd = 'export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE'\n" + "\t\t\t\t\t\t } else {\n"
				+ "\t\t\t\t\t\t\t print_cmd = 'set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE'\n" + "\t\t\t\t\t\t }\n\n"

				// TODO check if file exists. Always delete for clean results?
				// + "\t\t\t\t\t\t sh \"rm -r ${reportdir}/temp*\"\n"
				// + "\t\t\t\t\t\t sh \"mkdir ${reportdir}/temp\"\n\n"

				+ "\t\t\t\t\t\t String main_vc_dir = sh (\n" + "\t\t\t\t\t\t\t script: 'echo $VECTORCAST_DIR',\n"
				+ "\t\t\t\t\t\t\t returnStdout: true\n" + "\t\t\t\t\t\t ).trim()\n"
				+ "\t\t\t\t\t\t main_vc_dir = main_vc_dir.replaceAll(\"\\\\\\\\\", \"/\")\n"
				+ "\t\t\t\t\t\t String main_vpython = \"${main_vc_dir}/vpython.exe\"\n\n"

				// + "\t\t\t\t\t\t if (!fileExists(\"${projreportdir}\")) {\n"
				// TODO - fileExists() always returns false if file is outside Jenkins workspace.
				// Use Java for file operations.
				+ "\t\t\t\t\t\t\t sh \"mkdir ${projreportdir}\"\n" // TODO - dos equivalent
				// + "\t\t\t\t\t\t }\n\n"

				+ "\t\t\t\t\t\t extract = extract_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t extract = extract.replace(\"WORKSPACE\", \"${ws}\")\n\n"

				+ "\t\t\t\t\t\t aggregator_cmd = aggregator_cmd.replace(\"REPORTLOC\", \"${artifactsdir}\")\n"
				+ "\t\t\t\t\t\t aggregator_cmd = aggregator_cmd.replace(\"VPYTHON\", \"${main_vpython}\")\n"
				+ "\t\t\t\t\t\t aggregator_cmd = aggregator_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t aggregator_cmd = aggregator_cmd.replace(\"PROJECTREPORTDIR\", \"${projreportdir}\")\n\n"

				+ "\t\t\t\t\t\t proj_full_status_html_cmd = proj_full_status_html_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t proj_full_status_html_cmd = proj_full_status_html_cmd.replace(\"WORKSPACE\", \"${artifactsdir}\")\n"
				+ "\t\t\t\t\t\t proj_full_status_html_cmd = proj_full_status_html_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
				+ "\t\t\t\t\t\t proj_full_status_html_cmd = proj_full_status_html_cmd.replace(\"PROJECTREPORTDIR\", \"${projreportdir}\")\n\n"

				+ "\t\t\t\t\t\t proj_full_status_txt_cmd = proj_full_status_txt_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t proj_full_status_txt_cmd = proj_full_status_txt_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
				+ "\t\t\t\t\t\t proj_full_status_txt_cmd = proj_full_status_txt_cmd.replace(\"PROJECTREPORTDIR\", \"${projreportdir}\")\n\n"

				+ "\t\t\t\t\t\t proj_aggregate_cmd = proj_aggregate_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t proj_aggregate_cmd = proj_aggregate_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
				+ "\t\t\t\t\t\t proj_aggregate_cmd = proj_aggregate_cmd.replace(\"PROJECTREPORTDIR\", \"${projreportdir}\")\n\n"

				+ "\t\t\t\t\t\t proj_metrics_cmd = proj_metrics_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t proj_metrics_cmd = proj_metrics_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
				+ "\t\t\t\t\t\t proj_metrics_cmd = proj_metrics_cmd.replace(\"PROJECTREPORTDIR\", \"${projreportdir}\")\n\n"

				+ "\t\t\t\t\t\t proj_env_cmd = proj_env_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t proj_env_cmd = proj_env_cmd.replace(\"VCMFILE\", \"${vcm_file}\")\n"
				+ "\t\t\t\t\t\t proj_env_cmd = proj_env_cmd.replace(\"PROJECTREPORTDIR\", \"${projreportdir}\")\n\n"

				+ "\t\t\t\t\t\t proj_combined_cov_cmd = proj_combined_cov_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t proj_combined_cov_cmd = proj_combined_cov_cmd.replace(\"WORKSPACE\", \"${artifactsdir}\")\n"
				+ "\t\t\t\t\t\t proj_combined_cov_cmd = proj_combined_cov_cmd.replace(\"PROJECTREPORTDIR\", \"${projreportdir}\")\n\n"

				+ "\t\t\t\t\t\t proj_totals_cmd = proj_totals_cmd.replace(\"VECTORCAST_DIR\", \"${main_vc_dir}\")\n"
				+ "\t\t\t\t\t\t proj_totals_cmd = proj_totals_cmd.replace(\"WORKSPACE\", \"${artifactsdir}\")\n"
				+ "\t\t\t\t\t\t proj_totals_cmd = proj_totals_cmd.replace(\"PROJECTREPORTDIR\", \"${projreportdir}\")\n\n"

				+ "\t\t\t\t\t\t cmds = [\"set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE\",\n"
				+ "\t\t\t\t\t\t\t \"${print_cmd}\",\n"
				// + "\t\t\t\t\t\t\t \"${extract}\",\n"
				+ "\t\t\t\t\t\t\t \"${proj_full_status_txt_cmd}\",\n"
				+ "\t\t\t\t\t\t\t \"${proj_full_status_html_cmd}\",\n" + "\t\t\t\t\t\t\t \"${proj_env_cmd}\",\n"
				+ "\t\t\t\t\t\t\t \"${proj_metrics_cmd}\",\n" + "\t\t\t\t\t\t\t \"${proj_totals_cmd}\",\n"
				+ "\t\t\t\t\t\t\t \"${proj_aggregate_cmd}\",\n" + "\t\t\t\t\t\t\t \"${proj_combined_cov_cmd}\",	\n"
				+ "\t\t\t\t\t\t ]\n\n"

				+ "\t\t\t\t\t\t cmds.each {\n" + "\t\t\t\t\t\t\t execute(it)\n" + "\t\t\t\t\t\t }\n\n"

				+ "\t\t\t\t\t\t dir(\"${reportdir}/temp\") {\n" // TODO - Check if it exists, if so delete contents (dos equivalent)
				+ "\t\t\t\t\t\t\t execute(\"${aggregator_cmd}\")\n" + "\t\t\t\t\t\t }\n"
				// + "\t\t\t\t\t\t sh \"rm -r ${reportdir}/temp\"\n\n"
				+ "\t\t\t\t\t\t def full = \"\"\n" + "\t\t\t\t\t\t def combined = \"\"\n\n"

				+ "\t\t\t\t\t\t File c = new File(\"${projreportdir}/CombinedReport.html\");\n"
				+ "\t\t\t\t\t\t boolean c_exists = c.exists();\n"
				+ "\t\t\t\t\t\t File f = new File(\"${projreportdir}/Project_Full_Report.txt\");\n"
				+ "\t\t\t\t\t\t boolean f_exists = f.exists(); \n"
				+ "\t\t\t\t\t\t archiveArtifacts '*_rebuild*, *_report.html, execution/**, management/**, xml_data/**'\n\n"

				+ "\t\t\t\t\t\t if (c_exists && f_exists) {\n"
				+ "\t\t\t\t\t\t\t combined = readFile(\"${projreportdir}/CombinedReport.html\")\n"
				+ "\t\t\t\t\t\t\t full = readFile(\"${projreportdir}/Project_Full_Report.txt\")\n"
				+ "\t\t\t\t\t\t } else { \n"
				+ "\t\t\t\t\t\t\t manager.createSummary(\"warning.gif\").appendText(\"General Failure\", false, false, false, \"red\")\n"
				+ "\t\t\t\t\t\t\t manager.buildUnstable()\n"
				+ "\t\t\t\t\t\t\t manager.build.description = \"General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\"\n"
				+ "\t\t\t\t\t\t\t manager.addBadge(\"warning.gif\", \"General Error\")\n" + "\t\t\t\t\t\t }\n"
				+ "\t\t\t\t\t }\n" + "\t\t\t\t }\n" + "\t\t\t }\n" + "\t\t }\n\n"

				+ "\t\t stage ('Generate Coverage') {\n" + "\t\t\t steps {\n" + "\t\t\t\t dir (\"${ws}\") {\n"
				+ "\t\t\t\t\t step (\n" + "\t\t\t\t\t [$class: 'VectorCASTPublisher', \n"
				+ "\t\t\t\t\t includes: 'xml_data/coverage_results*.xml',\n" + "\t\t\t\t\t useThreshold: false])\n"
//				step(
//				[$class: 'JUnitResultArchiver', 
//				testResults: '**/test_results_*.xml'])
				+ "\t\t\t\t }\n" + "\t\t\t }\n" + "\t\t }\n" + "\t}\n" + " }";

		return script;
	}
        
	@Override
	protected void cleanupProject() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
