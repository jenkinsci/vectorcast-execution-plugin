"""XML writer for Jenkins JUnit reports.

DataAPI traversal stays in generate_junit.py; this module owns only XML structure
and serialization so values are escaped by the XML library, not by callers.
"""

from xml.etree import ElementTree as ET


class JUnitReport:
    """A single Jenkins JUnit suite with test cases and optional diagnostics."""

    def __init__(self, name, errors, tests, failures):
        self.root = ET.Element("testsuites")
        self.suite = ET.SubElement(self.root, "testsuite", {
            "errors": str(errors), "tests": str(tests),
            "failures": str(failures), "name": name, "id": "1"})

    def add_case(self, name, classname, time="0", file=None, line=None,
                 status=None, failure_message="", output=None):
        attributes = {"name": name, "classname": classname, "time": str(time)}
        if file is not None:
            attributes["file"] = file
        if line is not None:
            attributes["line"] = str(line)
        testcase = ET.SubElement(self.suite, "testcase", attributes)
        if status == "skipped":
            ET.SubElement(testcase, "skipped")
        elif status == "failure":
            ET.SubElement(testcase, "failure", {
                "type": "failure", "message": failure_message})
        if output is not None:
            ET.SubElement(testcase, "system-out").text = output

    def set_counts(self, errors, tests, failures):
        self.suite.set("errors", str(errors))
        self.suite.set("tests", str(tests))
        self.suite.set("failures", str(failures))

    def write(self, path, encoding):
        ET.ElementTree(self.root).write(path, encoding=encoding,
                                        xml_declaration=True)
