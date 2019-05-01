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
import subprocess
import os
import sys
import argparse
import shutil
import re
import time


class ManageWait():
    def __init__(self, verbose, command_line, wait_time, wait_loops):
        self.wait_time = wait_time
        self.wait_loops = wait_loops
        self.verbose = verbose
        self.command_line = command_line

    def exec_manage(self):
        # Versions of VectorCAST prior to 2019 relied on the environment variable VECTORCAST_DIR.
        # We will use that variable as a fall back if the VectorCAST executables aren't on the system path.
        exe_env = os.environ.copy()
        if 'VECTORCAST_DIR' in os.environ:
            exe_env['PATH'] = os.pathsep.join([os.environ.get('PATH', ''), exe_env['VECTORCAST_DIR']])

        callStr = "manage " + self.command_line
        output = ''
        if self.verbose:
            output += "\nVerbose: %s" % callStr

        # capture the output of the manage call
        loop_count = 0
        while 1:
            loop_count += 1
            p = subprocess.Popen(callStr,stdout=subprocess.PIPE,stderr=subprocess.STDOUT, shell=True, env=exe_env)
            (out_mgt, out_mgt2) = p.communicate()

            output += "\n" + out_mgt.rstrip()
            # No point checking error code - errors are not propagated from
            # clicast to manage
            if "Licensed number of users already reached" in out_mgt:
                if loop_count < self.wait_loops:
                    # Change FLEXlm Error to FLEXlm Err.. to avoid Groovy script from
                    # marking retry attempts as overall job failure
                    out_mgt = out_mgt.replace("FLEXlm Error", "FLEXlm Err..")
                    print out_mgt
                    msg = "Warning: Failed to obtain a license, sleeping %ds and then re-trying, attempt %d of %d" % (self.wait_time, loop_count+1, self.wait_loops)
                    print msg
                    output += "\n" + msg
                    time.sleep(self.wait_time)
                else:
                    print out_mgt
                    msg = "ERROR: Failed to obtain a license after %d attempts, terminating" % self.wait_loops
                    print msg
                    output += "\n" + msg
                    sys.exit(-1)
            else:
                print out_mgt
                break;

        return output

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
