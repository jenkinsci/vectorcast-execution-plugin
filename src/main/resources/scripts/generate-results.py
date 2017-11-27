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
import subprocess
import os
import sys
import argparse
import shutil
import re

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
        print "(Levels change in version 17 and above)"
    return version

# build the Test Case Management Report for Manage Project
def buildReports(FullManageProjectName = None, level = None, envName = None, genExeRpt = True):

    # make sure the project exists
    if not os.path.isfile(FullManageProjectName) and not os.path.isfile(FullManageProjectName + ".vcm"):
        raise IOError(FullManageProjectName + ' does not exist')
        return

    version = readManageVersion(FullManageProjectName)

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

    if "database missing or inaccessible" in out_mgt:
        callStr = callStr.replace("report custom","cover report")
        print callStr
        out_mgt2 = runManageWithWait(callStr)
        out_mgt = out_mgt + out_mgt2

    if genExeRpt:
        print "Generating Execution Reports"
        if level and envName:
            callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --level " + level + " --environment " + envName + " --clicast-args report custom actual"
        else:
            callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --clicast-args report custom actual"

        print callStr

        out_exe = runManageWithWait(callStr)
        out = out_mgt + out_exe
    else:
        out = out_mgt

    if verbose:
        print out

    # save the output of the manage command for debug purposes
    outFile = open("build.log", "w")
    outFile.write(out)
    outFile.close()

    vcastcsv2jenkins.run()

    copyList = []
    jobName = ""
    level = ""

    if not os.path.exists("management"):
        os.mkdir("management")

    if not os.path.exists("execution"):
        os.mkdir("execution")

    #loop over each line of the manage command output
    for line in out.split('\n'):
        # the TEST_SUITE line will give us information for building a jobName that will be
        # inserted into the CSV name so it will match with the Jenkins integration job names
        if "TEST SUITE" in line:
            info  = line.split(": ")
            level = info[1].split("/")
            if version >= 17:
                # Level does not include source and platform
                jobName = level[0] + "_" + level[1].rstrip()
            else:
                # Level includes source and platform
                jobName = level[2] + "_" + level[3].rstrip()

        # Get the HTML file name that was created
        if "HTML report was saved" in line:

            # strip out anything that isn't the html file name
            reportName = line.rstrip()[34:-2]
#            basename = os.path.basename(reportName)

            # setup to save the execution report
            if 'execution_results_report' in reportName:
                print "   Processing Execution Report: " + reportName

                if envName:
                    adjustedReportName = "execution" + os.sep + envName + "_" + jobName + ".html"
                else:
                    adjustedReportName = "execution" + os.sep + jobName + ".html"

            # setup to save the management report
            if 'management_report' in reportName:
                print "   Processing Test Case Management Report: " + reportName

                # Create the test_results_ and coverage_results_ csv files
                testResultName, coverageResultsName = tcmr2csv.run(reportName, level, version)

                vcastcsv2jenkins.run(test = testResultName,coverage = coverageResultsName,cleanup=True,useExecRpt=genExeRpt, version=version)

                if envName:
                    adjustedReportName = "management" + os.sep + envName + "_" + jobName + ".html"
                else:
                    adjustedReportName = "management" + os.sep + jobName + ".html"

            # Create a list for later to copy the files over
            copyList.append([reportName,adjustedReportName])

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

