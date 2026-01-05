package com.vectorcast.plugins.vectorcastexecution


class VectorCASTLogsImpl {

    def script
    VectorCASTLogsImpl(script) { this.script = script }

    // ===============================================================
    //
    // Function : checkLogsForErrors
    // Inputs   : log
    // Action   : Scans the input log file to for keywords listed above
    // Returns  : found foundKeywords, failure and/or unstable_flag
    // Notes    : Used to Check for VectorCAST build errors/problems
    //
    // ===============================================================

    def checkLogsForErrors(VC, log) {

        def failure_flag = false
        def unstable_flag = false
        def foundKeywords = []

        def failurePhrases  = (VC?.failurePhrases  ?: []) as List
        def unstablePhrases = (VC?.unstablePhrases ?: []) as List

        if (failurePhrases.isEmpty() && unstablePhrases.isEmpty()) {
            return ["", false, false]
        }

        log = log ?: ""

        // Check for unstable first
        // Loop over all the unstable keywords above
        unstablePhrases.each {
            if (log.contains(it)) {
                // found a phrase considered unstable, mark the build accordingly
                foundKeywords << it
                unstable_flag = true
            }
        }

        // The check for failure keywords first
        // Loop over all the failure keywords above
        failurePhrases.each {
            if (log.contains(it)) {
                // found a phrase considered failure, mark the build accordingly
                foundKeywords << it
                failure_flag = true
            }
        }
        return [foundKeywords.join(", "), failure_flag, unstable_flag]
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

    def checkBuildLogForErrors(VC, logFile) {

        def status = 0
        def cmd = ""
        def foundKeywords = ""
        def failure_flag = false
        def unstable_flag = false

        def failurePhrases  = (VC?.failurePhrases  ?: []) as List
        def unstablePhrases = (VC?.unstablePhrases ?: []) as List

        if (failurePhrases.isEmpty() && unstablePhrases.isEmpty()) {
            return ["", false, false]
        }

        script.writeFile (
                file: "phrases.txt",
                text: unstablePhrases.join("\n") + "\n" + failurePhrases.join("\n")
        )

        if (!script.vcastFileExists("phrases.txt")) {
            script.echo("phrases.txt not found in ${script.pwd()} after just writing int")
            // treat as no matches
            return ["", false, false]
        }


        if (script.isUnix()) {
            cmd =  "grep -f phrases.txt \"${logFile}\" > search_results.txt"
            status = (
                    script.sh (
                            label: 'Checking build log for errors',
                            returnStatus: true,
                            script: cmd
                    ) as int
            )
        } else {
            cmd =  "findstr /g:phrases.txt \"${logFile}\" > search_results.txt"
            status = (
                    script.bat(
                            label: 'Checking build log for errors',
                            returnStatus: true,
                            script: cmd
                    ) as int
            )
        }

        if (status == 0) {
            if (!script.vcastFileExists("search_results.txt")) {
                script.echo("search_results.txt not found in ${script.pwd()}")
                // treat as no matches
                return ["", false, false]
            }

            def res = checkLogsForErrors(VC, script.vcastReadFile("search_results.txt"))

            foundKeywords = res[0]
            failure_flag  = res[1]
            unstable_flag = res[2]

        } else if (status != 1) {
            script.error("Error in checking build log file: ${cmd}")
        }
        return [foundKeywords, failure_flag, unstable_flag]
    }

    // ===============================================================
    //
    // Function : checkBuildLog
    // Inputs   : VC
    // Action   : Concatenate build logs into one file
    // Returns  : None
    // Stage    : Check-Build-Log
    //
    // ===============================================================

    def checkBuildLog(VC) {
        // checking the build log of all the VC commands that have run
        // setup overall job's build descript with aggregated incremental build report and full status report

        def mpName = VC.helpersDsl.getMpName(VC.mpName)

        def foundKeywords = ""
        def boolean failure = false
        def boolean unstable_flag = false

        (foundKeywords, failure, unstable_flag) = VC.logsDsl.checkBuildLogForErrors(VC,'complete_build.log')

        // if the found keywords is great that the init value \n then we found something
        // set the build description accordingly
        if (foundKeywords) {
            script.currentBuild.description += "Problematic data found in console output, search the console output for the following phrases: " + foundKeywords + "\n"
        }

        // Make sure the build completed and we have two key reports
        //   - Using CBT - CombinedReport.html (combined rebuild reports from all the environments)
        //   - full status report from the manage project

        // Grab the coverage differences
        def summaryText = ""

        if (script.vcastFileExists('coverage_diffs.html_tmp')) {
            summaryText += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
            summaryText += script.vcastReadFile('coverage_diffs.html_tmp')

        } else if (VC.useCoverHist && !VC.useCoverPlgin) {
            script.echo "coverage_diffs.html_tmp missing"
        }

        if (VC.useCBT) {
            if (script.vcastFileExists('combined_incr_rebuild.tmp') && script.vcastFileExists("${mpName}_full_report.html_tmp") && script.vcastFileExists("${mpName}_metrics_report.html_tmp")) {
                // If we have both of these, add them to the summary in the "normal" job view
                // Blue ocean view doesn't have a summary
                summaryText += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
                summaryText += script.vcastReadFile('combined_incr_rebuild.tmp')
                summaryText += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
                summaryText += script.vcastReadFile("${mpName}_full_report.html_tmp")
                summaryText += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
                summaryText += script.vcastReadFile("${mpName}_metrics_report.html_tmp")

                VC.metricsDsl.pluginCreateSummary ("icon-document icon-xlg", summaryText)

            } else {
                if (script.vcastFileExists('combined_incr_rebuild.tmp')) {
                    script.echo "combined_incr_rebuild.tmp found"
                } else {
                    script.echo "combined_incr_rebuild.tmp missing"
                }
                if (script.vcastFileExists("${mpName}_full_report.html_tmp")) {
                    script.echo "${mpName}_full_report.html_tmp found"
                } else {
                    script.echo "${mpName}_full_report.html_tmp missing"
                }
                if (script.vcastFileExists("${mpName}_metrics_report.html_tmp")) {
                    script.echo "${mpName}_metrics_report.html_tmp found"
                } else {
                    script.echo "${mpName}_metrics_report.html_tmp missing"
                }

                // If not, something went wrong... Make the build as unstable
                script.currentBuild.result = 'UNSTABLE'
                VC.metricsDsl.pluginCreateSummary ("icon-warning icon-xlg", "General Failure")
                script.currentBuild.description += "General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\n"
            }
        } else {
            if (script.vcastFileExists("${mpName}_full_report.html_tmp") && script.vcastFileExists("${mpName}_metrics_report.html_tmp")) {
                // If we have both of these, add them to the summary in the "normal" job view
                // Blue ocean view doesn't have a summary

                summaryText += script.vcastReadFile("${mpName}_full_report.html_tmp") + "<br> " + script.vcastReadFile("${mpName}_metrics_report.html_tmp")
                VC.metricsDsl.pluginCreateSummary ("icon-document icon-xlg", summaryText)

            } else {
                // If not, something went wrong... Make the build as unstable
                script.currentBuild.result = 'UNSTABLE'
                VC.metricsDsl.pluginCreateSummary ("icon-warning icon-xlg", "General Failure")
                script.currentBuild.description += "General Failure, Full Report or Metrics Report Not Present. Please see the console for more information\n"
            }
        }

        // Remove temporary files used for summary page
        def cmds = """
            _RM coverage_diffs.html_tmp
            _RM combined_incr_rebuild.tmp
            _RM ${mpName}_full_report.html_tmp
            _RM ${mpName}_metrics_report.html_tmp
        """

        VC.execDsl.runCommands(VC, cmds)

        // use unit_test_fail_count.txt to see if there were any failed test cases
        // if any failed test cases, Junit will mark as at least unstable.
        def unitTestErrorCount = ""
        unitTestErrorCount = script.vcastReadFile("unit_test_fail_count.txt")
        if (unitTestErrorCount != "0") {
            script.currentBuild.description += "Failed test cases, Junit will mark at least as UNSTABLE\n"
        }
        if (failure) {
            script.currentBuild.result = 'FAILURE'
            script.error ("Raising Error: " + "Problematic data found in console output, search the console output for the following phrases: " + foundKeywords)
        } else if (unstable_flag) {
            script.unstable("Triggering stage unstable because keywords found: " + foundKeywords)
        }
    }
}
