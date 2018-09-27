import os
from vector.apps.ReportBuilder.report_section import ReportSection
from vector.apps.ReportBuilder.custom_report import fmt_percent
from operator import attrgetter
from vector.enums import COVERAGE_TYPE_TYPE_T

class JenkinsCoverage(ReportSection):
    title = 'Jenkins Coverage'
    supported_environments = ('UNIT', 'COVER')

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
        if self.section_context['has_function_coverage']:
            if cover_func.has_covered_objects:
                entry["function"] = '100% (1 / 1)'
            else:
                entry["function"] = '0% (0 / 1)'
        if self.section_context['has_call_coverage']:
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
        if self.section_context['has_function_coverage']:
            entry["function"] = self.calc_cov_values(self.section_context['grand_total_max_covered_functions'], self.section_context['grand_total_function_calls'])
        if self.section_context['has_call_coverage']:
            entry["functioncall"] = self.calc_cov_values(self.section_context['grand_total_max_covered_function_calls'], self.section_context['grand_total_function_calls'])
        if cov_type == "MC/DC":
            entry["branch"] = self.calc_cov_values(self.section_context['grand_total_max_mcdc_covered_branches'], self.section_context['grand_total_mcdc_branches'])
            if not self.simplified_mcdc:
                entry["mcdc"] = self.calc_cov_values(self.section_context['grand_total_max_covered_mcdc_pairs'], self.section_context['grand_total_mcdc_pairs'])
        elif cov_type == "Basis Paths":
            entry["basis_path"] = self.calc_cov_values(self.section_context['grand_total_cov_basis_path'], self.section_context['grand_total_total_basis_path'])
        elif cov_type == "Statement+MC/DC":
            entry["statement"] = self.calc_cov_values(self.section_context["grand_total_max_covered_statements"], self.section_context["grand_total_statements"])
            entry["branch"] = self.calc_cov_values(self.section_context['grand_total_max_mcdc_covered_branches'], self.section_context['grand_total_mcdc_branches'])
            if not self.simplified_mcdc:
                entry["mcdc"] = self.calc_cov_values(self.section_context['grand_total_max_covered_mcdc_pairs'], self.section_context['grand_total_mcdc_pairs'])
        elif cov_type == "Statement":
            entry["statement"] = self.calc_cov_values(self.section_context["grand_total_max_covered_statements"], self.section_context["grand_total_statements"])
        elif cov_type == "Statement+Branch":
            entry["statement"] = self.calc_cov_values(self.section_context["grand_total_max_covered_statements"], self.section_context["grand_total_statements"])
            entry["branch"] = self.calc_cov_values(self.section_context["grand_total_max_covered_branches"], self.section_context["grand_total_branches"])
        elif cov_type == "Branch":
            entry["branch"] = self.calc_cov_values(self.section_context["grand_total_max_covered_branches"], self.section_context["grand_total_branches"])

        return entry

    def prepare_data(self):
        if "JENKINS_FULL_NAME" in os.environ:
            self.section_context["full_name"] = os.environ["JENKINS_FULL_NAME"]
        else:
            self.section_context["full_name"] = "NOT DEFINED"
        self.section_context["num_functions"] = 0

        self.simplified_mcdc = self.api.environment.get_option("VCAST_SIMPLIFIED_CONDITION_COVERAGE")
        self.section_context['has_function_coverage'] = self.api.environment.get_option("VCAST_DISPLAY_FUNCTION_COVERAGE")
        self.section_context['units'] = []
        self.section_context['has_call_coverage'] = False
        self.section_context['grand_total_complexity'] = 0

        self.section_context['grand_total_max_covered_branches'] = 0
        self.section_context['grand_total_branches'] = 0
        self.section_context['grand_total_max_covered_statements'] = 0
        self.section_context['grand_total_statements'] = 0
        self.section_context['grand_total_max_mcdc_covered_branches'] = 0
        self.section_context['grand_total_mcdc_branches'] = 0
        self.section_context['grand_total_max_covered_mcdc_pairs'] = 0
        self.section_context['grand_total_mcdc_pairs'] = 0
        self.section_context['grand_total_max_covered_function_calls'] = 0
        self.section_context['grand_total_function_calls'] = 0
        self.section_context['grand_total_max_covered_functions'] = 0
        self.section_context['grand_total_total_basis_path'] = 0
        self.section_context['grand_total_cov_basis_path'] = 0
        cov_type = self.api.environment.coverage_type_text
        units = self.units
        if self.using_cover:
            units.sort(key=lambda x: (x.coverage_type, x.unit_index))
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
                self.section_context['has_call_coverage'] = True
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
                    self.section_context['grand_total_complexity'] += complexity
                func_entry = {}
                func_entry["func"] = func
                func_entry["complexity"] = func.cover_data.complexity
                func_entry["coverage"] = self.add_coverage(func.cover_data.metrics, cov_type)
                self.section_context["num_functions"] += 1
                entry["functions"].append(func_entry)
            if functions_added:
                self.section_context['units'].append(entry)

            metrics = unit.cover_metrics

            self.section_context['grand_total_max_covered_branches'] += metrics.max_covered_branches
            self.section_context['grand_total_branches'] += metrics.branches
            self.section_context['grand_total_max_covered_statements'] += metrics.max_covered_statements
            self.section_context['grand_total_statements'] += metrics.statements
            self.section_context['grand_total_max_mcdc_covered_branches'] += metrics.max_covered_mcdc_branches
            self.section_context['grand_total_mcdc_branches'] += metrics.mcdc_branches
            self.section_context['grand_total_max_covered_mcdc_pairs'] += metrics.max_covered_mcdc_pairs
            self.section_context['grand_total_mcdc_pairs'] += metrics.mcdc_pairs
            self.section_context['grand_total_max_covered_function_calls'] += metrics.max_covered_function_calls
            self.section_context['grand_total_function_calls'] += metrics.function_calls
            (total_funcs, funcs_covered) = cover_file.functions_covered
            self.section_context['grand_total_max_covered_functions'] += funcs_covered

            if cov_type == "Basis Paths":
                (cov, total) = unit.basis_paths_coverage
                self.section_context['grand_total_total_basis_path'] += total
                self.section_context['grand_total_cov_basis_path'] += cov

        self.section_context["coverage"] = self.grand_total_coverage(cov_type)
        self.section_context["num_units"] = len(self.section_context["units"])

