import sys, os
from pprint import pprint
import argparse
import subprocess

from vcast_utils import checkVectorCASTVersion, dump

if not checkVectorCASTVersion(20, quiet = True):
    print("Full reports generated by previous call to generate-results.py")
    sys.exit()
else:
    import multiprocessing
    from vector.apps.DataAPI.unit_test_api import UnitTestApi
    from vector.apps.DataAPI.vcproject_api import VCProjectApi
    from vector.apps.DataAPI.cover_api import CoverApi


def dump(obj):
    if hasattr(obj, '__dict__'):
        return vars(obj)
    else:
        try:
            return {attr: getattr(obj, attr, None) for attr in obj.__slots__}
        except:
            return str(obj)

def generate_report(params):

    key, env_name, env_is_monitored, env_orig_env_dir, env_relative_wd, env_is_coverapi, env_is_ut_api, vcm_file, workspace, jenkins_workspace, VCD = params
    
    report_name = ""
    
    if env_is_monitored:
        build_dir = os.path.join(os.path.dirname(vcm_file), env_orig_env_dir)
        # print("Monitored: ", build_dir, env_name)
    else:
        build_dir = workspace + '/' + env_relative_wd
        # print("Migrated : ", build_dir, env_name)

    if len(key.split("/")) != 3:
        comp, ts, group, env_name = key.split("/")
    else:
        comp, ts, env_name = key.split("/")

    report_name = jenkins_workspace + "/management/" + comp + "_" + ts + "_" + env_name + ".html"

    try:
        cmd = ""

        if env_is_coverapi:
            cmd = VCD + "/clicast -e " + env_name + " COVER REPORT AGGREGATE " + report_name
            process = subprocess.Popen(cmd.split(), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=build_dir)
            stdout, stderr = process.communicate()
            result = process.returncode

        elif env_is_ut_api:
            cmd = VCD + "/clicast -e " + env_name + " REPORT CUSTOM FULL " + report_name
            process = subprocess.Popen(cmd.split(), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=build_dir)
            stdout, stderr = process.communicate()
            result = process.returncode

        else:
            return key, "Error: Cannot find the environment " + build_dir + "/" + env_name + ".vcp/.vce): " + cmd

        return key, stdout.strip()
    except Exception as e:
        import traceback
        return key, "Error: " + trackback.format_exc()

    return key, "Success: " + report_name + ": " + result

class RunFullReportsParallel(object):

    def __init__(self):
        parser = argparse.ArgumentParser()
        parser.add_argument('ManageProject', help='VectorCAST Project Name')
        parser.add_argument('-j','--jobs', help='Number of concurrent jobs (default = maximun license count or processor)', default="max")
        parser.add_argument('--ci', help='Use Continuous Integration Licenses', action="store_true", default = False)

        args = parser.parse_args()

        self.mpName = args.ManageProject

        self.api = VCProjectApi(self.mpName)
        self.results = self.api.project.repository.get_full_status([])
        
        try:
            self.jenkins_workspace = os.environ['WORKSPACE'].replace("\\","/") + "/"
        except:
            self.jenkins_workspace = os.getcwd().replace("\\","/") + "/"

        if args.jobs == "max":
            try:
                max_cpus = os.cpu_count()  # Python 3.4+
            except AttributeError:
                max_cpus = multiprocessing.cpu_count()  # Python 2.7 fallback
            max_licenses = self.getLicenseCount()
            max_envs = self.getEnvCount()

            # print([max_cpus, max_licenses, max_envs])

            self.max_concurrent = min(x for x in [max_cpus,max_licenses, max_envs] if x > 0)

            # print("Using licensing max = ", self.max_concurrent)
        else:
            self.max_concurrent = int(args.jobs)


        self.envDict = {}

        self.info = []

        self.VCD = os.environ['VECTORCAST_DIR']

        for env in self.api.Environment.all():
            if env.is_active:
                self.envDict[env.level._full_path] = env

        if args.ci:
            os.environ['VCAST_USE_CI_LICENSES'] = "1"

    def getEnvCount(self):
        max_envs = len(self.api.Environment.all())
        return max_envs

    def getLicenseCount(self):

        if os.environ.get("VECTOR_LICENSE_FILE") is not None:
            if sys.platform.startswith('win32'):
                cmd =  r'%VECTORCAST_DIR%\flexlm\lmutil lmstat -a -c %VECTOR_LICENSE_FILE% | findstr VECTORCAST_MANAGE:'
            else:
                cmd =  r'$VECTORCAST_DIR/flexlm/lmutil lmstat -a -c $VECTOR_LICENSE_FILE  | grep VECTORCAST_MANAGE:'

            #result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
            
            process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout, stderr = process.communicate()
            
            largest_available = 0
            
            if isinstance(stdout, bytes):
                stdout = stdout.decode('utf-8')
            
            for line in stdout.splitlines():
                if len(line) > 0:
                    total = line.split("Total of ")[1].split(" ")[0]
                    used  = line.split("Total of ")[2].split(" ")[0]
                    available = int(total) - int(used)
                    if available > largest_available:
                        largest_available = available

            #print("Largest Available: ", largest_available)

            return largest_available

        else:
            import xml.etree.ElementTree as ET

            cmd =  r'"C:\Program Files (x86)\Vector License Client\Vector.LicenseClient.exe" -list -network'
            # result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
            process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            stdout, stderr = process.communicate()

            xml_data = stdout

            # Parse XML
            root = ET.fromstring(xml_data)

            # Find DeviceSerialNumber
            FreeLicenses = root.find(".//FreeLicenses").text

            #print("FreeLicenses:", FreeLicenses)

            return FreeLicenses

        return 1

    def run(self):
        
        pool = multiprocessing.Pool(processes=self.max_concurrent)
        
        # Run all tasks

        variables = [(key, 
            self.envDict[key].name, 
            self.envDict[key].definition.is_monitored, 
            self.envDict[key].definition.original_environment_directory,  
            self.envDict[key].relative_working_directory,
            isinstance(self.envDict[key].api,CoverApi),  
            isinstance(self.envDict[key].api,UnitTestApi),
            self.api.vcm_file, 
            self.api.project.workspace, 
            self.jenkins_workspace, 
            self.VCD) 
            for key in self.envDict.keys()]

        results = pool.map(generate_report, variables)  
        
        pool.close()
        pool.join()

        for key, result in results:
            try:
                print("Completed: Full report for " + key)
            except Exception as e:
                import traceback
                print("Exception in", key, ":", traceback.format_exc())

        print("All reports completed!")

if __name__ == '__main__':

    runner = RunFullReportsParallel()

    runner.run()
