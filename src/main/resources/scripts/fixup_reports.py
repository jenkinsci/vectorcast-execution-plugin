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

import sys
from bs4 import BeautifulSoup
from io import open

    
def fixup_2020_soup(main_soup):

    # For Jenkins, we don't need to 
    #   - display the contents-block (toc)
    #   - title-bar
    #   - remove the space for toc from the page
    #   - adjust the <th> tags to use style vs class
    #   - adjust the <td> tags to use style vs class
    
    for div in main_soup.find_all("div", {'class':'contents-block'}): 
        div.decompose()
        
    for div in main_soup.find_all("div", {'id':'title-bar'}): 
        div.decompose()
    
    #<div class="report-body no-toc" id="main-scroller">
    div = main_soup.find("div", {'class':'report-body'})    
    div['class']="report-body no-toc"
    
    for th in main_soup.find_all("th",): 
        th['style'] = "text-align:left;padding:0.25em;padding-right:1em;border-bottom:1px solid #e5e5e5;"
        
    for td in main_soup.find_all("td", {'class':'success'}): 
        del(td['class'])
        td['style'] = "background-color:#c8f0c8;border-bottom:1px solid #e5e5e5;"
        
    for td in main_soup.find_all("td", {'class':'danger'}): 
        del(td['class'])
        td['style'] = "background-color:#facaca;border-bottom: 1px solid #e5e5e5;"

    for td in main_soup.find_all("td", {'class':'warning'}): 
        del(td['class'])
        td['style'] = "background-color:#f5f5c8;border-bottom: 1px solid #e5e5e5;"

        
    for td in main_soup.find_all("td", {'class':'bold-text i1'}): 
        del(td['class'])
        td['style'] = "font-weight: bold;padding-left: 1.25em;min-width: 11em;border-bottom:1px solid #e5e5e5;"
        
    for td in main_soup.find_all("td", {'class':'bold-text i2'}): 
        del(td['class'])
        td['style'] = "font-weight: bold;padding-left: 2.25em;border-bottom:1px solid #e5e5e5;"
        
    for td in main_soup.find_all("td", {'class':'bold-text i3'}): 
        del(td['class'])
        td['style'] = "font-weight: bold;padding-left: 3.25em;border-bottom:1px solid #e5e5e5;"
        
    for td in main_soup.find_all("td", {'class':'i4'}): 
        del(td['class'])
        td['style'] = "font-weight: bold;padding-left: 4.25em;border-bottom:1px solid #e5e5e5;"
        
    for td in main_soup.find_all("td", {'class':'i5'}): 
        del(td['class'])
        td['style'] = "font-weight: bold;padding-left: 5.25em;border-bottom:1px solid #e5e5e5;"
    for td in main_soup.find_all("td", {'class':'danger'}): 
        del(td['class'])
        td['style'] = "background-color:#facaca;"

    for td in main_soup.find_all("td", {'class':'warning'}): 
        del(td['class'])
        td['style'] = "background-color:#f5f5c8;"
        
    return main_soup

def fixup_2020_reports(report_name):
    main_soup = BeautifulSoup(open(report_name),features="lxml")
    main_soup = fixup_2020_soup(main_soup)
    f = open(report_name,"w", encoding="utf-8")
    f.write(main_soup.prettify(formatter="html"))
    f.close()
        
if __name__ == '__main__':
    report_name = sys.argv[1]
    fixup_2020_reports(report_name)
    