# This script defines the FileHooks python class, which can be modified
# to implement a custom file hooks scheme.
#
# An instance of the class will generally be kept in memory for a full
# lock/unlock cycle for each file locked, so you may utilize class
# member variables with the assumption they will be stored until
# unlock() is called.


import os
import sys
import stat

def GetLockObj(*args, **keywords):
    return FileHooks(*args, **keywords)

class FileHooks(object):
    def __init__(self, *args, **keywords):
        keys = keywords.keys()
        keys.sort()
        if 'working_dir' in keys:
            self.working_dir = keywords['working_dir']

    def preFileWrite(self, filename):
        ''' Called before writing to a file.'''
        # This function must return a tuple containing a success boolean and
        # the error text in case of a failure.
        print "Calling preFileWrite hook on %s\n" % filename

        # Do something like this:
        # try:
        #     if os.path.isabs(filename):
        #         os.chmod('%s' % filename, stat.S_IWRITE | stat.S_IREAD)
        #     else:
        #         os.chmod('%s/%s' % (self.working_dir, filename), stat.S_IWRITE | stat.S_IREAD)
        # except OSError as e:
        #     return (False, e.strerror)

        return (True, "")

    def postFileWrite(self, files):
        ''' Called after writing to a file.'''
        # This function must return a tuple containing a success boolean and
        # the error text in case of a failure.
        for filename in files:
            print "Calling postFileWrite hook on %s\n" % filename

            # Do something like this:
            # try:
            #     if os.path.isabs(filename):
            #         os.chmod('%s' % filename, stat.S_IREAD)
            #     else:
            #         os.chmod('%s/%s' % (self.working_dir, filename), stat.S_IREAD)
            # except OSError as e:
            #     return (False, e.strerror)

        return (True, "")

    def directorySeparator(self):
        ''' Returns the directory separator preferred by this system. '''
        if sys.platform == "win32":
            return '\\'
        else:
            return '/'

    def abortProcess(self):
        pass
