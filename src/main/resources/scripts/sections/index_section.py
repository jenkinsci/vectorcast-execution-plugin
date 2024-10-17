import sys

from vector.apps.ReportBuilder.report_section import ReportSection
from create_index_html import create_index_html_body

class IndexSection(ReportSection):
    title = 'HTML Reports'
    supported_environments = ('COVER', 'UNIT', 'MANAGE')
    def prepare_data(self):
    #def write(self, format_streams, after_contents_streams):
        # Use write rather than prepare_data to write the html directly
        
        # Set data to be available when writing the report
        # Create list of tuples, (title, link)
        # Format as HTML with styles etc. in the template
        topLevelEntries, indEnvFullEntries, miscEntries = create_index_html_body()
        self.section_context["topLevel"] = topLevelEntries
        self.section_context["indFull"] = indEnvFullEntries
        self.section_context["misc"] = miscEntries
        
        contentOverall = {
            "title" : "Overall Reports",
            "link" : "overall", 
            }
            
        contentFullReports = { 
            "title": "Individual Environment Full Reports",
            "link" : "fullreports"
        }

        contentMiscHtml = { 
            "title": "Miscellaneous HTML docs",
            "link" : "misc"
        }

        self.contents_table_entries[self.title].append(contentOverall)
        self.contents_table_entries[self.title].append(contentFullReports)
        self.contents_table_entries[self.title].append(contentMiscHtml)
