import requests
import xml.etree.ElementTree as ET

import os, sys
import json

from vcast_utils import getVectorCASTEncoding

from pprint import pprint

PASS = "✅"
FAIL = "❌"
PARTIAL = "🟡"

# Parse Cobertura XML
def parse_cobertura(xml_path, send_all_coverage):
    tree = ET.parse(xml_path)
    root = tree.getroot()
    annotations = []
    for cls in root.findall(".//class"):
        file_path = cls.attrib['filename']
        for line in cls.findall("lines/line"):
            num = int(line.attrib['number'])
            hits = int(line.attrib['hits'])
            branch = line.attrib.get('branch', 'false')
            condition_coverage  = line.attrib.get('condition-coverage', '')
            functioncall_coverage = line.attrib.get('functioncall-coverage', '')
            mcdcpair_coverage = line.attrib.get('mcdcpair-coverage', '')

            summary = ""
            publishAnnotation = True #send_all_coverage

            if hits == 0:
                summary = FAIL + " No coverage on line."
                publishAnnotation = True
                
            else:
                summary = PASS + " ST" 
                if branch == 'true':
                    if condition_coverage.startswith("100.0%"):
                        summary += " | {} BR: {}".format (PASS,condition_coverage)
                    elif condition_coverage.startswith("0.0%"):
                        summary += " | {} BR: {}".format (FAIL,condition_coverage)
                        publishAnnotation = True
                    else:
                        summary += " | {} BR: {}".format (PARTIAL,condition_coverage)
                        publishAnnotation = True

                if functioncall_coverage.startswith("100.0%"):
                    summary += " | {} FC".format (PASS)
                elif functioncall_coverage != '':
                    summary += " | {} FC".format (FAIL)
                    publishAnnotation = True

                if mcdcpair_coverage.startswith("100.0%"):
                    summary += " | {} MCDC: {}".format (PASS, mcdcpair_coverage)
                elif mcdcpair_coverage.startswith("0.0%"):
                    summary += " | {} MCDC: {}".format (FAIL, mcdcpair_coverage)
                    publishAnnotation = True
                elif mcdcpair_coverage != '':
                    summary += " | {} MCDC: {}".format (PARTIAL, mcdcpair_coverage)
                    publishAnnotation = True

            if True: #publishAnnotation:
                # annotations.append({
                    # "path": file_path,
                    # "line": num,
                    # "external_id": "{}#{}".format(file_path,num),
                    # 'message' : summary
                    # }
                # )
                
                annotations.append({
                    "title": "Coverage",
                    "annotation_type": "COVERAGE",
                    "summary": summary,
                    "severity": "LOW",
                    "path": file_path,
                    "line": num,
                    "external_id": "{}#{}".format(file_path,num)
                    }
                )
    return annotations

def get_summary_string(type_str, rate):
    
    if rate == -1:
        return None
    
    return {"title" : type_str, "type" : "PERCENTAGE", "value": round(rate * 100.0,  2)}
        
    
def get_summary_resuts(xml_path, minimum_passing_coverage, verbose):
    
    tree = ET.parse(xml_path)
    root = tree.getroot()
    line = root
        
    line_rate                  = float(line.attrib.get('line-rate', -1))
    statement_rate             = float(line.attrib.get('statement-rate', -1))
    branch_rate                = float(line.attrib.get('branch-rate', -1))
    mcdcpair_coverage_rate     = float(line.attrib.get('mcdcpair-coverage-rate',-1))
    functioncall_coverage_rate = float(line.attrib.get('functioncall-coverage-rate', -1))
    function_coverage_rate     = float(line.attrib.get('function-coverage', -1))
    timestamp                  = line.attrib['timestamp']
    version                    = line.attrib['version'].rsplit(" ", 1)[0]
    
    summary = ""
    
    data = []
    
    if statement_rate == -1:
        summary = "No coverage available"
        overall_coverage = "FAIL"
    else:
        if statement_rate >= minimum_passing_coverage:
            overall_coverage = "PASS"
        else:
            overall_coverage = "FAIL"
        
        # If you ever have more coverage types, you can refactor like this:

        metrics = [
            ("Statement",     statement_rate),
            ("Branch",        branch_rate),
            ("MCDC Pair",     mcdcpair_coverage_rate),
            ("Function Call", functioncall_coverage_rate),
            ("Function ",     function_coverage_rate),
        ]

        data = [
            v
            for _, v in ((n, get_summary_string(n, rate)) for n, rate in metrics)
            if v is not None
        ]
        
    return data, timestamp, version, overall_coverage
    
def create_code_coverage_report_in_bitbucket(filename, workspace, repo_slug, commit_hash, email, token, minimum_passing_coverage, verbose):
    
    print("Creating coverage report for commit {}".format(commit_hash))
    
    # CONFIGURATION
    report_id = "coverage-report"

    data, timestamp, version, overall_coverage = get_summary_resuts(filename, minimum_passing_coverage, verbose)
    
    url = "https://api.bitbucket.org/2.0/repositories/{}/{}/commit/{}/reports/{}".format(workspace, repo_slug, commit_hash, report_id)

    report_payload = {
        "title": "Coverage Report",
        "details": "VectorCAST Code Coverage Summary.",
        "report_type": "COVERAGE",
        "reporter": version,
        "data": data
    }
    
    if verbose:
        print(json.dumps(report_payload, indent=2))
    
    resp = requests.put(
        url,
        auth=(email, token),
        json=report_payload,
        headers = {"Accept": "application/json", "Content-Type": "application/json"},
        timeout=30
    )

    if resp.status_code == 200:
        print("Reported Created")
    else:
        print("Reported Creation - FAILED")
        print("Report creation status:", resp.status_code)
        print("Response:", resp.text)


# Send annotations in batches of 100
def send_code_coverage_annoations(annotations, workspace, repo_slug, commit_hash, email, token, verbose):
    
    print("Sending coverage annotations")

    # CONFIGURATION
    report_id = "coverage-report"

    url = "https://api.bitbucket.org/2.0/repositories/{}/{}/commit/{}/reports/{}/annotations".format(workspace, repo_slug, commit_hash, report_id)

    headers = {"Accept": "application/json", "Content-Type": "application/json"},

    for i in range(0, len(annotations), 100):
        batch = annotations[i:i+100]     
                                          
        if verbose:  
            print(json.dumps(annotations[1:10]))

        resp = requests.post(
            url, 
            auth=(email, token), 
            json=batch, 
            headers= {"Accept": "application/json", "Content-Type": "application/json"}
        )
        
        if resp.status_code != 200 or verbose:
            print(f"Batch {i//100+1} response:", resp.status_code, resp.text)

    print("Complete")


def run(filename, minimum_passing_coverage, send_all_coverage, verbose):

    workspace   = os.environ['BITBUCKET_WORKSPACE']
    repo_slug   = os.environ['BITBUCKET_REPO_SLUG']
    commit_hash = os.environ['BITBUCKET_COMMIT']
    bitbucket_api_token = os.environ['BITBUCKET_API_TOKEN']
    bitbucket_email = os.environ['BITBUCKET_EMAIL']

    create_code_coverage_report_in_bitbucket(
        filename, 
        workspace, 
        repo_slug, 
        commit_hash, 
        bitbucket_email, 
        bitbucket_api_token, 
        minimum_passing_coverage,
        verbose
    )
    
    annotations = parse_cobertura(filename, send_all_coverage)
    
    encFmt = getVectorCASTEncoding()
    

    with open("coverage_results.json", "wb") as fd:
        fd.write(json.dumps(annotations, indent=2).encode(encFmt,'replace'))
    
    send_code_coverage_annoations(
        annotations, 
        workspace, 
        repo_slug, 
        commit_hash, 
        bitbucket_email, 
        bitbucket_api_token, 
        verbose
    )
    
if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Parse a Cobertura XML report and check against a minimum coverage threshold."
    )
    
    parser.add_argument(
        "-f", "--filename",
        help="Path to the Cobertura XML file to parse",
        default="cobertura.xml"
    )

    parser.add_argument(
        "-a", "--send_all_coverage",
        help="Report all coverage.  Default is partial/fail only",
        action="store_true",
        default=False
    )

    parser.add_argument(
        "--minimum_passing_coverage",
        type=float,
        help="Minimum overall coverage required to pass (default 80 percent)",
        default=80
    )
    
    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Enable verbose output for debugging or detailed reporting"
    )

    args = parser.parse_args()

    run(args.filename, args.minimum_passing_coverage, args.send_all_coverage, args.verbose)

