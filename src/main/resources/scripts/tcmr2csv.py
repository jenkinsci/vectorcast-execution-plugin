#tcmr2csv.py

import re
import argparse
import os
import sys
import cgi

# adding path
jenkinsScriptHome = os.getenv("WORKSPACE") + os.sep + "vc_scripts"
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)
python_path_updates += os.sep + "vpython-addons"
sys.path.append(python_path_updates)

from bs4 import BeautifulSoup

#global variables
global manageProjectName
manageProjectName = ""

# column constants
UNIT_NAME_COL = 0
SUBPROG_COL = 1
TEST_CASE_COL = 2
DATE_TIME_COL = 3
TC_STATUS_COL = 4

# optional columns
reasonCol = -1
reqCol = -1
notesCol = -1

NPBS = '&#160;'
BLANK = ' '

# setup strings so the file name will look like [dir]/[report_type][jobname_][HtmlReportNameRoot].csv
# example:
#   input: 
#       HtmlReportName: c:\test\test.html
#       jobName: MinGW_TestSuite
#       reportType: test_results_
#   output: c:\test_results_MinGW_TestSuite_test.csv

def getCsvName(HtmlReportName,level,reportType):
    #setup the CSV file name depending on if we have a level modifier
    # split the directory name from the root file name
    
    #get the manage project name by getting the basename less the "_management_report.html"    
    envName = os.path.basename(HtmlReportName)[:-23]
    jobName = envName + "_" + level[2] + "_" + level[3].rstrip()
    
    (root, ext) = os.path.splitext(os.path.basename(HtmlReportName))
    
    if jobName:
        jobName = jobName + "_"
    else:
        jobName = ""

    CsvFileName = reportType + jobName + root + ".csv"
    return CsvFileName

## Process the Test Results Section of the Test Case Managemenet Report
def procTestResults(HtmlReportName, table, level):

    global manageProjectName
    
    #get the manage project name by getting the basename less the "    
    envName = os.path.basename(HtmlReportName)[:-23]

    #setup the filename
    CsvFileName = getCsvName(HtmlReportName,level,"test_results_")
    
    csv_file = open(CsvFileName,"w")
    
    #write out additional info
    csv_file.write("Project," + manageProjectName + "\n")
    csv_file.write("Environment," + envName + "\n")
    csv_file.write("Level," + "/".join(level).rstrip() + "\n")
    csv_file.write("HtmlFilename," + HtmlReportName + "\n")

    # setup BeautifulSoup processor for input table
    tableSoup = BeautifulSoup(table.encode('ascii'),'html.parser')
    
    # Get Column Titles 
    columnTitles = []

    # navigate to Table's 2nd <tr> tag then to the <tr> tag inside that
    # Input Table
    #   <tr>
    #   <tr>
    #       <tr>
    # and process the children which are <td> info

    dataTable = tableSoup.tr.next_sibling.tr
    for child in dataTable.children:
        columnTitles.append(child.string.encode('ascii','ignore'))
        
    # write the titles to the CSV file
    csv_file.write(columnTitles[UNIT_NAME_COL] + "," + columnTitles[SUBPROG_COL] + "," + columnTitles[TEST_CASE_COL] + "," + columnTitles[TC_STATUS_COL] + "\n")

    unitName = ""
    subpName = ""
    
    # navigate to Table's 2nd <tr> tag then to the <tr> tag inside that
    # Input Table
    #   <tr>
    #   <tr>
    #       <tr>
    #       <tr>
    # and process the children which are <td> info

    dataEntry = tableSoup.tr.next_sibling.tr.next_sibling
    
    while dataEntry is not None:
      
        # grab the info inside each of the <td> tags
        info = [child.string.encode('ascii','xmlcharrefreplace').replace(NPBS,BLANK) for child in dataEntry.children]
    
        # go to the next <td>
        dataEntry = dataEntry.next_sibling
        
        # fix up any blank fields that results from TCMR only printing unit/subprogram once
        if info[UNIT_NAME_COL] == BLANK:
            info[UNIT_NAME_COL] = unitName
        elif info[UNIT_NAME_COL] != unitName:
            unitName = info[0]            
            
        if info[SUBPROG_COL] == BLANK:
            info[SUBPROG_COL] = subpName
        elif info[SUBPROG_COL] != subpName:
            subpName = info[SUBPROG_COL]
        
        # fix up <<COMPOUND>>, and <<INIT>> only having a unit, no subprogram
        if info[UNIT_NAME_COL] == '<<COMPOUND>>':
            info[SUBPROG_COL] = '<<COMPOUND>>'
        if info[UNIT_NAME_COL] == '<<INIT>>':
            info[SUBPROG_COL] = '<<INIT>>'
        
        # skip totals 
        if 'TOTALS' in info[UNIT_NAME_COL]:
            continue
        if info[TC_STATUS_COL] == BLANK:
            continue  

        # take of subprogram and test cases that have , in them like contructors
        if "," in info[SUBPROG_COL]:
            info[SUBPROG_COL] = info[SUBPROG_COL].replace(",","%2C")
        if "," in info[TEST_CASE_COL]:
            info[TEST_CASE_COL] = info[TEST_CASE_COL].replace(",","%2C")
            
        # write data to the CSV file
        csv_file.write(info[UNIT_NAME_COL] + "," + info[SUBPROG_COL] + "," + info[TEST_CASE_COL] + "," + info[TC_STATUS_COL] + "\n")
   
    csv_file.close()
    
    return CsvFileName

## Process the Coverage Results Section of the Test Case Management Report
def procCoverageResults(HtmlReportName,table, level):
    global manageProjectName
    #get the manage project name by getting the basename less the "    
    envName = os.path.basename(HtmlReportName)[:-23]
    
    #setup the filename
    CsvFileName = getCsvName(HtmlReportName,level,"coverage_results_")
    csv_file = open(CsvFileName,"w")
    
    #write out additional info
    csv_file.write("Project," + manageProjectName + "\n")
    csv_file.write("Environment," + envName + "\n")
    csv_file.write("Level," + "/".join(level).rstrip() + "\n")
    csv_file.write("HtmlFilename," + HtmlReportName + "\n")

    # setup BeautifulSoup processor for input table
    tableSoup = BeautifulSoup(table.encode('ascii'),'html.parser')

    # Get Column Titles 
    columnTitles = []
        
    # navigate to Table's 3rd <tr> tag then to the <tr> tag inside that
    # Input Table
    #   <tr>
    #   <tr>
    #   <tr>
    #       <tr>
    # and process the children which are <td> info
    try:
        if tableSoup.tr.next_sibling.next_sibling is None:
            dataTable = tableSoup.tr.next_sibling.tr
        else:
            dataTable = tableSoup.tr.next_sibling.next_sibling.tr        
    except AttributeError:
        csv_file.write("No Coverage Found\n")
        csv_file.close()
        print "No Coverage Found"
        return None
        
    titleStr = ""
    
    # remember the complexity column index so we can split the coverage info into multiple cells in CSV
    complexityIndex = -1
    idx = 0
    
    try:
        # process the <td> tags
        for child in dataTable.children:
        
            # if we haven't found the complexity yet...
            if complexityIndex == -1:
                # write out the information directly 
                titleStr = titleStr + child.string.encode('ascii','ignore') + ","

                # check if this field is the complexity
                if "Complexity" in child.string:
                    complexityIndex = idx
            else:
                # otherwise write it out as Covered, Total, Percent
                str = child.string.encode('ascii','ignore')
                titleStr = titleStr + str + " Covered," + str + " Total," + str + " Percent,"
                
            idx += 1
    except AttributeError as e:
        print "Error with Test Case Management Report: " + HtmlReportName
        csv_file.write("Error with Test Case Management Report:\n")
        csv_file.close()
        return None
        
    # write out the title information except for the trailing comma
    csv_file.write(titleStr[:-1] + "\n")

    # navigate to Table's 3rd <tr> tag then to the <tr> tag inside that
    # Input Table
    #   <tr>
    #   <tr>
    #   <tr>
    #       <tr>
    #       <tr>
    # and process the children which are <td> info
    if tableSoup.tr.next_sibling.next_sibling is None:
        dataEntry = tableSoup.tr.next_sibling.tr.next_sibling
    else:
        dataEntry = tableSoup.tr.next_sibling.next_sibling.tr.next_sibling
    unitName = ""

    # loop over the <td> tags
    while dataEntry is not None:
    
        # grab the info inside each of the <td> tags
        info = [child.string.encode('ascii','xmlcharrefreplace').replace(NPBS,BLANK) for child in dataEntry.children]

        # move to next <td> tag
        dataEntry = dataEntry.next_sibling
        
        # skip TOTALS and blank lines
        if 'TOTALS' in info[UNIT_NAME_COL]:
            continue
            
        # fix up any blank fields that results from TCMR only printing unit once
        if info[UNIT_NAME_COL] == BLANK:
            info[UNIT_NAME_COL] = unitName
        elif info[UNIT_NAME_COL] != unitName:
            unitName = info[0]
            
        if info[SUBPROG_COL] == "    Analysis" or info[SUBPROG_COL] == "    Execution":
            continue;
        
        # take of subprogram and test cases that have , in them like contructors
        if "," in info[SUBPROG_COL]:
            info[SUBPROG_COL] = info[SUBPROG_COL].replace(",","%2C")

        # process each of the fields in the <td> tag
        dataStr = ""
        idx = 0
        
        for item in info:
            # if we haven't passed complexity yet...
            if idx <= complexityIndex:
                # save data normally
                dataStr = dataStr + item + ","
            else:
                # else 
                if item != BLANK:
                    #split the data into covered, total, percent
                    if "(" in item:
                        covered,na,total,percent = item.split()

                        # remove the () from around the percent field
                        percent = re.sub("[()]","",percent)
                    elif item == "100%":
                        percent = item
                        covered = "1"
                        total = "1"
                    else:
                        percent = "0"
                        covered = "0"
                        total = "1"
                        
                    dataStr = dataStr  + covered + "," + total + "," + percent + ","
                        
                else:
                    # handle blank field
                    dataStr = dataStr  + "," + "," + ","
            
            idx += 1
            
        if not ', , ,' in dataStr:
            # write data to CSV file
            csv_file.write(dataStr[:-1] + "\n")
        
    csv_file.close()
    return CsvFileName
    
    
def run(HtmlReportName = "", jobName = ""):

    TestResultsName = None
    CoverageResultsName = None

    # verify the html report exists
    if not os.path.isfile(HtmlReportName):
        raise IOError(HtmlReportName + ' does not exist')
        return
        
    # open the file and create BS4 object
    html_file = open(HtmlReportName,"r")
    html_doc = html_file.read()
    html_file.close()
    soup = BeautifulSoup(html_doc,'html.parser')

    # find all tables and loop over
    tables = soup.findAll('table')

    # loop over all the tables in the TCMR
    for table in tables:
        # If the table doesn't have a <span> and <strong> tag -- continue
        try:
            span = table.find('span')
            title = span.find('strong')
        except:
            continue
            
        # if the title contains Testcase*Management in the title
        if re.search("Testcase.*Management",title.encode('ascii')) is not None:
            #print "   Processing Test Case Results from: " + os.path.basename(HtmlReportName)
            TestResultsName = procTestResults(HtmlReportName,table, jobName)

        # if the title contains Metrics in the title
        if re.search("Metrics",title.encode('ascii')) is not None:
            #print "   Processing Coverage Results from: " + os.path.basename(HtmlReportName)
            CoverageResultsName = procCoverageResults(HtmlReportName,table, jobName)
            
    return TestResultsName,CoverageResultsName
    
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('HtmlReportName', help='HTML Report Name')
    parser.add_argument('--manage-project',   help='Location of Manage Project that generated report (optional)')
    parser.add_argument('--level',   help='Jenkins Job Name Identifier(Source/Platform/Compiler/Testsuite) (optional')
    parser.add_argument('--local',   help='Creates CVS files locally', action="store_true")
    
    args = parser.parse_args()
        
    if args.manage_project:
        manageProjectName = args.manage_project
    else:
        manageProjectName = "NoManageProject"
        
    if args.level:
        level = args.level
    else:
        level = "source/windows/compiler/testsuite"
        
    run(args.HtmlReportName,level.split("/"))
