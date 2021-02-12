#tee_print.py
from __future__ import print_function

class TeePrint(object):
    def __init__(self, filename = "command.log", verbose = False):
        self.verbose = verbose
        self.logfile = open(filename, 'a')

    def __enter__(self):
        return self

    def __exit__(self, exct_type, exce_value, traceback):
        try:
            self.logfile.close()
        except:
            pass

    def teePrint(self, str):
        print (str)
        self.logfile.write(str + "\n")
        

if __name__ == '__main__':

    with TeePrint() as teePrint:
        teePrint.teePrint("Hello world")
        teePrint.teePrint("Hello world")
        teePrint.teePrint("Hello world")
