from __future__ import print_function
import subprocess
import os
import re
import sys
import tee_print
teePrint = tee_print.TeePrint()

manageCMD=os.environ['VECTORCAST_DIR'] + "/manage"


def printEnvironmentInfo(ManageProjectName, printData = True):

    somethingPrinted = False
    output = ""
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
            
            output += "%s %s %s\n" % (compiler , testsuite , env_name)
                            
            somethingPrinted = True;
            
    if not somethingPrinted:
        teePrint.teePrint ("No environments found in " + ManageProjectName + ". Please check configuration", file=sys.stderr)
    
    teePrint.teePrint(output)

    return output
    
if __name__ == "__main__":
    printEnvironmentInfo(sys.argv[1])
