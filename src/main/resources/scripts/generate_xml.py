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
import datetime
import cgi
from vector.apps.DataAPI.api import Api
from vector.apps.DataAPI.cover_api import CoverApi
from vector.apps.ReportBuilder.custom_report import fmt_percent
from operator import attrgetter
from vector.enums import COVERAGE_TYPE_TYPE_T
from vector.apps.DataAPI.models import TestCase

class GenerateXml(object):

    def __init__(self, build_dir, env, cover_report_name, jenkins_name, unit_report_name, jenkins_link, jobNameDotted):
        self.build_dir = build_dir
        self.env = env
        self.cover_report_name = cover_report_name
        self.unit_report_name = unit_report_name
        self.jenkins_name = jenkins_name
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
            self.api = Api(unit_path)
        else:
            self.api = None
            print "Error: Could not determine project type for {}/{}".format(build_dir, env)

    def calc_cov_values(self, x, y):
        column = ''
        if y == 0:
            column = None
        else:
            column = '%s%% (%d / %d)' % (fmt_percent(x, y), x, y)
        return column

    def add_coverage(self, metrics, cov_type):
        entry = {}
        entry["statement"] = None
        entry["branch"] = None
        entry["mcdc"] = None
        entry["basispath"] = None
        entry["function"] = None
        entry["functioncall"] = None
        if self.has_function_coverage:
            if cover_func.has_covered_objects:
                entry["function"] = '100% (1 / 1)'
            else:
                entry["function"] = '0% (0 / 1)'
        if self.has_call_coverage:
            entry["functioncall"] = self.calc_cov_values(metrics.max_covered_function_calls, metrics.function_calls)
        if cov_type == "MC/DC":
            entry["branch"] = self.calc_cov_values(metrics.max_covered_mcdc_branches, metrics.mcdc_branches)
            if not self.simplified_mcdc:
                entry["mcdc"] = self.calc_cov_values(metrics.max_covered_mcdc_pairs, metrics.mcdc_pairs)
        elif cov_type == "Basis Paths":
            (cov,total) = func.basis_paths_coverage
            entry["basis_path"] = self.calc_cov_values(cov, total)
        elif cov_type == "Statement+MC/DC":
            entry["statement"] = self.calc_cov_values(metrics.max_covered_statements, metrics.statements)
            entry["branch"] = self.calc_cov_values(metrics.max_covered_mcdc_branches, metrics.mcdc_branches)
            if not self.simplified_mcdc:
                entry["mcdc"] = self.calc_cov_values(metrics.max_covered_mcdc_pairs, metrics.mcdc_pairs)
        elif cov_type == "Statement":
            entry["statement"] = self.calc_cov_values(metrics.max_covered_statements, metrics.statements)
        elif cov_type == "Statement+Branch":
            entry["statement"] = self.calc_cov_values(metrics.max_covered_statements, metrics.statements)
            entry["branch"] = self.calc_cov_values(metrics.max_covered_branches, metrics.branches)
        elif cov_type == "Branch":
            entry["branch"] = self.calc_cov_values(metrics.max_covered_branches, metrics.branches)

        return entry

    def grand_total_coverage(self, cov_type):
        entry = {}
        entry["statement"] = None
        entry["branch"] = None
        entry["mcdc"] = None
        entry["basispath"] = None
        entry["function"] = None
        entry["functioncall"] = None
        if self.has_function_coverage:
            entry["function"] = self.calc_cov_values(self.grand_total_max_covered_functions, self.grand_total_function_calls)
        if self.has_call_coverage:
            entry["functioncall"] = self.calc_cov_values(self.grand_total_max_covered_function_calls, self.grand_total_function_calls)
        if cov_type == "MC/DC":
            entry["branch"] = self.calc_cov_values(self.grand_total_max_mcdc_covered_branches, self.grand_total_mcdc_branches)
            if not self.simplified_mcdc:
                entry["mcdc"] = self.calc_cov_values(self.grand_total_max_covered_mcdc_pairs, self.grand_total_mcdc_pairs)
        elif cov_type == "Basis Paths":
            entry["basis_path"] = self.calc_cov_values(self.grand_total_cov_basis_path, self.grand_total_total_basis_path)
        elif cov_type == "Statement+MC/DC":
            entry["statement"] = self.calc_cov_values(self.grand_total_max_covered_statements, self.grand_total_statements)
            entry["branch"] = self.calc_cov_values(self.grand_total_max_mcdc_covered_branches, self.grand_total_mcdc_branches)
            if not self.simplified_mcdc:
                entry["mcdc"] = self.calc_cov_values(self.grand_total_max_covered_mcdc_pairs, self.grand_total_mcdc_pairs)
        elif cov_type == "Statement":
            entry["statement"] = self.calc_cov_values(self.grand_total_max_covered_statements, self.grand_total_statements)
        elif cov_type == "Statement+Branch":
            entry["statement"] = self.calc_cov_values(self.grand_total_max_covered_statements, self.grand_total_statements)
            entry["branch"] = self.calc_cov_values(self.grand_total_max_covered_branches, self.grand_total_branches)
        elif cov_type == "Branch":
            entry["branch"] = self.calc_cov_values(self.grand_total_max_covered_branches, self.grand_total_branches)

        return entry

#        if "BUILD_URL" in os.environ:
#            self.section_context["build_url"] = os.getenv('BUILD_URL') + "artifact/execution/" + os.environ["JENKINS_LINK_NAME"] + ".html#ExecutionResults_"
#        else:
#            self.section_context["build_url"] = "undefined"
#
#from vector.apps.ReportBuilder.report_section import ReportSection
#from vector.apps.ReportBuilder.custom_report import vt_date_time
#from vector.apps.DataAPI.models import TestCase
#from vector.enums import EXECUTION_STATUS_T, TEST_HISTORY_FAILURE_REASON_T
#
## See DynamicReport.cpp:5508 (TestcaseManagementSection::generateTestcaseRows)
#class TestcaseManagement(ReportSection):
#    title = 'Testcase Management'
#
#    def add_totals_row(self):
#        '''Add a 'total' row to the list of testcases'''
#
#        entry = {}
#        entry['object'] = None
#        if self.total_functions == 0 and self.total_tests == 0:
#            return
#        entry['style'] = ''
#        entry['is_totals'] = True
#        if self.total_functions == 0:
#            entry['total_functions'] = ''
#        else:
#            entry['total_functions'] = self.total_functions
#        if self.total_tests > 0:
#            entry['total_testcases'] = self.total_tests
#            if self.passes == 0 and self.total_possible_passes == 0:
#                entry['pass_fail'] = ''
#            else:
#                if self.passes == self.total_possible_passes:
#                    entry['pass_fail'] = "PASS " + str(self.passes) + ' / ' + str(self.total_possible_passes)
#                    entry['style'] = 'success'
#                else:
#                    entry['pass_fail'] = "FAIL " + str(self.passes) + ' / ' + str(self.total_possible_passes)
#                    entry['style'] = 'danger'
#        else:  
#            entry['total_testcases'] = ''
#            entry['pass_fail'] = ''
#        entry['row'] = []
#        entry['row'].append('TOTALS')
#        entry['row'].append(str(entry['total_functions']))
#        entry['row'].append(str(entry['total_testcases']))
#        entry['row'].append('')
#        entry['row'].append(entry['pass_fail'])
#        if self.section_context['any_errors_exist']:
#            entry['row'].append('')
#        if self.section_context['any_requirements_exist']:
#            entry['row'].append('')
#        
#        self.section_context['testcases'].append(entry)
#

#    def add_testcase(self, tc):
#        '''Add given testcase line to the testcases list'''
#
#        entry = {}
#        entry['style'] = ''
#        entry['unit_name'] = self.unit_name
#        entry['function_name'] = self.func_name
#        entry['is_totals'] = False
#
#        entry['testcase_name'] = tc.name
#        entry['failure_reason'] = ''
#        entry['pass_fail'] = ''
#        entry['object'] = tc
#
#        if tc.for_compound_only:
#            entry['start_time'] = 'Compound-only Test'
#            entry['pass_fail'] = ''
#            entry['failure_reason'] = ''
#        else:
#            self.total_possible_passes += 1
#            if tc.start_time:
#                entry['start_time'] = vt_date_time(tc.start_time)
#            else:
#                entry['start_time'] = 'No Execution Results Exist'
#            history = tc.history
#            summary = history.summary
#            exp_total = summary.expected_total
#            exp_pass = exp_total - summary.expected_fail
#            if self.api.environment.get_option("VCAST_OLD_STYLE_MANAGEMENT_REPORT"):
#                exp_pass += summary.control_flow_total - summary.control_flow_fail
#                exp_total += summary.control_flow_total + summary.signals + summary.unexpected_exceptions
#
#            if tc.testcase_status == "TCR_STATUS_OK" and not history.get_failure_reasons() and \
#                 (tc.exec_status == None or tc.exec_status == EXECUTION_STATUS_T.EXEC_SUCCESS_NONE):
#                if tc.execution_status == None:
#                    entry['pass_fail'] = ''
#                else:
#                    if tc.status == "TC_EXECUTION_PASSED":
#                        entry['style'] = 'success'
#                        self.passes += 1
#                        if (exp_total > 0):
#                            entry['pass_fail'] = 'PASS ' + str(exp_pass) + ' / ' + str(exp_total)
#                        else:
#                            entry['pass_fail'] = 'PASS'
#                    elif tc.status == "TC_EXECUTION_NONE":
#                        entry['style'] = 'danger'
#                        entry['pass_fail'] = ''
#                        entry['start_time'] = 'No Execution Results Exist'
#                    else:
#                        entry['style'] = 'danger'
#                        if tc.start_time:
#                            if (exp_total > 0):
#                                entry['pass_fail'] = 'FAIL ' + str(exp_pass) + ' / ' + str(exp_total)
#                            else:
#                                entry['pass_fail'] = 'FAIL'
#            else:
#                entry['style'] = 'danger'
#                entry['pass_fail'] = 'Abnormal Termination'
#                if history.get_failure_reasons():
#                    if TEST_HISTORY_FAILURE_REASON_T.TEST_HISTORY_FAILURE_REASON_EXECUTABLE_MISSING in history.get_failure_reasons(): 
#                        entry['failure_reason'] = 'Executable Missing'
#                    else:
#                        entry['failure_reason'] = 'See Execution Report for error'
#                else:
#                    # Check for empty slots and need for 'Trouble Slots' message
#                    if tc.is_compound_test and tc.auto_failed_empty_slots and \
#                        (tc.exec_status == EXECUTION_STATUS_T.EXEC_EMPTY_TESTCASE or tc.exec_status == EXECUTION_STATUS_T.EXEC_NO_EXPECTED_VALUES):
#                        if tc.testcase_status == "TCR_EMPTY_TEST_CASES":
#                            entry['failure_reason'] = 'Empty Test Case'
#                        else:
#                            entry['failure_reason'] = history.convert_reason_to_description(None, tc.exec_status)[0]
#                        start = ' - Trouble Slots: '
#                        for slot in tc.auto_failed_empty_slots:
#                            entry['failure_reason'] += start + str(slot)
#                            start = ', '
#                    else:
#                        entry['failure_reason'] = ''
#                        if tc.exec_status != None:
#                            if tc.testcase_status == "TCR_EMPTY_TEST_CASES":
#                                entry['failure_reason'] = 'Empty Test Case'
#                            else:
#                                entry['failure_reason'] = history.convert_reason_to_description(None, tc.exec_status)[0]
#                        else:
#                            for reason in history.failure_descriptions:
#                                entry['failure_reason'] += reason["short"]
#        entry['requirements'] = ''
#        newline = ''
#        for req in tc.requirements:
#            entry["requirements"] += "%s%s  %s" % (newline, req.external_key, req.title)
#            newline = '\n'
#        entry['notes'] = tc.notes[:80]
#        self.add_entry_row(entry)
#
#        self.total_tests += 1
#
#        self.section_context['testcases'].append(entry)
#        self.added_func = True
#
#        # Reset function name so subsequent rows do not repeat the function name
#        self.func_name = ''
#        # Reset unit name so subsequent rows do not repeat the unit name
#        self.unit_name = ''
#
#
#    def process_function(self, func):
#        '''Process given function and add any testcases'''
#
#        self.added_func = False
#        self.func_name = func.display_name
#        for tc in func.testcases:
#            if not tc.is_csv_map:
#                self.add_testcase(tc)
#
#        if self.added_func == False and func.index != 0 and func.display_name != "":
#            # Add empty function entry (for when a function had no testcases)
#            entry = {}
#            entry['unit_name'] = self.unit_name
#            entry['function_name'] = self.func_name
#            entry['is_totals'] = False
#            entry['testcase_name'] = ''
#            entry['start_time'] = ''
#            entry['failure_reason'] = ''
#            entry['pass_fail'] = ''
#            entry['requirements'] = ''
#            entry['notes'] = ''
#            entry['style'] = ''
#            self.add_entry_row(entry)
#            self.section_context['testcases'].append(entry)
#            self.added_func = True
#        if self.added_func:
#            self.total_functions += 1
#            self.unit_name = ''
#            self.added_unit = True
#
#
#    def add_simple_row(self, unit_name):
#        entry = {}
#        entry['unit_name'] = unit_name
#        entry['function_name'] = ''
#        entry['is_totals'] = False
#        entry['testcase_name'] = ''
#        entry['start_time'] = ''
#        entry['failure_reason'] = ''
#        entry['pass_fail'] = ''
#        entry['object'] = None
#        entry['requirements'] = ''
#        entry['notes'] = ''
#        entry['style'] = ''
#        self.add_entry_row(entry)
#        self.section_context['testcases'].append(entry)
#        entry = {}
#        entry['is_totals'] = True
#        entry['total_functions'] = ''
#        entry['total_testcases'] = ''
#        entry['pass_fail'] = ''
#        entry['style'] = ''
#        entry['row'] = []
#        entry['row'].append('TOTALS')
#        entry['row'].append('')
#        entry['row'].append('')
#        entry['row'].append('')
#        entry['row'].append('')
#        if self.section_context['any_errors_exist']:
#            entry['row'].append('')
#        if self.section_context['any_requirements_exist']:
#            entry['row'].append('')
#        if self.section_context['notes_column']:
#            entry['row'].append('')
#        self.section_context['testcases'].append(entry)
#
#
#    def add_entry_row(self, entry):
#        entry['row'] = []
#        entry['style_row'] = []
#        
#        entry['row'].append(entry['unit_name'])
#        entry['style_row'].append('')
#        entry['row'].append(entry['function_name'])
#        entry['style_row'].append('')
#        entry['row'].append(entry['testcase_name'])
#        entry['style_row'].append(entry['style'])
#        entry['row'].append(entry['start_time'])
#        entry['style_row'].append(entry['style'])
#        entry['row'].append(entry['pass_fail'])
#        entry['style_row'].append(entry['style'])
#        if self.section_context['any_errors_exist']:
#            entry['row'].append(entry['failure_reason'])
#            entry['style_row'].append(entry['style'])
#        if self.section_context['any_requirements_exist']:
#            entry['row'].append(entry['requirements'])
#            entry['style_row'].append(entry['style'])
#        if self.section_context['notes_column']:
#            entry['row'].append(entry['notes'])
#            entry['style_row'].append(entry['style'])

    def add_compound_tests(self):
#        self.total_tests = 0
#        self.total_possible_passes = 0
#        self.passes = 0
#        self.total_functions = 0
#
#        added_tests = False
        for tc in self.api.TestCase.all():
            if tc.kind == TestCase.KINDS['compound']:
                self.write_testcase(tc, "<<COMPOUND>>", "<<COMPOUND>>")
#                added_tests = True
#
#        if added_tests:
#            self.add_totals_row()
#        else:
#            if self.report_context['env_wide']:
#                self.add_simple_row('<<COMPOUND>>')

    def add_init_tests(self):
#        self.total_tests = 0
#        self.total_possible_passes = 0
#        self.passes = 0
#        self.total_functions = 0
#
#        added_tests = False
#        self.unit_name = '<<INIT>>'
#        self.func_name = ''
        for tc in self.api.TestCase.all():
            if tc.kind == TestCase.KINDS['init']:
                self.write_testcase(tc, "<<INIT>>", "<<INIT>>")
#                self.add_testcase(tc)
#                added_tests = True
#
#        if added_tests:
#            self.add_totals_row()
#        else:
#            if self.report_context['env_wide']:
#                self.add_simple_row('<<INIT>>')


    def generate_unit(self):
        if "BUILD_URL" in os.environ:
            self.build_url = os.getenv('BUILD_URL') + "artifact/execution/" + self.jenkins_link + ".html#ExecutionResults_"
        else:
            self.build_url = "undefined"
        self.start_unit_file()
#        self.testcases = []
#        self.section_context['any_errors_exist'] = False
#        self.section_context['any_requirements_exist'] = self.api.environment.requirements_exist
#        self.section_context['notes_column'] = self.api.environment.get_option("VCAST_VERBOSE_MANAGEMENT_REPORT")

#        for tc in self.api.testcases:
#            pass

#            if len(tc.requirements) > 0:
#                self.section_context['any_requirements_exist'] = True
#            if tc.testcase_status == "TCR_STATUS_OK" and not tc.history.get_failure_reasons() and \
#                 (tc.exec_status == None or tc.exec_status == EXECUTION_STATUS_T.EXEC_SUCCESS_NONE):
#                pass
#            else:
#                self.section_context['any_errors_exist'] = True
#
#        self.total_tests = 0
#        self.total_possible_passes = 0
#        self.passes = 0
#        self.total_functions = 0
#

        self.add_compound_tests()
        self.add_init_tests()
        for unit in self.api.Unit.all():
            if unit.is_uut:
#                self.added_unit = False
#                self.unit_name = unit.name
#                self.total_tests = 0
#                self.total_possible_passes = 0
#                self.passes = 0
#                self.total_functions = 0
                for func in unit.functions:
                    if not func.is_non_testable_stub:
#        self.added_func = False
#        self.func_name = func.display_name
                        for tc in func.testcases:
                            if not tc.is_csv_map:
                                self.write_testcase(tc, tc.function.unit.name, tc.function.display_name)
#
#        if self.added_func == False and func.index != 0 and func.display_name != "":
#            # Add empty function entry (for when a function had no testcases)
#            entry = {}
#            entry['unit_name'] = self.unit_name
#            entry['function_name'] = self.func_name
#            entry['is_totals'] = False
#            entry['testcase_name'] = ''
#            entry['start_time'] = ''
#            entry['failure_reason'] = ''
#            entry['pass_fail'] = ''
#            entry['requirements'] = ''
#            entry['notes'] = ''
#            entry['style'] = ''
#            self.add_entry_row(entry)
#            self.section_context['testcases'].append(entry)
#            self.added_func = True
#        if self.added_func:
#            self.total_functions += 1
#            self.unit_name = ''
#            self.added_unit = True
#
#                if self.added_unit == False:
#                    # Add empty unit entry (for when all functions had
#                    # no testcases)
#                    self.add_simple_row(self.unit_name)
#                    
#                self.unit_name = ''
#                
#                self.add_totals_row()
#        
#        # define header rows and column widths for the table
#        self.section_context['column_widths'] = []
#
#        self.section_context["sep_length"] = ( \
#            self.section_context['columns']['VCAST_RPTS_UNIT_COLUMN_WIDTH'] +
#            self.section_context['columns']['VCAST_RPTS_SUBPROGRAM_COLUMN_WIDTH'] +
#            self.section_context['columns']['VCAST_RPTS_TESTCASE_COLUMN_WIDTH'] +
#            self.section_context['columns']['VCAST_RPTS_DATE_COLUMN_WIDTH'] +
#            self.section_context['columns']['VCAST_RPTS_RESULT_COLUMN_WIDTH'])
#        extra = 4
#
#        self.section_context["table_header_row"] = []
#        self.section_context["table_header_row"].append("Unit")
#        self.section_context['column_widths'].append(self.section_context['columns']['VCAST_RPTS_UNIT_COLUMN_WIDTH'])
#        self.section_context["table_header_row"].append("Subprogram")
#        self.section_context['column_widths'].append(self.section_context['columns']['VCAST_RPTS_SUBPROGRAM_COLUMN_WIDTH'])
#        self.section_context["table_header_row"].append("Test Cases")
#        self.section_context['column_widths'].append(self.section_context['columns']['VCAST_RPTS_TESTCASE_COLUMN_WIDTH'])
#        self.section_context["table_header_row"].append("Execution Date and Time")
#        self.section_context['column_widths'].append(self.section_context['columns']['VCAST_RPTS_DATE_COLUMN_WIDTH'])
#        self.section_context["table_header_row"].append("Pass/Fail")
#        self.section_context['column_widths'].append(self.section_context['columns']['VCAST_RPTS_RESULT_COLUMN_WIDTH'])
#        if self.section_context['any_errors_exist']:
#            self.section_context["table_header_row"].append("Failure Reason")
#            self.section_context['column_widths'].append(25)
#        if  self.section_context['any_requirements_exist']:
#            self.section_context["table_header_row"].append("Requirements")
#            # Probably should be VCAST_RPTS_REQUIREMENTS_COLUMN_WIDTH but seems to be fixed at 25
#            self.section_context['column_widths'].append(25)
#            self.section_context["sep_length"]+= 25
#            extra = 5
#        if  self.section_context["notes_column"]:
#            self.section_context["table_header_row"].append("Notes")
#            self.section_context['column_widths'].append(self.section_context['columns']['VCAST_RPTS_NOTES_COLUMN_WIDTH'])
#            self.section_context["sep_length"]+= self.section_context['columns']['VCAST_RPTS_NOTES_COLUMN_WIDTH']
#            extra = 5
#        self.section_context["sep_length"]+= extra
        
        self.end_unit_file()

    def start_unit_file(self):
        print "Writing unit xml file: {}".format(self.unit_report_name)
        self.fh = open(self.unit_report_name, "w")
        self.fh.write('<testsuites xmlns="http://check.sourceforge.net/ns">\n')
        self.fh.write('    <datetime>%s</datetime>\n' % self.get_timestamp())
        self.fh.write('    <suite>\n')
        self.fh.write('        <title>%s</title>\n' % self.jobNameDotted)

    def write_testcase(self, tc, unit_name, func_name):
        unit_name = cgi.escape(unit_name)
        func_name = cgi.escape(func_name)
        tc_name = cgi.escape(tc.name)
        if tc.passed:
            self.fh.write('        <test result="success">\n')
        else:
            self.fh.write('        <test result="failure">\n')
        self.fh.write('            <fn>{}.{}</fn>\n'.format(unit_name, func_name))
        self.fh.write('            <id>{}.{}.{}</id>\n'.format(unit_name, func_name, tc_name))
        self.fh.write('            <iteration>1</iteration>\n')
        self.fh.write('            <description>Simple Test Case</description>\n')
        summary = tc.history.summary
        exp_total = summary.expected_total
        exp_pass = exp_total - summary.expected_fail
        if self.api.environment.get_option("VCAST_OLD_STYLE_MANAGEMENT_REPORT"):
            exp_pass += summary.control_flow_total - summary.control_flow_fail
            exp_total += summary.control_flow_total + summary.signals + summary.unexpected_exceptions
        if tc.passed:
            status = "PASS"
        else:
            status = "FAIL"
        if exp_pass == 0 and exp_total == 0:
            msg = "{} See Execution Report:\n {}{}".format(status, self.build_url, tc.id)
        else:
            msg = "{} {} / {} See Execution Report:\n {}{}".format(status, exp_pass, exp_total, self.build_url, tc.id)
        self.fh.write('            <message>%s</message>\n' % msg)
        self.fh.write('        </test>\n')

    def end_unit_file(self):
        self.fh.write('    </suite>\n')
        self.fh.write('    <duration>1</duration>\n\n')
        self.fh.write('</testsuites>\n')
        self.fh.close()

    def generate_cover(self):
        self.num_functions = 0

        self.simplified_mcdc = self.api.environment.get_option("VCAST_SIMPLIFIED_CONDITION_COVERAGE")
        self.has_function_coverage = self.api.environment.get_option("VCAST_DISPLAY_FUNCTION_COVERAGE")
        self.units = []
        self.has_call_coverage = False
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
        self.grand_total_total_basis_path = 0
        self.grand_total_cov_basis_path = 0
        cov_type = self.api.environment.coverage_type_text
        if self.using_cover:
            units = self.api.File.all()
            units.sort(key=lambda x: (x.coverage_type, x.unit_index))
        else:
            units = self.api.Unit.all()
        for unit in units:
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
            if cover_file.has_call_coverage:
                self.has_call_coverage = True
            entry = {}
            entry["unit"] = unit
            entry["functions"] = []
            entry["complexity"] = 0
            entry["coverage"] = self.add_coverage(unit.cover_metrics, cov_type)
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
                func_entry["coverage"] = self.add_coverage(func.cover_data.metrics, cov_type)
                self.num_functions += 1
                entry["functions"].append(func_entry)
            if functions_added:
                self.units.append(entry)

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

            if cov_type == "Basis Paths":
                (cov, total) = unit.basis_paths_coverage
                self.grand_total_total_basis_path += total
                self.grand_total_cov_basis_path += cov

        self.coverage = self.grand_total_coverage(cov_type)
        self.num_units = len(self.units)
        
        self.start_cov_file()
        self.write_cov_units()
        self.end_cov_file()
        
    def get_timestamp(self):
        dt = datetime.datetime.now()
        hour = dt.hour
        if hour > 12:
            hour -= 12
        return dt.strftime('%d %b %Y  @HR@:%M:%S %p').upper().replace('@HR@', str(hour))

    def start_cov_file(self):
        print "Writing coverage xml file: {}".format(self.cover_report_name)
        self.fh = open(self.cover_report_name, "w")
        self.fh.write('<!-- VectorCAST/Jenkins Integration, Generated %s -->\n' % self.get_timestamp())
        self.fh.write('<report>\n')
        self.fh.write('  <version value="3"/>\n')
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

        self.fh.write('      <environment name="%s">\n' % self.jenkins_name)
        if self.coverage["statement"]:
            self.fh.write('        <coverage type="statement, %%" value="%s"/>\n' % self.coverage["statement"])
        if self.coverage["branch"]:
            self.fh.write('        <coverage type="branch, %%" value="%s"/>\n' % self.coverage["branch"])
        if self.coverage["mcdc"]:
            self.fh.write('        <coverage type="mcdc, %%" value="%s"/>\n' % self.coverage["mcdc"])
        if self.coverage["basispath"]:
            self.fh.write('        <coverage type="basispath, %%" value="%s"/>\n' % self.coverage["basispath"])
        if self.coverage["function"]:
            self.fh.write('        <coverage type="function,% %" value="%s"/>\n' % self.coverage["function"])
        if self.coverage["functioncall"]:
            self.fh.write('        <coverage type="functioncall, %%" value="%s"/>\n' % self.coverage["functioncall"])
        self.fh.write('        <coverage type="complexity, %%" value="0%% (%s / 0)"/>\n' % self.grand_total_complexity)
        self.fh.write('\n')

    def write_cov_units(self):
        for unit in self.units:
            self.fh.write('        <unit name="%s">\n' % unit["unit"].name)
            if unit["coverage"]["statement"]:
                self.fh.write('          <coverage type="statement, %%" value="%s"/>\n' % unit["coverage"]["statement"])
            if unit["coverage"]["branch"]:
                self.fh.write('          <coverage type="branch, %%" value="%s"/>\n' % unit["coverage"]["branch"])
            if unit["coverage"]["mcdc"]:
                self.fh.write('          <coverage type="mcdc, %%" value="%s"/>\n' % unit["coverage"]["mcdc"])
            if unit["coverage"]["basispath"]:
                self.fh.write('          <coverage type="basispath, %%" value="%s"/>\n' % unit["coverage"]["basispath"])
            if unit["coverage"]["function"]:
                self.fh.write('          <coverage type="function,% %" value="%s"/>\n' % unit["coverage"]["function"])
            if unit["coverage"]["functioncall"]:
                self.fh.write('          <coverage type="functioncall, %%" value="%s"/>\n' % unit["coverage"]["functioncall"])
            self.fh.write('          <coverage type="complexity, %%" value="0%% (%s / 0)"/>\n' % unit["complexity"])

            for func in unit["functions"]:
                self.fh.write('          <subprogram name="%s">\n' % func["func"].name)
                if func["coverage"]["statement"]:
                    self.fh.write('            <coverage type="statement, %%" value="%s"/>\n' % func["coverage"]["statement"])
                if func["coverage"]["branch"]:
                    self.fh.write('            <coverage type="branch, %%" value="%s"/>\n' % func["coverage"]["branch"])
                if func["coverage"]["mcdc"]:
                    self.fh.write('            <coverage type="mcdc, %%" value="%s"/>\n' % func["coverage"]["mcdc"])
                if func["coverage"]["basispath"]:
                    self.fh.write('            <coverage type="basispath, %%" value="%s"/>\n' % func["coverage"]["basispath"])
                if func["coverage"]["function"]:
                    self.fh.write('            <coverage type="function,% %" value="%s"/>\n' % func["coverage"]["function"])
                if func["coverage"]["functioncall"]:
                    self.fh.write('            <coverage type="functioncall, %%" value="%s"/>\n' % func["coverage"]["functioncall"])
                self.fh.write('            <coverage type="complexity, %%" value="0%% (%s / 0)"/>\n' % func["complexity"])

                self.fh.write('          </subprogram>\n')
            self.fh.write('        </unit>\n')

    def end_cov_file(self):
        self.fh.write('      </environment>\n')
        self.fh.write('    </all>\n')
        self.fh.write('  </data>\n')
        self.fh.write('</report>')
        self.fh.close()
