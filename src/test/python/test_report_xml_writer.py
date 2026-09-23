"""JUnit XML serialization checks that do not require a VectorCAST installation."""

from pathlib import Path
import sys
import tempfile
import unittest
from xml.etree import ElementTree as ET


SCRIPT_DIR = Path(__file__).resolve().parents[2] / "main" / "resources" / "scripts"
sys.path.insert(0, str(SCRIPT_DIR))

from report_xml import JUnitReport


class JUnitReportTest(unittest.TestCase):
    def test_schema_and_failure_message_escaping(self):
        report = JUnitReport('suite & "one"', 1, 2, 1)
        report.add_case('test <one>', 'class & "one"', file='a&b.c', line=12,
                        status="failure", failure_message='expected "a" < b',
                        output='first\nsecond & third')
        report.add_case("not run", "class", status="skipped")

        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "junit.xml"
            report.write(path, "utf-8")
            root = ET.parse(path).getroot()

        suite = root.find("testsuite")
        self.assertEqual('suite & "one"', suite.get("name"))
        self.assertEqual("2", suite.get("tests"))
        case = suite.find("testcase")
        self.assertEqual("test <one>", case.get("name"))
        self.assertEqual("a&b.c", case.get("file"))
        self.assertEqual('expected "a" < b', case.find("failure").get("message"))
        self.assertEqual("first\nsecond & third", case.find("system-out").text)
        self.assertIsNotNone(suite.findall("testcase")[1].find("skipped"))


if __name__ == "__main__":
    unittest.main()
