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
import inspect

def __get_script_filename():
    # Get the previous frame in the call stack (i.e., the caller)
    caller_frame = inspect.stack()[2]
    
    try:
        caller_filename = caller_frame.filename
    except:
        caller_filename = caller_frame[1]  # In Python 2.7, the filename is the second item in the frame

    return os.path.basename(caller_filename)


def checkVectorCASTVersion(minimumVersion, quiet = False):
    tool_version = os.path.join(os.environ['VECTORCAST_DIR'], "DATA", "tool_version.txt")
    with open(tool_version,"r") as fd:
        ver = fd.read()
    
    try:
        verNo = int(ver.split(" ",1)[0])
    except:
        verNo = int(ver.split(".",1)[0])

    if verNo >= minimumVersion:
        if not quiet:
            print("Running with VC Version: ", ver);
        return True
        
    if not quiet:
        print("\nXXX " + __get_script_filename() + "    requires VectorCAST Version 2021 or higher.\nXXX\nXXX   Using version: " + ver)
    
    return False

def dump(obj):
    if hasattr(obj, '__dict__'): 
        return vars(obj) 
    else:
        try:
            return {attr: getattr(obj, attr, None) for attr in obj.__slots__} 
        except:
            return str(obj)

def fmt_percent(num, dom):
    pct = 0.0
    if (dom > 0.0):
        pct = 100.0 * num / dom
        pct_round = round(pct, 2)
        str_num = str(pct_round)
    else:
        str_num = "0%"
    return str_num

if __name__ == '__main__':

    checkVectorCASTVersion(21)