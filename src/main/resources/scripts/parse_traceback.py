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

#parse_traceback.py
from pprint import pprint
import tee_print


def printTraceback(excDataAPI, compiler, testsuite, env, build_dir, error_str, tb, teePrint):
    if excDataAPI:
        teePrint.teePrint ("   *ERROR: Error accessing DataAPI for " + compiler + "/" + testsuite + "/" + env + " in directory " + build_dir + ". Check console for environment build/execution errors")
    else:
        teePrint.teePrint (error_str + ". Check console for environment build/execution errors")
        
    teePrint.teePrint (tb)

def parse(tb, print_exc = False, compiler = "Compiler" , testsuite = "TestSuite" , env = "Environment" , build_dir = "Directory", error_str = "   *ERROR: Jenkins integration error" ):
    
    lines = tb.split("\n")
    excDataAPI = False
    
    for line in lines:
        if "vector/apps/DataAPI" in line:
            excDataAPI = True

    with tee_print.TeePrint() as teePrint:
        printTraceback(
            excDataAPI,
            compiler,
            testsuite,
            env,
            build_dir,
            error_str,
            tb,
            teePrint)

if __name__ == '__main__':

    import traceback
    try:
        raise UserWarning("This is a test warning")
    except:
        parse(traceback.format_exc(), False, "Compiler" , "testsuite",  "env", "build_dir")
