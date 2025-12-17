package com.vectorcast.plugins.vectorcastexecution


class VectorCASTAdditionalToolsImpl {

    def script

    VectorCASTAdditionalToolsImpl(script) { this.script = script }


    // ===============================================================
    //
    // Function : additionalTools
    // Inputs   : VC
    // Action   : Runs PCLP and/or Squore
    // Returns  : None
    // Stage    : Additional Tools
    //
    // ===============================================================

    def additionalTools(VC) {
        // Stage for additional tools from Vector

        // If there's a PC Lint Plus command...
        if (VC.usePclp) {
            // run the PC Lint Plus command
            runCommands(VC, VC.pLcpCmd)
            // record the results with Warnings-NG plugin
            script.recordIssues(
                    tools: [
                            script.pcLint(
                                    pattern: VC.pclpRsltPattern,
                                    reportEncoding: 'UTF-8'
                            )
                    ]
            )
            // Archive the PC Lint Results
            script.archiveArtifacts(
                    allowEmptyArchive: true,
                    artifacts: VC.pclpRsltPattern
            )
        }

        // If we are using Squore...
        if (VC.useSquore) {
            // Generate the results from Squore and run the squore command which should publish the information to Squore Server
            runCommands(VC, "${VC.squoreCmd}")

            // Archive the Squore results
            script.archiveArtifacts(
                    allowEmptyArchive: true,
                    artifacts: 'xml_data/squore_results*.xml'
            )
        }
    }
}