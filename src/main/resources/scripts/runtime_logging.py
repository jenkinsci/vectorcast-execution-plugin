"""Console and command.log output shared by VectorCAST runtime scripts."""

import locale
import logging
import sys

_LOGGER = logging.Logger("vectorcast.execution.scripts", logging.INFO)

def get_logger():
    """Log each message to Jenkins stdout and the per-command log."""
    logger = _LOGGER
    if not logger.handlers:
        formatter = logging.Formatter("%(message)s")
        console = logging.StreamHandler(sys.stdout)
        console.setFormatter(formatter)
        output = logging.FileHandler(
            "command.log", mode="a",
            encoding=locale.getpreferredencoding(False), errors="replace")
        output.setFormatter(formatter)
        logger.addHandler(console)
        logger.addHandler(output)
    return logger


def log_exception(compiler="Compiler", testsuite="TestSuite",
                  env="Environment", build_dir="Directory",
                  error_str="   *ERROR: Jenkins integration error"):
    """Record the active exception with its build context and traceback."""
    traceback = sys.exc_info()[2]
    data_api_error = False
    while traceback is not None:
        module = traceback.tb_frame.f_globals.get("__name__", "")
        if module.startswith("vector.apps.DataAPI"):
            data_api_error = True
            break
        traceback = traceback.tb_next

    if data_api_error:
        message = ("   *ERROR: Error accessing DataAPI for "
                   + compiler + "/" + testsuite + "/" + env
                   + " in directory " + build_dir
                   + ". Check console for environment build/execution errors")
    else:
        message = (error_str
                   + ". Check console for environment build/execution errors")

    get_logger().exception(message)
