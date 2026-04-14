// ===============================================================
//
// Generic file from VectorCAST Pipeline Plug-in DO NOT ALTER
//
// ===============================================================

// Code Coverage threshold numbers
def VC_Healthy_Target = [ maxStatement: 100, maxBranch: 100, maxFunctionCall: 100, maxFunction: 100, maxMCDC: 100,
                          minStatement: 20,  minBranch: 20,  minFunctionCall: 20,  minFunction: 20,  minMCDC: 20]

def VC_Use_Threshold = true

def VC_failurePhrases = [
        "No valid edition(s) available",
        "py did not execute correctly",
        "Traceback (most recent call last)",
        "Failed to acquire lock on environment",
        "Environment Creation Failed",
        "Error with Test Case Management Report",
        "FLEXlm Error",
        "Unable to obtain license",
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
        "Invalid Workspace.",
        "not being accessed by another process",
        "not permitted in continuous integration mode",
        "has been opened by a newer version of VectorCAST"
]

def VC_unstablePhrases = [
        "Dropping probe point",
        "Value Line Error - Command Ignored",
        "INFO: Problem parsing test results file",
        "INFO: File System Error",
        "ERROR: Error accessing DataAPI",
        "ERROR: Undefined Error",
        "Unapplied Test Data"
]

// Gathering global variables to be used by functions
def VC = [
    mpName:             VC_Manage_Project,
    setup:              VC_EnvSetup,
    preamble:           VC_Build_Preamble,
    teardown:           VC_EnvTeardown,
    label:              VC_Agent_Label,
    oneChkDir:          VC_useOneCheckoutDir,
    usingSCM:           VC_usingSCM,
    postSCMSteps:       VC_postScmStepsCmds,
    maxParallel:        VC_maxParallel,
    useRGW3:            VC_useRGW3,
    waitTime:           VC_waitTime,
    waitLoops:          VC_waitLoops,
    useCI:              VC_useCILicense,
    useCBT:             VC_useCBT,
    useCoverPlgin:      VC_useCoveragePlugin,
    sharedBldDir:       VC_sharedArtifactDirectory,
    useCoverHist:       VC_useCoverageHistory,
    strictImp:          VC_useStrictImport,
    useImpRst:          VC_useImportedResults,
    useLocImpRst:       VC_useLocalImportedResults,
    useExtImpRst:       VC_useExternalImportedResults,
    extRst:             VC_externalResultsFilename,
    usePclp:            VC_usePCLintPlus,
    pLcpCmd:            VC_pclpCommand,
    pclpRsltPattern:    VC_pclpResultsPattern,
    useSquore:          VC_useSquore,
    squoreCmd:          VC_squoreCommand,
    healthyTarget:      VC_Healthy_Target,
    useThreshold:       VC_Use_Threshold,
    failurePhrases:     VC_failurePhrases,
    unstablePhrases:    VC_unstablePhrases,
    createdWithVersion: VC_createdWithVersion,
    vcastProjectDir:    (env.VCAST_PROJECT_DIR?.trim() ? env.VCAST_PROJECT_DIR.trim() : ""),
    forceNodeExecName:  (env.VCAST_FORCE_NODE_EXEC_NAME?.trim() ? env.VCAST_FORCE_NODE_EXEC_NAME.trim() : ""),
    jobName:            env.JOB_Name,

    // below are the DSL script shortcuts
    execDsl:            VectorCASTExecution,
    utilsDsl:           VectorCASTUtils
]

// ===============================================================
def runCommands(cmds) {
    if (!cmds?.trim()) {
        return ("")
    }

    // clean out old command.log file
    writeFile file: "command.log", text: ""

    echo "Running commands ${cmds}"

    if (isUnix()) {
        sh label : 'Running VectorCAST Commands', returnStdout: false, script: cmds
    } else {
        bat label : 'Running VectorCAST Commands', returnStdout: false, script: cmds
    }
    // get the log file and return it
    return fileExists('command.log') ? readFile('command.log') : ''
}

// ===============================================================
def pluginCreateSummary(inIcon, inText) {
    try {
        addSummary icon: inIcon, text: inText
    } catch (NoSuchMethodError e) {
        createSummary icon: inIcon, text: inText
    }
}

// ===============================================================
def makeStepFromSpec(VC, spec) {
    return {
        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
            node(spec.nodeID as String) {

                if (!VC.oneChkDir) {
                    getRepo(VC)
                }

                step([$class: 'VectorCASTSetup'])

                // run commands via your CPS-safe runner
                def buildLogText = runCommands(spec.cmds)
                
                def (foundKeywords, failureFlag, unstableFlag) = checkLogsForErrors(VC, buildLogText)

                def fixedJobName = VectorCASTUtils.fixUpName("${env.JOB_NAME}")
                def key = "${spec.compiler}_${spec.test_suite}_${spec.environment}"
		
                // if we didn't fail and don't have a shared artifact directory - we may have to copy back build directory artifacts...
                if (!failureFlag && !VC.sharedBldDir) {

                    // if we are using an SCM checkout and we aren't using a single checkout directory, we need to copy back build artifacts
                    if (VC.usingSCM && !VC.oneChkDir) {
                        def cmds = VectorCASTExecution.getRunCommands(VC, """_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/copy_build_dir.py ${VC.mpName} --level ${spec.level} --basename ${fixedJobName}_${key} --environment ${spec.environment}""")

                        buildLogText += runCommands(cmds)
                    }
                }

                writeFile file: "${key}_build.log",
                          text: buildLogText

                def stashIncludes = [
                    "${key}_build.log",
                    "${key}_rebuild.html",
                    "${fixedJobName}_${key}_build.tar",
                ].join(', ')

                stash includes: stashIncludes,
                        name: spec.stashName
            }
        }
    }
}

// ===============================================================
def stepsForJobList(VC, localEnvList) {
    def jobList = [:]
    localEnvList.each { line ->
        def spec = VectorCASTExecution.buildStepSpec(VC, line)
        jobList[line] = makeStepFromSpec(VC, spec)
    }

    return jobList
}

// ===============================================================
def concatenateBuildLogs(logFileNames, outputFileName) {
    def cmd = ""
    def err = ""
    if (isUnix()) {
        cmd =  "cat "
        err = "/dev/null"
    } else {
        cmd =  "type "
        err = "nul"
    }

    cmd += logFileNames.join(" ") + " > ${outputFileName} 2>${err}"

    return runCommands(cmd)
}

// ===============================================================
def getRepo(VC) {
    scmStep()

    // If there are post SCM checkout steps, do them now
    if (VC.postSCMSteps) {
        def cmds = VectorCASTExecution.getRunCommands(VC, VC.postSCMSteps)
        runCommands(cmds)
    }
}

// ===============================================================
def readIfExists = { f -> fileExists(f) ? readFile(f) : null }

// ===============================================================
def checkLogsForErrors(VC, String logText) {
    def found = []
    def failure_flag = false
    def unstable_flag = false
    logText = logText ?: ""   // guard null

    def failurePhrases  = (VC?.failurePhrases  ?: []) as List
    def unstablePhrases = (VC?.unstablePhrases ?: []) as List

    unstablePhrases.each { p -> if (p && logText.contains(p)) { found << p; unstable_flag = true } }
    failurePhrases.each  { p -> if (p && logText.contains(p)) { found << p; failure_flag = true } }

    return [found.unique().join(", "), failure_flag, unstable_flag]
}

// ***************************************************************
//
//              VectorCAST Execution Pipeline
//
// ***************************************************************

pipeline {

    // Use the input from the job creation label as for the "main job"
    agent {label VC.label as String}

    stages {
        stage('Update for Single Checkout') {
            steps {
                script {
                    def needsCheckout = VectorCASTSingleCheckout.updateForSingleCheckout(VC)
                    if (needsCheckout) {
                        getRepo(VC)
                    }
                }
            }
        }
        stage('Get Environment Info') {
            steps {
                script {

                    if (currentBuild.description == null) {
                        currentBuild.description = ""
                    }

                    if (!VC.oneChkDir) {
                        getRepo(VC)
                    }

                    step([$class: 'VectorCASTSetup'])

                    def cmds = """"""
                    cmds += """_VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/archive_extract_reports.py --archive
                               _VECTORCAST_DIR/vpython "${env.WORKSPACE}"/vc_scripts/getjobs.py ${VC.mpName} --type
                    """

                    def runCmds = VectorCASTExecution.getRunCommands(VC, cmds)
                    def getJobsLog = runCommands(runCmds)
                    def (UtEnvList, StEnvList) = VectorCASTUtils.getEnvironmentInfo(getJobsLog)
                    VC.StEnvList = StEnvList
                    VC.UtEnvList = UtEnvList
                }
            }
        }
        // This is the stage that we use the EnvList via stepsForJobList >> transformIntoStep
        stage('System Test Build Execute Stage') {
            steps {
                script {
                
                    if (vc.useImpRst) {
                        if (vc.useLocImpRst) {
                            try {
                                copyArtifacts filter: "${mpName}_results.vcr", fingerprintArtifacts: true, optional: true, projectName: "${env.JOB_NAME}", selector: lastSuccessful()
                            } catch(Exception e) {
                                print "No result artifact to use"
                            }
                        }
                    }

                    runCommands(VectorCASTExecution.getSetupManageProject(VC))

                    // Get the job list from the system test environment listed
                    def jobs = stepsForJobList(VC, VC.StEnvList)

                    // run each of those jobs in serial
                    jobs.each { name, job ->
                        echo ("Running System Test Job: " + name)
                        job.call()
                    }
                }
            }
        }
        // This is the stage that we use the EnvList via stepsForJobList >> transformIntoStep
        stage('Unit Test Build Execute Stage') {
            steps {
                script {
                    runCommands(VectorCASTExecution.getSetupManageProject(VC))

                    // Get the job list from the unit test environment listed
                    def jobs = stepsForJobList(VC, VC.UtEnvList)

                    if (VC.maxParallel) {
                        def runningJobs = [:]
                        jobs.each { job ->
                            runningJobs.put(job.key, job.value)
                            if (runningJobs.size() == VC.maxParallel) {
                                parallel(runningJobs)
                                runningJobs = [:]
                            }
                        }
                        if (runningJobs.size() > 0) {
                            parallel(runningJobs)
                            runningJobs = [:]
                        }
                    } else {
                        // run those jobs in parallel
                        parallel(jobs)
                    }
                }
            }
        }
        stage('Generate Overall Reports') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    script {
                        def buildFileNames = []
                        def cmds = ""
                        def buildLogText = ""

                        (VC.UtEnvList + VC.StEnvList).each {
                            def ret = VectorCASTMetrics.getMetricsEnvCmds(VC, it)

                            buildFileNames << ret.buildFileName

                            unstash(ret.stashName)
                            buildLogText += runCommands(cmds)
                        }

                        concatenateBuildLogs(buildFileNames, "unstashed_build.log")

                        cmds = VectorCASTMetrics.getMetricsCmds(VC, [""])

                        buildLogText += runCommands(cmds)

                        writeFile file: "metrics_build.log", text: buildLogText
                        buildFileNames << "metrics_build.log"
                        concatenateBuildLogs(buildFileNames, "complete_build.log")
                        def (foundKeywords, failureFlag, unstableFlag) = checkLogsForErrors(VC, readIfExists("complete_build.log"))

                        if (failureFlag) throw new Exception ("Error in Commands: " + foundKeywords)

                        if (VC.useCoverPlgin) {
                            // Send reports to the Jenkins Coverage Plugin
                            discoverReferenceBuild()
                            if (VC.useCoverHist) {
                                recordCoverage qualityGates: [[baseline: 'PROJECT_DELTA', criticality: 'NOTE', metric: 'LINE', threshold: -0.001], [baseline: 'PROJECT_DELTA', criticality: 'FAILURE', metric: 'BRANCH', threshold: -0.001]], tools: [[parser: 'VECTORCAST', pattern: 'xml_data/cobertura/coverage_results*.xml']]
                            } else {
                                recordCoverage tools: [[parser: 'VECTORCAST', pattern: 'xml_data/cobertura/coverage_results*.xml']]
                            }
                        } else {
                            def currResult = ""
                            if (VC.useCoverHist) {
                                currResult = currentBuild.result
                            }

                            // Send reports to the VectorCAST Coverage Plugin
                            step([$class: 'VectorCASTPublisher',
                                  includes: 'xml_data/coverage_results*.xml',
                                  useThreshold: VC.useThreshold,
                                  healthyTarget:   VC.healthyTarget,
                                  useCoverageHistory: VC.useCoverHist,
                                  maxHistory : 20])

                            if (VC.useCoverHist) {
                                if ((currResult != currentBuild.result) && (currentBuild.result == 'FAILURE')) {
                                    pluginCreateSummary("icon-error icon-xlg", "Code Coverage Decreased")
                                    currentBuild.description += "Code coverage decreased.  See console log for details\n"
                                    addBadge(
                                            icon: "icon-error icon-xlg",
                                            text: "Code Coverage Decreased"
                                    )
                                }
                            }
                        }
                    }

                    // Send test results to JUnit plugin
                    step([$class: 'JUnitResultArchiver', keepLongStdio: true, allowEmptyResults: true, testResults: '**/test_results_*.xml'])

                    // Save all the html, xml, and txt files
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.html, xml_data/**/*.xml, unit_test_*.txt, **/*.png, **/*.css, complete_build.log, *_results.vcr'
                }
            }
        }

        stage('Check Build Log') {
          steps {
            script {
              def mpName = VectorCASTUtils.getMpName(VC.mpName)

              def (foundKeywords, failure_flag, unstable_flag) = checkLogsForErrors(VC, readIfExists('complete_build.log'))

              def inputs = [
                foundKeywords     : foundKeywords,
                failure_flag      : failure_flag,
                unstable_flag     : unstable_flag,
                coverageDiffHtml  : readIfExists('coverage_diffs.html_tmp'),
                combinedIncrHtml  : VC.useCBT ? readIfExists('combined_incr_rebuild.tmp') : null,
                fullReportHtml    : readIfExists("${mpName}_full_report.html_tmp"),
                metricsReportHtml : readIfExists("${mpName}_metrics_report.html_tmp"),
                unitTestFailCount : readIfExists('unit_test_fail_count.txt')
              ]

              // 2) bridged groovy returns a plan
              def plan = VectorCASTLogs.checkBuildLogPlan(VC, inputs)

              // 3) apply the plan using pipeline steps
              if (plan.descriptionAppend) currentBuild.description = (currentBuild.description ?: "") + plan.descriptionAppend
              if (plan.setResult) currentBuild.result = plan.setResult

              if (plan.summaryIcon && plan.summaryHtml) {
                pluginCreateSummary(plan.summaryIcon, plan.summaryHtml)
              }

              if (plan.cleanupCmds) {
                def cleanupRun = VectorCASTExecution.getRunCommands(VC, plan.cleanupCmds)
                runCommands(cleanupRun)
              }

              if (plan.failMessage) {
                error(plan.failMessage)
              } else if (plan.unstableMessage) {
                unstable(plan.unstableMessage)
              }
            }
          }
        }

        stage('Additional Tools') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {
                        def cmds = ""

                        // If there's a PC Lint Plus command...
                        if (VC.usePclp) {
                            cmds = VectorCASTExecution.getRunCommands(VC, VC.pLcpCmd)
                            runCommands(cmds)
                            recordIssues(tools: [pcLint(pattern: VC.pclpRsltPattern,reportEncoding: 'UTF-8')])
                            archiveArtifacts(allowEmptyArchive: true,artifacts: VC.pclpRsltPattern)
                        }

                        // If we are using Squore...
                        if (VC.useSquore) {
                            cmds = VectorCASTExecution.getRunCommands(VC, VC.squoreCmd)
                            runCommands(cmds)
                            archiveArtifacts(allowEmptyArchive: true,artifacts: 'xml_data/squore_results*.xml')
                        }
                    }
                }
            }
        }
    }
}
