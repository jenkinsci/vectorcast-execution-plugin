from __future__ import print_function
from __future__ import absolute_import

import sys, os
from managewait import ManageWait

def generate_full_status(manageProject): 

    mpName = os.path.splitext(os.path.basename(manageProject))[0]
    report_name = mpName + "_full_report.html"
    try:
        from vector.apps.DataAPI.vcproject_api import VCProjectApi
        api = VCProjectApi(manageProject)
        from vector.apps.ReportBuilder.custom_report import CustomReport
        CustomReport.report_from_api(api, report_type="Demo", formats=["HTML"], output_file=report_name, sections=["CUSTOM_HEADER", "REPORT_TITLE", "MANAGE_CONFIG_DATA", "MANAGE_STATUS_FULL", "CUSTOM_FOOTER"], environments=api.Environment.all(), levels = [] )

    except:
    
        cmd = "--project " + manageProject + " --full-status=" + report_name
        manageWait = ManageWait(false, command_line, 30, 1)
        return manageWait.exec_manage(silent)
        
if __name__ == '__main__':
    manageProject = sys.argv[1]
    generate_full_status(manageProject)
    