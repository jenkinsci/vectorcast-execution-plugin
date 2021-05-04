#parse_console.py
from __future__ import print_function
from pprint import pprint
import sys
import hashlib
from datetime import datetime

compoundTestIndex = 0
initTestIndex = 1
simpleTestIndex = 2

class ParseConsoleForCBT(object):
    def __init__(self, verbose = False):
        self.environmentDict = {}
        self.verbose = verbose

    def checkForSave(self, compoundTests, initTests, simpleTestcases):
        if len(compoundTests) > 0 or len(initTests) > 0 or  len(simpleTestcases) > 0:
            #print "saved", compoundTests, len(compoundTests) , initTests, len(initTests), simpleTestcases, len(simpleTestcases)
            return True
        else:
            return False
        
    def parse(self, console_log):

        runningCompound = False
        runningInits = False
        fileName = ""
        hashCode = ""
        started = False
        start_dto = datetime.now()
        end_dto = datetime.now()
        line_dto = datetime.now()
        now = datetime.now()
        tc_name = ""
        currTestNdx = 0

        for line in console_log:
            try:
                lineTime, line = line.split(" ",1)
                line_dto = datetime.strptime(lineTime,"%H:%M:%S.%f")
            except:
                pass 

            line = line.strip()
                        
            if line.startswith("Processing options file"):

                build_dir = "/".join(line.replace("\\","/").split(" ")[-1].strip().split("/")[:-1]).upper()
                build_dir = "/".join(build_dir.split("/")[-2:])
                
                # Unicode-objects must be encoded before hashing in Python 3
                if sys.version_info[0] >= 3:
                    build_dir = build_dir.encode('utf-8')

                hashCode = hashlib.md5(build_dir).hexdigest()
                
                if self.verbose:
                    print ("Parse Dir: " + str(build_dir) + " Hash: " + hashCode)
                
                started = True
                if hashCode not in  self.environmentDict.keys():
                    self.environmentDict[hashCode] = [{},{},{}]
                continue
                

            if started: 
                # system test
                if line.startswith("Adding result file"):
                    tc_name = line.split(" as ",1)[1]
                    self.environmentDict[hashCode][simpleTestIndex][tc_name] = [now, now]
                    continue

                if line.startswith("Creating report"):
                    started = False
                    continue
                
                elif "Completed Incremental Execution processing" in line:
                    started = False
                    continue
                
                elif "Completed Batch Execution processing" in line:
                    started = False
                    continue

                if "Preparing to run all" in line: 
                    line = line.replace("Preparing to run", "Running")

                elif "Preparing to run " in line: 
                    line = line.replace("Preparing to run", "Running:")


                if "Running all" in line or "Preparing to run all" in line:
                    fileName = ""
                    func = ""
                    try:
                        fileName, func = line.split()[2].split(".",1)
                    except:
                        fileName = line.split()[2].split(".",1)[0]

                    # Running all <<COMPOUND>> test cases
                    if "Running all <<COMPOUND>> test cases" in line:
                        runningCompound = True
                        runningInits = False

                    # Running all manager.<<INIT>> test cases
                    elif "<<INIT>>" == func:
                        runningCompound = False
                        runningInits = True

                    #Running all MANAGER."-" test cases
                    elif "\"" in func:
                        runningCompound = False
                        runningInits = False

                    #Running all manager.(cl)Manager::PlaceOrder test cases
                    else:
                        runningCompound = False
                        runningInits = False
                    
                if "Test Execution Complete" in line or "Error: " in line:
                    end_tdo = line_dto
                    duration_tdo = end_tdo - start_dto
                    try:
                        self.environmentDict[hashCode][currTestNdx][tc_name][1] = end_tdo
                    except KeyError:
						# key error would be for the "Error: " when the test case hadn't started
                        pass                        

                if "Running: " in line:
                    start_dto = line_dto
                    if runningCompound:
                        tc_name = "<<COMPOUND>>/<<COMPOUND>>/" + line.split("Running: ")[-1]
                        currTestNdx = compoundTestIndex
                        self.environmentDict[hashCode][currTestNdx][tc_name] = [start_dto, None]
                    elif runningInits:
                        tc_name = "<<INIT>>/<<INIT>>/" + line.split("Running: ")[-1]
                        currTestNdx = initTestIndex
                        self.environmentDict[hashCode][currTestNdx][tc_name] = [start_dto, None]
                    else:
                        tc = line.split("Running: ")[-1]     
                        tc_name = fileName + "/" + func + "/" + tc
                        currTestNdx = simpleTestIndex                        
                        self.environmentDict[hashCode][currTestNdx][tc_name] = [start_dto, None]
                elif "There are no slots in compound test" in line:
                    ##     There are no slots in compound test <<COMPOUND>>.FailNo_Slots.
                    tc_name = line.split(" ")[-1][:-1]
                    currTestNdx = compoundTestIndex                        
                    self.environmentDict[hashCode][currTestNdx][tc_name] = [start_dto, None]

                elif "All slots in compound test" in line:
                    ##     There are no slots in compound test <<COMPOUND>>.FailNo_Slots.
                    tc_name = line.split(" ")[5]
                    currTestNdx = compoundTestIndex                        
                    self.environmentDict[hashCode][currTestNdx][tc_name] = [start_dto, None]

        if self.verbose:
            pprint(self.environmentDict, width=132)
            
        return self.environmentDict           

if __name__ == '__main__':
    
    buildLogData = open(sys.argv[1],"r").readlines()
    parser = ParseConsoleForCBT(True)
    parser.parse(buildLogData)
    #pprint(parser.parse(buildLogData), width=132)
    
