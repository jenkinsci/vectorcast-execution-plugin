#
# The MIT License
#
# Copyright 2020 Vector Informatik, GmbH.
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

from lxml import etree
try:
    from vector.apps.DataAPI.vcproject_api import VCProjectApi 
    from vector.apps.DataAPI.vcproject_models import VCProject
except:
    pass
try:
    from vector.apps.DataAPI.unit_test_api import UnitTestApi
except:
    from vector.apps.DataAPI.api import Api as UnitTestApi

from vector.apps.DataAPI.cover_api import CoverApi
import sys, os
from collections import defaultdict
from pprint import pprint

from vcast_utils import dump, checkVectorCASTVersion

fileList = []

def getCoveredFunctionCount(source):
    if len(source.functions) == 0:
        return 0,0

    funcTotal = 0
    funcCovTotal = 0
    for funcApi in source.functions:
        func_cov = False
        funcTotal += 1
        for instFunc in funcApi.instrumented_functions:
            if instFunc.covered(True):
                func_cov = True
        if func_cov:
            funcCovTotal += 1
        # print(source.name,"::",funcApi.name, func_cov)
        
    # print(funcCovTotal, funcTotal)
        
    return funcCovTotal, funcTotal
    
def has_any_coverage(line):
    
    return (line.metrics.statements + 
        line.metrics.branches + 
        line.metrics.mcdc_branches + 
        line.metrics.mcdc_pairs + 
        line.metrics.functions +
        line.metrics.function_calls)

def has_anything_covered(line):
    
    return (line.metrics.covered_statements + 
        line.metrics.covered_branches + 
        line.metrics.covered_mcdc_branches + 
        line.metrics.covered_mcdc_pairs + 
        line.metrics.covered_functions +
        line.metrics.covered_function_calls + 
        line.metrics.max_covered_statements + 
        line.metrics.max_covered_branches + 
        line.metrics.max_covered_mcdc_branches + 
        line.metrics.max_covered_mcdc_pairs + 
        line.metrics.max_covered_functions +
        line.metrics.max_covered_function_calls)
        
def runCoverageResultsMP(mpFile, verbose = False):

    vcproj = VCProjectApi(mpFile)
    api = vcproj.project.cover_api
    
    return runGcovResults(api, verbose = False)
    
def runGcovResults(api, verbose = False):
   
    fileDict = {}
    try:
        prj_dir = os.environ['CI_PROJECT_DIR'].replace("\\","/") + "/"
    except:
        try:
            prj_dir = os.environ['WORKSPACE'].replace("\\","/") + "/"
        except:
            prj_dir = os.getcwd().replace("\\","/") + "/"    
    
    # get a sorted listed of all the files with the proj directory stripped off
     
    DA = []
    BRF = []
    BRH = []
    
    ## FN:108,206,BETP_Wrapper
    functionName_FN = []
    
    ## FNDA:2775985,BETP_Wrapper
    functionSomething_FNDA = []
    
    ## totals
    functionsFound_FNF = 0
    functionsHit_FNH = 0
    linesFound_LF = 0
    linesHit_LH = 0
    
    for file in api.SourceFile.all():  
        if file.display_name == "":
            continue
        if not has_any_coverage(file):
            continue
            
        #fpath = file.display_path.rsplit('.',1)[0]
        fpath = file.display_name
        fpath = os.path.relpath(fpath,prj_dir).replace("\\","/")
        
        # print("*", file.name, file.display_name, fpath)

        fileDict[fpath] = file
    
    output = ""
    
    for path in sorted(fileDict.keys()):
        
        DA = []
        BRDA = []
        FN = []
        FNDA = []

        BRH = 0
        BRF = 0
        
        file = fileDict[path]        
        new_path = path.rsplit('/',1)[0]

        output += "TN:" + "\n"
        output += "SF:" + new_path+ "/" + file.name + "\n"
        
        for func in file.functions:
            fName = func.name + func.instrumented_functions[0].parameterized_name.replace(func.name,"",1)
            FN.append("FN:" + str(func.start_line) + "," + fName)
            if has_anything_covered(func) > 0:
                FNDA.append("FNDA:1" + "," + fName)
            else:
                FNDA.append("FNDA:0" + "," + fName)
            
            block_count = 0
            branch_number = 0
            line_branch = []
                
            for line in func.iterate_coverage():
                if has_any_coverage(line):
                    if line.metrics.covered_statements or line.metrics.covered_branches or line.metrics.covered_mcdc_branches:
                        lineCovered = "1"
                    else:
                        lineCovered = "0"
                    DA.append("DA:" + str(line.line_number) + "," + lineCovered)
                    
                    newBranch = False
                    if (line.metrics.branches + line.metrics.mcdc_branches) > 0:

                        if line.line_number not in line_branch:
                            line_branch.append(line.line_number)
                            newBranch = True
                            
                        if (line.metrics.covered_branches + line.metrics.covered_mcdc_branches) > 0:
                            taken = str(line.metrics.covered_branches + line.metrics.covered_mcdc_branches)
                        else:
                            taken = "-"
                        BRDA.append("BRDA:" + str(line.line_number) + "," + str(block_count) + "," + str(branch_number) + "," + taken)
                        
                        if newBranch:
                            block_count += 1
                            branch_number += 1
        
        for idx in range(0,len(FN)):
            output += FN[idx] + "\n"
            output += FNDA[idx] + "\n"
            
        FNH, FNF = getCoveredFunctionCount(file)
        output += "FNF:" + str(FNF) + "\n"
        output += "FNH:" + str(FNH) + "\n"
        
        for branch in BRDA:
            output += branch + "\n"
        
        output += "BRF:" + str(file.metrics.branches + file.metrics.mcdc_branches) + "\n"
        output += "BRH:" + str(file.metrics.aggregate_covered_branches + file.metrics.aggregate_covered_mcdc_branches) + "\n"
        
            
        for data in DA:
            output += data + "\n"
            
        output += "LF:"+ str(file.metrics.statements) + "\n"
        output += "LH:"+ str(file.metrics.aggregate_covered_statements) + "\n"

        output += "end_of_record" + "\n"
        
    return output

def generateCoverageResults(inFile, xml_data_dir = "xml_data", verbose = False):
    
    cwd = os.getcwd()
    xml_data_dir = os.path.join(cwd,xml_data_dir)
    
    name = os.path.splitext(os.path.basename(inFile))[0]

    output = ""
    
    if inFile.endswith(".vce"):
        api=UnitTestApi(inFile)
        cdb = api.environment.get_coverdb_api()
        output = runGcovResults(cdb, verbose=verbose)
    elif inFile.endswith(".vcp"):
        api=CoverApi(inFile)
        output = runGcovResults(api, verbose=verbose)
    else:        
        output = runCoverageResultsMP(inFile, verbose=verbose)

    lcov_data_dir = os.path.join(xml_data_dir,"lcov")
    if not os.path.exists(lcov_data_dir):
        os.makedirs(lcov_data_dir)

    open(os.path.join(lcov_data_dir, name + "-info"), "w").write(output)
    
if __name__ == '__main__':
    
    if not checkVectorCASTVersion(21):
        print("Cannot create LCOV metrics. Please upgrade VectorCAST")
        sys.exit()
            
    try:
        inFile = sys.argv[1]
    except:
        inFile = os.getenv('VCV_ENVIRONMENT_FILE')
        
    generateCoverageResults(inFile, xml_data_dir = "xml_data", verbose = False)
    
    ## if opened from VectorCAST GUI...
    if not os.getenv('VCAST_MANAGE_PROJECT_DIRECTORY') is None:
        from vector.lib.core import VC_Report_Client

        # Open report in VectorCAST GUI
        report_client = VC_Report_Client.ReportClient()
        if report_client.is_connected():
            report_client.open_report("out/index.html", "lcov Results")

