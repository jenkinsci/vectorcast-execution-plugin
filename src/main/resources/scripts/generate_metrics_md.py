#
# The MIT License
#
# Copyright 2025 Vector Informatik, GmbH.
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

from vcast_utils import checkVectorCASTVersion

import sys, io, re, os

if not checkVectorCASTVersion(20, quiet = False):
    if __name__ != "__main__":
        raise ImportError("Cannot generate metrics with DataApi. Please upgrade VectorCAST")
    else:
        print("Cannot generate metrics with DataApi. Please upgrade VectorCAST")
        sys.exit(0)

from vector.apps.ReportBuilder.custom_report import CustomReport
from vector.apps.DataAPI.vcproject_api import VCProjectApi

# ----------------------------------------------------------------------
# Emoji setup (2.7 + 3.x safe)
PASS = u"\u2705"       
FAIL = u"\u274C"       
PARTIAL = u"\U0001F7E1"
PART = PARTIAL

def updateTextMetricsReport(mpReportName):
    """
    Parse a VectorCAST metrics report (.txt) and return:
       summary  -> Overall Coverage string
       annotations -> list of [file, summary string, severity]
    """
    encFmt = "utf-8"
    COV_RE = re.compile(r"\(([\d.]+)%\)")
    TOTALS_RE = re.compile(r"^\s*(?:GRAND\s+)?TOTALS\b", re.IGNORECASE)

    def emoji_for(pct):
        """Return (emoji, weight) where weight contributes to severity."""
        try:
            p = float(pct)
        except Exception:
            return u"", 0

        if p >= 100.0:
            return PASS, -1         # perfect -> bonus credit
        elif p >= 80.0:
            return PASS, 0          # solid
        elif p >= 50.0:
            return PART, 1          # fair
        elif p > 0.0:
            return PART, 2          # weak
        else:
            return FAIL, 3          # failed

    def severity_for(emoji_pairs):
        """Compute overall severity using average weighted score."""
        weights = [w for (_, w) in emoji_pairs if w is not None]
        if not weights:
            return "LOW"

        avg = sum(weights) / float(len(weights))

        if avg <= 0:         # many perfect or high scores
            return "LOW"
        elif avg <= 0.75:    # mostly PASS with some PART
            return "MEDIUM"
        elif avg <= 1.75:    # mix of PASS/PART and a few FAIL
            return "HIGH"
        else:                # mostly FAIL
            return "CRITICAL"

    # --- find the metrics text file ---
    if not os.path.exists(mpReportName):
        raise FileNotFoundError("Cannot find metrics file: {}".format(mpReportName))

    # --- read and parse ---
    lines = []
    with open(mpReportName, "rb") as fd:
        lines = [line.decode(encFmt, "replace") for line in fd.readlines()]

    rows = []
    grand_total = None

    for line in lines:
        if not line.strip() or line.strip().startswith("-"):
            continue
        if line.upper().strip().startswith("GRAND TOTALS"):
            grand_total = line
            continue

        # file total lines contain '.c' or '.cpp' and percentages
        if (".c" in line or ".cpp" in line) and "(" in line:
            parts = re.split(r"\s{2,}", line.strip())
            file_ = parts[0]

            # Extract numeric percentages
            pcts = [float(m.group(1)) for m in COV_RE.finditer(line)]

            # Map percentages to (emoji, weight)
            emoji_pairs = [emoji_for(p) for p in pcts[:5]]
            emojis = [e for (e, _) in emoji_pairs]

            # Pad out to 5 entries if fewer found
            while len(emojis) < 5:
                emojis.append(u"")

            # Compute severity from weighted averages
            severity = severity_for(emoji_pairs)

            # Build markdown coverage summary line
            coverage_str = u"FN {0} | ST {1} | BR {2} | PR {3} | FC {4}".format(
                emojis[0], emojis[1], emojis[2], emojis[3], emojis[4])

            # Store result
            rows.append([file_, coverage_str, severity])

    # --- build summary (from GRAND TOTALS line) ---
    summary = u""
    
    if grand_total:
        pcts = [float(m.group(1)) for m in COV_RE.finditer(grand_total)]

        # get (emoji, weight) pairs, extract emoji only
        emoji_pairs = [emoji_for(p) for p in pcts[:5]]
        emojis = [e for (e, _) in emoji_pairs]

        while len(emojis) < 5:
            emojis.append(u"")

        summary = u"Overall Coverage: FN {0} | ST {1} | BR {2} | PR {3} | FC {4}".format(
            emojis[0], emojis[1], emojis[2], emojis[3], emojis[4])

    # --- build Markdown output ---
    md_lines = [u"## Summary", summary, u"\n## Annotations",
                u"| File | Summary | Severity |",
                u"|------|----------|-----------|"]

    for file_, cov, sev in rows:
        md_lines.append(u"| {0} | {1} | {2} |".format(file_, cov, sev))

    md_text = u"\n".join(md_lines)
    md_path = os.path.splitext(mpReportName)[0] + "_summary.md"

    with io.open(md_path, "w", encoding=encFmt) as out:
        out.write(md_text + u"\n")

    print(u"Markdown written to {}".format(md_path))
    
    artifactName = mpReportName.replace("_metrics_report.txt","_aggregate_report.html")
    
    workspace = os.environ['BITBUCKET_WORKSPACE']
    repo_slug = os.environ['BITBUCKET_REPO_SLUG']
    build_num = os.environ['BITBUCKET_BUILD_NUMBER']
    
    # --- Append link to full HTML report ---
    html_artifact_url = "https://bitbucket.org/{}/{}/addons/bitbucket-build/{}/artifacts/reports/html/{}".format(workspace, repo_slug, build_num, artifactName)

    return summary, rows, html_artifact_url

def generate_metrics_md(mpName):
    
    print("Generating metrics to BitBucket in Markdown format")
    mpBaseName = os.path.basename(mpName)[:-4]
    report_name = "{}_metrics_report.txt".format(mpBaseName)
    
    with VCProjectApi(mpName) as vcproj:
        CustomReport.report_from_api(vcproj, report_type="Demo", formats=["TEXT"], output_file=report_name, sections=["METRICS"])

    summary, rows, link = updateTextMetricsReport(report_name)

    return summary, rows, link

if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument('manageProject' , help='VectorCAST Project name')
    args = parser.parse_args()
    
    if not args.manageProject.endswith(".vcm"):
        args.manageProject += ".vcm"
        
    if not os.path.exists(args.manageProject):
        print ("VectorCAST Project not found! " + args.manageProject)
        sys.exit(-1)

    summary, rows = generate_metrics_md(args.manageProject)
