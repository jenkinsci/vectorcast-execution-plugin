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


VC_FailurePhrases = ["py did not execute correctly", 
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
                  "not recognized as an internal or external command"]
                
VC_UnstablePhrases = ["Value Line Error - Command Ignored"]                
       
// check log for errors

def checkLogsForErrors(log) {

    def boolean failure = false;
    def boolean unstable = false;
    def foundKeywords = ""

    // Check for unstable first
    // Loop over all the unstable keywords above
    VC_UnstablePhrases.each { 
        if (log.contains(it)) {
            // found a phrase considered unstable, mark the build accordingly  
            foundKeywords =  foundKeywords + it + ", " 
            unstable = true
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

    return [foundKeywords, failure, unstable]
}
// run commands on Unix (Linux) or Windows
def runCommands(cmds) {
    def boolean failure = false;
    def boolean unstable = false;
    def foundKeywords = ""

    // if its Linux run the sh command and save the stdout for analysis
    if (isUnix()) {
        log = sh  label: 'Running VectorCAST Commands', returnStdout: true, script: cmds
    } else {
        log = bat label: 'Running VectorCAST Commands', returnStdout: true, script: cmds
    }
    
    println log        
   
    (foundKeywords, failure, unstable) = checkLogsForErrors(log)
    if (failure) {
        throw new Exception ("Error in VectorCAST Commands: " + foundKeywords)
    }
}

// transform environment data, a line at a time, into an execution node 
// inputString is a space separated string = <<compiler>> <<testsuite>> <<environment>>
// return a node definition based on compiler for the specific job
def transformIntoStep(inputString) {
    
    // grab the compiler test_suite and environment out the single line
    def (compiler, test_suite, environment) = inputString.split()
    
    // set the stashed file name for later
    String stashName = "${env.JOB_NAME}_${compiler}_${test_suite}_${environment}-build-execute-stage"
    
    // return the auto-generated node and job
    // node is based on compiler label
    // this will route the job to a specific node matching that label 
    return {
        catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
            node ( compiler as String ){
            
                // call any SCM step if needed
                scmStep()
                
                // Run the setup step to copy over the scripts
                step([$class: 'VectorCASTSetup'])
                
                // get the manage projects full name and base name
                def mpFullName = VC_Manage_Project.split("/")[-1]
                def mpName = mpFullName.take(mpFullName.lastIndexOf('.'))  
                
                // setup the commands for building, executing, and transferring information
                cmds =  """
                    ${VC_EnvSetup}
                    
                    $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"                           --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" ${VC_sharedArtifactDirectory} --status"
                    $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"                           --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" --config VCAST_CUSTOM_REPORT_FORMAT=HTML"
                    $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"                           --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" --force --release-locks"
                    ${VC_Build_Preamble} $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"      --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}" --level ${compiler}/${test_suite} -e ${environment} --build-execute --incremental --output ${compiler}_${test_suite}_${environment}_rebuild.html"
                    $VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/generate-results.py  ${VC_Manage_Project}  --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --level ${compiler}/${test_suite} -e ${environment} --junit
                    
                    ${VC_EnvTeardown}

                """
                
                runCommands(cmds)

                if (VC_usingSCM && !VC_usingSharedArtifactDirectory) {
                    runCommands("""$VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/copy_build_dir.py    ${VC_Manage_Project}  ${compiler}/${test_suite} ${env.JOB_NAME}_${compiler}_${test_suite}_${environment} ${environment}""" )
                }

                stash includes: '**/*_rebuild*,execution/*.html, management/*.html, xml_data/**, *_build.tar', name: stashName as String
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

        // This stage gets the information on the manage project from the full-status report
        // Parsing the output determines the level and environment name
        stage('Get-Environment-Info') {
            steps {
                script {
                    // Get the repo (should only need the .vcm file)
                    // Grab the scripts
                    scmStep()
                    
                    // Run the setup step to copy over the scripts
                    step([$class: 'VectorCASTSetup'])
                    
                    // -------------------------------------------------------------------------------------------
                    // this part could be done with Manage_Project.getJobs() but it doesn't seem to be working VVV
                    def EnvData = ""
                    
                    // Run a script to determine the compiler test_suite and environment
                    if (isUnix()) {
                        EnvData = sh label: 'GettingEnviromentInfo', returnStdout: true, script: """$VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/getjobs.py ${VC_Manage_Project}"""
                    }
                    else {
                        EnvData = bat label: 'GettingEnviromentInfo', returnStdout: true, script: """$VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/getjobs.py ${VC_Manage_Project}"""
                    }
                    
                    // for a groovy list that is stored in a global variable EnvList to be use later in multiple places
                    def lines = EnvData.split('\n')
                    lines.each { line ->
                        def trimmedString = line.trim()
                        boolean containsData = trimmedString?.trim()

                        if (containsData && !trimmedString.contains("vpython"))  {
                            EnvList = EnvList + [trimmedString]
                        }
                    }
                    println EnvList
                    
                    // down to here                                                                            ^^^
                    // -------------------------------------------------------------------------------------------                    
                }
            }
        }

        // This is the stage that we use the EnvList via stepsForParallel >> transformIntoStep 
        stage('Build-Execute Stage') {
            steps {
                script {
                    jobs = stepsForParallel(EnvList)
                    parallel jobs
                }
            }
        }

        // Generating the reports needed for VectorCAST/Coverage plugin and JUnit
        stage('Generate-Overall-Reports') {
            steps {
                // Run the setup step to copy over the scripts
                step([$class: 'VectorCASTSetup'])
                
                script {
                    // unstash each of the files
                    EnvList.each {
                        (compiler, test_suite, environment) = it.split()
                        String stashName = "${env.JOB_NAME}_${compiler}_${test_suite}_${environment}-build-execute-stage"
                        
                        try {
                            unstash stashName as String
                        }
                        catch (Exception ex) {
                            println ex
                        }
                    } 
                    
                    // get the manage projects full name and base name
                    def mpFullName = VC_Manage_Project.split("/")[-1]
                    def mpName = mpFullName.take(mpFullName.lastIndexOf('.'))  

                    // run a script to extract stashed files and process data into xml reports
                    if (VC_usingSCM && !VC_usingSharedArtifactDirectory) {
                        runCommands("""$VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/extract_build_dir.py""" )
                    }
                    cmds =  """
                        set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
                         
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/incremental_build_report_aggregator.py --rptfmt HTML
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"                           --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  --full-status=${mpName}_full_report.html"
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"                           --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  --full-status > ${mpName}_full_report.txt"
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"                           --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  --create-report=aggregate   --output=${mpName}_aggregate_report.html"
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"                           --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  --create-report=metrics     --output=${mpName}_metrics_report.html"
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}/vc_scripts/managewait.py"                           --wait_time ${VC_waitTime} --wait_loops ${VC_waitLoops} --command_line "--project "${VC_Manage_Project}"  --create-report=environment --output=${mpName}_environment_report.html"
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/gen-combined-cov.py ${mpName}_aggregate_report.html
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/getTotals.py ${mpName}_full_report.txt
                        $VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/generate-results.py ${VC_Manage_Project} --final
                    """
                    
                    runCommands(cmds)

                }
                
                // Send reports to the code coverage plugin
                step([$class: 'VectorCASTPublisher', 
                    includes: 'xml_data/coverage_results*.xml', 
                    useThreshold: VC_Use_Threshold,        
                    healthyTarget:   VC_Healthy_Target, 
                    unhealthyTarget: VC_Unhealthy_Target
                    ])

                // Send test results to JUnit plugin
                step([$class: 'JUnitResultArchiver', allowEmptyResults: true, testResults: '**/test_results_*.xml'])

                // Save all the html, xml, and txt files
                archiveArtifacts '*.html'
                archiveArtifacts '*/*.html'
                archiveArtifacts '*/*.xml'
                archiveArtifacts '*.txt'
            }
        }
        
        stage('Check-Build-Log') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE', 
                    message : "Failure while checking reports...Please see the console for more information") {
                    script {
                            
                        println "getting the log"
                        
                        // get the console log - this requires running outside of the Groovy Sandbox
                        def logContent = Jenkins.getInstance()
                            .getItemByFullName(env.JOB_NAME)
                            .getBuildByNumber(
                                Integer.parseInt(env.BUILD_NUMBER))
                            .logFile.text
                            
                        def foundKeywords = ""
                        def mpFullName = VC_Manage_Project.split("/")[-1]
                        def mpName = mpFullName.take(mpFullName.lastIndexOf('.'))  
                        
                        boolean failure = false
                        boolean unstable = false
                        
                        println "checking for errors"
                        
                        (foundKeywords, failure, unstable) = checkLogsForErrors(logContent)
                        
                        println "found keywords: " + foundKeywords
                        println "failure = " + failure
                        println "unstable = " + unstable
                        
                        
                        // if the found keywords is great that the init value \n then we found something
                        // set the build description accordingly
                        if (foundKeywords.size() > 0) {
                            currentBuild.description = "Problematic data found in console output, search the console output for the following phrases: " + foundKeywords
                        }
                                            
                        // Make sure the build completed and we have two key reports
                        //   - CombinedReport.html (combined rebuild reports from all the environments)
                        //   - full status report from the manage project
                        if (fileExists('CombinedReport.html') && fileExists("${mpName}_full_report.html")) {
                        
                            // If we have both of these, add them to the summary in the "normal" job view
                            // Blue ocean view doesn't have a summary
                            manager.createSummary("monitor.png").appendText(readFile('CombinedReport.html') + "<br> " + readFile("${mpName}_full_report.html"), false)
                            
                        } else {
                        
                            // If not, something went wrong
                            // Make the build as unstable 
                            currentBuild.description = currentBuild.description + "Files missing - can't complete summary"
                            currentBuild.result = 'UNSTABLE'
                            manager.createSummary("warning.gif").appendText("General Failure", false, false, false, "red")
                            manager.buildUnstable()
                            manager.build.description = "General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information"
                            manager.addBadge("warning.gif", "General Error")
                        }                     

                        if (unstable) {
                            currentBuild.result = 'UNSTABLE'
                        }
                        if (failure) {
                            currentBuild.result = 'FAILURE'
                            println "Throwing exception: " + "Problematic data found in console output, search the console output for the following phrases: " + foundKeywords
                            throw new Exception ("Problematic data found in console output, search the console output for the following phrases: " + foundKeywords)
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
