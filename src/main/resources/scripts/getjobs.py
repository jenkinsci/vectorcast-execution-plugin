import subprocess
import os
import re
import sys

try:
    from vector.apps.DataApi.vcproject_api import VCProjectApi
    useManageApi = True
except:
    useManageApi = False

manageCMD=os.environ['VECTORCAST_DIR'] + "/manage"


def parseFullStatusReport(ManageProjectName):
    p = subprocess.Popen(manageCMD + " --project " + ManageProjectName + " --full-status",shell=True,stdout=subprocess.PIPE)
    out, err = p.communicate()
    
    job_list = []
    level_list = []
    info = ""
    list = out.split(os.linesep)

    for str in list:
        if re.match("^   [^\s]",str) is not None:
            compiler = str.split()[0]
        elif re.match("^    [^\s]",str) is not None:
            testsuite = str.split()[0]
        elif re.match("^      [^\s]",str) is not None and not str.startswith("      Disabled Environment"):
            env_name = str.split()[0]
            info += ("%s %s %s\n" % (compiler , testsuite , env_name))
    return info

def printEnvironmentInfo(ManageProjectName):
    if useManageApi:
        info = ""
        m_api = VCProjectApi(ManageProjectName)
        for env in m_api.Environment.all():
            if env.is_enabled:
                info += ("%s %s %s\n" % (env.compiler.name, env.testsuite.name, env.name))
    else:
        info = parseFullStatusReport(ManageProjectName)
        
    print info
    
if __name__ == "__main__":
    printEnvironmentInfo(sys.argv[1])
