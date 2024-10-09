import sys

from vector.apps.ReportBuilder.report_section import ReportSection
from generate_pclp_reports import generate_source

class PclpSourceSection(ReportSection):
    title = 'PC-Lint Plus Source'
    supported_environments = ('COVER', 'UNIT', 'MANAGE')
    def prepare_data(self):
    #def write(self, format_streams, after_contents_streams):
        # Use write rather than prepare_data to write the html directly
        
        # Set data to be available when writing the report
        # Create list of tuples, (title, link)
        # Format as HTML with styles etc. in the template
        sourceHTML, srcFnameLink = generate_source()
        self.section_context["sourceHTML"] = sourceHTML
  
        for item in srcFnameLink:
            self.contents_table_entries[self.title].append(item)

##    <h4 id="CurrentRelease_database_src_database_c">Coverage for </h4>


# "title" : "CurrentRelease_database_src_database_c"
# "link"  : "CurrentRelease\database\src\database.c"