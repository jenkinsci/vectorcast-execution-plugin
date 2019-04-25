#
# The MIT License
#
# Copyright 2016 Vector Software, East Greenwich, Rhode Island USA
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
import os
import sys
import argparse
import shutil
import re
import glob

# adding path
jenkinsScriptHome = os.getenv("WORKSPACE") + os.sep + "vc_scripts"
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)
python_path_updates += os.sep + "vpython-addons"
sys.path.append(python_path_updates)

import tcmr2csv
import vcastcsv2jenkins
from managewait import ManageWait

#global variables
global verbose
global wait_time
global wait_loops

verbose = False

VECTORCAST_DIR = os.getenv('VECTORCAST_DIR') + os.sep

import os

def runManageWithWait(command_line):
    global verbose
    global wait_time
    global wait_loops

    manageWait = ManageWait(verbose, command_line, wait_time, wait_loops)
    return manageWait.exec_manage()

# Determine if this version of VectorCAST supports new-style reporting/Data API
def checkUseNewReportsAndAPI():
    if "VCAST_REPORT_ENGINE" in os.environ and os.environ["VCAST_REPORT_ENGINE"] == "LEGACY":
        # Using legacy reporting with new reports - fall back to parsing html report
        if verbose:
            print "Reports forced to be old/legacy, so use them"
        return False
    # Look for existence of file that only exists in distribution with the new reports
    check_file = os.path.join(os.getenv("VECTORCAST_DIR"),
                             "python",
                             "vector",
                             "apps",
                             "ReportBuilder",
                             "reports",
                             "full_report.pyc")
    if os.path.isfile(check_file):
        if verbose:
            print "Using VectorCAST with new style reporting. Use Data API for Jenkins reports."
        return True
    else:
        if verbose:
            print "Using VectorCAST without new style reporting. Use VectorCAST reports for Jenkins reports."
        return False

# Read the Manage project file to determine its version
# File has already been checked for existence
def readManageVersion(ManageFile):
    version = 14
    if os.path.isfile(ManageFile + ".vcm"):
        ManageFile = ManageFile + '.vcm'
    with open(ManageFile, 'r') as projFile:
        for line in projFile:
            if 'version' in line and 'project' in line:
                version = int(re.findall(r'\d+', line)[0])
                break
    if verbose:
        print "Version of Manage project file = %d" % version
        print "(Levels change in version 17 (*maybe) and above)"
    return version

# Call manage to get the mapping of Envs to Directory etc.
def getManageEnvs(FullManageProjectName):
    manageEnvs = {}

    callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --build-directory-name"
    out_mgt = runManageWithWait(callStr)
    if verbose:
        print out_mgt
        
    for line in out_mgt.split('\n'):
        if "Compiler:" in line:
            compiler = line.split(":")[-1].strip()
        elif "Testsuite ID:" in line:
            pass
        elif "TestSuite:" in line:
            testsuite = line.split(":")[-1].strip()
        elif "Environment:" in line:
            env_name = line.split(":")[-1].strip()
        elif "Build Directory:" in line:
            build_dir = line.split(":")[-1].strip()
            #rare case where there's a problem with the environment
            if build_dir == "":
                continue
            build_dir_number = build_dir.split("/")[-1]
            level = compiler + "/" + testsuite + "/" + env_name.upper()
            entry = {}
            entry["env"] = env_name.upper()
            entry["compiler"] = compiler
            entry["testsuite"] = testsuite
            entry["build_dir"] = build_dir
            entry["build_dir_number"] = build_dir_number
            manageEnvs[level] = entry
            
            if verbose:
                print level
                print entry
                
        elif "Log Directory:" in line:
            pass
        elif "Control Status:" in line:
            pass

    return manageEnvs

def delete_file(filename):
    if os.path.exists(filename):
        os.remove(filename)

# build the Test Case Management Report for Manage Project
# envName and level only supplied when doing reports for a sub-project
# of a multi-job
def buildReports(FullManageProjectName = None, level = None, envName = None, genExeRpt = True):

    # make sure the project exists
    if not os.path.isfile(FullManageProjectName) and not os.path.isfile(FullManageProjectName + ".vcm"):
        raise IOError(FullManageProjectName + ' does not exist')
        return

    version = readManageVersion(FullManageProjectName)
    useNewReport = checkUseNewReportsAndAPI()
    manageEnvs = {}
    if useNewReport:
        manageEnvs = getManageEnvs(FullManageProjectName)

    # parse out the manage project name
    manageProjectName = os.path.splitext(os.path.basename(FullManageProjectName))[0]
    tcmr2csv.manageProjectName = manageProjectName

    print "Generating Test Case Management Reports"

    # release locks and create all Test Case Management Report
    callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --force --release-locks"
    out_mgt = runManageWithWait(callStr)

    if level and envName:
        callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --level " + level + " --environment " + envName + " --clicast-args report custom management"
    else:
        callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --clicast-args report custom management"
    print callStr

    # capture the output of the manage call
    out_mgt = runManageWithWait(callStr)

    missing = False
    if "database missing or inaccessible" in out_mgt:
        missing = True
    elif re.search('Environment directory.*is missing', out_mgt):
        missing = True
    if missing:
        callStr = callStr.replace("report custom","cover report")
        print callStr
        out_mgt2 = runManageWithWait(callStr)
        out_mgt = out_mgt +  "\n" + out_mgt2

    if genExeRpt:
        print "Generating Execution Reports"
        if level and envName:
            callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --level " + level + " --environment " + envName + " --clicast-args report custom actual"
        else:
            callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --clicast-args report custom actual"

        print callStr

        out_exe = runManageWithWait(callStr)
        out = out_mgt + "\n" + out_exe
    else:
        out = out_mgt

    if verbose:
        print out

    # save the output of the manage command for debug purposes
    outFile = open("build.log", "w")
    outFile.write(out)
    outFile.close()

    #vcastcsv2jenkins.run()

    copyList = []
    jobName = ""
    level = ""

    if not os.path.exists("management"):
        os.mkdir("management")

    if not os.path.exists("execution"):
        os.mkdir("execution")

    #loop over each line of the manage command output
    env = None
    
    if not os.path.exists("xml_data"):
        os.mkdir("xml_data")
    if verbose:
        print "Cleaning up old XML data files"
    for file in glob.glob("xml_data/*.xml"):
        try:
            os.remove(file);
            if verbose:
                print "Removing file: " + file
        except Exception as e:
            print "Error removing " + file
            print e

    for line in out.split('\n'):
        # the TEST_SUITE line will give us information for building a jobName that will be
        # inserted into the CSV name so it will match with the Jenkins integration job names
        # Generated jobName ONLY used for reports in a single job
        if "COMMAND:" in line:
            info = line.split("-e ")
            env = info[1].split(" ")[0]
        if "TEST SUITE" in line:
            info  = line.split(": ")
            level = info[1].split("/")
            if len(level) == 2:
                # Level does not include source and platform
                jobName = level[0] + "_" + level[1].rstrip()
            else:
                # Level includes source and platform
                jobName = level[2] + "_" + level[3].rstrip()
        if "DIRECTORY:" in line:
            directory = line.split(": ")[1].strip()

        # Get the HTML file name that was created
        if "HTML report was saved" in line:

            # strip out anything that isn't the html file name
            reportName = line.rstrip()[34:-2]
            
            if not os.path.isfile(reportName):
                reportName = os.path.join(directory,env,os.path.basename(reportName))

            # setup to save the execution report
            if 'execution_results_report' in reportName:
                print "Processing Execution Report: " + reportName

                if envName:
                    adjustedReportName = "execution" + os.sep + envName + "_" + jobName + ".html"
                else:
                    adjustedReportName = "execution" + os.sep + env + "_" + jobName + ".html"

            # setup to save the management report
            if 'management_report' in reportName:

                if useNewReport:
                    # Only import when available (i.e. new reports are available)
                    try:
                        from generate_xml import GenerateXml
                        

                        if envName:
                            jobNameDotted = '.'.join([level[0].strip(), level[1].strip(), envName])
                            index = "{}/{}/{}".format(level[0].strip(), level[1].strip(), envName)
                            jenkins_name = jobName + "_" + envName
                            jenkins_link = envName + "_" + jobName
                            old_xmlUnitReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "test_results_" + envName + "_" + jobName + "_.xml"
                            old_xmlCoverReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "coverage_results_" + envName + "_" + jobName + "_.xml"
                            xmlUnitReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "test_results_" + envName + "_" + jobName + ".xml"
                            xmlCoverReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "coverage_results_" + envName + "_" + jobName + ".xml"
                        else:
                            jobNameDotted = '.'.join([level[0].strip(), level[1].strip(), env])
                            index = "{}/{}/{}".format(level[0].strip(), level[1].strip(), env)
                            jenkins_name = jobName + "_" + env
                            jenkins_link = env + "_" + jobName
                            old_xmlUnitReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "test_results_" + env + "_" + jobName + "_.xml"
                            old_xmlCoverReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "coverage_results_" + env + "_" + jobName + "_.xml"
                            xmlUnitReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "test_results_" + env + "_" + jobName + ".xml"
                            xmlCoverReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "coverage_results_" + env + "_" + jobName + ".xml"

                        # Remove any existing (old and newer) XML files
                        delete_file(old_xmlUnitReportName)
                        delete_file(old_xmlCoverReportName)
                        delete_file(xmlUnitReportName)
                        delete_file(xmlCoverReportName)

                        xml_file = GenerateXml(manageEnvs[index]["build_dir"],
                                               manageEnvs[index]["env"],
                                               xmlCoverReportName,
                                               jenkins_name,
                                               xmlUnitReportName,
                                               jenkins_link,
                                               jobNameDotted)

                        if verbose:
                            print "  Generate Jenkins xUnit report: {}".format(xmlUnitReportName)
                        xml_file.generate_unit()

                        if verbose:
                            print "  Generate Jenkins coverage report: {}".format(xmlCoverReportName)
                        xml_file.generate_cover()

                    except RuntimeError as r_error:
                        print "ERROR: failed to generate XML reports using vpython and the Data API"
                        print r_error

                else:
                    print "Processing Test Case Management Report: " + reportName
                    # Create the test_results_ and coverage_results_ csv files
                    testResultName, coverageResultsName = tcmr2csv.run(reportName, level, version)

                    vcastcsv2jenkins.run(test = testResultName,coverage = coverageResultsName,cleanup=True,useExecRpt=genExeRpt, version=version)

                if envName:
                    adjustedReportName = "management" + os.sep + envName + "_" + jobName + ".html"
                else:
                    adjustedReportName = "management" + os.sep + env + "_" + jobName + ".html"

            # Create a list for later to copy the files over
            copyList.append([reportName,adjustedReportName])
            # Reset env
            env = None

    for file in copyList:

        if verbose:
            print "moving %s -> %s" % (file[0], file[1])

        shutil.move(file[0], file[1])


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject', help='Manager Project Name')
    parser.add_argument('-v', '--verbose',   help='Enable verbose output', action="store_true")
    parser.add_argument('-l', '--level',   help='Environment Name if only doing single environment.  Should be in the form of level/env')
    parser.add_argument('-e', '--environment',   help='Environment Name if only doing single environment.  Should be in the form of level/env')
    parser.add_argument('-g', '--dont-gen-exec-rpt',   help='Don\'t Generated Individual Execution Reports',  action="store_true")
    parser.add_argument('--wait_time',   help='Time (in seconds) to wait between execution attempts', type=int, default=30)
    parser.add_argument('--wait_loops',   help='Number of times to retry execution', type=int, default=1)
    parser.add_argument('--api',   help='Unused', type=int)

    args = parser.parse_args()

    tcmr2csv.useLocalCsv = True

    if args.verbose:
        verbose = True
    wait_time = args.wait_time
    wait_loops = args.wait_loops

    if args.dont_gen_exec_rpt:
        gen_exec_rpt = False
    else:
        gen_exec_rpt = True

    os.environ['VCAST_RPTS_PRETTY_PRINT_HTML'] = 'FALSE'

    buildReports(args.ManageProject,args.level,args.environment,gen_exec_rpt)

