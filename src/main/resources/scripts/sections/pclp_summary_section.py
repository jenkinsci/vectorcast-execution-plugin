import sys

from vector.apps.ReportBuilder.report_section import ReportSection
from generate_pclp_reports import generate_summaries

class PclpSummarySection(ReportSection):
    title = 'PC-Lint Plus Summary'
    supported_environments = ('COVER', 'UNIT', 'MANAGE')
    def prepare_data(self):
    #def write(self, format_streams, after_contents_streams):
        # Use write rather than prepare_data to write the html directly
        
        # Set data to be available when writing the report
        # Create list of tuples, (title, link)
        # Format as HTML with styles etc. in the template
        summaryHtml = generate_summaries()
        self.section_context["summaryHtml"] = summaryHtml