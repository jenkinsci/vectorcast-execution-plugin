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

import os
from datetime import datetime
try:
    from html import escape
except ImportError:
    # html not standard module in Python 2.
    from cgi import escape
import sys
# Later version of VectorCAST have renamed to Unit Test API
# Try loading the newer (renamed) version first and fall back
# to the older.
try:
    from vector.apps.DataAPI.unit_test_api import UnitTestApi
    from vector.apps.DataAPI.unit_test_models import TestCase
except:
    from vector.apps.DataAPI.api import Api as UnitTestApi
    from vector.apps.DataAPI.models import TestCase

try:
    from vector.apps.DataAPI.vcproject_api import VCProjectApi
except:
    pass

from vector.enums import ENVIRONMENT_STATUS_TYPE_T

from vector.apps.DataAPI.cover_api import CoverApi
try:
    from vector.apps.ReportBuilder.custom_report import fmt_percent
except:
    from vcast_utils import fmt_percent
    pass
        
from operator import attrgetter
import hashlib 
import traceback
from pprint import pprint
try:
    from vector.apps.ReportBuilder.custom_report import CustomReport
except:
    pass

import re
from enum import Enum

def dummy(*args, **kwargs):
    return None
    
class TC_STATUS(Enum):
    NONE = 0
    PASS = 1
    FAIL = 2
    ERROR = 3
    SKIPPED = 4
    NOT_APPLICABLE = 5

xml_status = ["N/A","PASS","FAIL","ERROR","N/A","N/A"]

##########################################################################
# This class generates the XML (JUnit based) report for the overall
#
class BaseGenerateXml(object):
    def __init__(self, FullManageProjectName, verbose, xml_data_dir = "xml_data"):
        projectName = os.path.splitext(os.path.basename(FullManageProjectName))[0]
        self.manageProjectName = projectName
        self.xml_data_dir = xml_data_dir
        self.unit_report_name = os.path.join(self.xml_data_dir,"sonarqube","test_results_"+ self.manageProjectName + ".xml")
        self.verbose = verbose
        self.print_exc = False

        self.test_id = 1
        
        # get the VC langaguge and encoding
        self.encFmt = 'utf-8'
        from vector.apps.DataAPI.configuration import vcastqt_global_options
        self.lang = vcastqt_global_options.get('Translator','english')
        if self.lang == "english":
            self.encFmt = "utf-8"
        if self.lang == "japanese":
            self.encFmt = "shift-jis"
        if self.lang == "chinese":
            self.encFmt = "GBK"
            
        self.failDict = {}
        self.passDict = {}
        self.api = None
        
        try:
            prj_dir = os.environ['CI_PROJECT_DIR'].replace("\\","/") + "/"
        except:
            prj_dir = os.getcwd().replace("\\","/") + "/"
            
        self.workspace = prj_dir

    def dump(self,obj):
        if hasattr(obj, '__dict__'): 
            return vars(obj) 
        else:
            try:
                return {attr: getattr(obj, attr, None) for attr in obj.__slots__} 
            except:
                return str(obj)

    def convertTHStatus(self, status):
       
        convertDict = {'TEST_HISTORY_FAILURE_REASON_DATA_SKEW_UNDERFLOW':                    'Data Skew Underflow',
                       'TEST_HISTORY_FAILURE_REASON_DATA_SKEW_OVERFLOW':                     'Data Skew Overflow',
                       'TEST_HISTORY_FAILURE_REASON_HARNESS_FAILURE':                        'Harness Failure',
                       'TEST_HISTORY_FAILURE_REASON_THISTORY_FILE_DOES_NOT_EXIST':           'Thistory File Does Not Exist',
                       'TEST_HISTORY_FAILURE_REASON_INSUFFICIENT_HEAP_SIZE':                 'Insufficient Heap Size',
                       'TEST_HISTORY_FAILURE_REASON_LIBRARY_MALLOC_FAILED':                  'Library Malloc Failed',
                       'TEST_HISTORY_FAILURE_REASON_THISTORY_LINE_INVALID':                  'Thistory Line Invalid',
                       'TEST_HISTORY_FAILURE_REASON_THISTORY_ENDED_PREMATURELY':             'Thistory Ended Prematurely',
                       'TEST_HISTORY_FAILURE_REASON_EXPECTED_ENDED_PREMATURELY':             'Expected Ended Prematurely',
                       'TEST_HISTORY_FAILURE_REASON_HARNESS_COMMNAD_INVALID':                'Harness Commnad Invalid',
                       'TEST_HISTORY_FAILURE_REASON_TEST_HISTORY_OUTPUT_FILES_CONTAIN_ERROR':'Test History Output Files Contain Error',
                       'TEST_HISTORY_FAILURE_REASON_STRICT_IMPORT_FAILED':                   'Strict Import Failed',
                       'TEST_HISTORY_FAILURE_REASON_MACRO_NOT_FOUND':                        'Macro Not Found',
                       'TEST_HISTORY_FAILURE_REASON_SYMBOL_OR_MACRO_NOT_FOUND':              'Symbol Or Macro Not Found',
                       'TEST_HISTORY_FAILURE_REASON_SYMBOL_OR_MACRO_TYPE_MISMATCH':          'Symbol Or Macro Type Mismatch',
                       'TEST_HISTORY_FAILURE_REASON_EMPTY_TESTCASES':                        'Empty Testcases',
                       'TEST_HISTORY_FAILURE_REASON_NO_EXPECTED_RETURN':                     'No Expected Return',
                       'TEST_HISTORY_FAILURE_REASON_NO_EXPECTED_VALUES':                     'No Expected Values',
                       'TEST_HISTORY_FAILURE_REASON_EXECUTABLE_MISSING':                     'Executable Missing',
                       'TEST_HISTORY_FAILURE_REASON_MAX_VARY_EXCEEDED':                      'Max Vary Exceeded',
                       'TEST_HISTORY_FAILURE_REASON_TRUNCATED_HARNESS_DATA':                 'Truncated Harness Data',
                       'TEST_HISTORY_FAILURE_REASON_HARNESS_STDOUT_DATA_UNDERFLOW':          'Harness Stdout Data Underflow',
                       'TEST_HISTORY_FAILURE_REASON_MAX_STRING_LENGTH_EXCEEDED':             'Max String Length Exceeded',
                       'TEST_HISTORY_FAILURE_REASON_MAX_TARGET_FILES_EXCEEDED':              'Max Target Files Exceeded',
                       'TEST_HISTORY_FAILURE_TIMEOUT_EXCEEDED':                              'Timeout Exceeded' 
                       }

        try:                  
            s = convertDict[str(status)]
        except:
            s = convertDict[status]
        return s

    def convertExecStatusToEnum(self, status):
        convertDict = { 'EXEC_SUCCESS_PASS'                             : TC_STATUS.PASS,
                        'EXEC_SUCCESS_FAIL'                             : TC_STATUS.FAIL,
                        'EXEC_SUCCESS_NONE'                             : TC_STATUS.PASS,
                        'EXEC_EXECUTION_FAILED'                         : TC_STATUS.FAIL,
                        'EXEC_ABORTED'                                  : TC_STATUS.FAIL,
                        'EXEC_TIMEOUT_EXCEEDED'                         : TC_STATUS.FAIL,
                        'EXEC_VXWORKS_LOAD_ERROR'                       : TC_STATUS.ERROR,
                        'EXEC_USER_CODE_COMPILE_FAILED'                 : TC_STATUS.ERROR,
                        'EXEC_COMPOUND_ONLY'                            : TC_STATUS.ERROR,
                        'EXEC_STRICT_IMPORT_FAILED'                     : TC_STATUS.ERROR,
                        'EXEC_MACRO_NOT_FOUND'                          : TC_STATUS.ERROR,
                        'EXEC_SYMBOL_OR_MACRO_NOT_FOUND'                : TC_STATUS.ERROR,
                        'EXEC_SYMBOL_OR_MACRO_TYPE_MISMATCH'            : TC_STATUS.ERROR,
                        'EXEC_MAX_VARY_EXCEEDED'                        : TC_STATUS.ERROR,
                        'EXEC_COMPOUND_WITH_NO_SLOTS'                   : TC_STATUS.ERROR,
                        'EXEC_COMPOUND_WITH_ZERO_ITERATIONS'            : TC_STATUS.ERROR,
                        'EXEC_STRING_LENGTH_EXCEEDED'                   : TC_STATUS.ERROR,
                        'EXEC_FILE_COUNT_EXCEEDED'                      : TC_STATUS.ERROR,
                        'EXEC_EMPTY_TESTCASE'                           : TC_STATUS.ERROR,
                        'EXEC_NO_EXPECTED_RETURN'                       : TC_STATUS.FAIL,
                        'EXEC_NO_EXPECTED_VALUES'                       : TC_STATUS.FAIL,
                        'EXEC_CSV_MAP'                                  : TC_STATUS.NOT_APPLICABLE,
                        'EXEC_DRIVER_DATA_COMPILE_FAILED'               : TC_STATUS.ERROR,
                        'EXEC_RECURSIVE_COMPOUND'                       : TC_STATUS.ERROR,
                        'EXEC_SPECIALIZED_COMPOUND_CONTAINING_COMMON'   : TC_STATUS.ERROR,
                        'EXEC_COMMON_COMPOUND_CONTAINING_SPECIALIZED'   : TC_STATUS.ERROR,
                        'EXEC_HIDING_EXPECTED_RESULTS'                  : TC_STATUS.ERROR,
                        'INVALID_TEST_CASE'                             : TC_STATUS.ERROR,
                       }
        try:                  
            s = convertDict[str(status)]
        except:
            s = convertDict[status]
        return s

    def convertExecStatusToStr(self, status):
        convertDict = { 'EXEC_SUCCESS_PASS':'Testcase passed',
                        'EXEC_SUCCESS_FAIL':'Testcase failed on expected values',
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

#
# BaseGenerateXml - generate the formatted timestamp to write to the coverage file
#
    def get_timestamp(self):
        dt = datetime.now()
        hour = dt.hour
        if hour > 12:
            hour -= 12
        return dt.strftime('%d %b %Y  @HR@:%M:%S %p').upper().replace('@HR@', str(hour))



##########################################################################
# This class generates the XML (JUnit based) report for the overall
#
class GenerateManageXml (BaseGenerateXml):

# GenerateManageXml

    def __init__(self, FullManageProjectName, verbose = False, 
                       cbtDict = None, 
                       generate_exec_rpt_each_testcase = True,
                       skipReportsForSkippedEnvs = False,
                       report_failed_only = False,
                       no_full_reports = False,
                       print_exc = False,
                       xml_data_dir = "xml_data"):
                   
        super(GenerateManageXml, self).__init__(FullManageProjectName, verbose, xml_data_dir)
        
        self.api = VCProjectApi(FullManageProjectName)

        self.FullManageProjectName = FullManageProjectName        
        self.generate_exec_rpt_each_testcase = generate_exec_rpt_each_testcase
        self.skipReportsForSkippedEnvs = skipReportsForSkippedEnvs
        self.report_failed_only = report_failed_only
        self.cbtDict = cbtDict
        self.no_full_reports = no_full_reports
        self.error_count = 0
        self.failed_count = 0
        self.error_count = 0
        self.passed_count = 0
        self.print_exc = print_exc
        
        self.fh_data = ""

        self.cleanupXmlDataDir()

    def cleanupXmlDataDir(self):
        path=os.path.join(self.xml_data_dir,"sonarqube")
        import glob
        # if the path exists, try to delete all file in it
        if os.path.isdir(path):
            for file in glob.glob(path + "/*.*"):
                try:
                    os.remove(file);
                except:
                    print("   *INFO: File System Error removing file after failed to remove directory: " + path + "/" + file + ".  Check console for environment build/execution errors")
                    if print_exc:  traceback.print_exc()

        # we should either have an empty directory or no directory
        else:
            try:
                os.makedirs(path)
            except:
                print("failed making path: " + path)
                print("   *INFO: File System Error creating directory: " + path + ".  Check console for environment build/execution errors")
                if print_exc:  traceback.print_exc()
                
    def __del__(self):
        try:
            self.api.close()
        except:
            pass
        
    def generate_local_results(self, results, key):
        # get the level from the name

        if len(key.split("/")) != 3:
            comp, ts, group, env_name = key.split("/")
        else:
            comp, ts, env_name = key.split("/")
            
        env_key = comp + "/" + ts + "/" + env_name
        
        env = self.api.project.environments[env_key]
        env_def = self.api.project.environments[env_key].definition
    
        build_dir = os.path.join(self.api.project.workspace,env.relative_working_directory)
        vceFile =  os.path.join(build_dir, env.name+".vce")
        
        xmlUnitReportName = os.path.join(self.xml_data_dir, "sonarqube", "test_results_" + key.replace("/","_") + ".xml")

        localXML = GenerateXml(self.FullManageProjectName, build_dir, env_name, comp, ts, 
                               key, xmlUnitReportName, None, None, False, 
                               self.cbtDict, 
                               self.generate_exec_rpt_each_testcase, 
                               self.skipReportsForSkippedEnvs, 
                               self.report_failed_only,
                               self.print_exc,
                               xml_data_dir = self.xml_data_dir)
                               
        localXML.topLevelAPI = self.api
        localXML.failDict = self.failDict
        localXML.passDict = self.passDict
        localXML.failed_count = self.failed_count
        localXML.error_count = self.error_count
        localXML.passed_count = self.passed_count
        localXML.test_id = self.test_id
        localXML.generate_unit()
        self.failDict = localXML.failDict
        self.passDict = localXML.passDict
        self.failed_count = localXML.failed_count
        self.error_count = localXML.error_count
        self.passed_count = localXML.passed_count
        self.test_id = localXML.test_id
        del localXML
        
        
# GenerateManageXml
    def generate_testresults(self):
        results = self.api.project.repository.get_full_status([])
        all_envs = []
        for env in self.api.Environment.all():
            if not env.is_active:
                continue
            all_envs.append(env.level._full_path)
            
        if results['ALL']['testcase_results'] == {}:
            return
            
        self.localDataOnly = True
        for result in results:
            if result in all_envs:
                if len(result.split("/")) != 3:
                    comp, ts, group, env_name = result.split("/")
                else:
                    comp, ts, env_name = result.split("/")
                    env.level._full_path
                    
                env_to_investigate = self.api.Environment.filter(level___full_path__equals=result)
                
                try:
                    testPath = env_to_investigate[0].build_directory + "/" + env_to_investigate[0].name
                    if not os.path.exists(testPath):
                        print("SQ:    Skipping environment: "+ env_to_investigate[0].name)
                        print("SQ:        *Missing path for environment: " + env_to_investigate[0].name)
                        print("SQ:            Expected Path: " + testPath)
                        continue
                    if isinstance(env_to_investigate[0].api, UnitTestApi) and \
                            env_to_investigate[0].api.environment.status != ENVIRONMENT_STATUS_TYPE_T.NORMAL:
                        print("SQ:    Skipping environment: "+ env_to_investigate[0].name)
                        print("SQ:        *" + env_to_investigate[0].name + " status is not NORMAL")
                        continue 
                except:
                    print("    Skipping environment: "+ env_to_investigate[0].name)
                    print("        *Issue with DataAPI")
                    continue

                if isinstance(env_to_investigate[0].api, UnitTestApi):
                    uname = (env_to_investigate[0].api.TestCase.first().unit_display_name)
                    fname = (env_to_investigate[0].api.TestCase.first().function_display_name)
                    
                if results[result]['local'] != {}:
                    self.generate_local_results(results,result)                    
                else:
                    for key in results[result]['imported'].keys():
                        self.localDataOnly = False
                        importedResult = results[result]['imported'][key]
                        total   = importedResult['testcase_results']['total_count']
                        success = importedResult['testcase_results']['success_count']
                        failed  = total - success
                        importName = importedResult['name']
                        classname = "ImportedResults." + importName + "." + comp + "." + ts + "." + env_name
                        classname = comp + "." + ts + "." + env_name
                        for idx in range(1,success+1):                        
                            tc_name_full = "ImportedResults." + importName + ".TestCase.PASS.%03d" % idx

                            p1 = "        <Test id=\"%d\">\n"  % (self.test_id)
                            p2 = "          <Name>%s</Name>\n" % (tc_name_full)
                            p3 = "          <Time>0</Time>\n"
                            p4 = "        </Test>\n"
                                   
                            self.passDict[tc_name_full] = p1 + p2 + p3 + p4                            
                            self.passed_count += 1
                            self.test_id += 1

                        for idx in range(1,failed+1):
                            tc_name_full = "ImportedResults." + importName + ".TestCase.FAIL.%03d" % idx
                            
                            f1 = "        <FailedTest id=\"%d\">\n" % (self.test_id)
                            f2 = "          <Name>%s</Name>n"       % (tc_name_full)
                            f3 = "          <FailureType>Failure</FailureType>\n"
                            f4 = "          <Message/>\n"
                            f5 = "        </FailedTest>\n"
                                         
                            self.failDict[tc_name_full] = f1 + f2 + f4 + f4 + f5                            
                            self.failed_count += 1
                            self.test_id += 1

        self.write_cppunit_data()
        
            
            
    def write_cppunit_data(self):
    
        ### sort and output the fhdata here

        self.fh_data = "<?xml version=\"1.0\" encoding='ISO-8859-1' standalone='yes' ?>\n"
        self.fh_data += "<TestRun>\n"

        self.fh_data += "    <FailedTests>\n"
        for key in self.failDict:
            self.fh_data += self.failDict[key]
        self.fh_data += "    </FailedTests>\n"
        
        self.fh_data += "    <SuccessfulTests>\n"
        for key in self.passDict:
            self.fh_data += self.passDict[key]
        self.fh_data += "    </SuccessfulTests>\n"
        
        self.fh_data += "    <Statistics>\n" 
        self.fh_data += "        <Tests>%d</Tests>\n"                 % (self.passed_count  + self.failed_count + self.error_count) 
        self.fh_data += "        <FailuresTotal>%d</FailuresTotal>\n" % (self.error_count   + self.failed_count) 
        self.fh_data += "        <Errors>%d</Errors>\n"               % (self.error_count ) 
        self.fh_data += "        <Failures>%d</Failures>\n"           % (self.failed_count)  
        self.fh_data += "    </Statistics>\n"
        
        self.fh_data += "</TestRun>\n"
        
        with open(self.unit_report_name, "w") as fd:
            fd.write(self.fh_data)

        
            
##########################################################################
# This class generates the XML (Junit based) report for dynamic tests and
#
# In both cases these are for a single environment
#
class GenerateXml(BaseGenerateXml):

    def __init__(self, FullManageProjectName, build_dir, env, compiler, testsuite, jenkins_name, unit_report_name, jenkins_link, jobNameDotted, verbose = False, cbtDict= None, generate_exec_rpt_each_testcase = True, skipReportsForSkippedEnvs = False, report_failed_only = False, print_exc = False, xml_data_dir = "xml_data"):
        super(GenerateXml, self).__init__(FullManageProjectName, verbose, xml_data_dir = xml_data_dir)

        self.cbtDict = cbtDict
        self.FullManageProjectName = FullManageProjectName
        self.generate_exec_rpt_each_testcase = generate_exec_rpt_each_testcase
        self.skipReportsForSkippedEnvs = skipReportsForSkippedEnvs
        self.report_failed_only = report_failed_only
        self.print_exc = print_exc
        self.topLevelAPI = None
        
        ## use hash code instead of final directory name as regression scripts can have overlapping final directory names
        
        build_dir_4hash = build_dir.upper()
        build_dir_4hash = "/".join(build_dir_4hash.split("/")[-2:])
        
        # Unicode-objects must be encoded before hashing in Python 3
        if sys.version_info[0] >= 3:
            build_dir_4hash = build_dir_4hash.encode(self.encFmt)

        self.hashCode = hashlib.md5(build_dir_4hash).hexdigest()
        
        if verbose:
            print ("gen Dir: " + str(build_dir_4hash)+ " Hash: " +self.hashCode)

        #self.hashCode = build_dir.split("/")[-1].upper()
        self.build_dir = build_dir
        self.env = env
        self.compiler = compiler
        self.testsuite = testsuite
        self.jenkins_name = jenkins_name
        self.unit_report_name = os.path.join(self.xml_data_dir,"sonarqube","test_results_"+ self.manageProjectName + ".xml")
        self.jenkins_link = jenkins_link
        self.jobNameDotted = jobNameDotted
        cov_path = os.path.join(build_dir,env + '.vcp')
        unit_path = os.path.join(build_dir,env + '.vce')
        if os.path.exists(cov_path):
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
        self.test_id = 0
        

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

        if self.topLevelAPI == None:
            self.start_test_results_file()
        
        if isinstance(self.api, CoverApi):
            try:
                if self.topLevelAPI == None:
                    api = VCProjectApi(self.FullManageProjectName)
                else:
                    api = self.topLevelAPI
                        
                for env in api.Environment.all():
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
                            if self.verbose:
                                print (level, st.name, pass_fail_rerun)
                            self.write_testcase(st, level, st.name, env.definition.is_monitored)

                # callStr = os.getenv('VECTORCAST_DIR') + os.sep + "manage -p " + self.FullManageProjectName + " --system-tests-status=" + self.manageProjectName + "_system_tests_status.html"
                # p = subprocess.Popen(callStr, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
                # out, err = p.communicate()

                if self.topLevelAPI == None:
                    api.close()

            except ImportError as e:
                from generate_qa_results_xml import genQATestResults
                pc,fc = genQATestResults(self.FullManageProjectName, self.compiler+ "/" + self.testsuite, self.env, True, self.encFmt)
                self.failed_count += fc
                self.passed_count += pc
                self.test_id += pc + fc
                return

        else:
            try:
                self.add_compound_tests()
                self.add_init_tests()
                for unit in self.api.Unit.all():
                    if unit.is_uut:
                        for func in unit.functions:
                            if not func.is_non_testable_stub:
                                for tc in func.testcases:
                                    if not isTcPlaceHolder(tc):
                                        if not tc.for_compound_only or tc.testcase_status == "TCR_STRICT_IMPORT_FAILED":
                                            self.write_testcase(tc, tc.function.unit.name, tc.function.display_name)
                                            
            except AttributeError as e:
                traceback.print_exc()
                
        if self.topLevelAPI == None:
            self.end_test_results_file()
#
# GenerateXml - is this test cases a placeholder (CVS Map, vctMap, Coded test map, etc)
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
            
        # Placeholder "testcases" that need to be ignored
        if tc.is_csv_map or vctMap or vcCodedTestMap:   
            placeHolder = True
            
        return placeHolder 
#
# GenerateXml - write the end of the jUnit XML file and close it
#
    def start_test_results_file(self):
        self.fh_data = "<?xml version=\"1.0\" encoding='ISO-8859-1' standalone='yes' ?>\n"
        self.fh_data += "<TestRun>\n"

    def end_test_results_file(self):
        self.fh_data += "</TestRun>\n"
        
        with open(self.unit_report_name, "w") as fd:
            fd.write(self.fh_data)

#
# GenerateXml - start the JUnit XML file
#

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
    def write_testcase(self, tc, unit_name, func_name, st_is_monitored = False):

        ## <testExecutions version="1">
        ##   <file path="testx/ClassOneTest.xoo">
        ##     <testCase name="test1" duration="5"/>
        ##     <testCase name="test2" duration="500">
        ##       <skipped/>
        ##     </testCase>
        ##     <testCase name="test3" duration="100">
        ##       <failure/>
        ##     </testCase>
        ##     <testCase name="test4" duration="500">
        ##       <error/>
        ##     </testCase>
        ##   </file>
        ## </testExecutions>
        
        
        isSystemTest = False
        
        try:
            from vector.apps.DataAPI.manage_models import SystemTest
            if (isinstance(tc, SystemTest)):
                isSystemTest = True
        except:
            pass

        tc_time = "0"            
        
        if not isSystemTest and tc.function:           
            if tc.end_time and tc.start_time:
                tc_time = str(int((tc.end_time - tc.start_time).total_seconds()))
            
        failure_message = ""
        
        if self.report_failed_only and not self.testcase_failed(tc):
            return

        unit_name = escape(unit_name, quote=False)
        func_name = escape(func_name, quote=True)
        tc_name = escape(tc.name, quote=False)
        compiler = escape(self.compiler, quote=False).replace(".","")
        testsuite = escape(self.testsuite, quote=False).replace(".","")
        envName = escape(self.env, quote=False).replace(".","")
        
        tc_name_full =  unit_name + "." + func_name + "." + tc_name

        classname = compiler + "." + testsuite + "." + envName

        
        tcStatus = TC_STATUS.NONE
               
        idx = 1
        
        if isSystemTest:        
            if tc.run_needed and tc.type == 2: #SystemTestType.MANUAL:
                tcStatus = TC_STATUS.SKIPPED
            elif tc.run_needed:
                tcStatus = TC_STATUS.SKIPPED
            elif tc.passed == tc.total:
                tcStatus = TC_STATUS.PASS
            else:
                tcStatus = TC_STATUS.FAIL
        else:
            
            if tc.testcase_status != 'TCR_STATUS_OK':
                tcStatus = TC_STATUS.ERROR
        
        if tc.passed == None:
            tcStatus = TC_STATUS.SKIPPED
            
        elif tcStatus == TC_STATUS.NONE and not tc.passed:
            tcStatus = TC_STATUS.FAIL
            
        elif tcStatus != TC_STATUS.ERROR:
            tcStatus = TC_STATUS.PASS
        
        if tcStatus == TC_STATUS.PASS:
        
            id_name = classname + "." + tc_name
            p1 = "        <Test id=\"%d\">\n"  % (self.test_id)
            p2 = "          <Name>%s</Name>\n" % (id_name)
            p3 = "          <Time>%s</Time>\n" % (tc_time)
            p4 = "        </Test>\n"
            
            self.passDict[id_name] = p1 + p2 + p4 # + p3
            self.passed_count += 1
            
        elif tcStatus == TC_STATUS.FAIL:
            id_name = classname + "." + tc_name
            
            message = ""

            if tc.summary.expected_total > 0:
                pct = '%s' % (fmt_percent(tc.summary.expected_total-tc.summary.expected_fail, tc.summary.expected_total))                
                message += "Expected Results: " + pct + "% FAIL | "
                
            if tc.summary.control_flow_total > 0:
                pct = '%s' % (fmt_percent(tc.summary.control_flow_total - tc.summary.control_flow_fail, tc.summary.control_flow_total))
                message += "Control Flow: " + pct + "% FAIL | "
                                
            if tc.summary.signals > 0:
                message += "Signals : " + str(tc.summary.signals) + " FAIL | "
                
            if tc.summary.unexpected_exceptions > 0:
                message += "Unexpected Exceptions :" + str(tc.summary.unexpected_exceptions) + " FAIL | "
            if len(message) > 0: 
                message = message[:-3]
            f1 = "        <FailedTest id=\"%d\">\n" % (self.test_id) 
            f2 = "          <Name>%s</Name>\n"           % (id_name )
            f3 = "          <FailureType>Failed</FailureType>\n" 
            f4 = "          <Message>" + message + "</Message>\n"
            f5 = "        </FailedTest>\n" 
                        
            self.failDict[id_name] = f1 + f2 + f3 + f4 + f5
            self.failed_count += 1
            
        elif tcStatus == TC_STATUS.ERROR:
            id_name = classname + "." + tc_name
                      
            message = ""                      
            for fr in tc.failure_reasons:
                message += self.convertTHStatus(fr) + " | "
            if len(message) > 0: 
                message = message[:-3]
            f1 = "        <FailedTest id=\"%d\">\n" % (self.test_id) 
            f2 = "          <Name>%s</Name>\n"           % (id_name )
            f3 = "          <FailureType>Error</FailureType>\n" 
            f4 = "          <Message>" + message + "</Message>\n"
            f5 = "        </FailedTest>\n" 
                        
            self.failDict[id_name] = f1 + f2 + f3 + f4 + f5
            self.error_count += 1
            
        self.test_id += 1

def __generate_xml(xml_file, envPath, env, xmlCoverReportName, xmlTestingReportName):
    if xml_file.api == None:
        print ("\nCannot find project file (.vcp or .vce): " + envPath + "/" + env)
        
    else:
        xml_file.generate_unit()
        print ("\nJunit plugin for Jenkins compatible file generated: " + xmlTestingReportName)

def run(FullMP, xml_data_dir = "xml_data"):
    xml_file = GenerateManageXml(FullMP, xml_data_dir=xml_data_dir)
    xml_file.generate_testresults()
    del xml_file 

if __name__ == '__main__':

    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('--project', "-p", help='VectorCAST Project name', default=None)
    parser.add_argument('--environment', "-e", help='VectorCAST environment name', default=None)
    parser.add_argument('-v', '--verbose', default=False, help='Enable verbose output', action="store_true")
    args = parser.parse_args()
    
    if args.project:
        if not args.project.endswith(".vcm"):
            args.project += ".vcm"
            
        if not os.path.exists(args.project):
            print("Path to VectorCAST Project not found: ", args.project)
            sys.exit(-1)
            
        run(args.project)
                       
    if args.environment:
        envPath = os.path.dirname(os.path.abspath(args.environment))
        env = os.path.basename(args.environment)
        
        if env.endswith(".vcp"):
            env = env[:-4]
            
        if env.endswith(".vce"):
            env = env[:-4]
            
        jobNameDotted = env
        jenkins_name = env
        jenkins_link = env
        xmlTestingReportName = "test_results_" + env + ".xml"

        xml_file = GenerateXml(env,
                               envPath,
                               env, "", "", 
                               jenkins_name,
                               xmlTestingReportName,
                               jenkins_link,
                               jobNameDotted, 
                               args.verbose, 
                               None)

        __generate_xml(
            xml_file,
            envPath,
            env,
            xmlTestingReportName)
            
        
        del xml_file
