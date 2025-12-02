# tee_print.py
from __future__ import print_function
import sys
from vcast_utils import getVectorCASTEncoding

class TeePrint(object):
    def __init__(self, filename="command.log", verbose=False):
        self.filename = filename
        self.verbose = verbose
        self.encFmt = getVectorCASTEncoding()    
        self.logfile = open(filename, "ab")     


    def __enter__(self):
        return self

    def __exit__(self, exct_type, exec_value, traceback):
        try:
            self.logfile.close()
        except Exception:
            pass

    def teePrint(self, msg):
        # --- Normalize message to text (unicode in Py2, str in Py3)
        if not isinstance(msg, (str, bytes)):
            msg = str(msg)

        print(msg)

        # --- File ---
        try:
            self.logfile.write((msg + "\n").encode(self.encFmt, "replace"))
            self.logfile.flush()
        except Exception:
            # Last-chance fallback
            try:
                self.logfile.write((str(msg) + "\n").encode("utf-8", "replace"))
                self.logfile.flush()
            except Exception:
                print("Error writing to logfile: " + self.filename)   
                
