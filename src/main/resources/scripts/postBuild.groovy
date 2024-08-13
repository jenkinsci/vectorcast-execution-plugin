import hudson.FilePath

Boolean buildFailed = false
Boolean buildUnstable = false

if(manager.logContains(".*INFO: File System Error.*"))
{
    manager.createSummary("warning.gif").appendText("File System Error", false, false, false, "red")
    buildUnstable = true
    manager.addBadge("warning.gif", "File System Error")
}
if(manager.logContains(".*INFO: Problem parsing test results.*"))
{
    manager.createSummary("warning.gif").appendText("Test Results Parse Error", false, false, false, "red")
    buildUnstable = true
    manager.addBadge("warning.gif", "Test Results Parse Error")
}
if(manager.logContains(".*ERROR: Error accessing DataAPI for.*"))
{
    manager.createSummary("warning.gif").appendText("DataAPI Error", false, false, false, "red")
    buildUnstable = true
    manager.addBadge("warning.gif", "VectorCAST DataAPI Error")
}
else if(manager.logContains(".*py did not execute correctly.*") || manager.logContains(".*Traceback .most recent call last.*"))
{
    manager.createSummary("error.gif").appendText("Jenkins Integration Script Failure", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Jenkins Integration Script Failure")
}
if (manager.logContains(".*Failed to acquire lock on environment.*"))
{
    manager.createSummary("error.gif").appendText("Failed to acquire lock on environment", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Failed to acquire lock on environment")
}
if (manager.logContains(".*Environment Creation Failed.*"))
{
    manager.createSummary("error.gif").appendText("Environment Creation Failed", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Environment Creation Failed")
}
if (manager.logContains(".*Error with Test Case Management Report.*"))
{
    manager.createSummary("error.gif").appendText("Error with Test Case Management Report of at least one Environment", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Error with Test Case Management Report of at least one Environment")
}
if (manager.logContains(".*FLEXlm Error.*") || manager.logContains(".*ERROR: Failed to obtain a license.*"))
{
    manager.createSummary("error.gif").appendText("FLEXlm Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "FLEXlm Error")
}
if (manager.logContains(".*INCR_BUILD_FAILED.*"))
{
    manager.createSummary("error.gif").appendText("Build Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Build Error")
}
if (manager.logContains(".*Environment was not successfully built.*"))
{
    manager.createSummary("error.gif").appendText("Build Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Build Error")
}
if (manager.logContains(".*NOT_LINKED.*"))
{
    manager.createSummary("error.gif").appendText("Link Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Link Error")
}
if (manager.logContains(".*Preprocess Failed.*"))
{
    manager.createSummary("error.gif").appendText("Preprocess Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Preprocess Error")
}
if (manager.logContains(".*Value Line Error - Command Ignored.*") || manager.logContains(".*(E) @LINE:.*"))
{
    manager.createSummary("warning.gif").appendText("Test Case Import Error", false, false, false, "red")
    buildUnstable = true
    manager.addBadge("warning.gif", "Test Case Import Error")
}

if(manager.logContains(".*Abnormal Termination on Environment.*")) 
{
    manager.createSummary("error.gif").appendText("Abnormal Termination of at least one Environment", false, false, false, "red")
    buildFailed = true
    manager.addBadge("error.gif", "Abnormal Termination of at least one Environment")
}

FilePath fp_i = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_rebuild.@HTML_TEXT@_tmp')
FilePath fp_f = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_full_report.@HTML_TEXT@_tmp')
if (fp_i.exists() && fp_f.exists())
{
    manager.createSummary("monitor.png").appendText(fp_i.readToString() + "<br>" + fp_f.readToString(), false)
}
else if (fp_f.exists())
{
    manager.createSummary("monitor.png").appendText(fp_f.readToString(), false)
    manager.build.description = "Warning: Incremental Build Report not found.  Ignore if not building with Change Based Testing"
    manager.addBadge("warning.gif", "CBT Report Missing - Ignore if not testing with CBT")
}
else
{
    manager.createSummary("warning.gif").appendText("General Failure", false, false, false, "red")
    buildUnstable = true
    manager.build.description = "General Failure, Incremental Build Report or Full Report Not Present. Please see the console for more information"
    manager.addBadge("warning.gif", "General Error")
}

if (buildFailed && !buildUnstable)
{
    manager.buildFailure()
}
if (buildUnstable)
{
    manager.buildUnstable()
}

