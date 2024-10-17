import os
import sys
import argparse
import glob


class cd:
    """Context manager for changing the current working directory"""
    def __init__(self, newPath):
        self.newPath = os.path.expanduser(newPath)

    def __enter__(self):
        self.savedPath = os.getcwd()
        os.chdir(self.newPath)

    def __exit__(self, etype, value, traceback):
        os.chdir(self.savedPath)

def getReportName(filename):
    
    reportName = filename
    reportType = 0
    
    if "aggregate" in filename:
        manageProject = filename.split("_aggregate",1)[0]
        reportName = "Aggregate Coverage Report"
        
    elif "full_status" in filename:
        manageProject = filename.split("_aggregate",1)[0]
        reportName = "Full Status Report"
        
    elif "environment" in filename:
        manageProject = filename.split("_environment",1)[0]
        reportName = "Environment Report"
        
    elif "manage_incremental_rebuild_report" in filename:
        manageProject = filename.split("_manage_incremental_rebuild_report",1)[0]
        reportName = "Incremental Report Report"
    
    elif "metrics" in filename:
        manageProject = filename.split("_metrics",1)[0]
        reportName = "Metrics Report"
    
    elif  "html_reports" in filename or "management" in filename:
        ## html_reports/VectorCAST_MinGW_C++_UnitTesting_ENV_LINKED_LIST.html
        comp_ts_env = filename.replace("html_reports/","").replace("management/","").replace(".html","")
        reportName = comp_ts_env 
        reportType = 1
    else:
        reportType = 2
        
    return reportName, reportType

def create_index_html(mpName):
    import pathlib
    from vector.apps.DataAPI.vcproject_api import VCProjectApi
    from vector.apps.ReportBuilder.custom_report import CustomReport
    with VCProjectApi(mpName) as api:
        # Set custom report directory to the where this script was
        # found. Must contain sections/index_section.py
        rep_path = pathlib.Path(__file__).parent.resolve()
        output_file="index.html"
        CustomReport.report_from_api(
                api=api,
                title="HTML Reports",
                report_type="INDEX_FILE",
                formats=["HTML"],
                output_file=output_file,
                sections=['CUSTOM_HEADER', 'REPORT_TITLE', 'TABLE_OF_CONTENTS','INDEX_SECTION', 'CUSTOM_FOOTER'],
                customization_dir=rep_path)

def create_index_html_body ():
    
    tempHtmlReportList = glob.glob("*.html")
    tempHtmlReportList += glob.glob("html_reports/*.html")
    tempHtmlReportList += glob.glob("management/*.html")

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
    miscEntries = []

    for html_file_name in htmlReportList:
        
        reportName, reportType = getReportName(html_file_name)
        
        if reportType == 1:
            indEnvFullEntries.append((reportName,html_file_name))
        elif reportType == 2:
            miscEntries.append((reportName,html_file_name))
        else:
            topLevelEntries.append((reportName,html_file_name))
        
    return topLevelEntries, indEnvFullEntries, miscEntries
    
    
def run(html_file_list):

    print("Creating index.html for VectorCAST Project Reports")

    if len(html_file_list) > 0:
        create_index_html (html_file_list)
        
    else:
        print("No HTML reports found")
        return 1
        
    try:
        with open("index.html", 'w') as fd:
            fd.write(indexHtmlText)
    except:
        print("Unable to write to index.html")
        return 1
    
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--html_base_dir", help='Set the base directory of the html_reports directory. The default is the workspace directory', default = "html_reports")
    args = parser.parse_args()   
    
    try:
        prj_dir = os.environ['CI_PROJECT_DIR'].replace("\\","/") + "/"
    except:
        prj_dir = os.getcwd().replace("\\","/") + "/"
    
    tempHtmlReportList = glob.glob("*.html")
    tempHtmlReportList += glob.glob(os.path.join(args.html_base_dir, "*.html"))
    htmlReportList = []

    for report in tempHtmlReportList:
        if "index.html" not in report:
            report = report.replace("\\","/")
            report = report.replace(prj_dir,"")
            htmlReportList.append(report)
            
    return run(htmlReportList)

if __name__ == "__main__" :
    if len(sys.argv) > 0:
        ret = create_custom_index_html()
    else:
        ret = main()
    sys.exit (ret)