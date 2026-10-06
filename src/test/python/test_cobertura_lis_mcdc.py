"""Regression tests for translation-unit MC/DC export (run with vpython)."""
import sys
import unittest
from pathlib import Path
from types import SimpleNamespace as NS
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "main/resources/scripts"))
import cobertura
from vector.apps.DataAPI.coverdb import Metrics, SourceCoverageMode


class LisMcdcExportTest(unittest.TestCase):
    def decision(self, line, hits):
        return NS(start_line=line, is_branch=False, conditions=[
            NS(get_covered_pair=lambda hit=hit: object() if hit else None)
            for hit in hits])

    def source(self, instrumentations, mode=SourceCoverageMode.LIS_SOURCE_COVERAGE_MODE):
        return NS(coverdb=NS(coverage_mode=NS(get_source_coverage_mode=lambda: mode)),
                  functions=[NS(metrics=NS(mcdc_pairs=3), instrumented_functions=[
                      NS(mcdc_decisions=decisions) for decisions in instrumentations])])

    def test_recovers_two_of_three_pairs(self):
        source = self.source([[self.decision(70, [True, True, False])]])
        self.assertEqual({70: [2, 3]}, cobertura.get_lis_mcdc_pairs_by_line(source))

    def test_merges_instrumentations_without_summing_duplicates_or_unioning_hits(self):
        source = self.source([[self.decision(70, [True, False, False])],
                              [self.decision(70, [False, True, False])]])
        self.assertEqual({70: [1, 3]}, cobertura.get_lis_mcdc_pairs_by_line(source))

    def test_sums_distinct_decisions_on_same_line(self):
        source = self.source([[self.decision(70, [True]), self.decision(70, [False, False])]])
        self.assertEqual({70: [1, 3]}, cobertura.get_lis_mcdc_pairs_by_line(source))

    def test_ignores_branch_only_decisions_and_simplified_coverage(self):
        source = self.source([[NS(is_branch=True)]])
        self.assertEqual({}, cobertura.get_lis_mcdc_pairs_by_line(source))
        source.functions[0].metrics.mcdc_pairs = 0
        source.functions[0].instrumented_functions = None
        self.assertEqual({}, cobertura.get_lis_mcdc_pairs_by_line(source))

    def test_sfp_does_not_inspect_translation_unit_objects(self):
        source = self.source([], SourceCoverageMode.SFP_SOURCE_COVERAGE_MODE)
        source.functions = None
        self.assertEqual({}, cobertura.get_lis_mcdc_pairs_by_line(source))

    def test_writes_pairs_without_changing_branch_or_call_counts(self):
        source = self.source([[self.decision(70, [True, True, False]),
                               self.decision(78, [False])]])
        metrics = Metrics()
        metrics.branches = 2
        metrics.max_covered_branches = 2
        metrics.function_calls = 1
        metrics.max_covered_function_calls = 1
        source.iterate_coverage = lambda: iter([NS(line_number=n, metrics=metrics) for n in (70, 78)])
        lines = cobertura.etree.Element("lines")
        cobertura.processStatementBranchMCDC(source, lines, extended=True)
        self.assertEqual("66.66666666666667% (2/3)", lines[0].get("mcdcpair-coverage"))
        self.assertEqual("0.0% (0/1)", lines[1].get("mcdcpair-coverage"))
        self.assertEqual("100.0% (2/2)", lines[0].get("condition-coverage"))
        self.assertEqual("100.0% (1/1)", lines[0].get("functioncall-coverage"))
        with mock.patch.object(cobertura, "get_lis_mcdc_pairs_by_line") as fallback:
            cobertura.processStatementBranchMCDC(source, cobertura.etree.Element("lines"), extended=False)
            fallback.assert_not_called()


if __name__ == "__main__":
    unittest.main()
