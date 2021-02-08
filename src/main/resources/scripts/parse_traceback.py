#parse_traceback.py
from pprint import pprint
import tee_print
teePrint = tee_print.TeePrint()


def parse(tb, print_exc = False, compiler = "Compiler" , testsuite = "TestSuite" , env = "Environment" , build_dir = "Directory", error_str = "   *ERROR: Undefined Error" ):
    
    lines = tb.split("\n")
    excDataAPI = False
    
    for line in lines:
        if "vector/apps/DataAPI" in line:
            excDataAPI = True
    if excDataAPI:
        teePrint.teePrint ("   *ERROR: Error accessing DataAPI for " + compiler + "/" + testsuite + "/" + env + " in directory " + build_dir + ".  Check console for environment build/execution errors")
    else:
        teePrint.teePrint (error_str + ".  Check console for environment build/execution errors")
        
    if print_exc:
        teePrint.teePrint (tb)
        
        
if __name__ == '__main__':

    teePrint.teePrint ("Hello from main")
