from __future__ import print_function
import datetime
try:
    from html import escape
except ImportError:
    # html not standard module in Python 2.
    from cgi import escape
import sys, subprocess, os
global saved_compiler, saved_testsuite, saved_envname

saved_compiler = ""
saved_testsuite = ""
saved_envname = ""

def get_timestamp():
    dt = datetime.datetime.now()
    hour = dt.hour
    if hour > 12:
        hour -= 12
    return dt.strftime('%d %b %Y  @HR@:%M:%S %p').upper().replace('@HR@', str(hour))

def writeJunitHeader(currentEnv, junitfile, failed, total, unit_report_name, encoding = 'UTF-8'):
    
    junitfile.write("<?xml version=\"1.0\" encoding=\"" + encoding.upper() + "\"?>\n")

    junitfile.write("<testsuites>\n  <!--" + unit_report_name + "-->\n")
                    
    junitfile.write("  <testsuite errors=\"%d\" tests=\"%d\" failures=\"%d\" name=\"%s\" id=\"1\">\n" % 
        (0, total, failed, currentEnv))

def writeJunitData(junitfile,all_tc_data):
    junitfile.write(all_tc_data)
    
def writeJunitFooter(junitfile):
    junitfile.write("  </testsuite>\n")
    junitfile.write("</testsuites>\n")

def write_tc_data(currentEnv, unit_report_name, jobNameDotted, passed, failed, error, testcase_data, encoding = 'utf-8', xml_data_dir = "xml_data"):

    fh = open(os.path.join(xml_data_dir,"junit",unit_report_name), "w")

    writeJunitHeader(currentEnv, fh, failed, failed+passed, unit_report_name, encoding)
    writeJunitData(fh, testcase_data)
    writeJunitFooter(fh)
    fh.close()

def generateJunitTestCase(jobname, tc_name, passFail):
    testCasePassString ="    <testcase name=\"%s\" classname=\"%s\" time=\"0\"/>\n"
    testCaseFailString ="""    <testcase name="%s" classname="%s" time="0">
      <failure type="failure" message="FAIL: %s"/>
    </testcase>
    """
    
    jobname = escape(jobname, quote=False)
    tc_name = escape(tc_name, quote=False)
    
    if 'PASS' in passFail:
        successFailure = 'success'
    else:
        successFailure = 'failure'    

    if 'ABNORMAL' in passFail:
        print("Abnormal Termination on Environment\n")

    unit_subp = jobname
   
    if 'PASS' in passFail:
        tc_data = (testCasePassString % (tc_name, unit_subp)) 
    else:   
        tc_data = (testCaseFailString % (tc_name, unit_subp, passFail))
        
    return tc_data
        
def getTestCaseData(line):

    data = line.split()
    
    if data[-1] == "-":
        percent = "N/A"
        ratio = "N/A"
        yesNo = data[-1]
        auto_man = data[-2]
        testcase_name = " ".join(data[:-3])
    else:
        percent = data[-1]
        ratio = data[-2]
        yesNo = data[-3]
        auto_man = data[-4]
        testcase_name = " ".join(data[:-4])
    
    return testcase_name, ratio, percent
    
def processDataLine(line):
    global saved_compiler, saved_testsuite, saved_envname
    line = line.rstrip()
    if line[2] != " ":
        compiler, x, testsuite, x, envName = line.split()[0:5]
        # print "--level " +compiler+"/"+testsuite+" --environment "+envName
        # get test name
        testcase_name, ratio, percent = getTestCaseData(" ".join(line.split()[6:]))
        
        saved_compiler  = compiler
        saved_testsuite = testsuite 
        saved_envname   = envName

    else:        
        testcase_name, ratio, percent = getTestCaseData(line)
        
    compiler = saved_compiler
    testsuite = saved_testsuite
    envName = saved_envname
        
    if percent != "N/A" and percent != "(100%)":
        pass_fail = False
    else:
        pass_fail = True
        
        
    return compiler, testsuite, envName, testcase_name, ratio, percent, pass_fail

def processSystemTestResultsData(lines, encoding = 'utf-8'):
    foundData = False
    oneMore = False
    oldEnvName = ""
    firstEnvFound = False
    
    testcase_data = ""
    passFail = ""
    passed = 0
    failed = 0
    error = 0

    ## get the summary
    for line in lines:
        if line.strip() == "":
            continue
        if not foundData:
            if not oneMore:
                if "Expecteds" in line:
                    oneMore = True
            else:
                foundData = True
                oneMore = False
                
        else:
            compiler, testsuite, envName, testcase_name, tc_ratio, tc_percent, tc_passed = processDataLine(line)
            
            # new files
            if oldEnvName != envName:
                if firstEnvFound:
                    write_tc_data(oldEnvName, unit_report_name, jobNameDotted, passed, failed, error, testcase_data, encoding)
                    
                    # reset summary
                    testcase_data = ""
                    passFail = ""
                    passed        = 0
                    failed        = 0
                    error         = 0
                else:
                    firstEnvFound = True;
                oldEnvName = envName
                    
                compiler = compiler.replace(".","").replace(" ", "")
                
                unit_report_name = "_".join(["test_results",compiler, testsuite, envName]) + ".xml"
                jobNameDotted = ".".join([compiler, testsuite, envName])
                
            if tc_percent != "N/A":
                if tc_percent == "(100%)":
                    passFail = "PASS"
                    passed += 1
                else:
                    passFail = tc_ratio + " " + tc_percent 
                    failed += 1
            else:
                passFail = "PASS"
                passed += 1
                
            testcase_data += generateJunitTestCase(jobNameDotted, testcase_name, passFail)
                   
    if firstEnvFound:
        write_tc_data(oldEnvName, unit_report_name, jobNameDotted, passed, failed, error, testcase_data, encoding)
        
    return passed, failed
        
def saveQATestStatus(mp):
    callStr = os.environ.get('VECTORCAST_DIR') + os.sep + "manage -p " + mp + " --system-tests-status=" + os.path.basename(mp)[:-4] + "_system_tests_status.html"
    p = subprocess.Popen(callStr, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
    out, err = p.communicate()

def genQATestResults(mp, level = None, envName = None, verbose = False, encoding = 'utf-8'):
    try:
        from vector.apps.DataAPI.manage_models import SystemTest
        if verbose:
            print("No need to process system test results using --system-tests-status")
        return
    except:
        pass

    print("   Processing QA test results for " + mp)
    callStr = os.environ.get('VECTORCAST_DIR') + os.sep + "manage -p " + mp + " --system-tests-status"
    if level:
        callStr += " --level " + level
        if envName:
            callStr += " -e " + envName
        
    p = subprocess.Popen(callStr, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
    out, err = p.communicate()
        
    if err:
        print(out, err)
    passed_count, failed_count = processSystemTestResultsData(out.splitlines(), encoding)
    
    saveQATestStatus(mp)
    
    return passed_count, failed_count
        
if __name__ == '__main__':
    genQATestResults(sys.argv[1])

