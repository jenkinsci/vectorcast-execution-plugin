import concurrent.futures
from vector.apps.DataAPI.unit_test_api import UnitTestApi
from vector.apps.DataAPI.vcproject_api import VCProjectApi
from vector.apps.DataAPI.cover_api import CoverApi

import sys, os
from pprint import pprint
import argparse
import subprocess


def dump(obj):
    if hasattr(obj, '__dict__'): 
        return vars(obj) 
    else:
        try:
            return {attr: getattr(obj, attr, None) for attr in obj.__slots__} 
        except:
            return str(obj)

    def runSingleAtgCommand(self, cmd):
        
        """Run a single shell command and return the output."""
        try:
            result = subprocess.run(atgCmd, shell=True, capture_output=True, text=True)
            return result.stdout.strip()
        except Exception as e:
            return f'Error: {e}'

class RunFullReportsParallel(object):
    
    def __init__(self):
        parser = argparse.ArgumentParser()
        parser.add_argument('ManageProject', help='VectorCAST Project Name')
        parser.add_argument('-j','--jobs', help='Number of concurrent jobs (default = 4)', default="4")
        parser.add_argument('--ci', help='Use Continuous Integration Licenses', action="store_true", default = False)
        
        args = parser.parse_args()
        
        self.mpName = args.ManageProject

        self.api = VCProjectApi(self.mpName)
        self.results = self.api.project.repository.get_full_status([])
        self.workspace = self.api.project.workspace

        self.max_concurrent = int(args.jobs)
        self.envDict = {}

        self.info = []

        self.VCD = os.environ['VECTORCAST_DIR']
        
        for env in self.api.Environment.all():
            if env.is_active:
                self.envDict[env.level._full_path] = env
                
        if args.ci:
            os.environ['VCAST_USE_CI_LICENSES'] = "1"


    def generate_report(self, key):
        
        env = self.envDict[key]
        
        report_name = ""
        
        build_dir = self.api.project.workspace + '/' + env.relative_working_directory
        
        if len(key.split("/")) != 3:
            comp, ts, group, env_name = key.split("/")
        else:
            comp, ts, env_name = key.split("/")
                
        report_name = "management/" + env_name + "_" + comp + "_" + ts
                
        try:
            cmd = ""

            if isinstance(env.api,CoverApi):
                cmd = self.VCD + "/clicast -e " + env.name + " COVER REPORT AGGREGATE " + os.getcwd() + "/" + report_name + "_AGGREGATE_REPORT.html"
                result = subprocess.run(cmd.split(), capture_output=True, text=True, cwd=build_dir)
                
            elif isinstance(env.api,UnitTestApi):
                cmd = self.VCD + "/clicast -e " + env.name + " REPORT CUSTOM FULL " + os.getcwd() + "/" + report_name + "_FULL_REPORT.html"
                result = subprocess.run(cmd.split(), capture_output=True, text=True, cwd=build_dir)
                
            else:
                return f"Error: Cannot find the environment {build_dir}/{env.name}(.vcp/.vce): {cmd}"

            return result.stdout.strip()
        except Exception as e:
            return f'Error: {e}'

        return f"Success: {report_name}: {result}"


    def run(self):
                        
        with concurrent.futures.ThreadPoolExecutor(max_workers=self.max_concurrent) as executor:
            futures = [executor.submit(self.generate_report, key) for key in self.envDict.keys()]
            concurrent.futures.wait(futures)

            
        for future in concurrent.futures.as_completed(futures):
            try:
                result = future.result()  # This raises the exception if one occurred
                print(result)
            except Exception as e:
                report_name = futures[future]
                print(f"Exception in {report_name}: {e}")

        print("All reports completed!")
        
if __name__ == '__main__':
    
    runner = RunFullReportsParallel()
    
    runner.run()
    