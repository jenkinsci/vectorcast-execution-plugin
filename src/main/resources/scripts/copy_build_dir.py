import os
import sys
import subprocess
import tarfile

global build_dir

def addFile(tf, file):
    global build_dir
    fullpath = build_dir + os.path.sep + file
    if os.path.isfile(fullpath):
        tf.add(fullpath)

ManageProjectName = sys.argv[1]
Level = sys.argv[2]
BaseName = sys.argv[3]
Env = sys.argv[4]

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
        addFile(tf, "cover.db")
        addFile(tf, "master.db")
        addFile(tf, "testcase.db")
        addFile(tf, "COMMONDB.VCD")
        addFile(tf, "UNITDATA.VCD")
        addFile(tf, "UNITDYNA.VCD")
        addFile(tf, "manage.xml")
    finally:
        tf.close()


