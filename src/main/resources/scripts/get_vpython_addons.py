import sys
def get_vpython_addons():
    return "vpython3-addons" if sys.version_info[0] >= 3 else "vpython-addons"
