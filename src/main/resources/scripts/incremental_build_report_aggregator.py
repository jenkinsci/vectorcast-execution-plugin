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
import os
import sys

# This script takes Manage Incremental Rebuild Reports and combines them
#     into one comprehensive report.
# 
# adding path
jenkinsScriptHome = os.getenv("WORKSPACE") + os.sep + "vc_scripts"
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)
python_path_updates += os.sep + "vpython-addons"
sys.path.append(python_path_updates)

from bs4 import BeautifulSoup

import re
def parse_text_files():
    header = """
--------------------------------------------------------------------------------
Manage Incremental Rebuild Report
--------------------------------------------------------------------------------



--------------------------------------------------------------------------------
Environments Affected
--------------------------------------------------------------------------------
  -------------------------------------------------------------------------------
  Environment           Rebuild Status              Unaffecte Affected  Total Tes
                                                    d Tests   Tests     ts
  -------------------------------------------------------------------------------
"""

    report_file_list = []
    full_file_list = os.listdir(".")
    for file in full_file_list:
        if "_rebuild.txt" in file[-37:]:
            print file
            report_file_list.append(file)


    rebuild_count = 0
    rebuild_total = 0
    preserved_count = 0
    executed_count = 0
    total_count = 0

    outStr = ""

    for file in report_file_list:
        print "processing file: " + file
        sepCount = 0
        f = open(file,"r")
        lines = f.readlines()
        f.close()
        for line in lines:
            if re.search ("^  Totals",line):
                totals = line.replace("(","").replace(")","").split()
                rebuild_count += int(totals[2])
                rebuild_total += int(totals[4])
                preserved_count += int(totals[5])
                executed_count += int(totals[6])
                total_count += int(totals[7])
            if "--------" in line:
                sepCount += 1
            elif sepCount == 6:
                outStr += line

    try:
        percentage = rebuild_count * 100 / rebuild_total
    except:
        percentage = 0

    totalStr = "\n  -------------------------------------------------------------------------------"
    template = "\nTotals                  %3d%% (%4d / %4d)          %9d %9d %9d"
    totalStr += template%(percentage,rebuild_count,rebuild_total,preserved_count,executed_count,total_count)

    f = open("CombinedReport.txt","w")
    f.write(header + outStr + totalStr)
    f.close()
	
def parse_html_files():

    report_file_list = []
    full_file_list = os.listdir(".")
    for file in full_file_list:
        if "_rebuild.html" in file[-38:]:
            report_file_list.append(file)

    main_soup = BeautifulSoup(open(report_file_list[0]),features="lxml")
    preserved_count = 0
    executed_count = 0
    total_count = 0
    main_row_list = main_soup.table.table.tr.find_next_siblings()
    main_count_list = main_row_list[-1].td.find_next_siblings()
    preserved_count = preserved_count + int(main_count_list[1].get_text())
    executed_count = executed_count + int(main_count_list[2].get_text())
    total_count = total_count + int(main_count_list[3].get_text())
    build_success, build_total = [int(s.strip()) for s in main_count_list[0].get_text().strip().split('(')[-1][:-1].split('/')]
    
    insert_idx = 2
    for file in report_file_list[1:]:
        soup = BeautifulSoup(open(file),features="lxml")
        row_list = soup.table.table.tr.find_next_siblings()
        count_list = row_list[-1].td.find_next_siblings()
        for item in row_list[:-1]:
            main_soup.table.table.insert(insert_idx,item)
            insert_idx = insert_idx + 1
        preserved_count = preserved_count + int(count_list[1].get_text())
        executed_count = executed_count + int(count_list[2].get_text())
        total_count = total_count + int(count_list[3].get_text())
        build_totals = [int(s.strip()) for s in count_list[0].get_text().strip().split('(')[-1][:-1].split('/')]
        build_success = build_success + build_totals[0]
        build_total = build_total + build_totals[1]

    try:
        percentage = build_success * 100 / build_total
    except:
        percentage = 0
    main_soup.table.table.tr.find_next_siblings()[-1].td.find_next_siblings()[0].string.replace_with(str(percentage) + "% (" + str(build_success) + " / " + str(build_total) + ")")
    main_soup.table.table.tr.find_next_siblings()[-1].td.find_next_siblings()[1].string.replace_with(str(preserved_count))
    main_soup.table.table.tr.find_next_siblings()[-1].td.find_next_siblings()[2].string.replace_with(str(executed_count))
    main_soup.table.table.tr.find_next_siblings()[-1].td.find_next_siblings()[3].string.replace_with(str(total_count))

    f = open("CombinedReport.html","w")
    f.write(main_soup.prettify(formatter="html"))
    f.close()

if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument('--rptfmt')
    parser.add_argument('--api',   help='Unused', type=int)

    args = parser.parse_args()

    if args.rptfmt and "TEXT" in args.rptfmt:
        parse_text_files()
    else:
        parse_html_files()

