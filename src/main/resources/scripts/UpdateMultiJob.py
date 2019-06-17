import argparse
import json
import sys
import os
# adding path
jenkinsScriptHome = os.getenv("WORKSPACE") + os.sep + "vc_scripts"
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)
python_path_updates += os.sep + "vpython-addons"
sys.path.append(python_path_updates)
import requests

class VcJob:

    def __init__(self, base_url, username, password, verbose):
        self.jenkins_url = base_url
        self.vc_url = base_url + '/VectorCAST'
        self.update_job = self.vc_url + '/job-update/updateFromSaved'
        self.user = username
        self.password = password
        self.verbose = verbose

        url = self.jenkins_url + 'crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)'
        if username:
            crumb = requests.get(url, auth=(username, password))
            if crumb.status_code == 200:
                self.crumb_headers = dict()
                self.crumb_headers[crumb.text.split(":")[0]] = crumb.text.split(":")[1]
                if self.verbose:
                    print "Got crumb: %s" % crumb.text
            else:
                print "Failed to get crumb: %s" % crumb.text
                sys.exit(-1)

    def update_multi(self, proj_name, proj_file):
        '''Update a multi-job for the given project name and project file

        Args:
            project_name - name of manage project
            project_file - path to manage project file
        '''
        payload = {'manageProjectName' : proj_file}
        files = {'manageProject' : open(proj_file, 'rb')}
        # Jenkins form submission requires data (or payload) to be part of
        # form element 'json'
        jsondata = {'json': json.dumps(payload)}
        if self.verbose:
            print "Using url '%s'" % self.update_job
            print "Update job '%s'" % proj_name
        if self.user:
            rslt = requests.post(self.update_job,
                                 data=jsondata,
                                 files=files,
                                 auth=(self.user, self.password),
                                 headers=self.crumb_headers)
        else:
            rslt = requests.post(self.update_job,
                                 data=jsondata,
                                 files=files)
        if rslt.status_code == 200:
            if self.verbose:
                print "Update request completed"
        else:
            print "Failed to update job: %s" % rslt.status_code
            print rslt.text.encode('utf-8', 'ignore').decode('utf-8')
            sys.exit(-1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    parser.add_argument('--projname', help='Manage Project Name (no path or extension)', required=True)
    parser.add_argument('--projfile', help='Manage Project File (includes path)', required=True)
    parser.add_argument('--url',   help='Jenkins URL')
    parser.add_argument('--user',   help='Jenkins Username')
    parser.add_argument('--password',   help='Jenkins Password')
    parser.add_argument('--verbose',   help='Verbose output', action="store_true")

    args = parser.parse_args()

    if args.verbose:
        print args
        verbose = True
        
    if args.url is None:
        args.url = "http://localhost:8080"

    job = VcJob(args.url, args.user, args.password, args.verbose)
    job.update_multi(args.projname, args.projfile)


