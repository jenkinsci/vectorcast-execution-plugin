#
# The MIT License
#
# Copyright 2025 Vector Informatik, GmbH.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#

from __future__ import print_function
import datetime
try:
    from html import escape
except ImportError:
    # html not standard module in Python 2.
    from cgi import escape
import sys, subprocess, os           
from vcast_utils import dump, getVectorCASTEncoding

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
                     
    data = "<?xml version=\"1.0\" encoding=\"{}\"?>\n".format(encoding)
    data += "<testsuites>\n  <!-- {} -->\n".format(unit_report_name)
    data += "  <testsuite errors=\"{}\" tests=\"{}\" failures=\"{}\" name=\"{}\" id=\"1\">\n".format(0, total, failed, currentEnv)  

    junitfile.write(data.encode(encoding, "replace"))

def writeJunitData(junitfile,all_tc_data, encoding):
    junitfile.write(all_tc_data.encode(encoding, "replace"))
    
def writeJunitFooter(junitfile, encoding):
    junitfile.write("  </testsuite>\n".encode(encoding, "replace"))
    junitfile.write("</testsuites>\n".encode(encoding, "replace"))

def write_tc_data(currentEnv, unit_report_name, jobNameDotted, passed, failed, error, testcase_data, encoding = 'utf-8', xml_data_dir = "xml_data"):

    with open(os.path.join(xml_data_dir,unit_report_name), "wb") as fh:
        encoding = getVectorCASTEncoding()
        writeJunitHeader(currentEnv, fh, failed, failed+passed, unit_report_name, encoding)
        writeJunitData(fh, testcase_data, encoding)
        writeJunitFooter(fh, encoding)         
        
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
        
def genQATestResults(mp, level = None, envName = None, verbose = False, encoding = 'utf-8'):
    passed_count = 0
    failed_count = 0

    # try:
        # from vector.apps.DataAPI.manage_models import SystemTest
    # except:
        # if verbose:
            # print("No QA Environment that can be processed using --system-tests-status")
        # return passed_count, failed_count

    if level and envName:
        nameLevel = level + "_" + envName
        nameLevel = nameLevel.replace("\\","/").replace("/","_")
        report_name = "{}_{}_system_tests_status.html".format(os.path.basename(mp)[:-4], nameLevel)
    else:
        report_name = os.path.basename(mp)[:-4] + "_system_tests_status.html"

    if os.path.exists(report_name):
        with open(report_name,"rb") as fd:
            raw = fd.read()
            out = raw.decode(encoding, 'replace')
                            
        passed_count, failed_count = processSystemTestResultsData(out.splitlines(), encoding)
        
    return passed_count, failed_count
        
if __name__ == '__main__':
    genQATestResults(sys.argv[1])

