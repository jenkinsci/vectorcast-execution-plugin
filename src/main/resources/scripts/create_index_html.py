import os
import sys
import argparse
import glob

report_style="html{line-height:1.15;-webkit-text-size-adjust:100%}\n" \
"body{margin:0}\n" \
"h1{font-size:2em;margin:.67em 0}\n" \
"hr{box-sizing:content-box;height:0;overflow:visible}\n" \
"pre{font-family:monospace,monospace;font-size:1em}\n" \
"a{background-color:transparent}\n" \
"abbr[title]{border-bottom:none;text-decoration:underline;text-decoration:underline dotted}\n" \
"b,strong{font-weight:bolder}\n" \
"code,kbd,samp{font-family:monospace,monospace;font-size:1em}\n" \
"small{font-size:80%}\n" \
"sub,sup{font-size:75%;line-height:0;position:relative;vertical-align:baseline}\n" \
"sub{bottom:-.25em}\n" \
"sup{top:-.5em}\n" \
"img{border-style:none}\n" \
" html {box-sizing:border-box;position:relative;height:100%;width:100%;}\n" \
"*, *:before, *:after {box-sizing:inherit;}\n" \
"body {position:relative;height:100%;width:100%;font-size:10pt;font-family:helvetica, Arial, sans-serif;color:#3a3e3f;}\n" \
".alternate-font {font-family:Arial Unicode MS, Arial, sans-serif;}\n" \
"#page {position:relative;width:100%;height:100%;overflow:hidden;}\n" \
"#title-bar {position:absolute;top:0px;left:0em;right:0px;height:1.8em;background-color:#B1B6BA;white-space:nowrap;box-shadow:1px 1px 5px black;z-index:100;}\n" \
"#report-title {font-size:3em;text-align:center;font-weight:bold;background-color:white;padding:0.5em;margin-bottom:0.75em;border:1px solid #e5e5e5;}\n" \
".contents-block {position:absolute;top:1.8em;left:0em;width:XXem;bottom:0em;overflow:auto;background-color:#DADEE1;border-right:1px solid silver;padding-left:0.75em;padding-right:0.5em;}\n" \
".report-body {position:absolute;top:1.8em;left:22em;right:0em;bottom:0em;padding-left:2em;padding-right:2em;overflow:auto;padding-bottom:1.5em;background-color:#DADEE1;}\n" \
".report-body.no-toc {left:0em;}\n" \
".report-body > .report-block, .report-body > .report-block-coverage, .report-body > .report-block-scroll, .report-body > .testcase {border:1px solid #e5e5e5;margin-bottom:2em;padding-bottom:1em;padding-right:2em;background-color:white;padding-left:2em;padding-top:0.1em;margin-top:1em;}\n" \
".report-body > .report-block-scroll {overflow-x:visible;background-color:inherit;}\n" \
".title-bar-heading {display:none;position:absolute;text-align:center;width:100%;color:white;font-size:3em;bottom:0px;margin-bottom:0.3em;}\n" \
".title-bar-logo {display:inline-block;height:100%;}\n" \
".title-bar-logo img {width:120px;margin-left:0.5em;margin-top:-16px;}\n" \
".contents-block ul {padding-left:1.5em;list-style-type:none;line-height:1.5;}\n"

report_title="   <!-- ReportTitle -->\n" \
'   <div id="title-bar">\n' \
'    <div class="title-bar-logo">\n' \
'      <img alt="Vector" src="data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAxNi4wLjAsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjwhRE9DVFlQRSBzdmcgUFVCTElDICItLy9XM0MvL0RURCBTVkcgMS4xLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL0dyYXBoaWNzL1NWRy8xLjEvRFREL3N2ZzExLmR0ZCI+DQo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IldPUlQtX3gyRl9CSUxETUFSS0Vfc1JHQiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayINCgkgeD0iMHB4IiB5PSIwcHgiIHdpZHRoPSI0MTUuMTc5cHgiIGhlaWdodD0iMTk3LjU3MnB4IiB2aWV3Qm94PSIwIDAgNDE1LjE3OSAxOTcuNTcyIiBlbmFibGUtYmFja2dyb3VuZD0ibmV3IDAgMCA0MTUuMTc5IDE5Ny41NzIiDQoJIHhtbDpzcGFjZT0icHJlc2VydmUiPg0KPHBvbHlnb24gaWQ9IkJpbGRtYXJrZV8xODNfeDJGXzBfeDJGXzUwIiBmaWxsPSIjQjcwMDMyIiBwb2ludHM9IjI5Mi4yODgsMTE1LjI1IDI5Mi4yODgsMTMxLjcxNSAzNDkuMzIyLDk4Ljc4NiAyOTIuMjg4LDY1Ljg1NyANCgkyOTIuMjg4LDgyLjMyMSAzMjAuODA1LDk4Ljc4NiAiLz4NCjxwb2x5Z29uIHBvaW50cz0iOTAuNTU0LDg0LjUyOSA5OC43ODcsODQuNTI5IDgyLjMyMiwxMTMuMDQ3IDY1Ljg1Nyw4NC41MjkgNzQuMDg5LDg0LjUyOSA4Mi4zMjIsOTguNzg4ICIvPg0KPHBhdGggZD0iTTIxNy43NTQsODMuODE5Yy04LjI2OSwwLTE0Ljk3Miw2LjcwMy0xNC45NzIsMTQuOTcyYzAsOC4yNjksNi43MDMsMTQuOTcxLDE0Ljk3MiwxNC45NzENCgljOC4yNjksMCwxNC45NzItNi43MDMsMTQuOTcyLTE0Ljk3MUMyMzIuNzI2LDkwLjUyMiwyMjYuMDIzLDgzLjgxOSwyMTcuNzU0LDgzLjgxOXogTTIxNy43NTUsMTA2LjY2OA0KCWMtNC4zNTEsMC03Ljg3OC0zLjUyNy03Ljg3OC03Ljg3OGMwLTQuMzUxLDMuNTI3LTcuODc3LDcuODc4LTcuODc3YzQuMzUxLDAsNy44NzgsMy41MjcsNy44NzgsNy44NzcNCglDMjI1LjYzMywxMDMuMTQyLDIyMi4xMDUsMTA2LjY2OCwyMTcuNzU1LDEwNi42Njh6Ii8+DQo8cGF0aCBkPSJNMTU1LjEyMSwxMDYuNjY4Yy00LjM1MSwwLTcuODc4LTMuNTI3LTcuODc4LTcuODc4YzAtNC4zNTEsMy41MjctNy44NzcsNy44NzgtNy44NzdjMi4xNzQsMCw0LjE0MywwLjg4MSw1LjU2OCwyLjMwNQ0KCWw1LjAxNi01LjAxNmMtMi43MDktMi43MDgtNi40NTEtNC4zODMtMTAuNTg0LTQuMzgzYy04LjI2OSwwLTE0Ljk3Miw2LjcwMy0xNC45NzIsMTQuOTcyYzAsOC4yNjksNi43MDMsMTQuOTcxLDE0Ljk3MiwxNC45NzENCgljNC4xMzYsMCw3Ljg3OS0xLjY3NiwxMC41ODktNC4zODdsLTUuMDE2LTUuMDE2QzE1OS4yNjgsMTA1Ljc4NiwxNTcuMjk3LDEwNi42NjgsMTU1LjEyMSwxMDYuNjY4eiIvPg0KPHBvbHlnb24gcG9pbnRzPSIxNzEuNDQ2LDkwLjk0NiAxODEuMDcxLDkwLjk0NiAxODEuMDcxLDExMy4wNDcgMTg4LjIwMSwxMTMuMDQ3IDE4OC4yMDEsOTAuOTQ2IDE5Ny44MjUsOTAuOTQ2IDE5Ny44MjUsODQuNTI5IA0KCTE3MS40NDYsODQuNTI5ICIvPg0KPHBvbHlnb24gcG9pbnRzPSIxMDguMDY5LDExMy4wNDcgMTMwLjE3LDExMy4wNDcgMTMwLjE3LDEwNi42MyAxMTUuMTk4LDEwNi42MyAxMTUuMTk4LDEwMS42NCAxMjYuODkxLDEwMS42NCAxMjYuODkxLDk1LjM2NiANCgkxMTUuMTk4LDk1LjM2NiAxMTUuMTk4LDkwLjk0NiAxMzAuMTcsOTAuOTQ2IDEzMC4xNyw4NC41MjkgMTA4LjA2OSw4NC41MjkgIi8+DQo8cGF0aCBkPSJNMjcwLjE4Nyw5NC4wNDdjMC01LjI1LTQuMjU1LTkuNTE4LTkuNTA1LTkuNTE4aC0xNy41ODd2MjguNTE4aDcuMTI5di05LjQ4Mmg2LjI0NGw1LjQ4Nyw5LjQ4Mmg4LjIzMWwtNS44OTktMTAuMjMxDQoJQzI2Ny43NDgsMTAxLjM5NSwyNzAuMTg3LDk4LjAyLDI3MC4xODcsOTQuMDQ3eiBNMjYwLjAyMyw5Ny4yMmgtOS43OTh2LTYuMzQ2aDkuNzk2YzEuNjg5LDAuMDc0LDMuMDM3LDEuNDY2LDMuMDM3LDMuMTczDQoJQzI2My4wNTgsOTUuNzUzLDI2MS43MTEsOTcuMTQ1LDI2MC4wMjMsOTcuMjJ6Ii8+DQo8L3N2Zz4NCg=="/>\n' \
'      </div>\n' \
'    </div>\n' 

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
    
    if "aggregate" in filename:
        manageProject = filename.split("_aggregate",1)[0]
        reportName = " Aggregate Coverage Report"
        
    elif "environment" in filename:
        manageProject = filename.split("_environment",1)[0]
        reportName = " Environment Report"
        
    elif "manage_incremental_rebuild_report" in filename:
        manageProject = filename.split("_manage_incremental_rebuild_report",1)[0]
        reportName = "Incremental Report Report"
    
    elif "metrics" in filename:
        manageProject = filename.split("_metrics",1)[0]
        reportName = " Metrics Report"
    
    elif  "html_reports" in filename:
        ## html_reports/VectorCAST_MinGW_C++_UnitTesting_ENV_LINKED_LIST.html
        comp_ts_env = filename.replace("html_reports/","").replace(".html","")
        reportName = "Full Report: "+ comp_ts_env 
        
    return reportName
    
def create_index_html (html_file_list):

    ## No idea what this is for
    global report_style
    global report_title
    maxlen = 1
    for html_file_name in html_file_list :
        if len(html_file_name) > maxlen :
            maxlen = len(html_file_name)
    # somewhat ad-hoc calculation        
    maxlen /= 2
    maxlen += 4
    
    indexHtmlText = ""
    
    indexHtmlText += "<!DOCTYPE html>\n"
    indexHtmlText += "<!-- VectorCAST Report header -->\n"
    indexHtmlText += '<html lang="en">\n'
    indexHtmlText += " <head>\n"
    indexHtmlText += "    <title>Index</title>\n"
    indexHtmlText += '    <meta charset="utf-8"/>\n'
    indexHtmlText += "    <style>\n"
    indexHtmlText += report_style.replace("XXem","{}em".format(str(maxlen)))
    indexHtmlText += "    </style>\n"
    indexHtmlText += " </head>\n"
    indexHtmlText += " <body>\n"
    
    indexHtmlText += report_title
    
    indexHtmlText += "  <!-- TableOfContents -->\n"
    indexHtmlText += "    <div class='contents-block'>\n"
#    indexHtmlText += '    <a id="TableOfContents"></a>' 
    indexHtmlText += "    <h3>VectorCAST HTML Reports</h3>\n"
    
    indexHtmlText += "    <table>\n"
    indexHtmlText += "    <tbody>\n"  

    for html_file_name in html_file_list:
        reportName = getReportName(html_file_name)
        line = '    <tr><td><a href="{}">{}</a></td></tr>\n'.format(html_file_name, reportName)        
        indexHtmlText += line
        
    indexHtmlText += "    </tbody>\n"
    indexHtmlText += "    </table>\n"
    indexHtmlText += "    </div>\n"

    indexHtmlText += "  </div>\n"  
    
    indexHtmlText += " </body>\n"
    indexHtmlText += "</html>\n"
    
    return indexHtmlText

def run(html_file_list):

    print("Creating index.html for VectorCAST Project Reports")

    if len(html_file_list) > 0:
        indexHtmlText = create_index_html (html_file_list)
        
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
    ret = main()
    sys.exit (ret)