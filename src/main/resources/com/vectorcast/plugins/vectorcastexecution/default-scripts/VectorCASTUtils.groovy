package com.vectorcast.plugins.vectorcastexecution


class VectorCASTUtilsImpl {

    def script

    VectorCASTUtilsImpl(script) { this.script = script }

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

        name = name ?: ""

        return (name.replace("/", "_")
                .replaceAll('\\%..', '_')
                .replaceAll('\\W', '_'))
    }

    // ===============================================================
    //
    // Function : getMpName
    // Inputs   : None
    // Action   : Returns the base name
    // Notes    : Used for creating report name
    //
    // ===============================================================

    def getMpName(mpNameInput) {
        // get the manage projects full name and base name
        def mpFullName = mpNameInput.split("/")[-1]
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
