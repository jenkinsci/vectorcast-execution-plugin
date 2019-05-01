import datetime
import cgi
import sys, subprocess, os

global compiler, testsuite, envName  , fh

compiler = ""
testsuite = ""
envName = ""
fh = None
VECTORCAST_DIR = os.getenv('VECTORCAST_DIR') + os.sep

def get_timestamp():
    dt = datetime.datetime.now()
    hour = dt.hour
    if hour > 12:
        hour -= 12
    return dt.strftime('%d %b %Y  @HR@:%M:%S %p').upper().replace('@HR@', str(hour))
#
# Internal - start the xUnit XML file
#
def start_unit_file(unit_report_name, jobNameDotted, verbose = False):
    global fh
    if verbose:
        print "Writing unit xml file: {}".format(unit_report_name)
    fh = open("xml_data/" + unit_report_name, "w")
    fh.write('<testsuites xmlns="http://check.sourceforge.net/ns">\n')
    fh.write('    <datetime>%s</datetime>\n' % get_timestamp())
    fh.write('    <suite>\n')
    fh.write('        <title>%s</title>\n' % jobNameDotted)

    
#
# Internal - write a testcase to the xUnit XML file
#
def write_testcase(jobNameSlashed, tc_name, unit_name, func_name, tc_passed, tc_ratio, tc_percent):
    global fh
    unit_name = cgi.escape(unit_name)
    func_name = cgi.escape(func_name)
    tc_name = cgi.escape(tc_name)
    if tc_passed:
        fh.write('        <test result="success">\n')
    else:
        fh.write('        <test result="failure">\n')
    fh.write('            <fn>{}</fn>\n'.format(unit_name))
    fh.write('            <id>{}.{}</id>\n'.format(unit_name, tc_name))
    fh.write('            <iteration>1</iteration>\n')
    fh.write('            <description>System Test Case</description>\n')
    if tc_passed:
        status = "PASS"
    else:
        status = "FAIL"        

    msg = "{} : {} {} ".format(status, tc_ratio, tc_percent)
        
    fh.write('            <message>System Test: %s : %s</message>\n' % (jobNameSlashed, msg))
    fh.write('        </test>\n')

#
# Internal - write the end of the xUnit XML file and close it
#
def end_unit_file():
    fh.write('    </suite>\n')
    fh.write('    <duration>1</duration>\n\n')
    fh.write('</testsuites>\n')
    fh.close()

#

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
    

compiler = ""
testsuite = ""
envName = ""


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
            
            if oldEnvName != envName:
                if firstEnvFound:
                    end_unit_file()
                else:
                    firstEnvFound = True;
                oldEnvName = envName
                    
                unit_report_name = "_".join(["test_results",envName,compiler, testsuite]) + ".xml"
                jobNameDotted = ".".join([compiler, testsuite, envName])
                jobNameSlashed = "/".join([compiler, testsuite, envName])
                
                start_unit_file(unit_report_name,jobNameDotted)
                if tc_percent != "N/A":
                    write_testcase(jobNameSlashed, testcase_name, envName, testcase_name, tc_passed, tc_ratio, tc_percent)
            else:
                if tc_percent != "N/A":
                    write_testcase(jobNameSlashed, testcase_name, envName, testcase_name, tc_passed, tc_ratio, tc_percent)
                   
    if firstEnvFound:
        end_unit_file()

def genQATestResults(mp):
    callStr = VECTORCAST_DIR + "manage -p " + mp + " --system-tests-status"
    p = subprocess.Popen(callStr, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = p.communicate()
    
    processSystemTestResultsData(out.split("\n"))
    
if __name__ == '__main__':
    genQATestResults(sys.argv[1])

