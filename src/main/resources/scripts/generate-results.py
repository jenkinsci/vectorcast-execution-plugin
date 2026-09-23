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

from __future__ import print_function
from __future__ import absolute_import

import os
import argparse
import re
import shutil
import time
import cobertura
from runtime_logging import get_logger, log_exception
from vcast_utils import getVectorCASTEncoding
from parse_console_for_cbt import ParseConsoleForCBT
from vector.apps.DataAPI.vcproject_api import VCProjectApi

encFmt = getVectorCASTEncoding()

verbose = False
print_exc = False


def running_in_jenkins(environ=None):
    """Recognize Jenkins-invoked scripts, including older generated jobs."""
    env = os.environ if environ is None else environ
    return any(env.get(name) for name in (
        "JENKINS_URL", "HUDSON_URL", "JENKINS_HOME", "BUILD_URL"))

def require_supported_data_api(project_file):
    """Reject unsupported DataAPI versions before changing report outputs."""
    api = VCProjectApi(project_file)
    try:
        version = str(api.tool_version)
    finally:
        api.close()
    match = re.match(r"^(\d{2})(?!\d)", version.strip())
    if match is None or int(match.group(1)) < 24:
        raise RuntimeError(
            "VectorCAST 24 or newer DataAPI is required; found " + version)


def useManageAPI(FullManageProjectName, cbtDict, generate_exec_rpt_each_testcase, use_archive_extract, report_only_failures, no_full_report, useStartLine, teePrint, use_cte):
    global verbose

    print("Using VCProjectApi")
    
    xml_file = ""
    
    try:
        from generate_junit import GenerateManageXml

        xml_file = GenerateManageXml(FullManageProjectName, 
                               verbose, 
                               cbtDict,
                               generate_exec_rpt_each_testcase,
                               use_archive_extract,
                               report_only_failures,
                               no_full_report,
                               print_exc,
                               useStartLine, teePrint, use_cte)
                               
        if xml_file.api != None:
            xml_file.generate_testresults()
        else:
            print("   Skipping project without an available DataAPI")
            print("\n\n")
            print ("******************************************************")
            print ("** Environment's that only use imported results     **")
            print ("** will not properly generate metrics with this     **")
            print ("** version of VectorCAST.                           **")
            print ("******************************************************")
            print("\n\n")
    
    except Exception as e:
        log_exception()


    try:       
        return xml_file.passed_count, xml_file.failed_count
    except:
        return 0, 0


def cleanupDirectory(path, teePrint):

    # if the path exists, try to delete all file in it
    if os.path.isdir(path):
        shutil.rmtree(path)
    os.mkdir(path)

def cleanupOldBuilds(teePrint):
    for path in ["xml_data","management","execution"]:
        cleanupDirectory(path, teePrint)

# Generate Jenkins reports for a VectorCAST Manage project.
def buildReports(FullManageProjectName = None,
    level = None,
    envName = None,
    generate_individual_reports = True,
    timing = False,
    cbtDict = None,
    use_archive_extract = False,
    report_only_failures = False,
    no_full_report = False,
    use_ci = "",
    xml_data_dir = "xml_data",
    useStartLine = False,
    teePrint = None,
    use_cte = False,
    extended_coverage = None,
    generate_coverage = True):
        
    if timing:
        print("Start report generation: " + str(time.time()))
        
    if not os.path.isfile(FullManageProjectName) and not os.path.isfile(FullManageProjectName + ".vcm"):
        raise FileNotFoundError(FullManageProjectName + " does not exist")

    require_supported_data_api(FullManageProjectName)
    if teePrint is None:
        teePrint = get_logger()

    cleanupOldBuilds(teePrint)
    passed_count, failed_count = useManageAPI(
        FullManageProjectName, cbtDict, generate_individual_reports,
        use_archive_extract, report_only_failures, no_full_report,
        useStartLine, teePrint, use_cte)

    if generate_coverage:
        if extended_coverage is None:
            extended_coverage = running_in_jenkins()
        cobertura.generateCoverageResults(
            FullManageProjectName, xml_data_dir=xml_data_dir,
            extended=extended_coverage)

    with open("unit_test_fail_count.txt", "wb") as fd:
        fd.write(str(failed_count).encode(encFmt, "replace"))
    with open("unit_test_passfail_count.txt", "wb") as fd:
        fd.write("{} {}".format(passed_count, failed_count).encode(encFmt, "replace"))
    if timing:
        print("Complete report generate: " + str(time.time()))
        
    return failed_count, passed_count

        
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject',                    help='Manager Project Name')
    parser.add_argument('-v', '--verbose',                  help='Enable verbose output', action="store_true")
    parser.add_argument('-l', '--level',                    help='Level for doing single environment.  Should be in the form of compiler/testsuite')
    parser.add_argument('-e', '--environment',              help='Environment Name if only doing single environment')
    parser.add_argument('-g', '--dont-generate-individual-reports',
                        help='Do not generate per-test execution reports',
                        action="store_true", default=False)
    parser.add_argument('--wait_time',                      help='Time (in seconds) to wait between execution attempts', type=int, default=30)
    parser.add_argument('--wait_loops',                     help='Number of times to retry execution', type=int, default=1)
    parser.add_argument('--timing',                         help='Display timing information for report generation', action="store_true", default = False)
    parser.add_argument('--buildlog',                       help='Build Log for CBT Statitics', default = None)
    parser.add_argument('--extended',                       help='Generate extended Cobertura coverage XML', action='store_true')
    
    ## Hidden because they are specific to customer need or testing
    parser.add_argument('--junit',                          help=argparse.SUPPRESS, action="store_true")
    parser.add_argument('--junit_use_cte_for_classname',    help=argparse.SUPPRESS, action="store_true", dest="use_cte")
    parser.add_argument('--print_exc',                      help=argparse.SUPPRESS, action="store_true")
    parser.add_argument('--use_archive_extract',            help=argparse.SUPPRESS, action="store_true", default = False)
    parser.add_argument('--report_only_failures',           help=argparse.SUPPRESS, action="store_true", default = False)
    parser.add_argument('--no_full_report',                 help=argparse.SUPPRESS, action="store_true", default = False)

    args = parser.parse_args()
    
    if args.use_archive_extract and (not args.buildlog or not os.path.exists(args.buildlog)):
        print("Must have a valid --buildlog file to use --use_archive_extract")
        print("The option use_archive_extract is disabled")
        args.use_archive_extract = False
    
    timing = args.timing
    
    if timing:
        print("Start: " + str(time.time()))
        
    generate_individual_reports = not args.dont_generate_individual_reports

    if args.verbose:
        verbose = True
        
    if args.print_exc or verbose:
        print_exc = True
        
    if args.buildlog and os.path.exists(args.buildlog):
        with open(args.buildlog,"rb") as fd:
            buildLogData = [line.decode(encFmt, "replace") for line in fd.readlines()]
            
        cbt = ParseConsoleForCBT(verbose)
        cbtDict = cbt.parse(buildLogData)
        
        if timing:
            print("CBT Parse: " + str(time.time()))
        
    else:
        cbtDict = None
            
    if timing:
        print("Getting enabled envs: " + str(time.time()))

    # Used for VC19 SP2 onwards
    os.environ['VCAST_RPTS_SELF_CONTAINED'] = 'FALSE'
    # Set VCAST_MANAGE_PROJECT_DIRECTORY to match .vcm directory
    os.environ['VCAST_MANAGE_PROJECT_DIRECTORY'] = os.path.abspath(args.ManageProject).rsplit(".",1)[0]
 
    teePrint = get_logger()
    buildReports(args.ManageProject,
            args.level,
            args.environment,
            generate_individual_reports,
            timing,
            cbtDict,
            args.use_archive_extract,
            args.report_only_failures,
            args.no_full_report,
            use_ci = "",
            xml_data_dir = "xml_data",
            useStartLine = False,
            teePrint = teePrint,
            use_cte = args.use_cte,
            extended_coverage = args.extended or running_in_jenkins())
    
    import archive_extract_reports
        
    if (args.use_archive_extract):
        archive_extract_reports.extract(verbose)
        if timing:
            print("extracting reports: " + str(time.time()))


