#parallel_build.py

from __future__ import unicode_literals
from __future__ import print_function

import sys, os, subprocess, argparse, glob, shutil
from pprint import pprint
import pdb, time
from datetime import timedelta
from io import open

from vector.apps.DataAPI.vcproject_api import VCProjectApi 
from threading import Thread, Lock, Semaphore
try:
        from Queue import Queue, Empty
except ImportError:
        from queue import Queue, Empty  # python 3.x
 
VCD = os.environ['VECTORCAST_DIR']

class ParallelBuild(object):
    def __init__(self):
        self.manageProject = None
        self.lock = Lock()

        parser = argparse.ArgumentParser()        
        
        parser.add_argument('--project', '-p',     help='Manager Project Name')
        parser.add_argument('--dryrun',      help='Dry Run without build/execute', action="store_true")
        parser.add_argument('--jobs', '-j',     help='Number of concurrent jobs', default="1")
        parser.add_argument('--verbose',     help='Dry Run without build/execute', action="store_true")
        parser.add_argument('--ci', help='Use CI Licenses', action="store_true", default = False)
        args = parser.parse_args()

        if args.ci:
            self.useCI = " --ci "
        else:
            self.useCI = ""

        try:
            self.manageProject = os.environ['VCV_ENVIRONMENT_FILE']
        except:
            self.manageProject = args.project
                
        self.dryrun = args.dryrun
            
        if self.manageProject is None:
            print ("\n** Use either --project [Manage Project Name] or enviroment variable VCV_ENVIRONMENT_FILE to specify the manage project name")
            sys.exit()
        
        if not os.path.isfile(self.manageProject) and not os.path.isfile(self.manageProject + ".vcm"):
            raise IOError(self.manageProject + ' does not exist')
            return
                        
        if args.verbose:
            self.verbose = True
        else:
            self.verbose = False
            
        self.mpName = self.manageProject.replace(".vcm","")   
        self.reportName = os.path.basename(self.manageProject).replace(".vcm","")   
        self.buildSemaphore = Semaphore(int(args.jobs))
        
        print ("Disabling range check globally")
        
        self.api = VCProjectApi(self.manageProject)
        self.oldRangeCheck = self.api.project.options["enums"]["RANGE_CHECK"][0]
        self.api.close()

        buildCmd = VCD + "/manage --project " + self.manageProject + self.useCI + " --config=RANGE_CHECK=NONE"
        self.runManageCmd(buildCmd)

        self.api = VCProjectApi(self.manageProject)
 
    def __enter__(self):
        return self

    def __exit__(self, exct_type, exce_value, traceback):
        self.api.close()
        
        print ("Clearing disable of range check globally")
        buildCmd = VCD + "/manage --project " + self.manageProject + self.useCI +" --config=RANGE_CHECK="+self.oldRangeCheck
        self.runManageCmd(buildCmd)
        build_log_data = ""
        
        for file in glob.glob("build*.log"):
            build_log_data += " ".join(open(file,"r").readlines())
            os.remove(file)
            
        try:       
            open(self.reportName + "_build.log","w", encoding="utf-8").write(unicode(build_log_data))
        except:
            open(self.reportName + "_build.log","w").write(build_log_data)
            
        print(build_log_data)

    def th_Print (self, str):
        self.lock.acquire()
        print (str)
        self.lock.release()

    def runManageCmd(self, cmd, env = None):
        if self.verbose:
            self.th_Print (cmd)
            
        if self.dryrun:
            return
            
        if env:
            logName = "build_" + self.reportName + "_" + env.compiler.name + "_" + env.testsuite.name + "_" + env.name + ".log"
            build_log = open(logName,"w")
            process = subprocess.Popen(cmd, shell=True, stdout=build_log, stderr=build_log)   
            process.wait()
            build_log.close()
        else:
            process = subprocess.Popen(cmd, shell=True)   
            process.wait()
    
    def build_env(self,env):
        if not self.verbose:
            self.th_Print ("Building: " + env.compiler.name + "/" + env.testsuite.name + "/" + env.name)
        buildCmd = VCD + "/manage --project " + self.manageProject + self.useCI + " --build --level " + env.compiler.name + "/" + env.testsuite.name + " --environment " + env.name
        self.runManageCmd(buildCmd,env)
            
        self.buildSemaphore.release() 
            
    def doit(self):
        buildingList = []
        
        for env in self.api.Environment.all():
            if env.system_tests:
                print("Building System Test: " + env.compiler.name + "/" + env.testsuite.name + "/" + env.name)
                buildCmd = VCD + "/manage --project " + self.manageProject + self.useCI + " --build --level " + env.compiler.name + "/" + env.testsuite.name + " --environment " + env.name
                self.runManageCmd(buildCmd,env)
                continue
                
            self.buildSemaphore.acquire()
            t = Thread(target=self.build_env,args=[env])
            t.daemon = True # thread dies with the program
            t.start()
            buildingList.append(t)
            
        checkThreads = True    
        while checkThreads:
            checkThreads = False
            for t in buildingList:
                if t.is_alive():
                    time.sleep(1)
                    checkThreads = True
                    break
        
if __name__ == '__main__':

    with ParallelBuild() as parallel_build:
        parallel_build.doit()

