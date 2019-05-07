import datetime
import cgi
import sys, subprocess, os

global compiler, testsuite, envName  , fh

compiler = ""
testsuite = ""
envName = ""

# Versions of VectorCAST prior to 2019 relied on the environment variable VECTORCAST_DIR.
# We will use that variable as a fall back if the VectorCAST executables aren't on the system path.
has_exe = lambda p, x : os.access(os.path.join(p, x), os.X_OK)
has_vcast_exe = lambda p : has_exe(p, 'manage') or has_exe(p, 'manage.exe')
vcast_dirs = (path for path in os.environ["PATH"].split(os.pathsep) if has_vcast_exe(path))
vectorcast_install_dir = next(vcast_dirs, os.environ.get("VECTORCAST_DIR", ""))

def get_timestamp():
    dt = datetime.datetime.now()
    hour = dt.hour
    if hour > 12:
        hour -= 12
    return dt.strftime('%d %b %Y  @HR@:%M:%S %p').upper().replace('@HR@', str(hour))

def writeJunitHeader(junitfile, failed, total):
    
    global envName

    junitfile.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    junitfile.write("<testsuites>\n")
                    
    junitfile.write(" 	<testsuite errors=\"%d\" tests=\"%d\" failures=\"%d\" name=\"%s\" id=\"1\">\n" % 
        (0, total, failed, envName))

def writeJunitData(junitfile,all_tc_data):
    junitfile.write(all_tc_data)
    
def writeJunitFooter(junitfile):
    junitfile.write("   </testsuite>\n")
    junitfile.write("</testsuites>\n")

def write_tc_data(unit_report_name, jobNameDotted, passed, failed, error):
    fh = open("xml_data/" + unit_report_name, "w")

    writeJunitHeader(fh, failed, failed+passed)
    writeJunitData(fh, testcase_data)
    writeJunitFooter(fh)
    fh.close()

def generateJunitTestCase(unit, subp, tc_name, passFail):
    testCasePassString =" 		<testcase name=\"%s\" classname=\"%s\" time=\"0\"/>\n"
    testCaseFailString ="""
            <testcase name="%s" classname="%s" time="0">
                <failure type="failure" message="FAIL: %s"/>
            </testcase>
    """
    
    unit = cgi.escape(unit)
    subp = cgi.escape(subp)
    tc_name = cgi.escape(tc_name)
    
    if 'PASS' in passFail:
        successFailure = 'success'
    else:
        successFailure = 'failure'    

    if 'ABNORMAL' in passFail:
        print "Abnormal Termination on Environment\n"

    unit_subp = unit + "." + subp
   
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
    global compiler, testsuite, envName  
    line = line.rstrip()
    if line[2] != " ":
        compiler, x, testsuite, x, envName = line.split()[0:5]
        # print "--level " +compiler+"/"+testsuite+" --environment "+envName
        # get test name
        testcase_name, ratio, percent = getTestCaseData(" ".join(line.split()[6:]))
        
    else:
        testcase_name, ratio, percent = getTestCaseData(line)
        
    if percent != "N/A" and percent != "(100%)":
        pass_fail = False
    else:
        pass_fail = True
        
        
    return testcase_name, ratio, percent, pass_fail

def processSystemTestResultsData(lines):
    foundData = False
    oneMore = False
    oldEnvName = ""
    firstEnvFound = False
    
    testcase_data = ""
    passed = 0
    failed = 0
    error = 0

    ## get the summary
    for line in lines:
        if line == "":
            continue
        if not foundData:
            if not oneMore:
                if "Expecteds" in line:
                    oneMore = True
            else:
                foundData = True
                oneMore = False
                
        else:
            testcase_name, tc_ratio, tc_percent, tc_passed = processDataLine(line)
            
            # new files
            if oldEnvName != envName:
                if firstEnvFound:
                    write_tc_data(unit_report_name, jobNameDotted, passed, failed, error)
                    
                    # reset summary
                    testcase_data = ""
                    passed        = 0
                    failed        = 0
                    error         = 0
                else:
                    firstEnvFound = True;
                oldEnvName = envName
                    
                unit_report_name = "_".join(["test_results",envName,compiler, testsuite]) + ".xml"
                jobNameDotted = ".".join([compiler, testsuite, envName])
                jobNameSlashed = "/".join([compiler, testsuite, envName])
                
                
                if tc_percent != "N/A":
                    testcase_data += generateJunitTestCase(jobNameSlashed, testcase_name, envName, testcase_name, tc_passed, tc_ratio, tc_percent)
                    
                    if tc_ratio == "(100%)":
                        passed += 1
                    else:
                        failed += 1

            else:
                if tc_percent != "N/A":
                    testcase_data += generateJunitTestCase(jobNameSlashed, testcase_name, envName, testcase_name, tc_passed, tc_ratio, tc_percent)
                   
    if firstEnvFound:
        write_tc_data(unit_report_name, jobNameDotted, passed, failed, error)

def genQATestResults(mp):
    callStr = "manage -p " + mp + " --system-tests-status"
    p = subprocess.Popen(callStr, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = p.communicate()
    
    processSystemTestResultsData(out.split("\n"))
    
if __name__ == '__main__':
    genQATestResults(sys.argv[1])

