"""Regression examples distilled from four PointOfSales build-execute logs.

The full logs contain workspace paths and much unrelated build output. These
excerpts preserve the event ordering used by the CBT parser without checking
in machine-specific data.
"""

import hashlib
from pathlib import Path
import sys
import unittest


SCRIPT_DIR = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts"
sys.path.insert(0, str(SCRIPT_DIR))

from parse_console_for_cbt import ParseConsoleForCBT


def options_file(build_id):
    return ("Processing options file "
            "C:\\workspace\\project\\build\\{}\\CCAST_.CFG".format(build_id))


def environment_key(build_id):
    return hashlib.md5("BUILD/{}".format(build_id).encode("utf-8")).hexdigest()


class ParseConsoleForCbtTest(unittest.TestCase):
    def parse(self, *lines):
        return ParseConsoleForCBT().parse(lines)

    def test_full_serial_execution(self):
        # build.log: ENV_ENCRYPT executes both cases before the end marker.
        data = self.parse(
            options_file("1476887732"),
            "Running all encrypt.transmit_Info test cases",
            "Running: encrypt.transmit_Info.failure",
            "Test Execution Complete",
            "Running: encrypt.transmit_Info.good",
            "Test Execution Complete",
            "Completed Incremental Execution processing",
            "Running: should_not_be_recorded")

        cases = data[environment_key("1476887732")][2]
        self.assertEqual({
            "encrypt/transmit_Info/encrypt.transmit_Info.failure",
            "encrypt/transmit_Info/encrypt.transmit_Info.good"}, set(cases))
        self.assertTrue(all(end is not None for start, end in cases.values()))

    def test_full_parallel_execution_keeps_environment_sections_separate(self):
        # parallel.log: command sections are grouped by environment.
        data = self.parse(
            options_file("3907815837"),
            "Running all manager.Add_Included_Dessert test cases",
            "Running: Add_Included_Dessert.001",
            "Test Execution Complete",
            "Running all manager.Place_Order test cases",
            "Running: LobserFailTest",
            "Test Execution Complete",
            "Completed Batch Execution processing",
            options_file("1749245873"),
            "Running all linked_list.RemoveAllDataItems test cases",
            "Running: RemoveAllDataItems.001",
            "Test Execution Complete")

        manager = data[environment_key("3907815837")][2]
        linked_list = data[environment_key("1749245873")][2]
        self.assertEqual({
            "manager/Add_Included_Dessert/Add_Included_Dessert.001",
            "manager/Place_Order/LobserFailTest"}, set(manager))
        self.assertEqual(
            {"linked_list/RemoveAllDataItems/RemoveAllDataItems.001"},
            set(linked_list))

    def test_incremental_serial_records_environment_with_no_executed_cases(self):
        # inc-build.log: ENV_ENCRYPT has a scope announcement but no Running:.
        data = self.parse(
            options_file("1476887732"),
            "Running all encrypt.transmit_Info test cases",
            "Completed Incremental Execution processing",
            options_file("3907815837"),
            "Running all manager.Place_Order test cases",
            "Running: Place_Order.001",
            "Test Execution Complete",
            "Completed Incremental Execution processing")

        self.assertEqual({}, data[environment_key("1476887732")][2])
        self.assertEqual(
            {"manager/Place_Order/Place_Order.001"},
            set(data[environment_key("3907815837")][2]))

    def test_incremental_parallel_handles_adjacent_scopes_and_compounds(self):
        # inc-parallel.log: adjacent scopes precede the first executed case.
        data = self.parse(
            options_file("3907815837"),
            "Running all manager.Add_Included_Dessert test cases",
            "Running all manager.Place_Order test cases",
            "Running: LobserFailTest",
            "Test Execution Complete",
            "Completed Incremental Execution processing",
            options_file("2483108167"),
            "Running all <<COMPOUND>> test cases",
            "Running: FuzzTesting.001",
            "Test Execution Complete",
            "Completed Incremental Execution processing")

        self.assertEqual(
            {"manager/Place_Order/LobserFailTest"},
            set(data[environment_key("3907815837")][2]))
        self.assertEqual(
            {"<<COMPOUND>>/<<COMPOUND>>/FuzzTesting.001"},
            set(data[environment_key("2483108167")][0]))

    def test_system_results_and_empty_initialization_scope(self):
        # parallel.log: system-test results are announced by Adding result file.
        # inc-parallel.log: an init scope may contain no executed init case.
        data = self.parse(
            options_file("106522577"),
            "Adding result file C:\\tests\\TESTINSS.DAT as Add Free Dessert",
            "Adding result file C:\\tests\\TESTINSS.DAT as _Sequence",
            "Creating report in project_manage_incremental_rebuild_report.html.",
            "Adding result file C:\\tests\\TESTINSS.DAT as ignored",
            options_file("3846379353"),
            "Running all auto_init.<<INIT>> test cases",
            "Running all manager.coded_tests_driver test cases",
            "Running: tutorial_coded_test",
            "Test Execution Complete")

        self.assertEqual(
            {"Add Free Dessert", "_Sequence"},
            set(data[environment_key("106522577")][2]))
        self.assertEqual({}, data[environment_key("3846379353")][1])
        self.assertEqual(
            {"manager/coded_tests_driver/tutorial_coded_test"},
            set(data[environment_key("3846379353")][2]))


if __name__ == "__main__":
    unittest.main()
