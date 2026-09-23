"""Focused tests for the packaged reporting scripts (run with vpython)."""

import contextlib
import importlib.util
import io
import os
from pathlib import Path
import sys
import tarfile
import tempfile
import unittest
from unittest import mock


SCRIPT_DIR = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts"
sys.path.insert(0, str(SCRIPT_DIR))

import check_build_log
import archive_extract_reports
import runtime_logging
import vcast_exec

spec = importlib.util.spec_from_file_location(
    "generate_results", SCRIPT_DIR / "generate-results.py")
generate_results = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generate_results)


class RuntimeScriptsTest(unittest.TestCase):
    def setUp(self):
        logger = runtime_logging._LOGGER
        for handler in logger.handlers[:]:
            logger.removeHandler(handler)
            handler.close()
        self.previous_directory = Path.cwd()
        self.workdir = tempfile.TemporaryDirectory()
        os.chdir(self.workdir.name)

    def tearDown(self):
        logger = runtime_logging._LOGGER
        for handler in logger.handlers[:]:
            logger.removeHandler(handler)
            handler.close()
        os.chdir(self.previous_directory)
        self.workdir.cleanup()

    def test_messages_and_exceptions_reach_console_and_build_log(self):
        console = io.StringIO()
        with contextlib.redirect_stdout(console):
            runtime_logging.get_logger().info("ordinary diagnostic")
            try:
                raise RuntimeError("broken report")
            except RuntimeError:
                runtime_logging.log_exception("compiler", "suite", "env", "build")

        recorded = Path("command.log").read_text(
            encoding=runtime_logging.locale.getpreferredencoding(False))
        for output in (console.getvalue(), recorded):
            self.assertIn("ordinary diagnostic", output)
            self.assertIn("Jenkins integration error", output)
            self.assertIn("Traceback (most recent call last)", output)
            self.assertIn("broken report", output)
        self.assertEqual(2, check_build_log.check_build_log("command.log"))

    def test_version_24_or_newer_is_required_and_api_is_closed(self):
        for version, accepted in (("24", True), ("24sp4", True),
                                  ("24.sp4", True), ("25.sp4", True),
                                  ("26.sp4 (09/07/26)", True),
                                  ("23.sp4", False), ("unknown", False)):
            with self.subTest(version=version):
                api = mock.Mock(tool_version=version)
                with mock.patch.object(generate_results, "VCProjectApi",
                                       return_value=api):
                    if accepted:
                        generate_results.require_supported_data_api("project.vcm")
                    else:
                        with self.assertRaisesRegex(RuntimeError, "24 or newer DataAPI"):
                            generate_results.require_supported_data_api("project.vcm")
                api.close.assert_called_once_with()

    def test_parallel_execution_uses_manage_jobs_on_current_vectorcast(self):
        executor = object.__new__(vcast_exec.VectorCASTExecute)
        executor.FullMP = "project.vcm"
        executor.build_execute = "--build-execute"
        executor.useCBT = ""
        executor.importedResults = None
        executor.useLevelEnv = False
        executor.mpName = "project"
        executor.jobs = "4"
        executor.level_option = ""
        executor.env_option = ""
        executor.build_log_name = "build.log"
        executor.encFmt = "utf-8"
        executor.manageWait = mock.Mock()
        executor.manageWait.exec_manage_command.return_value = "build complete"

        with mock.patch.object(vcast_exec, "checkVectorCASTVersion",
                               return_value=True), \
             mock.patch.dict(sys.modules, {"parallel_build_execute": None}):
            executor.runExec()

        commands = [call.args[0] for call in
                    executor.manageWait.exec_manage_command.call_args_list]
        self.assertTrue(any("--build-execute" in command and "--jobs=4" in command
                            for command in commands))
        self.assertEqual("build complete", Path("build.log").read_text())

    def test_pre_25_parallel_execution_requires_vectorcast_module(self):
        executor = object.__new__(vcast_exec.VectorCASTExecute)
        executor.FullMP = "project.vcm"
        executor.build_execute = "--build-execute"
        executor.useCBT = ""
        executor.importedResults = None
        executor.useLevelEnv = False
        executor.mpName = "project"
        executor.jobs = "4"
        executor.manageWait = mock.Mock()

        with mock.patch.object(vcast_exec, "checkVectorCASTVersion",
                               side_effect=lambda version, quiet=False: version <= 24), \
             mock.patch.dict(sys.modules, {"parallel_build_execute": None}):
            with self.assertRaisesRegex(RuntimeError, "before VectorCAST 25"):
                executor.runExec()

    def test_report_driver_writes_counts_from_manage_api(self):
        Path("project.vcm").touch()
        with mock.patch.object(generate_results, "require_supported_data_api") as validate, \
             mock.patch.object(generate_results, "cleanupOldBuilds") as cleanup, \
             mock.patch.object(generate_results, "useManageAPI",
                               return_value=(7, 2)) as report, \
             mock.patch.object(generate_results.cobertura,
                               "generateCoverageResults") as coverage:
            counts = generate_results.buildReports(
                "project.vcm", extended_coverage=True)

        self.assertEqual((2, 7), counts)
        self.assertEqual("2", Path("unit_test_fail_count.txt").read_text())
        self.assertEqual("7 2", Path("unit_test_passfail_count.txt").read_text())
        validate.assert_called_once_with("project.vcm")
        cleanup.assert_called_once()
        report.assert_called_once()
        coverage.assert_called_once_with(
            "project.vcm", xml_data_dir="xml_data", extended=True)

    def test_manage_api_generates_junit_without_old_coverage_format(self):
        report = mock.Mock()
        report.api = object()
        report.passed_count = 9
        report.failed_count = 1
        with mock.patch("generate_junit.GenerateManageXml", return_value=report) as factory:
            counts = generate_results.useManageAPI(
                "project.vcm", {"cbt": "data"}, True, False, True,
                False, False, runtime_logging.get_logger(), True)

        self.assertEqual((9, 1), counts)
        factory.assert_called_once()
        self.assertEqual("project.vcm", factory.call_args.args[0])
        self.assertEqual({"cbt": "data"}, factory.call_args.args[2])
        report.generate_testresults.assert_called_once_with()

    def test_jenkins_detection_selects_extended_cobertura(self):
        for env, expected in (({}, False),
                              ({"JENKINS_URL": "https://jenkins.example"}, True),
                              ({"BUILD_URL": "https://jenkins.example/job/1"}, True)):
            with self.subTest(env=env):
                self.assertEqual(expected, generate_results.running_in_jenkins(env))

    def test_default_coverage_is_standard_outside_jenkins_and_can_be_skipped(self):
        Path("project.vcm").touch()
        with mock.patch.dict(os.environ, {}, clear=True), \
             mock.patch.object(generate_results, "require_supported_data_api"), \
             mock.patch.object(generate_results, "cleanupOldBuilds"), \
             mock.patch.object(generate_results, "useManageAPI", return_value=(1, 0)), \
             mock.patch.object(generate_results.cobertura,
                               "generateCoverageResults") as coverage:
            generate_results.buildReports("project.vcm")
            coverage.assert_called_once_with(
                "project.vcm", xml_data_dir="xml_data", extended=False)
            coverage.reset_mock()
            generate_results.buildReports("project.vcm", generate_coverage=False)
            coverage.assert_not_called()

    def test_existing_jenkins_jobs_get_extended_coverage_without_new_flag(self):
        Path("project.vcm").touch()
        with mock.patch.dict(os.environ, {"JENKINS_URL": "https://jenkins.example"},
                             clear=True), \
             mock.patch.object(generate_results, "require_supported_data_api"), \
             mock.patch.object(generate_results, "cleanupOldBuilds"), \
             mock.patch.object(generate_results, "useManageAPI", return_value=(1, 0)), \
             mock.patch.object(generate_results.cobertura,
                               "generateCoverageResults") as coverage:
            generate_results.buildReports("project.vcm")
        coverage.assert_called_once_with(
            "project.vcm", xml_data_dir="xml_data", extended=True)

    def test_generated_jenkins_jobs_request_coverage_once(self):
        templates = (
            SCRIPT_DIR / "baselineSingleJobLinux.txt",
            SCRIPT_DIR / "baselineSingleJobWindows.txt",
            SCRIPT_DIR.parent / "com" / "vectorcast" / "plugins"
            / "vectorcastexecution" / "default-scripts" / "VectorCASTMetrics.groovy",
        )
        for template in templates:
            with self.subTest(template=template.name):
                content = template.read_text(encoding="utf-8")
                self.assertIn("generate-results.py", content)
                self.assertIn("--extended", content)
                self.assertNotIn("/cobertura.py", content)
                self.assertNotIn("\\cobertura.py", content)

        pipeline = (SCRIPT_DIR / "baseJenkinsfile.groovy").read_text(
            encoding="utf-8")
        self.assertNotIn("VectorCASTPublisher", pipeline)
        self.assertNotIn("xml_data/coverage_results*.xml", pipeline)

    def test_report_archive_includes_cobertura_xml_and_dtd(self):
        Path("management").mkdir()
        Path("xml_data/cobertura").mkdir(parents=True)
        for name in ("management/report.html", "xml_data/test_results.xml",
                     "xml_data/cobertura/coverage_results.xml",
                     "xml_data/cobertura/coverage-extended-04.dtd"):
            Path(name).touch()
        files = {name.replace("\\", "/") for name in
                 archive_extract_reports.report_files()}
        self.assertIn("xml_data/cobertura/coverage_results.xml", files)
        self.assertIn("xml_data/cobertura/coverage-extended-04.dtd", files)

    def test_report_archive_does_not_restore_old_vectorcast_coverage(self):
        legacy = Path("xml_data/coverage_results_old.xml")
        modern = Path("xml_data/cobertura/coverage_results_new.xml")
        modern.parent.mkdir(parents=True)
        legacy.write_text("old")
        modern.write_text("new")
        with tarfile.open(archive_extract_reports.archive_name, "w") as archive:
            archive.add(legacy, arcname=legacy.as_posix())
            archive.add(modern, arcname=modern.as_posix())
        legacy.unlink()
        modern.unlink()

        archive_extract_reports.extract()

        self.assertFalse(legacy.exists())
        self.assertEqual("new", modern.read_text())

    def test_unsupported_version_does_not_delete_old_reports(self):
        Path("project.vcm").touch()
        Path("xml_data").mkdir()
        Path("xml_data/previous.xml").write_text("preserve")
        with mock.patch.object(generate_results, "VCProjectApi") as factory:
            factory.return_value.tool_version = "23.sp4"
            with self.assertRaisesRegex(RuntimeError, "24 or newer DataAPI"):
                generate_results.buildReports("project.vcm")
        self.assertTrue(Path("xml_data/previous.xml").exists())


if __name__ == "__main__":
    unittest.main()
