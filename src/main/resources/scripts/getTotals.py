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
import argparse
import sys
import re
from io import open
import get_encoding

parser = argparse.ArgumentParser()
parser.add_argument('fullReportName')
parser.add_argument('--api',   help='Unused', type=int)

args = parser.parse_args()

f = open(args.fullReportName,"r", encoding=get_encoding.get_file_encoding(args.fullReportName))
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

