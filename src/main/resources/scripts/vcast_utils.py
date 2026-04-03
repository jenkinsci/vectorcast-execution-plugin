#
# The MIT License
#
# Copyright 2026 Vector Informatik, GmbH.
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
    """Return 'filename::function#line' of the caller. Compatible with Python 2.7-3.9."""
    try:
        stack = inspect.stack()
        # [2] = caller of the function that called this
        frameinfo = stack[2]

        # Handle both 2.x and 3.x attribute names
        filename = getattr(frameinfo, 'filename', frameinfo[1])
        funcname = getattr(frameinfo, 'function', frameinfo[3])
        lineno = getattr(frameinfo, 'lineno', frameinfo[2])
    except Exception:
        filename = '<unknown>'
        funcname = '<unknown>'
        lineno = -1
    finally:
        # Clean up to prevent reference cycles (important for Py2)
        del stack

    return "%s::%s#%s" % (os.path.basename(filename), funcname, lineno)

def checkProjectResults(vcproj):
    
    anyLocalResults = False
    anyImportedResults = False

    try:
        results = vcproj.project.repository.get_full_status([])
        all_envs = []
        for env in vcproj.Environment.all():
            if env.is_active:
                all_envs.append(env.level._full_path)

        for result in results:
            if result in all_envs:
                if results[result]['local'] != {}:
                    anyLocalResults = True

                if results[result]['imported'] != {}:
                    anyImportedResults = True
    except Exception as e:
        print(e)
        
    return anyLocalResults, anyImportedResults
    
def checkVectorCASTVersion(minimumVersion, quiet = False):
    
    encFmt = getVectorCASTEncoding()
    tool_version = os.path.join(os.environ['VECTORCAST_DIR'], "DATA", "tool_version.txt")
    with open(tool_version,"rb") as fd:
        ver = fd.read().decode(encFmt,"replace")
    
    try:
        verNo = int(ver.split(" ",1)[0])
    except:
        verNo = int(ver.split(".",1)[0])

    if verNo >= minimumVersion:
        if not quiet:
            print("Running with VC Version: " + ver);
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
    
def getVectorCASTEncoding():

    import locale
    
    # get the VC langaguge and encoding
    enc = locale.getpreferredencoding(False)
        
    return enc;
    
def printVectorLogo():
    print( "                                                                                  ####                             ")
    print( "                                                                                  ########                         ")
    print( "                                                                                  ###########                      ")
    print( "          @@@    @@@   @@@@@@@@    @@@@@@@ @@@@@@@@@@  @@@@@@@@    @@@@@@@@          ###########                   ")
    print( "           @@@@ @@@    @@@        @@@         @@@     @@@@  @@@@   @@@   @@@            ###########                ")
    print( "            @@@@@@     @@@@@@    @@@          @@@    @@@     @@@   @@@@@@@@@               ##########              ")
    print( "             @@@@      @@@        @@@         @@@     @@@@  @@@@   @@@@@@@@             ###########                ")
    print( "              @@       @@@@@@@@    @@@@@@@    @@@      @@@@@@@@    @@@  @@@@         ###########                   ")
    print( "                                                                                  ###########                      ")
    print( "                                                                                  ########                         ")
    print( "                                                                                  ####                             ")

if __name__ == '__main__':

    checkVectorCASTVersion(21)