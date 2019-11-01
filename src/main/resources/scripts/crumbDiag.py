import requests
import sys
import os

verbose=True
try:
    username=os.environ['USERNAME']
    password=os.environ['PASSWORD']
except:
    print("Crumb Diaganostic requires USERNAME/PASSWORD to be set as environment variables")
    sys.exit(-1)
jenkins_url=os.environ['JENKINS_URL']
url = jenkins_url + 'crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)'
print(url)
if username:
    crumb = requests.get(url, auth=(username, password))
    if crumb.status_code == 200:
        crumb_headers = dict()
        crumb_headers[crumb.text.split(":")[0]] = crumb.text.split(":")[1]
        if verbose:
            print("Got crumb: %s" % crumb.text)
    else:
        print("Failed to get crumb")
        print("\nYou may need to enable \"Prevent Cross Site Request Forgery exploits\" from:")
        print("Manage Jenkins > Configure Global Security > CSRF Protection and select the appropriate Crumb Algorithm")
        print(jenkins_url + "/configureSecurity")
        sys.exit(-1)
