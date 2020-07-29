#
# The MIT License
#
# Copyright 2020 Vector Software, East Greenwich, Rhode Island USA
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#

from __future__ import division
from __future__ import print_function

import argparse
import os
import sys
import fixup_reports
import shutil

def generate_full_status(manageProject): 

    mpName = os.path.splitext(os.path.basename(manageProject))[0]
    report_name = mpName + "_full_report.html"
    try:
        from vector.apps.DataAPI.vcproject_api import VCProjectApi
        api = VCProjectApi(manageProject)
        from vector.apps.ReportBuilder.custom_report import CustomReport
        
        CustomReport.report_from_api(api, report_type="MANAGE_STATUS_FULL_REPORT", formats=["HTML"], output_file=report_name, environments=api.Environment.all(), levels = [] )
        shutil.copy(report_name,report_name + "_tmp")
        fixup_reports.fixup_2020_reports(report_name + "_tmp")
        
    except:
        from managewait import ManageWait

        cmd = "--project " + manageProject + " --full-status=" + report_name
        manageWait = ManageWait(False, cmd, 30, 1)
        return manageWait.exec_manage(True)
        
if __name__ == '__main__':
    manageProject = sys.argv[1]
    generate_full_status(manageProject)
    