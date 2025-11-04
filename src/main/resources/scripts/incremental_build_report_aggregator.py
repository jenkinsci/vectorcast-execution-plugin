#
# The MIT License
#
# Copyright 2024 Vector Informatik, GmbH.
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
import shutil
import locale
import os, shutil, glob, logging
import sys, os

# adding path
if sys.version_info[0] < 3:
    python_path_updates = os.path.join(os.path.dirname(os.path.abspath(__file__)),'vpython-addons')
    sys.path.append(python_path_updates)

from bs4 import BeautifulSoup

try:
    from safe_open import open
except:
    pass
from vcast_utils import getVectorCASTEncoding

import re
def parse_text_files(mpName, verbose = False):
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
    encFmt = getVectorCASTEncoding()

    report_file_list = []
    full_file_list = os.listdir(".")
    for file in full_file_list:
        if "_rebuild.txt" in file:
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
        with open(file,"rb") as fd:
            lines = [line.decode(encFmt, "replace") for line in fd.readlines()]

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

    with open(mpName + "_rebuild.txt","wb") as fd:
        data = header + outStr + totalStr
        fd.write(data.encode(encFmt,"replace"))

    # moving rebuild reports down in to a sub directory
    if not os.path.exists("rebuild_reports"):
        os.mkdir("rebuild_reports")
    for file in report_file_list:
        if os.path.exists(file):
          shutil.move(file, "rebuild_reports/"+file)

def parse_html_files(mpName, verbose = False):
    """
    Parse and merge multiple *_rebuild.html reports into one combined report.
    Works on both Python 2.7 and 3.x.
    """
    
    # ---------------------------------------------------------------------
    # Logging setup: both file + console
    # ---------------------------------------------------------------------
    logging.basicConfig(
        filename='parse_html_files.log',
        filemode='w',
        level=logging.INFO,
        format='%(asctime)s [%(levelname)s] %(message)s'
    )

    def log(msg, level="info"):
        # Mirror log messages to both log file and stdout
        if verbose:
            print(msg)
        getattr(logging, level)(msg)

    encFmt = getVectorCASTEncoding()
    log("[DEBUG] Detected encoding: {}".format(encFmt))

    # ---------------------------------------------------------------------
    # Preparation
    # ---------------------------------------------------------------------
    if os.path.exists(mpName + "_rebuild.html"):
        os.remove(mpName + "_rebuild.html")

    report_file_list = [f for f in os.listdir(".") if "_rebuild.html" in f]

    if len(report_file_list) == 0:
        log("No incremental rebuild reports found in the workspace...skipping", "error")
        return False

    keepLooping = True
    main_soup = None
    preserved_count = executed_count = total_count = 0
    build_success = build_total = 0

    # ---------------------------------------------------------------------
    # Find first valid report
    # ---------------------------------------------------------------------
    while keepLooping:
        if not report_file_list:
            log("No files left in report_file_list - exiting.", "error")
            log("No valid rebuild reports", "error")
            return False

        current_file = report_file_list[0]
        log("\n[INFO] Attempting to open and parse report: {}".format(current_file))

        try:
            with open(current_file, "rb") as fd:
                raw = fd.read()

            log("[DEBUG] Detected encoding: {}".format(encFmt))

            # Parse with fallback encodings
            try:
                # First attempt: use lxml if available, else let BS pick
                try:
                    import lxml  # noqa
                    parser = "lxml"
                except ImportError:
                    parser = "html.parser"

                main_soup = BeautifulSoup(raw, features=parser)

            except Exception:
                try:
                    # Fallback to UTF-8
                    main_soup = BeautifulSoup(
                        raw.encode("utf-8", "replace"),
                        features=parser
                    )
                except Exception:
                    # Final fallback: use whatever parser is available, no feature string
                    main_soup = BeautifulSoup(
                        raw.encode(encFmt, "replace")
                    )

            if len(main_soup.find_all('table')) < 1:
                raise LookupError("No <table> elements found in {}".format(current_file))

            if main_soup.find(id="report-title"):
                main_manage_api_report = True
                log("[INFO] Detected Manage API report format.")
                main_row_list = main_soup.find_all('table')[1].tr.find_next_siblings()
                main_count_list = main_row_list[-1].th.find_next_siblings()
            else:
                main_manage_api_report = False
                log("[INFO] Detected legacy report format.")
                main_row_list = main_soup.table.table.tr.find_next_siblings()
                main_count_list = main_row_list[-1].td.find_next_siblings()

            log("[SUCCESS] Parsed report successfully: {}".format(current_file))
            keepLooping = False

        except Exception as e_outer:
            log("[EXCEPTION] Failed on file '{}': {}".format(current_file, e_outer), "error")
            if len(report_file_list) > 1:
                popped = report_file_list.pop(0)
                log("[INFO] Removed failed file: {}".format(popped))
                log("[INFO] Remaining files: {}".format(report_file_list))
                keepLooping = True
            else:
                log("[ERROR] No valid rebuild reports remain.", "error")
                return False

    # ---------------------------------------------------------------------
    # Initialize totals from the first valid report
    # ---------------------------------------------------------------------
    preserved_count += int(main_count_list[1].get_text())
    executed_count  += int(main_count_list[2].get_text())
    total_count     += int(main_count_list[3].get_text())

    if main_manage_api_report:
        build_success, build_total = [
            int(s.strip()) for s in main_count_list[0].get_text().strip().split('(')[0][:-1].split('/')
        ]
    else:
        build_success, build_total = [
            int(s.strip()) for s in main_count_list[0].get_text().strip().split('(')[-1][:-1].split('/')
        ]

    insert_idx = 2

    # ---------------------------------------------------------------------
    # Merge additional reports
    # ---------------------------------------------------------------------
    for file in report_file_list[1:]:
        log("\n[INFO] Merging additional report: {}".format(file))
        try:
            with open(file, "rb") as fd:
                raw = fd.read()
            encFmt = getVectorCASTEncoding()
            log("[DEBUG] Encoding detected: {}".format(encFmt))
        except Exception as e:
            log("[ERROR] Failed to open file '{}': {}".format(file, e))
            continue

        soup = None
        try:
            # First attempt: use lxml if available, else let BS pick
            try:
                import lxml  # noqa
                parser = "lxml"
            except ImportError:
                parser = "html.parser"

            soup = BeautifulSoup(raw, features=parser)

        except Exception:
            try:
                # Fallback to UTF-8
                soup = BeautifulSoup(
                    raw.encode("utf-8", "replace"),
                    features=parser
                )
            except Exception:
                # Final fallback: use whatever parser is available, no feature string
                soup = BeautifulSoup(
                    raw.encode(encFmt, "replace")
                )

        try:
            if soup.find(id="report-title"):
                manage_api_report = True
                log("[INFO] Manage API format detected for {}".format(file))
                row_list = soup.find_all('table')[1].tr.find_next_siblings()
                count_list = row_list[-1].th.find_next_siblings()
            else:
                manage_api_report = False
                log("[INFO] Legacy format detected for {}".format(file))
                row_list = soup.table.table.tr.find_next_siblings()
                count_list = row_list[-1].td.find_next_siblings()

            for item in row_list[:-1]:
                if manage_api_report:
                    main_soup.find_all('table')[1].insert(insert_idx, item)
                else:
                    main_soup.table.table.insert(insert_idx, item)
                insert_idx += 1

            preserved_count += int(count_list[1].get_text())
            executed_count  += int(count_list[2].get_text())
            total_count     += int(count_list[3].get_text())

            if manage_api_report:
                build_totals = [
                    int(s.strip()) for s in count_list[0].get_text().strip().split('(')[0][:-1].split('/')
                ]
            else:
                build_totals = [
                    int(s.strip()) for s in count_list[0].get_text().strip().split('(')[-1][:-1].split('/')
                ]

            build_success += build_totals[0]
            build_total   += build_totals[1]

            log("[SUCCESS] Merged report: {} (pres={}, exec={}, total={})".format(
                file, preserved_count, executed_count, total_count
            ))

        except Exception as e_inner:
            log("[EXCEPTION] Failed merging '{}': {}".format(file, e_inner), "error")
            continue

    # ---------------------------------------------------------------------
    # Final percentage and totals
    # ---------------------------------------------------------------------
    try:
        percentage = build_success * 100 // build_total
    except Exception:
        percentage = 0

    if main_manage_api_report:
        main_row_list = main_soup.find_all('table')[1].tr.find_next_siblings()
        main_count_list = main_row_list[-1].th.find_next_siblings()
        main_count_list[0].string.replace_with(
            "{} / {} ({}%)".format(build_success, build_total, percentage)
        )
    else:
        main_row_list = main_soup.table.table.tr.find_next_siblings()
        main_count_list = main_row_list[-1].td.find_next_siblings()
        main_count_list[0].string.replace_with(
            "{}% ({} / {})".format(percentage, build_success, build_total)
        )

    main_count_list[1].string.replace_with(str(preserved_count))
    main_count_list[2].string.replace_with(str(executed_count))
    main_count_list[3].string.replace_with(str(total_count))

    # ---------------------------------------------------------------------
    # Cleanup + output
    # ---------------------------------------------------------------------
    for div in main_soup.find_all("div", {'class': 'contents-block'}):
        div.decompose()

    div = main_soup.find("div", {'class': 'report-body'})
    if div:
        div['class'] = "report-body no-toc"

    # Write final combined report
    data = main_soup.prettify(formatter="html")

    try:
        with open(mpName + "_rebuild.html", "wb") as fd:
            fd.write(data.encode(encFmt, "replace"))
            
    except TypeError:
        # Python 2.7 fallback (no encoding arg)
        with open(mpName + "_rebuild.html", "wb") as fd:
            fd.write(data.encode("utf-8", "replace"))

    # Optional VectorCAST-specific fixup
    try:
        import fixup_reports
        main_soup = fixup_reports.fixup_2020_soup(main_soup)
    except Exception as e:
        log("[WARN] fixup_reports failed or not present: {}".format(e))

    # Write temporary combined file
    with open("combined_incr_rebuild.tmp", "wb") as fd:
        fd.write(data.encode(encFmt,"replace"))

    # ---------------------------------------------------------------------
    # Archive old reports and assets
    # ---------------------------------------------------------------------
    if not os.path.exists("rebuild_reports"):
        os.mkdir("rebuild_reports")

    for file in report_file_list:
        if file != mpName + "_rebuild.html" and os.path.exists(file):
            shutil.move(file, os.path.join("rebuild_reports", file))

    for file in glob.glob("*.css"):
        shutil.copy(file, os.path.join("rebuild_reports", file))
    for file in glob.glob("*.png"):
        shutil.copy(file, os.path.join("rebuild_reports", file))

    log("[DONE] Combined rebuild report written to {}".format(mpName + "_rebuild.html"))
    return True

if __name__ == "__main__":

    parser = argparse.ArgumentParser()
    parser = argparse.ArgumentParser(description="Parse and merge VectorCAST rebuild reports.")

    parser.add_argument('ManageProject', default='Project.vcm', help="Name of the VectorCAST Project")

    # --rptfmt can be either HTML or TEXT
    parser.add_argument('--rptfmt', choices=['HTML', 'TEXT'], default='HTML', help='Output report format: HTML or TEXT (default: HTML)')

    # --api is deprecated / unused, but still accepted (hidden from help)
    parser.add_argument('--api',help=argparse.SUPPRESS, type=int, default=None)

    # --verbose flag, defaults to False
    parser.add_argument('--verbose',action='store_true',default=False,help='Enable detailed console logging.')

    args = parser.parse_args()

    if args.rptfmt and "TEXT" in args.rptfmt:
        parse_text_files(args.ManageProject, args.verbose)
    else:
        parse_html_files(args.ManageProject, args.verbose)

