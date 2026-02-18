import os
import re
import sys
from vcast_utils import getVectorCASTEncoding

VC_failurePhrases = [
    "No valid edition(s) available",
    "py did not execute correctly",
    "Traceback (most recent call last)",
    "Failed to acquire lock on environment",
    "Environment Creation Failed",
    "Error with Test Case Management Report",
    "FLEXlm Error",
    "Unable to obtain license",
    "INCR_BUILD_FAILED",
    "Environment was not successfully built",
    "NOT_LINKED",
    "Preprocess Failed",
    "Abnormal Termination on Environment",
    "not recognized as an internal or external command",
    "Another Workspace with this path already exists",
    "Destination directory or database is not writable",
    "Could not acquire a read lock on the project's vcm file",
    "No environments found in ",
    ".vcm is invalid",
    "Invalid Workspace.",
    "not being accessed by another process",
    "not permitted in continuous integration mode",
    "has been opened by a newer version of VectorCAST",
    "Environment built but not Compiled",
    "ERROR: Compile failed for unit"
]

VC_unstablePhrases = [
    "Dropping probe point",
    "Value Line Error - Command Ignored",
    "INFO: Problem parsing test results file",
    "INFO: File System Error",
    "ERROR: Error accessing DataAPI",
    "ERROR: Undefined Error",
    "Unapplied Test Data",
]

def _compile_phrase_regex(phrases):
    """Compile literal phrases into one regex (fast, readable)."""
    phrases = [p for p in phrases if p]
    if not phrases:
        return None
    # longest first is optional, but can help when phrases overlap
    phrases.sort(key=len, reverse=True)
    return re.compile("|".join(re.escape(p) for p in phrases))

FAIL_RX = _compile_phrase_regex(VC_failurePhrases)
UNSTABLE_RX = _compile_phrase_regex(VC_unstablePhrases)

def check_build_log(log_name: str) -> int:
    """
    Returns:
      0 = ok
      1 = unstable phrase found
      2 = failure phrase found
     -1 = log missing/unreadable
    """
    if not os.path.exists(log_name):
        print(f"Build log named {log_name} does not exist")
        return -1

    enc = getVectorCASTEncoding()

    found_fail = set()
    found_unstable = set()

    with open(log_name, "rb") as fd:
        for raw in fd:
            line = raw.decode(enc, "replace")

            m = FAIL_RX.search(line) if FAIL_RX else None
            if m:
                found_fail.add(m.group(0))

            m = UNSTABLE_RX.search(line) if UNSTABLE_RX else None
            if m:
                found_unstable.add(m.group(0))

    if found_fail:
        print("FAILURE phrases found:")
        for s in sorted(found_fail):
            print(f"  - {s}")
        return 2

    if found_unstable:
        print("UNSTABLE phrases found:")
        for s in sorted(found_unstable):
            print(f"  - {s}")
        return 1

    print("No failure/unstable phrases found.")
    return 0


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser()
    parser.add_argument("buildlog", help="Build log name")
    args = parser.parse_args()

    sys.exit(check_build_log(args.buildlog))
