package com.vectorcast.plugins.vectorcastexecution

class VectorCASTLogsImpl {
    def script
    VectorCASTLogsImpl(script) { this.script = script }

    def checkLogsForErrors(VC, String logText) {
        def found = []
        def failure_flag = false
        def unstable_flag = false

        def failurePhrases  = (VC?.failurePhrases  ?: []) as List
        def unstablePhrases = (VC?.unstablePhrases ?: []) as List

        unstablePhrases.each { p -> if (p && logText.contains(p)) { found << p; unstable_flag = true } }
        failurePhrases.each  { p -> if (p && logText.contains(p)) { found << p; failure_flag = true } }

        return [found.unique().join(", "), failure_flag, unstable_flag]
    }

    /**
     * Bridge-safe: NO pipeline steps here.
     *
     * inputs:
     *  - foundKeywords (String)
     *  - failure_flag (boolean)
     *  - unstable_flag (boolean)
     *  - coverageDiffHtml (String|null)
     *  - combinedIncrHtml (String|null)
     *  - fullReportHtml (String|null)
     *  - metricsReportHtml (String|null)
     *  - unitTestFailCount (String|null)
     *
     * returns plan:
     *  - descriptionAppend (String)
     *  - summaryIcon (String|null)
     *  - summaryHtml (String|null)
     *  - setResult (String|null)   // "UNSTABLE" / "FAILURE"
     *  - failMessage (String|null)
     *  - unstableMessage (String|null)
     *  - cleanupCmds (String)      // uses _RM macros
     */
    def checkBuildLogPlan(VC, Map inputs) {
        script.echo "Entry checkBuildLogPlan"

        def mpName = VC.helpersDsl.getMpName(VC.mpName)

        def foundKeywords = (inputs.foundKeywords ?: "").toString()
        def failure_flag = (inputs.failure_flag ? true : false)
        def unstable_flag = (inputs.unstable_flag ? true : false)

        def descAdd = ""
        if (foundKeywords) {
            script.echo "BP1"

            descAdd += "Problematic data found in console output, search the console output for the following phrases: ${foundKeywords}\n"
        }

        def hr = "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
        def summaryHtml = ""

        // coverage diffs is optional
        if (inputs.coverageDiffHtml) {
            script.echo "BP2"
            summaryHtml += hr + inputs.coverageDiffHtml
        }

        def hasFull = (inputs.fullReportHtml ? true : false)
        def hasMetrics = (inputs.metricsReportHtml ? true : false)

        def setResult = null
        def summaryIcon = null
        def finalSummary = null

        if (VC.useCBT) {
            script.echo "BP3"
            def hasCombined = (inputs.combinedIncrHtml ? true : false)

            if (hasCombined && hasFull && hasMetrics) {
                script.echo "BP4"
                summaryIcon = "icon-document icon-xlg"
                finalSummary =
                        hr + inputs.combinedIncrHtml +
                        hr + inputs.fullReportHtml +
                        hr + inputs.metricsReportHtml
                if (summaryHtml) {
                    script.echo "BP4.1"
                    finalSummary = summaryHtml + finalSummary
                }
            } else {
                script.echo "BP5"
                setResult = "UNSTABLE"
                summaryIcon = "icon-warning icon-xlg"
                finalSummary = "General Failure"
                descAdd += "General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information\n"
            }
        } else {
            script.echo "BP6"
            if (hasFull && hasMetrics) {
                script.echo "BP7"
                summaryIcon = "icon-document icon-xlg"
                finalSummary = inputs.fullReportHtml + "<br> " + inputs.metricsReportHtml
                if (summaryHtml){
                    script.echo "BP8"
                    finalSummary = summaryHtml + hr + finalSummary
                }
            } else {
                script.echo "BP9"
                setResult = "UNSTABLE"
                summaryIcon = "icon-warning icon-xlg"
                finalSummary = "General Failure"
                descAdd += "General Failure, Full Report or Metrics Report Not Present. Please see the console for more information\n"
            }
        }

        // unit test fail count handling (caller reads file)
        def utc = (inputs.unitTestFailCount ?: "").toString().trim()
        if (utc && utc != "0") {
            script.echo "BP10"
            descAdd += "Failed test cases, Junit will mark at least as UNSTABLE\n"
        }

        // cleanup commands: RETURN them; pipeline executes them
        def cleanupCmds = """
            _RM coverage_diffs.html_tmp
            _RM combined_incr_rebuild.tmp
            _RM ${mpName}_full_report.html_tmp
            _RM ${mpName}_metrics_report.html_tmp
        """

        // do not call script.error/script.unstable here; return intent
        def failMessage = null
        def unstableMessage = null
        if (failure_flag) {
            script.echo "BP11"

            setResult = "FAILURE"
            failMessage = "Raising Error: Problematic data found in console output, search the console output for the following phrases: ${foundKeywords}"
        } else if (unstable_flag) {
            script.echo "BP12"

            unstableMessage = "Triggering stage unstable because keywords found: ${foundKeywords}"
        }

        return [
                descriptionAppend : descAdd,
                summaryIcon       : summaryIcon,
                summaryHtml       : finalSummary,
                setResult         : setResult,
                failMessage       : failMessage,
                unstableMessage   : unstableMessage,
                cleanupCmds       : cleanupCmds
        ]
    }
}
