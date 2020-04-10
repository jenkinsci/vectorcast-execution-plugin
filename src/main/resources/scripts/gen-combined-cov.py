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

from __future__ import print_function
from __future__ import absolute_import

import os
import sys

# adding path
jenkinsScriptHome = os.getenv("WORKSPACE") + os.sep + "vc_scripts"
python_path_updates = jenkinsScriptHome
sys.path.append(python_path_updates)
# needed because vc18 vpython does not have bs4 package
if sys.version_info[0] < 3:
    python_path_updates += os.sep + 'vpython-addons'
    sys.path.append(python_path_updates)

import tcmr2csv

verbose = True

def generate_from_html():
    tcmr2csv.useLocalCsv = True
    print("Generating Combined Coverage from " + sys.argv[1])
    tcmr2csv.runCombinedCov(sys.argv[1])

def generate_with_api():
    from generate_xml import GenerateManageXml
    print("Use Data API to generate combined coverage XML file...")
    xml_coverage_name = "xml_data/coverage_results_top-level.xml"
    manage_path = sys.argv[2]
    if os.path.isdir(manage_path):
        manage_path = manage_path + ".vcm"
    xml_file = GenerateManageXml(
                           xml_coverage_name,
                           verbose, 
                           manage_path)
    if verbose:
        print("  Generate Jenkins coverage report: {}".format(xml_coverage_name))
    xml_file.generate_cover()


# Generate the combined coverage (for the complete manage project)
# Prior to VC19 ?, use the Aggregate coverage metrics report from Manage
# From VC19 ?, use the Manage Data API to generate the XML file directly
using_api = False
try:
    data = open(sys.argv[1],"r").read()
    if "<!-- VectorCAST Report header -->" in data:
        # Test to see if this version of VectorCAST has the required API
        # ... It is loaded as a result of this import
        from generate_xml import GenerateManageXml
        # Using new Manage report, use Data API to generate XML
        if len(sys.argv) <= 2:
            print("")
            print("*******************************************************")
            print("*** Error - Jenkins job needs to be re-generated. Second argument missing from call to gen-combined-cov.py")
            print("*** - no coverage_results_top-level.xml generated")
            print("*******************************************************")
            print("")
            sys.exit(1)
        else:
            using_api = True
except:
    using_api = False

if using_api:
    generate_with_api()
else:
    generate_from_html()
