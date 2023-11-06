#
# The MIT License
#
# Copyright 2016 Vector Software, East Greenwich, Rhode Island USA
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
#vcastcsv2jenkins.py

from __future__ import division
from __future__ import print_function

import argparse
import glob
import os
import sys
import time
import shutil

import cgi
from safe_open import open

# adding path
workspace = os.getenv("WORKSPACE")
if workspace is None:
    workspace = os.getcwd()
jenkinsScriptHome = os.path.join(workspace,"vc_scripts")
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)
# needed because vc18 vpython does not have bs4 package
if sys.version_info[0] < 3:
    python_path_updates += os.sep + 'vpython-addons'
    sys.path.append(python_path_updates)

from xml.sax.saxutils import escape

# column constants
UNIT_NAME_COL = 0
SUBPROG_COL   = 1
TEST_CASE_COL = 2
TC_STATUS_COL = 3

testCaseString = """
    <test result=\"%s\">
        <fn>%s</fn>
        <id>%s</id>
        <iteration>1</iteration>
        <description>Simple Test Case</description>
        <message>%s</message>
    </test>
"""

global manageProject
global level
global compiler
global jobName
global jobNamePrefix
global tcmrFilename
global fullManageProject
global testCaseCount
global jobNameDotted
global manageVersion
manageVersion = 14

global stIndex,brIndex,pairIndex,pathIndex,baIndex,fncIndex,fncCallIndex,VgIndex

global COVERED_INDEX, TOTAL_INDEX, PERCENT_INDEX,UNIT_INDEX, SUBP_INDEX
COVERED_INDEX = 0
TOTAL_INDEX = 1
PERCENT_INDEX = 2
UNIT_INDEX = 0
SUBP_INDEX = 1


testCaseCount = 0
tcmrFilename = ""
manageProject = ""
level = ""
jobNamePrefix = ""
jobNameDotted = ""
fullManageProject = ""

stIndex = brIndex = pairIndex = pathIndex = baIndex = fncCallIndex = fncCallIndex = VgIndex = -1
global gUseExecRpt
gUseExecRpt = True

def readCsvFile(csvFilename):
    global fullManageProject
    global manageProject
    global compiler
    global jobName
    global level
    global envName
    global jobNamePrefix
    global jobNameDotted

    #mode = 'r' if sys.version_info[0] >= 3 else 'rb'
    with open(csvFilename, "r") as fd:
        csvList = fd.readlines()
    os.remove(csvFilename)

    fullManageProject = csvList[0].split(",")[1].rstrip()
    (manageProject, ext) = os.path.splitext(os.path.basename(fullManageProject))
    envName       = csvList[1].split(",")[1].rstrip()
    level         = csvList[2].split(",")[1].rstrip().split('/')
    htmlFilename  = csvList[3].split(",")[1].rstrip()
    if len(level) == 2:
        # Level does not include source and platform
        jobNamePrefix       = '_'.join([level[0],level[1],envName])
        jobName = level[0] + "_" + level[1].rstrip()
        compiler = level[0]
    else:
        # Level includes source and platform
        jobNamePrefix       = '_'.join([level[2],level[3],envName])
        jobName = level[2] + "_" + level[3].rstrip()
        compiler = level[2]

    envName = envName.replace('.','_')

    level[0] = level[0].replace('.','_')
    level[1] = level[1].replace('.','_')
    if len(level) == 2:
        # Level does not include source and platform
        jobNameDotted       = '.'.join([level[0],level[1],envName])
    else:
        # Level includes source and platform
        level[2] = level[2].replace('.','_')
        level[3] = level[3].replace('.','_')
        jobNameDotted       = '.'.join([level[2],level[3],envName])

    dataArray = []
    for row in csvList[4:]:
        if row:
            data = row.strip().split(',')
            dataArray.append(data)

    return dataArray;
    
def writeJunitHeader(dataArray):
    
    global jobNameDotted
    global envName
    
    junitData = ""

    junitData += ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    junitData += ("<testsuites>\n")
    
    errors = 0
    failed = 0
    
    for data in dataArray:
        if 'ABNORMAL' in data[TC_STATUS_COL]:
            errors += 1
      
        elif not 'PASS' in data[TC_STATUS_COL]:
            failed += 1
            
    junitData += ("   <testsuite errors=\"%d\" tests=\"%d\" failures=\"%d\" name=\"%s\" id=\"1\">\n" % 
        (errors,len(dataArray), failed, envName))
        
    return junitData 

 
def writeJunitTestCase(unit, subp, tc_name, passFail):
    global jobNamePrefix
    global testCaseCount
    
    junitData = ""

    testCasePassString ="        <testcase name=\"%s\" classname=\"%s\" time=\"0\"/>\n"
    testCaseFailString ="""
            <testcase name="%s" classname="%s" time="0">
                <failure type="failure" message="FAIL: %s"/>
            </testcase>
    """
    testCaseCount += 1
    
    if 'PASS' in passFail:
        successFailure = 'success'
    else:
        successFailure = 'failure'    

    if 'ABNORMAL' in passFail:
        print("Abnormal Termination on Environment\n")

    unit_subp = unit + "." + subp
   
    if 'PASS' in passFail:
        junitData += (testCasePassString % (tc_name, unit_subp))
    else:   
        junitData += (testCaseFailString % (tc_name, unit_subp, passFail))
       
    return junitData
    
def writeJunitFooter():

    junitData = ""
    junitData += ("   </testsuite>\n")
    junitData += ("</testsuites>\n")
    
    return junitData

def runCsv2JenkinsTestResults(csvFilename, junit):

    dataArray = readCsvFile(csvFilename)
        
    titles = dataArray[0]
       
    junitData = ""
    
    junitData += writeJunitHeader(dataArray[1:])

    for data in dataArray[1:]:
        data[UNIT_NAME_COL] = escape(data[UNIT_NAME_COL])
        data[SUBPROG_COL] = escape(data[SUBPROG_COL])
        data[TEST_CASE_COL] = escape(data[TEST_CASE_COL])
        junitData += writeJunitTestCase(data[UNIT_NAME_COL],data[SUBPROG_COL].replace("%2C",","),data[TEST_CASE_COL].replace("%2C",","),data[TC_STATUS_COL])

    junitData += writeJunitFooter()
    
    with open(csvFilename[:-4]+".xml","w") as fd:
        fd.write(junitData)

def determineCoverage(titles):
    global stIndex,brIndex,pairIndex,pathIndex,baIndex,fncIndex,fncCallIndex,VgIndex

    #determine which coverages are present and which index into the tables they are
    try:
        stIndex = titles.index('Statements Covered')
    except ValueError:
        stIndex = -1

    try:
        brIndex = titles.index('Branches Covered')
    except ValueError:
        brIndex = -1

    try:
        pairIndex = titles.index('Pairs Covered')
    except ValueError:
        pairIndex = -1

    try:
        pathIndex = titles.index('Paths Covered')
    except ValueError:
        pathIndex = -1

    try:
        baIndex = titles.index('ByAnalysis Covered')
    except ValueError:
        baIndex = -1

    try:
        fncIndex = titles.index('FunctionCoverage Covered')
    except ValueError:
        fncIndex = -1

    try:
        fncCallIndex = titles.index('FunctionCalls Covered')
    except ValueError:
        fncCallIndex = -1

    try:
        VgIndex = titles.index('Complexity')
    except ValueError:
        VgIndex = -1

def writeEmmaHeader():
    
    time_tuple = time.localtime()
    date_string = time.strftime("%m/%d/%Y", time_tuple)
    time_string = time.strftime("%I:%M %p", time_tuple)
    datetime_str = date_string + "\t" + time_string
    
    return ("<!-- VectorCAST/Jenkins Integration, Generated " + datetime_str+ " -->\n<report>\n  <version value=\"3\"/>\n")

def countUnitSubp(data):
    unitName   = ""
    unitCount  = 0
    subpCount = 0
    for row in data:
        # if we have a different unit name -- bump up the unit count
        subpCount += 1
        if row[UNIT_INDEX] != unitName:
            unitCount += 1
            unitName = row[UNIT_INDEX]
    return unitCount,subpCount

def calulatePercentages(statement,branch,pair,path,byAnalysis,function,functionCall,complexity):
    try:
        statement [PERCENT_INDEX] = 100 * statement [COVERED_INDEX] // statement[TOTAL_INDEX]
    except:
        pass
    try:
        branch    [PERCENT_INDEX] = 100 * branch    [COVERED_INDEX] // branch[TOTAL_INDEX]
    except:
        pass
    try:
        pair      [PERCENT_INDEX] = 100 * pair      [COVERED_INDEX] // pair[TOTAL_INDEX]
    except:
        pass
    try:
        path      [PERCENT_INDEX] = 100 * path      [COVERED_INDEX] // path[TOTAL_INDEX]
    except:
        pass
    try:
        byAnalysis[PERCENT_INDEX] = 100 * byAnalysis[COVERED_INDEX] // byAnalysis[TOTAL_INDEX]
    except:
        pass
    try:
        function[PERCENT_INDEX] = 100 * function[COVERED_INDEX] // function[TOTAL_INDEX]
    except:
        pass
    try:
        functionCall[PERCENT_INDEX] = 100 * functionCall[COVERED_INDEX] // functionCall[TOTAL_INDEX]
    except:
        pass

    return statement,branch,pair,path,byAnalysis,function,functionCall,complexity


def getCoverageTotals(data,unitName):
    global stIndex,brIndex,pairIndex,pathIndex,baIndex,fncIndex,fncCallIndex,VgIndex
    statement    = [0,0,0]
    branch       = [0,0,0]
    pair         = [0,0,0]
    path         = [0,0,0]
    byAnalysis   = [0,0,0]
    function     = [0,0,0]
    functionCall = [0,0,0]
    complexity   = [0,0,0]

    #loop over all the data
    for row in data:
        # if we have a different unit name -- bump up the unit count
        if row[0] == unitName or unitName == 'all':

            #if statement coverage is available -- bump up the statement count
            if stIndex != -1:
                try:
                    statement[COVERED_INDEX] += int(row[stIndex+COVERED_INDEX])
                    statement[TOTAL_INDEX  ] += int(row[stIndex+TOTAL_INDEX  ])
                except:
                    pass

            #if branch coverage is available -- bump up the branch count
            if brIndex != -1:
                try:
                    branch[COVERED_INDEX] += int(row[brIndex+COVERED_INDEX])
                    branch[TOTAL_INDEX  ] += int(row[brIndex+TOTAL_INDEX  ])
                except:
                    pass

            #if pair coverage is available -- bump up the pair count
            if pairIndex != -1:
                try:
                    pair[COVERED_INDEX] += int(row[pairIndex+COVERED_INDEX])
                    pair[TOTAL_INDEX  ] += int(row[pairIndex+TOTAL_INDEX  ])
                except:
                    pass


            #if path coverage is available -- bump up the path count
            if pathIndex != -1:
                try:
                    path[COVERED_INDEX] += int(row[pathIndex+COVERED_INDEX])
                    path[TOTAL_INDEX  ] += int(row[pathIndex+TOTAL_INDEX  ])
                except:
                    pass

            #if byAnalysis coverage is available -- bump up the byAnalysis count
            if baIndex != -1:
                try:
                    byAnalysis[COVERED_INDEX] += int(row[baIndex+COVERED_INDEX])
                    byAnalysis[TOTAL_INDEX  ] += int(row[baIndex+TOTAL_INDEX  ])
                except:
                    pass

            #if function coverage is available -- bump up the function count
            if fncIndex != -1:
                try:
                    function[COVERED_INDEX] += int(row[fncIndex+COVERED_INDEX])
                    function[TOTAL_INDEX  ] += int(row[fncIndex+TOTAL_INDEX  ])
                except:
                    pass

            #if functionCall coverage is available -- bump up the functionCall count
            if fncCallIndex != -1:
                try:
                    functionCall[COVERED_INDEX] += int(row[fncCallIndex+COVERED_INDEX])
                    functionCall[TOTAL_INDEX  ] += int(row[fncCallIndex+TOTAL_INDEX  ])
                except:
                    pass

            #if complexity coverage is available -- bump up the complexity count
            if VgIndex != -1:
                try:
                    # Complexity only has one field
                    complexity[COVERED_INDEX] += int(row[VgIndex+COVERED_INDEX])
                    complexity[TOTAL_INDEX  ] += int(row[VgIndex+TOTAL_INDEX  ])
                except:
                    pass

    return calulatePercentages(statement,branch,pair,path,byAnalysis,function,functionCall,complexity)

def getFunctionData(data):
    global stIndex,brIndex,pairIndex,pathIndex,baIndex,fncIndex,fncCallIndex,VgIndex
    statement    = [0,0,0]
    branch       = [0,0,0]
    pair         = [0,0,0]
    path         = [0,0,0]
    byAnalysis   = [0,0,0]
    function     = [0,0,0]
    functionCall = [0,0,0]
    complexity   = [0,0,0]

    if stIndex != -1 and data[stIndex+TOTAL_INDEX]:
        statement  = [int(data[stIndex+COVERED_INDEX])  ,int(data[stIndex+TOTAL_INDEX])  ,0]
    if brIndex != -1 and data[brIndex+TOTAL_INDEX]:
        branch     = [int(data[brIndex+COVERED_INDEX])  ,int(data[brIndex+TOTAL_INDEX])  ,0]
    if pairIndex != -1 and data[pairIndex+TOTAL_INDEX]:
        pair       = [int(data[pairIndex+COVERED_INDEX]),int(data[pairIndex+TOTAL_INDEX]),0]
    if pathIndex != -1 and data[pathIndex+TOTAL_INDEX]:
        path       = [int(data[pathIndex+COVERED_INDEX]),int(data[pathIndex+TOTAL_INDEX]),0]
    if baIndex != -1 and data[baIndex+TOTAL_INDEX]:
        byAnalysis = [int(data[baIndex+COVERED_INDEX])  ,int(data[baIndex+TOTAL_INDEX])  ,0]
    if fncIndex != -1 and len(data) > fncIndex and data[fncIndex+TOTAL_INDEX]:
        function = [int(data[fncIndex+COVERED_INDEX])  ,int(data[fncIndex+TOTAL_INDEX])  ,0]
    if fncCallIndex != -1 and data[fncCallIndex+TOTAL_INDEX]:
        functionCall = [int(data[fncCallIndex+COVERED_INDEX])  ,int(data[fncCallIndex+TOTAL_INDEX])  ,0]
    if VgIndex != -1 and data[VgIndex]:
        try:
            complexity = [int(data[VgIndex+COVERED_INDEX])  ,int(data[VgIndex+COVERED_INDEX])  ,0]
        except:
            pass
    return calulatePercentages(statement,branch,pair,path,byAnalysis,function,functionCall,complexity)

def writeEmmaStatSummary(data):
    unitCount, subpCount = countUnitSubp(data)

    emmaData =  ("""  <stats>
    <environments   value=\"1\"/>
    <units    value=\"""" + str(unitCount)              + """\"/>
    <subprograms value=\"""" + str(subpCount)              + """\"/>
  </stats>
""")

    return emmaData

def writeEmmaSummaryData(indent, xxx_todo_changeme):

    (statement,branch,pair,path,byAnalysis,function,functionCall,complexity) = xxx_todo_changeme
    global stIndex,brIndex,pairIndex,pathIndex,baIndex,fncIndex,fncCallIndex,VgIndex

    myStr = " " * indent + "<coverage type=\"%s, %%\" value=\"%d%% (%d / %d)\"/>\n"

    emmaData = ""
    
    if stIndex != -1 and statement[TOTAL_INDEX] != 0:
        emmaData += (myStr % ("statement", statement[PERCENT_INDEX], statement[COVERED_INDEX], statement[TOTAL_INDEX]))

    if brIndex != -1 and branch[TOTAL_INDEX] != 0:
        emmaData += (myStr % ("branch", branch[PERCENT_INDEX], branch[COVERED_INDEX], branch[TOTAL_INDEX]))

    if pairIndex != -1 and pair[TOTAL_INDEX] != 0:
        emmaData += (myStr % ("mcdc", pair[PERCENT_INDEX], pair[COVERED_INDEX], pair[TOTAL_INDEX]))

    if pathIndex != -1 and path[TOTAL_INDEX] != 0:
        emmaData += (myStr % ("basispath", path[PERCENT_INDEX], path[COVERED_INDEX], path[TOTAL_INDEX]))

    if fncIndex != -1 and function[TOTAL_INDEX] != 0:
        emmaData += (myStr % ("function", function[PERCENT_INDEX], function[COVERED_INDEX], function[TOTAL_INDEX]))

    if fncCallIndex != -1 and functionCall[TOTAL_INDEX] != 0:
        emmaData += (myStr % ("functioncall", functionCall[PERCENT_INDEX], functionCall[COVERED_INDEX], functionCall[TOTAL_INDEX]))

    if VgIndex != -1 and complexity[COVERED_INDEX] != 0:
        emmaData += (myStr % ("complexity", 0, complexity[COVERED_INDEX], 0))

    return emmaData
    
def writeEmmaDataHeader(data):
    emmaData = ""
    
    emmaData += ("    <all name=\"all environments\">\n")

    emmaData += writeEmmaSummaryData(6,getCoverageTotals(data,'all'))

    return emmaData

def writeEmmaData(data):
    global jobNamePrefix
    unit_found = False
    
    emmaData = ""
    emmaData += ("  <data>\n")
    emmaData += writeEmmaDataHeader(data)
    emmaData += ("\n      <environment name =\"" + jobNamePrefix + "\">\n")
    emmaData += writeEmmaSummaryData(8,getCoverageTotals(data,'all'))

    unitName   = ""
    for row in data:
        unit_found = True
        row[UNIT_NAME_COL] = escape(row[UNIT_NAME_COL])
        row[SUBPROG_COL] = escape(row[SUBPROG_COL].replace("%2C",","))
        row[TEST_CASE_COL] = escape(row[TEST_CASE_COL])

        # if we have a different unit name -- bump up the unit count
        if row[UNIT_INDEX] != unitName:
            if unitName:
                emmaData += ("        </unit>\n")

            unitName = row[UNIT_INDEX]
            emmaData += ("\n        <unit name=\"" + unitName + "\">\n")

            emmaData += writeEmmaSummaryData(10,getCoverageTotals(data,unitName))

        emmaData += ("          <subprogram name=\"" + row[SUBP_INDEX] + "\">\n")
        emmaData += writeEmmaSummaryData(12,getFunctionData(row))
        emmaData += ("          </subprogram>\n")

    if unit_found:
        emmaData += ("        </unit>\n")

    return emmaData
    
def writeEmmaFooter():

    emmaData = ""
    emmaData +=  ("      </environment>\n")
    emmaData +=  ("    </all>\n")
    emmaData +=  ("  </data>\n")
    emmaData +=  ("</report>\n")
    return emmaData

def runCsv2JenkinsCoverageResults(csvFilename):

    #read in the CSV file
    dataArray = readCsvFile(csvFilename)

    #parse the title to determine the coverage info
    titles = dataArray[0]
    determineCoverage(titles)

    #open the emma format file
    emmaData = ""

    #write out the header information for emma format
    emmaData += writeEmmaHeader()

    #write out the stat summary
    emmaData += writeEmmaStatSummary(dataArray[1:])

    #write out the data for the emma file
    emmaData += writeEmmaData(dataArray[1:])
    
    #write out the footer information for emma format
    emmaData += writeEmmaFooter()

    with open(csvFilename[:-4]+".xml","w") as fd:
        fd.write(emmaData)

def writeBlankCCFile():
    with open("coverage_results_blank.xml","w") as fd:
        fd.write("""<report>
  <version value="3"/>
<data>
<all name="environments">
<coverage type="complexity, %" value="0% (0 / 0)"/>
</all>
</data>
</report>""")
    print("Generating a blank coverage report\n")

def run(test = "",coverage="", useExecRpt = True, version=14, junit = True):

    global gUseExecRpt, testCaseCount
    global manageVersion
    gUseExecRpt = useExecRpt
    testCaseCount = 0
    manageVersion = version

    if test:
        #print "Processing Test Results File: " + test
        runCsv2JenkinsTestResults(test, junit)

    if coverage:
        #print "Processing Coverage Results File: " + coverage
        runCsv2JenkinsCoverageResults(coverage)
    else:
        writeBlankCCFile();
        
    for file in glob.glob("*.xml"):
        try:
            # Remove destination first
            try:
                os.remove("xml_data/" + file);
            except:
                pass
            shutil.move(file, "xml_data")
        except:
            pass
                
    if os.path.isfile("xml_data/coverage_results_blank.xml") and len(glob.glob("xml_data/*.xml")) > 1:
        print("Removing xml_data/coverage_results_blank.xml...")
        os.remove("xml_data/coverage_results_blank.xml")

if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('--test',        help='Test Result CSV Filename')
    parser.add_argument('--coverage',    help='Coverage Result CSV Filename')
    parser.add_argument('--ignore-exec-rpt',     help='Don\'t use execution report', action="store_true")
    parser.add_argument('--junit',     help='Use Junit for testcase format', action="store_true")

    args = parser.parse_args()

    if args.ignore_exec_rpt:
        use_exec_rpt = False
    else:
        use_exec_rpt = True
        
    if args.junit:
        junit = True
    else:
        junit = False
        
    run(args.test,args.coverage,use_exec_rpt, junit)

    print("done")
