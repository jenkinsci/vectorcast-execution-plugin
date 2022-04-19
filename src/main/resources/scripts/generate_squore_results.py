import sys
import argparse
import sys
import os
import datetime
import glob
import subprocess
import re

try:
    try:
        from vector.apps.DataAPI.unit_test_api import UnitTestApi
        from vector.apps.DataAPI.unit_test_models import TestCase
    except:
        from vector.apps.DataAPI.api import Api as UnitTestApi
        from vector.apps.DataAPI.models import TestCase

    from vector.apps.DataAPI.vcproject_api import VCProjectApi
    from vector.apps.DataAPI.vcproject_models import EnvironmentType

    from vector.apps.DataAPI.cover_api import CoverApi
    from vector.apps.DataAPI.coverdb import COVERED_STATUSES
    from vector.lib.platform.vcast_platform import vcast_platform
except:
    print("VectorCAST v2019 or above is required for Squore export...")
    sys.exit(-1)
    
import getjobs

import xml.etree.cElementTree as ET
from xml.dom import minidom

variants = {}
all_functions = {}
all_test_executions = {}
enabledEnvironmentArray = []

def getEnabledEnvironments(MPname):
    output = getjobs.printEnvironmentInfo(MPname, False)

    for line in output.split("\n"):
        if line.strip():
            compiler, testsuite, environment = line.split()
            enabledEnvironmentArray.append([compiler, testsuite, environment])
                       
def environmentEnabled(comp,ts,env):
    for c,t,e in enabledEnvironmentArray:
        if comp == c and ts == t and env == e:
            return True
    return False 


def get_apis_from_vcm_file(vcm):
    if not os.path.isfile(vcm):
        raise IOError("Could not find: " + vcm)

    getEnabledEnvironments(vcm)

    # Run manage
    manage_exe = vcast_platform().manage
    manage_exe = os.path.join(os.getenv("VECTORCAST_DIR"), manage_exe)

    out = subprocess.check_output([manage_exe, "--project=" + vcm,
                                   "--build-directory-name"], stderr=subprocess.STDOUT).decode('utf-8')
                                       
    # Parse manage output
    unit_test_apis = []
    cover_apis = []
    env_name = None
    comp = None
    ts = None
    
    for line in out.splitlines():
    
        line = line.strip()
        if ":" not in line:
            continue

        k,v = line.split(":", 1)
        v = v.strip()
        if k == "Compiler":
            comp = v
        elif k == "TestSuite":
            ts = v
        if k == "Environment":
            env_name = v
        elif k == "Build Directory":
            if not environmentEnabled(comp,ts,env_name):
                print ("Disabled  : " , comp,ts,env_name)
                continue
            else:
                print ("Processing: " , comp,ts,env_name)
            
            env_dir = os.path.join(v, env_name)
            vce = env_dir + ".vce"
            vcp = env_dir + ".vcp"

            try:
                if os.path.isfile(vce):
                    unit_test_apis.append(UnitTestApi(vce))
                elif os.path.isfile(vcp):
                    cover_apis.append(CoverApi(vcp))
                else:
                    pass
            except:
                import traceback
                print ("Could not process: " + env_name + " " + traceback.format_exc())
            
            env_name = None
    
    # Separate again if you need to
    return unit_test_apis + cover_apis

def is_default_basis_path_name(tc):
    if tc.basis_path_index is None:
        return False # not a basis path
    else:
        name_re = r'^BASIS-PATH-(\d+)(-(PARTIAL|TEMPLATE))?$'
        match = re.match(name_re, tc.name)
        if match is None:
            # Not a match, so must have been renamed
            return False
        else:
            index_from_name = int(match.group(1))
            return (index_from_name == tc.basis_path_index)


def getCompilerfromConfig(config_file):
    compiler = "Unknown_Compiler"
    
    config_file = open(config_file, 'r')

    compiler = "Compiler_Not_Found"
    for line in config_file.readlines():
        match = re.match(r'^C_COMPILER_HIERARCHY_STRING: (.*)$', line)
        if match is not None:
            compiler = match.group(1)
    return str(compiler)


def process_file (vc_file, outputdir):
    if not os.path.isfile(vc_file):
        raise IOError("Could not find vectorCAST file: " + vc_file)

    if vc_file.endswith(".vce"):
        print ("Processing " + os.path.splitext(os.path.basename(vc_file))[0])

        api = UnitTestApi(vc_file)
        # print(api.environment_directory)
        # print(vars(api.testhistory))
        compiler = getCompilerfromConfig(os.path.dirname(vc_file) + "/CCAST_.CFG")        
        process_api(api, compiler, "Default_Suite", "Default_Group", os.path.dirname(vc_file))

    elif vc_file.endswith(".vcp"):
        print ("Processing " + os.path.splitext(os.path.basename(vc_file))[0])
        api = CoverApi(vc_file)
        compiler = getCompilerfromConfig(os.path.dirname(vc_file) + "/CCAST_.CFG")
        process_api(api, compiler, "Default_Suite", "Default_Group", os.path.dirname(vc_file))

    elif vc_file.endswith(".vcm"):
        # for env in Api(vc_file).Environment.all():
        for env in get_apis_from_vcm_file(vc_file):
            ##print ("Processing " + env.name)

            # print(env.Environment.first().configuration.path)
            path = ""
            if not isinstance(env, CoverApi):
                path = env.Environment.first().configuration.path    
            else:
                path = env.environment.configuration.path
    
            compiler = getCompilerfromConfig(path)
            process_api(env, compiler, "Unknown", "Unknown", path)
                # print(str(env.name) + "=" + str(env.definition.env_type))
                
                # if env.definition.env_type == EnvironmentType.UNIT:
                    # unit_test_env = env.environment_directory + "\\" + env.definition.name
                    
                    # api = UnitTestApi(unit_test_env)
                    ### xml_tests.append(process_api(api, str(env.compiler.name), str(env.testsuite.name), str(env.group.name)))
                    # process_api(api, str(env.compiler.name), str(env.testsuite.name), str(env.group.name), env.environment_directory)
                
                # else:
                    # process_api(env.api, str(env.compiler.name), str(env.testsuite.name), str(env.group.name), env.environment_directory)
                    
    fname = outputdir + "/squore_results_" + os.path.splitext(os.path.basename(vc_file))[0] + ".xml"
    
    write_xml_report(fname)
    
def process_api(a, compiler, testsuite, group, env_path):
    
    use_path_in_key = True
    if a is not None:
    
        result_map = {}
        
        variant_key = compiler + "_" + testsuite
        if use_path_in_key is True:
            variant_key = compiler + "_" + testsuite + "(" + env_path  + ")"
            
        variant_dict = {
                "variant": variant_key,
                "compiler": compiler,
                "testsuite": testsuite,
                "group": group,
                "env_path": env_path
            }
            
        # print(variant_dict)
        cover_api = None

        ref = datetime.datetime(1970, 1, 1)
        
        # Retrieving TEST RESULTS data
        if not isinstance(a, CoverApi):
            for test in a.TestCase.all():
                
                # Convert date into milliseconds
                start_sec = ((test.start_time - ref).total_seconds() * 1000) if test.start_time else ""
                created_sec = ((test.created - ref).total_seconds() * 1000) if test.created else ""
                
                test_exec_key = compiler + "/" + testsuite + "/" + group + "/" + test.function_display_name  + "/" + test.name + "(" + env_path  + ")"
                
                if test_exec_key not in all_test_executions:
                        all_test_executions[test_exec_key] = {}
                
                all_test_executions[test_exec_key]["attrib"] = {
                    "fullname": test.function_display_name  + "/" + test.name,
                    "function_name": test.function_display_name,
                    "test_name": test.name,
                    "passed": str(test.passed),
                    # "start_time": test.start_time.isoformat() if test.start_time else "",
                    "start_time": str(start_sec) if test.start_time else "",
                    "created": str(created_sec) if test.created else "",
                    "failure_reason": test.failure_reasons if test.failure_reasons else "",
                    "index": str(test.index) if test.index else "",
                    "kind": str(test.kind) if test.kind else "",
                    "execution_status": str(test.execution_status) if test.execution_status else "",
                }
                
                all_test_executions[test_exec_key]["info"] = variant_dict
                # xml_test = ET.Element("test")
                # xml_test.attrib = 
                
                if "findings" not in all_test_executions[test_exec_key]:
                        all_test_executions[test_exec_key]["findings"] = []
                
                if is_default_basis_path_name(test):
                    all_test_executions[test_exec_key]["findings"].append({"id": "R_BASIS_PATH_DEFAULT_NAME"})
                if len(test.expected) == 0:
                    all_test_executions[test_exec_key]["findings"].append({"id": "R_NO_EXPECTED_RESULTS"})
                if test.for_compound_only:
                    all_test_executions[test_exec_key]["findings"].append({"id": "R_TEST_IS_COMPOUND_ONLY"})
                if len(test.requirements) == 0:
                    all_test_executions[test_exec_key]["findings"].append({"id": "R_TEST_NO_REQUIREMENT"})
            
                result = test.cover_data # maps to the cover api
                if result is not None:
                    result_map[result.id] = all_test_executions[test_exec_key]["attrib"]["fullname"]
                    
            cover_api = a.environment.get_coverdb_api()
        else:        
            for result in a.Result.all():
                # print(result)
                # print(vars(result))
                test_exec_key = compiler + "/" + testsuite + "/" + group + "/" + result.name + "(" + env_path  + ")"
                
                if test_exec_key not in all_test_executions:
                    all_test_executions[test_exec_key] = {}
                
                all_test_executions[test_exec_key]["attrib"] = {
                    "fullname": result.name,
                    "function_name": "",
                    "test_name": result.name,
                    "passed": str(result.passed),  # VectorCAST QA only
                    # "start_time": test.start_time.isoformat() if test.start_time else "",
                    "start_time": "",
                    "created": "",
                    "failure_reason": "",
                    "index": str(result.id),
                    "kind": "COVER",
                    "execution_status": "",
                }
                
                all_test_executions[test_exec_key]["info"] = variant_dict
                
                result_map[result.id] = all_test_executions[test_exec_key]["attrib"]["fullname"]
            cover_api = a
        
        # Retrieving COVERAGE data
        
        for fil in cover_api.File.all():
            for func in fil.functions:
                results = set()
                func_key = fil.path + "/" + func.name
                
                if func_key not in all_functions:
                    all_functions[func_key] = {}
                    
                    
                all_functions[func_key]["attrib"] = {"name": str(func.name), "file": fil.path}
         
                if func.file.has_statement_coverage:
                    object = func.metrics.statements
                    tested = func.metrics.aggregate_covered_statements
                    if "statement" not in all_functions[func_key]:
                        all_functions[func_key]["statement"] = {"OBJECT_STAT": object, "TESTED_STAT": tested}
                    else:
                        all_functions[func_key]["statement"]["OBJECT_STAT"] = max(all_functions[func_key]["statement"]["OBJECT_STAT"], object)
                        all_functions[func_key]["statement"]["TESTED_STAT"] = max(all_functions[func_key]["statement"]["TESTED_STAT"], tested)

                    
                if func.file.has_branch_coverage or func.file.has_mcdc_coverage:
                    object = func.metrics.mcdc_branches+func.metrics.branches
                    tested = func.metrics.aggregate_covered_mcdc_branches+func.metrics.aggregate_covered_branches
                    if "branch" not in all_functions[func_key]:
                        all_functions[func_key]["branch"] = {"OBJECT_BRANCH": object, "TESTED_BRANCH": tested}
                    else:
                        all_functions[func_key]["branch"]["OBJECT_BRANCH"] = max(all_functions[func_key]["branch"]["OBJECT_BRANCH"], object)
                        all_functions[func_key]["branch"]["TESTED_BRANCH"] = max(all_functions[func_key]["branch"]["TESTED_BRANCH"], tested)

                
                if func.file.has_mcdc_coverage:
                    object = func.metrics.mcdc_pairs
                    tested = func.metrics.aggregate_covered_mcdc_pairs
                    if "mcdc" not in all_functions[func_key]:
                        all_functions[func_key]["mcdc"] = {"OBJECT_MCDC": object, "TESTED_MCDC": tested}
                    else:
                        all_functions[func_key]["mcdc"]["OBJECT_MCDC"] = max(all_functions[func_key]["mcdc"]["OBJECT_MCDC"], object)
                        all_functions[func_key]["mcdc"]["TESTED_MCDC"] = max(all_functions[func_key]["mcdc"]["TESTED_MCDC"], tested)


                for statement in func.statements:
                    if "covered_statement" not in all_functions[func_key]:
                        all_functions[func_key]["covered_statement"] = []
                    if "uncovered_statement" not in all_functions[func_key]:
                        all_functions[func_key]["uncovered_statement"] = []

                    status = statement.covered() in COVERED_STATUSES
                    lines = []
                    for i in range(statement.start_line, statement.end_line + 1):
                        lines.append(i)
                    
                    if status is True:
                        for i in lines:
                            if i not in all_functions[func_key]["covered_statement"]:
                                all_functions[func_key]["covered_statement"].append(i)
                            if i in all_functions[func_key]["uncovered_statement"]:
                                all_functions[func_key]["uncovered_statement"].remove(i)
                    else:
                        for i in lines:
                            if i not in all_functions[func_key]["covered_statement"]:
                                if i not in all_functions[func_key]["uncovered_statement"]:
                                    all_functions[func_key]["uncovered_statement"].append(i)
                    
                    
                    if "tested_by" not in all_functions[func_key]:
                        all_functions[func_key]["tested_by"]=[]
   
                    for test in statement.results:
                        # if test.name not in all_functions[func_key]["tested_by"]:
                            # all_functions[func_key]["tested_by"].append(test.name)
                                
                        if test.id in result_map:
                            # test_name = result_map[test.id]
                            #if "BASIS-PATH-001-PARTIAL" in result_map[test.id]:    
                            #    print(func_key + "=>" + result_map[test.id])
                            
                            if result_map[test.id] not in all_functions[func_key]["tested_by"]:
                                all_functions[func_key]["tested_by"].append(result_map[test.id])
  

def xmlToString(xml_node):
        return  minidom.parseString(ET.tostring(xml_node)).toprettyxml(indent="   ")
    
def write_xml(file, xml_content, mode):
    fichier = open(file, mode)
    fichier.write(xmlToString(xml_content))
    fichier.close()

def write_xml_report(file):
    root = ET.Element("vectorCAST_Manage_report")
    xml_tests = ET.Element("tests_results")
    xml_coverage = ET.Element("coverage")
    
    for exec_key in all_test_executions:
        node = ET.Element("test_execution")
        node.attrib = all_test_executions[exec_key]["attrib"]
        key = ET.Element("key")
        key.attrib = {"value": exec_key}
        node.append(key)
        info = ET.Element("info")
        info.attrib = all_test_executions[exec_key]["info"]
        node.append(info)
        if "findings" in all_test_executions[exec_key]:
            for f in all_test_executions[exec_key]["findings"]:
                finding = ET.Element("finding")
                finding.attrib = f
                node.append(finding)
        xml_tests.append(node)
    
    for func in all_functions:
        # print(all_functions[func])
        xml_func = ET.Element("function")
        xml_func.attrib = all_functions[func]["attrib"]
        
        xml_metrics = ET.Element("metrics")
        xml_metrics.attrib = {
            "OBJECT_STAT": str(all_functions[func]["statement"]["OBJECT_STAT"]) if "statement" in all_functions[func] else "",
            "TESTED_STAT": str(all_functions[func]["statement"]["TESTED_STAT"]) if "statement" in all_functions[func] else "",
            "OBJECT_BRANCH": str(all_functions[func]["branch"]["OBJECT_BRANCH"]) if "branch" in all_functions[func] else "",
            "TESTED_BRANCH": str(all_functions[func]["branch"]["TESTED_BRANCH"]) if "branch" in all_functions[func] else "",
            "OBJECT_MCDC": str(all_functions[func]["mcdc"]["OBJECT_MCDC"]) if "mcdc" in all_functions[func] else "",
            "TESTED_MCDC": str(all_functions[func]["mcdc"]["TESTED_MCDC"]) if "mcdc" in all_functions[func] else ""
        }
        xml_func.append(xml_metrics)
        
      
        xml_covered_lines = ET.Element("covered_lines")
        
        covered_lines = ""
        for line in sorted(all_functions[func]["covered_statement"]):
            covered_lines += str(line) + ","
        covered_lines = covered_lines[:-1]
        
        xml_covered_lines.attrib = {"value": covered_lines}
        xml_func.append(xml_covered_lines)
        # xml_uncovered_lines = ET.Element("uncovered_lines")
        # xml_uncovered_lines.attrib = {"value": str(all_functions[func]["uncovered_statement"])}
        # xml_func.append(xml_uncovered_lines)
        
        for test in all_functions[func]["tested_by"]:
            # print(test)
            xml_link = ET.Element("link")
            xml_link.attrib = {"link_id": "TESTED", "from": func, "to": test}
            xml_func.append(xml_link)
            
        xml_coverage.append(xml_func)
    root.append(xml_tests)
    root.append(xml_coverage)
    write_xml(file, root, "w")

  
  
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("project_file", help="A VectorCAST .vcm project.")
    parser.add_argument("-o", "--output_dir", help="The export directory.", default = "xml_data")
    args = parser.parse_args()
    
    if os.path.isfile(args.project_file + ".vcm"):
        args.project_file += ".vcm"
    
    process_file(args.project_file, args.output_dir)

if __name__ == "__main__":
    sys.exit(main())
