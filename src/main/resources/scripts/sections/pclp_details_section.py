import sys

from vector.apps.ReportBuilder.report_section import ReportSection
from generate_pclp_reports import generate_details

class PclpDetailsSection(ReportSection):
    title = 'PC-Lint Plus Details'
    supported_environments = ('COVER', 'UNIT', 'MANAGE')
    def prepare_data(self):
        
        # Set data to be available when writing the report
        # Create list of tuples, (title, link)
        # Format as HTML with styles etc. in the template
        detailHTML = generate_details()
        self.section_context["detailHTML"] = detailHTML
                