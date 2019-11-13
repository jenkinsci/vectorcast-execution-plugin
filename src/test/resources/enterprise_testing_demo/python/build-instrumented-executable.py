import os
import shutil
import subprocess
import sys

import environment

def get_working_directory():
    """This function returns the directory that the make process will be spawned
    in."""
    return environment.sourceDirectory()

class Make:
    '''make utility interface.'''
    def __init__(self):
        self.args = []
        self.args.append(self.__get_executable_name())

    def execute(self):
        self.__log_command()
        self.process = subprocess.Popen(self.args,
                                        stdin=subprocess.PIPE,
                                        stdout=subprocess.PIPE,
                                        stderr=subprocess.STDOUT,
                                        cwd=get_working_directory(),
                                        env=self.__get_process_environment())
        self.__log_output()
        self.process.wait()
        return self.process.returncode

    def __get_executable_name(self):
        return 'make.exe' if os.name == 'nt' else 'make'

    def __log_command(self):
        cmd = 'COMMAND: %s in %s' % (' '.join(self.args),
                                     get_working_directory())
        environment.log(cmd, environment.VerbosityNormal)

    def __get_process_environment(self):
        """Returns the environment that this process will use (the env vars
        that are set). """
        return environment.processEnvironment()

    def __log_output(self):
        for line in iter(self.process.stdout.readline,''):
            if line is not None:
                environment.log(line.rstrip())

def get_cover_directory():
    """This function returns the cover environment's directory in the manage
    build directory."""
    return os.path.join(environment.path(), environment.name())

def get_cover_io_source_file_path():
    cover_io_source_file = 'c_cover_io.cpp'
    return os.path.join(get_cover_directory(), cover_io_source_file)

def get_cover_io_header_file_path():
    cover_io_header_file = 'vcast_c_options.h'
    return os.path.join(get_cover_directory(), cover_io_header_file)

def copy_cover_io_files():
    """This function copies the cover_io files to the working directory."""
    shutil.copy(get_cover_io_source_file_path(), get_working_directory())
    shutil.copy(get_cover_io_header_file_path(), get_working_directory())

def build():
    make = Make()
    make.execute()

def main():
    copy_cover_io_files()
    build()

if __name__ == "__main__":
    main()
