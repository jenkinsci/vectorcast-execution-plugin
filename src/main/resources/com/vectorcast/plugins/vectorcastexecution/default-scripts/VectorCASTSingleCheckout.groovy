package com.vectorcast.plugins.vectorcastexecution

class VectorCASTSingleCheckoutImpl {

    def script

    VectorCASTSingleCheckoutImpl(script) { this.script = script }

    // ===============================================================
    //
    // Function : updateForSingleCheckout
    // Inputs   : VC
    // Action   : Updates for a single checkout
    // Returns  : None
    // Stage    : Update for Single Checkout
    //
    // ===============================================================
    def updateForSingleCheckout(VC) {
        // If we are using a single checkout directory option, do the checkout here
        // This stage also includes the implementation for the parameterized Jenkins job
        //    that includes a forced node name and an external repository
        // External repository is used when another job has already checked out the source code
        //    and is passing that information to this pipeline via VCAST_PROJECT_DIR env var
        def needsCheckout = false
        def usingExternalRepo = false
        def oneChkDir = (VC.oneChkDir as boolean)

        VC.setup = (VC.setup ?: "")
        VC.teardown = (VC.teardown ?: "")
        VC.sharedBldDir = (VC.sharedBldDir ?: "")
        VC.postSCMSteps = (VC.postSCMSteps ?: "")
        VC.mpName = (VC.mpName ?: "")

        // check to see if env var VCAST_PROJECT_DIR is setup from another job
        if (VC.vcastProjectDir) {
            usingExternalRepo = true
            def base = (VC.vcastProjectDir ?: "").replaceAll(/[\/\\]+$/, "")
            VC.mpName = base ? (base + "/" + VC.mpName) : VC.mpName
        }

        // If we are using a single checkout directory option and its not a
        //    SMC checkout from another job...
        if (oneChkDir && !usingExternalRepo) {

            // we need to convert all the future job's workspaces to point to the original checkout
            def originalWorkspace = "${script.env.WORKSPACE}"
            script.echo "scmStep executed here: " + originalWorkspace
            needsCheckout = true
            script.echo "Updating " + VC.mpName + " to: " + originalWorkspace + "/" + VC.mpName
            VC.mpName = originalWorkspace + "/" + VC.mpName

            def origSetup = VC.setup
            def origTeardown = VC.teardown
            def orig_sharedBldDir = VC.sharedBldDir
            def orig_postSCMSteps = VC.postSCMSteps

            if (script.isUnix()) {
                VC.setup = VC.setup.replace("\$WORKSPACE" ,originalWorkspace)
                VC.teardown = VC.teardown.replace("\$WORKSPACE" ,originalWorkspace)
                VC.sharedBldDir = VC.sharedBldDir.replace("\$WORKSPACE" ,originalWorkspace)
                VC.postSCMSteps = VC.postSCMSteps.replace("\$WORKSPACE" ,originalWorkspace)
            } else {
                originalWorkspace = originalWorkspace.replace('\\','/')

                def tmpInfo = ""

                // replace case insensitive workspace with WORKSPACE
                tmpInfo = VC.setup.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                VC.setup = tmpInfo.replace("%WORKSPACE%",originalWorkspace)

                // replace case insensitive workspace with WORKSPACE
                tmpInfo = VC.teardown.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                VC.teardown = tmpInfo.replace("%WORKSPACE%",originalWorkspace)

                // replace case insensitive workspace with WORKSPACE
                tmpInfo = VC.sharedBldDir.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                VC.sharedBldDir = tmpInfo.replace("%WORKSPACE%" ,originalWorkspace)

                // replace case insensitive workspace with WORKSPACE
                tmpInfo = VC.postSCMSteps.replaceAll("(?i)%WORKSPACE%","%WORKSPACE%")
                VC.postSCMSteps = tmpInfo.replace("%WORKSPACE%" ,originalWorkspace)
            }
            script.echo "Updating setup script " + origSetup + " \nto: " + VC.setup
            script.echo "Updating teardown script " + origTeardown + " \nto: " + origTeardown
            script.echo "Updating shared artifact directory " + orig_sharedBldDir + " \nto: " + VC.sharedBldDir
            script.echo "Updating post SCM steps "  + orig_postSCMSteps + "\nto: " + VC.postSCMSteps

        } else {
            if (usingExternalRepo) {
                script.echo "Using ${VC.forceNodeExecName}/${VC.mpName} as single checkout directory"
            }
            else {
                script.echo "Not using Single Checkout"
            }
        }
        return needsCheckout
    }
}
