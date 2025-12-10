// Code Coverage threshold numbers
// Basis path coverage is no longer support after VectorCAST 2019SP1
def VC_Healthy_Target = [ maxStatement: 100, maxBranch: 100, maxFunctionCall: 100, maxFunction: 100, maxMCDC: 100,
                          minStatement: 20,  minBranch: 20,  minFunctionCall: 20,  minFunction: 20,  minMCDC: 20]

def VC_Use_Threshold = true

// ===============================================================
//
// Generic file from VectorCAST Pipeline Plug-in DO NOT ALTER
//
// ===============================================================        

// Gathering global variables to be used by functions
def VC = [
    mpName:             VC_Manage_Project
    setup:              VC_EnvSetup
    preable:            VC_Build_Preamble
    teardown:           VC_EnvTeardown
    oneChkDir:          VC_useOneCheckoutDir
    usingSCM:           VC_usingSCM
    postSCMSteps:       VC_postScmStepsCmds
    maxParallel:        VC_maxParallel
    useRGW3:            VC_useRGW3
    waitTime:           VC_waitTime
    waitLoops:          VC_waitLoops
    useCI:              VC_useCILicense
    useCBT:             VC_useCBT
    useCoverPlgin:      VC_useCoveragePlugin
    sharedBldDir:       VC_sharedArtifactDirectory
    strictImp:          VC_useStrictImport
    useCoverHist:       VC_useCoverageHistory
    useImpRst:          VC_useImportedResults
    useLocImpRst:       VC_useLocalImportedResults
    useExtImpRst:       VC_useExternalImportedResults
    extRst:             VC_externalResultsFilename
    usePclp:            VC_usePCLintPlus 
    pLcpCmd:            VC_pclpCommand 
    pLcpRsltPattern:    VC_pclpResultsPattern
    useSquore:          VC_useSquore 
    squoreCmd:          VC_squoreCommand
    healthyTarget:      VC_Healthy_Target
    useThreshold:       VC_Use_Threshold
]



// ===============================================================
//
// Generic file from VectorCAST Pipeline Plug-in DO NOT ALTER
//
// ===============================================================



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

    } catch(Exception e) {

       //Catch block
       addSummary icon: inIcon, text: inText
    }
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

def getMPname(manageProject) {
    // get the manage projects full name and base name
    def mpFullName = manageProject.split("/")[-1]
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
def getMPpath(manageProject) {
    // get the manage projects full name and base name
    def mpFullName = manageProject
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
def runCommands(cmds, envSetup, useCILicense) {
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
            ${envSetup}
            export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
            export VCAST_NO_FILE_TRUNCATION=1
            export VCAST_RPTS_SELF_CONTAINED=FALSE
            """

        // if using CI licenses add in both CI license env vars
        if (useCILicense.length() != 0) {
            localCmds += """
                export VCAST_USING_HEADLESS_MODE=1
                export VCAST_USE_CI_LICENSES=1
            """
        }
        cmds = localCmds + cmds
        cmds = stripLeadingWhitespace(cmds.replaceAll("_VECTORCAST_DIR","\\\$VECTORCAST_DIR").replaceAll("_RM","rm -rf ").replaceAll("_COPY","cp -p ").replaceAll("_IF_EXIST","if [[ -f ").replaceAll("_IF_THEN"," ]] ; then ").replaceAll("_ENDIF","; fi") )
        println "Running Linux Command: " + cmds

        // run command in shell
        sh label: 'Running VectorCAST Commands', returnStdout: false, script: cmds

    } else {
        // add VC setup to beginning of script
        // add extra env vars to make debugging of commands useful
        // add extra env for reports
        localCmds = """
            @echo off
            ${envSetup}
            set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
            set VCAST_NO_FILE_TRUNCATION=1
            set VCAST_RPTS_SELF_CONTAINED=FALSE

            """

        // if using CI licenses add in both CI license env vars
         if (useCILicense.length() != 0) {
            localCmds += """
                set VCAST_USING_HEADLESS_MODE=1
                set VCAST_USE_CI_LICENSES=1
            """
        }
        cmds = localCmds + cmds
        cmds = stripLeadingWhitespace(cmds.replaceAll("_VECTORCAST_DIR","%VECTORCAST_DIR%").replaceAll("_RM","DEL /Q ").replaceAll("_COPY","copy /y /b").replaceAll("_IF_EXIST","if exist ").replaceAll("_IF_THEN"," ( ").replaceAll("_ENDIF"," )"))
        println "Running Windows Command: " + cmds

        // run command in bat
        bat label: 'Running VectorCAST Commands', returnStdout: false, script: cmds
    }

    // read back the command.log - this is specific to
    def log = ""
    
    if (fileExists("command.log")) {
        log = readFile "command.log"
        println "Commands Output: " + log
    } else {
        println "Error getting command.log from these commands: " + cmds
    }

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

def setupManageProject(VC) {
            
    def mpName = getMPname(manageProject)

    def cmds = """"""

    if (sharedArtifactDirectory.length() > 0) {
        cmds += """
            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} ${sharedArtifactDirectory} --status"
        """
    }

    if (useStrictImport) {
        cmds += """
            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --config=VCAST_STRICT_TEST_CASE_IMPORT=TRUE"
        """
    }

    cmds += """
        _RM *_rebuild.html
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --status"
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --force --release-locks"
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --config VCAST_CUSTOM_REPORT_FORMAT=HTML"
        """

    if (useOneCheckoutDir) {
        cmds += """_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --config VCAST_DEPENDENCY_CACHE_DIR=./vcqik" """
    }

    if (useImportedResults) {
        if (useLocalImportedResults) {
            try {
                copyArtifacts filter: "${mpName}_results.vcr", fingerprintArtifacts: true, optional: true, projectName: "${env.JOB_NAME}", selector: lastSuccessful()
                cmds += """
                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --force --import-result=${mpName}_results.vcr"
                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --status"
                    _IF_EXIST ${mpName}_results.vcr _IF_THEN _COPY ${mpName}_results.vcr ${mpName}_results_orig.vcr _ENDIF
                """
            } catch(Exception e) {
                print "No result artifact to use"
            }
        } else if (useExternalImportedResults)  {
            if (externalResultsFilename.length() != 0) {
                cmds += """
                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --force --import-result=${externalResultsFilename}"
                    _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --status"
                """
            } else {
                error ("External result specified, but external result file is blank")
            }
        }
    }


    runCommands(cmds, envSetup, useCILicense)
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

def transformIntoStep(inputString, VC) {
        
    def compiler = ""
    def test_suite = ""
    def environment = ""
    def source = ""
    def machine = ""
    def level = ""
    
    def trimmedLine = inputString.trim()
    def wordCount = trimmedLine.split(/\s+/).length
    if (wordCount == 3) {
        (compiler, test_suite, environment) = inputString.split()
        level = compiler + "/" + test_suite
    } else if (wordCount == 5) {
        (compiler, test_suite, environment, source, machine) = inputString.split()
        level = source + "/" + machine + "/" + compiler + "/" + test_suite
    }
    // grab the compiler test_suite and environment out the single line
    // set the stashed file name for later
    String stashName = fixUpName("${env.JOB_NAME}_${compiler}_${test_suite}_${environment}-build-execute-stage")

    // return the auto-generated node and job
    // node is based on compiler label
    // this will route the job to a specific node matching that label
    return {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {

            def cmds = ""

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
            } catch(Exception e) {
               nodeID = compiler
            }

            print "Using NodeID = " + nodeID


            // node definition and job starting here
            node ( nodeID as String ){

                println "Starting Build-Execute Stage for ${compiler}/${test_suite}/${environment}"

                // if we are not using a single checkout directory
                if (!useOneCheckoutDir) {

                    // call the scmStep for each job
                    scmStep()
                }

                // Run the setup step to copy over the scripts
                step([$class: 'VectorCASTSetup'])

                // if we are not using a single checkout directory and using SCM step
                if (usingSCM && !useOneCheckoutDir) {

                    // set options for each manage project pulled out out of SCM
                    setupManageProject(VC)
                }

                // setup the commands for building, executing, and transferring information
                if (useRGW3) {
                    cmds =  """
                        ${envSetup}
                         _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/patch_rgw_directory.py "${manageProject}"
                        ${buildPreamble} _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --level ${level} -e ${environment} --build-execute ${useCBT} --output ${compiler}_${test_suite}_${environment}_rebuild.html"
                        ${envTeardown}
                    """
                } else {

                    cmds =  """
                        ${envSetup}
                        ${buildPreamble} _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${waitTime} --wait_loops ${waitLoops} --command_line "--project "${manageProject}" ${useCILicense} --level ${level} -e ${environment} --build-execute ${useCBT} --output ${compiler}_${test_suite}_${environment}_rebuild.html"
                        ${envTeardown}
                    """
                }

                // setup build lot test variable to hold all VC commands results for this job
                def buildLogText = ""

                // run the build-execute step and save the results
                buildLogText = runCommands(cmds, envSetup, useCILicense)

                def foundKeywords = ""
                def boolean failure = false
                def boolean unstable_flag = false

                // check log for errors/unstable keywords
                (foundKeywords, failure, unstable_flag) = VCUtils.checkLogsForErrors(buildLogText)

                // if we didn't fail and don't have a shared artifact directory - we may have to copy back build directory artifacts...
                if (!failure && sharedArtifactDirectory.length() == 0) {

                    // if we are using an SCM checkout and we aren't using a single checkout directory, we need to copy back build artifacts
                    if (usingSCM && !useOneCheckoutDir) {
                        def fixedJobName = fixUpName("${env.JOB_NAME}")
                        buildLogText += runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/copy_build_dir.py ${manageProject} --level ${level} --basename ${fixedJobName}_${compiler}_${test_suite}_${environment} --environment ${environment}""", envSetup, useCILicense)

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
                (foundKeywords, failure, unstable_flag) = VCUtils.checkLogsForErrors(buildLogText)

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
def stepsForJobList(localEnvList, VC) {

    def jobList = [:]
    localEnvList.each {
        jobList[it] =  transformIntoStep(it, VC)
    }

    return jobList
}

// global environment list used to create pipeline jobs
def EnvList = []
def UtEnvList = []
def StEnvList = []
def origManageProject = VC_Manage_Project


// ***************************************************************
//
//              VectorCAST Execution Pipeline
//
// ***************************************************************

pipeline {

    // Use the input from the job creation label as for the "main job"
    agent {label VC_Agent_Label as String}

    stages {

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
                    } catch(Exception e) {
                       usingExternalRepo = false
                    }

                    // If we are using a single checkout directory option and its not a
                    //    SMC checkout from another job...
                    if (VC_useOneCheckoutDir && !usingExternalRepo) {

                        // we need to convert all the future job's workspaces to point to the original checkout
                        def originalWorkspace = "${env.WORKSPACE}"
                        println "scmStep executed here: " + originalWorkspace
                        scmStep()
                        print "Updating " + VC_Manage_Project + " to: " + originalWorkspace + "/" + VC_Manage_Project
                        VC_Manage_Project = originalWorkspace + "/" + VC_Manage_Project

                        def origSetup = VC_EnvSetup
                        def origTeardown = VC_EnvTeardown
                        def orig_VC_sharedArtifactDirectory = VC_sharedArtifactDirectory
                        def orig_VC_postScmStepsCmds = VC_postScmStepsCmds

                        if (isUnix()) {
                            VC_EnvSetup = VC_EnvSetup.replace("\$WORKSPACE" ,originalWorkspace)
                            VC_EnvTeardown = VC_EnvTeardown.replace("\$WORKSPACE" ,originalWorkspace)
                            VC_sharedArtifactDirectory = VC_sharedArtifactDirectory.replace("\$WORKSPACE" ,originalWorkspace)
                            VC_postScmStepsCmds = VC_postScmStepsCmds.replace("\$WORKSPACE" ,originalWorkspace)
                        } else {
                            originalWorkspace = originalWorkspace.replace('\\','/')

                            def tmpInfo = ""

                            // replace case insensitive workspace with WORKSPACE
                            tmpInfo = VC_EnvSetup.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                            VC_EnvSetup = tmpInfo.replace("%WORKSPACE%",originalWorkspace)

                            // replace case insensitive workspace with WORKSPACE
                            tmpInfo = VC_EnvTeardown.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                            VC_EnvTeardown = tmpInfo.replace("%WORKSPACE%",originalWorkspace)

                            // replace case insensitive workspace with WORKSPACE
                            tmpInfo = VC_sharedArtifactDirectory.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                            VC_sharedArtifactDirectory = tmpInfo.replace("%WORKSPACE%" ,originalWorkspace)

                            // replace case insensitive workspace with WORKSPACE
                            tmpInfo = VC_postScmStepsCmds.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                            VC_postScmStepsCmds = tmpInfo.replace("%WORKSPACE%" ,originalWorkspace)
                        }
                        print "Updating setup script " + origSetup + " \nto: " + VC_EnvSetup
                        print "Updating teardown script " + origTeardown + " \nto: " + origTeardown
                        print "Updating shared artifact directory " + orig_VC_sharedArtifactDirectory + " \nto: " + VC_sharedArtifactDirectory
                        print "Updating post SCM steps "  + orig_VC_postScmStepsCmds + "\nto: " + VC_postScmStepsCmds

                        // If there are post SCM checkout steps, do them now
                        if (VC_postScmStepsCmds.length() > 0) {
                            runCommands(VC_postScmStepsCmds, VC_EnvSetup, VC_useCILicense)
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
                            runCommands(VC_postScmStepsCmds, VC_EnvSetup, VC_useCILicense)
                        }
                    }

                    println "Created with VectorCAST Execution Version: " + VC_createdWithVersion

                    // Run the setup step to copy over the scripts
                    step([$class: 'VectorCASTSetup'])

                    runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/archive_extract_reports.py --archive""", VC_EnvSetup, VC_useCILicense)

                    // -------------------------------------------------------------------------------------------
                    // this part could be done with manageProject.getJobs() but it doesn't seem to be working VVV
                    def EnvData = ""

                    // Run a script to determine the compiler test_suite and environment
                    EnvData = runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/getjobs.py ${VC_Manage_Project} --type""", VC_EnvSetup, VC_useCILicense)

                    // for a groovy list that is stored in a global variable EnvList to be use later in multiple places
                    def lines = EnvData.split('\n')

                    // loop over each returned line from getjobs.py to determine if its a system test or unit test
                    //    and add it to the appropriate list
                    lines.each { line ->
                        def trimmedString = line.trim()
                        boolean containsData = trimmedString?.trim()
                        if (containsData) {
                            def testType = ""
                            def wordCount = trimmedString.split(/\s+/).length
                            def source = ""
                            def machine = ""
                            def compiler = ""
                            def test_suite = ""
                            def environment = ""

                            if (wordCount == 4) {
                                (testType, compiler, test_suite, environment) = trimmedString.split()
                            } else if (wordCount == 6) {
                                (testType, compiler, test_suite, environment, source, machine) = trimmedString.split()
                            } else {
                                print(trimmedString + " isn't splitting into 4/6 elements " + wordCount)
                            }

                            if (testType == "ST:") {
                                trimmedString = compiler + " " + test_suite + " " + environment + " " + source + " " + machine
                                // print("ST:" + trimmedString)
                                StEnvList = StEnvList + [trimmedString]
                            }
                            else if (testType == "UT:") {
                                trimmedString = compiler + " " + test_suite + " " + environment + " " + source + " " + machine
                                // print("UT:" + trimmedString)
                                UtEnvList = UtEnvList + [trimmedString]
                            }
                            else {
                                trimmedString = compiler + " " + test_suite + " " + environment + " " + source + " " + machine
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
                    setupManageProject(VC)

                    // Get the job list from the system test environment listed
                    def jobs = stepsForJobList(StEnvList, VC)

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
                    setupManageProject(VC)

                    // Get the job list from the unit test environment listed
                    def jobs = stepsForJobList(UtEnvList, VC)

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
                VCStages.generateOverallReports(VC, EnvList, )    
            }
        }

        // checking the build log of all the VC commands that have run
        // setup overall job's build descript with aggregated incremental build report and full status report
        stage('Check-Build-Log') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {

                        def mpName = getMPname(VC_Manage_Project)

                        def foundKeywords = ""
                        def boolean failure = false
                        def boolean unstable_flag = false

                        (foundKeywords, failure, unstable_flag) = VCUtils.checkBuildLogForErrors('complete_build.log')

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

                        } else if (VC_useCoverageHistory && !VC_useCoveragePlugin) {
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

                        runCommands(cmds, VC_EnvSetup, VC_useCILicense)

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
        // Currently supporting PC Lint Plus and Squore
        stage('Additional Tools') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {

                        // If there's a PC Lint Plus command...
                        if (VC_usePCLintPlus) {
                            // run the PC Lint Plus command
                            runCommands(VC_pclpCommand, VC_EnvSetup, VC_useCILicense)
                            // record the results with Warnings-NG plugin
                            recordIssues(tools: [pcLint(pattern: VC_pclpResultsPattern, reportEncoding: 'UTF-8')])
                            // Archive the PC Lint Results
                            archiveArtifacts allowEmptyArchive: true, artifacts: VC_pclpResultsPattern
                        }

                        // If we are using Squore...
                        if (VC_useSquore) {
                            // Generate the results from Squore and run the squore command which should publish the information to Squore Server
                            def cmd = "${VC_squoreCommand}"
                            runCommands(cmd, VC_EnvSetup, VC_useCILicense)

                            // Archive the Squore results
                            archiveArtifacts allowEmptyArchive: true, artifacts: 'xml_data/squore_results*.xml'
                        }
                    }
                }
            }
        }
    }
}
