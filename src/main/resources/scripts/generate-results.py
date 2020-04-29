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

from __future__ import print_function
from __future__ import absolute_import

import os
import sys
import argparse
import shutil
import re
import glob
import subprocess
import time
import traceback

# adding path
jenkinsScriptHome = os.getenv("WORKSPACE") + os.sep + "vc_scripts"
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)
# needed because vc18 vpython does not have bs4 package
if sys.version_info[0] < 3:
    python_path_updates += os.sep + 'vpython-addons'
    sys.path.append(python_path_updates)

import tcmr2csv
import vcastcsv2jenkins
from managewait import ManageWait
import generate_qa_results_xml
from parse_console_for_cbt import ParseConsoleForCBT

try:
    from vector.apps.ReportBuilder.custom_report import CustomReport
    try:
        from vector.apps.DataAPI.unit_test_api import UnitTestApi
    except:
        from vector.apps.DataAPI.api import Api as UnitTestApi
except:
    pass
    
#global variables
global verbose
global wait_time
global wait_loops

verbose = False

#
# Internal - jUnit works best with one overall file for all test results
#
def writeJunitFinalCombinedTestResults(manageProjectName):
    fname = manageProjectName
    totalFailed = 0
    
    f=open("xml_data/test_results_"+fname+"_combined_final.xml","w")
    f.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    f.write("<testsuites>\n")

    for testResults in glob.glob('xml_data/test_results*.xml'):
        if testResults.endswith("_combined_final.xml"):
            continue
        if not testResults.endswith("_combined.xml"):
            os.remove(testResults)
            continue
            
        data = open(testResults,"r").readlines()[2:-1]
        for line in data:
            if "<testsuite errors=" in line:
                totalFailed += int(line.split("\"")[5])
        os.remove(testResults)
        wrData = "".join(data)
        f.write(wrData)
     
    f.write("\n</testsuites>\n")
    f.close()
    
    open("unit_test_fail_count.txt","w").write(str(totalFailed))

#
# Internal - jUnit works best with one overall file for all test results
#
def writeJunitCombinedTestResults(manageProjectName,orig_level,envName):
    fname = manageProjectName
    if orig_level:
        level = orig_level.split("/")
        if len(level) == 2:
            # Level does not include source and platform
            jobName = level[0] + "_" + level[1].rstrip()
        else:
            # Level includes source and platform
            jobName = level[2] + "_" + level[3].rstrip()
        fname = manageProjectName + "_" + jobName + "_" + envName
        
    f=open("xml_data/test_results_"+fname+"_combined.xml","w")
    f.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    f.write("<testsuites>\n")

    for testResults in glob.glob('xml_data/test_results*.xml'):
        if  "combined" in testResults:
            continue
        data = open(testResults,"r").readlines()[2:-1]
        os.remove(testResults)
        wrData =  "".join(data)
        f.write(wrData)
     
    f.write("\n</testsuites>\n")
    f.close()

def runManageWithWait(command_line, silent=False):
    global verbose
    global wait_time
    global wait_loops

    manageWait = ManageWait(verbose, command_line, wait_time, wait_loops)
    return manageWait.exec_manage(silent)

# Determine if this version of VectorCAST supports new-style reporting/Data API
def checkUseNewReportsAndAPI():
    if os.environ.get("VCAST_REPORT_ENGINE", "") == "LEGACY":
        # Using legacy reporting with new reports - fall back to parsing html report
        if verbose:
            print("VectorCAST/Execution ignoring LEGACY VCAST_REPORT_ENGINE.")

    # Look for existence of file that only exists in distribution with the new reports
    check_file = os.path.join(os.environ.get('VECTORCAST_DIR'),
                             "python",
                             "vector",
                             "apps",
                             "ReportBuilder",
                             "reports",
                             "full_report.pyc")
    if os.path.isfile(check_file):
        if verbose:
            print("Using VectorCAST with new style reporting. Use Data API for Jenkins reports.")
        return True
    else:
        if verbose:
            print("Using VectorCAST without new style reporting. Use VectorCAST reports for Jenkins reports.")
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
        print("Version of Manage project file = %d" % version)
        print("(Levels change in version 17 (*maybe) and above)")
    return version

# Call manage to get the mapping of Envs to Directory etc.
def getManageEnvs(FullManageProjectName):
    manageEnvs = {}

    cmd_prefix = os.environ.get('VECTORCAST_DIR') + os.sep
    callStr = cmd_prefix + "manage --project " + FullManageProjectName + " --build-directory-name"
    out_mgt = runManageWithWait(callStr, silent=True)
    if verbose:
        print(out_mgt)
        
    for line in out_mgt.split('\n'):
        if "Compiler:" in line:
            compiler = line.split(":",1)[-1].strip()
        elif "Testsuite ID:" in line:
            pass
        elif "TestSuite:" in line:
            testsuite = line.split(":",1)[-1].strip()
        elif "Environment:" in line:
            env_name = line.split(":",1)[-1].strip()
        elif "Build Directory:" in line:
            build_dir = line.split(":",1)[-1].strip()
            #rare case where there's a problem with the environment
            if build_dir == "":
                continue
            build_dir_number = build_dir.split("/")[-1]
            level = compiler + "/" + testsuite + "/" + env_name # env_name.upper()
            entry = {}
            entry["env"] = env_name #env_name.upper()
            entry["compiler"] = compiler
            entry["testsuite"] = testsuite
            entry["build_dir"] = build_dir
            entry["build_dir_number"] = build_dir_number
            manageEnvs[level] = entry
            
            if verbose:
                print(entry)
                
        elif "Log Directory:" in line:
            pass
        elif "Control Status:" in line:
            pass

    return manageEnvs

def delete_file(filename):
    if os.path.exists(filename):
        os.remove(filename)
        
def genDataApiReports(entry, jUnit, cbtDict):
    xml_file = ""
    
    try:
        from generate_xml import GenerateXml

        # Compiler/TestSuite
        env = entry["env"]
        level = entry["compiler"] + "_" + entry["testsuite"]
        
        jobNameDotted = '.'.join([entry["compiler"], entry["testsuite"], entry["env"]])
        jenkins_name = level + "_" + env
        jenkins_link = env + "_" + level
        xmlUnitReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "test_results_" + level + "_" + env + ".xml"
        xmlCoverReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "coverage_results_" + level + "_" + env + ".xml"

        xml_file = GenerateXml(entry["build_dir"],
                               entry["env"],entry["compiler"],entry["testsuite"],
                               xmlCoverReportName,
                               jenkins_name,
                               xmlUnitReportName,
                               jenkins_link,
                               jobNameDotted, 
                               verbose, 
                               jUnit,
                               cbtDict)
                               
        if xml_file.api != None:
            if verbose:
                print("  Generate Jenkins testcase report: {}".format(xmlUnitReportName))
            xml_file.generate_unit()

            if verbose:
                print("  Generate Jenkins coverage report: {}".format(xmlCoverReportName))
            xml_file.generate_cover()
        else:
            print("   Skipping environment: " + jobNameDotted)
            
    except Exception as e:
        print("ERROR: failed to generate XML reports using vpython and the Data API for ", entry["compiler"] + "_" + entry["testsuite"] + "_" + entry["env"], "in directory", entry["build_dir"])
        if verbose:
            traceback.print_exc()
        
    return xml_file
    
def fixup_css(report_name):
    # Needed for VC19 and VC19 SP1.
    # From VC19 SP2 onwards a new option VCAST_RPTS_SELF_CONTAINED is used instead
    data = open(report_name,"r").read()
 
    # When using new option, there will be an <img src="vector_log.png"/> in the
    # generated HTML. If present, no need to do anything else.
    if '"vector_logo.png"' in data:
        return

    #fix up inline CSS because of Content Security Policy violation
    newData = data[: data.index("<style>")-1] +  """
    <link rel="stylesheet" href="normalize.css">
    <link rel="stylesheet" href="default-style.css">
    """ + data[data.index("</style>")+8:]
    
    #fix up style directive because of Content Security Policy violation
    newData = newData.replace("<div class='event bs-callout' style=\"position: relative\">","<div class='event bs-callout relative'>")
    
    #fixup the inline VectorCAST image because of Content Security Policy violation
    regex_str = r"<img alt=\"Vector\".*"
    newData =  re.sub(regex_str,"<img alt=\"Vector\" src=\"vectorcast.png\"/>",newData)
    
    open(report_name,"w").write(newData)
    
    vc_scripts = os.getenv("WORKSPACE")+"/vc_scripts/"
    
    shutil.copy(vc_scripts+"normalize.css", "management/normalize.css")
    shutil.copy(vc_scripts+"default-style.css", "management/default-style.css")
    shutil.copy(vc_scripts+"vectorcast.png", "management/vectorcast.png")

def generateCoverReport(path, env, level ):

    from vector.apps.ReportBuilder.custom_report import CustomReport
    from vector.apps.DataAPI.cover_api import CoverApi

    api=CoverApi(path)

    report_name = "management/" + level + "_" + env + ".html"

    try:
        CustomReport.report_from_api(api, report_type="Demo", formats=["HTML"], output_file=report_name, sections=["CUSTOM_HEADER", "REPORT_TITLE", "TABLE_OF_CONTENTS", "CONFIG_DATA", "METRICS", "MCDC_TABLES",  "AGGREGATE_COVERAGE", "CUSTOM_FOOTER"])

        fixup_css(report_name)
    except Exception as e:
        print("   *Problem generating custom report for " + env + ": ")
        if verbose:
            traceback.print_exc()

def generateUTReport(path, env, level): 
    global verbose

    def _dummy(*args, **kwargs):
        return True

    api=UnitTestApi(path)
    report_name = "management/" + level + "_" + env + ".html"
    try:
        api.commit = _dummy
        api.report(report_type="FULL_REPORT", formats=["HTML"], output_file=report_name)
        fixup_css(report_name)
    except Exception as e:
        print("   *Problem generating custom report for " + env + ".")
        if verbose:
            traceback.print_exc()

def generateIndividualReports(entry, envName):
    global verbose

    env = entry["env"]
    build_dir = entry["build_dir"]
    level = entry["compiler"] + "_" + entry["testsuite"]
   
    if envName == None or envName == env:
        cov_path = os.path.join(build_dir,env + '.vcp')
        unit_path = os.path.join(build_dir,env + '.vce')

        if os.path.exists(cov_path):
            generateCoverReport(cov_path, env, level)

        elif os.path.exists(unit_path):
            generateUTReport(unit_path , env, level)                

def useNewAPI(manageEnvs, level, envName, jUnit, cbtDict):
        
    for currentEnv in manageEnvs:
        if envName == None:
            genDataApiReports(manageEnvs[currentEnv], jUnit, cbtDict)
            generateIndividualReports(manageEnvs[currentEnv], envName)
            
        elif manageEnvs[currentEnv]["env"].upper() == envName.upper(): 
            env_level = manageEnvs[currentEnv]["compiler"] + "/" + manageEnvs[currentEnv]["testsuite"]
            
            if env_level.upper() == level.upper():
                genDataApiReports(manageEnvs[currentEnv], jUnit, cbtDict)
                generateIndividualReports(manageEnvs[currentEnv], envName)


# build the Test Case Management Report for Manage Project
# envName and level only supplied when doing reports for a sub-project
# of a multi-job
def buildReports(FullManageProjectName = None, level = None, envName = None, generate_individual_reports = True, timing = False, jUnit = True, cbtDict = None):

    if timing:
        print("Start: " + str(time.time()))
        
    saved_level = level
    saved_envName = envName
    
    # make sure the project exists
    if not os.path.isfile(FullManageProjectName) and not os.path.isfile(FullManageProjectName + ".vcm"):
        raise IOError(FullManageProjectName + ' does not exist')
        return
        
    manageProjectName = os.path.splitext(os.path.basename(FullManageProjectName))[0]

    version = readManageVersion(FullManageProjectName)
    useNewReport = checkUseNewReportsAndAPI()
    manageEnvs = {}

    if timing:
        print("Version Check: " + str(time.time()))

    # cleaning up old builds
    for path in ["xml_data","management","execution"]:
        # if the path exists, try to delete it
        if os.path.isdir(path):
            try:
                shutil.rmtree(path)
            except:
                # if there was an error removing the directory...delete all the files
                print("Error removing directory: " + path)
                for file in glob.glob(path + "/*.*"):
                    try:
                        os.remove(file);
                    except:
                        print("Error removing file after failed to remove directory: " + path + "/" + file)
                pass
                
        # we should either have an empty directory or no directory
        if not os.path.isdir(path):
            try:
                os.mkdir(path)
            except:
                print("Error creating directory: " + path)
            
    for file in glob.glob("*.csv"):
        try:
            os.remove(file);
            if verbose:
                print("Removing file: " + file)
        except Exception as e:
            print("Error removing " + file)
            print(e)
    
    
    ### Using new data API - 2019 and beyond
    if timing:
        print("Cleanup: " + str(time.time()))
    if useNewReport:

        try:
            shutil.rmtree("execution") 
        except:
            pass
        manageEnvs = getManageEnvs(FullManageProjectName)
        if timing:
            print("Using DataAPI for reporting")
            print("Get Info: " + str(time.time()))
        useNewAPI(manageEnvs, level, envName, jUnit, cbtDict)
        if timing:
            print("XML and Individual reports: " + str(time.time()))

    ### NOT Using new data API        
    else:
    
        # parse out the manage project name
        tcmr2csv.manageProjectName = manageProjectName

        print("Generating Test Case Management Reports")

        cmd_prefix = os.environ.get('VECTORCAST_DIR') + os.sep

        # release locks and create all Test Case Management Report
        callStr = cmd_prefix + "manage --project " + FullManageProjectName + " --force --release-locks"
        out_mgt = runManageWithWait(callStr)

        if level and envName:
            callStr = cmd_prefix + "manage --project " + FullManageProjectName + " --level " + level + " --environment " + envName + " --clicast-args report custom management"
        else:
            callStr = cmd_prefix + "manage --project " + FullManageProjectName + " --clicast-args report custom management"
        print(callStr)

        # capture the output of the manage call
        out_mgt = runManageWithWait(callStr)

        missing = False
        if "database missing or inaccessible" in out_mgt:
            missing = True
        elif re.search('Environment directory.*is missing', out_mgt):
            missing = True
        if missing:
            callStr = callStr.replace("report custom","cover report")
            print(callStr)
            out_mgt2 = runManageWithWait(callStr)
            out_mgt = out_mgt +  "\n" + out_mgt2

        if generate_individual_reports:
            print("Generating Execution Reports")
            if level and envName:
                callStr = cmd_prefix + "manage --project " + FullManageProjectName + " --level " + level + " --environment " + envName + " --clicast-args report custom actual"
            else:
                callStr = cmd_prefix + "manage --project " + FullManageProjectName + " --clicast-args report custom actual"

            print(callStr)

            out_exe = runManageWithWait(callStr)
            out = out_mgt + "\n" + out_exe
        else:
            out = out_mgt

        if verbose:
            print(out)

        # save the output of the manage command for debug purposes
        outFile = open("build.log", "w")
        outFile.write(out)
        outFile.close()
        copyList = []
        jobName = ""
        level = ""

        if timing:
            print("Using report scraping for metrics")
            print("Individual report generation: " + str(time.time()))
        if not os.path.exists("management"):
            os.mkdir("management")

        if not os.path.exists("execution"):
            os.mkdir("execution")

        #loop over each line of the manage command output
        env = None
        
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
                    print("Processing Execution Report: " + reportName)

                    if envName:
                        adjustedReportName = "execution" + os.sep + envName + "_" + jobName + ".html"
                    else:
                        adjustedReportName = "execution" + os.sep + env + "_" + jobName + ".html"

                # setup to save the management report
                if 'management_report' in reportName:

                    print("Processing Test Case Management Report: " + reportName)
                    
                    # Create the test_results_ and coverage_results_ csv files
                    testResultName, coverageResultsName = tcmr2csv.run(reportName, level, version)

                    vcastcsv2jenkins.run  (test = testResultName, coverage = coverageResultsName, useExecRpt=generate_individual_reports, version=version, junit=junit)

                    if envName:
                        adjustedReportName = "management" + os.sep + jobName + "_" + envName + ".html"
                    else:
                        adjustedReportName = "management" + os.sep + jobName + "_" + env + ".html"

                # Create a list for later to copy the files over
                copyList.append([reportName,adjustedReportName])
                # Reset env
                env = None
                

        for file in copyList:

            if verbose:
                print("moving %s -> %s" % (file[0], file[1]))

            shutil.move(file[0], file[1])
            
    if timing:
        print("QA Results reports: " + str(time.time()))
            
    if junit:   
        if verbose:
            print("Writing combined test data for JUnit")
        generate_qa_results_xml.genQATestResults(FullManageProjectName, saved_level, saved_envName)
        writeJunitCombinedTestResults(manageProjectName,saved_level,saved_envName)

    if timing:
        print("Complete: " + str(time.time()))
        
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject', help='Manager Project Name')
    parser.add_argument('-v', '--verbose',   help='Enable verbose output', action="store_true")
    parser.add_argument('-l', '--level',   help='Environment Name if only doing single environment.  Should be in the form of level/env')
    parser.add_argument('-e', '--environment',   help='Environment Name if only doing single environment.  Should be in the form of level/env')
    parser.add_argument('-g', '--dont-generate-individual-reports',   help='Don\'t Generated Individual Reports (below 2019 - this just controls execution report generate, 2019 and later - no individual reports will be generated',  action="store_true")
    parser.add_argument('--wait_time',   help='Time (in seconds) to wait between execution attempts', type=int, default=30)
    parser.add_argument('--wait_loops',   help='Number of times to retry execution', type=int, default=1)
    parser.add_argument('--timing',   help='Display timing information for report generation', action="store_true")
    parser.add_argument('--junit',   help='Output test resutls in junit format', action="store_true")
    parser.add_argument('--api',   help='Unused', type=int)
    parser.add_argument('--final',   help='Write Final JUnit Test Results file',  action="store_true")
    parser.add_argument('--buildlog',   help='Build Log for CBT Statitics')

    args = parser.parse_args()
    
    if args.final:
        generate_qa_results_xml.genQATestResults(args.ManageProject)
        writeJunitFinalCombinedTestResults(os.path.basename(args.ManageProject))
        sys.exit(0)
    try:
        if "19.sp1" in open(os.path.join(os.environ['VECTORCAST_DIR'],"DATA/tools_version.txt").read):
            # custom report patch for SP1 problem - should be fixed in future release      
            old_init = CustomReport._post_init
            def new_init(self):
                old_init(self)
                self.context['report']['use_all_testcases'] = True
            CustomReport._post_init = new_init
    except:
        pass
    
    tcmr2csv.useLocalCsv = True

    if args.verbose:
        verbose = True
    wait_time = args.wait_time
    wait_loops = args.wait_loops

    if args.dont_generate_individual_reports:
        dont_generate_individual_reports = False
    else:
        dont_generate_individual_reports = True

    if args.timing:
        timing = True
    else:
        timing = False

    if args.junit:
        junit = True
    else:
        junit = False
        
    if args.buildlog and os.path.exists(args.buildlog):
        buildLogData = open(args.buildlog,"r").readlines()
        cbt = ParseConsoleForCBT()
        cbtDict = cbt.parse(buildLogData)
        
    else:
        cbtDict = None


    # Used for pre VC19
    os.environ['VCAST_RPTS_PRETTY_PRINT_HTML'] = 'FALSE'
    # Used for VC19 SP2 onwards
    os.environ['VCAST_RPTS_SELF_CONTAINED'] = 'FALSE'

    buildReports(args.ManageProject,args.level,args.environment,dont_generate_individual_reports, timing, junit, cbtDict)

