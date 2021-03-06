from __future__ import print_function
import subprocess
import os
import re
import sys

manageCMD=os.environ['VECTORCAST_DIR'] + "/manage"


def printEnvironmentInfo(ManageProjectName):

    somethingPrinted = False
    
    p = subprocess.Popen(manageCMD + " --project " + ManageProjectName + " --full-status",
                         shell=True,
                         stdout=subprocess.PIPE,
                         universal_newlines=True)
    out, err = p.communicate()
    
    job_list = []
    level_list = []

    list = out.splitlines()

    for str in list:
        if re.match("^   [^\s]",str) is not None:
            compiler = str.split()[0]
        elif re.match("^    [^\s]",str) is not None:
            testsuite = str.split()[0]
        elif re.match("^      [^\s]",str) is not None and not str.startswith("      Disabled Environment"):
                
            env_name = str.split()[0]
                
            print ("%s %s %s" % (compiler , testsuite , env_name))
            
            somethingPrinted = True;
            
    if not somethingPrinted:
        print ("No environments found in " + ManageProjectName + ". Please check configuration", file=sys.stderr)
        
if __name__ == "__main__":
    printEnvironmentInfo(sys.argv[1])
