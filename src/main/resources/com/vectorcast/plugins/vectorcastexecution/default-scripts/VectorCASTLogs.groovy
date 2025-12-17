package com.vectorcast.plugins.vectorcastexecution


// Current one from Layla @ TTband
class VectorCASTUtilsImpl {

    def script
    VectorCASTUtilsImpl(script) { this.script = script }

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

        if (!script.fileExists("phrases.txt")) {
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
            if (!script.fileExists("search_results.txt")) {
                script.echo("search_results.txt not found in ${script.pwd()}")
                // treat as no matches
                return ["", false, false]
            }

            def res = checkLogsForErrors(VC, script.readFile("search_results.txt"))

            foundKeywords = res[0]
            failure_flag  = res[1]
            unstable_flag = res[2]

        } else if (status != 1) {
            script.error("Error in checking build log file: ${cmd}")
        }
        return [foundKeywords, failure_flag, unstable_flag]
    }
}
