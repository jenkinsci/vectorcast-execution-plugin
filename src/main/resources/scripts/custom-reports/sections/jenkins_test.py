import os
from vector.apps.ReportBuilder.sections.testcase_management import TestcaseManagement

class JenkinsTest(TestcaseManagement):
    title = 'Jenkins Test'

    def prepare_data(self):
        super(JenkinsTest, self).prepare_data()

        if "BUILD_URL" in os.environ:
            self.section_context["build_url"] = os.getenv('BUILD_URL') + "artifact/execution/" + os.environ["JENKINS_LINK_NAME"] + ".html#ExecutionResults_"
        else:
            self.section_context["build_url"] = "undefined"
