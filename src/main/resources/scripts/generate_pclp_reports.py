#####################################################################
# PC-lint Plus Report Generator / Output Format Converter
#
# Invoke with no arguments for command-line help screen with
# usage information, option list, and supported output formats.
#
# Last Updated 2023-02-07
#####################################################################

import argparse
try:
    from html import escape
except ImportError:
    # html not standard module in Python 2.
    from cgi import escape
import json
import xml.etree.ElementTree
import os
import pathlib

from pprint import pprint

from vector.apps.DataAPI.vcproject_api import VCProjectApi
from vector.apps.ReportBuilder.custom_report import CustomReport
from vcast_utils import checkVectorCASTVersion
try:
    from safe_open import open
except:
    pass
        
g_msgs = []
g_fullMpName = ""

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
        out += "</th>\n"
    out += "</tr>"
    for item in data_source:
        out += "<tr>"
        row = row_generator(item)
        if 'Total' in row:
            boldStart = "<b>"
            boldEnd   = "</b>"
        else:
            boldStart = ""
            boldEnd   = ""
        for data in row:
            out += "<td>"
            out += boldStart + str(data) + boldEnd
            out += "</td>\n"
        out += "</tr>\n"
    out += "</table>"
    return out

def format_benign_zero(x):
    return str(x) if x != 0 else "<span class=\"zero\">" + str(x) + "</span>"

def generate_details():
    global g_msgs
    msgs = g_msgs
    
    out = ""
    out += build_html_table(
        ['File', 'Line', 'Category', '#', 'Description'],
        msgs,
        lambda msg: [
            "<span class=\"filename\"><a href=\"#" + escape(msg.file).replace("\\","/").replace("/","_").replace(".","_") + "_" + msg.line + "\">" + escape(msg.file) +  "</a></span>",
            msg.line if msg.line != "0" else "",
            msg.category,
            msg.number,
            escape(msg.text)
        ]
    )

    return out

def generate_summaries():
    
    global g_msgs
    msgs = g_msgs
    out = ""

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
            ("<span class=\"filename\"><a href=\"#" + escape(file.filename).replace("\\","/").replace("/","_").replace(".","_") + "\">" + escape(file.filename) + " </a></span>") if file.filename != 'Total' else file.filename,
            format_benign_zero(file.msg_count),
            format_benign_zero(file.error_count),
            format_benign_zero(file.warning_count),
            format_benign_zero(file.info_count),
            format_benign_zero(file.note_count),
            format_benign_zero(file.misra_count)
        ]
    )
    
    return out

def generate_source():
    
    global g_msgs
    msgs = g_msgs
    
    global g_fullMpName
    fullMpName = g_fullMpName
    
    output = "<h4>No Source Infomation avialable</h4>"
    
    file_summaries = summarize_files(msgs)
    if not checkVectorCASTVersion(21, True):
        print("XXX Cannot generate Source Code section of the PC-Line Report report")
        print("XXX The Summary and File Detail sections are present")
        print("XXX If you'd like to see the Source Code section of the PC-Line Report, please upgrade VectorCAST")
        
    filenames = []
    filenames = list(map(lambda file: file.filename, file_summaries.values()))

    filename_dict = {file.filename.replace("\\","/").lower(): file.filename for file in file_summaries.values()}
    
    # Use a lambda inside map to create a dictionary keyed by file and then by line
    messages_by_file_and_line = {}

    # Group by file
    list(map(lambda msg: messages_by_file_and_line.setdefault(msg.file, {})
             .setdefault(msg.line, msg), msgs))    
    
    if fullMpName is None:
        return output
        
    api = VCProjectApi(fullMpName)

    localUnits = api.project.cover_api.SourceFile.all()
    localUnits.sort(key=lambda x: (x.name))

    try:
        basepath = os.environ['WORKSPACE'].replace("\\","/") + "/"
    except:
        basepath = os.getcwd().replace("\\","/") + "/"
        
    output = "" 
    
    listOfContent = []
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
        
        filename_anchor = orig_fname.replace("\\","_").replace("/","_").replace(".","_")
        
        content = { 
            "title": basename,
            "link" : filename_anchor
        }
        
        listOfContent.append(content)

        output += "<h4 id=\"" + filename_anchor + "\">Coverage for " + escape(fname) + "</h4>\n"
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
                    output += "<div id=\"" + anchor + "\" class=\"tooltip\">"
                    output += "<span class=\"na-cvg\">"
                    output += lineno_str_justified + " <span class=\"tooltiptext\"> " + tooltip + "</span>" + esc_line
                    output += "</span>"
                    output += "</div>"
                else:
                    output += "<span class=\"na-cvg\">" + lineno_str_justified + " " +  esc_line + "</span>"
                    
                output += "\n"

            output += "</pre>"
            
    return output, listOfContent

def generate_html_report(mpName: str, input_xml: str, output_html: str) -> None:
        
    global g_fullMpName
    global g_msgs
    g_fullMpName = mpName
    g_msgs = parse_msgs(input_xml)
    
    if output_html is None:
        output_html = "pclp_findings.html"
        
    with VCProjectApi(mpName) as api:
        # Set custom report directory to the where this script was
        # found. Must contain sections/index_section.py
        rep_path = pathlib.Path(__file__).parent.resolve()
        CustomReport.report_from_api(
                api=api,
                title="PC-Lint Plus Results",
                report_type="INDEX_FILE",
                formats=["HTML"],
                output_file=output_html,
                sections=['CUSTOM_HEADER', 'REPORT_TITLE', 'TABLE_OF_CONTENTS','PCLP_SUMMARY_SECTION','PCLP_DETAILS_SECTION','PCLP_SOURCE_SECTION', 'CUSTOM_FOOTER'],
                customization_dir=rep_path)

def has_any_coverage(line):
    
    return (line.metrics.statements + 
        line.metrics.branches + 
        line.metrics.mcdc_branches + 
        line.metrics.mcdc_pairs + 
        line.metrics.functions +
        line.metrics.function_calls)
    
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
    out += generate_summaries(msgs)
    out += generate_details(msgs)

    out += generate_source(full_mp_name, file_summaries, msgs, output_filename)    
    
    out += "<br>"
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
        generate_html_report(msgs, output_html)
        #write_output(emit_html(msgs), output_html)
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
    parser.add_argument('--vc-project', action='store', help='VectorCAST Project Name.  Used for source view', dest="full_mp_name", default = None, required=False)
    parser.add_argument('-g', '--gen-lint-xml-cmd', action='store', help='Command to genreate lint XML files', dest="gen_lint_xml_cmd", default = None, required=False)
    
    args = parser.parse_args()
    if args.gen_lint_xml_cmd is not None:
        subprocess.run(args.gen_lint_xml_cmd)

    if not (args.output_text or args.output_html or args.output_json or args.output_gitlab):
        parser.error("please specify one or more outputs using the '--output-<FORMAT>=<FILENAME>' options")
        
    generate_reports(args.input_xml, args.output_text, args.output_html, args.output_json, args.output_gitlab)

    ## if opened from VectorCAST GUI...
    if (args.full_mp_name is not None) and (args.output_html is not None) and (os.getenv('VCAST_PROG_STARTED_FROM_GUI') == "true"):
        from vector.lib.core import VC_Report_Client

        # Open report in VectorCAST GUI
        report_client = VC_Report_Client.ReportClient()
        if report_client.is_connected():
            report_client.open_report(args.output_html, "PC Lint Plus results")
            
if __name__ == "__main__":
    main()
