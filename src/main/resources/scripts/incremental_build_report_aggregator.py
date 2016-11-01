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

def parse_files():
     
    report_file_list = []
    full_file_list = os.listdir(".")
    for file in full_file_list:
        if "manage_incremental_rebuild_report.html" in file[-38:]:
            report_file_list.append(file)

    main_soup = BeautifulSoup(open(report_file_list[0]))
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
        soup = BeautifulSoup(open(file))
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
    parser.add_argument('--api', type=int)
    args = parser.parse_args()

    if args.api != 2:
        print "**********************************************************************"
        print "* Error - unsupported API version. This script expects API version 2 *"
        print "**********************************************************************"
        sys.exit(-1)

    parse_files()
