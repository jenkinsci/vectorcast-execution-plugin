package com.vectorcast.plugins.vectorcastexecution

class VectorCASTEnvInfoImpl {

    def script

    VectorCASTEnvInfoImpl(script) { this.script = script }

    // ===============================================================
    //
    // Function : getEnvironmentInfo
    // Inputs   : VC
    // Action   : Gets the environment information
    // Returns  : None
    // Stage    : Get-Environment-Info
    //
    // ===============================================================
    def getEnvironmentInfo(String getJobLog) {

        def UtEnvList = []
        def StEnvList = []

        // for a groovy list that is stored locally and returned to be use later in multiple places
        def lines = getJobLog.split('\n')

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
                    script.echo(trimmedString + " isn't splitting into 4/6 elements: " + wordCount)
                }

                if (testType == "ST:") {
                    trimmedString = compiler + " " + test_suite + " " + environment + " " + source + " " + machine
                    StEnvList << trimmedString
                } else if (testType == "UT:") {
                    trimmedString = compiler + " " + test_suite + " " + environment + " " + source + " " + machine
                    UtEnvList << trimmedString
                }
            }
        }

        return [UtEnvList, StEnvList]
    }
}