import json
import os
import shutil
from vector.lib.core import system

from vector.manage import events
from vector.manage.events import refresh_project_results
from vector.manage.targets import TargetFactory
from vector.manage.targets import EnvironmentVariables
from vector.manage.system_tests_factory import SystemTestsFactory

"""
This script may be used to provide customized setup and teardown processing 
for VectorCAST tests. To use this feature:
  1. Create a class that implements the "setup" and "teardown" methods of the
     Target class below.
  2. Add the Target subclass along with a VectorCAST Project's Compiler Node
     Name as an entry to the compiler_name_to_target_mapping dict.
  3. Reload the Project (the Project must be reloaded whenever this
     script is modified).

VectorCAST will run the provided "setup" method before running tests,
and the "teardown" method after running tests.

If you have multiple test environments under a particular compiler node, the
setup and teardown will only be invoked once per for that group of tests.
"""

class Target(object):
    """
    This is a default class instance that does nothing.
    It is used by the default case in compiler_name_to_target_mapping
    """
    def setup(self):
        pass

    def teardown(self):
        pass
        

class TargetVS2010(Target):
    """
    This derived class handles VisualStudio2010 Compiler nodes.

    Note that this compiler does not need any teardown processing, so we inherit
    the empty teardown provided by our parent class.
    """

    def setup(self):
        """
        We extract the default path and environment variable
        settings from the compiler's .bat setup file. We extract those variables
        from the script and then append them to the current compiler's process
        environment.
        """
        variables = EnvironmentVariables()
        variables.capture_from_script(self.__get_bat_script())

    def __get_bat_script(self):
        # Note: The path to the compiler might be different on your system
        return os.path.normpath(
            r'C:/"Program Files (x86)"/"Microsoft Visual Studio 10.0"/VC/vcvarsall.bat')

class TargetCudaListener(events.Listener):
    def __init__(self):
        super(TargetCudaListener, self).__init__()
        # events.add_listener(self)
        self.is_executing_system = False

    def process_event(self, data):
        event = json.loads(data)
        if self.is_system_exec(event):
            self.is_executing_system = True
        else:
            pass

    def is_system_exec(self, event):
        return 'exec' in event['type']
        
globalTargetCudaListener = TargetCudaListener()

class TargetCuda(object):
    """
    This derived class handles CUDA Compiler nodes.
    """

    def __init__(self):
        pass

    def setup(self):
        shutil.rmtree('.cuda', ignore_errors=True)
        os.mkdir('.cuda')

    def teardown(self):
        shutil.rmtree('.cuda', ignore_errors=True)
        # only do this for 'exec' events
        if globalTargetCudaListener.is_executing_system:
            refresh_project_results()
            globalTargetCudaListener.is_executing_system = False

"""
This dict is a mapping between the Compiler Node Names
in your VectorCAST Project and the Target class derivative
that should be used when a test is run for a child of that node.
Uncomment the TargetVS2010 mapping to use Visual Studio 10. If
there is not explicit match, the 'default' mapping is used.
"""
compiler_name_to_target_mapping = {
    # Insert your own mappings here ...

    # 'Microsoft_VisualC++_2010_C' : TargetVS2010,
    # 'Microsoft_VisualC++_2010_C++' : TargetVS2010,
    'CudaSystemTest': TargetCuda,
    'default': Target
}

# This call creates the association between this script and VectorCAST ...
TargetFactory.register(compiler_name_to_target_mapping)

