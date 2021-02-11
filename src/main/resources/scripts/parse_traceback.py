#parse_traceback.py
from pprint import pprint
import tee_print


def printTraceback(excDataAPI, compiler, testsuite, env, build_dir, error_str, tb, print_exc, teePrint):
    if excDataAPI:
        teePrint.teePrint ("   *ERROR: Error accessing DataAPI for " + compiler + "/" + testsuite + "/" + env + " in directory " + build_dir + ".  Check console for environment build/execution errors")
    else:
        teePrint.teePrint (error_str + ".  Check console for environment build/execution errors")
        
    if print_exc:
        teePrint.teePrint (tb)

        
def parse(tb, print_exc = False, compiler = "Compiler" , testsuite = "TestSuite" , env = "Environment" , build_dir = "Directory", error_str = "   *ERROR: Undefined Error" ):
    
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
            print_exc,
            teePrint)


if __name__ == '__main__':

    with tee_print.TeePrint() as teePrint:
        teePrint.teePrint ("Hello from main")
