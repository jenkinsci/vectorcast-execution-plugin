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

from __future__ import division
from __future__ import print_function

import argparse
import os
import sys
import shutil
from io import open

import get_encoding

# This script takes Manage Incremental Rebuild Reports and combines them
#     into one comprehensive report.
# 
# adding path
jenkinsScriptHome = os.getenv("WORKSPACE") + os.sep + "vc_scripts"
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)

# needed because vc18 vpython does not have bs4 package
if sys.version_info[0] < 3:
    python_path_updates += os.sep + 'vpython-addons'
    sys.path.append(python_path_updates)

from bs4 import BeautifulSoup

import re
def parse_text_files(mpName):
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
        if "_rebuild.txt" in file:
            print(file)
            report_file_list.append(file)

    rebuild_count = 0
    rebuild_total = 0
    preserved_count = 0
    executed_count = 0
    total_count = 0

    outStr = ""

    for file in report_file_list:
        print("processing file: " + file)
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
        percentage = rebuild_count * 100 //  rebuild_total
    except:
        percentage = 0

    totalStr = "\n  -------------------------------------------------------------------------------"
    template = "\nTotals                  %3d%% (%4d / %4d)          %9d %9d %9d"
    totalStr += template%(percentage,rebuild_count,rebuild_total,preserved_count,executed_count,total_count)

    f = open(mpName + "_rebuild.txt","w")
    f.write(header + outStr + totalStr)
    f.close()

    # moving rebuild reports down in to a sub directory
    if not os.path.exists("rebuild_reports"):
        os.mkdir("rebuild_reports")
    for file in report_file_list:
        if os.path.exists(file):
          shutil.move(file, "rebuild_reports/"+file)
        
def parse_html_files(mpName):

    if os.path.exists(mpName + "_rebuild.html"):
        os.remove(mpName + "_rebuild.html")
        
    report_file_list = []
    full_file_list = os.listdir(".")
    for file in full_file_list:
        if "_rebuild.html" in file:
            report_file_list.append(file)

    if len(report_file_list) == 0:
        print("No incrementatal rebuild reports found in the workspace...skipping")
        return
        
    try:
        main_soup = BeautifulSoup(open(report_file_list[0], encoding=get_encoding.get_file_encoding(report_file_list[0])),features="lxml")
    except:
        main_soup = BeautifulSoup(open(report_file_list[0], encoding=get_encoding.get_file_encoding(report_file_list[0])))

    preserved_count = 0
    executed_count = 0
    total_count = 0
        
    if main_soup.find(id="report-title"):
        main_manage_api_report = True
        # New Manage reports have div with id=report-title
        # Want second table (skip config data section)
        main_row_list = main_soup.find_all('table')[1].tr.find_next_siblings()
        main_count_list = main_row_list[-1].th.find_next_siblings()
    else:
        main_manage_api_report = False
        main_row_list = main_soup.table.table.tr.find_next_siblings()
        main_count_list = main_row_list[-1].td.find_next_siblings()

    preserved_count = preserved_count + int(main_count_list[1].get_text())
    executed_count = executed_count + int(main_count_list[2].get_text())
    total_count = total_count + int(main_count_list[3].get_text())
    if main_manage_api_report:
        build_success, build_total = [int(s.strip()) for s in main_count_list[0].get_text().strip().split('(')[0][:-1].split('/')]
    else:
        build_success, build_total = [int(s.strip()) for s in main_count_list[0].get_text().strip().split('(')[-1][:-1].split('/')]
    
    insert_idx = 2
    for file in report_file_list[1:]:
        try:
            soup = BeautifulSoup(open(file, encoding="utf-8"),features="lxml")
        except:
            soup = BeautifulSoup(open(file, encoding="utf-8"))
            
        if soup.find(id="report-title"):
            manage_api_report = True
            # New Manage reports have div with id=report-title
            # Want second table (skip config data section)
            row_list = soup.find_all('table')[1].tr.find_next_siblings()
            count_list = row_list[-1].th.find_next_siblings()
        else:
            manage_api_report = False
            row_list = soup.table.table.tr.find_next_siblings()
            count_list = row_list[-1].td.find_next_siblings()
        for item in row_list[:-1]:
            if manage_api_report:
                main_soup.find_all('table')[1].insert(insert_idx,item)
            else:
                main_soup.table.table.insert(insert_idx,item)
            insert_idx = insert_idx + 1
        preserved_count = preserved_count + int(count_list[1].get_text())
        executed_count = executed_count + int(count_list[2].get_text())
        total_count = total_count + int(count_list[3].get_text())
        if manage_api_report:
            build_totals = [int(s.strip()) for s in count_list[0].get_text().strip().split('(')[0][:-1].split('/')]
        else:
            build_totals = [int(s.strip()) for s in count_list[0].get_text().strip().split('(')[-1][:-1].split('/')]
        build_success = build_success + build_totals[0]
        build_total = build_total + build_totals[1]

    try:
        percentage = build_success * 100 // build_total
    except:
        percentage = 0
    if main_manage_api_report:
        main_row_list = main_soup.find_all('table')[1].tr.find_next_siblings()
        main_count_list = main_row_list[-1].th.find_next_siblings()
        main_count_list[0].string.replace_with(str(build_success) + " / " + str(build_total) + " (" + str(percentage) + "%)" )
    else:
        main_row_list = main_soup.table.table.tr.find_next_siblings()
        main_count_list = main_row_list[-1].td.find_next_siblings()
        main_count_list[0].string.replace_with(str(percentage) + "% (" + str(build_success) + " / " + str(build_total) + ")")

    main_count_list[1].string.replace_with(str(preserved_count))
    main_count_list[2].string.replace_with(str(executed_count))
    main_count_list[3].string.replace_with(str(total_count))

    # remove the table of content because the >v icon is messing stuff up and its pointless in this report    
    for div in main_soup.find_all("div", {'class':'contents-block'}): 
        div.decompose()
        
    #<div class="report-body no-toc" id="main-scroller">
    div = main_soup.find("div", {'class':'report-body'})  
    if div:
        div['class']="report-body no-toc"
    
    f = open(mpName + "_rebuild.html","w", encoding="utf-8")
    f.write(main_soup.prettify(formatter="html"))
    f.close()

    import fixup_reports
    main_soup = fixup_reports.fixup_2020_soup(main_soup)
    
    # moving rebuild reports down in to a sub directory
    f = open("combined_incr_rebuild.tmp","w", encoding="utf-8")
    f.write(main_soup.prettify(formatter="html"))
    f.close()
    
    # moving rebuild reports down in to a sub directory
    if not os.path.exists("rebuild_reports"):
        os.mkdir("rebuild_reports")
    for file in report_file_list:
        if mpName + "_rebuild.html" in file:
            continue
        if os.path.exists(file):
          shutil.move(file, "rebuild_reports/"+file)

    # copy the CSS and PNG files for manage rebuild reports...if available
    import glob        
    for file in glob.glob("*.css"):
        shutil.copy(file, "rebuild_reports/"+file)
    for file in glob.glob("*.png"):
        shutil.copy(file, "rebuild_reports/"+file)

if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser.add_argument('ManageProject')
    parser.add_argument('--rptfmt')
    parser.add_argument('--api',   help='Unused', type=int)

    args = parser.parse_args()

    if args.rptfmt and "TEXT" in args.rptfmt:
        parse_text_files(args.ManageProject)
    else:
        parse_html_files(args.ManageProject)

