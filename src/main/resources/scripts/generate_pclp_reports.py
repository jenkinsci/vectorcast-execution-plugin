#####################################################################
# PC-lint Plus Report Generator / Output Format Converter
#
# Invoke with no arguments for command-line help screen with
# usage information, option list, and supported output formats.
#
# Last Updated 2023-02-07
#####################################################################

import argparse
import json
import xml.etree.ElementTree
import os
from vector.apps.DataAPI.vcproject_api import VCProjectApi
from collections import defaultdict
import subprocess

from vcast_utils import dump, checkVectorCASTVersion

try:
    from html import escape
except ImportError:
    # html not standard module in Python 2.
    from cgi import escape
from pprint import pprint

from safe_open import open

# PC-lint Plus message representation and parsing

class Message:
    def __init__(self, file, line, category, number, text):
        self.file = file if file is not None else ''
        self.line = line
        self.category = category
        self.number = number
        self.text = text
        self.supplementals = []

def parse_msgs(filename):

    # save the base directory of the input file
    directoryName = os.path.dirname(filename)
    
    try:
        basepath = os.environ['WORKSPACE'].replace("\\","/") + "/"
    except:
        basepath = os.getcwd().replace("\\","/") + "/"
    
    with open(filename, "r") as fd:
        pcplXmlData = fd.read()
    
    index = pcplXmlData.find('<')
    
    # If '<' is found, slice the string to remove characters before it
    if index != -1:
        pcplXmlData = pcplXmlData[index:]

    root = xml.etree.ElementTree.fromstring(pcplXmlData)
    # pprint(dump(tree))
    # root = tree.getroot()
    msgs = []
    last_primary_msg = None
    for child in root:
        try:
            adjustedFname = os.path.relpath(child.find('file').text,basepath).replace("\\","/")
        except:
            adjustedFname = child.find('file').text
                
        if adjustedFname is None:
            adjustedFname = "GLOBAL"
            
        msg = Message(
            adjustedFname,
            child.find('line').text,
            child.find('type').text,
            child.find('code').text,
            child.find('desc').text
        )
       
        # add the directory name to the filename
        #    this assumes that the input.xml filnames are 
        #    relative to the directory of the input file
        # msg.file = os.path.join(directoryName,msg.file)
        
        if msg.category == "supplemental":
            last_primary_msg.supplementals.append(msg)
        else:
            last_primary_msg = msg
            msgs.append(msg)
    return msgs

# HTML summary output

class FileSummary:
    def __init__(self, filename):
        self.msg_count = 0
        self.error_count = 0
        self.warning_count = 0
        self.info_count = 0
        self.note_count = 0
        self.supplemental_count = 0
        self.misra_count = 0
        self.filename = filename

def summarize_files(msgs):
    file_summaries = dict()
    for msg in msgs:
        if msg.file not in file_summaries:
            file_summaries[msg.file] = FileSummary(msg.file)
        file_summary = file_summaries[msg.file]

        if msg.category == 'error':
            file_summary.error_count += 1
        elif msg.category == 'warning':
            file_summary.warning_count += 1
        elif msg.category == 'info':
            file_summary.info_count += 1
        elif msg.category == 'note':
            file_summary.note_count += 1
        elif msg.category == 'supplemental':
            file_summary.supplemental_count += 1
        
        if msg.category != 'supplemental':
            file_summary.msg_count += 1
        
        if 'MISRA' in msg.text:
            file_summary.misra_count += 1
    return file_summaries

def build_html_table(column_headers, data_source, row_generator):
    out = ""
    out += "<table>\n"
    out += "<tr>\n"
    for header in column_headers:
        out += "<th scope=\"col\">"
        out += header
        out += "</th>\n"
    out += "</tr>\n"
    for item in data_source:
        out += "<tr>\n"
        row = row_generator(item)
        for data in row:
            out += "<td>"
            out += str(data)
            out += "</td>"
        out += "</tr>\n"
    out += "</table>\n"
    return out

def format_benign_zero(x):
    #return str(x) if x != 0 else "<span class=\"zero\">" + str(x) + "</span>\n"
    return "<span class=\"filename\">" + str(x) + "</span>\n"

def emit_html(msgs, full_mp_name = None, output_filename = None):

    out = ""
    out += "\n" + "<!DOCTYPE html><html>"
    out += "\n" + "<head>"
    out += "\n" + "<meta charset=\"utf-8\"><title>Report</title>"
    out += "\n" + "<style>" + "\n"
    out += """            /*! normalize.css v8.0.0 | MIT License | github.com/necolas/normalize.css */
            html{line-height:1.15;-webkit-text-size-adjust:100%}
            body{margin:0}
            h1{font-size:2em;margin:.67em 0}
            hr{box-sizing:content-box;height:0;overflow:visible}
            pre{font-family:monospace,monospace;font-size:1em}
            a{background-color:transparent}
            abbr[title]{border-bottom:none;text-decoration:underline;text-decoration:underline dotted}
            b,strong{font-weight:bolder}
            code,kbd,samp{font-family:monospace,monospace;font-size:1em}
            small{font-size:80%}
            sub,sup{font-size:75%;line-height:0;position:relative;vertical-align:baseline}
            sub{bottom:-.25em}
            sup{top:-.5em}
            img{border-style:none}
            button,input,optgroup,select,textarea{font-family:inherit;font-size:100%;line-height:1.15;margin:0}
            button,input{overflow:visible}
            button,select{text-transform:none}
            [type=button],[type=reset],[type=submit],button{-webkit-appearance:button}
            [type=button]::-moz-focus-inner,[type=reset]::-moz-focus-inner,[type=submit]::-moz-focus-inner,button::-moz-focus-inner{border-style:none;padding:0}
            [type=button]:-moz-focusring,[type=reset]:-moz-focusring,[type=submit]:-moz-focusring,button:-moz-focusring{outline:1px dotted ButtonText}
            fieldset{padding:.35em .75em .625em}
            legend{box-sizing:border-box;color:inherit;display:table;max-width:100%;padding:0;white-space:normal}
            progress{vertical-align:baseline}
            textarea{overflow:auto}
            [type=checkbox],[type=radio]{box-sizing:border-box;padding:0}
            [type=number]::-webkit-inner-spin-button,[type=number]::-webkit-outer-spin-button{height:auto}
            [type=search]{-webkit-appearance:textfield;outline-offset:-2px}
            [type=search]::-webkit-search-decoration{-webkit-appearance:none}
            ::-webkit-file-upload-button{-webkit-appearance:button;font:inherit}
            details{display:block}
            summary{display:list-item}
            template{display:none}
            [hidden]{display:none}
            html {box-sizing:border-box;position:relative;height:100%;width:100%;}
            *, *:before, *:after {box-sizing:inherit;}
            body {position:relative;height:100%;width:100%;font-size:10pt;font-family:helvetica, Arial, sans-serif;color:#3a3e3f;}
            .alternate-font {font-family:Arial Unicode MS, Arial, sans-serif;}
            #page {position:relative;width:100%;height:100%;overflow:hidden;}
            #title-bar {position:absolute;top:0px;left:0em;right:0px;height:1.8em;background-color:#B1B6BA;white-space:nowrap;box-shadow:1px 1px 5px black;z-index:100;}
            #report-title {font-size:3em;text-align:center;font-weight:bold;background-color:white;padding:0.5em;margin-bottom:0.75em;border:1px solid #e5e5e5;}
            .contents-block {position:absolute;top:1.8em;left:0em;width:22em;bottom:0em;overflow:auto;background-color:#DADEE1;border-right:1px solid silver;padding-left:0.75em;padding-right:0.5em;}
            .testcase .report-block, .testcase .report-block-coverage {padding-bottom:2em;border-bottom:3px double #e5e5e5;}
            .testcase .report-block:last-child, .testcase .report-block-coverage:last-child {border-bottom:0px solid white;}
            .report-body {position:absolute;top:1.8em;left:22em;right:0em;bottom:0em;padding-left:2em;padding-right:2em;overflow:auto;padding-bottom:1.5em;background-color:#DADEE1;}
            .report-body.no-toc {left:0em;}
            .report-body > .report-block, .report-body > .report-block-coverage, .report-body > .report-block-scroll, .report-body > .testcase {border:1px solid #e5e5e5;margin-bottom:2em;padding-bottom:1em;padding-right:2em;background-color:white;padding-left:2em;padding-top:0.1em;margin-top:1em;}
            .report-body > .report-block-scroll {overflow-x:visible;background-color:inherit;}
            .title-bar-heading {display:none;position:absolute;text-align:center;width:100%;color:white;font-size:3em;bottom:0px;margin-bottom:0.3em;}
            .title-bar-logo {display:inline-block;height:100%;}
            .title-bar-logo img {width:120px;margin-left:0.5em;margin-top:-16px;}
            .contents-block ul {padding-left:1.5em;list-style-type:none;line-height:1.5;}
            li.collapsible-toc > input ~ ul {display:none;}
            li.collapsible-toc > input:checked ~ ul {display:block;}
            li.collapsible-toc > input {display:none;}
            li.collapsible-toc > label {cursor:pointer;}
            li.collapsible-toc > label.collapse {display:none;}
            li.collapsible-toc > label.expand {display:inline;}
            li.collapsible-toc > input:checked ~ label.expand {display:none;}
            li.collapsible-toc > input:checked ~ label.collapse {display:inline;}
            .contents-block ul.toc-level1 {padding-left:1em;}
            .contents-block ul.toc-level1 > li {margin-top:0.75em;}
            .contents-block li {white-space:nowrap;text-overflow:ellipsis;overflow:hidden;}
            .tc-passed:before, .tc-failed:before, .tc-none:before{display:inline-block;margin-right:0.25em;width:0.5em;text-align:center;}
            .tc-passed:before {content:"+";color:darkgreen;}
            .tc-failed:before {content:"x";color:#b70032;}
            .tc-none:before {content:"o";}
            .tc-item {margin-top:0.75em;margin-left:-1em;}
            table {margin-top:0.5em;margin-bottom:1em;border-collapse:collapse;min-width:40em;}
            .expansion_row_icon_minus, .expansion_file_icon_minus, .expansion_all_icon_minus{display:none !important;}
            .sfp-table{margin:0;border-bottom:1px solid gray;min-width:50em;}
            .sfp-table th{padding:0 !important;border-bottom:solid 1px grey;font-size:15px;padding-left:2px !important;padding-right:2px !important;position:sticky;top:0;box-shadow:inset 0 -1px 0 gray;background-color:#F3F4F5;}
            .sfp-table td{padding:0;border-right:solid 1px grey;border-bottom:0;}
            .sfp-table td:last-child {border-right:0;padding-left:5px !important;}
            .sfp-table tr:hover, .highlight:hover{background-color:#F3F4F5;}
            table.table-hover tbody tr:hover {background-color:#DADEE1;}
            th, td {text-align:left;padding:0.25em;padding-right:1em;border-bottom:1px solid #e5e5e5;}
            table thead th {border-bottom:2px solid silver;border-top:0;border-left:0;border-right:0;}
            .top-align {vertical-align:top;}
            .pull-right {display:none;}
            h1, h2 {border-bottom:3px solid silver;}
            h1, h2, h3, h4 {margin-top:1em;margin-bottom:0.25em;}
            h1 {font-size:3.5em;}
            h2 {font-size:2.5em;}
            h3 {font-size:2em;}
            h4 {font-size:1.25em;border-bottom:0;margin-bottom:0;}
            h5 {font-size:1em;font-weight:bold;margin-top:0.5em;margin-bottom:0.25em;}
            pre {padding:1em;padding-left:1.5em;border:1px solid #e5e5e5;background-color:#DADEE1;min-width:40em;width:auto;clear:both;display:inline-block;margin-top:0.25em;}
            .sfp-pre{padding:0;background-color:transparent;border:0;margin:0;}
            ul.unstyled {list-style-type:none;margin-top:0.25em;padding-left:0px;}
            ul {line-height:1.3;}
            p {margin-top:0.5em;margin-bottom:1em;max-width:50em;}
            pre p {max-width:inherit;}
            a, a:visited {color:inherit;text-decoration:none;}
            a:hover {text-decoration:underline;text-decoration-color:#b70032;-webkit-text-decoration-color:#b70032;-moz-text-decoration-color:#b70032;cursor:pointer;}
            pre.aggregate-coverage {padding-left:0px;padding-right:0px;margin-top:1em;}
            pre.aggregate-coverage span {width:100%;display:inline-block;padding-left:1em;padding-right:1em;line-height:1.3;}
            .sfp-span{width:auto ! important;padding-left:0 ! important;padding-right:0 ! important;}
            .sfp-span-color{width:100% !important;padding-left:1px !important;padding-right:1px !important;}
            .sfp_number{text-align:right;width:1px;white-space:nowrap;padding-right:2px !important;}
            .sfp_coverage{text-align:center;width:1px;}
            .sfp_color{padding-left:0 !important;padding-right:0 !important;width:1px;}
            .up_arrow{position:absolute;left:65px;}
            .collapse_icon{float:right;height:24px;width:24px;display:inline-block;color:black;font-size:24px;line-height:24px;text-align:center;border-radius:50%;cursor:pointer;}
            .underline{color:#a0a0a0;}
            .temp_description{font-style:italic;}
            .ips{display:none;}
            .bg-success, .success, span.full-cvg {background-color:#c8f0c8;}
            .sfp-full{background-color:#28a745;}
            .sfp-part{background-color:#ffc107;}
            .sfp-none{background-color:#dc3545;}
            span.ann-cvg, span.annpart-cvg {background-color:#cacaff;}
            .bg-danger, .danger, span.no-cvg {background-color:#facaca;}
            .bg-warning, .warning, span.part-cvg {background-color:#f5f5c8;}
            .fit-content {width:max-content;}
            .rel-pos {position:relative;}
            .report-block > .bs-callout.test-timeline, .mcdc-condition {padding-left:0px;}
            .report-block.single-test {padding-left:2em;padding-right:2em;width:100%;height:100%;overflow:auto;}
            .test-action-header + h4.event{margin-top:0.5em;}
            .test-auto-header {background-color:LightGray;}
            .event.bs-callout > .bs-callout, .mcdc-condition {margin-bottom:2em;border-left:2px solid silver;}
            .event.bs-callout {margin-top:1.5em;}
            .bs-callout, .mcdc-condition {padding-left:1.5em;}
            .bs-callout.bs-callout-success {border-left:2px solid green;margin-left:0px;}
            .bs-callout.bs-callout-success .bs-callout.bs-callout-success {border-left:none;margin-left:0px;}
            .bs-callout.bs-callout-danger {border-left:2px solid #b70032;margin-left:0px;}
            .bs-callout.bs-callout-warning {border-left:2px solid wheat;padding-left:1.5em;margin-left:0px;}
            .event.bs-callout .bs-callout.bs-callout-warning {border-left:2px solid wheat;padding-left:1.5em;margin-left:0px;}
            .bs-callout.bs-callout-danger .bs-callout.bs-callout-danger {margin-left:1.5em;border-left:2px solid #b70032;padding-left:1.5em;}
            .bs-callout-info {margin-top:1em;}
            .text-muted {font-size:0.9em;}
            .test-action-header {border-top:3px solid silver;border-bottom:1px solid #e5e5e5;min-width:42em;white-space:nowrap;margin-bottom:0.5em;padding-top:0.25em;padding-bottom:0.25em;margin-top:2em;background-color:#DADEE1;margin-left:-1.5em;padding-left:1.5em;}
            .test-action-header h4 {margin-top:0px;}
            .test-action-header:first-child{margin-top:0px;}
            .test-timeline > .event.bs-callout:first-child{margin-top:1em;}
            .event > .bs-callout {margin-left:2px !important;}
            .testcase-notes {white-space:pre;word-wrap:none;}
            .mcdc-condition {margin-bottom:3em;}
            .mcdc-table {margin-top:0px;}
            .mcdc-rows-table {margin-top:2em;min-width:auto;}
            .mcdc-rows-table td{border:1px solid #e5e5e5;}
            .mcdc-condition.full-cvg {border-left-color:green;border-left-width:2px;}
            .mcdc-condition.part-cvg {border-left-color:wheat;border-left-width:2px;}
            .mcdc-condition.no-cvg {border-left-color:#b70032;border-left-width:2px;}
            .i0 {padding-left:0.25em;min-width:11em }
            .i1 {padding-left:1.25em;min-width:11em }
            .i2 {padding-left:2.25em;}
            .i3 {padding-left:3.25em;}
            .i4 {padding-left:4.25em;}
            .i5 {padding-left:5.25em;}
            .i6 {padding-left:6.25em;}
            .i7 {padding-left:7.25em;}
            .i8 {padding-left:8.25em;}
            .i9 {padding-left:9.25em;}
            .i10 {padding-left:10.25em;}
            .i11 {padding-left:11.25em;}
            .i12 {padding-left:12.25em;}
            .i13 {padding-left:13.25em;}
            .i14 {padding-left:14.25em;}
            .i15 {padding-left:15.25em;}
            .i16 {padding-left:16.25em;}
            .i17 {padding-left:17.25em;}
            .i18 {padding-left:18.25em;}
            .i19 {padding-left:19.25em;}
            .i20 {padding-left:20.25em;}
            .right {float:right;}
            .req-notes-list {white-space:pre;}
            .uncovered_func_calls-list {white-space:pre;}
            .bold-text {font-weight:bold;}
            .static-cell {}
            .static-cell-right {text-align:right;}
            .static-doc {width:100%;}
            .static-title-bar {height:1.9em;width:100%;font-size:1.7em;border-bottom:1px solid #e5e5e5;margin:0;padding-top:8px;font-weight:bold;}
            .static-id {float:left;width:100px;}
            .static-title {float:left;}
            .static-doctext {width:auto;background-color:#efefef;border:1px solid #e5e5e5;min-width:40em;}
            .static-doctext pre {padding:0;margin:0.2em 0 0.2em 1em;border:none;}
            .static-doctext p {padding:0;margin:0.2em 0 0.2em 1em;}
            .static-doctext h2 {font-size:1.5em;}
            .static-doctext h3 {font-size:1.3em;}
            .missing_control_flow {max-width:40em;}
            .preformatted {font-family:monospace;white-space:pre;}
            .probe_point_notes {font-family:monospace;width:15%;}
            .col_unit {word-break:break-all;width:30%;}
            .col_subprogram {word-break:break-all;width:30%;}
            .col_complexity {white-space:nowrap;}
            .col_metric {white-space:nowrap;}
            .col_nowrap {white-space:nowrap;}
            .col_wrap {word-break:break-all;}
            .subtitle {font-size:1.3em;}
            @media print{html, body {height:auto;overflow:auto;color:black;}
            .contents-block {display:none;}
            #title-bar {position:initial;height:20pt;}
            #main-scroller {background-color:white;margin:0;padding:0;position:initial;left:auto;top:auto;}
            pre {background-color:white;font-size:10pt;border:none;border-left:1px solid #e5e5e5;font-size:9pt;}
            .report-body > .testcase, .report-body > .report-block, .report-body > .report-block-coverage {border:none;padding-bottom:0in;}
            .report-body > .testcase {page-break-before:always }
            #report-title {border:none;font-size:3em;text-align:left;font-weight:bold;padding-bottom:0px;border-bottom:3px solid silver;}
            h1, h2, h3, h4 {padding-top:0;margin-top:0.2in;}
            html, body, .report-body {background-color:white;font-size:1vw;}
            }
"""
    out += "\n" + "</style>"
    out += "\n" + "</head>"
    out += "\n" + "<body>"
    out += """<div id='page'>
            <!-- ReportTitle -->
            <div id="title-bar">
                <div class="title-bar-logo">
                    <img alt="Vector" src="data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4NCjwhLS0gR2VuZXJhdG9yOiBBZG9iZSBJbGx1c3RyYXRvciAxNi4wLjAsIFNWRyBFeHBvcnQgUGx1Zy1JbiAuIFNWRyBWZXJzaW9uOiA2LjAwIEJ1aWxkIDApICAtLT4NCjwhRE9DVFlQRSBzdmcgUFVCTElDICItLy9XM0MvL0RURCBTVkcgMS4xLy9FTiIgImh0dHA6Ly93d3cudzMub3JnL0dyYXBoaWNzL1NWRy8xLjEvRFREL3N2ZzExLmR0ZCI+DQo8c3ZnIHZlcnNpb249IjEuMSIgaWQ9IldPUlQtX3gyRl9CSUxETUFSS0Vfc1JHQiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4bWxuczp4bGluaz0iaHR0cDovL3d3dy53My5vcmcvMTk5OS94bGluayINCgkgeD0iMHB4IiB5PSIwcHgiIHdpZHRoPSI0MTUuMTc5cHgiIGhlaWdodD0iMTk3LjU3MnB4IiB2aWV3Qm94PSIwIDAgNDE1LjE3OSAxOTcuNTcyIiBlbmFibGUtYmFja2dyb3VuZD0ibmV3IDAgMCA0MTUuMTc5IDE5Ny41NzIiDQoJIHhtbDpzcGFjZT0icHJlc2VydmUiPg0KPHBvbHlnb24gaWQ9IkJpbGRtYXJrZV8xODNfeDJGXzBfeDJGXzUwIiBmaWxsPSIjQjcwMDMyIiBwb2ludHM9IjI5Mi4yODgsMTE1LjI1IDI5Mi4yODgsMTMxLjcxNSAzNDkuMzIyLDk4Ljc4NiAyOTIuMjg4LDY1Ljg1NyANCgkyOTIuMjg4LDgyLjMyMSAzMjAuODA1LDk4Ljc4NiAiLz4NCjxwb2x5Z29uIHBvaW50cz0iOTAuNTU0LDg0LjUyOSA5OC43ODcsODQuNTI5IDgyLjMyMiwxMTMuMDQ3IDY1Ljg1Nyw4NC41MjkgNzQuMDg5LDg0LjUyOSA4Mi4zMjIsOTguNzg4ICIvPg0KPHBhdGggZD0iTTIxNy43NTQsODMuODE5Yy04LjI2OSwwLTE0Ljk3Miw2LjcwMy0xNC45NzIsMTQuOTcyYzAsOC4yNjksNi43MDMsMTQuOTcxLDE0Ljk3MiwxNC45NzENCgljOC4yNjksMCwxNC45NzItNi43MDMsMTQuOTcyLTE0Ljk3MUMyMzIuNzI2LDkwLjUyMiwyMjYuMDIzLDgzLjgxOSwyMTcuNzU0LDgzLjgxOXogTTIxNy43NTUsMTA2LjY2OA0KCWMtNC4zNTEsMC03Ljg3OC0zLjUyNy03Ljg3OC03Ljg3OGMwLTQuMzUxLDMuNTI3LTcuODc3LDcuODc4LTcuODc3YzQuMzUxLDAsNy44NzgsMy41MjcsNy44NzgsNy44NzcNCglDMjI1LjYzMywxMDMuMTQyLDIyMi4xMDUsMTA2LjY2OCwyMTcuNzU1LDEwNi42Njh6Ii8+DQo8cGF0aCBkPSJNMTU1LjEyMSwxMDYuNjY4Yy00LjM1MSwwLTcuODc4LTMuNTI3LTcuODc4LTcuODc4YzAtNC4zNTEsMy41MjctNy44NzcsNy44NzgtNy44NzdjMi4xNzQsMCw0LjE0MywwLjg4MSw1LjU2OCwyLjMwNQ0KCWw1LjAxNi01LjAxNmMtMi43MDktMi43MDgtNi40NTEtNC4zODMtMTAuNTg0LTQuMzgzYy04LjI2OSwwLTE0Ljk3Miw2LjcwMy0xNC45NzIsMTQuOTcyYzAsOC4yNjksNi43MDMsMTQuOTcxLDE0Ljk3MiwxNC45NzENCgljNC4xMzYsMCw3Ljg3OS0xLjY3NiwxMC41ODktNC4zODdsLTUuMDE2LTUuMDE2QzE1OS4yNjgsMTA1Ljc4NiwxNTcuMjk3LDEwNi42NjgsMTU1LjEyMSwxMDYuNjY4eiIvPg0KPHBvbHlnb24gcG9pbnRzPSIxNzEuNDQ2LDkwLjk0NiAxODEuMDcxLDkwLjk0NiAxODEuMDcxLDExMy4wNDcgMTg4LjIwMSwxMTMuMDQ3IDE4OC4yMDEsOTAuOTQ2IDE5Ny44MjUsOTAuOTQ2IDE5Ny44MjUsODQuNTI5IA0KCTE3MS40NDYsODQuNTI5ICIvPg0KPHBvbHlnb24gcG9pbnRzPSIxMDguMDY5LDExMy4wNDcgMTMwLjE3LDExMy4wNDcgMTMwLjE3LDEwNi42MyAxMTUuMTk4LDEwNi42MyAxMTUuMTk4LDEwMS42NCAxMjYuODkxLDEwMS42NCAxMjYuODkxLDk1LjM2NiANCgkxMTUuMTk4LDk1LjM2NiAxMTUuMTk4LDkwLjk0NiAxMzAuMTcsOTAuOTQ2IDEzMC4xNyw4NC41MjkgMTA4LjA2OSw4NC41MjkgIi8+DQo8cGF0aCBkPSJNMjcwLjE4Nyw5NC4wNDdjMC01LjI1LTQuMjU1LTkuNTE4LTkuNTA1LTkuNTE4aC0xNy41ODd2MjguNTE4aDcuMTI5di05LjQ4Mmg2LjI0NGw1LjQ4Nyw5LjQ4Mmg4LjIzMWwtNS44OTktMTAuMjMxDQoJQzI2Ny43NDgsMTAxLjM5NSwyNzAuMTg3LDk4LjAyLDI3MC4xODcsOTQuMDQ3eiBNMjYwLjAyMyw5Ny4yMmgtOS43OTh2LTYuMzQ2aDkuNzk2YzEuNjg5LDAuMDc0LDMuMDM3LDEuNDY2LDMuMDM3LDMuMTczDQoJQzI2My4wNTgsOTUuNzUzLDI2MS43MTEsOTcuMTQ1LDI2MC4wMjMsOTcuMjJ6Ii8+DQo8L3N2Zz4NCg=="/>
                </div>
            </div>
            <!-- TableOfContents -->
            <div class='contents-block'>
                <a id="TableOfContents"/>
                <h3 class="toc-title-small">Contents</h3>
                <ul class="toc-level1">
                    <li class=""><a href="#Summary">PC-Lint Plus Summary</a></li>
                    <li class=""><a href="#Details">PC-Lint Plus Details</a></li>
                    <li class=" collapsible-toc" title="Source Code">
                        <label for="collapsible-5">Source Code</label>
                        <input type="checkbox" id="collapsible-5"/>
                        <label class="expand" for="collapsible-5">></label>
                        <label class="collapse" for="collapsible-5">V</label>
                        <ul>
                            <li class=""><a href="#CurrentRelease_encrypt_src_encrypt_c">encrypt.c</a></li>
                            <li class=""><a href="#CurrentRelease_main_pos_driver_c">pos_driver.c</a></li>
                            <li class=""><a href="#CurrentRelease_order_entry_inc_waiting_list_h">waiting_list.h</a></li>
                            <li class=""><a href="#CurrentRelease_order_entry_src_manager_c">manager.c</a></li>
                            <li class=""><a href="#CurrentRelease_order_entry_src_waiting_list_c">waiting_list.c</a></li>
                            <li class=""><a href="#CurrentRelease_utils_inc_linked_list_h">linked_list.h</a></li>
                            <li class=""><a href="#CurrentRelease_utils_src_linked_list_c">linked_list.c</a></li>
                            <li class=""><a href="#CurrentRelease_utils_src_whitebox_c">whitebox.c</a></li>                    
                        </ul>
                    </li>
                </ul>
            </div>
    """    
    out += "\n" + "<h1>PC-Lint Plus Report</h1>"
    out += "\n" + "<h2 id=\"Summary\">Summary</h2>"
    file_summaries = summarize_files(msgs)
    summary_total = FileSummary('Total')
    for file in file_summaries.values():
        summary_total.msg_count += file.msg_count
        summary_total.error_count += file.error_count
        summary_total.warning_count += file.warning_count
        summary_total.info_count += file.info_count
        summary_total.note_count += file.note_count
        summary_total.misra_count += file.misra_count
    file_summaries['Total'] = summary_total
    out += build_html_table(
        ['File', 'Messages','Error','Warning','Info','Note','MISRA'],
        file_summaries.values(),
        lambda file: [ 
            ("\n<span class=\"filename\">" + "<a href=\"#" + file.filename.replace("/","_").replace(".","_") + "\">" + escape(file.filename.strip()) + "</a>" + "</span>\n") if file.filename != 'Total' else file.filename,
            format_benign_zero(file.msg_count),
            format_benign_zero(file.error_count),
            format_benign_zero(file.warning_count),
            format_benign_zero(file.info_count),
            format_benign_zero(file.note_count),
            format_benign_zero(file.misra_count)
        ]
    )
    out += "<h2 id=\"Details\">Details</h2>\n"
    out += build_html_table(
        ['File', 'Line', 'Category', '#', 'Description'],
        msgs,
        lambda msg: [
            "\n<span class=\"filename\">" + "<a href=\"#" + msg.file.replace("/","_").replace(".","_") + "_" + escape(msg.line.strip()) + "\">" + escape(msg.file) + "</a></span>\n",
            "\n<span class=\"filename\">" + msg.line if msg.line != "0" else "" + "</span>",
            "\n<span class=\"filename\">" + msg.category + "</span>\n",
            "\n<span class=\"filename\">" + msg.number + "</span>\n",
            "\n<span class=\"filename\">" + escape(msg.text) + "</span>\n"
            ]
    )
    out += "\n" + "<h2 id=\"SourceCode\">Source Code</h2>"
    out += "\n<div>\n"
    out += processSource(full_mp_name, file_summaries, msgs, output_filename)    
    out += "\n</div>\n"
    
    out += "\n</body>\n"
    out += "\n</html>\n"
    return out

# Text output

def processSource(full_mp_name, file_summaries, msgs, output_filename):
    
    if not checkVectorCASTVersion(21, True):
        print("XXX Cannot generate Source Code section of the PC-Line Report report")
        print("XXX The Summary and File Detail sections are present in " + output_filename)
        print("XXX If you'd like to see the Source Code section of the PC-Line Report, please upgrade VectorCAST")
        
    filenames = []
    filenames = list(map(lambda file: file.filename, file_summaries.values()))

    filename_dict = {file.filename.lower(): file.filename for file in file_summaries.values()}
    
    # Use a lambda inside map to create a dictionary keyed by file and then by line
    messages_by_file_and_line = {}

    # Group by file
    list(map(lambda msg: messages_by_file_and_line.setdefault(msg.file, {})
             .setdefault(msg.line, msg), msgs))    
    
    if full_mp_name is None:
        return ""
        
    output = ""
    
    api = VCProjectApi(full_mp_name)
    output = ""

    localUnits = api.project.cover_api.SourceFile.all()
    localUnits.sort(key=lambda x: (x.name))

    try:
        basepath = os.environ['WORKSPACE'].replace("\\","/") + "/"
    except:
        basepath = os.getcwd().replace("\\","/") + "/"
    
        
    for f in localUnits:
        try:
            adjustedFname = os.path.relpath(f.display_path,basepath).replace("\\","/").lower()
        except:
            adjustedFname = f.display_path.lower()
                
        if adjustedFname not in filename_dict.keys():
            continue
        
        fname = filename_dict[adjustedFname]
        orig_fname = fname;
        
        smap = dict()
        
        for line in f.iterate_coverage():
            smap[line.line_number] = line
                           
        basename = os.path.basename(fname)
        
        filename_anchor = orig_fname.replace("/","_").replace(".","_")
        
        output += "<h3 id=" + filename_anchor + ">Coverage for " + escape(fname) + "</h3>\n"
        output += "<pre class=\"aggregate-coverage\">\n"
            
        if not os.path.isfile(fname) and not os.path.isfile(fname + ".vcast.bak"):
            sys.stderr.write(fname + " not found in the current source tree...skipping\n")
            return 
                
        if os.path.isfile(fname + ".vcast.bak"):
            fname = fname + ".vcast.bak"
            
        with open(fname , 'r') as fh:
            # read and replace the line ending for consistency
            contents = fh.read()
            contents = contents.replace("\r\n", "\n").replace("\r","\n")
            for lineno, line in enumerate(contents.splitlines(), start=1):
            
                # get all the statements
                srcLine = smap[lineno]

                lineno_str = str(lineno)
                lineno_str_justified = str(lineno).ljust(6)
                esc_line = escape(line)
                
                if lineno_str in messages_by_file_and_line[orig_fname].keys():
                    msg = messages_by_file_and_line[orig_fname][lineno_str]
                    esc_msg_text = escape(msg.text)
                    tooltip = msg.category + " " + str(msg.number) + " " + esc_msg_text              
                    anchor =  filename_anchor + "_" + lineno_str
                    output += "<div id=\"" + anchor + "> class=\"tooltip\">"
                    output += "<span class=\"na-cvg\">"
                    output += lineno_str_justified + " <span class=\"tooltiptext\"> " + tooltip + "</span>" + esc_line
                    output += "</span>"
                    output += "</div>"
                else:
                    output += "<span class=\"na-cvg\">" + lineno_str_justified + " " +  esc_line + "</span>"

            output += "</pre>"

    return output
    
def has_any_coverage(line):
    
    return (line.metrics.statements + 
        line.metrics.branches + 
        line.metrics.mcdc_branches + 
        line.metrics.mcdc_pairs + 
        line.metrics.functions +
        line.metrics.function_calls)

def text_format_msg(msg):
    out = ""
    if (msg.file and msg.file != "") or (msg.line and msg.line != '0'):
        out += msg.file + " " + str(msg.line) + " "
    out += msg.category + " " + str(msg.number) + ": "
    out += msg.text
    for supplemental in msg.supplementals:
        out += text_format_msg(supplemental)
    return out

def emit_text(msgs):
    out = ""
    for msg in msgs:
        out += text_format_msg(msg)
    return out

# JSON output

def json_transform_key(k):
    if k == 'number':
        k = 'msgno'
    return k

def json_should_include_item(k,v):
    return bool(v) and v != "0"

def json_serialize_msg(msg):
    items = {json_transform_key(k):v for k, v in msg.__dict__.items() if json_should_include_item(k,v)}
    return items

def emit_json(msgs):
    return json.dumps(msgs, default=json_serialize_msg, sort_keys=True, indent=4)


def gitlab_serialize_msg(msg):

    fname = None
    lineno = None
    findingNum = None
    findingDesc = None
    
    import hashlib
    fingerprint = hashlib.md5((msg.line + msg.category + msg.number + msg.text).encode('utf-8')).hexdigest()

    items = {}
    items["type"] = "issue"
    items["fingerprint"] = fingerprint
    
    for key, value in msg.__dict__.items():
        if key == "category":
            if value == 'error':
                items["severity"] = 'critical'
            elif value == 'warning':
                items["severity"] = 'minor'
            elif value == 'info' or value == 'note':
                items["severity"] = 'info'
            else:
                return {}
        
        if key == "number":
            findingNum = value
            
        if key == "text":
            findingDesc = value.split("[")[0].strip()
            
        if key == "file":
            fname = value
        elif key == "line":
            lineno = value
            
        if findingNum is not None and findingDesc is not None:
            outStr = ""
            count = 0
            for part in findingDesc.split("'"):
               if (count % 2) == 0:
                  outStr += part.strip()  + " "
               count += 1
                  
            items["check_name"] = findingNum + " " + outStr.strip()
            items["description"] = findingNum + ": " + value      
            findingDesc = None
            findingNum = None
            
        if fname is not None and lineno is not None:
            items['location'] = {}
            items['location']['path'] = fname.replace("\\","/")
            items['location']['lines'] = {}
            items['location']['lines']['begin'] = lineno
            fname = None
            lineno = None
            
    return_items = {}
    return_items['description'] = items['description']
    return_items['check_name'] = items['check_name'].replace(" ","-")
    return_items['fingerprint'] = items['fingerprint']
    return_items['severity'] = items['severity']
    return_items['location'] = {}
    return_items['location']['path'] = items['location']['path']
    return_items['location']['lines'] = {}
    return_items['location']['lines']['begin'] = int(items['location']['lines']['begin'])
    

    return return_items


def emit_gitlab(msgs):
    return json.dumps(msgs, default=gitlab_serialize_msg, indent=2)

# Driver

def write_output(output, filename):
    with open(filename, 'w') as file:
        try:
            file.write(output)
        except:
            file.write(output.decode('utf-8'))
        
def generate_reports(input_xml, output_text = None, output_html = None, output_json = None, output_gitlab = None, full_mp_name = None):
    msgs = parse_msgs(input_xml)
    msgs.sort(key=lambda msg: (msg.file == "", msg.file, int(msg.line) if msg.line != "" else 0))
    if output_text:
        write_output(emit_text(msgs), output_text)
    if output_html:
        write_output(emit_html(msgs, full_mp_name, output_html), output_html)
    if output_json:
        write_output(emit_json(msgs), output_json)
    if output_gitlab:
        write_output(emit_gitlab(msgs), output_gitlab)

def parse_args():
    parser = argparse.ArgumentParser(description='Generate HTML, JSON, or text output from PC-lint Plus XML reports (XML reports are produced by running PC-lint Plus with env-xml.lnt)')
    parser.add_argument('--input-xml', action='store', help='XML input filename', required=True)
    parser.add_argument('--output-text', action='store', help='Text output filename', default = None, required=False)
    parser.add_argument('--output-html', action='store', help='HTML output filename', default = None, required=False)
    parser.add_argument('--output-json', action='store', help='JSON output filename', default = None, required=False)
    parser.add_argument('--output-gitlab', action='store', help='GitLab output filename', default = None, required=False)
    parser.add_argument('--vc-project', action='store', help='VectorCAST Project Name.  Used for source view', dest="full_mp_name", default = None, required=False)
    parser.add_argument('-g', '--gen-lint-xml-cmd', action='store', help='Command to genreate lint XML files', dest="gen_lint_xml_cmd", default = None, required=False)
    
    return parser.parse_args()
    
def main():
    
    args = parse_args()
    
    if args.gen_lint_xml_cmd is not None:
        subprocess.run(args.gen_lint_xml_cmd)

    if not (args.output_text or args.output_html or args.output_json or args.output_gitlab):
        parser.error("please specify one or more outputs using the '--output-<FORMAT>=<FILENAME>' options")

        
    generate_reports(args.input_xml, args.output_text, args.output_html, args.output_json, args.output_gitlab, args.full_mp_name)

    ## if opened from VectorCAST GUI...
    if (args.full_mp_name is not None) and (args.output_html is not None) and (os.getenv('VCAST_PROG_STARTED_FROM_GUI') == "true"):
        from vector.lib.core import VC_Report_Client

        # Open report in VectorCAST GUI
        report_client = VC_Report_Client.ReportClient()
        if report_client.is_connected():
            report_client.open_report(args.output_html, "PC Lint Plus results")
            
if __name__ == "__main__":

    main() 
