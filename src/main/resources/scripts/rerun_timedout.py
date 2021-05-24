#rerun.py
import sys

import os
if os.name == 'nt':
    esc_quote = "\"\""
else:
    esc_quote = "\\\""

log = sys.argv[1]
if len(sys.argv) == 2:
   idx = 3
else:
   idx = 2

    
lines = open(log, "r").readlines()


tc_name = None
tc_unit = None
tc_subp = None

for line in lines:
    if "Running all" in line:
        unit_subp = line.split()[idx]
        if "<<INIT>>" in unit_subp:
            tc_unit = esc_quote + "<<INIT>>" + esc_quote 
            tc_subp = esc_quote + "<<INIT>>" + esc_quote 
        elif "<<COMPOUND>>" in unit_subp:
            tc_unit = esc_quote + "<<COMPOUND>>" + esc_quote 
            tc_subp = esc_quote + "<<COMPOUND>>" + esc_quote 
        else:
            tc_unit, tc_subp = unit_subp.split(".")
    if "Running: " in line:
        tc_name = esc_quote + line.split(": ")[-1].strip() + esc_quote
    if "Timed out: The process was terminated because the TEST_CASE_TIMEOUT value was exceeded" in line:
        if tc_name and tc_unit and tc_subp:
            print ("-u " + tc_unit + " -s " + tc_subp + " -t " + tc_name + " execute run")

