#####################################################################
# PC-lint Plus Report Generator / Output Format Converter
#
# Invoke with no arguments for command-line help screen with
# usage information, option list, and supported output formats.
#
# Last Updated 2023-02-07
#####################################################################

import argparse
import html
import json
import xml.etree.ElementTree
import os

from pprint import pprint

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
    
    
    tree = xml.etree.ElementTree.parse(filename)
    root = tree.getroot()
    msgs = []
    last_primary_msg = None
    for child in root:
        msg = Message(
            child.find('file').text,
            child.find('line').text,
            child.find('type').text,
            child.find('code').text,
            child.find('desc').text
        )
       
        # add the directory name to the filename
        #    this assumes that the input.xml filnames are 
        #    relative to the directory of the input file
        msg.file = os.path.join(directoryName,msg.file)
        
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
    out += "<table>"
    out += "<tr>"
    for header in column_headers:
        out += "<th scope=\"col\">"
        out += header
        out += "</th>"
    out += "</tr>"
    for item in data_source:
        out += "<tr>"
        row = row_generator(item)
        for data in row:
            out += "<td>"
            out += str(data)
            out += "</td>"
        out += "</tr>"
    out += "</table>"
    return out

def format_benign_zero(x):
    return str(x) if x != 0 else "<span class=\"zero\">" + str(x) + "</span>"

def emit_html(msgs):
    out = ""
    out += "<!DOCTYPE html><html>"
    out += "<head>"
    out += "<meta charset=\"utf-8\"><title>Report</title>"
    out += "<style>"
    out += "body { font-family: sans-serif; margin: 1em; }"
    out += "table { border-collapse: collapse; }"
    out += "td { padding: 0.25em; border: 1px solid #AAAAAA; }"
    out += "th { padding: 0.5em; }"
    out += ".filename { font-family: monospace; font-weight: bold; }"
    out += ".zero { color: #AAAAAA; }"
    out += "</style>"
    out += "</head>"
    out += "<body>"
    out += "<div>"
    out += "<h1>Report</h1>"
    out += "<h2>Summary</h2>"
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
            ("<span class=\"filename\">" + html.escape(file.filename) + "</span>") if file.filename != 'Total' else file.filename,
            format_benign_zero(file.msg_count),
            format_benign_zero(file.error_count),
            format_benign_zero(file.warning_count),
            format_benign_zero(file.info_count),
            format_benign_zero(file.note_count),
            format_benign_zero(file.misra_count)
        ]
    )
    out += "<br>"
    out += "<h2>Details</h2>"
    out += build_html_table(
        ['File', 'Line', 'Category', '#', 'Description'],
        msgs,
        lambda msg: [
            "<span class=\"filename\">" + html.escape(msg.file) + "</span>",
            msg.line if msg.line != "0" else "",
            msg.category,
            msg.number,
            html.escape(msg.text)
        ]
    )
    out += "</div>"
    out += "</body>"
    out += "</html>\n"
    return out

# Text output

def text_format_msg(msg):
    out = ""
    if (msg.file and msg.file != "") or (msg.line and msg.line != '0'):
        out += msg.file + " " + str(msg.line) + " "
    out += msg.category + " " + str(msg.number) + ": "
    out += msg.text + "\n"
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
        file.write(output)
        
def generate_reports(input_xml, output_text = None, output_html = None, output_json = None, output_gitlab = None):
    msgs = parse_msgs(input_xml)
    msgs.sort(key=lambda msg: (msg.file == "", msg.file, int(msg.line) if msg.line != "" else 0))
    if output_text:
        write_output(emit_text(msgs), output_text)
    if output_html:
        write_output(emit_html(msgs), output_html)
    if output_json:
        write_output(emit_json(msgs), output_json)
    if output_gitlab:
        write_output(emit_gitlab(msgs), output_gitlab)

def main():
    parser = argparse.ArgumentParser(description='Generate HTML, JSON, or text output from PC-lint Plus XML reports (XML reports are produced by running PC-lint Plus with env-xml.lnt)')
    parser.add_argument('--input-xml', action='store', help='XML input filename', required=True)
    parser.add_argument('--output-text', action='store', help='Text output filename', default = None, required=False)
    parser.add_argument('--output-html', action='store', help='HTML output filename', default = None, required=False)
    parser.add_argument('--output-json', action='store', help='JSON output filename', default = None, required=False)
    parser.add_argument('--output-gitlab', action='store', help='GitLab output filename', default = None, required=False)
    
    args = parser.parse_args()

    if not (args.output_text or args.output_html or args.output_json or args.output_gitlab):
        parser.error("please specify one or more outputs using the '--output-<FORMAT>=<FILENAME>' options")
        
    generate_reports(args.input_xml, args.output_text, args.output_html, args.output_json, args.output_gitlab)

if __name__ == "__main__":
    main()
