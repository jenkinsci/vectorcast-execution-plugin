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
                if os.path.exists(os.path.join(build_dir,env_name+".vcp")):
                    return "ST: "

    return "UT: "
    
    
def printEnvironmentInfo(ManageProjectName, printData = True):

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

    with tee_print.TeePrint() as teePrint:
        printOutput(somethingPrinted, ManageProjectName, output, teePrint)

    return output
    
if __name__ == "__main__":
    printEnvironmentInfo(sys.argv[1])
