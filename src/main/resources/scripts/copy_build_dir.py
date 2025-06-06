#
# The MIT License
#
# Copyright 2024 Vector Informatik, GmbH.
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
import os
import fnmatch
import sys
import subprocess
import tarfile
import sqlite3
import shutil
import argparse

try:
    ## This tests to see if 2018 is present.
    from vector.apps.ReportBuilder.custom_report import CustomReport
    try:
        from vector.apps.DataAPI.unit_test_api import UnitTestApi
    except:
        from vector.apps.DataAPI.api import Api as UnitTestApi
except:
    pass
    
try:
    from vector.apps.DataAPI.vcproject_api import VCProjectApi
except:
    pass

def make_relative(path, workspace, vCastProjectWorkspace, ManageProjectName):

    path = path.replace("\\","/")
    

    if path.endswith('.LIS') or '000000' in path:
        return path

    if not os.path.isabs(path):
        return path
        
    # if the paths match
    if path.lower().startswith(workspace.lower()):
        if "@" in path:
            path = path[len(workspace):]
            path = path.split("/",1)[1]
        else:
            path = path[len(workspace)+1:]
        
    # if the paths match except for first character (d:\ changed to j:\)
    elif path.lower()[1:].startswith(workspace.lower()[1:]):
        path = path[len(workspace)+1:]
        
    elif "workspace" in path:
        # if paths are different, find the workspace in the jenkins path
        workspaceIndex = path.lower().find("workspace")
        
        path = path[workspaceIndex:].split("/",2)[2]
        
    elif vCastProjectWorkspace is not None and vCastProjectWorkspace.lower() in path.lower():
        path = path.replace(vCastProjectWorkspace,"")
        path = ManageProjectName[:-4].replace("\\", "/") + path
        
    else:
        print("  Warning: Unable to convert source file: " + path + " to relative path based on WORKSPACE: " + workspace)
    
    return path

    
def updateDatabase(conn, nocase, workspace, updateWhat, updateFrom, vCastProjectWorkspace, ManageProjectName):
    sql = "SELECT id, %s FROM %s" % (updateWhat, updateFrom)
    files = conn.execute(sql)
    for id_, path in files:
        relative = make_relative(path,workspace, vCastProjectWorkspace, ManageProjectName)    
        sql = "UPDATE %s SET %s = '%s' WHERE id=%s COLLATE NOCASE" % (updateFrom, updateWhat, relative, id_)
        conn.execute(sql)
        
def addFile(tf, file, build_dir, backOneDir = False):
    
    local_build_dir = build_dir 
    
    if backOneDir:
        local_build_dir = os.sep.join(build_dir.split(os.sep)[:-1])
        
    try:
        for f in os.listdir(local_build_dir):
            if fnmatch.fnmatch(f, file):
                tf.add(os.path.join(local_build_dir, f))
    except:
        pass

def addDirectory(tf, build_dir, dir):

    if build_dir is None:
        rootDir = dir
    else:
        rootDir = os.path.join(build_dir,dir).replace("\\","/")

    for dirName, subdirList, fileList in os.walk(rootDir):
        if len(fileList) == 0:
            tf.add(dirName)
        else:
            for fname in fileList:
                tf.add(os.path.join(dirName, fname))

def addConvertCoverFile(tf, file, workspace, build_dir, nocase, vCastProjectWorkspace, ManageProjectName, noTar):
    
    fullpath = build_dir + os.path.sep + file
    bakpath = fullpath + '.bk'

    print("Updating cover.db: ", fullpath)
    
    if os.path.isfile(fullpath):
        conn = sqlite3.connect(fullpath)
        if conn:
            shutil.copyfile(fullpath, bakpath)

            # update the database paths to be relative from workspace
            try:
                updateDatabase(conn, nocase, workspace, "LIS_file", "instrumented_files", vCastProjectWorkspace, ManageProjectName)
            except:
                updateDatabase(conn, nocase, workspace, "path", "lis_files" , vCastProjectWorkspace, ManageProjectName)
            updateDatabase(conn, nocase, workspace, "display_path", "source_files", vCastProjectWorkspace, ManageProjectName)
            updateDatabase(conn, nocase, workspace, "path", "source_files", vCastProjectWorkspace, ManageProjectName)
            
            conn.commit()
            conn.close()
            addFile(tf, file, build_dir)
            if not noTar:
                os.remove(fullpath)
                shutil.move(bakpath, fullpath)

def addConvertMasterFile(tf, file, workspace, build_dir, nocase, vCastProjectWorkspace, ManageProjectName, noTar):
    print("Updating master.db")
    fullpath = build_dir + os.path.sep + file
    bakpath = fullpath + '.bk'
    if os.path.isfile(fullpath):
        conn = sqlite3.connect(fullpath)
        if conn:
            shutil.copyfile(fullpath, bakpath)
            updateDatabase(conn, nocase, workspace, "path", "sourcefiles", vCastProjectWorkspace, ManageProjectName)
            conn.commit()
            conn.close()
            addFile(tf, file, build_dir)
            if not noTar:
                os.remove(fullpath)
                shutil.move(bakpath, fullpath)

def addConvertFiles(tf, workspace, build_dir, nocase, vCastProjectWorkspace, ManageProjectName, noTar):
    addConvertCoverFile (tf, "cover.db",  workspace, build_dir, nocase, vCastProjectWorkspace, ManageProjectName, noTar)
    addConvertMasterFile(tf, "master.db", workspace, build_dir, nocase, vCastProjectWorkspace, ManageProjectName, noTar)


def run(ManageProjectName, Level, BaseName, Env, workspace, vCastProjectWorkspace, noTar):

    if sys.platform.startswith('win32'):
        workspace = workspace.replace("\\", "/")
        vCastProjectWorkspace = vCastProjectWorkspace.replace("\\", "/")
        nocase = "COLLATE NOCASE"
    else:
        nocase = ""

    manageCMD = os.path.join(os.environ.get('VECTORCAST_DIR'), "manage")
    cmd = manageCMD + " --project " + ManageProjectName + " --build-directory-name --level " + Level + " -e " + Env
    p = subprocess.Popen(cmd,
                         shell=True,
                         stdout=subprocess.PIPE,
                         universal_newlines=True)
    out, err = p.communicate()
    list = out.splitlines()
    build_dir = ''

    for item in list:
        if "Build Directory:" in item:
            length = len(item.split()[0]) + 1 + len(item.split()[1]) + 1
            build_dir = os.path.relpath(item[length:])

    try:
        rgwDir = getReqRepo(ManageProjectName).replace("\\","/").replace(workspace+"/","")
        rgwExportDir = os.path.join(rgwDir, "requirements_gateway/export_data").replace("\\","/")
    except:
        rgwDir=None

    if build_dir != "":
        build_dir = build_dir + os.path.sep + Env
        if noTar:
            tf = tarfile.open("delete_me_" + BaseName + "_build.tar", mode='w')
        else:
            tf = tarfile.open(BaseName + "_build.tar", mode='w')
        try:
            addConvertFiles(tf, workspace, build_dir, nocase, vCastProjectWorkspace, ManageProjectName, noTar)
            addFile(tf, "testcase.db", build_dir)
            addFile(tf, "COMMONDB.VCD", build_dir)
            addFile(tf, "UNITDATA.VCD", build_dir)
            addFile(tf, "UNITDYNA.VCD", build_dir)
            addFile(tf, "manage.xml", build_dir)
            addFile(tf, "testcase_data.xml", build_dir)
            addFile(tf, "*.LIS", build_dir)
            addFile(tf, "ENVIRO.AUX*", build_dir)
            addFile(tf, "system_test_results.xml", build_dir)
            addDirectory(tf, build_dir, "TESTCASES")
            addDirectory(tf, build_dir, "results")
            addFile(tf, "CCAST_.CFG", build_dir, backOneDir=True)
            addFile(tf, Env + ".vce", build_dir, backOneDir=True)
            addFile(tf, Env + ".vcp", build_dir, backOneDir=True)
            addFile(tf, Env + ".env", build_dir, backOneDir=True)
            addFile(tf, Env + ".tst", build_dir, backOneDir=True)
            addFile(tf, Env + "_cba.cvr", build_dir, backOneDir=True)
            addFile(tf, "vcast_manage.cfg", build_dir, backOneDir=True)

            if rgwDir is not None:
                addDirectory(tf, None, rgwExportDir)

        finally:
            tf.close()
            
        if noTar:
            os.remove("delete_me_" + BaseName + "_build.tar")


def getVcastProjectWorkspace(args):

    vc_api = VCProjectApi(args.ManageProject)
    vCastProjectWorkspace = vc_api.project.workspace
    vc_api.close()
    
    return vCastProjectWorkspace

if __name__ == '__main__':
    
    
    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject',       help='Manager Project Name')
    parser.add_argument('-l', '--level',       help='Environment Name if only doing single environment.  Should be in the form of level/env', default="NotProvided/NotProvided")
    parser.add_argument('-b', '--basename',    help='Enable verbose output',    default="")
    parser.add_argument('-e', '--environment', help='Enable verbose output', default="")
    parser.add_argument('--notar',             help='Don\'t Product a tar file', default=False, action="store_true")
    parser.add_argument('-v', '--verbose',     help='Enable verbose output',     action="store_true")
    
    args = parser.parse_args()
    
    ManageProjectName = args.ManageProject
    Level = args.level
    BaseName = args.basename
    Env = args.environment
    vCastProjectWorkspace = getVcastProjectWorkspace(args)
    noTar = args.notar
    
    workspace = os.getenv("WORKSPACE")

    if workspace is None:
        workspace = os.getcwd()

    if workspace.endswith("/") or workspace.endswith("\\"):
        workspace = workspace[:-1]

    os.environ['VCAST_MANAGE_PROJECT_DIRECTORY'] = os.path.abspath(ManageProjectName).rsplit(".",1)[0]
    
    run(ManageProjectName, Level, BaseName, Env, workspace, vCastProjectWorkspace, noTar)
    