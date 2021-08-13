from __future__ import print_function
import subprocess
import os
import re
import sys
import tee_print


manageCMD=os.environ['VECTORCAST_DIR'] + "/manage"


def printOutput(somethingPrinted, ManageProjectName, output, teePrint):
    if not somethingPrinted:
        teePrint.teePrint ("No environments found in " + ManageProjectName + ". Please check configuration")
    
    teePrint.teePrint(output)

def checkForSystemTest(compiler , testsuite , env_name, buildDirInfo):

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
                if os.path.exists(os.path.join(build_dir,env_name+".vcp")) or os.path.exists(os.path.join(build_dir,env_name+".enc")):
                    return "ST: "

    return "UT: "
    
    
def printEnvInfoDataAPI(api, printData = True):
    print ("Using data api")
    somethingPrinted = False
    output = ""
    
    for env in api.Environment.all():
        somethingPrinted = True

        if env.system_tests:
            st_ut = "ST: "
        else:
            st_ut = "UT: "
        
        output += "%s %s %s %s\n" % (st_ut, env.compiler.name , env.testsuite.name , env.name)
        
    if printData:
        with tee_print.TeePrint() as teePrint:
            printOutput(somethingPrinted, api.vcm_file, output, teePrint)

    return output

def printEnvInfoNoDataAPI(ManageProjectName, printData = True):

    print ("Old Method")
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
            
            st_ut = checkForSystemTest(compiler , testsuite , env_name, buildDirInfo)

            output += "%s %s %s %s\n" % (st_ut, compiler , testsuite , env_name)
                            
            somethingPrinted = True;

    if printData:
        with tee_print.TeePrint() as teePrint:
            printOutput(somethingPrinted, ManageProjectName, output, teePrint)

    return output
 
def printEnvironmentInfo(ManageProjectName, printData = True):
    try:
        from vector.apps.DataAPI.vcproject_api import VCProjectApi
        api = VCProjectApi(ManageProjectName)
        return printEnvInfoDataAPI(api, printData)
    
    except:
        import parse_traceback
        import traceback
        print (parse_traceback.parse(traceback.format_exc()))
        return printEnvInfoNoDataAPI(ManageProjectName, printData)
        
        
if __name__ == "__main__":
    ManageProjectName = sys.argv[1]
    
    printEnvironmentInfo(ManageProjectName)
