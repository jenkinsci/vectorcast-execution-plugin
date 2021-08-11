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

from __future__ import division
from __future__ import print_function

from io import open

def get_file_encoding(file, default_encoding = 'utf-8'):
    
    try:
        import chardet
        with open(file, "rb") as fd:
            cur_encoding = chardet.detect(fd.read())["encoding"]
            if cur_encoding == 'GB2312':
                cur_encoding = 'GBK'
    except:
        print ("Problem detecting encoding of " + file + ".  Defaulting to " + default_encoding)
        cur_encoding = default_encoding

    return cur_encoding

if __name__ == '__main__':
    import sys
    print(get_file_encoding(sys.argv[1]))