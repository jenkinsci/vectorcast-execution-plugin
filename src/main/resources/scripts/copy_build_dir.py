#
# The MIT License
#
# Copyright 2016 Vector Software, East Greenwich, Rhode Island USA
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

global build_dir
global workspace
global nocase
global ws_length

def addFile(tf, file):
    global build_dir
    for f in os.listdir(build_dir):
        if fnmatch.fnmatch(f, file):
            tf.add(os.path.join(build_dir, f))

def addConvertCoverFile(tf, file):
    global workspace
    global nocase
    global ws_length
    fullpath = build_dir + os.path.sep + file
    if os.path.isfile(fullpath):
        conn = sqlite3.connect(fullpath)
        if conn:
            cur = conn.cursor()
            conn.execute("UPDATE source_files "
                         "SET display_path = "
                         "REPLACE(SUBSTR(display_path, " + ws_length + "), \"\\\", \"/\") "
                         "WHERE path LIKE '" + workspace + "%' " + nocase)
            conn.commit()
            conn.execute("UPDATE source_files "
                         "SET path = "
                         "display_path "
                         "WHERE path LIKE '" + workspace + "%' " + nocase)
            conn.commit()
            conn.execute("UPDATE instrumented_files "
                         "SET LIS_file = "
                         "SUBSTR(LIS_file, " + ws_length + ") "
                         "WHERE LIS_file LIKE '" + workspace + "%' " + nocase)
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
        addFile(tf, "*.LIS")
    finally:
        tf.close()
