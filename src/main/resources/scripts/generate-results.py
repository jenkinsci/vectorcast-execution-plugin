#
# The MIT License
#
# Copyright 2024 Vector Informatik, GmbH.
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
import parse_traceback
import tee_print

from safe_open import open

# adding path
workspace = os.getenv("WORKSPACE")
if workspace is None:
    workspace = os.getcwd()

jenkinsScriptHome = os.path.join(workspace,"vc_scripts")

python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)

if sys.version_info[0] < 3:
    python_path_updates += os.sep + 'vpython-addons'
    sys.path.append(python_path_updates)
    using_27_python = True
else:
    using_27_python = False

import tcmr2csv
import vcastcsv2jenkins
from managewait import ManageWait
import generate_qa_results_xml
from parse_console_for_cbt import ParseConsoleForCBT

using_new_reports = False
legacy = False

try:
    ## This tests to see if 2018 is present.
    from vector.apps.ReportBuilder.custom_report import CustomReport
    try:
        from vector.apps.DataAPI.unit_test_api import UnitTestApi
    except:
        from vector.apps.DataAPI.api import Api as UnitTestApi
    using_new_reports = True
except:
    pass
    

try:
    from vector.apps.DataAPI.vcproject_api import VCProjectApi
except:
    pass
    
#global variables
global verbose
global print_exc
global wait_time
global wait_loops

verbose = False
print_exc = False
need_fixup = False
wait_time = 30
wait_loops = 1

import getjobs


def skipReporting(build_dir, skipReportsForSkippedEnvs, cbtDict):

    import hashlib 

    ## use hash code instead of final directory name as regression scripts can have overlapping final directory names
    
    build_dir_4hash = build_dir.upper()
    build_dir_4hash = "/".join(build_dir_4hash.split("/")[-2:])
    
    # Unicode-objects must be encoded before hashing in Python 3
    if sys.version_info[0] >= 3:
        build_dir_4hash = build_dir_4hash.encode('utf-8')

    hashCode = hashlib.md5(build_dir_4hash).hexdigest()
    
    # skip report gen for skipped environments 
    if skipReportsForSkippedEnvs and cbtDict:
        if hashCode not in cbtDict.keys():
            if verbose:
                print("skipping report because hash not round in cbtdict")

            return True
        else:
            c,i,s = cbtDict[hashCode]
            if len(c)==0 and len(i)==0 and len(s)==0:
                if verbose:
                    print("skipping report because c,i,s are all 0 size")
                return True
    return False

enabledEnvironmentArray = []

def getEnabledEnvironments(MPname):
    output = getjobs.printEnvironmentInfo(MPname, False)

    for line in output.split("\n"):
        if line.strip():
            # type being system or unit test
            try:
                compiler, testsuite, environment = line.split()
            except:
                compiler, testsuite, environment, source, machine = line.split()
                
            enabledEnvironmentArray.append([compiler, testsuite, environment])
                       
def environmentEnabled(comp,ts,env):
    for c,t,e in enabledEnvironmentArray:
        if comp == c and ts == t and env == e:
            return True
    print(comp + "/" + ts + "/" + env + ": Disabled")
    return False 

def runManageWithWait(command_line, silent=False):
    global verbose
    global wait_time
    global wait_loops

    manageWait = ManageWait(verbose, command_line, wait_time, wait_loops)
    return manageWait.exec_manage(silent)

# Determine if this version of VectorCAST supports new-style reporting/Data API
def checkUseNewReportsAndAPI():
    if os.environ.get("VCAST_REPORT_ENGINE", "") == "LEGACY":
        # The execution plugin will ignore this value, but warn user.
        if verbose:
            print("VectorCAST/Execution ignoring LEGACY VCAST_REPORT_ENGINE.")

    if verbose:
        if using_new_reports:
            print("Using VectorCAST with new style reporting. Use Data API for Jenkins reports.")
        else:
            print("Using VectorCAST without new style reporting. Use VectorCAST reports for Jenkins reports.")

    return using_new_reports

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
        print("Version of VectorCAST project file = %d" % version)
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
            if not environmentEnabled(compiler,testsuite,env_name):
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
        
def genDataApiReports(FullManageProjectName, entry, cbtDict, generate_exec_rpt_each_testcase, use_archive_extract, report_only_failures, useStartLine, teePrint, use_cte):
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

        xml_file = GenerateXml(FullManageProjectName,
                               entry["build_dir"],
                               entry["env"],entry["compiler"],entry["testsuite"],
                               xmlCoverReportName,
                               jenkins_name,
                               xmlUnitReportName,
                               jenkins_link,
                               jobNameDotted, 
                               verbose, 
                               cbtDict,
                               generate_exec_rpt_each_testcase,
                               use_archive_extract,
                               report_only_failures,
                               print_exc,
                               useStartLine,
                               teePrint,
                               use_cte)
                               
        if xml_file.api != None:
            if verbose:
                print("   Generate Jenkins testcase report: {}".format(xmlUnitReportName))
            xml_file.generate_unit()

            if verbose:
                print("   Generate Jenkins coverage report: {}".format(xmlCoverReportName))
            xml_file.generate_cover()
        else:
            print("   Skipping environment: " + jobNameDotted)
            print("\n\n")
            print ("******************************************************")
            print ("** Environment's that only use imported results     **")
            print ("** will not properly generate metrics with this     **")
            print ("** version of VectorCAST.                           **")
            print ("******************************************************")
            print("\n\n")

    
    except Exception as e:
        parse_traceback.parse(traceback.format_exc(), print_exc, entry["compiler"] , entry["testsuite"],  entry["env"], entry["build_dir"])

    try:       
        return xml_file.passed_count, xml_file.failed_count
    except:
        return 0, 0
        
        
def fixup_css(report_name):
    global need_fixup
    # Needed for VC19 and VC19 SP1.
    # From VC19 SP2 onwards a new option VCAST_RPTS_SELF_CONTAINED is used instead
    
    if not need_fixup:
        return

    with open(report_name,"r") as fd:
        data = fd.read() 

    #fix up inline CSS because of Content Security Policy violation
    newData = data[: data.index("<style>")-1] +  """
    <link rel="stylesheet" href="vector_style.css">
    """ + data[data.index("</style>")+8:]
    
    #fix up style directive because of Content Security Policy violation
    newData = newData.replace("<div class='event bs-callout' style=\"position: relative\">","<div class='event bs-callout relative'>")
    
    #fixup the inline VectorCAST image because of Content Security Policy violation
    regex_str = r"<img alt=\"Vector\".*"
    newData =  re.sub(regex_str,"<img alt=\"Vector\" src=\"vectorcast.png\"/>",newData)
    
    with open(report_name, "w") as fd:
        fd.write(newData)
   
    workspace = os.getenv("WORKSPACE")
    if workspace is None:
        workspace = os.getcwd()

    vc_scripts = os.path.join(workspace,"vc_scripts")
    
    shutil.copy(os.path.join(vc_scripts,"vector_style.css"), "management/vector_style.css")
    shutil.copy(os.path.join(vc_scripts,"vectorcast.png"), "management/vectorcast.png")

def generateCoverReport(path, env, level ):

    def _dummy(*args, **kwargs):
        return True
        
    from vector.apps.DataAPI.cover_api import CoverApi

    api=CoverApi(path)

    report_name = "management/" + level + "_" + env + ".html"
    
    try:
        try:
            api.commit = _dummy
            api.report(report_type="AGGREGATE_REPORT", formats=["HTML"], output_file=report_name)
        except:
            CustomReport.report_from_api(api, report_type="Demo", formats=["HTML"], output_file=report_name, sections=["CUSTOM_HEADER", "REPORT_TITLE", "TABLE_OF_CONTENTS", "CONFIG_DATA", "METRICS", "MCDC_TABLES",  "AGGREGATE_COVERAGE", "CUSTOM_FOOTER"])

        fixup_css(report_name)
        
    except Exception as e:
        build_dir = path.replace("\\","/")
        build_dir = build_dir.rsplit("/",1)[0]

        parse_traceback.parse(traceback.format_exc(), print_exc, level.split("_")[0] , level.split("_")[2], env, build_dir)

def generateUTReport(path, env, level): 
    global verbose

    def _dummy(*args, **kwargs):
        return True
    report_name = "management/" + level + "_" + env + ".html"

    api=UnitTestApi(path)
    try:
        api.commit = _dummy
        api.report(report_type="FULL_REPORT", formats=["HTML"], output_file=report_name)
        fixup_css(report_name)
    except Exception as e:
        build_dir = path.replace("\\","/")
        build_dir = build_dir.rsplit("/",1)[0]

        parse_traceback.parse(traceback.format_exc(), print_exc, level.split("_")[0] , level.split("_")[2], env, build_dir)
        
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

def useManageAPI(FullManageProjectName, cbtDict, generate_exec_rpt_each_testcase, use_archive_extract, report_only_failures, no_full_report, useStartLine, teePrint, use_cte):
    global verbose

    print("Using VCProjectApi")
    
    xml_file = ""
    
    try:
        from generate_xml import GenerateManageXml

        xml_file = GenerateManageXml(FullManageProjectName, 
                               verbose, 
                               cbtDict,
                               generate_exec_rpt_each_testcase,
                               use_archive_extract,
                               report_only_failures,
                               no_full_report,
                               print_exc,
                               useStartLine, teePrint, use_cte)
                               
        if xml_file.api != None:
            xml_file.generate_testresults()
            xml_file.generate_cover()
        else:
            print("   Skipping environment: " + jobNameDotted)
            print("\n\n")
            print ("******************************************************")
            print ("** Environment's that only use imported results     **")
            print ("** will not properly generate metrics with this     **")
            print ("** version of VectorCAST.                           **")
            print ("******************************************************")
            print("\n\n")
    
    except Exception as e:
        parse_traceback.parse(traceback.format_exc(), print_exc)
        #traceback.print_exc()


    try:       
        return xml_file.passed_count, xml_file.failed_count
    except:
        return 0, 0


def useNewAPI(FullManageProjectName, manageEnvs, level, envName, cbtDict, generate_exec_rpt_each_testcase, use_archive_extract, report_only_failures, no_full_report, useStartLine, teePrint, use_cte):

    failed_count = 0 
    passed_count = 0
    
    print("Using DataAPI per environment")
        
    for currentEnv in manageEnvs:
        if skipReporting(manageEnvs[currentEnv]["build_dir"], use_archive_extract, cbtDict):
            print("   No Change for " + currentEnv + ".  Skipping reporting.")
            continue 

        if envName == None:
            pc, fc = genDataApiReports(FullManageProjectName, manageEnvs[currentEnv],  cbtDict, generate_exec_rpt_each_testcase,use_archive_extract, report_only_failures, useStartLine, teePrint, use_cte)
            passed_count += pc
            failed_count += fc
            
            if no_full_report:
                continue                
                
            generateIndividualReports(manageEnvs[currentEnv], envName)
            
        elif manageEnvs[currentEnv]["env"].upper() == envName.upper(): 
            env_level = manageEnvs[currentEnv]["compiler"] + "/" + manageEnvs[currentEnv]["testsuite"]
            
            if level == None or env_level.upper() == level.upper():
                pc, fc = genDataApiReports(FullManageProjectName, manageEnvs[currentEnv], cbtDict, generate_exec_rpt_each_testcase,use_archive_extract, report_only_failures, useStartLine, teePrint, use_cte)
                passed_count += pc
                failed_count += fc
                
                if no_full_report:
                    continue                

                generateIndividualReports(manageEnvs[currentEnv], envName)
                
    return passed_count, failed_count

def cleanupDirectory(path, teePrint):

    # if the path exists, try to delete all file in it
    if os.path.isdir(path):
        shutil.rmtree(path)
    os.mkdir(path)

def cleanupOldBuilds(teePrint):
    for path in ["xml_data","management","execution"]:
        cleanupDirectory(path, teePrint)

# build the Test Case Management Report for Manage Project
# envName and level only supplied when doing reports for a sub-project
# of a multi-job
def buildReports(FullManageProjectName = None,
    level = None,
    envName = None,
    generate_individual_reports = True,
    timing = False,
    cbtDict = None,
    use_archive_extract = False,
    report_only_failures = False,
    no_full_report = False,
    use_ci = "",
    xml_data_dir = "xml_data",
    useStartLine = False,
    teePrint = None,
    use_cte = False):
        
    if timing:
        print("Start report generation: " + str(time.time()))
        
    saved_level = level
    saved_envName = envName
   
    getEnabledEnvironments(FullManageProjectName)
    
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

    if teePrint is None:
        teePrint = tee_print.TeePrint()
            
    cleanupOldBuilds(teePrint)

    for file in glob.glob("*.csv"):
        try:
            os.remove(file);
            if verbose:
                print("Removing file: " + file)
        except Exception as e:
            teePrint.teePrint("   *INFO: File System Error removing " + file + ".  Check console for environment build/execution errors")
            if print_exc:  traceback.print_exc()
   
    failed_count = 0
    passed_count = 0
    
    ### Using new data API - 2019 and beyond
    if timing:
        print("Cleanup: " + str(time.time()))
    if useNewReport and not legacy:
        try:
            api = VCProjectApi(FullManageProjectName)
            tool_version = api.tool_version
            if tool_version.startswith("20"):
                use_manage_api = False
            else:
                use_manage_api = True
            api.close()
        except:
            ##teePrint.teePrint("   *INFO: Issue getting tool version from: " + FullManageProjectName)
            use_manage_api = False
            
        if use_manage_api:
            passed_count, failed_count = useManageAPI(FullManageProjectName, cbtDict, generate_individual_reports, 
                    use_archive_extract, report_only_failures, no_full_report,
                    useStartLine, teePrint, use_cte)

        else:
                
            manageEnvs = getManageEnvs(FullManageProjectName)
            if timing:
                print("Using DataAPI for reporting")
                print("Get Info: " + str(time.time()))
            passed_count, failed_count = useNewAPI(FullManageProjectName, 
                manageEnvs, level, envName, cbtDict, generate_individual_reports, 
                use_archive_extract, report_only_failures, no_full_report,
                useStartLine, teePrint, use_cte)
            
        with open("unit_test_fail_count.txt", "w") as fd:
            failed_str = str(failed_count)
            try:
                fd.write(unicode(failed_str))
            except:
                fd.write(failed_str)    
           
        with open("unit_test_passfail_count.txt", "w") as fd:
            passfail_str = str(passed_count) + " " + str(failed_count)
            try:
                fd.write(unicode(passfail_str))
            except:
                fd.write(passfail_str)    
           
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
        
        if "Database error:" in out_mgt:
            print("\n\n")
            print ("******************************************************")
            print ("** May have failed to create individual environment **")
            print ("**   reports because of a database error).          **")
            print ("**                                                  **")
            print ("** FYI: Environments that only use imported results **")
            print ("**   will not properly generate metrics with this   **")
            print ("**   version of VectorCAST.                         **")
            print ("******************************************************")
            print("\n\n")

        coverProjectInManageProject = False
        if "database missing or inaccessible" in out_mgt:
            coverProjectInManageProject = True
        elif re.search('Environment directory.*is missing', out_mgt):
            coverProjectInManageProject = True
        if coverProjectInManageProject:
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
            
            if "Database error:" in out_exe:
                print("\n\n")
                print ("******************************************************")
                print ("** May have failed to create individual environment **")
                print ("**   reports because of a database error).          **")
                print ("**                                                  **")
                print ("** FYI: Environments that only use imported results **")
                print ("**   will not properly generate metrics with this   **")
                print ("**   version of VectorCAST.                         **")
                print ("******************************************************")
                print("\n\n")

        else:
            out = out_mgt

        if verbose:
            print(out)

        # save the output of the manage command for debug purposes
        with open("build.log","w") as fd:
            fd.write(out)
        
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

                    vcastcsv2jenkins.run  (test = testResultName, coverage = coverageResultsName, useExecRpt=generate_individual_reports, version=version)

                    if envName:
                        adjustedReportName = "management" + os.sep + jobName + "_" + envName + ".html"
                    else:
                        adjustedReportName = "management" + os.sep + jobName + "_" + env + ".html"

                # Create a list for later to copy the files over
                copyList.append([reportName,adjustedReportName])
                # Reset env
                env = None
        
        if coverProjectInManageProject:
            generate_qa_results_xml.genQATestResults(FullManageProjectName,saved_level,saved_envName)
            
        failed_count = 0
        passed_count = 0
        try:
            for file in glob.glob("xml_data/test_results_*.xml"):
                with open(file,"r") as fd:
                    lines = fd.readlines()
            
                for line in lines:
                    if "failures" in line:
                        failed_count += int(line.split("\"")[5])
                        passed_count += int(line.split("\"")[3]) - failed_count
                        break
        except:
            teePrint.teePrint ("   *INFO: Problem parsing test results file for unit testcase failure count: " + file)
            if print_exc:  traceback.print_exc()
            
        with open("unit_test_fail_count.txt", "w") as fd:
            failed_str = str(failed_count)
            try:
                fd.write(unicode(failed_str))
            except:
                fd.write(failed_str)    

        with open("unit_test_passfail_count.txt", "w") as fd:
            passfail_str = str(passed_count) + " " + str(failed_count)
            try:
                fd.write(unicode(passfail_str))
            except:
                fd.write(passfail_str)    
           

        for file in copyList:

            if verbose:
                print("moving %s -> %s" % (file[0], file[1]))

            shutil.move(file[0], file[1])
            
    if timing:
        print("Complete report generate: " + str(time.time()))
        
    return failed_count, passed_count

        
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject',                    help='Manager Project Name')
    parser.add_argument('-v', '--verbose',                  help='Enable verbose output', action="store_true")
    parser.add_argument('-l', '--level',                    help='Environment Name if only doing single environment.  Should be in the form of level/env')
    parser.add_argument('-e', '--environment',              help='Environment Name if only doing single environment.  Should be in the form of level/env')
    parser.add_argument('-g', '--dont-generate-individual-reports',   
                                                            help='Don\'t Generated Individual Reports. Below VC2019 - this just controls execution report generate. VC2019 and later - execution reports for each testcase won\'t be generated',  action="store_true", default=False)
    parser.add_argument('--wait_time',                      help='Time (in seconds) to wait between execution attempts', type=int, default=30)
    parser.add_argument('--wait_loops',                     help='Number of times to retry execution', type=int, default=1)
    parser.add_argument('--timing',                         help='Display timing information for report generation', action="store_true", default = False)
    parser.add_argument('--buildlog',                       help='Build Log for CBT Statitics', default = None)
    
    ## Hidden because they are specific to customer need or testing
    parser.add_argument('--junit',                          help=argparse.SUPPRESS, action="store_true")
    parser.add_argument('--junit_use_cte_for_classname',    help=argparse.SUPPRESS, action="store_true", dest="use_cte")
    parser.add_argument('--print_exc',                      help=argparse.SUPPRESS, action="store_true")
    parser.add_argument('--api',                            help=argparse.SUPPRESS, type=int)
    parser.add_argument('--use_archive_extract',            help=argparse.SUPPRESS, action="store_true", default = False)
    parser.add_argument('--report_only_failures',           help=argparse.SUPPRESS, action="store_true", default = False)
    parser.add_argument('--no_full_report',                 help=argparse.SUPPRESS, action="store_true", default = False)
    parser.add_argument('--legacy',                         help=argparse.SUPPRESS, action="store_true", default = False)

    args = parser.parse_args()
    
    if args.use_archive_extract and (not args.buildlog or not os.path.exists(args.buildlog)):
        print("Must have a valid --buildlog file to use --use_archive_extract")
        print("The option use_archive_extract is disabled")
        args.use_archive_extract = False
    
    legacy = args.legacy
    timing = args.timing
    
    if timing:
        print("Start: " + str(time.time()))
        
    if legacy and sys.version_info[0] >= 3:
        print ("Legacy mode testing not support with Python3 (VectorCAST 2021 and above)")
        sys.exit(-1)
        
    try:
        tool_version = os.path.join(os.environ['VECTORCAST_DIR'], "DATA", "tool_version.txt")
        with open(tool_version,"r") as fd:
            ver = fd.read()
            
        if ver.startswith("19 "):
            need_fixup = True
            
        if ver.startswith("19.sp1"):
            need_fixup = True
            # custom report patch for SP1 problem - should be fixed in future release      
            old_init = CustomReport._post_init
            def new_init(self):
                old_init(self)
                self.context['report']['use_all_testcases'] = True
            CustomReport._post_init = new_init
    except:
        pass
    
    tcmr2csv.useLocalCsv = True

    generate_individual_reports = not args.dont_generate_individual_reports

    if args.verbose:
        verbose = True
        
    if args.print_exc or verbose:
        print_exc = True
        
    wait_time = args.wait_time
    wait_loops = args.wait_loops

    junit = True
        

    if args.buildlog and os.path.exists(args.buildlog):
        with open(args.buildlog,"r") as fd:
            buildLogData = fd.readlines()
        cbt = ParseConsoleForCBT(verbose)
        cbtDict = cbt.parse(buildLogData)
        
        if timing:
            print("CBT Parse: " + str(time.time()))
        
    else:
        cbtDict = None
            
    if timing:
        print("Getting enabled envs: " + str(time.time()))

    # Used for pre VC19
    os.environ['VCAST_RPTS_PRETTY_PRINT_HTML'] = 'FALSE'
    # Used for VC19 SP2 onwards
    os.environ['VCAST_RPTS_SELF_CONTAINED'] = 'FALSE'
    # Set VCAST_MANAGE_PROJECT_DIRECTORY to match .vcm directory
    os.environ['VCAST_MANAGE_PROJECT_DIRECTORY'] = os.path.abspath(args.ManageProject).rsplit(".",1)[0]
 
    with tee_print.TeePrint() as teePrint:
        buildReports(args.ManageProject,
            args.level,
            args.environment,
            generate_individual_reports,
            timing,
            cbtDict,
            args.use_archive_extract,
            args.report_only_failures,
            args.no_full_report,
            use_ci = "",
            xml_data_dir = "xml_data",
            useStartLine = False,
            teePrint = teePrint,
            use_cte = args.use_cte)
    
    import archive_extract_reports
        
    if (args.use_archive_extract):
        archive_extract_reports.extract(verbose)
        if timing:
            print("extracting reports: " + str(time.time()))


