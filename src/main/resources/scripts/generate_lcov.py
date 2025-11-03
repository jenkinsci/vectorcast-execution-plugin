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

from lxml import etree
try:
    from vector.apps.DataAPI.vcproject_api import VCProjectApi 
    from vector.apps.DataAPI.vcproject_models import VCProject
except:
    pass

from vector.apps.DataAPI.cover_api import CoverApi

try:
    from vector.apps.DataAPI.unit_test_api import UnitTestApi
except:
    from vector.apps.DataAPI.api import Api as UnitTestApi

import sys, os
from collections import defaultdict
from pprint import pprint
import subprocess
import argparse

from vcast_utils import dump, checkVectorCASTVersion, getVectorCASTEncoding
try:
    from safe_open import open
except:
    pass
    
try:
    import math
    INF = math.inf
except Exception:
    INF = float("inf")  # Py2-compatible
encFmt = getVectorCASTEncoding()

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
        
def has_branch_coverage(line):
    
    return (line.metrics.branches + 
        line.metrics.mcdc_branches + 
        line.metrics.mcdc_pairs)
        

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
        
def has_branches_covered(line):
    
    count = (line.metrics.covered_branches + 
        line.metrics.covered_mcdc_branches + 
        line.metrics.covered_mcdc_pairs)
        
    if count == 0:
        count = (line.metrics.max_covered_branches + 
            line.metrics.max_covered_mcdc_branches + 
            line.metrics.max_covered_mcdc_pairs)
        
    return count
       
def get_function_name_line_number(file_path, function, initial_guess):

    with open(file_path, "rb") as fd:
        lines = [line.decode(encFmt, "replace") for line in fd.readlines()]

    if initial_guess is None or initial_guess >= len(lines):
        initial_guess = len(lines) - 1

    line_number_closest_so_far = initial_guess
    delta = INF

    for count, line in enumerate(reversed(lines[:initial_guess + 1])):
        if function in line.replace(" ", ""):
            line_num = initial_guess - count
            if abs(line_num - initial_guess) < delta:
                line_number_closest_so_far = line_num
                delta = abs(line_num - initial_guess)

    return line_number_closest_so_far + 1  # convert 0-based to 1-based


def runCoverageResultsMP(mpFile, verbose = False, testName = "", source_root = ""):

    with VCProjectApi(mpFile) as vcproj:
        api = vcproj.project.cover_api
        results = runGcovResults(api, verbose = verbose, testName = vcproj.project.name, source_root=source_root)
    
    return results
    
def runGcovResults(api, verbose = False, testName = "", source_root = "") :
   
    fileDict = {}
    try:
        prj_dir = os.environ['CI_PROJECT_DIR'].replace("\\","/") + "/"
    except:
        try:
            prj_dir = os.environ['WORKSPACE'].replace("\\","/") + "/"
        except:
            prj_dir = os.getcwd().replace("\\","/") + "/"    
    
    # get a sorted listed of all the files with the proj directory stripped off
    for file in api.SourceFile.all():  
        if file.display_name == "":
            continue
        if not has_any_coverage(file):
            continue
            
        fname = file.display_name
        fpath = file.display_path.rsplit('.',1)[0]
        try:
            fpath = os.path.relpath(fpath,prj_dir).replace("\\","/")
        except:
            fpath = fpath.replace("\\","/")

        fileDict[fpath] = file

    output = ""
    
    for path in sorted(fileDict.keys()):
        
        DA = []
        BRDA = []
        FN = []
        FNDA = []

        BRH = 0
        BRF = 0
        
        LH = 0
        LF = 0
        
        file = fileDict[path]        
        new_path = os.path.join(source_root,path.rsplit('/',1)[0])

        output += "TN:" + testName + "\n"
        new_path = new_path.replace("\\","/")
        
        if len(source_root) > 0:
            sourceFile = "SF:" + new_path + "/" + file.name + "\n"
        else:
            sourceFile = "SF:" + file.name + "\n"

        output += sourceFile;
        
        if verbose:
            print("source_root: " + source_root)
            print("path       : " + path)
            print("new_path   : " + new_path)
            print("file.name  : " + file.name)
            print("sourceFile : " + sourceFile + "\n")

        for func in file.functions:
            func_name_line_number = get_function_name_line_number(file.display_path, func.name, func.start_line)
            fName = func.mangled_name #func.name + func.instrumented_functions[0].parameterized_name.replace(func.name,"",1)
            FN.append("FN:" + str(func_name_line_number) + "," + fName)
            if has_anything_covered(func) > 0:
                FNDA.append("FNDA:1" + "," + fName)
            else:
                FNDA.append("FNDA:0" + "," + fName)
                            
            block_count = 0
            branch_number = 0
            line_branch = []

            last_line = ""
            any_line_covered = 0
            any_return_found = False
            found_func_start = False
            
            lastLine = None
            
            for line in func.iterate_coverage():
                if has_any_coverage(line):
                    lastLine = line
                    LF += 1
                    if has_anything_covered(line): 
                        lineCovered = "1"
                        LH += 1
                        any_line_covered += 1
                    else:
                        lineCovered = "0"

                    if " return" in line.text:
                       any_return_found = True
                    if not found_func_start:
                        DA.append("DA:" + str(func_name_line_number) + "," + lineCovered)
                        found_func_start = True
                    else:
                        DA.append("DA:" + str(line.line_number) + "," + lineCovered)
                    
                    last_line = line.text
 
                    newBranch = False
                    if has_branch_coverage(line) > 0:
                        BRF += 1
                        if line.line_number not in line_branch:
                            line_branch.append(line.line_number)
                            newBranch = True
                            
                        branches_covered = has_branches_covered(line)
                        if branches_covered > 0:
                            taken = str(branches_covered)
                            BRH += 1
                        else:
                            taken = "-"
                        BRDA.append("BRDA:" + str(line.line_number) + "," + str(block_count) + "," + str(branch_number) + "," + taken)
                        
                        if newBranch:
                            block_count += 1
                            branch_number += 1
            

            if lastLine is not None: #not any_return_found:
                if verbose: print("counting last line: ", func.name, lastLine.line_number,last_line)
                if any_line_covered > 0:
                    DA.append("DA:" + str(lastLine.line_number) + ",1")
                else:
                    DA.append("DA:" + str(lastLine.line_number) + ",0")
            else:
                if verbose: print("not counting last line: ", func.name, lastLine.line_number,last_line)
        
        for idx in range(0,len(FN)):
            output += FN[idx] + "\n"
            output += FNDA[idx] + "\n"
            
        FNH, FNF = getCoveredFunctionCount(file)
        output += "FNF:" + str(FNF) + "\n"
        output += "FNH:" + str(FNH) + "\n"
        
        sorted_BRDA = sorted(BRDA, key=lambda x: int(x.split(":")[1].split(",")[0]))
        
        for branch in sorted_BRDA:
            output += branch + "\n"
        
        output += "BRF:" + str(BRF) + "\n"
        output += "BRH:" + str(BRH) + "\n"
        
        sorted_DA = sorted(DA, key=lambda x: int(x.split(":")[1].split(",")[0]))

        for data in sorted_DA:
            output += data + "\n"
            
        output += "LF:"+ str(LF) + "\n"
        output += "LH:"+ str(LH) + "\n"

        output += "end_of_record" + "\n"
        
    return output

def generateCoverageResults(inFile, xml_data_dir = "xml_data", verbose = False, source_root = ""):
    
    cwd = os.getcwd()
    xml_data_dir = os.path.join(cwd,xml_data_dir)
    
    name = os.path.splitext(os.path.basename(inFile))[0]

    output = ""
    
    if inFile.endswith(".vce"):
        api=UnitTestApi(inFile)
        cdb = api.environment.get_coverdb_api()
        output = runGcovResults(cdb, verbose=verbose, testName = name, source_root=source_root)
    elif inFile.endswith(".vcp"):
        api=CoverApi(inFile)
        output = runGcovResults(api, verbose=verbose, testName = name, source_root=source_root)
    else:        
        output = runCoverageResultsMP(inFile, verbose=verbose, testName = name, source_root=source_root)

    lcov_data_dir = os.path.join(xml_data_dir,"lcov")
    if not os.path.exists(lcov_data_dir):
        os.makedirs(lcov_data_dir)

    pathToInfo = os.path.join(lcov_data_dir, name + ".info")
    with open(pathToInfo, "wb") as fd: 
        fd.write(output.encode(encFmt, "replace"))

    cmdStr = "genhtml " + pathToInfo + " --output-directory out"
    cmdArr = cmdStr.split()
    try:
        subprocess.Popen(cmdArr).wait()
        return True
    except:
        return False
    
if __name__ == '__main__':
    
    if not checkVectorCASTVersion(21):
        print("Cannot create LCOV metrics. Please upgrade VectorCAST")
        sys.exit(0)
        
    parser = argparse.ArgumentParser()
    parser.add_argument('vcProjectName', help='VectorCAST Project Name', action="store")
    parser.add_argument('-v', '--verbose',   help='Enable versobe output', dest="verbose", action="store_true", default=False)
    args = parser.parse_args()

    try:
        inFile = args.vcProjectName
        if not inFile.endswith(".vcm"):
           inFile += ".vcm"
    except:
        inFile = os.getenv('VCAST_MANAGE_PROJECT_DIRECTORY') + ".vcm"

    if args.verbose: print ("Running in verbose mode")
        
    passed = generateCoverageResults(inFile, xml_data_dir = "xml_data", verbose = args.verbose, source_root = "")
    
    try:
        ## if opened from VectorCAST GUI...
        if passed and not os.getenv('VCAST_MANAGE_PROJECT_DIRECTORY') is None:
            from vector.lib.core import VC_Report_Client

            # Open report in VectorCAST GUI
            report_client = VC_Report_Client.ReportClient()
            if report_client.is_connected():
                report_client.open_report("out/index.html", "lcov Results")
    except:
        pass



