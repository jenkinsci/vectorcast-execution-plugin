import hudson.FilePath

Boolean buildFailed = false
Boolean buildUnstable = false

if(manager.logContains(".*INFO: File System Error.*")) {
    manager.createSummary("icon-warning icon-xlg").appendText("File System Error", false, false, false, "red")
    buildUnstable = true
    manager.addBadge("icon-warning icon-xlg", "File System Error")
}
if(manager.logContains(".*INFO: Problem parsing test results.*")) {
    manager.createSummary("icon-warning icon-xlg").appendText("Test Results Parse Error", false, false, false, "red")
    buildUnstable = true
    manager.addBadge("icon-warning icon-xlg", "Test Results Parse Error")
}
if(manager.logContains(".*ERROR: Error accessing DataAPI for.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("DataAPI Error", false, false, false, "red")
    buildUnstable = true
    manager.addBadge("icon-warning icon-xlg", "VectorCAST DataAPI Error")
}
else if(manager.logContains(".*py did not execute correctly.*") || manager.logContains(".*Traceback .most recent call last.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Jenkins Integration Script Failure", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Jenkins Integration Script Failure")
}
if (manager.logContains(".*Failed to acquire lock on environment.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Failed to acquire lock on environment", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Failed to acquire lock on environment")
}
if (manager.logContains(".*Environment Creation Failed.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Environment Creation Failed", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Environment Creation Failed")
}
if (manager.logContains(".*Error with Test Case Management Report.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Error with Test Case Management Report of at least one Environment", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Error with Test Case Management Report of at least one Environment")
}
if (manager.logContains(".*FLEXlm Error.*") || manager.logContains(".*ERROR: Failed to obtain a license.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("FLEXlm Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "FLEXlm Error")
}
if (manager.logContains(".*INCR_BUILD_FAILED.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Build Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Build Error")
}
if (manager.logContains(".*Environment was not successfully built.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Build Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Build Error")
}
if (manager.logContains(".*NOT_LINKED.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Link Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Link Error")
}
if (manager.logContains(".*Preprocess Failed.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Preprocess Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Preprocess Error")
}
if (manager.logContains(".*Value Line Error - Command Ignored.*") || manager.logContains(".*(E) @LINE:.*")) {
    manager.createSummary("icon-warning icon-xlg").appendText("Test Case Import Error", false, false, false, "red")
    buildUnstable = true
    manager.addBadge("icon-warning icon-xlg", "Test Case Import Error")
}
if(manager.logContains(".*Abnormal Termination on Environment.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Abnormal Termination of at least one Environment", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Abnormal Termination of at least one Environment")
}

def debugInfo = ""
def summaryStr = ""

FilePath fp_cd = new FilePath(manager.build.getWorkspace(),'coverage_diffs.html_tmp')
FilePath fp_i  = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_rebuild.html_tmp')
FilePath fp_f  = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_full_report.html_tmp')
FilePath fp_m  = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_metrics_report.html_tmp')

if (fp_cd.exists()) {
    summaryStr  = "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
    summaryStr += fp_cd.readToString() 
}

if (fp_i.exists()) {
    summaryStr += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
    summaryStr += fp_i.readToString() 
}

if (fp_f.exists()) {
    summaryStr += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
    summaryStr += fp_f.readToString() 
} 

if (fp_m.exists())
{
    summaryStr += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
    summaryStr += fp_m.readToString() 

}

manager.createSummary("icon-orange-square icon-xlg").appendText(summaryStr, false)

if (!fp_f.exists() && !fp_m.exists())
{
    manager.createSummary("icon-error icon-xlg").appendText("General Failure", false, false, false, "red")
    manager.build.description = "General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information"
    manager.addBadge("icon-error icon-xlg", "General Error")

    if (!buildFailed) {
        buildUnstable = true
    }
}

if (buildFailed)
{
    manager.buildFailure()
}
if (buildUnstable)
{
    manager.buildUnstable()
}

