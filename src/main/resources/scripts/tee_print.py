#tee_print.py
from __future__ import print_function
import io, sys, locale
from vcast_utils import getVectorCASTEncoding

class TeePrint(object):
    def __init__(self, filename="command.log", verbose=False):
        self.verbose = verbose
        lang, encFmt = getVectorCASTEncoding()
        try:
            # Python 3
            self.logfile = open(filename, 'a', encoding=encFmt, errors="replace")
        except TypeError:
            # Python 2 fallback
            self.logfile = io.open(filename, 'a', encoding=encFmt, errors="replace")

    def __enter__(self):
        return self

    def __exit__(self, exct_type, exce_value, traceback):
        try:
            self.logfile.close()
        except Exception:
            pass

    def teePrint(self, msg):
        # Console
        try:
            print(msg)
        except Exception:
            try:
                if isinstance(msg, unicode):  # Python 2
                    sys.stdout.write(msg.encode(sys.stdout.encoding or "utf-8", "replace") + "\n")
                else:
                    sys.stdout.write(str(msg) + "\n")
            except Exception:
                pass

        # File
        try:
            self.logfile.write(msg + "\n")
        except Exception:
            if isinstance(msg, bytes):
                self.logfile.write(msg.decode(self.logfile.encoding or "utf-8", "replace") + "\n")
            else:
                self.logfile.write(str(msg) + "\n")
