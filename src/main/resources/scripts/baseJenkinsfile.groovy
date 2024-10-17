// Code Coverage threshold numbers
// Basis path coverage is no longer support after VectorCAST 2019SP1
VC_Healthy_Target = [ maxStatement: 100, maxBranch: 100, maxFunctionCall: 100, maxFunction: 100, maxMCDC: 100,
                      minStatement: 20,  minBranch: 20,  minFunctionCall: 20,  minFunction: 20,  minMCDC: 20]


VC_Use_Threshold = true

// ===============================================================
//
// Generic file from VectorCAST Pipeline Plug-in DO NOT ALTER
//
// ===============================================================


// Failure phrases for checkLogsForErrors

VC_FailurePhrases = ["No valid edition(s) available",
                  "py did not execute correctly",
                  "Traceback (most recent call last)",
                  "Failed to acquire lock on environment",
                  "Environment Creation Failed",
                  "Error with Test Case Management Report",
                  "FLEXlm Error",
                  "INCR_BUILD_FAILED",
                  "Environment was not successfully built",
                  "NOT_LINKED",
                  "Preprocess Failed",
                  "Abnormal Termination on Environment",
                  "not recognized as an internal or external command",
                  "Another Workspace with this path already exists",
                  "Destination directory or database is not writable",
                  "Could not acquire a read lock on the project's vcm file",
                  "No environments found in ",
                  ".vcm is invalid",
                  "Invalid Workspace. Please ensure the directory and database contain write permission",
                  "The environment is invalid because",
                  "Please ensure that the project has the proper permissions and that the environment is not being accessed by another process.",
                  "Error: That command is not permitted in continuous integration mode"
                  ]

// Unstable phrases for checkLogsForErrors

VC_UnstablePhrases = ["Dropping probe point",
                    "Value Line Error - Command Ignored",
                    "INFO: Problem parsing test results file",
                    "INFO: File System Error ",
                    "ERROR: Error accessing DataAPI",
                    "ERROR: Undefined Error"
                    ]

// ===============================================================
//
// Function : checkLogsForErrors
// Inputs   : log
// Action   : Scans the input log file to for keywords listed above
// Returns  : found foundKeywords, failure and/or unstable_flag
// Notes    : Used to Check for VectorCAST build errors/problems
//
// ===============================================================

def checkLogsForErrors(log) {

    def boolean failure = false;
    def boolean unstable_flag = false;
    def foundKeywords = ""

    // Check for unstable first
    // Loop over all the unstable keywords above
    VC_UnstablePhrases.each {
        if (log.contains(it)) {
            // found a phrase considered unstable, mark the build accordingly
            foundKeywords =  foundKeywords + it + ", "
            unstable_flag = true
        }
    }

    // The check for failure keywords first
    // Loop over all the failure keywords above
    VC_FailurePhrases.each {
        if (log.contains(it)) {
            // found a phrase considered failure, mark the build accordingly
            foundKeywords =  foundKeywords + it + ", "
            failure = true
        }
    }
    if (foundKeywords.endsWith(", ")) {
        foundKeywords = foundKeywords[0..-3]
    }

    return [foundKeywords, failure, unstable_flag]
}

// ===============================================================
//
// Function : pluginCreateSummary
// Inputs   : log
// Action   : Scans the input log file to for keywords listed above
// Returns  : found foundKeywords, failure and/or unstable_flag
// Notes    : Used to Check for VectorCAST build errors/problems
//
// ===============================================================

def pluginCreateSummary(inIcon, inText) {

    try { 
       //Protected code 
       createSummary icon: inIcon, text: inText

    } catch(ExceptionName e1) {

       //Catch block 
       addSummary icon: inIcon, text: inText
    }
}

// ===============================================================
//
// Function : checkBuildLogForErrors
// Inputs   : logFile
// Action   : Scans the input log file to for keywords listed above
// Returns  : found foundKeywords, failure and/or unstable_flag
// Notes    : Used to Check for VectorCAST build errors/problems
//
// ===============================================================

def checkBuildLogForErrors(logFile) {

    def boolean failure = false;
    def boolean unstable_flag = false;
    def foundKeywords = ""
    def output = ""
    def status = 0

    writeFile file: "phrases.txt", text: VC_UnstablePhrases.join("\n") + VC_FailurePhrases.join("\n")

    if (isUnix()) {
        cmd =  "grep -f phrases.txt " + logFile + " > search_results.txt"
        status = sh label: 'Checking build log for errors', returnStatus: true, script: cmd
    } else {
        cmd =  "findstr /g:phrases.txt " + logFile + " > search_results.txt"
        status = bat label: 'Checking build log for errors', returnStatus: true, script: cmd
    }

    if (status == 0) {
        output = readFile("search_results.txt")
        foundKeywords += output.replaceAll("\n",",")
        return checkLogsForErrors(output)
    } else {
        return [foundKeywords, failure, unstable_flag]
    }

    error ("Error in checking build log file: " + cmd)

}

// ***************************************************************
//
//                           SCM Utilities
//
// ***************************************************************

// ===============================================================
//
// Function : get_SCM_rev
// Inputs   : None
// Action   : Returns SCM revision from git or svn
// Notes    : Used for TESTinsight Command
//
// ===============================================================

def get_SCM_rev() {
    def scm_rev = ""
    def cmd = ""

    if (VC_TESTinsights_SCM_Tech=='git') {
        cmd = "git rev-parse HEAD"
    } else {
        cmd = "svn info --show-item revision"
    }

    if (isUnix()) {
        scm_rev = sh returnStdout: true, script: cmd
    } else {
        cmd = "@echo off \n " + cmd
        scm_rev = bat returnStdout: true, script: cmd
    }

    println "Git Rev Reply " + scm_rev.trim() + "***"
    return scm_rev.trim()
}

// ***************************************************************
//
//                       File/Pathing Utilities
//
// ***************************************************************

// ===============================================================
//
// Function : getMPname
// Inputs   : None
// Action   : Returns the base name
// Notes    : Used for creating report name
//
// ===============================================================

def getMPname() {
    // get the manage projects full name and base name
    def mpFullName = VC_Manage_Project.split("/")[-1]
    def mpName = ""
    if (mpFullName.toLowerCase().endsWith(".vcm")) {
        mpName = mpFullName.take(mpFullName.lastIndexOf('.'))
    } else {
        mpName = mpFullName
    }
    return mpName
}

// ===============================================================
//
// Function : stripLeadingWhitespace
// Inputs   : string or multiline string with leading spaces
// Action   : input string with leading spaces removed
// Notes    : None
//
// ===============================================================
def stripLeadingWhitespace(str) {
    def lines = str.split('\n')
    def trimmedString = ""
    lines.each { line ->
        trimmedString += line.trim() + "\n"
    }

    return trimmedString
}


// ===============================================================
//
// Function : getMPpath
// Inputs   : None
// Action   : Returns the path name to the manage project's directory
// Notes    : Used for accessing the build directory
//
// ===============================================================
def getMPpath() {
    // get the manage projects full name and base name
    def mpFullName = VC_Manage_Project
    def mpPath = ""
    if (mpFullName.toLowerCase().endsWith(".vcm")) {
        mpPath = mpFullName.take(mpFullName.lastIndexOf('.'))
    } else {
        mpPath = mpFullName
    }
    return mpPath
}

// ===============================================================
//
// Function : formatPath
// Inputs   : directory path
// Action   : on Windows it will change / path seperators to \ (\\)
// Returns  : fixed path
// Notes    : Used to Check for VectorCAST build errors/problems
//
// ===============================================================

def formatPath(inPath) {
    def outPath = inPath
    if (!isUnix()) {
        outPath = inPath.replace("/","\\")
    }
    return outPath
}

// ===============================================================
//
// Function : fixUpName
// Inputs   : command list
// Action   : Fixup name so it doesn't include / or %## or any other special characters
// Returns  : Fixed up name
// Notes    : Used widely
//
// ===============================================================

def fixUpName(name) {
    return name.replace("/","_").replaceAll('\\%..','_').replaceAll('\\W','_')
}

// ===============================================================
//
// Function : concatenateBuildLogs
// Inputs   : file list
// Action   : Concatenate build logs into one file
// Returns  : None
// Notes    : Generate-Overall-Reports
//
// ===============================================================

def concatenateBuildLogs(logFileNames, outputFileName) {

    def cmd = ""
    if (isUnix()) {
        cmd =  "cat "
    } else {
        cmd =  "type "
    }

    cmd += logFileNames + " > " + outputFileName

    runCommands(cmd)
}


// ***************************************************************
//
//                    Execution Utilities
//
// ***************************************************************


// ===============================================================
//
// Function : runCommands
// Inputs   : command list
// Action   : 1. Adds VC Setup calls to beginning of script
//            2. If using CI licenses, it set the appropriate envionment variables
//            3. Replaces keywords for windows/linux
//            4. Calls the command
//            5. Reads the log and return the log file (prints as well)
// Returns  : stdout/stderr from the commands
// Notes    : Used widely
//
// ===============================================================
def runCommands(cmds) {
    def boolean failure = false;
    def boolean unstable_flag = false;
    def foundKeywords = ""
    def localCmds = """"""

    // clear that old command log
    writeFile file: "command.log", text: ""

    // if its Linux run the sh command and save the stdout for analysis
    if (isUnix()) {
        // add VC setup to beginning of script
        // add extra env vars to make debugging of commands useful
        // add extra env for reports
        localCmds = """
            ${VC_EnvSetup}
            export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
            export VCAST_NO_FILE_TRUNCATION=1
            export VCAST_RPTS_SELF_CONTAINED=FALSE

            """

        // if using CI licenses add in both CI license env vars
        if (VC_UseCILicense.length() != 0) {
            localCmds += """
                export VCAST_USING_HEADLESS_MODE=1
                export VCAST_USE_CI_LICENSES=1
            """
        }
        cmds = localCmds + cmds
        cmds = stripLeadingWhitespace(cmds.replaceAll("_VECTORCAST_DIR","\\\$VECTORCAST_DIR").replaceAll("_RM","rm -rf ").replaceAll("_COPY","cp -p ").replaceAll("_IF_EXIST","if [[ -f ").replaceAll("_IF_THEN"," ]] ; then ").replaceAll("_ENDIF","; fi") )
        println "Running commands: " + cmds

        // run command in shell
        sh label: 'Running VectorCAST Commands', returnStdout: false, script: cmds

    } else {
        // add VC setup to beginning of script
        // add extra env vars to make debugging of commands useful
        // add extra env for reports
        localCmds = """
            @echo off
            ${VC_EnvSetup}
            set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
            set VCAST_NO_FILE_TRUNCATION=1
            set VCAST_RPTS_SELF_CONTAINED=FALSE

            """

        // if using CI licenses add in both CI license env vars
         if (VC_UseCILicense.length() != 0) {
            localCmds += """
                set VCAST_USING_HEADLESS_MODE=1
                set VCAST_USE_CI_LICENSES=1
            """
        }
        cmds = localCmds + cmds
        cmds = stripLeadingWhitespace(cmds.replaceAll("_VECTORCAST_DIR","%VECTORCAST_DIR%").replaceAll("_RM","DEL /Q ").replaceAll("_COPY","copy /y /b").replaceAll("_IF_EXIST","if exist ").replaceAll("_IF_THEN"," ( ").replaceAll("_ENDIF"," )"))
        println "Running commands: " + cmds

        // run command in bat
        bat label: 'Running VectorCAST Commands', returnStdout: false, script: cmds
    }

    // read back the command.log - this is specific to
    log = readFile "command.log"

    println "Commands Output: " + log

    return log
}

// ===============================================================
//
// Function : setupManageProject
// Inputs   : none
// Action   : Issues commands needed to run manage project from
//            .vcm and environment defintions
// Returns  : None
// Notes    : Used once per manage project checkout
//
// ===============================================================

def setupManageProject() {
    def mpName = getMPname()

    def cmds = """"""

    if (VC_sharedArtifactDirectory.length() > 0) {
        cmds += """
            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} ${VC_sharedArtifactDirectory} --status"
        """
    }

    if (VC_useStrictImport) {
        cmds += """
            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --config=VCAST_STRICT_TEST_CASE_IMPORT=TRUE"
        """
    }

    cmds += """
        _RM *_rebuild.html
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --status"
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --force --release-locks"
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --config VCAST_CUSTOM_REPORT_FORMAT=HTML"
    """

    if (VC_useImportedResults) {
        if (VC_useLocalImportedResults) {
            try {
                copyArtifacts filter: "${mpName}_results.vcr", fingerprintArtifacts: true, optional: true, projectName: "${env.JOB_NAME}", selector: lastSuccessful()
                cmds += """
                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --force --import-result=${mpName}_results.vcr"
                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --status"
                    _IF_EXIST ${mpName}_results.vcr _IF_THEN _COPY ${mpName}_results.vcr ${mpName}_results_orig.vcr _ENDIF
                """
            } catch (exe) {
                print "No result artifact to use"
            }
        } else if (VC_useExternalImportedResults)  {
            if (VC_externalResultsFilename.length() != 0) {
                cmds += """
                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --force --import-result=${VC_externalResultsFilename}"
                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --status"
                """
            } else {
                error ("External result specified, but external result file is blank")
            }
        }
    }


    runCommands(cmds)
}

// ===============================================================
//
// Function : transformIntoStep
// Inputs   : inputString containing a single [compiler, test_suite, environment]
// Action   : Uses the input to create a build step in the pipeline job
// Returns  : Build step for a job
// Notes    : This will be executed later in parallel with other jobs
//
// ===============================================================

def transformIntoStep(inputString) {

    // grab the compiler test_suite and environment out the single line
    def (compiler, test_suite, environment) = inputString.split()

    // set the stashed file name for later
    String stashName = fixUpName("${env.JOB_NAME}_${compiler}_${test_suite}_${environment}-build-execute-stage")

    // return the auto-generated node and job
    // node is based on compiler label
    // this will route the job to a specific node matching that label
    return {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {

            // Try to use VCAST_FORCE_NODE_EXEC_NAME parameter.
            // If 0 length or not present, use the compiler name as a node label
            def nodeID = "default"
            try {
                if ("${VCAST_FORCE_NODE_EXEC_NAME}".length() > 0) {
                    nodeID = "${VCAST_FORCE_NODE_EXEC_NAME}"
                }
                else {
                    nodeID = compiler
                }
            } catch (exe) {
               nodeID = compiler
            }

            print "Using NodeID = " + nodeID


            // node definition and job starting here
            node ( nodeID as String ){

                println "Starting Build-Execute Stage for ${compiler}/${test_suite}/${environment}"

                // if we are not using a single checkout directory
                if (!VC_useOneCheckoutDir) {

                    // call the scmStep for each job
                    scmStep()
                }

                // Run the setup step to copy over the scripts
                step([$class: 'VectorCASTSetup'])

                // if we are not using a single checkout directory and using SCM step
                if (VC_usingSCM && !VC_useOneCheckoutDir) {

                    // set options for each manage project pulled out out of SCM
                    setupManageProject()
                }

                // setup the commands for building, executing, and transferring information
                if (VC_useRGW3) {
                    cmds =  """
                        ${VC_EnvSetup}
                         _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/patch_rgw_directory.py "${VC_Manage_Project}"
                        ${VC_Build_Preamble} _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --level ${compiler}/${test_suite} -e ${environment} --build-execute ${VC_useCBT} --output ${compiler}_${test_suite}_${environment}_rebuild.html"
                        ${VC_EnvTeardown}
                    """
                } else {

                    cmds =  """
                    ${VC_EnvSetup}
                    ${VC_Build_Preamble} _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --level ${compiler}/${test_suite} -e ${environment} --build-execute ${VC_useCBT} --output ${compiler}_${test_suite}_${environment}_rebuild.html"
                    ${VC_EnvTeardown}
                    """
                }


                // setup build lot test variable to hold all VC commands results for this job
                def buildLogText = ""

                // run the build-execute step and save the results
                buildLogText = runCommands(cmds)

                def foundKeywords = ""
                def boolean failure = false
                def boolean unstable_flag = false

                // check log for errors/unstable keywords
                (foundKeywords, failure, unstable_flag) = checkLogsForErrors(buildLogText)

                // if we didn't fail and don't have a shared artifact directory - we may have to copy back build directory artifacts...
                if (!failure && VC_sharedArtifactDirectory.length() == 0) {

                    // if we are using an SCM checkout and we aren't using a single checkout directory, we need to copy back build artifacts
                    if (VC_usingSCM && !VC_useOneCheckoutDir) {
                        def fixedJobName = fixUpName("${env.JOB_NAME}")
                        buildLogText += runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/copy_build_dir.py ${VC_Manage_Project} ${compiler}/${test_suite} ${fixedJobName}_${compiler}_${test_suite}_${environment} ${environment}""" )
                    }
                }

                // write a build log file for compiler/test suite/environment
                writeFile file: "${compiler}_${test_suite}_${environment}_build.log", text: buildLogText

                // no cleanup - possible CBT
                // use individual names
                def fixedJobName = fixUpName("${env.JOB_NAME}")

                // save artifact in a "stash" to be "unstashed" by the main job
                stash includes: "${compiler}_${test_suite}_${environment}_build.log, **/${compiler}_${test_suite}_${environment}_rebuild.html, execution/*.html, management/*${compiler}_${test_suite}_${environment}*, xml_data/*${compiler}_${test_suite}_${environment}*, ${fixedJobName}_${compiler}_${test_suite}_${environment}_build.tar", name: stashName as String

                println "Finished Build-Execute Stage for ${compiler}/${test_suite}/${environment}"

                // check log for errors/unstable keywords again since the copy build dir could have thrown errors/unstable keywords
                (foundKeywords, failure, unstable_flag) = checkLogsForErrors(buildLogText)

                // if something failed, raise an error
                if (failure) {
                    error ("Error in Commands: " + foundKeywords)

                // else if something made the job unstable, mark as unsable
                } else if (unstable_flag) {
                    unstable("Triggering stage unstable because keywords found: " + foundKeywords)
                }
            }
        }
    }
}

// ===============================================================
//
// Function : stepsForJobList
// Inputs   : localEnvList - a list of [compiler, testsuite, environments] that need to be executed
// Action   : Loops of list and calls " transformIntoStep
// Returns  : A job that can be executed
// Notes    : Use to get a list of system and unit tests jobs
//
// ===============================================================
def stepsForJobList(localEnvList) {
    jobList = [:]
    localEnvList.each {
        jobList[it] =  transformIntoStep(it)
    }

    return jobList
}

// global environment list used to create pipeline jobs
EnvList = []
UtEnvList = []
StEnvList = []
origManageProject = VC_Manage_Project


// ***************************************************************
//
//              VectorCAST Execution Pipeline
//
// ***************************************************************

pipeline {

    // Use the input from the job creation label as for the "main job"
    agent {label VC_Agent_Label as String}

    stages {
        // Place holder for previous stages the customer may need to use
        stage('Previous-Stage') {
            steps {
                script {
                    println "place holder for previous stages"
                }
            }
        }

        // If we are using a single checkout directory option, do the checkout here
        // This stage also includes the implementation for the parameterized Jenkins job
        //    that includes a forced node name and an external repository
        // External repository is used when another job has already checked out the source code
        //    and is passing that information to this pipeline via VCAST_PROJECT_DIR env var
        stage('Single-Checkout') {
            steps {
                script {
                    def usingExternalRepo = false;

                    // check to see if env var VCAST_PROJECT_DIR is setup from another job
                    try {
                        if ("${VCAST_PROJECT_DIR}".length() > 0) {
                            usingExternalRepo = true
                            VC_Manage_Project = "${VCAST_PROJECT_DIR}/" + VC_Manage_Project
                        }
                        else {
                            usingExternalRepo = false
                        }
                    } catch (exe) {
                       usingExternalRepo = false
                    }

                    // If we are using a single checkout directory option and its not a
                    //    SMC checkout from another job...
                    if (VC_useOneCheckoutDir && !usingExternalRepo) {

                        // we need to convert all the future job's workspaces to point to the original checkout
                        VC_OriginalWorkspace = "${env.WORKSPACE}"
                        println "scmStep executed here: " + VC_OriginalWorkspace
                        scmStep()
                        print "Updating " + VC_Manage_Project + " to: " + VC_OriginalWorkspace + "/" + VC_Manage_Project
                        VC_Manage_Project = VC_OriginalWorkspace + "/" + VC_Manage_Project

                        def origSetup = VC_EnvSetup
                        def origTeardown = VC_EnvTeardown
                        def orig_VC_sharedArtifactDirectory = VC_sharedArtifactDirectory
                        def orig_VC_postScmStepsCmds = VC_postScmStepsCmds

                        if (isUnix()) {
                            VC_EnvSetup = VC_EnvSetup.replace("\$WORKSPACE" ,VC_OriginalWorkspace)
                            VC_EnvTeardown = VC_EnvTeardown.replace("\$WORKSPACE" ,VC_OriginalWorkspace)
                            VC_sharedArtifactDirectory = VC_sharedArtifactDirectory.replace("\$WORKSPACE" ,VC_OriginalWorkspace)
                            VC_postScmStepsCmds = VC_postScmStepsCmds.replace("\$WORKSPACE" ,VC_OriginalWorkspace)
                        } else {
                            VC_OriginalWorkspace = VC_OriginalWorkspace.replace('\\','/')

                            def tmpInfo = ""

                            // replace case insensitive workspace with WORKSPACE
                            tmpInfo = VC_EnvSetup.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                            VC_EnvSetup = tmpInfo.replace("%WORKSPACE%",VC_OriginalWorkspace)

                            // replace case insensitive workspace with WORKSPACE
                            tmpInfo = VC_EnvTeardown.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                            VC_EnvTeardown = tmpInfo.replace("%WORKSPACE%",VC_OriginalWorkspace)

                            // replace case insensitive workspace with WORKSPACE
                            tmpInfo = VC_sharedArtifactDirectory.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                            VC_sharedArtifactDirectory = tmpInfo.replace("%WORKSPACE%" ,VC_OriginalWorkspace)

                            // replace case insensitive workspace with WORKSPACE
                            tmpInfo = VC_postScmStepsCmds.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                            VC_postScmStepsCmds = tmpInfo.replace("%WORKSPACE%" ,VC_OriginalWorkspace)
                        }
                        print "Updating setup script " + origSetup + " \nto: " + VC_EnvSetup
                        print "Updating teardown script " + origTeardown + " \nto: " + origTeardown
                        print "Updating shared artifact directory " + orig_VC_sharedArtifactDirectory + " \nto: " + VC_sharedArtifactDirectory
                        print "Updating post SCM steps "  + orig_VC_postScmStepsCmds + "\nto: " + VC_postScmStepsCmds

                        // If there are post SCM checkout steps, do them now
                        if (VC_postScmStepsCmds.length() > 0) {
                            runCommands(VC_postScmStepsCmds)
                        }
                    } else {
                        if (usingExternalRepo) {
                            println "Using ${VCAST_FORCE_NODE_EXEC_NAME}/${VC_Manage_Project} as single checkout directory"
                        }
                        else {
                            println "Not using Single Checkout"
                        }
                    }
                }
            }
        }

        // This stage gets the information on the manage project from the full-status report
        // Parsing the output determines the level and environment name
        stage('Get-Environment-Info') {
            steps {
                script {
                        if (currentBuild.description == null) {
                            currentBuild.description = ""
                        }

                    if (!VC_useOneCheckoutDir) {
                        // Get the repo (should only need the .vcm file)
                        scmStep()

                        // If there are post SCM checkout steps, do them now
                        if (VC_postScmStepsCmds.length() > 0) {
                            runCommands(VC_postScmStepsCmds)
                        }
                    }
                    
                    // archive existing reports 
                    tar file: "reports_archive.tar" , glob: "management/*.html,xml_data/**/*.xml", overwrite: true

                    println "Created with VectorCAST Execution Version: " + VC_createdWithVersion

                    // Run the setup step to copy over the scripts
                    step([$class: 'VectorCASTSetup'])

                    // -------------------------------------------------------------------------------------------
                    // this part could be done with Manage_Project.getJobs() but it doesn't seem to be working VVV
                    def EnvData = ""

                    // Run a script to determine the compiler test_suite and environment
                    EnvData = runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/getjobs.py ${VC_Manage_Project} --type""")

                    // for a groovy list that is stored in a global variable EnvList to be use later in multiple places
                    def lines = EnvData.split('\n')

                    // loop over each returned line from getjobs.py to determine if its a system test or unit test
                    //    and add it to the appropriate list
                    lines.each { line ->
                        def trimmedString = line.trim()
                        boolean containsData = trimmedString?.trim()
                        if (containsData) {
                            (type, compiler, test_suite, environment) = trimmedString.split()
                            if (type == "ST:") {
                                trimmedString = compiler + " " + test_suite + " " + environment
                                // print("ST:" + trimmedString)
                                StEnvList = StEnvList + [trimmedString]
                            }
                            else if (type == "UT:") {
                                trimmedString = compiler + " " + test_suite + " " + environment
                                // print("UT:" + trimmedString)
                                UtEnvList = UtEnvList + [trimmedString]
                            }
                            else {
                                trimmedString = compiler + " " + test_suite + " " + environment
                                print("??:" + trimmedString)
                                return
                            }

                            print ("++ " + trimmedString)
                            EnvList = EnvList + [trimmedString]
                        }
                    }
                    // down to here                                                                            ^^^
                    // -------------------------------------------------------------------------------------------
                }
            }
        }

        // This is the stage that we use the EnvList via stepsForJobList >> transformIntoStep
        stage('System Test Build-Execute Stage') {
            steps {
                script {
                    setupManageProject()

                    // Get the job list from the system test environment listed
                    jobs = stepsForJobList(StEnvList)

                    // run each of those jobs in serial
                    jobs.each { name, job ->
                        print ("Running System Test Job: " + name)
                        job.call()
                    }
               }
            }
        }

        // This is the stage that we use the EnvList via stepsForJobList >> transformIntoStep
        stage('Unit Test Build-Execute Stage') {
            steps {
                script {
                    setupManageProject()

                    // Get the job list from the unit test environment listed
                    jobs = stepsForJobList(UtEnvList)

                    if (VC_maxParallel > 0) {
                        def runningJobs = [:]
                        jobs.each { job ->
                            runningJobs.put(job.key, job.value)
                            if (runningJobs.size() == VC_maxParallel) {
                                parallel runningJobs
                                runningJobs = [:]
                            }
                        }
                        if (runningJobs.size() > 0) {
                            parallel runningJobs
                            runningJobs = [:]
                        }
                    } else {
                        // run those jobs in parallel
                        parallel jobs
                    }
                }
            }
        }

        // Generating the reports needed for VectorCAST/Coverage plugin and JUnit
        stage('Generate-Overall-Reports') {

            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {

                    // Run the setup step to copy over the scripts
                    step([$class: 'VectorCASTSetup'])

                    // run script to unstash files and generate results/reports
                    script {
                        def buildFileNames = ""

                        // Loop over all environnment and unstash each of the files
                        // These files will be logs and build artifacts
                        EnvList.each {
                            (compiler, test_suite, environment) = it.split()
                            String stashName = fixUpName("${env.JOB_NAME}_${compiler}_${test_suite}_${environment}-build-execute-stage")

                            try {
                                unstash stashName as String
                                buildFileNames += "${compiler}_${test_suite}_${environment}_build.log "

                            }
                            catch (Exception ex) {
                                println ex
                            }
                        }

                        concatenateBuildLogs(buildFileNames, "unstashed_build.log")

                        // get the manage project's base name for use in rebuild naming
                        def mpName = getMPname()

                        def buildLogText = ""

                        // if we are using SCM and not using a shared artifact directory...
                        if (VC_usingSCM && !VC_useOneCheckoutDir && VC_sharedArtifactDirectory.length() == 0) {
                            // run a script to extract stashed files and process data into xml reports
                            def mpPath = getMPpath()
                            def coverDBpath = formatPath(mpPath + "/build/vcast_data/cover.db")

                            cmds = """
                                _RM ${coverDBpath}
                                _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/extract_build_dir.py  --leave_files
                                _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  ${VC_UseCILicense} --refresh"
                            """
                            buildLogText += runCommands(cmds)

                        }

                        // run the metrics at the end
                        buildLogText += runCommands("""_VECTORCAST_DIR/vpython  "${env.WORKSPACE}"/vc_scripts/generate-results.py  ${VC_Manage_Project} --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --junit --buildlog unstashed_build.log""")

                        if (VC_useRGW3) {
                            buildLogText += runCommands("""_VECTORCAST_DIR/vpython  "${env.WORKSPACE}"/vc_scripts/patch_rgw_directory.py  ${VC_Manage_Project}""")
                            buildLogText += runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  ${VC_UseCILicense} --clicast-args rgw export" """)
                        }

                        if (VC_useCoveragePlugin) {
                            buildLogText += runCommands("""_VECTORCAST_DIR/vpython  "${env.WORKSPACE}"/vc_scripts/cobertura.py  ${VC_Manage_Project}""")
                        }

                        cmds =  """
                            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/incremental_build_report_aggregator.py ${mpName} --rptfmt HTML
                            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/full_report_no_toc.py "${VC_Manage_Project}"
                            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  ${VC_UseCILicense} --create-report=aggregate   --output=${mpName}_aggregate_report.html"
                        """

                        if (VC_useImportedResults) {
                            if (VC_useLocalImportedResults) {
                                cmds += """
                                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  ${VC_UseCILicense} --export-result=${mpName}_results.vcr"
                                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/merge_vcr.py --new ${mpName}_results.vcr --orig ${mpName}_results_orig.vcr

                                """
                            }
                        }

                        buildLogText += runCommands(cmds)

                        writeFile file: "metrics_build.log", text: buildLogText

                        buildFileNames += "metrics_build.log "

                        concatenateBuildLogs(buildFileNames, "complete_build.log")

                        (foundKeywords, failure, unstable_flag) = checkBuildLogForErrors("complete_build.log")

                        if (failure) {
                            throw new Exception ("Error in Commands: " + foundKeywords)
                        }

                        if (VC_useCoveragePlugin) {
                            // Send reports to the Jenkins Coverage Plugin
                            discoverReferenceBuild()
                            if (VC_useCoverageHistory) {
                                recordCoverage qualityGates: [[baseline: 'PROJECT_DELTA', criticality: 'NOTE', metric: 'LINE', threshold: -0.001], [baseline: 'PROJECT_DELTA', criticality: 'FAILURE', metric: 'BRANCH', threshold: -0.001]], tools: [[parser: 'VECTORCAST', pattern: 'xml_data/cobertura/coverage_results*.xml']]                            
                            } else {
                                recordCoverage tools: [[parser: 'VECTORCAST', pattern: 'xml_data/cobertura/coverage_results*.xml']]
                            }

                        } else {
                            def currResult = ""
                            if (VC_useCoverageHistory) {
                                currResult = currentBuild.result
                            }

                            // Send reports to the VectorCAST Soverage Plugin
                            step([$class: 'VectorCASTPublisher',
                                includes: 'xml_data/coverage_results*.xml',
                                useThreshold: VC_Use_Threshold,
                                healthyTarget:   VC_Healthy_Target,
                                useCoverageHistory: VC_useCoverageHistory,
                                maxHistory : 20])

                            if (VC_useCoverageHistory) {
                                if ((currResult != currentBuild.result) && (currentBuild.result == 'FAILURE')) {
                                    pluginCreateSummary("icon-error icon-xlg", "Code Coverage Decreased")
                                    currentBuild.description += "Code coverage decreased.  See console log for details\n"
                                    addBadge icon: "icon-error icon-xlg", text: "Code Coverage Decreased"
                                }
                            }
                        }


                        // Send test results to JUnit plugin
                        step([$class: 'JUnitResultArchiver', keepLongStdio: true, allowEmptyResults: true, testResults: '**/test_results_*.xml'])
                    }
                }

                // Save all the html, xml, and txt files
                archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.html, xml_data/**/*.xml, unit_test_*.txt, **/*.png, **/*.css, complete_build.log, *_results.vcr'
            }
        }

        // checking the build log of all the VC commands that have run
        // setup overall job's build descript with aggregated incremental build report and full status report
        stage('Check-Build-Log') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {

                        def mpName = getMPname()

                        def foundKeywords = ""
                        def boolean failure = false
                        def boolean unstable_flag = false

                        (foundKeywords, failure, unstable_flag) = checkBuildLogForErrors('complete_build.log')

                        // if the found keywords is great that the init value \n then we found something
                        // set the build description accordingly
                        if (foundKeywords.size() > 0) {
                            currentBuild.description += "Problematic data found in console output, search the console output for the following phrases: " + foundKeywords + "\n"
                        }

                        // Make sure the build completed and we have two key reports
                        //   - Using CBT - CombinedReport.html (combined rebuild reports from all the environments)
                        //   - full status report from the manage project

                        // Grab the coverage differences
                        def summaryText = ""

                        if (fileExists('coverage_diffs.html_tmp')) {
                            summaryText += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> " 
                            summaryText += readFile('coverage_diffs.html_tmp') 

                        } else {
                            print "coverage_diffs.html_tmp missing"
                        }

                        if (VC_useCBT) {
                            if (fileExists('combined_incr_rebuild.tmp') && fileExists("${mpName}_full_report.html_tmp") && fileExists("${mpName}_metrics_report.html_tmp")) {
                                // If we have both of these, add them to the summary in the "normal" job view
                                // Blue ocean view doesn't have a summary
                                summaryText += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
                                summaryText += readFile('combined_incr_rebuild.tmp') 
                                summaryText += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> " 
                                summaryText += readFile("${mpName}_full_report.html_tmp") 
                                summaryText += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> " 
                                summaryText += readFile("${mpName}_metrics_report.html_tmp")
                                    
                                pluginCreateSummary ("icon-document icon-xlg", summaryText)

                            } else {
                                if (fileExists('combined_incr_rebuild.tmp')) {
                                    print "combined_incr_rebuild.tmp found"
                                } else {
                                    print "combined_incr_rebuild.tmp missing"
                                }
                                if (fileExists("${mpName}_full_report.html_tmp")) {
                                    print "${mpName}_full_report.html_tmp found"
                                } else {
                                    print "${mpName}_full_report.html_tmp missing"
                                }
                                if (fileExists("${mpName}_metrics_report.html_tmp")) {
                                    print "${mpName}_metrics_report.html_tmp found"
                                } else {
                                    print "${mpName}_metrics_report.html_tmp missing"
                                }

                                // If not, something went wrong... Make the build as unstable
                                currentBuild.result = 'UNSTABLE'
                                pluginCreateSummary ("icon-warning icon-xlg", "General Failure")
                                currentBuild.description += "General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\n"
                            }
                        } else {
                            if (fileExists("${mpName}_full_report.html_tmp") && fileExists("${mpName}_metrics_report.html_tmp")) {
                                // If we have both of these, add them to the summary in the "normal" job view
                                // Blue ocean view doesn't have a summary

                                summaryText += readFile("${mpName}_full_report.html_tmp") + "<br> " + readFile("${mpName}_metrics_report.html_tmp")
                                pluginCreateSummary ("icon-document icon-xlg", summaryText)

                            } else {
                                // If not, something went wrong... Make the build as unstable
                                currentBuild.result = 'UNSTABLE'
                                pluginCreateSummary ("icon-warning icon-xlg", "General Failure")
                                currentBuild.description += "General Failure, Full Report or Metrics Report Not Present. Please see the console for more information\n"
                            }
                        }

                        // Remove temporary files used for summary page
                        def cmds = """
                            _RM coverage_diffs.html_tmp
                            _RM combined_incr_rebuild.tmp
                            _RM ${mpName}_full_report.html_tmp
                            _RM ${mpName}_metrics_report.html_tmp
                        """

                        runCommands(cmds)

                        // use unit_test_fail_count.txt to see if there were any failed test cases
                        // if any failed test cases, Junit will mark as at least unstable.
                        def unitTestErrorCount = ""
                        unitTestErrorCount = readFile "unit_test_fail_count.txt"
                        if (unitTestErrorCount != "0") {
                            currentBuild.description += "Failed test cases, Junit will mark at least as UNSTABLE\n"
                        }
                        if (failure) {
                            currentBuild.result = 'FAILURE'
                            error ("Raising Error: " + "Problematic data found in console output, search the console output for the following phrases: " + foundKeywords)
                        } else if (unstable_flag) {
                            unstable("Triggering stage unstable because keywords found: " + foundKeywords)
                        }
                    }
                }
            }
        }

        // Stage for additional tools from Vector
        // Currently supporting PC Lint Plus, Squore, and TESTInsights
        stage('Additional Tools') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {

                        // If there's a PC Lint Plus command...
                        if (VC_usePCLintPlus) {
                            // run the PC Lint Plus command
                            runCommands(VC_pclpCommand)
                            // record the results with Warnings-NG plugin
                            recordIssues(tools: [pcLint(pattern: VC_pclpResultsPattern, reportEncoding: 'UTF-8')])
                            // Archive the PC Lint Results
                            archiveArtifacts allowEmptyArchive: true, artifacts: VC_pclpResultsPattern
                        }

                        // If we are using Squore...
                        if (VC_useSquore) {
                            // Generate the results from Squore and run the squore command which should publish the information to Squore Server
                            cmd = "${VC_squoreCommand}"
                            runCommands(cmd)

                            // Archive the Squore results
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'xml_data/squore_results*.xml'
                        }

                        // If we using TESTInsights...
                        if (VC_useTESTinsights){

                            // using the credentials passed in when creating the job...
                            withCredentials([usernamePassword(credentialsId: VC_TESTinsights_Credential_ID, usernameVariable : "VC_TI_USR", passwordVariable : "VC_TI_PWS")]){
                                TI_proxy = ""

                                // if we are using a proxy to communicate with TESTInsights, set the proxy from data input during job creation
                                if (VC_TESTinsights_Proxy.length() != 0) {
                                    TI_proxy = "--proxy ${VC_TESTinsights_Proxy}"
                                }

                                // Build the base TESTInsights command
                                TESTinsight_Command = "testinsights_connector --api ${VC_TESTinsights_URL} --user " + VC_TI_USR + "  --pass " + VC_TI_PWS + "  --action PUSH --project  ${VC_TESTinsights_Project} --test-object  ${BUILD_NUMBER} --vc-project ${VC_Manage_Project} " + TI_proxy + " --log TESTinsights_Push.log"

                                // If we are using an SCM, attempt to link the SCM info into TESTInsights
                                if (VC_usingSCM) {

                                    // Get the TESTinsights Revision
                                    VC_TESTinsights_Revision = get_SCM_rev()

                                    println "Git Rev: ${VC_TESTinsights_Revision}"

                                    // Update the TESTInsights command
                                    TESTinsight_Command += " --vc-project-local-path=${origManageProject} --vc-project-scm-path=${VC_TESTinsights_SCM_URL}/${origManageProject} --src-local-path=${env.WORKSPACE} --src-scm-path=${VC_TESTinsights_SCM_URL}/ --vc-project-scm-technology=${VC_TESTinsights_SCM_Tech} --src-scm-technology=${VC_TESTinsights_SCM_Tech} --vc-project-scm-revision=${VC_TESTinsights_Revision} --src-scm-revision ${VC_TESTinsights_Revision} --versioned"

                                }
                                // Run the command to push data to TESTInsights
                                runCommands(TESTinsight_Command)

                                // Archive the push log
                                archiveArtifacts allowEmptyArchive: true, artifacts: 'TESTinsights_Push.log'
                            }
                        }
                    }
                }
            }
        }

        // Place holder for previous stages the customer may need to use
        stage('Next-Stage') {
            steps {
                script {
                    println "place holder for next stages"
                }
            }
        }
    }
}
