#tee_print.py
from __future__ import print_function

class TeePrint(object):
    def __init__(self, filename = "command.log", verbose = False):
        self.verbose = verbose
        self.logfile = open(filename, 'w')

    def __del__(self):
        try:
            self.logfile.close()
        except:
            pass
    def teePrint(self, str):
        print (str)
        self.logfile.write(str + "\n")
        

if __name__ == '__main__':
    
    teePrint = tee_print.TeePrint()
    
    teePrint.teePrint("Hello world")
    teePrint.teePrint("Hello world")
    teePrint.teePrint("Hello world")
    