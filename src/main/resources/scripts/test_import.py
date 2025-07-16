import os, sys

os.environ['JENKINS_URL'] = 'http://localhost:8080/'
os.environ['USERNAME'] = 'tms'
os.environ['PASSWORD'] = 'schneider'

if sys.version_info[0] < 3:
    python_path_updates = os.path.join(os.environ['VECTORCAST_DIR'], "DATA", "python")
    sys.path.append(python_path_updates)



try:
    import archive_extract_reports
    import crumbDiag
    import fixup_reports
    import full_report_no_toc
    import parallel_full_reports
    import parse_console_for_cbt
    import parse_traceback
    import tcmr2csv
    import vcastcsv2jenkins
except ModuleNotFoundError as e:
    pass
    
    
try:
    import cobertura
    import copy_build_dir
    import create_index_html
    import extract_build_dir
    try:
        import generate_results 
    except:    
        try:
            import importlib
            generate_results = importlib.import_module("generate-results")
        except:
            vc_script = os.path.join(os.environ['WORKSPACE'], "vc_scripts", "generate-results.py")
            import imp
            generate_results = imp.load_source("generate_results", vc_script)
    try:
        import parallel_build_execute
    except:
        import prevcast_parallel_build_execute as parallel_build_execute
            
    import generate_lcov
    import generate_pclp_reports
    import generate_qa_results_xml
    import generate_sonarqube_pclp_reports
    import generate_sonarqube_testresults
    import generate_xml
    import getjobs
    import incremental_build_report_aggregator
    import managewait
    import merge_vcr
    import patch_rgw_directory
    import safe_open
    import tee_print
    import vcast_exec
    import vcast_utils
except Exception as e:
    import traceback
    traceback.print_exc()