import sys
import sqlite3
import os, shutil
import zipfile, glob
from pprint import pprint
import argparse

def mergeNewResultsIntoOrigDb(origVcrFile, newVcrFile, cursor_new, cursor_orig, table_name, del_old_table = False, verbose = False):
    '''
    This function merges the content of a specific table from an old cursor into a new cursor. 
    
    :param cursor_new: [sqlite3.Cursor] the primary cursor
    :param cursor_orig: [sqlite3.Cursor] the secondary cursor
    :param table_name: [str] the name of the table
    :return: None
    '''

    # dynamically determine SQL expression requirements
    column_names = cursor_new.execute(f"PRAGMA table_info({table_name})").fetchall()
    column_names = tuple([x[1] for x in column_names][1:])  # remove the primary keyword
    values_placeholders = ', '.join(['?' for x in column_names])  # format appropriately
    
    # SQL select columns from table
    orig_data = cursor_orig.execute(f"SELECT {', '.join(column_names)} FROM {table_name}").fetchall()
    new_data  = cursor_new.execute(f"SELECT {', '.join(column_names)} FROM {table_name}").fetchall()

    filtered_new_data = []
    rowsToRemove = []
    for new_row in new_data:
        new_testsuite_id = new_row[0]
        new_env_name     = new_row[1]
        for orig_row in orig_data:
            orig_testsuite_id = orig_row[0]
            orig_env_name     = orig_row[1]
            if verbose:
                tf = new_testsuite_id == orig_testsuite_id and orig_env_name == new_env_name
                print (str(new_testsuite_id) + " == " + str(orig_testsuite_id)  + " and " + orig_env_name + " == " + new_env_name + ": " + str(tf))
            if new_testsuite_id == orig_testsuite_id and orig_env_name == new_env_name:
                if verbose:
                    print("   need to replace contents of " + str(orig_testsuite_id) + "/" + orig_env_name + " in orig_dbx ")
                    print(f"   DELETE FROM {table_name} WHERE testsuite_id={new_testsuite_id} and environment={new_env_name}")
                rowsToRemove.append(new_row)
                cursor_orig.execute(f"DELETE FROM {table_name} WHERE testsuite_id={new_testsuite_id} and environment=\"{new_env_name}\"")
                cursor_orig.connection.commit()
                break
                
    cursor_orig.executemany(f"INSERT INTO {table_name} {column_names} VALUES ({values_placeholders})", new_data)
    if (cursor_orig.connection.commit() == None):
        # With Ephemeral RAM connections & testing, deleting the table may be ill-advised
        print(f"Table \"{table_name}\" merged from {origVcrFile} to {newVcrFile}") # Consider logging.info()
        
    return None

def run(origVcrFile, newVcrFile, verbose):

    
    try:
        os.makedirs("newVcr")
        os.makedirs("origVcr")
    except:
        pass
    
    tempNewVcrFile = os.path.join("newVcr",newVcrFile)
    tempOrigVcrFile = os.path.join("origVcr",os.path.basename(origVcrFile))
    
    shutil.copyfile(newVcrFile, tempNewVcrFile)
    shutil.copyfile(origVcrFile, tempOrigVcrFile)
    
    with zipfile.ZipFile(tempNewVcrFile, 'r') as zip_ref:
        zip_ref.extractall("newVcr")
        for file in glob.glob("newVcr/*.*"):
            if file.endswith(".db") and "_cover" not in file:
                newDbName = file

    with zipfile.ZipFile(tempOrigVcrFile, 'r') as zip_ref:
        zip_ref.extractall("origVcr")
        for file in glob.glob("origVcr/*.*"):
            if file.endswith(".db") and "_cover" not in file:
                origDbName = file
                
    os.remove(tempNewVcrFile)
    os.remove(tempOrigVcrFile)
    
    # Get connections to the databases
    new_db = sqlite3.connect(newDbName)
    orig_db = sqlite3.connect(origDbName)

    # Get the cursors of a tables
    new_cursor = new_db.cursor()
    orig_cursor = orig_db.cursor()

    mergeNewResultsIntoOrigDb(origVcrFile, newVcrFile, new_cursor, orig_cursor, "result", False, verbose)
    
    new_db.close()
    orig_db.close()

    os.remove(newVcrFile)
    os.remove(origVcrFile)

    shutil.make_archive(newVcrFile, 'zip', "origVcr")
    shutil.copyfile(newVcrFile+".zip", newVcrFile)
    
    os.remove(newVcrFile+".zip")
    shutil.rmtree("newVcr")
    shutil.rmtree("origVcr")
    
if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument('-o', '--orig', action='store', type=str,  help='Original Result Filename', dest="origVcrFile")
    parser.add_argument('-n', '--new',  action='store', type=str,  help='New Result Filename', dest="newVcrFile")
    parser.add_argument('-v', '--verbose',  action="store_true",  help='Verbose output', dest="verbose", default=False)

    args = parser.parse_args()
    
    if os.path.isfile(args.newVcrFile):
        if os.path.isfile(args.origVcrFile):
            run(args.origVcrFile, args.newVcrFile, args.verbose)
            
    
