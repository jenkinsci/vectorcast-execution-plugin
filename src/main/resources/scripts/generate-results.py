import subprocess
import os
import sys
import argparse
import shutil

# adding path
jenkinsScriptHome = os.getenv("WORKSPACE") + os.sep + "vc_scripts"
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)
python_path_updates += os.sep + "vpython-addons"
sys.path.append(python_path_updates)

import tcmr2csv
import vcastcsv2jenkins

#global variables
global verbose

verbose = False

VECTORCAST_DIR = os.getenv('VECTORCAST_DIR') + os.sep

from pprint import pprint
import glob
import time
import os

# build the Test Case Management Report for Manage Project
def buildReports(FullManageProjectName = None, level = None, envName = None, genExeRpt = True):

    # make sure the project exists
    if not os.path.isfile(FullManageProjectName) and not os.path.isfile(FullManageProjectName + ".vcm"):
        raise IOError(FullManageProjectName + ' does not exist')
        return

    # parse out the manage prject name
    manageProjectName = os.path.splitext(os.path.basename(FullManageProjectName))[0]
    tcmr2csv.manageProjectName = manageProjectName

    print "Generating Test Case Management Reports"

    # release locks and create all Test Case Management Report
    callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --release-locks"
    callList = callStr.split()
    p = subprocess.Popen(callList,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    p.wait()

    if level and envName:
        callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --level " + level + " --environment " + envName + " --clicast-args report custom management"
    else:
        callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --clicast-args report custom management"
    print callStr

    callList = callStr.split()

    # capture the output of the manage call
    p = subprocess.Popen(callList,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    out_mgt,err_mgt = p.communicate()

    if "database missing or inaccessible" in out_mgt:
        callStr = callStr.replace("report custom","cover report")
        print callStr
        callList = callStr.split()
        p = subprocess.Popen(callList,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        out_mgt2,err_mgt2 = p.communicate()
        out_mgt = out_mgt + out_mgt2
        err_mgt = err_mgt + err_mgt2

    if genExeRpt:
        print "Generating Execution Reports"
        if level and envName:
            callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --level " + level + " --environment " + envName + " --clicast-args report custom actual"
        else:
            callStr = VECTORCAST_DIR + "manage --project " + FullManageProjectName + " --clicast-args report custom actual"

        print callStr
        callList = callStr.split()

        p = subprocess.Popen(callList,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        out_exe,err_exe = p.communicate()
        out = out_mgt + out_exe
        err = err_mgt + err_exe
    else:
        out = out_mgt
        err = err_mgt

    if verbose:
        print out
        print err

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
            jobName = manageProjectName + "_" + level[2] + "_" + level[3].rstrip()

        # Get the HTML file name that was created
        if "HTML report was saved" in line:

            # strip out anything that isn't the html file name
            reportName = line.rstrip()[34:-2]
            basename = os.path.basename(reportName)

            # setup to save the execution report
            if 'execution_results_report' in reportName:
                adjustedReportName = "execution" + os.sep + jobName + "_" + basename

            if 'management_report' in reportName:

                #process the test case management report
                print "   Processing Test Case Management Report: " + reportName

                # Create the test_results_ and coverage_results_ csv files
                testResultName, coverageResultsName = tcmr2csv.run(reportName, level)

                vcastcsv2jenkins.run(test = testResultName,coverage = coverageResultsName,cleanup=True,useExecRpt=genExeRpt)

                adjustedReportName = "management" + os.sep + jobName + "_" + basename

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
    parser.add_argument('--api', type=int)

    args = parser.parse_args()

    if args.api != 2:
        print "**********************************************************************"
        print "* Error - unsupported API version. This script expects API version 2 *"
        print "**********************************************************************"
        sys.exit(-1)

    tcmr2csv.useLocalCsv = True

    if args.verbose:
        verbose = True
    verbose = True

    if args.dont_gen_exec_rpt:
        gen_exec_rpt = False
    else:
        gen_exec_rpt = True

    os.environ['VCAST_RPTS_PRETTY_PRINT_HTML'] = 'FALSE'

    buildReports(args.ManageProject,args.level,args.environment,gen_exec_rpt)

