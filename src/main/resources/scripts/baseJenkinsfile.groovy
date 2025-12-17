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
    unstablePhrases:    VC_unstablePhrases
]

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
                    VectorCASTStages.updateForSingleCheckout(VC)
                }
            }
        }

        stage('Get Environment Info') {
            steps {
                script {
                    def envInfo = VectorCASTStages.getEnvironmentInfo(VC)

                    VC.UtEnvList  = envInfo[0]
                    VC.StEnvList  = envInfo[1]
                }
            }
        }

        // This is the stage that we use the EnvList via stepsForJobList >> transformIntoStep
        stage('System Test Build Execute Stage') {
            steps {
                script {
                    VectorCASTStages.systemTestingSerial(VC)
               }
            }
        }

        // This is the stage that we use the EnvList via stepsForJobList >> transformIntoStep
        stage('Unit Test Build Execute Stage') {
            steps {
                script {
                    VectorCASTStages.unitTestingParallel(VC)
                }
            }
        }

        stage('Generate Overall Reports') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {

                    // Run the setup step to copy over the scripts
                    step([$class: 'VectorCASTSetup'])

                    // run script to unstash files and generate results/reports
                    script {
                        // --report_only_failures - report on only failed test cases
                        // --no_full_report - Don't generate full reports
                        // --dont-generate-individual-reports -
                        //      Below VC2019 - this just controls execution report generate.
                        //      VC2019 and later - execution reports for each testcase won't be generated
                        //
                        // Example: extraResultOptions = ['--report_only_failures', '--no_full_report', '--dont-generate-individual-reports' ]

                        // add options from above as needed
                        def extraResultOptions = []
                        VectorCASTStages.generateOverallReports(VC, extraResultOptions)
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
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {
                        VectorCASTStages.checkBuildLog(VC)
                    }
                }
            }
        }

        stage('Additional Tools') {
            steps {
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    script {
                        VectorCASTStages.additionalTools(VC)
                    }
                }
            }
        }
    }
}
