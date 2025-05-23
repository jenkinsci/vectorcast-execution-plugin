#
# The MIT License
#
# Copyright 2024 Vector Informatik, GmbH.
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
import contextlib
import os

from io import open as _open

def new_get_file_encoding():
    # get the VC langaguge and encoding
    cur_encoding = "utf-8"
    try:
        from vector.apps.DataAPI.configuration import vcastqt_global_options
        lang = vcastqt_global_options.get('Translator','english')
        if lang == "english":
            cur_encoding = "utf-8"
        if lang == "japanese":
            cur_encoding = "shift-jis"
        if lang == "chinese":
            cur_encoding = "GBK"
    except:
        pass
        
    return cur_encoding

@contextlib.contextmanager
def open(file, mode='r', buffering=-1, encoding=None, errors=None, newline=None, closefd=True, opener=None):

    if 'b' in mode:
        try:
            fd = _open(file, mode, buffering, encoding, errors, newline, closefd, opener)
        except:
            fd = _open(file, mode, buffering, encoding, errors, newline, closefd)
    else:
        if os.path.exists(file):
            encoding = new_get_file_encoding()
        else:
            encoding = "utf-8"
        fd = _open(file, mode, buffering, encoding, errors, newline)
    
    try:
        yield fd
    finally:
        fd.close()


# EOF
