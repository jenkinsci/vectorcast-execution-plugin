from vector.apps.ReportBuilder.custom_report import CustomReport

class JenkinsCoverageReport(CustomReport):
    title = 'Jenkins Coverage Report'

    @classmethod
    def get_format_streams(cls, output_file, environment, formats=None):
        format_streams = {}
        format_streams["TEXT"] = open(output_file, 'w')
        return format_streams

    def initialize(self, **kwargs):
        self.include_sections('JENKINS_COVERAGE')
