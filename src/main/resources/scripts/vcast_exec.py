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

import os, subprocess,argparse, glob, sys, shutil 

from managewait import ManageWait

import patch_rgw_directory as rgw

try:
    import generate_results 
except:    
    try:
        import importlib
        generate_results = importlib.import_module("generate-results")
    except:
        vc_script = os.path.join(os.environ['WORKSPACE'], "vc_scripts", "generate-results.py")
        import imp
        generate_results = imp.load_source("generate_results", vc_script)

if sys.version_info[0] < 3:
    python_path_updates = os.path.join(os.environ['VECTORCAST_DIR'], "DATA", "python")
    sys.path.append(python_path_updates)

try:
    import parallel_build_execute
except:
    import prevcast_parallel_build_execute as parallel_build_execute

from vcast_utils import checkVectorCASTVersion, dump

from enum import Enum

class CITool(Enum):
    JENKINS = "Jenkins"
    GITLAB = "GitLab CI"
    AZURE = "Azure Pipelines"
    GITHUB = "GitHub Actions"
    CIRCLECI = "CircleCI"
    TRAVIS = "Travis CI"
    BITBUCKET = "Bitbucket Pipelines"
    TEAMCITY = "TeamCity"
    BAMBOO = "Bamboo"
    UNKNOWN = "Unknown CI/CD"

class VectorCASTExecute(object):
    
    def detect_ci_tool(self):
        if "JENKINS_URL" in os.environ:
            self.ciTool = CITool.JENKINS
        elif "GITLAB_CI" in os.environ:
            self.ciTool = CITool.GITLAB
        elif "AZURE_PIPELINES" in os.environ or "BUILD_SOURCEVERSION" in os.environ:
            self.ciTool =  CITool.AZURE
        elif "GITHUB_ACTIONS" in os.environ:
            self.ciTool =  CITool.GITHUB
        elif "CIRCLECI" in os.environ:
            self.ciTool =  CITool.CIRCLECI
        elif "TRAVIS" in os.environ:
            self.ciTool =  CITool.TRAVIS
        elif "BITBUCKET_BUILD_NUMBER" in os.environ:
            self.ciTool =  CITool.BITBUCKET
        elif "TEAMCITY_VERSION" in os.environ:
            self.ciTool =  CITool.TEAMCITY
        elif "BAMBOO_BUILDNUMBER" in os.environ:
            self.ciTool =  CITool.BAMBOO
        else:
            self.ciTool =  CITool.UNKNOWN

    def __init__(self, args):

        self.detect_ci_tool()

        # setup default values
        self.azure = args.azure
        self.gitlab = args.gitlab
        self.print_exc = args.print_exc
        self.print_exc = args.print_exc
        self.timing = args.timing
        self.jobs = args.jobs
        self.sonarqube = args.sonarqube
        self.junit = args.junit
        self.cobertura = args.cobertura
        self.cobertura_extended = args.cobertura_extended
        self.metrics = args.metrics
        self.fullstatus = args.fullstatus
        self.aggregate = args.aggregate
        self.pclp_output_html = args.pclp_output_html
        self.pclp_input = args.pclp_input
        
        self.html_base_dir = args.html_base_dir
        self.use_cte = args.use_cte
        
        if args.exit_with_failed_count == 'not present':
            self.useJunitFailCountPct = False
            self.junit_percent_to_fail = 0
        elif args.exit_with_failed_count == '(default 0)':
            self.useJunitFailCountPct = True
            self.junit_percent_to_fail = 0
        else:
            self.useJunitFailCountPct = True
            self.junit_percent_to_fail = int(args.exit_with_failed_count)
        self.failed_count = 0
        
        if args.output_dir:
            self.xml_data_dir = os.path.join(args.output_dir, 'xml_data')
            if not os.path.exists(self.xml_data_dir):
                os.makedirs(self.xml_data_dir)
        else:
            self.xml_data_dir = "xml_data"
        
        if args.build and not args.build_execute:
            self.build_execute = "build"
            self.vcast_action = "--vcast_action " + self.build_execute
        elif args.build_execute:
            self.build_execute = "build-execute"
            self.vcast_action = "--vcast_action " + self.build_execute
        else:
            self.build_execute = ""
            self.vcast_action = ""
        
        self.source_root = args.source_root
        self.verbose = args.verbose
        self.FullMP = args.ManageProject
        self.mpName = os.path.basename(args.ManageProject)[:-4]

        if args.ci:
            self.useCI = " --use_ci "
            self.ci = " --ci "
        else:
            self.useCI = ""
            self.ci = ""
            
        if args.incremental:
            self.useCBT = " --incremental "
        else:
            self.useCBT = ""
                  
        self.useLevelEnv = False
        self.environment = None
        self.level = None
        self.compiler = None
        self.testsuite = None
        self.reportsName = ""
        self.env_option = ""
        self.level_option = ""
        self.needIndexHtml = False

        # if a manage level was specified...
        if args.level:        
            self.useLevelEnv = True
            self.level = args.level
            
            # try level being Compiler/TestSuite
            try:
                self.compiler, self.testsuite = args.level.split("/")
                self.reportsName = "_" + self.compiler + "_" + self.testsuite
            except:
                # just use the compiler name
                self.compiler = args.level
                self.reportsName = "_" + self.compiler
                
            self.level_option = "--level " + args.level + " "

        # if an environment was specified
        if args.environment:
            # afix the proper settings for commands later and report names
            self.useLevelEnv = True
            self.environment = args.environment
            self.env_option = "--environment " + args.environment + " "
            self.reportsName += "_" + self.environment
                  
        if self.useLevelEnv:
            self.build_log_name = self.reportsName + "_build.log"    
        else:
            self.build_log_name = self.mpName + "_build.log"    

        self.manageWait = ManageWait(self.verbose, "", 30, 1, self.FullMP, self.ci)
            
        self.cleanup("junit", "test_results_")
        self.cleanup("cobertura", "coverage_results_")
        self.cleanup("sonarqube", "test_results_")
        self.cleanup("pclp", "gl-code-quality-report.json")
        self.cleanup(".", self.mpName + "_aggregate_report.html")
        self.cleanup(".", self.mpName + "_metrics_report.html")
        
    def cleanup(self, dirName, fname):
        for file in glob.glob(os.path.join(self.xml_data_dir, dirName, fname + "*.*")):
            try:
                os.remove(file);
            except:
                print("Error removing file after failed to remove directory: " +  file)
                
        try:
            shutil.rmtree(os.path.join(self.xml_data_dir , dirName))
        except:
            pass

    
    def generateIndexHtml(self):
        if not checkVectorCASTVersion(21):
            print("Cannot create index.html. Please upgrade VectorCAST")
        else:
            print("Creating index.html")
        
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
            
            from create_index_html import create_index_html
            create_index_html(self.FullMP, self.ciTool == CITool.GITLAB)
    
    def runJunitMetrics(self):
        print("Creating JUnit Metrics")

        generate_results.verbose = self.verbose
        generate_results.print_exc = self.print_exc
        generate_results.timing = self.timing
        
        if checkVectorCASTVersion(21, quiet=True):
            self.useStartLine = True
        else:
            self.useStartLine = False
        
        self.failed_count, self.passed_count = generate_results.buildReports(
                FullManageProjectName = self.FullMP,
                level =self.level,
                envName = self.environment,
                generate_individual_reports = True,
                timing = self.timing,
                cbtDict = None,
                use_archive_extract = False,
                report_only_failures = False,
                no_full_report = False,
                use_ci = self.ci,
                xml_data_dir = self.xml_data_dir,
                useStartLine = self.useStartLine)
                
        # calculate the failed percentage
        if (self.failed_count + self.passed_count > 0):
            self.failed_pct = 100 * self.failed_count/ (self.failed_count + self.passed_count)
        else:
            self.failed_pct = 0
        
        # if the failed percentage is less that the specified limit (default = 0)
        # clear the failed count
        if self.useJunitFailCountPct and self.failed_pct < self.junit_percent_to_fail:
            self.failed_count = 0
            
        self.needIndexHtml = True

    def runLcovMetrics(self):
    
        if not checkVectorCASTVersion(21):
            print("XXX Cannot create LCOV metrics. Please upgrade VectorCAST\n")
        else:
            print("Creating LCOV Metrics")
            import generate_lcov
            generate_lcov.generateCoverageResults(self.FullMP, self.xml_data_dir, verbose = self.verbose, source_root = self.source_root)

    def runCoberturaMetrics(self):
        if not checkVectorCASTVersion(21):
            print("Cannot create Cobertura metrics. Please upgrade VectorCAST")
        else:
            import cobertura

            if self.cobertura_extended:
                print("Creating Extended Cobertura Metrics")
            else:
                print("Creating Cobertura Metrics")

            cobertura.generateCoverageResults(self.FullMP, self.azure, self.xml_data_dir, verbose = self.verbose, 
                extended=self.cobertura_extended, source_root = self.source_root)

    def runSonarQubeMetrics(self):
        if not checkVectorCASTVersion(21):
            print("Cannot create SonarQube metrics. Please upgrade VectorCAST")
        else:
            print("Creating SonarQube Metrics")
            import generate_sonarqube_testresults 
            generate_sonarqube_testresults.run(self.FullMP, self.xml_data_dir)
        
    def runPcLintPlusMetrics(self):
        print("Creating PC-lint Plus Metrics")
        if not checkVectorCASTVersion(21):
            print("Cannot create PC-Lint Plus HTML report. Please upgrade VectorCAST")
        else:
            import generate_pclp_reports 
            os.makedirs(os.path.join(self.xml_data_dir,"pclp"))
            report_name = os.path.join(self.xml_data_dir,"pclp","gl-code-quality-report.json")
            print("PC-lint Plus Metrics file: " + report_name)
            generate_pclp_reports.generate_reports(self.pclp_input, output_gitlab = report_name)
            
            if args.pclp_output_html:
                print("Creating PC-lint Plus Findings")
                generate_pclp_reports.generate_html_report(self.FullMP, self.pclp_input, self.pclp_output_html)
            
    def runReports(self):
        if self.aggregate:
            self.manageWait.exec_manage_command ("--create-report=aggregate --output=" + self.mpName + "_aggregate_report.html")
            self.needIndexHtml = True
        if self.metrics:
            self.manageWait.exec_manage_command ("--create-report=metrics --output=" + self.mpName + "_metrics_report.html")
            self.needIndexHtml = True
        if self.fullstatus:
            self.manageWait.exec_manage_command ("--full-status=" + self.mpName + "_full_status_report.html")
            self.needIndexHtml = True
            
    def generateTestCaseMgtRpt(self):
        if not os.path.exists("management"):
            os.makedirs("management")
        else:
            for file in glob.glob("management/*_management_report.html"):
                os.remove(file)
                
        if checkVectorCASTVersion(21):
            from vector.apps.DataAPI.vcproject_api import VCProjectApi
                                   
            with VCProjectApi(self.FullMP) as vcprojApi:
                for env in vcprojApi.Environment.all():
                    if not env.is_active:
                        continue
                            
                    self.needIndexHtml = True
                    
                    report_name = env.compiler.name + "_" + env.testsuite.name + "_" + env.name + "_management_report.html"
                    report_name = os.path.join("management",report_name)
                    env.api.report(report_type="MANAGEMENT_REPORT", formats=["HTML"], output_file=report_name)
        else:
            print("Cannot create Test Case Management HTML report. Please upgrade VectorCAST")

        
    def exportRgw(self):
        rgw.updateReqRepo(VC_Manage_Project=self.FullMP, VC_Workspace=os.getcwd() , top_level=False)
        self.manageWait.exec_manage_command ("--clicast-args rgw export")

    def runExec(self):

        self.manageWait.exec_manage_command ("--status")
        self.manageWait.exec_manage_command ("--force --release-locks")
        self.manageWait.exec_manage_command ("--config VCAST_CUSTOM_REPORT_FORMAT=HTML")

        if self.useLevelEnv:
            output = "--output " + self.mpName + self.reportsName + "_rebuild.html"
        else:
            output = ""
            
        if self.jobs != "1" and checkVectorCASTVersion(20, True):
            
            # setup project for parallel execution
            self.manageWait.exec_manage_command ("--config VCAST_DEPENDENCY_CACHE_DIR=./vcqik")

            # should work for pre-vcast parallel_build_execute or vcast parallel_build_execute
            pstr = "--project " + self.FullMP
            jstr = "--jobs="+str(self.jobs)
            cstr = "" if (self.compiler == None) else "--compiler="+self.compiler
            tstr = "" if (self.testsuite == None) else "--testsuite="+self.testsuite
            cbtStr = self.useCBT
            ciStr = self.useCI
            vbStr = "--verbose" if (self.verbose) else ""
            
            # filter out the blank ones
            callList = []
            for s in [pstr, jstr, cstr, tstr, cbtStr, ciStr, vbStr, self.vcast_action]:
                if s != "":
                    s = s.strip()
                    callList.append(s)

            callStr = " ".join(callList)
            parallel_build_execute.parallel_build_execute(callStr)

        else:      
            cmd = "--" + self.build_execute + " " + self.useCBT + self.level_option + self.env_option + output 
            build_log = self.manageWait.exec_manage_command (cmd)
            open(self.build_log_name,"w").write(build_log)


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject', help='VectorCAST Project Name')
    
    actionGroup = parser.add_argument_group('Script Actions', 'Options for the main tasks')
    actionGroup.add_argument('--build-execute', help='Builds and exeuctes the VectorCAST Project', action="store_true", default = False)
    parser_specify = actionGroup.add_mutually_exclusive_group()
    parser_specify.add_argument('--build',       help='Only builds the VectorCAST Project', action="store_true", default = False)
    parser_specify.add_argument('--incremental', help='Use Change Based Testing (Cannot be used with --build)', action="store_true", default = False)

    metricsGroup = parser.add_argument_group('Metrics Options', 'Options generating metrics')
    metricsGroup.add_argument('--output_dir',  help='Set the base directory of the xml_data directory. Default is the workspace directory', default = None)
    metricsGroup.add_argument('--source_root', help='Set the absolute path for the source file in coverage reporting', default = "")
    metricsGroup.add_argument("--html_base_dir", help='Set the base directory of the html_reports directory. The default is the workspace directory', default = "html_reports")
    metricsGroup.add_argument('--cobertura', help='Generate coverage results in Cobertura xml format', action="store_true", default = False)
    metricsGroup.add_argument('--cobertura_extended', help='Generate coverage results in extended Cobertura xml format', action="store_true", default = False)
    metricsGroup.add_argument('--lcov', help='Generate coverage results in an LCOV format', action="store_true", default = False)
    metricsGroup.add_argument('--junit', help='Generate test results in Junit xml format', action="store_true", default = False)
    metricsGroup.add_argument('--export_rgw', help='Export RGW data', action="store_true", default = False)
    metricsGroup.add_argument('--junit_use_cte_for_classname', help=argparse.SUPPRESS, action="store_true", dest="use_cte")
    metricsGroup.add_argument('--sonarqube', help='Generate test results in SonarQube Generic test execution report format (CppUnit)', action="store_true", default = False)
    metricsGroup.add_argument('--pclp_input', help='Generate static analysis results from PC-lint Plus XML file to generic static analysis format (codequality)', action="store", default = None)
    metricsGroup.add_argument('--pclp_output_html', help='Generate static analysis results from PC-lint Plus XML file to an HTML output', action="store", default = "pclp_findings.html")
    metricsGroup.add_argument('--exit_with_failed_count', help='Returns failed test case count as script exit.  Set a value to indicate a percentage above which the job will be marked as failed', 
                               nargs='?', default='not present', const='(default 0)')

    reportGroup = parser.add_argument_group('Report Selection', 'VectorCAST Manage reports that can be generated')
    reportGroup.add_argument('--aggregate', help='Generate aggregate coverage report VectorCAST Project', action="store_true", default = False)
    reportGroup.add_argument('--metrics', help='Generate metrics reports for VectorCAST Project', action="store_true", default = False)
    reportGroup.add_argument('--fullstatus', help='Generate full status reports for VectorCAST Project', action="store_true", default = False)
    reportGroup.add_argument('--tcmr', help='Generate Test Cases Management Reports for each VectorCAST environment in project', action="store_true", default = False)

    beGroup = parser.add_argument_group('Build/Execution Options', 'Options that effect build/execute operation')
    
    beGroup.add_argument('--jobs', help='Number of concurrent jobs (default = 1)', default="1")
    beGroup.add_argument('--ci', help='Use Continuous Integration Licenses', action="store_true", default = False)
    beGroup.add_argument('-l', '--level',   help='Environment Name if only doing single environment.  Should be in the form of compiler/testsuite', default=None)
    beGroup.add_argument('-e', '--environment',   help='Environment Name if only doing single environment.', default=None)

    parser_specify = beGroup.add_mutually_exclusive_group()
    parser_specify.add_argument('--gitlab', help='Build using GitLab CI (default)', action="store_true", default = True)
    parser_specify.add_argument('--azure',  help='Build using Azure DevOps', action="store_true", default = False)

    actionGroup = parser.add_argument_group('Script Debug ', 'Options used for debugging the script')
    actionGroup.add_argument('--print_exc', help='Prints exceptions', action="store_true", default = False)
    actionGroup.add_argument('--timing', help='Prints timing information for metrics generation', action="store_true", default = False)
    actionGroup.add_argument('-v', '--verbose',   help='Enable verbose output', action="store_true", default = False)
    

    args = parser.parse_args()
    
    if args.ci:
        os.environ['VCAST_USE_CI_LICENSES'] = "1"
        
    os.environ['VCAST_MANAGE_PROJECT_DIRECTORY'] = os.path.abspath(args.ManageProject).rsplit(".",1)[0]

    if not os.path.isfile(args.ManageProject):
        print ("Manage project (.vcm file) provided does not exist: " + args.ManageProject)
        print ("exiting...")
        sys.exit(-1)

        
    vcExec = VectorCASTExecute(args)
    
    if args.build_execute or args.build:
        vcExec.runExec()
        
    if args.junit or vcExec.useJunitFailCountPct:
        vcExec.runJunitMetrics()

    if args.cobertura or args.cobertura_extended:
        vcExec.runCoberturaMetrics()
        
    if args.lcov:
        vcExec.runLcovMetrics()

    if args.sonarqube:
        vcExec.runSonarQubeMetrics()

    if args.pclp_input:
        vcExec.runPcLintPlusMetrics()

    if args.aggregate or args.metrics or args.fullstatus:
        vcExec.runReports()

    if vcExec.useJunitFailCountPct:
        print("--exit_with_failed_count=" + args.exit_with_failed_count + " specified.  Fail Percent = " + str(round(vcExec.failed_pct,0)) + "% Return code: ", str(vcExec.failed_count))
        sys.exit(vcExec.failed_count)
        
    if args.tcmr:
        vcExec.generateTestCaseMgtRpt()

    if vcExec.needIndexHtml:
        vcExec.generateIndexHtml()
        
    if args.export_rgw:
        vcExec.exportRgw()
        
