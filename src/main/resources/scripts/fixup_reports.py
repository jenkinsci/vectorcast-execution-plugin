#
# The MIT License
#
# Copyright 2020 Vector Software, East Greenwich, Rhode Island USA
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

from __future__ import division
from __future__ import print_function

import sys, os, locale

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

import tee_print
from vcast_utils import getVectorCASTEncoding
    
def fixup_2020_soup(main_soup):

    # For Jenkins, we don't need to 
    #   - display the contents-block (toc)
    #   - title-bar
    #   - remove the space for toc from the page
    #   - adjust the <th> tags to use style vs class
    #   - adjust the <td> tags to use style vs class
    
    for divClassTypes in ['contents-block', 'return-to-top']:
        for div in main_soup.find_all("div", {'class':divClassTypes}): 
            div.decompose()
        
    for divIdTypes in ['title-bar', 'report-title', 'return-to-top']:
        for div in main_soup.find_all("div", {'id':divIdTypes}): 
            div.decompose()
        
    blocks = main_soup.find_all("div", class_="report-block")

    config_blocks = [div for div in blocks if div.find("a", id="ConfigurationData")]

    for div in config_blocks:
        div.decompose()
        
    for title in main_soup.find_all("title"): 
        title.decompose()
        
    #<div class="report-body no-toc" id="main-scroller">
    div = main_soup.find("div", {'class':'report-body'})    
    try:
        div['class']="report-body no-toc"
    except:
        pass
        
    for th in main_soup.find_all("th",): 
        th['style'] = "border:1px solid #e5e5e5;text-align:left;padding:0.25em;padding-right:1em;"
                    
    # replace class with style because Jenkins won't be able to use the .css in the build summary area
    class2style = {'bold-text' : 'font-weight: bold;',
                   'col_unit': 'word-break:break-all;width:30%;',
                   'col_subprogram': 'word-break:break-all;width:30%;',
                   'col_complexity': 'white-space:nowrap;',
                   'col_metric': 'white-space:nowrap;',
                   'mcdc-all-pairs': 'white-space:nowrap;',
                   'i0' : 'padding-left:0.25em;min-width:11em',
                   'i1' : 'padding-left: 1.25em;min-width: 11em;',
                   'i2' : 'padding-left: 2.25em;',
                   'i3' : 'padding-left: 3.25em;',
                   'i4' : 'padding-left: 4.25em;',
                   'i5' : 'padding-left: 5.25em;',
                   'success' : 'background-color:#c8f0c8;',
                   'warning' : 'background-color:#f5f5c8;',
                   'danger'  : 'background-color:#facaca;'}

    for td in main_soup.find_all("td"):
        style = 'border:1px solid #e5e5e5;'
        try:
            for item in td['class']:
                try:
                    style += class2style[item]
                except:
                    with tee_print.TeePrint() as teePrint:
                        teePrint.teePrint ("unhandled class " + item)
        except:
            pass

        try:
            del(td['class'])
            td['style'] = style
        except:
            pass  

   # for cell in main_soup.find_all(["th", "td"]):
        # style = cell.get("style")
        # if style and "border-bottom:1px" in style:
            # cell["style"] = style.replace("border-bottom:", "border:")
 
    return main_soup

def fixup_2020_reports(report_name):

    encFmt = getVectorCASTEncoding()

    with open(report_name, "rb") as fd:
        raw = fd.read().decode(encFmt, "replace")

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
            main_soup = BeautifulSoup(
                raw.encode(encFmt, "replace")
            )
        
    main_soup = fixup_2020_soup(main_soup)
    
    with open(report_name, "wb") as fd:
        fd.write(main_soup.prettify(formatter="html").encode(encFmt, "replace"))

    
if __name__ == '__main__':

    report_name = sys.argv[1]
    fixup_2020_reports(report_name)
    
