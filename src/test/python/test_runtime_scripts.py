"""Focused tests for the packaged reporting scripts (run with vpython)."""

import ast
import contextlib
import importlib.util
import io
import logging
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
import create_index_html
import full_report_no_toc
import incremental_build_report_aggregator
import merge_vcr
import parse_console_for_cbt
import runtime_logging
import vcast_exec

spec = importlib.util.spec_from_file_location(
    "generate_results", SCRIPT_DIR / "generate-results.py")
generate_results = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generate_results)


class MergeVcrPathTest(unittest.TestCase):
    def test_external_result_is_staged_under_new_vcr_directory(self):
        external_result = os.path.abspath("external.vcr")
        with mock.patch.object(merge_vcr.os, "makedirs"), \
             mock.patch.object(merge_vcr.shutil, "copyfile",
                               side_effect=RuntimeError("stop after staging")) as copy:
            with self.assertRaisesRegex(RuntimeError, "stop after staging"):
                merge_vcr.run("original.vcr", external_result, "merged.vcr", False)
        self.assertEqual(os.path.join("newVcr", "external.vcr"),
                         copy.call_args.args[1])


class IndexReportPathTest(unittest.TestCase):
    def test_cli_uses_explicit_workspace_directory(self):
        with mock.patch.object(sys, "argv", ["create_index_html.py", "Project.vcm",
                                             "--output-dir", "C:/Jenkins Workspace"]), \
             mock.patch.object(create_index_html, "run", return_value=0) as run:
            self.assertEqual(0, create_index_html.main())
        run.assert_called_once_with("Project.vcm", "C:/Jenkins Workspace")

    def test_report_is_written_to_workspace_root(self):
        workspace = "C:/Jenkins Workspace"
        with mock.patch("vector.apps.DataAPI.vcproject_api.VCProjectApi") as project, \
             mock.patch("vector.apps.ReportBuilder.custom_report.CustomReport.report_from_api") as report, \
             mock.patch.object(create_index_html, "baseOutputDir", ""):
            create_index_html.create_index_html("Project.vcm", output_dir=workspace)
        self.assertEqual(os.path.join(workspace, "index.html"),
                         report.call_args.kwargs["output_file"])
        project.return_value.close.assert_called_once_with()


class RuntimeScriptsTest(unittest.TestCase):
    def test_all_packaged_scripts_parse_as_python_39(self):
        for path in sorted(SCRIPT_DIR.rglob("*.py")):
            with self.subTest(script=path.name):
                ast.parse(path.read_text(encoding="utf-8-sig"),
                          filename=str(path), feature_version=(3, 9))

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

    def test_version_23_or_newer_is_required_and_api_is_closed(self):
        for version, accepted in (("23", True), ("23sp7", True),
                                  ("23.sp7 (02/13/24)", True),
                                  ("24", True), ("24sp4", True),
                                  ("24.sp4", True), ("25.sp4", True),
                                  ("26.sp4 (09/07/26)", True),
                                  ("22", False), ("22.sp7", False),
                                  ("230", False), ("unknown", False)):
            with self.subTest(version=version):
                api = mock.Mock(tool_version=version)
                with mock.patch.object(generate_results, "VCProjectApi",
                                       return_value=api):
                    if accepted:
                        generate_results.require_supported_data_api("project.vcm")
                    else:
                        with self.assertRaisesRegex(RuntimeError, "23 or newer DataAPI"):
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

    def test_vcast_exec_passes_build_log_cbt_data_to_junit(self):
        Path("unstashed_build.log").write_text("CBT log line\n", encoding="utf-8")
        executor = object.__new__(vcast_exec.VectorCASTExecute)
        executor.FullMP = "Project.vcm"
        executor.buildlog = "unstashed_build.log"
        executor.encFmt = "utf-8"
        executor.verbose = False
        executor.print_exc = False
        executor.timing = False
        executor.level = None
        executor.environment = None
        executor.generate_individual_reports = True
        executor.no_start_line = True
        executor.ci = ""
        executor.xml_data_dir = "xml_data"
        executor.cobertura = False
        executor.cobertura_extended = True
        executor.useJunitFailCountPct = False
        executor.needIndexHtml = False
        with mock.patch.object(parse_console_for_cbt, "ParseConsoleForCBT") as parser, \
             mock.patch.object(vcast_exec.generate_results, "buildReports",
                               return_value=(1, 3)) as reports, \
             mock.patch.object(vcast_exec, "checkVectorCASTVersion", return_value=True):
            parser.return_value.parse.return_value = {"env": "skipped"}
            executor.runJunitMetrics()
        parser.return_value.parse.assert_called_once()
        self.assertEqual(["CBT log line"],
                         [line.strip() for line in parser.return_value.parse.call_args.args[0]])
        self.assertEqual({"env": "skipped"}, reports.call_args.kwargs["cbtDict"])
        self.assertFalse(reports.call_args.kwargs["generate_coverage"])
        self.assertFalse(reports.call_args.kwargs["useStartLine"])
        self.assertEqual((1, 3), (executor.failed_count, executor.passed_count))

    def test_vcast_exec_finishes_only_requested_jenkins_reports(self):
        executor = object.__new__(vcast_exec.VectorCASTExecute)
        executor.FullMP = "Project.vcm"
        executor.mpName = "Project"
        executor.verbose = True
        executor.fixup_reports = True
        executor.aggregate_rebuild = True
        with mock.patch.object(full_report_no_toc, "fixup_full_status_reports") as fixup, \
             mock.patch.object(incremental_build_report_aggregator,
                               "parse_html_files", return_value=True) as aggregate:
            executor.finishJenkinsReports()
            fixup.assert_called_once_with("Project.vcm")
            aggregate.assert_called_once_with("Project", True)
            executor.aggregate_rebuild = False
            fixup.reset_mock()
            aggregate.reset_mock()
            executor.finishJenkinsReports()
            fixup.assert_called_once_with("Project.vcm")
            aggregate.assert_not_called()

    def test_vcast_exec_uses_jenkins_full_report_filename_when_fixing_up(self):
        executor = object.__new__(vcast_exec.VectorCASTExecute)
        executor.mpName = "Project"
        executor.aggregate = False
        executor.metrics = True
        executor.fullstatus = True
        executor.fixup_reports = True
        executor.needIndexHtml = False
        executor.manageWait = mock.Mock()
        executor.runReports()
        commands = [call.args[0] for call in
                    executor.manageWait.exec_manage_command.call_args_list]
        self.assertIn("--create-report=metrics --output=Project_metrics_report.html",
                      commands)
        self.assertIn("--full-status=Project_full_report.html", commands)

    def test_full_status_fixup_creates_both_summary_fragments(self):
        for name in ("Project_full_report.html", "Project_metrics_report.html"):
            Path(name).write_text("<html>report</html>", encoding="utf-8")
        with mock.patch.object(full_report_no_toc.fixup_reports,
                               "fixup_2020_reports") as fixup:
            full_report_no_toc.fixup_full_status_reports("Project.vcm")
        for name in ("Project_full_report.html", "Project_metrics_report.html"):
            self.assertEqual("<html>report</html>", Path(name + "_tmp").read_text())
        self.assertEqual(2, fixup.call_count)

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

    def test_cobertura_reuses_class_for_source_outside_workspace(self):
        cobertura = generate_results.cobertura
        classes = cobertura.etree.Element("classes")
        metrics = mock.Mock(
            branches=0, mcdc_branches=0, max_covered_branches=0,
            max_covered_mcdc_branches=0, max_annotations_branches=0,
            max_annotations_mcdc_branches=0,
            max_covered_statements_pct=50,
            max_covered_mcdc_pairs_pct=0,
            max_covered_function_calls_pct=0,
            max_covered_functions_pct=0, statements=2, complexity=1)
        source = mock.Mock(display_name="source.c", _relative_path="source.c",
                           display_path="C:/qa/project/source.c", metrics=metrics)
        with mock.patch.dict(os.environ, {"WORKSPACE": "C:/qa/workspace"}, clear=True):
            first_methods, first_lines = cobertura.getFileXML(classes, source)
            second_methods, second_lines = cobertura.getFileXML(classes, source)
        self.assertIs(first_methods, second_methods)
        self.assertIs(first_lines, second_lines)
        self.assertEqual(1, len(classes.findall("class")))
        self.assertTrue(classes.find("class").attrib["filename"])

    def test_cobertura_reads_relative_source_paths_outside_workspace(self):
        cobertura = generate_results.cobertura
        source_root = Path(self.workdir.name) / "external"
        source = source_root / "nested" / "source.c"
        source.parent.mkdir(parents=True)
        source.write_text("int source;", encoding="utf-8")
        source_api = mock.Mock(source_path=os.path.join("nested", "source.c"),
                               display_path=os.path.join("nested", "source.c"),
                               realpath=str(source))
        cover_api = mock.Mock()
        cover_api.SourceFile.all.return_value = [source_api]

        original = Path.cwd()
        with cobertura.source_file_directory(cover_api):
            self.assertEqual("int source;",
                             Path(source_api.source_path).read_text(encoding="utf-8"))
            self.assertEqual(source_root, Path.cwd())
        self.assertEqual(original, Path.cwd())
        with self.assertRaisesRegex(RuntimeError, "report failed"):
            with cobertura.source_file_directory(cover_api):
                raise RuntimeError("report failed")
        self.assertEqual(original, Path.cwd())

    def test_cobertura_resolves_environment_paths_from_project_ancestors(self):
        cobertura = generate_results.cobertura
        campaign = Path(self.workdir.name) / "campaign"
        source = campaign / "suite" / "working_dir" / "unit.c"
        source.parent.mkdir(parents=True)
        source.write_text("int unit;", encoding="utf-8")
        project_file = source.parent / "Project.vcm"
        project_file.touch()
        relative = os.path.join("suite", "working_dir", "unit.c")
        source_api = mock.Mock(source_path=relative, display_path=relative,
                               realpath=str(Path.cwd() / relative))
        cover_api = mock.Mock()
        cover_api.SourceFile.all.return_value = [source_api]

        original = Path.cwd()
        with cobertura.source_file_directory(cover_api, str(project_file)):
            self.assertEqual("int unit;", Path(relative).read_text(encoding="utf-8"))
            self.assertEqual(campaign, Path.cwd())
        self.assertEqual(original, Path.cwd())

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
                self.assertIn("vcast_exec.py", content)
                self.assertIn("--cobertura_extended", content)
                self.assertIn("--buildlog", content)
                self.assertIn("--fixup-reports", content)
                self.assertNotIn("/cobertura.py", content)
                self.assertNotIn("\\cobertura.py", content)

        pipeline = (SCRIPT_DIR / "baseJenkinsfile.groovy").read_text(
            encoding="utf-8")
        self.assertNotIn("VectorCASTPublisher", pipeline)
        self.assertNotIn("xml_data/coverage_results*.xml", pipeline)

    def test_incremental_report_aggregator_checks_encoding_before_reports(self):
        root_logger = logging.getLogger()
        existing_handlers = set(root_logger.handlers)
        try:
            with mock.patch.object(incremental_build_report_aggregator,
                                   "getVectorCASTEncoding", return_value="utf-8") as encoding:
                self.assertFalse(
                    incremental_build_report_aggregator.parse_html_files("Project"))
        finally:
            for handler in root_logger.handlers[:]:
                if handler not in existing_handlers:
                    root_logger.removeHandler(handler)
                    handler.close()
        encoding.assert_called_once_with()

    def test_pipeline_archives_junit_before_coverage_and_skips_empty_coverage(self):
        pipeline = (SCRIPT_DIR / "baseJenkinsfile.groovy").read_text(
            encoding="utf-8")
        self.assertIn("coverageReport?.contains('<packages/>')", pipeline)
        self.assertLess(pipeline.index("JUnitResultArchiver"),
                        pipeline.index("recordCoverage"))
        self.assertLess(pipeline.index("archiveArtifacts allowEmptyArchive"),
                        pipeline.index("recordCoverage"))

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
            factory.return_value.tool_version = "22.sp4"
            with self.assertRaisesRegex(RuntimeError, "23 or newer DataAPI"):
                generate_results.buildReports("project.vcm")
        self.assertTrue(Path("xml_data/previous.xml").exists())


if __name__ == "__main__":
    unittest.main()
