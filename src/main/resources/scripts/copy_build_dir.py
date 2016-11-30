import os
import sys
import subprocess
import tarfile
import sqlite3

global build_dir
global workspace
global nocase
global ws_length

def addFile(tf, file):
    global build_dir
    fullpath = build_dir + os.path.sep + file
    if os.path.isfile(fullpath):
        tf.add(fullpath)

def addConvertCoverFile(tf, file):
    global workspace
    global nocase
    global ws_length
    fullpath = build_dir + os.path.sep + file
    if os.path.isfile(fullpath):
        conn = sqlite3.connect(fullpath)
        if conn:
            print "RMK: " + ("UPDATE source_files "
                         "SET path = "
                         "SUBSTR(path, " + ws_length + "), "
                         "display_path = "
                         "SUBSTR(display_path, " + ws_length + ") "
                         "WHERE path LIKE '" + workspace + "%' " + nocase)
            conn.execute("UPDATE source_files "
                         "SET path = "
                         "SUBSTR(path, " + ws_length + "), "
                         "display_path = "
                         "SUBSTR(display_path, " + ws_length + ") "
                         "WHERE path LIKE '" + workspace + "%' " + nocase)
            conn.commit()
            conn.close()
            addFile(tf, file)

def addConvertMasterFile(tf, file):
    global workspace
    global nocase
    global ws_length
    fullpath = build_dir + os.path.sep + file
    if os.path.isfile(fullpath):
        conn = sqlite3.connect(fullpath)
        if conn:
            conn.execute("UPDATE sourcefiles "
                         "SET path = "
                         "SUBSTR(path, " + ws_length + ") "
                         "WHERE path LIKE '" + workspace + "%' " + nocase)
            conn.commit()
            conn.close()
            addFile(tf, file)

ManageProjectName = sys.argv[1]
Level = sys.argv[2]
BaseName = sys.argv[3]
Env = sys.argv[4]
workspace = os.getenv("WORKSPACE")
if sys.platform.startswith('win32'):
    workspace = workspace.replace("\\", "/")
    nocase = "COLLATE NOCASE"
else:
    nocase = ""
ws_length = str(len(workspace)+2)

VECTORCAST_DIR = os.getenv('VECTORCAST_DIR')
manageCMD = VECTORCAST_DIR + os.sep + "manage"

p = subprocess.Popen(manageCMD + " --project " + ManageProjectName + " --build-directory-name --level " + Level + " -e " + Env,shell=True,stdout=subprocess.PIPE)
out, err = p.communicate()
list = out.split(os.linesep)
build_dir = ''
for str in list:
    if "Build Directory:" in str:
        build_dir = os.path.relpath(str.split()[2])

if build_dir != "":
    build_dir = build_dir + os.path.sep + Env
    tf = tarfile.open(BaseName + "_build.tar", mode='w')
    try:
        addConvertCoverFile(tf, "cover.db")
        addConvertMasterFile(tf, "master.db")
        addFile(tf, "testcase.db")
        addFile(tf, "COMMONDB.VCD")
        addFile(tf, "UNITDATA.VCD")
        addFile(tf, "UNITDYNA.VCD")
        addFile(tf, "manage.xml")
    finally:
        tf.close()
