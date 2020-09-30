// Code Coverage threshold numbers
// Basis path coverage is no longer support after VectorCAST 2019SP1
VC_Healthy_Target = [ maxStatement: 100, maxBranch: 100, maxFunctionCall: 100, maxMCDC: 100,
                      minStatement: 100, minBranch: 100, minFunctionCall: 100, minMCDC: 100]
                      
VC_Unhealthy_Target = [ maxStatement: 20, maxBranch: 20, maxFunctionCall: 20, maxMCDC: 20,
                        minStatement:  0, minBranch:  0, minFunctionCall:  0, minMCDC:  0]
                     

VC_Use_Threshold = true

// ===============================================================
// 
// Generic file from VectorCAST Pipeline Plug-in DO NOT ALTER
//
// ===============================================================


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
                
VC_UnstablePhrases = ["Value Line Error - Command Ignored"]                       

// setup the manage project to have preset options
def setupManageProject() {
    def cmds = """        
        _RM *_rebuild.html
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} ${VC_sharedArtifactDirectory} --status"  
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --force --release-locks"
        _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --config VCAST_CUSTOM_REPORT_FORMAT=HTML"
    """.stripIndent()

    runCommands(cmds)
}

// check log for errors
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
// run commands on Unix (Linux) or Windows
def runCommands(cmds, useLocalCmds = true) {
    def boolean failure = false;
    def boolean unstable_flag = false;
    def foundKeywords = ""
    def localCmds = """"""
     
    // if its Linux run the sh command and save the stdout for analysis
    if (isUnix()) {
        localCmds = """
            ${VC_EnvSetup}
            export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
            export VCAST_NO_FILE_TRUNCATION=1
            
            """.stripIndent()
        if (useLocalCmds) {
            cmds = localCmds + cmds
        }
        cmds = cmds.replaceAll("_VECTORCAST_DIR","\\\$VECTORCAST_DIR").replaceAll("_RM","rm -rf ")
        println "Running commands: " + cmds
        log = sh label: 'Running VectorCAST Commands', returnStdout: true, script: cmds
        
    } else {
        localCmds = """
            ${VC_EnvSetup}
            set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
            set VCAST_NO_FILE_TRUNCATION=1
            """.stripIndent()
        if (useLocalCmds) {
            cmds = localCmds + cmds
        }
        cmds = cmds.replaceAll("_VECTORCAST_DIR","%VECTORCAST_DIR%").replaceAll("_RM","DEL /Q ")
        println "Running commands: " + cmds
        log = bat label: 'Running VectorCAST Commands', returnStdout: true, script: cmds
    }
    
    println "Commands Output: " + log        
       
    return log
}

// Fixup name so it doesn't include / or %## or any other special characters
def fixUpName(name) {
    return name.replace("/","_").replaceAll('\\%..','_').replaceAll('\\W','_')
}
// transform environment data, a line at a time, into an execution node 
// inputString is a space separated string = <<compiler>> <<testsuite>> <<environment>>
// return a node definition based on compiler for the specific job
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
            
            node ( nodeID as String ){
            
                println "Starting Build-Execute Stage for ${compiler}/${test_suite}/${environment}"
            
                // call any SCM step if needed
                if (!VC_useOneCheckoutDir) {
                    scmStep()
                }
                
                // Run the setup step to copy over the scripts
                step([$class: 'VectorCASTSetup'])

                if (VC_usingSCM && !VC_useOneCheckoutDir) {
                    // set options for each manage project pulled out out of SCM
                    setupManageProject()
                }

                // get the manage projects full name and base name
                def mpFullName = VC_Manage_Project.split("/")[-1]
                def mpName = mpFullName.take(mpFullName.lastIndexOf('.'))  
                
                // setup the commands for building, executing, and transferring information
                cmds =  """
                    ${VC_EnvSetup}
                    ${VC_Build_Preamble} _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_UseCILicense} --level ${compiler}/${test_suite} -e ${environment} --build-execute ${VC_useCBT} --output ${compiler}_${test_suite}_${environment}_rebuild.html"
                    ${VC_EnvTeardown}
                """.stripIndent()
                
                def buildLogText = ""
                
                buildLogText = runCommands(cmds)
                    
                def foundKeywords = ""
                def boolean failure = false
                def boolean unstable_flag = false
                                        
                (foundKeywords, failure, unstable_flag) = checkLogsForErrors(buildLogText) 
                
                if (!failure && VC_sharedArtifactDirectory.length() == 0) {
                    writeFile file: "build.log", text: buildLogText

                    if (VC_usingSCM && !VC_useOneCheckoutDir) {
                        def fixedJobName = fixUpName("${env.JOB_NAME}")
                        buildLogText = runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/copy_build_dir.py ${VC_Manage_Project} ${compiler}/${test_suite} ${fixedJobName}_${compiler}_${test_suite}_${environment} ${environment}""" )
                    }
                }
                
                writeFile file: "${compiler}_${test_suite}_${environment}_build.log", text: buildLogText

                // no cleanup - possible CBT
                // use individual names
                def fixedJobName = fixUpName("${env.JOB_NAME}")
                stash includes: "${compiler}_${test_suite}_${environment}_build.log, **/${compiler}_${test_suite}_${environment}_rebuild.html, execution/*.html, management/*${compiler}_${test_suite}_${environment}*, xml_data/*${compiler}_${test_suite}_${environment}*, ${fixedJobName}_${compiler}_${test_suite}_${environment}_build.tar", name: stashName as String
                
                println "Finished Build-Execute Stage for ${compiler}/${test_suite}/${environment}"

                (foundKeywords, failure, unstable_flag) = checkLogsForErrors(buildLogText) 
                
                if (failure) {
                    error ("Error in Commands: " + foundKeywords)
                } else if (unstable_flag) {
                    unstable("Triggering stage unstable because keywords found: " + foundKeywords)
                }
            }
        }
    }
}

// Convert localEnvList to a job list that's used for parallel execution.
//     EnvList:
//       <<compiler1>> <<testsuite1>> <<environment1>>
//       ...
//       <<compiler>> <<testsuite>> <<environmentN>>
def stepsForParallel(localEnvList) {
    jobList = [:]
    localEnvList.each {
        jobList[it] =  transformIntoStep(it)
    }
    
    return jobList
}

// global environment list used to create pipeline jobs
EnvList = []
 
pipeline {

    agent {label VC_Agent_Label as String}
    
    stages {
        stage('Previous-Stage') {
            steps {
                script {
                    println "place holder for previous stages"
                }
            }
        }
        
        stage('Single-Checkout') {
            steps {
                script {
                    def usingExternalRepo = false;

                    try {
                        if ("${VCAST_FORCE_NODE_EXEC_NAME}".length() > 0) {
                            usingExternalRepo = true
                        }
                        else {
                            usingExternalRepo = false
                        }
                    } catch (exe) {
                       usingExternalRepo = false
                    }
                
                    if (VC_useOneCheckoutDir && !usingExternalRepo) {
                        VC_OriginalWorkspace = "${env.WORKSPACE}"
                        println "scmStep executed here: " + VC_OriginalWorkspace
                        scmStep()
                        print "Updating " + VC_Manage_Project + " to: " + VC_OriginalWorkspace + "/" + VC_Manage_Project
                        VC_Manage_Project = VC_OriginalWorkspace + "/" + VC_Manage_Project
                        
                        def origSetup = VC_EnvSetup
                        def origTeardown = VC_EnvTeardown
                        def orig_VC_sharedArtifactDirectory = VC_sharedArtifactDirectory
                        if (isUnix()) {
                            VC_EnvSetup = VC_EnvSetup.replace("\$WORKSPACE" ,VC_OriginalWorkspace)
                            VC_EnvTeardown = VC_EnvTeardown.replace("\$WORKSPACE" ,VC_OriginalWorkspace)
                            VC_sharedArtifactDirectory = VC_sharedArtifactDirectory.replace("\$WORKSPACE" ,VC_OriginalWorkspace)
                        } else {
                            VC_OriginalWorkspace = VC_OriginalWorkspace.replace('\\','/')
                            VC_EnvSetup = VC_EnvSetup.replace("%WORKSPACE%",VC_OriginalWorkspace)
                            VC_EnvTeardown = VC_EnvTeardown.replace("%WORKSPACE%",VC_OriginalWorkspace)
                            VC_sharedArtifactDirectory = VC_sharedArtifactDirectory.replace("%WORKSPACE%" ,VC_OriginalWorkspace)
                        }
                        print "Updating setup script " + origSetup + " \nto: " + VC_EnvSetup
                        print "Updating teardown script " + origTeardown + " \nto: " + origTeardown
                        print "Updating shared artifact directory " + orig_VC_sharedArtifactDirectory + " \nto: " + VC_sharedArtifactDirectory
                        }
                    else {
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
                    if (!VC_useOneCheckoutDir) {
                        // Get the repo (should only need the .vcm file)
                        scmStep()
                    }
                    
                    println "Created with VectorCAST Execution Version: " + VC_createdWithVersion

                    // Run the setup step to copy over the scripts
                    step([$class: 'VectorCASTSetup'])
                    
                    // -------------------------------------------------------------------------------------------
                    // this part could be done with Manage_Project.getJobs() but it doesn't seem to be working VVV
                    def EnvData = ""
                    
                    // Run a script to determine the compiler test_suite and environment
                    EnvData = runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/getjobs.py ${VC_Manage_Project}""", false )
                    
                    // for a groovy list that is stored in a global variable EnvList to be use later in multiple places
                    def lines = EnvData.split('\n')
                    lines.each { line ->
                        def trimmedString = line.trim()
                        boolean containsData = trimmedString?.trim()

                        if (containsData && !trimmedString.contains("vpython"))  {
                            EnvList = EnvList + [trimmedString]
                        }
                    }
                    
                    // down to here                                                                            ^^^
                    // -------------------------------------------------------------------------------------------                    
                }
            }
        }

        // This is the stage that we use the EnvList via stepsForParallel >> transformIntoStep 
        stage('Build-Execute Stage') {
            steps {
                script {
                    setupManageProject()
                    
                    jobs = stepsForParallel(EnvList)
                    parallel jobs
                }
            }
        }

        // Generating the reports needed for VectorCAST/Coverage plugin and JUnit
        stage('Generate-Overall-Reports') {
        
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    // Run the setup step to copy over the scripts
                    step([$class: 'VectorCASTSetup'])
                    
                    script {
                        def unstashedBuildLogText = ""
                        
                        // unstash each of the files
                        EnvList.each {
                            (compiler, test_suite, environment) = it.split()
                            String stashName = fixUpName("${env.JOB_NAME}_${compiler}_${test_suite}_${environment}-build-execute-stage")
                            
                            try {
                                unstash stashName as String
                                unstashedBuildLogText += readFile "${compiler}_${test_suite}_${environment}_build.log"
                                unstashedBuildLogText += '\n'
                                
                            }
                            catch (Exception ex) {
                                println ex
                            }
                        } 
                        
                        // get the manage projects full name and base name
                        def mpFullName = VC_Manage_Project.split("/")[-1]
                        def mpName = mpFullName.take(mpFullName.lastIndexOf('.'))  
                        def buildLogText = ""
                        
                        // if we are using SCM and not using a shared artifact directory...
                        if (VC_usingSCM && !VC_useOneCheckoutDir && VC_sharedArtifactDirectory.length() == 0) {
                            // run a script to extract stashed files and process data into xml reports                        
                            buildLogText += runCommands("""_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/extract_build_dir.py""" )
                        }        
                        // use unstashed build logs to get the skipped data
                        writeFile file: "unstashed_build.log", text: unstashedBuildLogText

                        // run the metrics at the end
                        buildLogText += runCommands("""_VECTORCAST_DIR/vpython  "${env.WORKSPACE}"/vc_scripts/generate-results.py  ${VC_Manage_Project} --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --junit --buildlog unstashed_build.log""")

                        cmds =  """                         
                            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/incremental_build_report_aggregator.py ${mpName} --rptfmt HTML
                            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/full_report_no_toc.py "${VC_Manage_Project}"
                            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  ${VC_UseCILicense} --create-report=aggregate   --output=${mpName}_aggregate_report.html"
                            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  ${VC_UseCILicense} --create-report=metrics     --output=${mpName}_metrics_report.html"
                            _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  ${VC_UseCILicense} --create-report=environment --output=${mpName}_environment_report.html"
                        """.stripIndent()
                        
                        buildLogText += runCommands(cmds)

                        writeFile file: "complete_build.log", text: unstashedBuildLogText + buildLogText
                        
                        (foundKeywords, failure, unstable_flag) = checkLogsForErrors(buildLogText) 
                    
                        if (failure) {
                            throw new Exception ("Error in Commands: " + foundKeywords)
                        }
                    }
                    
                    // Send reports to the code coverage plugin
                    step([$class: 'VectorCASTPublisher', 
                        includes: 'xml_data/coverage_results*.xml', 
                        useThreshold: VC_Use_Threshold,        
                        healthyTarget:   VC_Healthy_Target, 
                        unhealthyTarget: VC_Unhealthy_Target
                        ])

                    // Send test results to JUnit plugin
                    step([$class: 'JUnitResultArchiver', keepLongStdio: true, allowEmptyResults: true, testResults: '**/test_results_*.xml'])
                }            

                // Save all the html, xml, and txt files
                archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.html, **/*.xml, **/*.txt, complete_build.log'
            }
        }
        
        stage('Check-Build-Log') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {
                                                    
                        // get the console log - this requires running outside of the Groovy Sandbox
                        def logContent = readFile 'complete_build.log'
                            
                        def mpFullName = VC_Manage_Project.split("/")[-1]
                        def mpName = mpFullName.take(mpFullName.lastIndexOf('.'))  
                        
                        def foundKeywords = ""
                        def boolean failure = false
                        def boolean unstable_flag = false
                                                
                        (foundKeywords, failure, unstable_flag) = checkLogsForErrors(logContent) 

                        // if the found keywords is great that the init value \n then we found something
                        // set the build description accordingly
                        currentBuild.description = ""
                        if (foundKeywords.size() > 0) {
                            currentBuild.description = "Problematic data found in console output, search the console output for the following phrases: " + foundKeywords + "\n"
                        }
                                            
                        // Make sure the build completed and we have two key reports
                        //   - Using CBT - CombinedReport.html (combined rebuild reports from all the environments)
                        //   - full status report from the manage project
                        if (VC_useCBT) {
                            if (fileExists('combined_incr_rebuild.tmp') && fileExists("${mpName}_full_report.html_tmp")) {
                                // If we have both of these, add them to the summary in the "normal" job view
                                // Blue ocean view doesn't have a summary

                                def summaryText = readFile('combined_incr_rebuild.tmp') + "<br> " + readFile("${mpName}_full_report.html_tmp")
                                createSummary icon: "monitor.gif", text: summaryText
                                
                            
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
                                
                                // If not, something went wrong... Make the build as unstable 
                                currentBuild.result = 'UNSTABLE'
                                createSummary icon: "warning.gif", text: "General Failure"
                                currentBuild.description = "General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\n"
                            }                     
                        } else {
                            if (fileExists("${mpName}_full_report.html_tmp")) {
                                // If we have both of these, add them to the summary in the "normal" job view
                                // Blue ocean view doesn't have a summary

                                def summaryText = readFile("${mpName}_full_report.html_tmp")
                                createSummary icon: "monitor.gif", text: summaryText
                            
                            } else {
                                // If not, something went wrong... Make the build as unstable 
                                currentBuild.result = 'UNSTABLE'
                                createSummary icon: "warning.gif", text: "General Failure"
                                currentBuild.description = "General Failure, Full Report Not Present. Please see the console for more information\n"
                            }                                             
                        }

                        // Remove temporary files used for summary page
                        def cmds = """        
                            _RM combined_incr_rebuild.tmp
                            _RM ${mpName}_full_report.html_tmp
                        """.stripIndent()
                        
                        runCommands(cmds)
                        
                        def unitTestErrorCount = ""
                        unitTestErrorCount = readFile "unit_test_fail_count.txt"
                        if (unitTestErrorCount != "0") {
                            currentBuild.description += "Failed test cases, Junit will mark at least as UNSTABLE"
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
        stage('Next-Stage') {
            steps {
                script {
                    println "place holder for next stages"
                }
            }
        }
    }
}
