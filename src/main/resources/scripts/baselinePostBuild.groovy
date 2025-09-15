import hudson.FilePath
import java.nio.charset.*
import java.nio.ByteBuffer
import java.nio.CharBuffer

import java.io.StringWriter
import java.io.PrintWriter
import hudson.model.Result

out = manager.listener.logger  // same as console log
out.println "Post Build Groovy: Starting"


// Returns only the decoded text; logs the charset used.
def readWithFallback(FilePath fp) {
    byte[] bytes
    fp.read().withStream { is -> bytes = is.readAllBytes() }

    // Quick BOM hint
    String bomHint = null
    if (bytes.length >= 3 && bytes[0]==(byte)0xEF && bytes[1]==(byte)0xBB && bytes[2]==(byte)0xBF) bomHint = 'UTF-8'
    else if (bytes.length >= 2 && bytes[0]==(byte)0xFF && bytes[1]==(byte)0xFE) bomHint = 'UTF-16LE'
    else if (bytes.length >= 2 && bytes[0]==(byte)0xFE && bytes[1]==(byte)0xFF) bomHint = 'UTF-16BE'

    List<String> charsets = [
        'UTF-8', 'UTF-16LE', 'UTF-16BE',
        'GB18030', 'GBK',
        'windows-31j', 'Shift_JIS', 'EUC-JP', 'ISO-2022-JP',
        'MS949', 'x-windows-949', 'EUC-KR', 'ISO-2022-KR',
        'windows-1252', 'ISO-8859-1'
    ].unique()

    if (bomHint && charsets.contains(bomHint)) {
        charsets = [bomHint] + (charsets - bomHint)
    }

    for (String name : charsets) {
        try {
            Charset cs = Charset.forName(name)
            CharsetDecoder dec = cs.newDecoder()
                                   .onMalformedInput(CodingErrorAction.REPORT)
                                   .onUnmappableCharacter(CodingErrorAction.REPORT)
            CharBuffer cb = dec.decode(ByteBuffer.wrap(bytes))
            out.println "Post Build Groovy: Decoded ${fp.getName()} with charset: ${name}"
            return cb.toString()
        } catch (CharacterCodingException ignore) {
            // try next
        } catch (Exception ignore) {
            // try next
        }
    }

    // Fallback: ISO-8859-1
    out.println "Post Build Groovy: Fall Back Decode ${fp.getName()} with charset: ISO-8859-1 (fallback)"
    return new String(bytes, Charset.forName('ISO-8859-1'))
}

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
if (manager.logContains(".*Environment Creation Failed.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Environment Creation Failed", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Environment Creation Failed")
}
if (manager.logContains(".*newer version of VectorCAST*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Error with conflicting versions of VectorCAST and VectorCAST Project", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Error with conflicting versions of VectorCAST and VectorCAST Project")
}
if (manager.logContains(".*FLEXlm Error.*") || manager.logContains(".*ERROR: Failed to obtain a license.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("FLEXlm Error", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "FLEXlm Error")
}
if (manager.logContains(".*Unable to obtain license.*")) {
    manager.createSummary("icon-error icon-xlg").appendText("Error: Unable to obtain license", false, false, false, "red")
    buildFailed = true
    manager.addBadge("icon-error icon-xlg", "Error: Unable to obtain license")
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

try {
    def summaryStr = ""

    FilePath fp_cd = new FilePath(manager.build.getWorkspace(),'coverage_diffs.html_tmp')
    FilePath fp_i  = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_rebuild.html_tmp')
    FilePath fp_f  = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_full_report.html_tmp')
    FilePath fp_m  = new FilePath(manager.build.getWorkspace(),'@PROJECT_BASE@_metrics_report.html_tmp')

    if (fp_cd.exists()) {
        summaryStr  = "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
        summaryStr += readWithFallback(fp_cd)

    }

    if (fp_i.exists()) {
        summaryStr += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
        summaryStr += readWithFallback(fp_i)
    }

    if (fp_f.exists()) {
        summaryStr += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
        summaryStr += readWithFallback(fp_f)
    } 

    if (fp_m.exists())
    {
        summaryStr += "<hr style=\"height:5px;border-width:0;color:gray;background-color:gray\"> "
        summaryStr += readWithFallback(fp_m)
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
    
} catch (Throwable t) {
    out.println "Post Build Groovy: Failed reading the reports"

    def sw = new StringWriter()
    t.printStackTrace(new PrintWriter(sw))

    out.println "Post Build Groovy: ERROR: ${t.class.name}: ${t.message}"
    out.println sw.toString()    // full stack with file/line numbers when available

    // Mark build as failed (or UNSTABLE if you prefer)
    manager.build.setResult(Result.FAILURE)

    // Re-throw if you want the Post Build Groovy: step itself to error out:
    throw t
}
out.println "Post Build Groovy: Complete - No errors"
