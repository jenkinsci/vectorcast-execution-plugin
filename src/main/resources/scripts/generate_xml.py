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

import os
from datetime import datetime
import cgi
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

from vector.apps.DataAPI.cover_api import CoverApi
from vector.apps.ReportBuilder.custom_report import fmt_percent
from operator import attrgetter
from vector.enums import COVERAGE_TYPE_TYPE_T
import hashlib 

def dummy(*args, **kwargs):
    return None

##########################################################################
# This class generates the XML (JUnit based) report for the overall
# (Emma based) report for Coverage
#
class BaseGenerateXml(object):
    def __init__(self, cover_report_name, verbose):
        self.cover_report_name = cover_report_name
        self.verbose = verbose
        self.using_cover = False

#
# Internal - calculate coverage value
#
    def calc_cov_values(self, x, y):
        column = ''
        if y == 0:
            column = None
        else:
            column = '%s%% (%d / %d)' % (fmt_percent(x, y), x, y)
        return column

#
# Internal - create coverage data object for given metrics entry
# for coverage report
#
    def add_coverage(self, is_unit, unit_or_func, metrics, cov_type):
        entry = {}
        entry["statement"] = None
        entry["branch"] = None
        entry["mcdc"] = None
        entry["basispath"] = None
        entry["function"] = None
        entry["functioncall"] = None

        if self.has_function_coverage:
            if is_unit:
                (total_funcs, funcs_covered) = unit_or_func.cover_data.functions_covered
                entry["function"] = self.calc_cov_values(funcs_covered, total_funcs)
            else:
                if unit_or_func.has_covered_objects:
                    entry["function"] = '100% (1 / 1)'
                else:
                    entry["function"] = '0% (0 / 1)'
                    
        if self.has_call_coverage:
            entry["functioncall"] = self.calc_cov_values(metrics.max_covered_function_calls, metrics.function_calls)
            
        if self.verbose:
            print("Coverage Type:", cov_type)

        if cov_type == None:
            return entry

        if "MC/DC" in cov_type:
            entry["branch"] = self.calc_cov_values(metrics.max_covered_mcdc_branches, metrics.mcdc_branches)
            if not self.simplified_mcdc:
                entry["mcdc"] = self.calc_cov_values(metrics.max_covered_mcdc_pairs, metrics.mcdc_pairs)
        if "Basis Paths" in cov_type:
            (cov,total) = unit_or_func.basis_paths_coverage
            entry["basis_path"] = self.calc_cov_values(cov, total)
        if "Statement" in cov_type:
            entry["statement"] = self.calc_cov_values(metrics.max_covered_statements, metrics.statements)
        if "Branch" in cov_type:
            entry["branch"] = self.calc_cov_values(metrics.max_covered_branches, metrics.branches)
        if "Function Call" in cov_type:
            entry["functioncall"] = self.calc_cov_values(metrics.max_covered_function_calls, metrics.function_calls)
                        
        return entry

#
# Internal - calculate 'grand total' coverage values for coverage report
#
    def grand_total_coverage(self, cov_type):
        entry = {}
        entry["statement"] = None
        entry["branch"] = None
        entry["mcdc"] = None
        entry["basispath"] = None
        entry["function"] = None
        entry["functioncall"] = None
        
        if self.has_function_coverage:
            entry["function"] = self.calc_cov_values(self.grand_total_max_covered_functions, self.grand_total_max_coverable_functions)
        if self.has_call_coverage:
            entry["functioncall"] = self.calc_cov_values(self.grand_total_max_covered_function_calls, self.grand_total_function_calls)           
        if cov_type == None:
            return entry
        if "MC/DC" in cov_type:
            entry["branch"] = self.calc_cov_values(self.grand_total_max_mcdc_covered_branches, self.grand_total_mcdc_branches)
            if not self.simplified_mcdc:
                entry["mcdc"] = self.calc_cov_values(self.grand_total_max_covered_mcdc_pairs, self.grand_total_mcdc_pairs)
        if "Basis Paths" in cov_type:
            entry["basis_path"] = self.calc_cov_values(self.grand_total_cov_basis_path, self.grand_total_total_basis_path)
        if "Statement" in cov_type:
            entry["statement"] = self.calc_cov_values(self.grand_total_max_covered_statements, self.grand_total_statements)
        if "Branch" in cov_type:
            entry["branch"] = self.calc_cov_values(self.grand_total_max_covered_branches, self.grand_total_branches)
        if "Function Call" in cov_type:
            entry["functioncall"] = self.calc_cov_values(self.grand_total_max_covered_function_calls, self.grand_total_function_calls)

        return entry

#
# Internal - generate the formatted timestamp to write to the coverage file
#
    def get_timestamp(self):
        dt = datetime.now()
        hour = dt.hour
        if hour > 12:
            hour -= 12
        return dt.strftime('%d %b %Y  @HR@:%M:%S %p').upper().replace('@HR@', str(hour))

#
# Internal - start writing to the coverage file
#
    def start_cov_file(self):
        if self.verbose:
            print("  Writing coverage xml file:        {}".format(self.cover_report_name))
        self.fh = open(self.cover_report_name, "w")
        self.fh.write('<!-- VectorCAST/Jenkins Integration, Generated %s -->\n' % self.get_timestamp())
        self.fh.write('<report>\n')
        self.fh.write('  <version value="3"/>\n')

#
# Internal - write the end of the coverage file and close it
#
    def end_cov_file(self):
        self.fh.write('</report>')
        self.fh.close()

#
# Generate the XML Modified 'Emma' coverage data
#
    def _generate_cover(self, cov_type):
        self.num_functions = 0

        self.simplified_mcdc = self.api.environment.get_option("VCAST_SIMPLIFIED_CONDITION_COVERAGE")
        self.our_units = []
        self.has_call_coverage = False
        self.has_function_coverage = False
        self.grand_total_complexity = 0

        self.grand_total_max_covered_branches = 0
        self.grand_total_branches = 0
        self.grand_total_max_covered_statements = 0
        self.grand_total_statements = 0
        self.grand_total_max_mcdc_covered_branches = 0
        self.grand_total_mcdc_branches = 0
        self.grand_total_max_covered_mcdc_pairs = 0
        self.grand_total_mcdc_pairs = 0
        self.grand_total_max_covered_function_calls = 0
        self.grand_total_function_calls = 0
        self.grand_total_max_covered_functions = 0
        self.grand_total_max_coverable_functions = 0
        self.grand_total_total_basis_path = 0
        self.grand_total_cov_basis_path = 0
        for unit in self.units:
            if not unit.unit_of_interest:
                if unit.coverage_type == COVERAGE_TYPE_TYPE_T.NONE:
                    continue
            if self.using_cover and not unit.is_instrumented:
                continue
            cover_file = unit.cover_data
            if cover_file is None or cover_file.lis_file is None:
                continue
            if self.using_cover:
                cov_type = cover_file.coverage_type_text
            try:
                if cover_file.coverage_type in (COVERAGE_TYPE_TYPE_T.FUNCTION_FUNCTION_CALL, COVERAGE_TYPE_TYPE_T.FUNCTION_COVERAGE):
                    self.has_function_coverage = True
            except Exception as e:
                self.has_function_coverage = self.api.environment.get_option("VCAST_DISPLAY_FUNCTION_COVERAGE")

            # 2019 SP1 and above until Sam changes it again :P
            try:
                if cover_file.coverage_type == COVERAGE_TYPE_TYPE_T.FUNCTION_FUNCTION_CALL:
                    self.has_call_coverage = True
            except:
                if cover_file.has_call_coverage:
                    self.has_call_coverage = True

            entry = {}
            entry["unit"] = unit
            entry["functions"] = []
            entry["complexity"] = 0
            entry["coverage"] = self.add_coverage(True, unit, unit.cover_metrics, cov_type)
            functions_added = False
            funcs_with_cover_data = []
            for func in unit.all_functions:
                if func.cover_data.has_coverage_data:
                    functions_added = True
                    funcs_with_cover_data.append(func)
            if self.using_cover:
                sorted_funcs = sorted(funcs_with_cover_data,key=attrgetter('cover_data.index'))
            else:
                sorted_funcs = sorted(funcs_with_cover_data,key=attrgetter('cover_data.id'))
            for func in sorted_funcs:
                cover_function = func.cover_data
                functions_added = True
                complexity = cover_function.complexity
                if complexity >= 0:
                    entry["complexity"] += complexity
                    self.grand_total_complexity += complexity
                func_entry = {}
                func_entry["func"] = func
                func_entry["complexity"] = func.cover_data.complexity
                func_entry["coverage"] = self.add_coverage(False, func, func.cover_data.metrics, cov_type)
                self.num_functions += 1
                entry["functions"].append(func_entry)
            if functions_added:
                self.our_units.append(entry)

            metrics = unit.cover_metrics

            self.grand_total_max_covered_branches += metrics.max_covered_branches
            self.grand_total_branches += metrics.branches
            self.grand_total_max_covered_statements += metrics.max_covered_statements
            self.grand_total_statements += metrics.statements
            self.grand_total_max_mcdc_covered_branches += metrics.max_covered_mcdc_branches
            self.grand_total_mcdc_branches += metrics.mcdc_branches
            self.grand_total_max_covered_mcdc_pairs += metrics.max_covered_mcdc_pairs
            self.grand_total_mcdc_pairs += metrics.mcdc_pairs
            self.grand_total_max_covered_function_calls += metrics.max_covered_function_calls
            self.grand_total_function_calls += metrics.function_calls
            (total_funcs, funcs_covered) = cover_file.functions_covered
            self.grand_total_max_covered_functions += funcs_covered
            self.grand_total_max_coverable_functions += total_funcs

            if cov_type == "Basis Paths":
                (cov, total) = unit.basis_paths_coverage
                self.grand_total_total_basis_path += total
                self.grand_total_cov_basis_path += cov

        self.coverage = self.grand_total_coverage(cov_type)
        self.num_units = len(self.our_units)

##########################################################################
# This class generates the XML (JUnit based) report for the overall
# (Emma based) report for Coverage
#
class GenerateManageXml(BaseGenerateXml):
    def __init__(self, cover_report_name, verbose, manage_path):
        super(GenerateManageXml, self).__init__(cover_report_name, verbose)
        self.using_cover = True
        from vector.apps.DataAPI.manage_api import ManageApi

        self.api = ManageApi(manage_path)

    def write_coverage_data(self):
        self.fh.write('  <combined-coverage type="complexity, %%" value="0%% (%s / 0)"/>\n' % self.grand_total_complexity)
        if self.coverage["statement"]:
            self.fh.write('  <combined-coverage type="statement, %%" value="%s"/>\n' % self.coverage["statement"])
        if self.coverage["branch"]:
            self.fh.write('  <combined-coverage type="branch, %%" value="%s"/>\n' % self.coverage["branch"])
        if self.coverage["mcdc"]:
            self.fh.write('  <combined-coverage type="mcdc, %%" value="%s"/>\n' % self.coverage["mcdc"])
        if self.coverage["basispath"]:
            self.fh.write('  <combined-coverage type="basispath, %%" value="%s"/>\n' % self.coverage["basispath"])
        if self.coverage["function"]:
            self.fh.write('  <combined-coverage type="function, %%" value="%s"/>\n' % self.coverage["function"])
        if self.coverage["functioncall"]:
            self.fh.write('  <combined-coverage type="functioncall, %%" value="%s"/>\n' % self.coverage["functioncall"])

    def generate_cover(self):
        self.units = self.api.project.cover_api.File.all()
        self._generate_cover(None)
        self.start_cov_file()
        self.write_coverage_data()
        self.end_cov_file()
        self.api.close()

##########################################################################
# This class generates the XML (Junit based) report for dynamic tests and
# the XML (Emma based) report for Coverage results
#
# In both cases these are for a single environment
#
class GenerateXml(BaseGenerateXml):

    def __init__(self, FullManageProjectName, build_dir, env, compiler, testsuite, cover_report_name, jenkins_name, unit_report_name, jenkins_link, jobNameDotted, verbose = False, cbtDict= None):
        super(GenerateXml, self).__init__(cover_report_name, verbose)

        self.cbtDict = cbtDict
        self.FullManageProjectName = FullManageProjectName
        
        ## use hash code instead of final directory name as regression scripts can have overlapping final directory names
        
        build_dir_4hash = build_dir.upper()
        # Unicode-objects must be encoded before hashing in Python 3
        if sys.version_info[0] >= 3:
            build_dir_4hash = build_dir_4hash.encode('utf-8')

        self.hashCode = hashlib.md5(build_dir_4hash).hexdigest()
        if verbose:
            print ("Dir: " + build_dir_4hash+ " Hash: " +self.hashCode)

        #self.hashCode = build_dir.split("/")[-1].upper()
        self.build_dir = build_dir
        self.env = env
        self.compiler = compiler
        self.testsuite = testsuite
        self.cover_report_name = cover_report_name
        self.jenkins_name = jenkins_name
        self.unit_report_name = unit_report_name
        self.jenkins_link = jenkins_link
        self.jobNameDotted = jobNameDotted
        self.using_cover = False
        cov_path = os.path.join(build_dir,env + '.vcp')
        unit_path = os.path.join(build_dir,env + '.vce')
        if os.path.exists(cov_path):
            self.using_cover = True
            self.api = CoverApi(cov_path)
        elif os.path.exists(unit_path):
            self.using_cover = False
            self.api = UnitTestApi(unit_path)
        else:
            self.api = None
            if verbose:
                print("Error: Could not determine project type for {}/{}".format(build_dir, env))
            return

        self.api.commit = dummy
        self.failed_count = 0

#
# Internal - add any compound tests to the unit report
#
    def add_compound_tests(self):
        for tc in self.api.TestCase.all():
            if tc.kind == TestCase.KINDS['compound']:
                if not tc.for_compound_only:
                    self.write_testcase(tc, "<<COMPOUND>>", "<<COMPOUND>>")

#
# Internal - add any intialisation tests to the unit report
#
    def add_init_tests(self):
        for tc in self.api.TestCase.all():
            if tc.kind == TestCase.KINDS['init']:
                if not tc.for_compound_only:
                    self.write_testcase(tc, "<<INIT>>", "<<INIT>>")

#
# Find the test case file
#
    def generate_unit(self):
        
        if isinstance(self.api, CoverApi):
            try:
                from vector.apps.DataAPI.vcproject_api import VCProjectApi
                self.start_system_test_file()
                api = VCProjectApi(self.FullManageProjectName)
                
                for env in api.Environment.all():
                    if env.compiler.name == self.compiler and env.testsuite.name == self.testsuite and env.name == self.env and env.system_tests:
                        for st in env.system_tests:
                            #pprint(vars(st))
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
                            self.write_testcase(st, level, st.name)
                api.close()

            except ImportError as e:
                from generate_qa_results_xml import genQATestResults
                genQATestResults(self.FullManageProjectName, self.compiler+ "/" + self.testsuite, self.env, True)
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
                                    try:
                                        vctMap = tc.is_vct_map
                                    except:
                                        vctMap = False
                                    if not tc.is_csv_map and not vctMap:
                                        if not tc.for_compound_only or tc.testcase_status == "TCR_STRICT_IMPORT_FAILED":
                                            self.write_testcase(tc, tc.function.unit.name, tc.function.display_name)

            except AttributeError as e:
                import traceback
                traceback.print_exc()
                pass
                
        self.end_test_results_file()
#
# Internal - start the JUnit XML file
#
    def start_system_test_file(self):
        if self.verbose:
            print("  Writing testcase xml file:        {}".format(self.unit_report_name))

        self.fh = open(self.unit_report_name, "w")
        errors = 0
        failed = 0
        success = 0                                            
        
        from vector.apps.DataAPI.vcproject_api import VCProjectApi 
        api = VCProjectApi(self.FullManageProjectName)
        
        for env in api.Environment.all():
            if env.compiler.name == self.compiler and env.testsuite.name == self.testsuite and env.name == self.env and env.system_tests:
                for st in env.system_tests:
                    if st.passed == st.total:
                        success += 1
                    else:
                        failed += 1
                        errors += 1  
                        self.failed_count += 1
        api.close()            
		
        self.fh.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        self.fh.write("<testsuites>\n")
        self.fh.write("    <testsuite errors=\"%d\" tests=\"%d\" failures=\"%d\" name=\"%s\" id=\"1\">\n" %
            (errors,success+failed+errors, failed, cgi.escape(self.env)))
                
    def start_unit_test_file(self):
        if self.verbose:
            print("  Writing testcase xml file:        {}".format(self.unit_report_name))

        self.fh = open(self.unit_report_name, "w")
        errors = 0
        failed = 0
        success = 0                                            
        
        for tc in self.api.TestCase.all():
            try:
                vctMap = tc.is_vct_map
            except:
                vctMap = False
        
            if (not tc.for_compound_only or tc.testcase_status == "TCR_STRICT_IMPORT_FAILED") and not tc.is_csv_map and not vctMap:
                if not tc.passed:
                    self.failed_count += 1
                    failed += 1
                    if tc.execution_status != "EXEC_SUCCESS_FAIL ":
                        errors += 1
                else:
                    success += 1
                    
        self.fh.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        self.fh.write("<testsuites>\n")
        self.fh.write("    <testsuite errors=\"%d\" tests=\"%d\" failures=\"%d\" name=\"%s\" id=\"1\">\n" %
            (errors,success+failed+errors, failed, cgi.escape(self.env)))

#
# Internal - write a testcase to the jUnit XML file
#
    def write_testcase(self, tc, unit_name, func_name):
    
        isSystemTest = False
        
        try:
            from vector.apps.DataAPI.manage_models import SystemTest
            if (isinstance(tc, SystemTest)):
                isSystemTest = True
        except:
            pass

        start_tdo = datetime.now()
        end_tdo   = None
        # If cbtDict is None, no build log was passed in...don't mark anything as skipped 
        if self.cbtDict == None:
            tcSkipped = False 
            
        # else there is something check , if the length of cbtDict is greater than zero
        elif len(self.cbtDict) > 0:
            tcSkipped, start_tdo, end_tdo = self.was_test_case_skipped(tc,"/".join([unit_name, func_name, tc.name]),isSystemTest)
            
        # finally - there was something to check, but it was empty
        else:
            tcSkipped = True
         
        if end_tdo:
            deltaTimeStr = str((end_tdo - start_tdo).total_seconds())
        else:
            deltaTimeStr = "0.0"

        testcaseString ="""
        <testcase name="%s" classname="%s" time="%s">
            %s
            <system-out>
%s                     
            </system-out>
        </testcase>
"""
        unit_name = cgi.escape(unit_name)
        func_name = cgi.escape(func_name).replace("\"","&quot;")
        tc_name = cgi.escape(tc.name)
        compiler = cgi.escape(self.compiler).replace(".","")
        testsuite = cgi.escape(self.testsuite).replace(".","")
        envName = cgi.escape(self.env).replace(".","")
        
        tc_name_full =  unit_name + "." + func_name + "." + tc_name

        classname = compiler + "." + testsuite + "." + envName

        if isSystemTest:        
            exp_total = tc.total
            exp_pass = tc.passed
            result = "  System Test Build Status: " + tc.build_status + ". \n   System Test: " + tc.name + " \n   Execution Status: "
            if tc.run_needed and tc.type == 2: #SystemTestType.MANUAL:
                result += "Manual system tests can't be run in Jenkins"
                tc.passed = 1
            elif tc.run_needed:
                result += "Needs to be executed"
                tc.passed = 1
            elif tc.passed == tc.total:
                result += "Passed"
            else:
                result += "Failed {} / {} ".format(tc.passed, tc.total)
                tc.passed = 0
                
        else:
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
                       
            if tc.testcase_status == "TCR_STRICT_IMPORT_FAILED":
                result += "\nStrict Test Import Failure."
    
        # Failure takes priority  
        if not tc.passed:
            if tcSkipped: 
                status = "Testcase may have been skipped by VectorCAST Change Based Testing.  Last execution data shown.\n\nFAIL"
            else:
                status = "FAIL"
            extraStatus = "\n            <failure type=\"failure\"/>\n"
        elif tcSkipped:
            status = "Skipped by VectorCAST Change Based Testing.  Last execution data shown.\n\nPASS"
            extraStatus = "\n            <skipped/>\n"
        else:
            status = "PASS"
            extraStatus = ""

        msg = "{} {} / {}  \n\nExecution Report:\n {}".format(status, exp_pass, exp_total, result)
        
        msg = cgi.escape(msg)
        msg = msg.replace("\"","")
        msg = msg.replace("\n","&#xA;")
        
        self.fh.write(testcaseString % (tc_name_full, classname, deltaTimeStr, extraStatus, msg))

#
# Internal - write the end of the jUnit XML file and close it
#
    def end_test_results_file(self):
        self.fh.write("   </testsuite>\n")
        self.fh.write("</testsuites>\n")
        self.fh.close()

#
# Internal - write the start of the coverage file for and environment
#
    def start_cov_file_environment(self):
        self.start_cov_file()
        self.fh.write('  <stats>\n')
        self.fh.write('    <environments value="1"/>\n')
        self.fh.write('    <units value="%d"/>\n' % self.num_units)
        self.fh.write('    <subprograms value="%d"/>\n' % self.num_functions)
        self.fh.write('  </stats>\n')
        self.fh.write('  <data>\n')
        self.fh.write('    <all name="all environments">\n')
        if self.coverage["statement"]:
            self.fh.write('      <coverage type="statement, %%" value="%s"/>\n' % self.coverage["statement"])
        if self.coverage["branch"]:
            self.fh.write('      <coverage type="branch, %%" value="%s"/>\n' % self.coverage["branch"])
        if self.coverage["mcdc"]:
            self.fh.write('      <coverage type="mcdc, %%" value="%s"/>\n' % self.coverage["mcdc"])
        if self.coverage["basispath"]:
            self.fh.write('      <coverage type="basispath, %%" value="%s"/>\n' % self.coverage["basispath"])
        if self.coverage["function"]:
            self.fh.write('      <coverage type="function, %%" value="%s"/>\n' % self.coverage["function"])
        if self.coverage["functioncall"]:
            self.fh.write('      <coverage type="functioncall, %%" value="%s"/>\n' % self.coverage["functioncall"])
        self.fh.write('      <coverage type="complexity, %%" value="0%% (%s / 0)"/>\n' % self.grand_total_complexity)
        self.fh.write('\n')

        self.fh.write('      <environment name="%s">\n' % cgi.escape(self.jenkins_name))
        if self.coverage["statement"]:
            self.fh.write('        <coverage type="statement, %%" value="%s"/>\n' % self.coverage["statement"])
        if self.coverage["branch"]:
            self.fh.write('        <coverage type="branch, %%" value="%s"/>\n' % self.coverage["branch"])
        if self.coverage["mcdc"]:
            self.fh.write('        <coverage type="mcdc, %%" value="%s"/>\n' % self.coverage["mcdc"])
        if self.coverage["basispath"]:
            self.fh.write('        <coverage type="basispath, %%" value="%s"/>\n' % self.coverage["basispath"])
        if self.coverage["function"]:
            self.fh.write('        <coverage type="function, %%" value="%s"/>\n' % self.coverage["function"])
        if self.coverage["functioncall"]:
            self.fh.write('        <coverage type="functioncall, %%" value="%s"/>\n' % self.coverage["functioncall"])
        self.fh.write('        <coverage type="complexity, %%" value="0%% (%s / 0)"/>\n' % self.grand_total_complexity)
        self.fh.write('\n')

#
# Internal - write the end of the coverage file and close it
#
    def end_cov_file_environment(self):
        self.fh.write('      </environment>\n')
        self.fh.write('    </all>\n')
        self.fh.write('  </data>\n')
        self.end_cov_file()

#
# Internal - write the units to the coverage file
#
    def write_cov_units(self):
        for unit in self.our_units:
            self.fh.write('        <unit name="%s">\n' % cgi.escape(unit["unit"].name))
            if unit["coverage"]["statement"]:
                self.fh.write('          <coverage type="statement, %%" value="%s"/>\n' % unit["coverage"]["statement"])
            if unit["coverage"]["branch"]:
                self.fh.write('          <coverage type="branch, %%" value="%s"/>\n' % unit["coverage"]["branch"])
            if unit["coverage"]["mcdc"]:
                self.fh.write('          <coverage type="mcdc, %%" value="%s"/>\n' % unit["coverage"]["mcdc"])
            if unit["coverage"]["basispath"]:
                self.fh.write('          <coverage type="basispath, %%" value="%s"/>\n' % unit["coverage"]["basispath"])
            if unit["coverage"]["function"]:
                self.fh.write('          <coverage type="function, %%" value="%s"/>\n' % unit["coverage"]["function"])
            if unit["coverage"]["functioncall"]:
                self.fh.write('          <coverage type="functioncall, %%" value="%s"/>\n' % unit["coverage"]["functioncall"])
            self.fh.write('          <coverage type="complexity, %%" value="0%% (%s / 0)"/>\n' % unit["complexity"])

            for func in unit["functions"]:
                if self.using_cover:
                    func_name = cgi.escape(func["func"].name).replace("\"","&quot;")
                    self.fh.write('          <subprogram name="%s">\n' % func_name)
                else:
                    func_name = cgi.escape(func["func"].display_name).replace("\"","&quot;")
                    self.fh.write('          <subprogram name="%s">\n' % func_name)
                if func["coverage"]["statement"]:
                    self.fh.write('            <coverage type="statement, %%" value="%s"/>\n' % func["coverage"]["statement"])
                if func["coverage"]["branch"]:
                    self.fh.write('            <coverage type="branch, %%" value="%s"/>\n' % func["coverage"]["branch"])
                if func["coverage"]["mcdc"]:
                    self.fh.write('            <coverage type="mcdc, %%" value="%s"/>\n' % func["coverage"]["mcdc"])
                if func["coverage"]["basispath"]:
                    self.fh.write('            <coverage type="basispath, %%" value="%s"/>\n' % func["coverage"]["basispath"])
                if func["coverage"]["function"]:
                    self.fh.write('            <coverage type="function, %%" value="%s"/>\n' % func["coverage"]["function"])
                if func["coverage"]["functioncall"]:
                    self.fh.write('            <coverage type="functioncall, %%" value="%s"/>\n' % func["coverage"]["functioncall"])
                self.fh.write('            <coverage type="complexity, %%" value="0%% (%s / 0)"/>\n' % func["complexity"])

                self.fh.write('          </subprogram>\n')
            self.fh.write('        </unit>\n')

#
# Generate the XML Modified 'Emma' coverage data
#
    def generate_cover(self):
        self.units = []
        if self.using_cover:
            self.units = self.api.File.all()
            self.units.sort(key=lambda x: (x.coverage_type, x.unit_index))
        else:
            self.units = self.api.Unit.all()
            
        # unbuilt (re: Error) Ada environments causing a crash
        try:
            cov_type = self.api.environment.coverage_type_text
        except Exception as e:
            print("Couldn't access coverage information...skipping.  Check console for environment build/execution errors")
            return
            
        self._generate_cover(cov_type)

        self.start_cov_file_environment()
        self.write_cov_units()
        self.end_cov_file_environment()

    def was_test_case_skipped(self, tc, searchName, isSystemTest):
        import sys, traceback, pprint
        try:
            if isSystemTest:
                compoundTests, initTests,  simpleTestcases = self.cbtDict[self.hashCode]
				# use tc.name because system tests aren't for a specific unit/function
                if searchName in simpleTestcases.keys():
                    return [False, simpleTestcases[searchName][0], simpleTestcases[searchName][1]]
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
            self.__print_test_case_was_skipped(searchName, tc.passed)
            return [True, None, None]
        except Exception as e: 
            pprint.pprint (self.cbtDict, width = 132)
            traceback.print_exc()
            sys.exit()

    def __get_testcase_execution_results(self, tc, classname, tc_name):
        report_name_hash =  '.'.join(
            ["execution_results", classname, tc_name])
        # Unicode-objects must be encoded before hashing in Python 3
        if sys.version_info[0] >= 3:
            report_name_hash = report_name_hash.encode('utf-8')

        report_name = hashlib.md5(report_name_hash).hexdigest()

        self.api.report(
            testcases=[tc],
            single_testcase=True,
            report_type="Demo",
            formats=["TEXT"],
            output_file=report_name,
            sections=[ "TESTCASE_SECTIONS"],
            testcase_sections=["EXECUTION_RESULTS"])

        with open(report_name,"r") as f:
            out = f.read()

        os.remove(report_name)

        return out

    def __print_test_case_was_skipped(self, searchName, passed):
        if self.verbose:
            print("skipping ", self.hashCode, searchName, passed)

if __name__ == '__main__':

    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('environment', help='VectorCAST environment name')
    parser.add_argument('-v', '--verbose', default=False, help='Enable verbose output', action="store_true")
    args = parser.parse_args()
    
    envPath = os.path.dirname(os.path.abspath(args.environment))
    env = os.path.basename(args.environment)
    
    if env.endswith(".vcp"):
        env = env[:-4]
        
    if env.endswith(".vce"):
        env = env[:-4]
        
    jobNameDotted = env
    jenkins_name = env
    jenkins_link = env
    xmlCoverReportName = "coverage_results_" + env + ".xml"
    xmlTestingReportName = "test_results_" + env + ".xml"

    xml_file = GenerateXml(env,
                           envPath,
                           env, "", "", 
                           xmlCoverReportName,
                           jenkins_name,
                           xmlTestingReportName,
                           jenkins_link,
                           jobNameDotted, 
                           args.verbose, 
                           None)

    if xml_file.api == None:
        print ("\nCannot find project file (.vcp or .vce): " + envPath + os.sep + env)
        
    elif xml_file.using_cover:
        xml_file.generate_cover()
        print ("\nvectorcast-coverage plugin for Jenkins compatible file generated: " + xmlCoverReportName)

    else:
        xml_file.generate_unit()
        print ("\nJunit plugin for Jenkins compatible file generated: " + xmlTestingReportName)
