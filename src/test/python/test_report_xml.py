"""Contract tests for the XML consumed by Jenkins reporting plugins."""

from pathlib import Path
import sys
import tempfile
import unittest
from types import SimpleNamespace
from unittest import mock
from xml.etree import ElementTree as ET


SCRIPT_DIR = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts"
sys.path.insert(0, str(SCRIPT_DIR))

from generate_junit import GenerateManageXml, GenerateXml, generate_environment_junit
from report_xml import JUnitReport


class ReportXmlTest(unittest.TestCase):
    def test_standalone_environment_generates_only_junit(self):
        for api in (object(), None):
            with self.subTest(api_available=api is not None):
                generator = mock.Mock(api=api)
                logger = mock.Mock()
                generate_environment_junit(
                    generator, "project", "environment", "test_results.xml", logger)
                if api is None:
                    generator.generate_unit.assert_not_called()
                else:
                    generator.generate_unit.assert_called_once_with()
                    logger.info.assert_called_once_with(
                        "\nJunit plugin for Jenkins compatible file generated: test_results.xml")
                generator.generate_cover.assert_not_called()

    def test_manage_imported_results_use_documented_full_status(self):
        project_status = {"testcase_results": {
            "total_count": 3, "success_count": 2}}
        imported = {"name": 'external & "source"', "testcase_results": {
            "total_count": 3, "success_count": 2}}
        environment = SimpleNamespace(
            is_active=True, name="environment",
            compiler=SimpleNamespace(name="compiler"),
            testsuite=SimpleNamespace(name="suite"),
            level=SimpleNamespace(full_status={
                "local": {}, "imported": {"one": imported}}))
        api = SimpleNamespace(
            project=SimpleNamespace(tree=SimpleNamespace(full_status=project_status)),
            Environment=SimpleNamespace(all=lambda: [environment]),
            close=lambda: None)
        generator = object.__new__(GenerateManageXml)
        generator.api = api
        generator.manageProjectName = "project"
        generator.encFmt = "utf-8"
        generator.failed_count = 0
        generator.passed_count = 0
        generator.skipReporting = mock.Mock(return_value=False)
        with tempfile.TemporaryDirectory() as directory:
            generator.unit_report_name = str(Path(directory) / "imported.xml")
            generator.generate_testresults()
            root = ET.parse(generator.unit_report_name).getroot()

        cases = root.findall("testsuite/testcase")
        self.assertEqual(3, len(cases))
        self.assertEqual(2, len([case for case in cases if case.find("skipped") is not None]))
        self.assertEqual(1, len([case for case in cases if case.find("failure") is not None]))
        self.assertIn('external & "source"', cases[0].get("name"))
        self.assertEqual("3", root.find("testsuite").get("tests"))
        self.assertEqual((2, 1), (generator.passed_count, generator.failed_count))

    def test_imported_results_supply_counts_when_project_has_no_summary(self):
        imported = {"name": "external", "testcase_results": {
            "total_count": 2, "success_count": 1}}
        environment = SimpleNamespace(
            is_active=True, name="environment",
            compiler=SimpleNamespace(name="compiler"),
            testsuite=SimpleNamespace(name="suite"),
            level=SimpleNamespace(full_status={
                "local": {}, "imported": {"one": imported}}))
        generator = object.__new__(GenerateManageXml)
        generator.api = SimpleNamespace(
            project=SimpleNamespace(tree=SimpleNamespace(full_status={
                "testcase_results": {}})),
            Environment=SimpleNamespace(all=lambda: [environment]),
            close=lambda: None)
        generator.manageProjectName = "project"
        generator.encFmt = "utf-8"
        generator.failed_count = generator.passed_count = 0
        generator.skipReporting = mock.Mock(return_value=False)
        with tempfile.TemporaryDirectory() as directory:
            generator.unit_report_name = str(Path(directory) / "imported.xml")
            generator.generate_testresults()
            root = ET.parse(generator.unit_report_name).getroot()

        self.assertEqual((1, 1), (generator.passed_count, generator.failed_count))
        self.assertEqual("2", root.find("testsuite").get("tests"))
        self.assertEqual(2, len(root.findall("testsuite/testcase")))

    def test_environment_testcase_is_written_as_valid_junit_xml(self):
        summary = SimpleNamespace(
            expected_total=2, expected_fail=1, control_flow_total=0,
            control_flow_fail=0, signals=0, unexpected_exceptions=0)
        testcase = SimpleNamespace(
            name='test <& "one"', passed=False,
            history=SimpleNamespace(summary=summary),
            testcase_status="TCR_STATUS_OK", status="TC_EXECUTION_FAILED")
        generator = object.__new__(GenerateXml)
        generator.api = SimpleNamespace(environment=SimpleNamespace(
            get_option=lambda option: False))
        generator.noResults = False
        generator.report_failed_only = False
        generator.cbtDict = None
        generator.generate_exec_rpt_each_testcase = False
        generator.compiler = "compiler"
        generator.testsuite = "suite"
        generator.env = "environment"
        generator.use_cte = False
        generator.junit_report = JUnitReport("environment", 1, 1, 1)

        generator.write_testcase(testcase, "unit &", "function <")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "test.xml"
            generator.junit_report.write(path, "utf-8")
            root = ET.parse(path).getroot()
        case = root.find("testsuite/testcase")
        self.assertEqual('unit &.function <.test <& "one"', case.get("name"))
        self.assertIsNotNone(case.find("failure"))
        self.assertIn("Execution Report disabled", case.find("system-out").text)


if __name__ == "__main__":
    unittest.main()
