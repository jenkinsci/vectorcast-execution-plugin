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

from __future__ import print_function

import subprocess
import os
import sys
import argparse
import shutil
import re
import time
from datetime import datetime
from threading import Thread
try:
        from Queue import Queue, Empty
except ImportError:
        from queue import Queue, Empty  # python 3.x

try:
    from safe_open import open
except:
    pass

from vcast_utils import getVectorCASTEncoding

class ManageWait(object):
    def __init__(self, verbose, command_line, wait_time, wait_loops, mpName = "", useCI = ""):
        self.wait_time = wait_time
        self.wait_loops = wait_loops
        self.verbose = verbose
        self.command_line = command_line
        self.mpName = mpName
        self.useCI = useCI
        self.stop_requested = False

        # get the VC langaguge and encoding
        self.encFmt = getVectorCASTEncoding()

    def enqueueOutput(self, io_target, queue, logfile):
        py2 = sys.version_info[0] < 3

        while not self.stop_requested:
            line = io_target.readline()
            if not line:
                continue
            line = line.rstrip()
            if line == '':
                continue

            # --- Normalize line to Unicode text ---
            if isinstance(line, bytes if not py2 else str):
                try:
                    line = line.decode(self.encFmt, 'replace')
                except Exception:
                    line = line.decode('utf-8', 'replace')

            output = u"{:s}  {:s}\n".format(datetime.now().strftime("%H:%M:%S.%f"), line)

            if not self.silent:
                # logfile opened in binary mode ? always write bytes
                try:
                    logfile.write(output.encode(self.encFmt, 'replace'))
                except Exception:
                    logfile.write(output.encode('utf-8', 'replace'))

            print(line)
            
            queue.put(line)

    def startOutputThread(self, io_target, logfile):
        self.q = Queue()
        self.io_t = Thread(target=self.enqueueOutput, args=(io_target, self.q, logfile))
        self.io_t.daemon = True # thread dies with the program
        self.io_t.start()

    def exec_manage_command(self, cmd_line, silent = False):
        self.command_line = "--project \"" + self.mpName + "\" " + self.useCI + " " + cmd_line
        if self.verbose:
            print (self.command_line)
        return self.exec_manage(silent)

    def exec_manage(self, silent=False):
        with open("command.log", "ab") as logfile:   # binary append
            return self.__exec_manage(silent, logfile)

    def __exec_manage(self, silent, logfile):
        self.silent = silent
        
        callStr = os.environ.get('VECTORCAST_DIR') + os.sep + "manage " + self.command_line
        ret_out = ''

        if self.verbose:
            logfile.write(("\nVerbose: %s\n" % callStr).encode(self.encFmt, "replace"))

        # capture the output of the manage call
        loop_count = 0
        while True:
            loop_count += 1
            
            # Build a base argument set for Popen
            popen_args = {
                "stdout": subprocess.PIPE,
                "stderr": subprocess.STDOUT,
                "shell": True,
                "universal_newlines": True,
            }

            # Add encoding only on Python 3+
            if sys.version_info[0] >= 3:
                popen_args["encoding"] = self.encFmt
                popen_args["text"] = True  # optional, same as universal_newlines=True

            p = subprocess.Popen(callStr, **popen_args)
            
            self.stop_requested = False

            self.startOutputThread(p.stdout, logfile)

            license_outage = False
            edited_license_outage_msg = ""
            actual_license_outage_msg = ""

            while p.poll() is None or not self.q.empty():
                try:
                    while not self.q.empty():
                        out_mgt = self.q.get(False)

                        if len(out_mgt) > 0:

                            LICENSE_ERRORS = [
                                "Unable to obtain license",
                                "Licensed number of users already reached",
                                "License server system does not support this feature"]

                            if any(error in out_mgt for error in LICENSE_ERRORS):
                                license_outage = True
                                # Change FLEXlm Error to FLEXlm Err.. to avoid Groovy script from
                                # marking retry attempts as overall job failure
                                actual_license_outage_msg = out_mgt
                                out_mgt = out_mgt.replace("FLEXlm Error", "FLEXlm Err..")
                                edited_license_outage_msg = out_mgt

                            ret_out +=  out_mgt + "\n"
                    time.sleep(0.5)

                except Empty:
                    pass

            self.stop_requested = True
            self.io_t.join()
            p.wait()

            
            # manage finished. Was there a license outage?
            if license_outage:
                if loop_count < self.wait_loops:
                    print(("Edited license outage message : " + edited_license_outage_msg ))
                    msg = "Warning: Failed to obtain a license, sleeping %ds and then re-trying, attempt %d of %d" % (self.wait_time, loop_count+1, self.wait_loops)
                    print (msg)
                    time.sleep(self.wait_time)
                else:
                    # send the unedited error to stdout for the post build groovy to mark a failure
                    print("Original license outage message : " + actual_license_outage_msg )
                    msg = "ERROR: Failed to obtain a license after %d attempts, terminating" % self.wait_loops
                    print (msg)
                    break
            else :
                break #leave outer while loop

        return ret_out # checked in generate-results.py

## main
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('-v', '--verbose',   help='Enable verbose output', action="store_true")
    parser.add_argument('--command_line',   help='Command line to pass to Manage', required=True)
    parser.add_argument('--wait_time',   help='Time (in seconds) to wait between execution attempts', type=int, default=30)
    parser.add_argument('--wait_loops',   help='Number of times to retry execution', type=int, default=1)

    args = parser.parse_args()

    manageWait = ManageWait(args.verbose, args.command_line, args.wait_time, args.wait_loops)
    manageWait.exec_manage()
