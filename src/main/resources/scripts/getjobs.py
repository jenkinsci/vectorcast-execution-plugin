import subprocess
import os
import re
import sys

manageCMD=os.environ['VECTORCAST_DIR'] + "/manage"


def printEnvironmentInfo(ManageProjectName):
    p = subprocess.Popen(manageCMD + " --project " + ManageProjectName + " --full-status",shell=True,stdout=subprocess.PIPE)
    out, err = p.communicate()
    
    job_list = []
    level_list = []

    list = out.split(os.linesep)

    for str in list:
        if re.match("^   [^\s]",str) is not None:
            compiler = str.split()[0]
        elif re.match("^    [^\s]",str) is not None:
            testsuite = str.split()[0]
        elif re.match("^      [^\s]",str) is not None and not str.startswith("      Disabled Environment"):
                
            env_name = str.split()[0]
                
            print ("%s %s %s" % (compiler , testsuite , env_name))

if __name__ == "__main__":
    printEnvironmentInfo(sys.argv[1])
