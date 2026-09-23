#
# The MIT License
#
# Copyright 2026 Vector Informatik, GmbH.
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

import os
from datetime import datetime
import sys
from report_xml import JUnitReport
from vector.apps.DataAPI.unit_test_api import UnitTestApi
from vector.apps.DataAPI.unit_test_models import TestCase
from vector.apps.DataAPI.vcproject_api import VCProjectApi
from vector.apps.DataAPI.cover_api import CoverApi
from vcast_utils import getVectorCASTEncoding
import hashlib
import traceback
from runtime_logging import get_logger, log_exception

def dummy(*args, **kwargs):
    return None

##########################################################################
# Shared JUnit result handling for Manage projects and environments.
#
class BaseGenerateXml(object):
    def __init__(self, FullManageProjectName, verbose, teePrint, use_cte):
        projectName = os.path.splitext(os.path.basename(FullManageProjectName))[0]
        self.manageProjectName = projectName
        self.unit_report_name = os.path.join("xml_data","test_results_"+ self.manageProjectName + ".xml")
        self.verbose = verbose
        self.print_exc = False
        self.teePrint = teePrint
        self.use_cte = use_cte
        
        # get the VC langaguge and encoding
        self.encFmt = getVectorCASTEncoding()
        self.compiler = ""
        self.testsuite = ""
        self.env = ""
        self.build_dir = ""

        if self.teePrint is None:
            self.teePrint = get_logger()

        self.system_tests_status_report_generated = False

    def generate_system_test_status_report(self):
        if self.system_tests_status_report_generated:
            return

        print("    Creating System Test Status " + self.FullManageProjectName)
        for report_name_ext in [".txt", ".html"]:
            report_name = os.path.basename(self.FullManageProjectName)[:-4] + "_system_tests_status" + report_name_ext
            callStr = os.environ.get('VECTORCAST_DIR') + os.sep + "manage --project " + self.FullManageProjectName + " --system-tests-status=" + report_name
            import subprocess
            p = subprocess.Popen(callStr, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
            out, err = p.communicate()

            if err:
                print("Cannot create system test status report{} {}".format(out, err))

            self.system_tests_status_report_generated = True

    def convertTestHistory (self,status):
        convertDict = {'TEST_HISTORY_FAILURE_REASON_DATA_SKEW_UNDERFLOW':"Harness Data Underflow",
                       'TEST_HISTORY_FAILURE_REASON_DATA_SKEW_OVERFLOW':"Harness Data Overflow",
                       'TEST_HISTORY_FAILURE_REASON_HARNESS_FAILURE':"Harness Error",
                       'TEST_HISTORY_FAILURE_REASON_THISTORY_FILE_DOES_NOT_EXIST':"Event History Missing",
                       'TEST_HISTORY_FAILURE_REASON_THISTORY_LINE_INVALID':"Event Data Invalid",
                       'TEST_HISTORY_FAILURE_REASON_THISTORY_ENDED_PREMATURELY':"Event History Processing Failed",
                       'TEST_HISTORY_FAILURE_REASON_EXPECTED_ENDED_PREMATURELY':"Event History Processing Failed",
                       'TEST_HISTORY_FAILURE_REASON_HARNESS_COMMNAD_INVALID':"Harness Command Invalid",
                       'TEST_HISTORY_FAILURE_REASON_TEST_HISTORY_OUTPUT_FILES_CONTAIN_ERROR':"Test History Output Files contain errors",
                       'TEST_HISTORY_FAILURE_REASON_STRICT_IMPORT_FAILED':"Strict Import Failure - See Scripting Log under Test=>View ",
                       'TEST_HISTORY_FAILURE_REASON_MACRO_NOT_FOUND':"Symbolic constant not found",
                       'TEST_HISTORY_FAILURE_REASON_SYMBOL_OR_MACRO_NOT_FOUND':"Symbolic constant not found",
                       'TEST_HISTORY_FAILURE_REASON_SYMBOL_OR_MACRO_TYPE_MISMATCH':"Symbolic constant has incorrect type",
                       'TEST_HISTORY_FAILURE_REASON_EMPTY_TESTCASES':"Empty Test Case",
                       'TEST_HISTORY_FAILURE_REASON_NO_EXPECTED_VALUES':"No expected values",
                       'TEST_HISTORY_FAILURE_REASON_NO_EXPECTED_RETURN':"No expected return",
                       'TEST_HISTORY_FAILURE_REASON_EXECUTABLE_MISSING':"Executable Missing",
                       'TEST_HISTORY_FAILURE_REASON_MAX_VARY_EXCEEDED':"Max Vary Failure - too many Range/List input values ",
                       'TEST_HISTORY_FAILURE_REASON_INSUFFICIENT_HEAP_SIZE':"VCAST_malloc failed - insufficient heap.",
                       'TEST_HISTORY_FAILURE_REASON_LIBRARY_MALLOC_FAILED':"malloc failed - memory was exhausted",
                       'TEST_HISTORY_FAILURE_REASON_TRUNCATED_HARNESS_DATA':"Truncated Harness Data",
                       'TEST_HISTORY_FAILURE_REASON_HARNESS_STDOUT_DATA_UNDERFLOW':"Harness Standard Out Data Underflow",
                       'TEST_HISTORY_FAILURE_REASON_MAX_STRING_LENGTH_EXCEEDED':"Harness Maximum String Length Exceeded",
                       'TEST_HISTORY_FAILURE_REASON_TIMEOUT_EXCEEDED':"Timed Out"}
        return convertDict[str(status)]

    def convertTcStatus(self, status):
        convertDict = { 'TCR_STATUS_OK' : 'Testcase can run',
                        'TCR_STRICT_IMPORT_FAILED' : 'Strict Testcase Import Failure',
                        'TCR_MAXIMUM_VARY_EXCEEDED' : 'Maximum varied parameters exceeded',
                        'TCR_EMPTY_TEST_CASES' : 'Empty testcase',
                        'TCR_NO_EXPECTED_VALUES' : 'No expected values',
                        'TCR_NO_EXPECTED_RETURN' : 'No expected return value',
                        'TCR_NO_SLOTS' : 'Compound with no slot',
                        'TCR_ZERO_ITERATIONS' : 'Compound with zero slot',
                        'TCR_RECURSIVE_COMPOUND' : 'Recursive Compound Test',
                        'TCR_COMMON_COMPOUND_CONTAINING_SPECIALIZED' : 'Non-specialized compound containing specialized testcases',
                        'TCR_HIDING_EXPECTED_RESULTS' : 'Hiding expected results',
                        'TCR_MAX_STRING_LENGTH_EXCEEDED' : 'Maximum string length exceeded',
                        'TCR_MAX_FILE_COUNT_EXCEEDED' : 'Maximum file count exceeded',
                        'TCR_TIMEOUT_EXCEEDED' : 'Testcase timeout',
                        'TCR_INTERNAL_ERROR' : 'Internal VectorCAST Error'
                      }
        return convertDict[str(status)]

    def convertExecStatus(self, status):
        convertDict = { 'EXEC_SUCCESS_PASS':'Testcase passed',
                        'EXEC_SUCCESS_FAIL':'Testcase failed',
                        'EXEC_SUCCESS_NONE':'No expected results',
                        'EXEC_EXECUTION_FAILED':'Testcase failed to run to completion (possible testcase timeout)',
                        'EXEC_ABORTED':'User aborted testcase',
                        'EXEC_TIMEOUT_EXCEEDED':'Testcase timeout',
                        'EXEC_VXWORKS_LOAD_ERROR':'VxWorks load error',
                        'EXEC_USER_CODE_COMPILE_FAILED':'User code failed to compile',
                        'EXEC_COMPOUND_ONLY':'Compound only test case',
                        'EXEC_STRICT_IMPORT_FAILED':'Strict Testcase Import Failure',
                        'EXEC_MACRO_NOT_FOUND':'Macro not found',
                        'EXEC_SYMBOL_OR_MACRO_NOT_FOUND':'Symbol or macro not found',
                        'EXEC_SYMBOL_OR_MACRO_TYPE_MISMATCH':'Symbol or macro type mismatch',
                        'EXEC_MAX_VARY_EXCEEDED':'Maximum varied parameters exceeded',
                        'EXEC_COMPOUND_WITH_NO_SLOTS':'Compound with no slot',
                        'EXEC_COMPOUND_WITH_ZERO_ITERATIONS':'Compound with zero slot',
                        'EXEC_STRING_LENGTH_EXCEEDED':'Maximum string length exceeded',
                        'EXEC_FILE_COUNT_EXCEEDED':'Maximum file count exceeded',
                        'EXEC_EMPTY_TESTCASE':'Empty testcase',
                        'EXEC_NO_EXPECTED_RETURN':'No expected return value',
                        'EXEC_NO_EXPECTED_VALUES':'No expected values',
                        'EXEC_CSV_MAP':'CSV Map',
                        'EXEC_DRIVER_DATA_COMPILE_FAILED':'Driver data failed to compile',
                        'EXEC_RECURSIVE_COMPOUND':'Recursive Compound Test',
                        'EXEC_SPECIALIZED_COMPOUND_CONTAINING_COMMON':'Specialized compound containing non-specialized testcases',
                        'EXEC_COMMON_COMPOUND_CONTAINING_SPECIALIZED':'Non-specialized compound containing specialized testcases',
                        'EXEC_HIDING_EXPECTED_RESULTS':'Hiding expected results',
                        'INVALID_TEST_CASE':'Invalid Test Case'
                       }
        try:
            s = convertDict[str(status)]
        except:
            s = convertDict[status]
        return s

class GenerateManageXml (BaseGenerateXml):

# GenerateManageXml

    def __init__(self, FullManageProjectName, verbose = False,
                       cbtDict = None,
                       generate_exec_rpt_each_testcase = True,
                       use_archive_extract = False,
                       report_failed_only = False,
                       no_full_reports = False,
                       print_exc = False,
                       useStartLine = False,
                       teePrint = None,
                       use_cte = False):

        super(GenerateManageXml, self).__init__(FullManageProjectName, verbose, teePrint, use_cte)

        self.FullManageProjectName = FullManageProjectName
        self.generate_exec_rpt_each_testcase = generate_exec_rpt_each_testcase
        self.use_archive_extract = use_archive_extract
        self.report_failed_only = report_failed_only
        self.cbtDict = cbtDict
        self.no_full_reports = no_full_reports
        self.failed_count = 0
        self.passed_count = 0
        self.print_exc = print_exc
        self.useStartLine = useStartLine

        self.cleanupXmlDataDir()

        vcproj = VCProjectApi(FullManageProjectName)

        hasCover = any(isinstance(env.api, CoverApi) for env in vcproj.Environment.all())
        vcproj.close()

        if hasCover:
            self.generate_system_test_status_report()

        self.api = VCProjectApi(FullManageProjectName)

    def cleanupXmlDataDir(self):
        path="xml_data"
        import glob
        # if the path exists, try to delete all file in it
        if os.path.isdir(path):
            for file in glob.glob(path + "/*.*", recursive=False):
                try:
                    os.remove(file);
                except:
                    self.teePrint.info("   *INFO: File System Error removing file after failed to remove directory: " + path + "/" + file + ". Check console for environment build/execution errors")
                    if print_exc:  traceback.print_exc()

        # we should either have an empty directory or no directory
        else:
            try:
                os.mkdir(path)
            except:
                print("failed making path: " + path)
                self.teePrint.info("   *INFO: File System Error creating directory: " + path + ". Check console for environment build/execution errors")
                if print_exc:  traceback.print_exc()

    def __del__(self):
        try:
            self.api.close()
        except:
            print("[DEBUG] Exception closing in self.api generate_junit::GenerateManageXml::__del__")
            pass

# GenerateManageXml

    def generate_local_results(self, env):
        comp, ts, env_name = env.compiler.name, env.testsuite.name, env.name
        build_dir = env.build_directory
        vceFile =  os.path.join(build_dir, env.name+".vce")
        vcpFile =  os.path.join(build_dir, env.name+".vcp")
        if not os.path.exists(vceFile) and not os.path.exists(vcpFile):
            print("Error: Could not determine environment location for {}/{}".format(build_dir, env.name))
            print("       {}/{}/{}".format(comp, ts, env_name))
            return

        xmlUnitReportName = os.getcwd() + os.sep + "xml_data" + os.sep + "test_results_" + "_".join([comp, ts, env_name]) + ".xml"

        localXML = None

        localXML = GenerateXml(self.FullManageProjectName, build_dir, env_name, comp, ts,
                               xmlUnitReportName, self.verbose,
                               self.cbtDict,
                               self.generate_exec_rpt_each_testcase,
                               self.use_archive_extract,
                               self.report_failed_only,
                               self.print_exc,
                               self.useStartLine,
                               self.teePrint,
                               self.use_cte,
                               self.system_tests_status_report_generated)

        localXML.topLevelAPI = self.api
        localXML.noResults = self.noResults
        localXML.generate_unit()

        if not self.no_full_reports:
            report_name = os.path.join("management", comp + "_" + ts + "_" + env_name + ".html")
            try:
                if isinstance(localXML.api, CoverApi):
                    try:
                        localXML.api.report(report_type="AGGREGATE_REPORT", formats=["HTML"], output_file=report_name)
                    except:
                        if self.verbose:
                            print("Failed to create " + report_name + " by CustomReport API. Using clicast directly")
                        self.runAggregateReport(comp, ts, env_name, report_name)
                else:
                    try:
                        localXML.api.report(report_type="FULL_REPORT", formats=["HTML"], output_file=report_name)
                    except:
                        if self.verbose:
                            print("Failed to create " + report_name + " by CustomReport API. Using clicast directly")
                        self.runFullReport(comp, ts, env_name, report_name)
            except:
                print("Error creating report " + report_name + ". Contact Vector Support")
                log_exception(self.compiler, self.testsuite, self.env, self.build_dir)

    def runFullReport(self,comp,ts,env_name,report_name):
        try:
            from managewait import ManageWait
            callStr = "--project " + self.FullManageProjectName + " --level " + comp + "/" + ts + " --environment " + env_name + " --clicast-args report custom full"

            manageWait = ManageWait(False, callStr, 1, 1)
            out = manageWait.exec_manage(True)
            fname = None
            for line in out.split("\n"):
                if "The HTML report was saved to" in line:
                    fname = line.split("\"")[1]

            if fname:
                import shutil
                shutil.copyfile(fname, report_name)
            else:
                print("Error creating report " + report_name + ". Contact Vector Support")
        except:
            traceback.print_exc()

    def runAggregateReport(self,comp,ts,env_name,report_name):
        try:
            from managewait import ManageWait
            callStr = "--project " + self.FullManageProjectName + " --level " + comp + "/" + ts + " --environment " + env_name + " --clicast-args cover report aggregate"

            manageWait = ManageWait(False, callStr, 1, 1)
            out = manageWait.exec_manage(True)
            for line in out.split("\n"):
                if "The HTML report was saved to" in line:
                    fname = line.split("\"")[1]
            if fname:
                import shutil
                shutil.copyfile(fname, report_name)
            else:
                print("Error creating report " + report_name + ". Contact Vector Support")
        except:
            traceback.print_exc()

    def skipReporting(self, env):

        build_dir = ""

        if self.use_archive_extract and self.cbtDict:
            try:
                prj_dir = os.environ['WORKSPACE'].replace("\\","/") + "/"
            except:
                prj_dir = os.getcwd().replace("\\","/") + "/"

            try:
                build_dir = os.path.relpath(env.build_directory,prj_dir).replace("\\","/")
            except:
                build_dir = env.build_directory.replace("\\","/")

            try:
                build_dir = "build/" + build_dir.rsplit("build/",1)[-1]

            except:
                traceback.print_exc()
                print("exception converting directory into relative path: {} {}".format(env.build_directory, build_dir))

            ## use hash code instead of final directory name as regression scripts can have overlapping final directory names

            build_dir_4hash = build_dir.upper()
            build_dir_4hash = "/".join(build_dir_4hash.split("/")[-2:])

            # Unicode-objects must be encoded before hashing in Python 3
            if sys.version_info[0] >= 3:
                build_dir_4hash = build_dir_4hash.encode('utf-8')

            hashCode = hashlib.md5(build_dir_4hash).hexdigest()

            if hashCode not in self.cbtDict.keys():
                if self.verbose:
                    print("Skipping report because hashCode (" + hashCode + ") for build dir (" + build_dir + ") not found in cbtdict")

                return True
            else:
                c,i,s = self.cbtDict[hashCode]
                if len(c)==0 and len(i)==0 and len(s)==0:
                    if self.verbose:
                        print("skipping report because c,i,s are all 0 size")
                    return True

        return False

# GenerateManageXml
    def generate_testresults(self):
        project_status = self.api.project.tree.full_status
        self.localDataOnly = True
        self.noResults = False
        if project_status['testcase_results'] == {}:
            print("** No results in project")
            self.noResults = True
            total = success = errors = failed = 0
        else:
            total   = project_status['testcase_results']['total_count']
            success = project_status['testcase_results']['success_count']
            errors  = total - success
            failed  = errors
            self.failed_count = errors
            self.passed_count = success
        self.junit_report = JUnitReport(
            self.manageProjectName, errors, total, failed)
        imported_passed = imported_failed = 0

        for env in self.api.Environment.all():
            if not env.is_active or self.skipReporting(env):
                continue
            comp, ts, env_name = env.compiler.name, env.testsuite.name, env.name
            status = env.level.full_status
            if status['local'] != {}:
                self.generate_local_results(env)
            else:
                for imported_result in status['imported'].values():
                    self.localDataOnly = False
                    total = imported_result['testcase_results']['total_count']
                    success = imported_result['testcase_results']['success_count']
                    failed = total - success
                    import_name = imported_result['name']
                    classname = comp + "." + ts + "." + env_name
                    for idx in range(1, success + 1):
                        name = "ImportedResults." + import_name + ".TestCase.PASS.%03d" % idx
                        self.junit_report.add_case(name, classname, status="skipped")
                        imported_passed += 1

                    for idx in range(1, failed + 1):
                        name = "ImportedResults." + import_name + ".TestCase.FAIL.%03d" % idx
                        self.junit_report.add_case(name, classname, status="failure")
                        imported_failed += 1

        if not self.localDataOnly:
            if self.noResults:
                self.passed_count = imported_passed
                self.failed_count = imported_failed
                self.junit_report.set_counts(
                    imported_failed, imported_passed + imported_failed,
                    imported_failed)
            self.junit_report.write(self.unit_report_name, self.encFmt)

##########################################################################
# This class generates the JUnit report for one environment.
#
class GenerateXml(BaseGenerateXml):

    def __init__(self, FullManageProjectName, build_dir, env, compiler, testsuite, unit_report_name, verbose = False, cbtDict= None, generate_exec_rpt_each_testcase = True,
            use_archive_extract = False, report_failed_only = False, print_exc = False, useStartLine = False, teePrint = None, use_cte = False, system_tests_status_report_generated = False):

        super(GenerateXml, self).__init__(FullManageProjectName, verbose, teePrint, use_cte)

        self.cbtDict = cbtDict
        self.FullManageProjectName = FullManageProjectName
        self.generate_exec_rpt_each_testcase = generate_exec_rpt_each_testcase
        self.use_archive_extract = use_archive_extract
        self.report_failed_only = report_failed_only
        self.print_exc = print_exc
        self.topLevelAPI = None
        self.noResults = False
        self.useStartLine = useStartLine
        self.system_tests_status_report_generated = system_tests_status_report_generated

        ## use hash code instead of final directory name as regression scripts can have overlapping final directory names
        build_dir = build_dir.replace("\\","/")
        if build_dir.endswith("/."):
            build_dir = build_dir.replace("/.","")
        build_dir_4hash = build_dir.upper()
        build_dir_4hash = "/".join(build_dir_4hash.split("/")[-2:])

        # Unicode-objects must be encoded before hashing in Python 3
        if sys.version_info[0] >= 3:
            build_dir_4hash = build_dir_4hash.encode(self.encFmt)

        self.hashCode = hashlib.md5(build_dir_4hash).hexdigest()

        if verbose:
            print ("HashCode: " + self.hashCode + "for build dir: " + build_dir)
            print(env)

        self.build_dir = build_dir
        self.env = env
        self.compiler = compiler
        self.testsuite = testsuite
        self.unit_report_name = unit_report_name
        cov_path = os.path.join(build_dir,env + '.vcp')
        unit_path = os.path.join(build_dir,env + '.vce')
        if os.path.exists(cov_path):
            self.generate_system_test_status_report()
            self.api = CoverApi(cov_path)
        elif os.path.exists(unit_path):
            self.api = UnitTestApi(unit_path)
        else:
            self.api = None
            if verbose:
                print("Error: Could not determine project type for {}/{}".format(build_dir, env))
                print("       {}/{}/{}".format(compiler, testsuite, env))
            return

        self.api.commit = dummy
        self.failed_count = 0
        self.passed_count = 0

#
# GenerateXml - add any compound tests to the unit report
#
    def add_compound_tests(self):
        for tc in self.api.TestCase.all():
            if tc.kind == TestCase.KINDS['compound']:
                if not tc.for_compound_only:
                    self.write_testcase(tc, "<<COMPOUND>>", "<<COMPOUND>>")

#
# GenerateXml - add any intialisation tests to the unit report
#
    def add_init_tests(self):
        for tc in self.api.TestCase.all():
            if tc.kind == TestCase.KINDS['init']:
                if not tc.for_compound_only:
                    self.write_testcase(tc, "<<INIT>>", "<<INIT>>")

#
# GenerateXml - Find the test case file
#
    def generate_unit(self):

        if isinstance(self.api, CoverApi):
            try:
                self.start_system_test_file()

                if self.topLevelAPI == None:
                    vcproj = VCProjectApi(self.FullManageProjectName)
                else:
                    vcproj = self.topLevelAPI

                for env in vcproj.Environment.all():
                    if env.compiler.name == self.compiler and env.testsuite.name == self.testsuite and env.name == self.env and env.system_tests:
                        for st in env.system_tests:
                            pass_fail_rerun = ""
                            if st.run_needed and st.type == 2: #SystemTestType.MANUAL:
                                pass_fail_rerun =  ": Manual system tests can't be run in Jenkins"
                            elif st.run_needed:
                                pass_fail_rerun =  ": Needs to be executed"
                            elif st.passed:
                                pass_fail_rerun =  ": Passed"
                            else:
                                pass_fail_rerun =  ": Failed"

                            level = env.compiler.name + "/" + env.testsuite.name + "/" + env.name
                            self.write_testcase(st, level, st.name, env.definition.is_monitored)

                if self.topLevelAPI == None:
                    vcproj.close()

            except ImportError as e:
                from generate_qa_results_xml import genQATestResults
                pc,fc = genQATestResults(self.FullManageProjectName, self.compiler + "/" + self.testsuite, self.env, True, self.encFmt)
                self.failed_count += fc
                self.passed_count += pc
                return
        else:
            try:
                self.start_unit_test_file()
                self.add_compound_tests()
                self.add_init_tests()
                for unit in self.api.Unit.all():
                    if unit.is_uut:
                        for func in unit.functions:
                            if not func.is_non_testable_stub:
                                for tc in func.testcases:
                                    if not self.isTcPlaceHolder(tc):
                                        if not tc.for_compound_only or tc.testcase_status == "TCR_STRICT_IMPORT_FAILED":
                                            self.write_testcase(tc, tc.function.unit.name, tc.function.display_name, unit = unit)

            except AttributeError as e:
                log_exception(self.compiler, self.testsuite, self.env, self.build_dir)

        self.end_test_results_file()

#
# GenerateXml - write the end of the jUnit XML file and close it
#
    def isTcPlaceHolder(self, tc):
        placeHolder = False
        try:
            vctMap = tc.is_vct_map
        except:
            vctMap = False
        try:
            vcCodedTestMap = tc.is_coded_tests_map
        except:
            vcCodedTestMap = False
        try:
            if tc and len(tc.variant_logic) > 0 and tc.execution_status == 'EXEC_VARIANT_LOGIC_FALSE':
                vcVariantTestSkipped = True
            else:
                vcVariantTestSkipped = False
        except:
            vcVariantTestSkipped = False

        # Placeholder "testcases" that need to be ignored
        if tc.is_csv_map or vctMap or vcCodedTestMap or vcVariantTestSkipped:
            placeHolder = True

        return placeHolder

#
# GenerateXml - write the end of the jUnit XML file and close it
#
    def end_test_results_file(self):
        self.junit_report.write(self.unit_report_name, self.encFmt)

#
# GenerateXml - start the JUnit XML file
#

    def start_system_test_file(self):
        errors = 0
        failed = 0
        success = 0

        from vector.apps.DataAPI.vcproject_api import VCProjectApi

        if self.topLevelAPI == None:
            vcproj = VCProjectApi(self.FullManageProjectName)
        else:
            vcproj = self.topLevelAPI

        for env in vcproj.Environment.all():
            if env.compiler.name == self.compiler and env.testsuite.name == self.testsuite and env.name == self.env and env.system_tests:
                for st in env.system_tests:
                    if st.passed == st.total:
                        success += 1
                        self.passed_count += 1
                    else:
                        failed += 1
                        errors += 1
                        self.failed_count += 1

        if self.topLevelAPI == None:
            vcproj.close()

        self.junit_report = JUnitReport(
            self.env, errors, success + failed + errors, failed)

    def start_unit_test_file(self):

        errors = 0
        failed = 0
        success = 0

        for tc in self.api.TestCase.all():
            if not self.noResults and (not tc.for_compound_only or tc.testcase_status == "TCR_STRICT_IMPORT_FAILED") and not self.isTcPlaceHolder(tc):
                if not tc.passed:
                    self.failed_count += 1
                    if tc.execution_status != "EXEC_SUCCESS_FAIL ":
                        errors += 1
                    else:
                        failed += 1
                else:
                    success += 1
                    self.passed_count += 1
        self.junit_report = JUnitReport(
            self.env, errors, success + failed + errors, failed)

    def testcase_failed(self, tc):

        try:
            from vector.apps.DataAPI.manage_models import SystemTest
            if (isinstance(tc, SystemTest)):
                if tc.run_needed and tc.type == 2:
                    return False
                elif tc.run_needed:
                    return False
                elif tc.passed == tc.total:
                    return False
                else:
                    return True
        except:
            pass

        if not tc.passed:
            return True

        return False

#
# GenerateXml - write a testcase to the jUnit XML file
#
    def write_testcase(self, tc, unit_name, func_name, st_is_monitored = False, unit = None):

        fpath = ""
        startLine = ""
        unitName = ""
        
        failureReasons = ""
        didntRunReason = ""
        
        if unit:
            if tc.status == "TC_EXECUTION_PASSED":
                pass

            if tc.status == "TC_EXECUTION_FAILED":
                for reason in tc.failure_reasons:
                    failureReasons += self.convertTestHistory(reason) + ' | '
                failureReasons = failureReasons[:-3]

            if tc.status == "TC_EXECUTION_NONE":
                didntRunReason = self.convertTcStatus(tc.testcase_status)

            try:
                filePath = unit.sourcefile.normalized_path(normcase=False)
            except:
                filePath = unit.sourcefile.normalized_path

            try:
                prj_dir = os.environ['WORKSPACE'].replace("\\","/") + "/"
            except:
                prj_dir = os.getcwd().replace("\\","/") + "/"

            try:
                fpath = os.path.relpath(filePath,prj_dir).replace("\\","/")
            except:
                fpath = filePath.replace("\\","/")

            if self.useStartLine:
                try:
                    startLine = str(tc.function.start_line)
                except:
                    try:
                        startLine = list(tc.cover_data.covered_statements)[0].start_line
                    except:
                        startLine = "0"
                        print("failed to access any start_line {} {} {}".format(self.env, func_name, tc.name))
            else:
                startLine = "0"

            unitName = unit.name

        if self.noResults:
            return

        failure_message = ""

        if self.report_failed_only and not self.testcase_failed(tc):
            return

        isSystemTest = False

        try:
            from vector.apps.DataAPI.manage_models import SystemTest
            if (isinstance(tc, SystemTest)):
                isSystemTest = True
        except:
            pass

        start_tdo = datetime.now()
        end_tdo   = None

        # don't do CBT analysis on migrated cover environments
        if isSystemTest and not st_is_monitored:
            tcSkipped = False

        # If cbtDict is None, no build log was passed in...don't mark anything as skipped
        elif self.cbtDict == None:
            tcSkipped = False

        # else there is something check , if the length of cbtDict is greater than zero
        elif len(self.cbtDict) > 0:
            tcSkipped, start_tdo, end_tdo = self.was_test_case_skipped(tc,"/".join([unit_name, func_name, tc.name]),isSystemTest)

        # finally - there was nothing to check
        else:
            tcSkipped = False

        if end_tdo:
            deltaTimeStr = str((end_tdo - start_tdo).total_seconds())
        else:
            deltaTimeStr = "0.0"

        tc_name = tc.name
        compiler = self.compiler.replace(".", "")
        testsuite = self.testsuite.replace(".", "")
        envName = self.env.replace(".", "")

        classname = compiler + "." + testsuite + "." + envName

        if isSystemTest:
            tc_name_full =  classname + "." + tc_name
            exp_total = tc.total
            exp_pass = tc.passed
            result = "  System Test Build Status: " + tc.build_status + ". \n   System Test: " + tc.name + " \n   Execution Status: "
            if tc.run_needed and tc.type == 2: #SystemTestType.MANUAL:
                result += "Manual system tests can't be run in Jenkins"
                tc.passed = 1
            elif tc.run_needed:
                result += "Needs to be executed"
                tc.passed = 1
            elif tc.passed > 0 and tc.passed == tc.total:
                result += "Passed"
            else:
                result += "Failed {} / {} ".format(tc.passed, tc.total)
                tc.passed = 0

        else:
            tc_name_full =  unit_name + "." + func_name + "." + tc_name
            summary = tc.history.summary
            exp_total = summary.expected_total
            exp_pass = exp_total - summary.expected_fail
            if self.api.environment.get_option("VCAST_OLD_STYLE_MANAGEMENT_REPORT"):
                exp_pass += summary.control_flow_total - summary.control_flow_fail
                exp_total += summary.control_flow_total + summary.signals + summary.unexpected_exceptions

            result = self.__get_testcase_execution_results(
                tc,
                classname,
                tc_name_full)

            exp_pass += summary.control_flow_total - summary.control_flow_fail
            exp_total += summary.control_flow_total + summary.signals + summary.unexpected_exceptions

            if tc.testcase_status == "TCR_STRICT_IMPORT_FAILED":
                result += "\nStrict Test Import Failure."

            # Failure takes priority
            if tc.status != "TC_EXECUTION_NONE":
                failure_message = failureReasons
            else:
                failure_message = didntRunReason

        case_status = None
        if tc.passed is None:
            case_status = "skipped"
            status = "Testcase may have been skipped by VectorCAST Change Based Testing. Last execution data shown.\n\nFAIL"
        elif not tc.passed:
            if tcSkipped:
                status = "Testcase may have been skipped by VectorCAST Change Based Testing. Last execution data shown.\n\nFAIL"
            else:
                status = "FAIL"
            case_status = "failure"
        elif tcSkipped:
            case_status = "skipped"
            status = "Skipped by VectorCAST Change Based Testing. Last execution data shown.\n\nPASS"
        else:
            status = "PASS"

        if self.use_cte or unitName == "":
            unitName = classname

        msg = "{} {} / {}  \n\nExecution Report:\n {}".format(
            status, exp_pass, exp_total, result).replace('"', '').replace('\r', '')
        self.junit_report.add_case(
            tc_name_full, unitName, time=deltaTimeStr, file=fpath,
            line=startLine, status=case_status,
            failure_message=failure_message, output=msg)

## GenerateXml

    def was_test_case_skipped(self, tc, searchName, isSystemTest):
        try:
            if isSystemTest:
                compoundTests, initTests,  simpleTestcases = self.cbtDict[self.hashCode]
                # use tc.name because system tests aren't for a specific unit/function
                if tc.name in simpleTestcases.keys():
                    return [False, simpleTestcases[tc.name][0], simpleTestcases[tc.name][1]]
                else:
                    self.__print_test_case_was_skipped(searchName, tc.passed)
                    return [True, None, None]
            else:
                #Failed import TCs don't get any indication in the build.log
                if tc.testcase_status == "TCR_STRICT_IMPORT_FAILED":
                    return [False, None, None]

                compoundTests, initTests,  simpleTestcases = self.cbtDict[self.hashCode]

                #Recursive Compound don't get any named indication in the build.log
                if tc.kind == TestCase.KINDS['compound'] and (tc.testcase_status == "TCR_RECURSIVE_COMPOUND" or searchName in compoundTests.keys()):
                    return [False, compoundTests[searchName][0], compoundTests[searchName][1]]
                elif tc.kind == TestCase.KINDS['init'] and searchName in initTests.keys():
                    return [False, initTests[searchName][0], initTests[searchName][1]]
                elif searchName in simpleTestcases.keys() or tc.testcase_status == "TCR_NO_EXPECTED_VALUES":
                    #print ("found" , self.hashCode, searchName, str( simpleTestcases[searchName][1] - simpleTestcases[searchName][0]))
                    return [False, simpleTestcases[searchName][0], simpleTestcases[searchName][1]]
                else:
                    self.__print_test_case_was_skipped(searchName, tc.passed)
                    return [True, None, None]
        except KeyError:
            self.__print_test_case_was_skipped(tc.name, tc.passed)
            return [True, None, None]
        except Exception as e:
            log_exception(self.compiler, self.testsuite, self.env, self.build_dir)
            if self.print_exc:
                import json
                print ("CBT Dictionary:\n{}".format(json.dumps(self.cbtDict, indent=2)))

## GenerateXml

    def __get_testcase_execution_results(self, tc, classname, tc_name):

            
        if not self.generate_exec_rpt_each_testcase:
            return "Execution Report disabled by using --dont-generate-individual-reports"

        report_name_hash =  '.'.join(
            ["execution_results", classname, tc_name])
        # Unicode-objects must be encoded before hashing in Python 3
        if sys.version_info[0] >= 3:
            report_name_hash = report_name_hash.encode(self.encFmt)

        report_name = hashlib.md5(report_name_hash).hexdigest()

        import time

        try:
            self.api.report(
                testcases=[tc],
                single_testcase=True,
                report_type="Demo",
                formats=["TEXT"],
                output_file=report_name,
                sections=[ "TESTCASE_SECTIONS"],
                testcase_sections=["EXECUTION_RESULTS"])

            with open(report_name, "rb") as fd:
                out = fd.read()

            try:
                # Prefer UTF-8 if possible
                out = out.decode("utf-8")
            except UnicodeDecodeError:
                # Fallback to system/default encoding (e.g. cp936 in CN) with replace
                out = out.decode(self.encFmt, errors="replace")

            os.remove(report_name)
        except:
            out = "No execution results found"
            log_exception(self.compiler, self.testsuite, self.env, self.build_dir)

        return out

## GenerateXml

    def __print_test_case_was_skipped(self, searchName, passed):
        if self.verbose:
            print("skipping {} {} {}".format(self.hashCode, searchName, passed))

def generate_environment_junit(xml_file, envPath, env, xmlTestingReportName, teePrint):
    if xml_file.api == None:
        teePrint.info("\nCannot find project file (.vcp or .vce): " + envPath + os.sep + env)

    else:
        xml_file.generate_unit()
        teePrint.info("\nJunit plugin for Jenkins compatible file generated: " + xmlTestingReportName)

if __name__ == '__main__':

    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('environment', help='VectorCAST environment name')
    parser.add_argument('-v', '--verbose', default=False, help='Enable verbose output', action="store_true")
    parser.add_argument('--ci', help='Use continuous integration licenses', action="store_true", default=False)
    args = parser.parse_args()

    envPath = os.path.dirname(os.path.abspath(args.environment))
    env = os.path.basename(args.environment)

    if env.endswith(".vcp"):
        env = env[:-4]

    if env.endswith(".vce"):
        env = env[:-4]

    xmlTestingReportName = "test_results_" + env + ".xml"

    teePrint = get_logger()
    xml_file = GenerateXml(env,
                           envPath,
                           env, "", "",
                           xmlTestingReportName,
                           args.verbose,
                           None,
                           teePrint)

    generate_environment_junit(
        xml_file,
        envPath,
        env,
        xmlTestingReportName,
        teePrint)
