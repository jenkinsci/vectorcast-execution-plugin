#
# The MIT License
#
# Copyright 2025 Vector Informatik, GmbH.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.
#
import subprocess, os
import argparse
from managewait import ManageWait

def getReqRepo(VC_Manage_Project):
    VC_waitLoops = 1
    VC_waitTime = 30

    command_line= "--project \"" + VC_Manage_Project + "\" --list-configuration\""
    manageWait = ManageWait(False, command_line, VC_waitTime, VC_waitLoops)
    output = manageWait.exec_manage(True)

    lines = output.split("\n")
    
    reqRepoDir = None
    for line in lines:
        if "VCAST_REPOSITORY" in line:
            reqRepoDir = line.split("VCAST_REPOSITORY VALUE:")[1]
            break
    
    if reqRepoDir is None:
        raise EnvironmentError("Requirements Repository Directory not set")

    reqRepoDir = reqRepoDir.replace("\\","/").strip()
    
    print(reqRepoDir)
    
    return reqRepoDir

def updateReqRepo(VC_Manage_Project, VC_Workspace, top_level):
    
    VC_Workspace = VC_Workspace.replace("\\","/")
    
    reqRepoDir = getReqRepo(VC_Manage_Project,)
        
    projDir = VC_Manage_Project.replace("\\","/").rsplit("/",1)[0]
    
    if projDir in reqRepoDir:
        
        basePath = reqRepoDir.split(projDir,1)[0]
        newPath = os.path.join(VC_Workspace, reqRepoDir.replace(basePath,"")).replace("\\","/")      
        
        if not os.path.exists(newPath): 
            raise FileNotFoundError("Patch ReqRepo Path " + newPath + " not found")

        command_line= "--project \"" + VC_Manage_Project + "\" --config VCAST_REPOSITORY=\"" + newPath + "\""
        manageWait = ManageWait(False, command_line, 30, 1)
        manageWait.exec_manage(True)

        if top_level:
            command_line= "--project \"" + VC_Manage_Project + "\" --clicast-args option VCAST_REPOSITORY\"" + newPath + "\""
            manageWait = ManageWait(False, command_line, 30, 1)
            manageWait.exec_manage(True)

        print("RGW directory patched from:\n   " + reqRepoDir + "\n   " + projDir)
    else:
        print("RGW directory not patched:\n   " + reqRepoDir + "\n   " + projDir)


## main
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('VcProject', help='VectorCAST Project Name')
    parser.add_argument('-v', '--verbose', default=False, help='Enable verbose output', action="store_true")
    parser.add_argument('-t', '--top_level', default=False, help='Apply VCAST_REPOSITORY at the top level', action="store_true")
    args = parser.parse_args()
    
    updateReqRepo(args.VcProject, os.getenv('WORKSPACE'), args.top_level)