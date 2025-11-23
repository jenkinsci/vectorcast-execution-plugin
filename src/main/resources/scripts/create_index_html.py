#
# The MIT License
#
# Copyright 2025 Vector Informatik, GmbH.
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
import os
import sys
import argparse
import glob

from vcast_utils import dump, checkVectorCASTVersion, getVectorCASTEncoding

encFmt = getVectorCASTEncoding()

class cd:
    """Context manager for changing the current working directory"""
    def __init__(self, newPath):
        self.newPath = os.path.expanduser(newPath)

    def __enter__(self):
        self.savedPath = os.getcwd()
        os.chdir(self.newPath)

    def __exit__(self, etype, value, traceback):
        os.chdir(self.savedPath)

def searchKeyword(search_string, filename):
    with open(filename, "rb") as fd:
        for line_number, line in enumerate(fd, start=1):
            line = line.decode(encFmt, "replace")
            if search_string in line:
                start_idx = line.find(search_string)
                if start_idx != -1: 
                    start_idx += len(search_string)
                    
                end_idx = line[start_idx:].find("<")
                
                if end_idx != -1: 
                    end_idx += start_idx

                return line_number, start_idx, end_idx, line
                
    return -1, -1, -1, line  # not found

def getEnvName(search_string, filename):
    report_name = None
    line_number, start_idx, end_idx, line = searchKeyword("<tr><th>Environment Name</th><td>",filename)
    
    if line_number == -1:
        env_name = None
    else:
        env_name = line[start_idx:end_idx]
        
    return env_name

def getReportName(filename):
    
    reportName = filename
    reportType = 0
    
    if searchKeyword(">Aggregate Coverage Report<", filename)[0] != -1:
        env_name = getEnvName("<tr><th>Environment Name</th><td>",filename)
        if env_name == None:
            reportName = "Aggregate Coverage Report"
        else:
            reportName = "Aggregate Coverage Report {}".format(env_name)
            reportType = 1
        
    elif searchKeyword(">Full Status Section<", filename)[0] != -1:
        reportName = "Full Status Report"
        
    elif searchKeyword("Manage Incremental Rebuild Report", filename)[0] != -1:
        reportName = "Incremental Report Report"
    
    elif searchKeyword(">Metrics Report<", filename)[0] != -1:
        reportName = "Metrics Report"
    
    elif searchKeyword(">Test Case Summary Report<", filename)[0] != -1:
        reportName = "System Test Status Report"
    
    elif searchKeyword(">PC-Lint Plus Results<", filename)[0] != -1:
        reportName = "PC-Lint Plus Results"
    
    elif searchKeyword(">PC-Lint Plus Results<", filename)[0] != -1:
        reportName = "PC-Lint Plus Results"
    
    elif searchKeyword(">Full Report<", filename)[0] != -1:
        reportName = "Full Report "
        reportName += getEnvName("<tr><th>Environment Name</th><td>",filename)
        
        reportType = 1

    else:
        reportType = 2
        
    return reportName, reportType

usingGitLabCI = False
baseOutputDir = ""

def create_index_html(mpName, isGitLab = False, output_dir = ""):
    from vector.apps.DataAPI.vcproject_api import VCProjectApi
    from vector.apps.ReportBuilder.custom_report import CustomReport

    global usingGitLabCI
    usingGitLabCI = isGitLab
    
    global baseOutputDir
    baseOutputDir = output_dir
    
    vcproj = VCProjectApi(mpName)
    
    # Set custom report directory to the where this script was
    # found. Must contain sections/index_section.py
    rep_path = os.path.abspath(os.path.dirname(__file__))

    output_file=os.path.join(baseOutputDir,"index.html")
        
    CustomReport.report_from_api(
            api=vcproj,
            title="HTML Reports",
            report_type="INDEX_FILE",
            formats=["HTML"],
            output_file=output_file,
            sections=['CUSTOM_HEADER', 'REPORT_TITLE', 'TABLE_OF_CONTENTS','INDEX_SECTION', 'CUSTOM_FOOTER'],
            customization_dir=rep_path)
    vcproj.close()
   
def create_index_html_body ():
    
    tempHtmlReportList =  glob.glob(os.path.join(baseOutputDir,"*.html"))
    tempHtmlReportList += glob.glob(os.path.join(baseOutputDir,"html_reports/*.html"))
    tempHtmlReportList += glob.glob(os.path.join(baseOutputDir,"management/*.html"))

    htmlReportList = []
    try:
        prj_dir = os.environ['CI_PROJECT_DIR'].replace("\\","/") + "/"
    except:
        prj_dir = os.getcwd().replace("\\","/") + "/"
    for report in tempHtmlReportList:
        if "index.html" not in report:
            report = report.replace("\\","/")
            report = report.replace(prj_dir,"")
            htmlReportList.append(report)
   
    entries = []
    topLevelEntries = []
    indEnvFullEntries = []
    indEnvTcmrEntries = []
    miscEntries = []

    for html_file_name in htmlReportList:
        
        reportName, reportType = getReportName(html_file_name)
        
        if usingGitLabCI:
            html_file_name = os.path.join("..", html_file_name)
        
        if reportType == 1:
            indEnvFullEntries.append((reportName,html_file_name))
        elif reportType == 2:
            miscEntries.append((reportName,html_file_name))
        elif reportType == 3:
            indEnvTcmrEntries.append((reportName,html_file_name))
        else:
            topLevelEntries.append((reportName,html_file_name))
        
    return topLevelEntries, indEnvFullEntries, indEnvTcmrEntries, miscEntries
    
    
def run(mpName):

    print("Creating index.html for VectorCAST Project Reports")

    create_index_html (mpName)

    return 0


def main():
    parser = argparse.ArgumentParser()    
    parser.add_argument('ManageProject',       help='Manager Project Name')
    args = parser.parse_args()   
    
    
    mpName = args.ManageProject
    try:
        prj_dir = os.environ['CI_PROJECT_DIR'].replace("\\","/") + "/"
    except:
        prj_dir = os.getcwd().replace("\\","/") + "/"
                
    return run(mpName)

if __name__ == "__main__" :
    sys.exit (main())