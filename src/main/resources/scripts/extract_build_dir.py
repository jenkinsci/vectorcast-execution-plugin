import os
import tarfile

for file in os.listdir("."):
    if file.endswith("_build.tar"):
        print "* Extracting " + file
        tf = tarfile.open(file, "r")
        tf.extractall()
