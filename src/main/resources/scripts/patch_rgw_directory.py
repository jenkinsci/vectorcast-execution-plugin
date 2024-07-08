import subprocess, os
import argparse
from managewait import ManageWait

def getReqRepo(VC_Manage_Project, testing=False):
    VC_waitLoops = 1
    VC_waitTime = 30

    command_line= f"--project \"{VC_Manage_Project}\" --list-configuration\""
    manageWait = ManageWait(False, command_line, VC_waitTime, VC_waitLoops)
    output = manageWait.exec_manage(True)

    lines = output.split("\n")
    
    reqRepoDir = None
    if testing:
        reqRepoDir = "d:/dev/PointOfSales_v2/CurrentRelease/vcast-workarea/vc_manage/reqrepo"
    else:
        for line in lines:
            if "VCAST_REPOSITORY" in line:
                reqRepoDir = line.split("VCAST_REPOSITORY VALUE:")[1]
                break
    
    if reqRepoDir is None:
        raise("Requirements Repository Directory not set")

    reqRepoDir = reqRepoDir.replace("\\","/").strip()
    
    print(reqRepoDir)
    
    return reqRepoDir

def updateReqRepo(VC_Manage_Project, VC_Workspace, testing):
    
    VC_Workspace = VC_Workspace.replace("\\","/")
    
    reqRepoDir = getReqRepo(VC_Manage_Project, testing)
        
    projDir = VC_Manage_Project.replace("\\","/").rsplit("/",1)[0]
    
    if projDir in reqRepoDir:
        
        basePath = reqRepoDir.split(projDir,1)[0]
        newPath = os.path.join(VC_Workspace, reqRepoDir.replace(basePath,"")).replace("\\","/")      
        
        if not os.path.exists(newPath): 
            raise Exception(f'Patch ReqRepo Path {newPath} not found')

        command_line = f"--project \"{VC_Manage_Project}\" --config VCAST_REPOSITORY={newPath}\""
        manageWait = ManageWait(False, command_line, 30, 1)
        manageWait.exec_manage(True)

        print(f"RGW directory patched from:\n   {reqRepoDir}\n   {newPath}")
    else:
        print(f"RGW directory not patched:\n   {reqRepoDir}\n   {projDir}")


## main
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('VcProject', help='VectorCAST Project Name')
    parser.add_argument('-v', '--verbose', default=False, help='Enable verbose output', action="store_true")
    parser.add_argument('-t', '--testing', default=False, help='Enable testing code', action="store_true")
    args = parser.parse_args()
    
    updateReqRepo(args.VcProject, os.getenv('WORKSPACE'), args.testing)