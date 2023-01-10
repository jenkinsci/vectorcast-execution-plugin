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
    else:
        teePrint.teePrint(output)
    
def parseBuildDirectory(ManageProjectName, printEnvType, buildDirInfo):
    output = ""
    
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
            envEnabled = True
            build_dir = line.split(":",1)[-1].strip()
            
            cmds = [manageCMD, "--project", ManageProjectName, "--compiler", build_comp,
                    "--testsuite", build_ts, "--list"]
            
            p = subprocess.Popen(cmds, stdout=subprocess.PIPE, universal_newlines=True)
            out, err = p.communicate()
            if "DISABLED ENVIRONMENTS" in out:
                listInfo = out.split("\n")
                disableIndex = listInfo.index("DISABLED ENVIRONMENTS:")
                enabledIndex = listInfo.index("ENVIRONMENTS:")
                disabledList = listInfo[disableIndex+1:enabledIndex-1]
                for env in disabledList:
                    if build_env.strip() == env.strip():
                        envEnabled = False
            if envEnabled:
                if printEnvType:
                    output += checkForSystemTest(build_dir, build_env)

                output += "%s %s %s\n" % (build_comp , build_ts , build_env)  

    return output

def checkForSystemTest(build_dir, env_name):

    # Leave for legacy, but not sure this is needed
    if (build_dir):
       
        # If the build directory is there, check that
        if os.path.exists(build_dir):
            if os.path.exists(os.path.join(build_dir,env_name+".vcp")) or os.path.exists(os.path.join(build_dir,env_name+".enc")):
                return "ST: "        
                
        # if there's no build directory, check the project's enviornment directory for a migrated ST project
        else:
            env_dir = build_dir.rsplit("/",2)[0] + "/environment/" + env_name
            if os.path.exists(os.path.join(env_dir,env_name+".enc")):
                return "ST: "        

    return "UT: "
        
def printEnvInfoNoDataAPI(ManageProjectName, printData = True, printEnvType = False):

    somethingPrinted = False
    output = ""

    p = subprocess.Popen(manageCMD + " --project " + ManageProjectName + " --build-directory-name",
                         shell=True,
                         stdout=subprocess.PIPE,
                         universal_newlines=True)
                         
    out, err = p.communicate()
    buildDirInfo = out.splitlines()
    
    output = parseBuildDirectory(ManageProjectName, printEnvType, buildDirInfo)

    if printData:
        with tee_print.TeePrint() as teePrint:
            printOutput(len(output) != 0, ManageProjectName, output, teePrint)

    return output
 
def printEnvironmentInfo(ManageProjectName, printData = True, printEnvType = False, legacy = False):

    return printEnvInfoNoDataAPI(ManageProjectName, printData, printEnvType)
        
        
if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject', help='Manager Project Name')
    parser.add_argument('-t', '--type',   help='Displays the type of environemnt (Unit test or System test)', action="store_true", default = False)    
    parser.add_argument('-l', '--legacy',   help='Use the legacy report parsing method - testing only)', action="store_true", default = False)    
    
    args = parser.parse_args()

    printEnvironmentInfo(args.ManageProject, True, args.type, args.legacy)
