import sys, os
from pprint import pprint
import argparse
import subprocess

from vcast_utils import checkVectorCASTVersion, dump

if not checkVectorCASTVersion(21):
    print("Full reports genreated by previous call to generate-results.py")
    sys.exit()
else:
    import concurrent.futures
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

    def runSingleAtgCommand(self, cmd):

        """Run a single shell command and return the output."""
        try:
            result = subprocess.run(atgCmd, shell=True, capture_output=True, text=True)
            return result.stdout.strip()
        except Exception as e:
            return "Error: " + e


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
            max_cpus = os.cpu_count()
            max_licenses = self.getLicenseCount()
            max_envs = self.getEnvCount()

            #print([max_cpus, max_licenses, max_envs])

            self.max_concurrent = min([max_cpus,max_licenses, max_envs])

            #print("Using licensing max = ", self.max_concurrent)
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
            cmd =  r'%VECTORCAST_DIR%\flexlm\lmutil lmstat -a -c %VECTOR_LICENSE_FILE% | findstr VECTORCAST_MANAGE:'

            result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
            largest_available = 0

            for line in result.stdout.split("\n"):
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
            result = subprocess.run(cmd, shell=True, capture_output=True, text=True)

            xml_data = result.stdout

            # Parse XML
            root = ET.fromstring(xml_data)

            # Find DeviceSerialNumber
            FreeLicenses = root.find(".//FreeLicenses").text

            #print("FreeLicenses:", FreeLicenses)

            return FreeLicenses

        return 1

    def generate_report(self, key):

        env = self.envDict[key]

        report_name = ""
        
        if env.definition.is_monitored:
            build_dir = os.path.join(os.path.dirname(self.api.vcm_file), env.definition.original_environment_directory)
            print("Monitored: ", build_dir, env.name)
        else:
            build_dir = self.api.project.workspace + '/' + env.relative_working_directory
            print("Migrated : ", build_dir, env.name)

        if len(key.split("/")) != 3:
            comp, ts, group, env_name = key.split("/")
        else:
            comp, ts, env_name = key.split("/")

        report_name = self.jenkins_workspace + "/management/" + comp + "_" + ts + "_" + env_name + ".html"

        try:
            cmd = ""

            if isinstance(env.api,CoverApi):
                cmd = self.VCD + "/clicast -e " + env.name + " COVER REPORT AGGREGATE " + report_name
                print("Report command: "+ cmd + " in " + build_dir)
                result = subprocess.run(cmd.split(), capture_output=True, text=True, cwd=build_dir)

            elif isinstance(env.api,UnitTestApi):
                cmd = self.VCD + "/clicast -e " + env.name + " REPORT CUSTOM FULL " + report_name
                print("Report command: "+ cmd + " in " + build_dir)
                result = subprocess.run(cmd.split(), capture_output=True, text=True, cwd=build_dir)

            else:
                return "Error: Cannot find the environment " + build_dir + "/" + env.name + ".vcp/.vce): " + cmd

            return result.stdout.strip()
        except Exception as e:
            return "Error: " + e

        return "Success: " + report_name + ": " + result


    def run(self):

        with concurrent.futures.ThreadPoolExecutor(max_workers=self.max_concurrent) as executor:
            futures = [executor.submit(self.generate_report, key) for key in self.envDict.keys()]
            concurrent.futures.wait(futures)


        for future in concurrent.futures.as_completed(futures):
            try:
                result = future.result()  # This raises the exception if one occurred
                #print(result)
            except Exception as e:
                report_name = futures[future]
                print("Exception in " + report_name + ": " + e)

        print("All reports completed!")

if __name__ == '__main__':

    runner = RunFullReportsParallel()

    runner.run()
