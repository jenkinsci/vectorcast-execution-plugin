import tarfile
import glob
import argparse
import os

archive_name = "reports_archive.tar"

def extract(verbose = False):

    if os.path.exists(archive_name):
        with tarfile.open(archive_name, mode='r') as tf:
            new_reports = glob.glob("management/*.html")+glob.glob("xml_data/*.xml")      
            for idx in range(len(new_reports)):
                new_reports[idx] = new_reports[idx].replace("\\","/")
                
            for f in tf.getmembers():
                if f.name not in new_reports:
                    if verbose:
                        print("extracting old report " + f.name)
                    tf.extract(f)
                        
def archive(verbose = False):
    if os.path.exists(archive_name):
        os.remove(archive_name)
        
    with tarfile.open(archive_name, mode='w') as tf:
        for f in glob.glob("management/*.html")+glob.glob("xml_data/*.xml"):
            if verbose:
                print ("archiving " + f)
            tf.add(f)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('-a', '--archive',   help='Archive reports', action="store_true", default = False)
    parser.add_argument('-e', '--extract',   help='Extract reports', action="store_true", default = False)
    parser.add_argument('-v', '--verbose',   help='Verbose output' , action="store_true", default = False)
    
    args = parser.parse_args()

    if args.archive:
        archive(args.verbose)
        
    if args.extract:
        extract(args.verbose)

