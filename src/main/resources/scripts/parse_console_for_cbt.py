#parse_console.py
from pprint import pprint
import sys

compoundTestIndex = 0
InitTestIndex = 1
simpleTestIndex = 2


class ParseConsoleForCBT():
    def __init__(self):
        self.environmentDict = {}

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

        for line in console_log:
            line = line.strip()
            
            if line.startswith("Processing options file"):
                if started:
                    self.environmentDict[hashCode] = [[],[],[]]
                hashCode = line.replace("\\","/").split("/")[-2].strip()
                started = True
                continue

            if started and line.startswith("Creating report"):
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
                
            if "Running: " in line:
                
                if runningCompound:
                    self.environmentDict[hashCode][compoundTestIndex].append(line.split("Running: ")[-1])

                elif runningInits:
                    self.environmentDict[hashCode][initTestIndex].append(line.split("Running: ")[-1])

                else:
                    tc = line.split("Running: ")[-1]                                            
                    self.environmentDict[hashCode][simpleTestIndex].append(fileName + "/" + func + "/" + tc)
                    
        return self.environmentDict           

if __name__ == '__main__':
    
    buildLogData = open(sys.argv[1],"r").readlines()
    parser = ParseConsoleForCBT()
    #parser.parse(buildLogData)
    pprint(parser.parse(buildLogData), width=132)
    
