import argparse
import sys
import re

parser = argparse.ArgumentParser()
parser.add_argument('fullReportName')
parser.add_argument('--api', type=int)
args = parser.parse_args()

if args.api != 2:
    print "**********************************************************************"
    print "* Error - unsupported API version. This script expects API version 2 *"
    print "**********************************************************************"
    sys.exit(-1)

#fullReportName = sys.argv[1];
f = open(args.fullReportName,"r")
lines = f.read().split("\n")
f.close()

dataFilename = args.fullReportName.replace("full_report","overall_results")

dataFile = open(dataFilename,"w")

keyLine = ""
dataLine = ""
matchLine = re.compile("^\s*BUILD\s*BUILD.TIME\s*EXPECTED\s*TESTCASES\s*EXECUTE.TIME.*$")
for line in lines:
    if matchLine.match(line):
        keyLine = line
        dataLine = lines[lines.index(line) + 2]
        break;


keyList = keyLine.split()
dataList = dataLine.split()
totalFailed = 0
try:

    if (dataList[4] != "-"):
        dataFile.write("Expected Values: " + dataList[4] + " " + dataList[5] + "\n")
        
    else:
        dataFile.write("Expected Values: 0 0\n")

    if (dataList[5] != "-"):
        dataFile.write("Test Cases: " + dataList[6] + " " + dataList[7] + "\n")
        totalFailed = int(dataList[6].split("/")[1]) - int(dataList[6].split("/")[0])
    else:    
        dataFile.write("Test Cases: 0 0\n")
except:
        dataFile.write("Expected Values: 0 0\n")
        dataFile.write("Test Cases: 0 0\n")
        sys.exit()
    
endIndex = -1
CoverageTypes = ["Statements", "Branches", "Paths", "Pairs"]
while True:
    if keyList[endIndex] in CoverageTypes:
        dataFile.write(keyList[endIndex] + ": " + dataList [endIndex*2] + " " + dataList[endIndex*2+1] + "\n")
    else:
        break

    endIndex -= 1



dataFile.close()

f = open("unit_test_fail_count.txt","w")
f.write(str(totalFailed))
f.close()

