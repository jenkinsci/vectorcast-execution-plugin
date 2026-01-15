package com.vectorcast.plugins.vectorcastexecution

class VectorCASTExecutionImpl {

    def script

    VectorCASTExecutionImpl(script) { this.script = script }

    // BRIDGE-SAFE: no pipeline steps here
    def buildStepSpec(VC, inputString) {
        def compiler="", test_suite="", environment="", source="", machine="", level=""

        def trimmed = (inputString ?: "").trim()
        def wordCount = trimmed ? trimmed.split(/\s+/).length : 0
        if (wordCount == 3) {
            (compiler, test_suite, environment) = trimmed.split()
            level = "${compiler}/${test_suite}"
        } else if (wordCount == 5) {
            (compiler, test_suite, environment, source, machine) = trimmed.split()
            level = "${source}/${machine}/${compiler}/${test_suite}"
        }

        def stashName = VC.helpersDsl.fixUpName("${VC.jobName}_${compiler}_${test_suite}_${environment}-build-execute-stage")
        def nodeID = (VC.forceNodeExecName ?: compiler)

        def cmds = "${VC.setup}\n"

        if (VC.useRGW3) {
            cmds += "_VECTORCAST_DIR/vpython \"${script.env.WORKSPACE}\"/vc_scripts/patch_rgw_directory.py \"${VC.mpName}\"\n"
        }
        cmds += """
            ${VC.preamble} _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}" ${VC.useCI} --level ${level} -e ${environment} --build-execute ${VC.useCBT} --output ${compiler}_${test_suite}_${environment}_rebuild.html"
            ${VC.teardown}
        """

        cmds = getRunCommands(VC,cmds)

        return [
                compiler: compiler,
                test_suite: test_suite,
                environment: environment,
                source: source,
                machine: machine,
                level: level,
                nodeID: nodeID,
                stashName: stashName,
                cmds: cmds
        ]
    }


    // ===============================================================
    // Function : stripLeadingWhitespace
    // ===============================================================
    static def stripLeadingWhitespace(str) {
        def lines = (str ?: "").split('\n')
        def trimmedString = ""
        lines.each { line -> trimmedString += line.trim() + "\n" }
        return trimmedString
    }

    // ===============================================================
    //
    // Function : getRunCommands
    // Inputs   : command list
    // Action   : 1. Adds VC Setup calls to beginning of script
    //            2. If using CI licenses, it set the appropriate environment variables
    //            3. Replaces keywords for windows/linux
    //            4. Calls the command
    //            5. Reads the log and return the log file (prints as well)
    // Returns  : stdout/stderr from the commands
    // Notes    : Used widely
    //
    // ===============================================================
    def getRunCommands(VC, cmds) {
        def localCmds = ""

        cmds = cmds ?: ""

        // if its Linux run the script.sh command and save the stdout for analysis
        if (script.isUnix()) {
            // add VC setup to beginning of script
            // add extra env vars to make debugging of commands useful
            // add extra env for reports
            localCmds = """
                ${VC.setup}
                export VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
                export VCAST_NO_FILE_TRUNCATION=1
                export VCAST_RPTS_SELF_CONTAINED=FALSE
                """

            // if using CI licenses add in both CI license env vars
            if (VC.useCI) {
                localCmds += """
                    export VCAST_USING_HEADLESS_MODE=1
                    export VCAST_USE_CI_LICENSES=1
                """
            }
            cmds = localCmds + cmds
            cmds = stripLeadingWhitespace(
                    cmds.replace("_VECTORCAST_DIR", "\$VECTORCAST_DIR")
                            .replace("_RM", "rm -rf ")
                            .replace("_COPY", "cp -p ")
                            .replace("_IF_EXIST", "if [[ -f ")
                            .replace("_IF_THEN", " ]] ; then ")
                            .replace("_ENDIF", "; fi")
            )

        } else {
            // add VC setup to beginning of script
            // add extra env vars to make debugging of commands useful
            // add extra env for reports
            localCmds = """
                @echo off
                ${VC.setup}
                set VCAST_RPTS_PRETTY_PRINT_HTML=FALSE
                set VCAST_NO_FILE_TRUNCATION=1
                set VCAST_RPTS_SELF_CONTAINED=FALSE
                """

            // if using CI licenses add in both CI license env vars
            if (VC.useCI) {
                localCmds += """
                    set VCAST_USING_HEADLESS_MODE=1
                    set VCAST_USE_CI_LICENSES=1
                """
            }
            cmds = localCmds + cmds
            cmds = stripLeadingWhitespace(
                    cmds.replace("_VECTORCAST_DIR", "%VECTORCAST_DIR%")
                            .replace("_RM", "DEL /Q ")
                            .replace("_COPY", "copy /y /b ")
                            .replace("_IF_EXIST", "if exist ")
                            .replace("_IF_THEN", " ( ")
                            .replace("_ENDIF", " )")
            )
        }

        return cmds
    }

    // ===============================================================
    // Function : getSetupManageProject
    // ===============================================================
    def getSetupManageProject(VC) {

        def mpName = VC.helpersDsl.getMpName(VC.mpName)

        mpName = mpName ?: "Unknown"

        def cmds = ""

        if (VC.sharedBldDir) {
            cmds += """
                _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}" ${VC.useCI} ${VC.sharedBldDir} --status"
            """
        }

        if (VC.strictImp) {
            cmds += """
                _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}" ${VC.useCI} --config=VCAST_STRICT_TEST_CASE_IMPORT=TRUE"
            """
        }

        cmds += """
            _RM *_rebuild.html
            _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}" ${VC.useCI} --status"
            _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}" ${VC.useCI} --force --release-locks"
            _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}" ${VC.useCI} --config VCAST_CUSTOM_REPORT_FORMAT=HTML"
        """

        if (VC.oneChkDir) {
            cmds += """
                _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}" ${VC.useCI} --config VCAST_DEPENDENCY_CACHE_DIR=./vcqik"
            """
        }

        return getRunCommands(VC,cmds)

    }
}
