from __future__ import print_function
import subprocess
import os
import re
import sys
import tee_print
import glob


manageCMD=os.environ['VECTORCAST_DIR'] + "/manage"


def printOutput(somethingPrinted, ManageProjectName, output, teePrint):
    if not somethingPrinted:
        teePrint.teePrint ("No environments found in " + ManageProjectName + ". Please check configuration")
    
    teePrint.teePrint(output)
    
def getBuildDirectory(compiler , testsuite , env_name, buildDirInfo):
    for line in buildDirInfo:
        if "Compiler:" in line:
            build_comp = line.split(":",1)[-1].strip()
        elif "Testsuite ID:" in line:
            pass
        elif "TestSuite:" in line:
            build_ts = line.split(":",1)[-1].strip()
        elif "Environment:" in line:
            build_env = line.split(":",1)[-1].strip()
        elif "Build Directory:" in line:
            build_dir = line.split(":",1)[-1].strip()
            if build_comp == compiler and build_ts == testsuite and build_env == env_name:
                return build_dir
                
    return None


def checkForSystemTest(build_dir, env_name):
    
    if (build_dir):
        if os.path.exists(os.path.join(build_dir,env_name+".vcp")) or os.path.exists(os.path.join(build_dir,env_name+".enc")):
            return "ST: "        

    return "UT: "
    
def checkForEnvChanges(vcm_fname, build_dir, env_name):

    env_coverdb = os.path.join(build_dir,env_name,"cover.db")
        
    if (os.path.exists(env_coverdb)):
        #print("cover.db file exists")
        env_coverdb_ts = os.path.getmtime(env_coverdb)
        vcm_file_ts = os.path.getmtime(vcm_fname)
        if (vcm_file_ts > env_coverdb_ts):
            print ("Changes to .vcm file.  Rebuild all")
            #print(str(env_coverdb_ts), str(vcm_file_ts))
            return " FR"
            
        env_files_latest_ts = 0
        
        vcm_path = vcm_fname.replace("\\","/")
        if "/" in vcm_path:
            vcm_path = vcm_path.rsplit("/",1)[0]
        else:
            vcm_path = ""
            
        vcm_basename = os.path.basename(vcm_fname).replace(".vcm","")
        
        env_data_path = os.path.join(vcm_path,vcm_basename, "environment", env_name,"*.*")
        for file in glob.glob(env_data_path):
            if os.path.getmtime(file) > env_coverdb_ts:
                print(file,"time greater than", env_coverdb)
                print("force rebuild of enviornment")
                return "FR"
            
        return " NA"
    
    return " FR"
    
    
def printEnvInfoDataAPI(api, printData = True, printEnvType = False):
    print ("Using data api!!!")
    somethingPrinted = False
    output = ""
    
    for env in api.Environment.all():
        somethingPrinted = True
        
        if (printEnvType):
            if env.system_tests:
                output += "ST: "
            else:
                output += "UT: "
                
        output += "%s %s %s\n" % (env.compiler.name , env.testsuite.name , env.name)

    if printData:
        with tee_print.TeePrint() as teePrint:
            printOutput(somethingPrinted, api.vcm_file, output, teePrint)

    return output

def printEnvInfoNoDataAPI(ManageProjectName, printData = True, printEnvType = False):

    somethingPrinted = False
    output = ""
    p = subprocess.Popen(manageCMD + " --project " + ManageProjectName + " --full-status",
                         shell=True,
                         stdout=subprocess.PIPE,
                         universal_newlines=True)
    out, err = p.communicate()
    enabledList = out.splitlines()

    p = subprocess.Popen(manageCMD + " --project " + ManageProjectName + " --build-directory-name",
                         shell=True,
                         stdout=subprocess.PIPE,
                         universal_newlines=True)
                         
    out, err = p.communicate()
    buildDirInfo = out.splitlines()
    
    for str in enabledList:
        if re.match("^   [^\s]",str) is not None:
            compiler = str.split()[0]
        elif re.match("^    [^\s]",str) is not None:
            testsuite = str.split()[0]
        elif re.match("^      [^\s]",str) is not None and not str.startswith("      Disabled Environment"):
                
            env_name = str.split()[0]
            build_dir = getBuildDirectory(compiler , testsuite , env_name, buildDirInfo)
            
            #force_rebuild = checkForEnvChanges(ManageProjectName, build_dir, env_name)

            if printEnvType:
                output += checkForSystemTest(build_dir, env_name)

            output += "%s %s %s\n" % (compiler , testsuite , env_name)            
                            
            somethingPrinted = True;

    if printData:
        with tee_print.TeePrint() as teePrint:
            printOutput(somethingPrinted, ManageProjectName, output, teePrint)

    return output
 
def printEnvironmentInfo(ManageProjectName, printData = True, printEnvType = False, legacy = False):
    # try:
        # if (legacy): raise KeyError
            
        # from vector.apps.DataAPI.vcproject_api import VCProjectApi
        # api = VCProjectApi(ManageProjectName)
        # return printEnvInfoDataAPI(api, printData, printEnvType)
    
    # except:
    
    if True:
        #import parse_traceback
        #import traceback
        #print (parse_traceback.parse(traceback.format_exc()))
        return printEnvInfoNoDataAPI(ManageProjectName, printData, printEnvType)
        
        
if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject', help='Manager Project Name')
    parser.add_argument('-t', '--type',   help='Displays the type of environemnt (Unit test or System test)', action="store_true", default = False)    
    parser.add_argument('-l', '--legacy',   help='Use the legacy report parsing method - testing only)', action="store_true", default = False)    
    
    args = parser.parse_args()

    printEnvironmentInfo(args.ManageProject, True, args.type, args.legacy)
