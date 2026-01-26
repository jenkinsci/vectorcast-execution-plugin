package com.vectorcast.plugins.vectorcastexecution

class VectorCASTMetricsImpl {

    def script

    VectorCASTMetricsImpl(script) { this.script = script }

    // ===============================================================
    //
    // Function : getMetricsEnvCmds
    // Inputs   : VC, envDesc
    // Action   : Generates the commands for generating metrics
    // Returns  : None
    // Stage    : Generate-Overall-Reports
    //
    // ===============================================================
    def getMetricsEnvCmds(VC, envDesc) {

        def compiler = ""
        def test_suite = ""
        def environment = ""
        def source = ""
        def machine = ""
        def level = ""

        def results = [
                cmds         : "",
                stashName    : "",
                buildFileName: ""
        ]

        def trimmedLine = envDesc.trim()
        def wordCount = trimmedLine.split(/\s+/).length
        if (wordCount == 3) {
            (compiler, test_suite, environment) = envDesc.split()
            level = compiler + "/" + test_suite
        } else if (wordCount == 5) {
            (compiler, test_suite, environment, source, machine) = envDesc.split()
            level = source + "/" + machine + "/" + compiler + "/" + test_suite
        }
        results.stashName = VC.helpersDsl.fixUpName("${VC.jobName}_${compiler}_${test_suite}_${environment}-build-execute-stage")
        results.buildFileName = "${compiler}_${test_suite}_${environment}_build.log "

        if (VC.sharedBldDir) {
            def fixedJobName = VC.helpersDsl.fixUpName("${script.env.JOB_NAME}")
            results.cmds += "_VECTORCAST_DIR/vpython \"${script.env.WORKSPACE}/vc_scripts/copy_build_dir.py\" ${VC.mpName} --level ${level} --basename ${fixedJobName}_${compiler}_${test_suite}_${environment} --environment ${environment} --notar\n"
        }

        results.cmds = (results.cmds?.trim()) ? VC.execDsl.getRunCommands(VC, results.cmds) : ""

        return results
    }

    // ===============================================================
    //
    // Function : getMPpath
    // Inputs   : mpFullPath
    // Action   : Returns the path name to the manage project's directory
    // Notes    : Used for accessing the build directory
    //
    // ===============================================================
    def getMPpath(mpFullPath) {
        // get the manage projects full name and base name
        def mpFullName = mpFullPath
        def mpPath = ""
        if (mpFullName.toLowerCase().endsWith(".vcm")) {
            mpPath = mpFullName.take(mpFullName.lastIndexOf('.'))
        } else {
            mpPath = mpFullName
        }
        return mpPath
    }

    // ===============================================================
    // Function : formatPath (PRIVATE)
    // Notes    : On Windows, changes / separators to \
    // ===============================================================
    def formatPath(inPath) {
        def outPath = inPath ?: ""
        if (!script.isUnix()) {
            outPath = inPath.replace("/", "\\\\")
        }
        return outPath
    }

    def getMetricsCmds(VC, List extraResultOptions) {

        def cmds = ""

        def extraOptStr = (extraResultOptions ?: [])
                .collect { it?.toString()?.trim() }
                .findAll { it }
                .join(' ')

        // get the manage project's base name for use in rebuild naming
        def mpName = VC.helpersDsl.getMpName(VC.mpName)

        if (VC.sharedBldDir) {
            def artifact_dir = ""
            try {
                artifact_dir = VC.sharedBldDir.split(" ")[1]
            }
            catch (Exception ex) {
                artifact_dir = VC.sharedBldDir.split("=")[1]
            }
            def coverDBpath = formatPath(artifact_dir + "/vcast_data/cover.db")
            def coverSfpDBpath = formatPath(artifact_dir + "/vcast_data/vcprj.db")

            results.cmds += """
                _RM ${coverDBpath}
                _RM ${coverSfpDBpath}
                _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}"  ${VC.useCI} --refresh"
            """
        }

        // if we are using SCM and not using a shared artifact directory...
        if (VC.usingSCM && !VC.oneChkDir && VC.sharedBldDir.length() == 0) {
            // run a script to extract script.stashed files and process data into xml reports
            def mpPath = getMPpath(VC.mpName)
            def coverDBpath = formatPath(mpPath + "/build/vcast_data/cover.db")
            def coverSfpDBpath = formatPath(mpPath + "/build/vcast_data/vcprj.db")
            cmds += """
                _RM ${coverDBpath}
                _RM ${coverSfpDBpath}
                _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/extract_build_dir.py  --leave_files
                _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}"  ${VC.useCI} --refresh"
            """
        }

        // run the metrics at the end
        cmds += """
            _VECTORCAST_DIR/vpython  "${script.env.WORKSPACE}"/vc_scripts/generate-results.py  ${VC.mpName} --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --junit ${extraOptStr} --buildlog unstashed_build.log"
            _VECTORCAST_DIR/vpython  "${script.env.WORKSPACE}"/vc_scripts/parallel_full_reports.py  ${VC.mpName} --jobs max
        """

        if (VC.useRGW3) {
            cmds += """
                _VECTORCAST_DIR/vpython  "${script.env.WORKSPACE}"/vc_scripts/patch_rgw_directory.py  ${VC.mpName}
                _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}"  ${VC.useCI} --clicast-args rgw export" 
            """
        }

        if (VC.useCoverPlgin) {
            cmds += "_VECTORCAST_DIR/vpython  \"${script.env.WORKSPACE}\"/vc_scripts/cobertura.py --extended ${VC.mpName}\n"
        }

        cmds += """
            _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/incremental_build_report_aggregator.py ${mpName} --rptfmt HTML --verbose
            _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/full_report_no_toc.py "${VC.mpName}"
            _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}"  ${VC.useCI} --create-report=aggregate   --output=${mpName}_aggregate_report.html"
        """

        if (VC.useImpRst) {
            if (VC.useLocImpRst) {
                cmds += """
                    _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/managewait.py --wait_time ${VC.waitTime} --wait_loops ${VC.waitLoops} --command_line "--project "${VC.mpName}"  ${VC.useCI} --export-result=${mpName}_results.vcr"
                    _VECTORCAST_DIR/vpython "${script.env.WORKSPACE}"/vc_scripts/merge_vcr.py --new ${mpName}_results.vcr --orig ${mpName}_results_orig.vcr
                """
            }
        }
        cmds = VC.execDsl.getRunCommands(VC, cmds)

        return cmds
    }
}
