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
from vector.apps.DataAPI.cover_api import CoverApi
try:
    from vector.apps.DataAPI.unit_test_api import UnitTestApi
except:
    from vector.apps.DataAPI.api import Api as UnitTestApi
import sys, os
from collections import defaultdict
from pprint import pprint

fileList = []

from vcast_utils import dump, checkVectorCASTVersion

def write_xml(x, name, verbose = False):
    
    if verbose:
        print(etree.tostring(x,pretty_print=True))

    xml_str =  "<?xml version='1.0' encoding='UTF-8'?>\n"
    xml_str += "<!DOCTYPE coverage SYSTEM 'http://cobertura.sourceforge.net/xml/coverage-04.dtd'>\n"
    
    xml_str += etree.tostring(x,pretty_print=True).decode()

    open(name + ".xml", "w").write(xml_str)
   
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

def getFileXML(testXml, coverAPI, verbose = False, extended = False, source_root = ""):

    try:
        prj_dir = os.environ['CI_PROJECT_DIR'].replace("\\","/") + "/"
    except:
        try:
            prj_dir = os.environ['WORKSPACE'].replace("\\","/") + "/"
        except:
            prj_dir = os.getcwd().replace("\\","/") + "/"
    
    
    fname = coverAPI.display_name
    fpath = os.path.relpath(coverAPI.display_path,prj_dir).replace("\\","/")

    new_path = os.path.join(source_root,fpath.rsplit('/',1)[0])
    fpath = new_path.replace("\\","/")

    branch_totals = float(coverAPI.metrics.branches + coverAPI.metrics.mcdc_branches)
    branch_covered = float(coverAPI.metrics.max_covered_branches + coverAPI.metrics.max_covered_mcdc_branches)
    
    if branch_totals > 0:
        branch_pct = branch_covered / branch_totals
    else:
        branch_pct = 0.0
        
    statement_pct = coverAPI.metrics.max_covered_statements_pct / 100.0
    mcdcpair_pct = coverAPI.metrics.max_covered_mcdc_pairs_pct / 100.0
    funccall_pct = coverAPI.metrics.max_covered_function_calls_pct / 100.0
    func_pct = coverAPI.metrics.max_covered_functions_pct / 100.0
        
    file = None

    if verbose:
        print ("   fname   = ", fname)
        print ("   fpath   = ", fpath)
    
    for element in testXml.iter():
        if element.tag == "class" and element.attrib['filename'] == fpath:
            file = element
            lines = file[0]
        
    if file == None:
        file = etree.SubElement(testXml, "class")
        if ".c" in fname:
            fname = fname.split(".c")[0]
        if ".h" in fname:
            fname = fname.split(".h")[0]
        file.attrib['name'] = fname.replace(".","_")
        file.attrib['filename'] = fpath 
        
        if coverAPI.metrics.statements > 0:     
            file.attrib['line-rate'] = str(statement_pct)

        if coverAPI.metrics.branches:
            file.attrib['branch-rate'] = str(branch_pct)  

        if extended:
            if coverAPI.metrics.branches or coverAPI.metrics.mcdc_branches:
                file.attrib['branch-rate'] = str(branch_pct)  
            if coverAPI.metrics.function_calls > 0:
                funcCallPercentStr = "{:.2f}".format(coverAPI.metrics.max_covered_function_calls_pct) + "% (" + str(coverAPI.metrics.max_covered_function_calls) + "/" + str(coverAPI.metrics.function_calls) + ")"    
                file.attrib['functioncall-coverage'] = funcCallPercentStr
            if coverAPI.metrics.mcdc_pairs > 0:
                mcdcPairPercentStr = "{:.2f}".format(coverAPI.metrics.max_covered_mcdc_pairs_pct) + "% (" + str(coverAPI.metrics.max_covered_mcdc_pairs) + "/" + str(coverAPI.metrics.mcdc_pairs) + ")"             
                file.attrib['mcdcpair-coverage'] = mcdcPairPercentStr
                
            funcCovTotal, funcTotal = getCoveredFunctionCount(coverAPI)
            
            if funcTotal > 0:            
                file.attrib['function-coverage'] =  "{:.2f}".format(100.0 *funcCovTotal/funcTotal) + "% (" + str(funcCovTotal) + "/" + str(funcTotal) + ")"  
            else:
                file.attrib['function-coverage'] =  "0.00% (0/0)"  
            
        file.attrib['complexity'] = str(coverAPI.metrics.complexity)

        methods = etree.SubElement(file, "methods")
        lines = etree.SubElement(file, "lines")
        path = os.path.dirname(fname)
        if path not in fileList:
            fileList.append(path)

    return methods, lines

def getLineCoverageElementXML(lines, lineno):

    covEle = None
    
    for element in lines.iter():
        if element.tag == "line" and element.attrib['number'] == str(lineno):
            covEle = element
        
    if covEle == None:
        covEle = etree.SubElement(lines, "line")
        covEle.attrib['number'] = str(lineno)
        covEle.attrib['hits'] = "0"
        covEle.attrib['branch'] = "false"
        
    return covEle

def getBranchMcdcPairFcCoverageElementXML(lines, line, branchPercent = None, mcdcPercent = None, functionCallPercent = None, extended = None):

    lineno = line.line_number
    covEle = None
    condition = None
    #print(etree.tostring(lines,pretty_print=True).decode())
    for element in lines.iter():
        if element.tag == "line" and element.attrib['number'] == str(lineno):
            covEle = element
            if branchPercent:
                if covEle.attrib['branch'] == 'false':
                    covEle.attrib['branch'] = 'true'
                    covEle.attrib['number'] = str(lineno)
                    covEle.attrib['condition-coverage'] = branchPercent
                    conditions = etree.SubElement(covEle, "conditions")
                    condition = etree.SubElement(conditions, "condition")
                else:
                    condition = covEle[0][0]
                    
            if extended:
                if mcdcPercent:
                    covEle.attrib['mcdcpair-coverage'] = mcdcPercent
                if functionCallPercent:
                    covEle.attrib['functioncall-coverage'] = functionCallPercent
                    
                
    if covEle == None:
        covEle = etree.SubElement(lines, "line")
        covEle.attrib['number'] = str(lineno)
        covEle.attrib['hits'] = "0"
        if branchPercent:
            covEle.attrib['condition-coverage'] = branchPercent
            covEle.attrib['branch'] = "true"
            conditions = etree.SubElement(covEle, "conditions")
            condition = etree.SubElement(conditions, "condition")
            
        if extended:
            if mcdcPercent:
                covEle.attrib['mcdcpair-coverage'] = mcdcPercent
            if functionCallPercent:
                covEle.attrib['functioncall-coverage'] = functionCallPercent
            
    if condition is not None:
        condition.attrib['number'] = "0"
        condition.attrib['type'] = "jump"
        condition.attrib['coverage'] = branchPercent.split()[0]

    return covEle
    
def has_any_coverage(line):
    
    return (line.metrics.statements + 
        line.metrics.branches + 
        line.metrics.mcdc_branches + 
        line.metrics.mcdc_pairs + 
        line.metrics.functions +
        line.metrics.function_calls)

def has_anything_covered(line):
    
    return (line.metrics.max_covered_statements + 
        line.metrics.max_covered_branches + 
        line.metrics.max_covered_mcdc_branches + 
        line.metrics.max_covered_mcdc_pairs + 
        line.metrics.max_covered_functions +
        line.metrics.max_covered_function_calls + 
        line.metrics.max_covered_statements + 
        line.metrics.max_covered_branches + 
        line.metrics.max_covered_mcdc_branches + 
        line.metrics.max_covered_mcdc_pairs + 
        line.metrics.max_covered_functions +
        line.metrics.max_covered_function_calls)

def processStatementBranchMCDC(fileApi, lines, extended = False):

    linesTotal = 0
    linesCovered = 0
    
    for line in fileApi.iterate_coverage():
        if not has_any_coverage(line):
            continue
            
        linesTotal += 1
        
        covEle = getLineCoverageElementXML(lines,line.line_number)
        
        if has_anything_covered(line):
            linesCovered += 1
            covEle.attrib['hits'] = "1"

        # multi-line statement
        # if line.start_line != line.end_line:
        #     lines.remove(covEle)
        #     # if its part of a multi-line statement, save the range for use later
        #     for num in range(line.start_line,line.end_line+1):
        #         covEle = getLineCoverageElementXML(lines,num)
        #         if covEle.attrib['hits'] != "0" or covered == "true":
        #             covEle.attrib['hits'] = "1"
            
        ## branches
        branchPct = -1.0
        totalBr = 0
        coverBr = 0
        
        if line.metrics.branches + line.metrics.mcdc_branches: 
            totalBr = line.metrics.branches + line.metrics.mcdc_branches
            if (line.metrics.max_covered_branches + line.metrics.max_covered_mcdc_branches) > 0:
                coverBr = line.metrics.max_covered_branches + line.metrics.max_covered_mcdc_branches
            elif (line.metrics.max_covered_branches + line.metrics.max_covered_mcdc_branches) > 0:
                coverBr = line.metrics.max_covered_branches + line.metrics.max_covered_mcdc_branches
            else:
                coverBr = 0
            
            branchPct = (coverBr * 100 ) / totalBr
                
        ## mcdc pair
        pairPct = -1.0
        totalPr = 0
        coverPr = 0
        if line.metrics.mcdc_pairs:
            totalPr = line.metrics.mcdc_pairs
            coverPr = line.metrics.max_covered_mcdc_pairs
            pairPct = (coverPr * 100 ) / totalPr
                
        ## function call
        totalFc = -1.0
        coverFc = 0
        fcPct = 0
        hasFc = False
        fcPctString = None
        
        if line.metrics.function_calls:
            hasFc = True
            totalFc = line.metrics.function_calls
            coverFc = line.metrics.max_covered_function_calls
            fcPct = (coverFc * 100 ) / totalFc
            fcPctString = str(fcPct) + "% (" + str(coverFc) + "/" + str(totalFc) + ")"
            
        ## has branches
        hasBranches = False
        pairPctString = None
        branchPctString = None
        if line.metrics.branches + line.metrics.mcdc_branches + line.metrics.mcdc_pairs:
            hasBranches = True
            if branchPct == -1:
                branchPctString = "0.0% (0/0)"
            else:
                branchPctString = str(branchPct) + "% (" + str(coverBr) + "/" + str(totalBr) + ")"
            if pairPct == -1:
                pairPctString = None
            else:
                pairPctString = str(pairPct) + "% (" + str(coverPr) + "/" + str(totalPr) + ")"
                
        if hasBranches or hasFc:
            covEle = getBranchMcdcPairFcCoverageElementXML(lines, line, branchPctString, pairPctString, fcPctString, extended)
                
    return linesCovered, linesTotal
    
                        
def procesCoverage(coverXML, coverApi, extended = False, source_root = ""):             
    
    methods, lines = getFileXML(coverXML, coverApi, extended = extended, source_root = source_root)

    if extended:
        for func in coverApi.functions:
                      
            method = etree.SubElement(methods, "method")
            
            method.attrib['name'] = func.name
            method.attrib['signature'] = func.instrumented_functions[0].parameterized_name.replace(func.name,"",1)
            method.attrib['line-rate'] = str(func.metrics.max_covered_statements_pct/100.0)
            
            statementPercentStr = "{:.2f}".format(func.metrics.max_covered_statements_pct) + "% (" + str(func.metrics.max_covered_statements) + "/" + str(func.metrics.statements) + ")"             
            #method.attrib['statements'] = statementPercentStr
            
            func_total_br = func.metrics.branches + func.metrics.mcdc_branches
            func_cov_br   = func.metrics.max_covered_branches + func.metrics.max_covered_mcdc_branches
            
            func_branch_rate = 0.0
            if func_total_br > 0:
                func_branch_rate = float(func_cov_br) / float(func_total_br)
            
            method.attrib['branch-rate'] = str(func_branch_rate)
            method.attrib['complexity'] = str(func.metrics.complexity)
                    
            if func.metrics.function_calls > 0:
                funcCallPercentStr = "{:.2f}".format(func.metrics.max_covered_function_calls_pct) + "% (" + str(func.metrics.max_covered_function_calls) + "/" + str(func.metrics.function_calls) + ")"    
                method.attrib['functioncall-coverage'] = funcCallPercentStr
            if func.metrics.mcdc_pairs > 0:
                mcdcPairPercentStr = "{:.2f}".format(func.metrics.max_covered_mcdc_pairs_pct) + "% (" + str(func.metrics.max_covered_mcdc_pairs) + "/" + str(func.metrics.mcdc_pairs) + ")"             
                method.attrib['mcdcpair-coverage'] = mcdcPairPercentStr
                
            if (func.metrics.max_covered_functions_pct +  
                func.metrics.max_covered_statements_pct + 
                func.metrics.max_covered_branches_pct + 
                func.metrics.max_covered_mcdc_branches_pct + 
                func.metrics.max_covered_mcdc_pairs + 
                func.metrics.max_covered_function_calls_pct) > 0:
                method.attrib['function-coverage'] = "100% (1/1)"
            else:
                method.attrib['function-coverage'] = "0% (0/1)"

    return processStatementBranchMCDC(coverApi, lines, extended)
    
def runCoverageResultsMP(packages, mpFile, verbose = False, extended=False, source_root = ""):

    vcproj = VCProjectApi(mpFile)
    api = vcproj.project.cover_api
    
    return runCoberturaResults(packages, api, verbose = False, extended = extended, source_root = source_root)
    
def runCoberturaResults(packages, api, verbose = False, extended = False, source_root = ""):
        
    total_br = 0
    total_st = 0
    total_func = 0 
    total_fc   = 0 
    total_mcdc = 0
    
    total_lines = 0
    
    cov_br   = 0 
    cov_st   = 0
    cov_func   = 0
    cov_fc   = 0 
    cov_mcdc = 0
    
    cov_lines = 0
    
    vg       = 0

    pkg_total_br = 0
    pkg_total_st = 0
    pkg_total_func = 0
    pkg_total_fc   = 0
    pkg_total_mcdc = 0

    pkg_total_lines = 0
    
    pkg_cov_br   = 0 
    pkg_cov_st   = 0
    pkg_cov_func = 0
    pkg_cov_fc   = 0
    pkg_cov_mcdc = 0
    pkg_vg       = 0

    pkg_cov_lines = 0

    path_name = "@@@@"
    
    package = None
    
    hasStatementCov = False
    hasBranchCov = False
    hasMcdcCov = False
    hasFunctionCov = False
    hasFunctionCallCov = False
    
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
            
        #fpath = file.display_path.rsplit('.',1)[0]
        fpath = file.display_name
        fpath = os.path.relpath(fpath,prj_dir).replace("\\","/")
        
        # print("*", file.name, file.display_name, fpath)

        fileDict[fpath] = file
    
    for path in sorted(fileDict.keys()):
        file = fileDict[path]        
        new_path = path.rsplit('/',1)[0]
        

        # when we switch paths
        if new_path != path_name:
        
            # If we have data to save...
            if package != None:
        
                if verbose:
                    print("saving data for package: " + path_name )
                    
                # calculate stats for package
                branch_rate = 0.0
                statement_rate = 0.0
                line_rate = 0.0
                MCDC_rate = 0.0
                FC_rate = 0.0
                func_rate = 0.0
                
                if pkg_total_st > 0:
                    statement_rate = float(pkg_cov_st) / float(pkg_total_st)
                
                if pkg_total_lines > 0:
                    line_rate = float(pkg_cov_lines) / float(pkg_total_lines)
                
                if pkg_total_br > 0:
                    branch_rate = float(pkg_cov_br) / float(pkg_total_br)
                    
                if extended:
                    if pkg_total_mcdc > 0:
                        MCDC_rate = float(pkg_cov_mcdc) / float(pkg_total_mcdc)
                    
                    if pkg_total_func > 0:
                        func_rate = float(pkg_cov_func) / float(pkg_total_func)
                    
                    if pkg_total_fc > 0:
                        FC_rate = float(pkg_cov_fc) / float(pkg_total_fc)
                
                # store the stats 
                # remove trailing . if present
                package_name = path_name.replace("/",".")
                package_name = package_name.lstrip(".")

                if package_name.endswith("."):
                    package_name = package_name[:-1]

                package.attrib['name'] = package_name
                
                package.attrib['complexity'] = str(pkg_vg)
                
                if file.has_statement_coverage:
                    package.attrib['line-rate'] = str(line_rate)
                if file.has_branch_coverage:        
                    package.attrib['branch-rate'] = str(branch_rate)
                    
                if extended:
                    if file.has_branch_coverage or file.has_mcdc_coverage:        
                        package.attrib['branch-rate'] = str(branch_rate)

                    if file.has_mcdc_coverage:          
                        mcdcPairPercentStr = "{:.2f}".format(MCDC_rate * 100.0) + "% (" + str(pkg_cov_mcdc) + "/" + str(pkg_total_mcdc) + ")"             
                        package.attrib['mcdcpair-coverage'] = mcdcPairPercentStr
                    if file.has_function_call_coverage: 
                        funcCallPercentStr = "{:.2f}".format(FC_rate * 100.0) + "% (" + str(pkg_cov_fc) + "/" + str(pkg_total_fc) + ")"             
                        package.attrib['functioncall-coverage'] = funcCallPercentStr
                        
                    funcCovTotal, funcTotal = getCoveredFunctionCount(file)
                    if funcTotal > 0:  
                        func_rate = funcCovTotal / funcTotal
                        funcPercentStr = "{:.2f}".format(func_rate * 100.0) + "% (" + str(pkg_cov_func) + "/" + str(pkg_total_func) + ")"             
                        package.attrib['function-coverage'] = funcPercentStr
                    
            path_name = new_path
            
            if verbose:
                # create a new package and zero out the stats
                print("creating blank package for: " + path_name + "/")

            package  = etree.SubElement(packages, "package")
            classes  = etree.SubElement(package, "classes")
            pkg_total_br = 0
            pkg_total_lines = 0
            pkg_total_st = 0
            pkg_total_func = 0
            pkg_total_fc   = 0
            pkg_total_mcdc = 0
            
            pkg_cov_br   = 0 
            pkg_cov_st   = 0
            pkg_cov_lines   = 0
            pkg_cov_func = 0
            pkg_cov_fc   = 0
            pkg_cov_mcdc = 0
            pkg_vg       = 0
                
        if verbose:
            print ("adding data for " + path)
            
            
        total_br += file.metrics.branches + file.metrics.mcdc_branches
        total_st += file.metrics.statements
        cov_br   += file.metrics.max_covered_branches + file.metrics.max_covered_mcdc_branches
        cov_st   += file.metrics.max_covered_statements

        pkg_total_br += file.metrics.branches + file.metrics.mcdc_branches
        pkg_total_st += file.metrics.statements
        

        pkg_cov_br += file.metrics.max_covered_branches + file.metrics.max_covered_mcdc_branches
        pkg_cov_st += file.metrics.max_covered_statements

        vg     += file.metrics.complexity
        pkg_vg += file.metrics.complexity

        if extended:            
            total_fc   += file.metrics.function_calls
            total_mcdc += file.metrics.mcdc_pairs

            cov_fc         += file.metrics.max_covered_function_calls
            cov_mcdc       += file.metrics.max_covered_mcdc_pairs
            pkg_total_fc   += file.metrics.function_calls
            pkg_total_mcdc += file.metrics.mcdc_pairs
        
            pkg_cov_fc   += file.metrics.max_covered_function_calls
            pkg_cov_mcdc += file.metrics.max_covered_mcdc_pairs
        
            funcCovTotal, funcTotal = getCoveredFunctionCount(file)
            pkg_total_func += funcTotal
            pkg_cov_func += funcCovTotal
            total_func += funcTotal
            cov_func += funcCovTotal
        
        linesCovered, linesTotal = procesCoverage(classes, file, extended, source_root)
        
        total_lines += linesTotal
        cov_lines   += linesCovered
        pkg_total_lines += linesTotal
        pkg_cov_lines   += linesCovered
        
        
    if package != None:
        if verbose:
            print("saving data for package: " + path_name )
            
        # calculate stats for package
        line_rate = 0.0
        branch_rate = 0.0
        func_rate = 0.0
        FC_rate = 0.0
        MCDC_rate = 0.0
            
        if pkg_total_st > 0:
            statement_rate = float(pkg_cov_st) / float(pkg_total_st)
        if pkg_total_lines > 0:
            line_rate = float(pkg_cov_lines) / float(pkg_total_lines)
        if pkg_total_br > 0:
            branch_rate = float(pkg_cov_br) / float(pkg_total_br)
            
        if extended:
            if pkg_total_func > 0:
                func_rate = float(pkg_cov_func) / float(pkg_total_func)
            if pkg_total_fc > 0:
                FC_rate = float(pkg_cov_fc) / float(pkg_total_fc)
            if pkg_total_mcdc > 0:
                MCDC_rate = float(pkg_cov_mcdc) / float(pkg_total_mcdc)   
            
        # store the stats 
        package_name = path_name.replace("/",".")
        package_name = package_name.lstrip(".")
        if package_name.endswith("."):
            package_name = package_name[:-1]
        
        package.attrib['name'] = package_name
        if file.has_statement_coverage:     
            package.attrib['line-rate'] = str(line_rate)    
            package.attrib['statement-rate'] = str(statement_rate)    
        if file.has_branch_coverage:        
            package.attrib['branch-rate'] = str(branch_rate)
            
        if extended:
            if file.has_branch_coverage or file.has_mcdc_coverage:        
                package.attrib['branch-rate'] = str(branch_rate)
            if file.has_mcdc_coverage:          
                mcdcPairPercentStr = "{:.2f}".format(MCDC_rate * 100.0) + "% (" + str(pkg_cov_mcdc) + "/" + str(pkg_total_mcdc) + ")"             
                package.attrib['mcdcpair-coverage'] = mcdcPairPercentStr
            if file.has_function_call_coverage: 
                funcCallPercentStr = "{:.2f}".format(FC_rate * 100.0) + "% (" + str(pkg_cov_fc) + "/" + str(pkg_total_fc) + ")"             
                package.attrib['functioncall-coverage'] = funcCallPercentStr
                
            if pkg_total_func > 0:      
                funcPercentStr = "{:.2f}".format(func_rate * 100.0) + "% (" + str(pkg_cov_func) + "/" + str(pkg_total_func) + ")"             
                package.attrib['function-coverage'] = funcPercentStr
        package.attrib['complexity'] = str(pkg_vg)
              
    branch_rate = -1.0
    statement_rate   = -1.0
    line_rate   = -1.0
    func_rate   = -1.0
    FC_rate     = -1.0
    MCDC_rate   = -1.0
    
    if total_st > 0:
        statement_rate = float(cov_st) / float(total_st)
        
    if total_lines > 0:
        line_rate = float(cov_lines) / float(total_lines)
        
    if total_br > 0:
        branch_rate = float(cov_br) / float(total_br)
        
    if total_mcdc > 0:
        MCDC_rate = float(cov_mcdc) / float(total_mcdc)

    if total_func > 0:
        func_rate = float(cov_func) / float(total_func)

    if total_fc > 0:
        FC_rate = float(cov_fc) / float(total_fc)
        
        
    return total_st, cov_st, total_lines, cov_lines, total_br, cov_br, total_func, cov_func, total_fc, cov_fc, total_mcdc, cov_mcdc, branch_rate, statement_rate, line_rate, func_rate, FC_rate, MCDC_rate, vg
            

def generateCoverageResults(inFile, azure = False, xml_data_dir = "xml_data", verbose = False, extended = False, source_root = "" ):
    
    cwd = os.getcwd()
    xml_data_dir = os.path.join(cwd,xml_data_dir)
    
    #coverage results
    coverages=etree.Element("coverage")
    
    sources = etree.SubElement(coverages, "sources")
    packages = etree.SubElement(coverages, "packages")
    name = os.path.splitext(os.path.basename(inFile))[0]
    
    complexity = 0
    branch_rate, line_rate, func_rate,  FC_rate,  MCDC_rate  = 0.0, 0.0, 0.0,  0.0, 0.0
    total_br,    total_st,  total_func, total_fc, total_mcdc =   0,   0,   0,    0,   0
    
    if inFile.endswith(".vce"):
        api=UnitTestApi(inFile)
        cdb = api.environment.get_coverdb_api()
        total_st, cov_st, total_lines, cov_lines, total_br, cov_br, total_func, cov_func, total_fc, cov_fc, total_mcdc, cov_mcdc, branch_rate, statement_rate, line_rate, func_rate, FC_rate, MCDC_rate, complexity  = runCoberturaResults(packages, cdb, verbose=verbose, extended=extended, source_root = source_root)
    elif inFile.endswith(".vcp"):
        api=CoverApi(inFile)
        total_st, cov_st, total_lines, cov_lines, total_br, cov_br, total_func, cov_func, total_fc, cov_fc, total_mcdc, cov_mcdc, branch_rate, statement_rate, line_rate, func_rate, FC_rate, MCDC_rate, complexity  = runCoberturaResults(packages, api, verbose=verbose, extended=extended, source_root = source_root)
    else:        
        total_st, cov_st, total_lines, cov_lines, total_br, cov_br, total_func, cov_func, total_fc, cov_fc, total_mcdc, cov_mcdc, branch_rate, statement_rate, line_rate, func_rate, FC_rate, MCDC_rate, complexity  = runCoverageResultsMP(packages, inFile, verbose=verbose, extended=extended, source_root = source_root)

    if line_rate        != -1.0: coverages.attrib['line-rate']        = str(line_rate) 
    if statement_rate   != -1.0: coverages.attrib['statement-rate']   = str(statement_rate) 
    if branch_rate      != -1.0: coverages.attrib['branch-rate']      = str(branch_rate)
    
    if extended:
        if MCDC_rate   != -1.0: coverages.attrib['mcdcpair-coverage-rate']     = str(MCDC_rate) 
        if func_rate   != -1.0: coverages.attrib['function-coverage-rate']     = str(func_rate) 
        if FC_rate     != -1.0: coverages.attrib['functioncall-coverage-rate'] = str(FC_rate) 
        
    from datetime import datetime
    coverages.attrib['timestamp'] = str(datetime.now())
    
    tool_version = os.path.join(os.environ['VECTORCAST_DIR'], "DATA", "tool_version.txt")
    with open(tool_version,"r") as fd:
        ver = fd.read()
    
    coverages.attrib['version'] = "VectorCAST " + ver.rstrip()
    
    if azure:
        if line_rate   != -1.0: coverages.attrib['lines-covered'] = str(cov_st)
        if line_rate   != -1.0: coverages.attrib['lines-valid'] = str(total_st)
        if branch_rate != -1.0: coverages.attrib['branches-covered'] = str(cov_br)
        if branch_rate != -1.0: coverages.attrib['branches-valid'] = str(total_br)
        
    if line_rate   != -1.0: print ("lines: {:.2f}% ({:d} out of {:d})".format(line_rate*100.0, cov_lines, total_lines))
    if statement_rate   != -1.0: print ("statements: {:.2f}% ({:d} out of {:d})".format(statement_rate*100.0, cov_st, total_st))
    if branch_rate != -1.0: print ("branches: {:.2f}% ({:d} out of {:d})".format(branch_rate*100.0, cov_br, total_br))
    if func_rate   != -1.0: print ("functions: {:.2f}% ({:d} out of {:d})".format(func_rate*100.0, cov_func, total_func))
    if FC_rate     != -1.0: print ("function calls: {:.2f}% ({:d} out of {:d})".format(FC_rate*100.0, cov_fc, total_fc))
    if MCDC_rate   != -1.0: print ("mcdc pairs: {:.2f}% ({:d} out of {:d})".format(MCDC_rate*100.0, cov_mcdc, total_mcdc))
    
    if statement_rate   != -1.0: print ("coverage: {:.2f}% of statements".format(statement_rate*100.0))
    print ("complexity: {:d}".format(complexity))
    source = etree.SubElement(sources, "source")
    source.text = "./"

    cob_data_dir = os.path.join(xml_data_dir,"cobertura")
    if not os.path.exists(cob_data_dir):
        os.makedirs(cob_data_dir)
        
    write_xml(coverages, os.path.join(cob_data_dir,"coverage_results_" + name))
             
if __name__ == '__main__':
    
    if not checkVectorCASTVersion(21):
        print("Cannot create Cobertura metrics. Please upgrade VectorCAST")
        sys.exit()
            
    extended = False
    azure = False
    
    inFile = sys.argv[1]
    try:
        if "--azure" == sys.argv[2]:
            azure = True
            print ("using azure mode")
        elif "--extended" == sys.argv[2]:
            extended = True
    except Exception as e:
        azure = False        
        extended = False        
        
    generateCoverageResults(inFile, azure, xml_data_dir = "xml_data", verbose = False, extended = extended)


