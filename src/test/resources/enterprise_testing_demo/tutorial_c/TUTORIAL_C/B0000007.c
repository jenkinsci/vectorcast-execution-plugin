/***********************************************
 *      VectorCAST Test Harness Component      *
 *     Copyright 2019 Vector Informatik, GmbH.    *
 *              19.sp3 (11/13/19)              *
 ***********************************************/
/*
---------------------------------------------
-- Copyright 2019 Vector Informatik, GmbH. --
---------------------------------------------
*/

/* -----------------------------------------------------------------------------
-- VectorCAST IO File
-- 
-- Inconsistent modifications may cause VectorCAST to fail to work properly
-- 
-- Macro Definitions that control I/O mode
--
--   VCAST_STDIO:              STDIO mode comes in a few flavors.  If you only
--                             set the VCAST_STDIO flag, then initialize_io will
--                             open stdin, read a stream of data from there,
--                             and build a virtual file system in memory for 
--                             all of the data it reads.  All output will be 
--                             sent to stdout.
--
--   --VCAST_STDIO_WITH_FILES  If this flag is set, then things work the same
--                             as described above, except at the stdin is read
--                             from the file: "VCAST_STDIN.DAT", and the stdout
--                             is written to "VCAST_STDOUT.DAT".
--           
--   --VCAST_STDOUT_ONLY       If this flag is set, then the input test data
--                             is compiled in, and located in a global character
--                             array.  In this mode, the virtual file system
--                             is simply created from this char array, rather
--                             than from stdin.  By default, "printf" is used
--                             for output.  To use something other than printf,
--                             set one of the following 4 flags ...
--    
--    ----VCAST_BUFFER_OUTPUT  If this macro is set, then the output will
--                             be stored into the: "vcast_output_buffer" object.
--                             The size of the buffer is controlled by marco"
--                             VCAST_OUTPUT_BUFFER_SIZE.  If you also set:
--                             VCAST_DUMP_BUFFER, then the contents of output
--                             buffer will be dumped at the end of the test.
--                             Buffer is output with explicit call to "printf"
--
--    ----VCAST_OUTPUT_VIA_DEBUGGER
--
--                             When this macro is set, each time a line of 
--                             output is ready, the data will be stored into 
--                             the global object: 'vcastSingleLineOfOutput', and 
--                             the function: vcastHaltDebugger will be called.
--                             To use this technique, set a break point on the 
--                             vcastHaltDebugger function and read this global 
--                             object each time the break is hit.  
--                             Replaces call to "printf"
--
--    ----VCAST_CUSTOM_OUTPUT  When this macro is set, the harness will call the 
--                             function defined by the macro with a char* paramter.
--                             To use this feature, simply add a macro similar to: 
--                             VCAST_CUSTOM_OUTPUT=vcastOutputFunction.  The
--                             following prototype will be automatically inserted:
--                             "void VCAST_CUSTOM_OUTPUT(const char* S);"
--                             And the harness will insert call like this: 
--                             "vcastOutputFunction(s)", to replace the call to "printf"
--
--    ----VCAST_CUSTOM_INPUT   When this macro is set, the harness will call the 
--                             function defined by the mcro and expect a char* back.
--                             To use this feature, simply add a macro similar to: 
--                             VCAST_CUSTOM_INPUT=vcastInputFunction.  The
--                             following prototype will be automatically inserted:
--                             "VCAST_CUSTOM_INPUT (char* S);"
--                             And the harness will insert call like this: 
--                             "vcastInputFunction(s)" to replace the call to "gets"
--
--    ----VCAST_USE_GH_SYSCALL If you are using the Green Hills Compiler, then           
--                             setting this macro will force the harness to 
--                             "print" output data via the: "__ghs_syscall" API.  
--                             Replaces call to "printf"
--                                    
-- File Operation Macros
--                             
--    VCAST_USE_SETBUF                    
--                             When doing output via file operations, this options
--                             provides buffer space for each file, which speeds up
--                             I/O performance.  This is defaulted on for GHS
--
--    VCAST_FILE_INDEX         When using STDIO or STDOUT_ONLY, this the default 
--                               output stream is to output lines with the filename
--                             followed by the data: "ASCIIRES.DAT: hello".
--                             If you set this macro, the lines will be output with
--                             a number in place of the name:  "1: hello", which
--                             saves some bandwidth for slow links
--
--    VCAST_FILE_PREFIX        This option will pre-pend the provided string to all
--                             filenames when the file is opened.  For example, if you
--                             use -DVCAST_FILE_PREFIX="./" then the filename we use
--                             to open a file will be: "./ASCIIRES.DAT".
--                              
-- Other Macros:
--    
--    VCAST_INPUT_DATA_ARRAY_ATTRIBUTE
--         Allows a compiler specific linker attribute to be used for the 
--         inputDataArray object. This is generally used if this data object needs 
--         to be defined in a specific section of memory
--
--    VCAST_READ_LF
--         When we read lines from an input stream, this macro tells us that the 
--         lines have an extra LF character that needs to be stripped
--
-- Compiler Macros
--
--       VAST_TASKING                
--       VCAST_KEIL and VCAST_USE_CHAR_FOR_BOOL
--       VCAST_COSMIC
--       VCAST_KEIL_ARM_STM32_TGT
--       VCAST_PARADIGM
--       VCAST_PARADIGM_SC520
--       VCAST_PIC24_TGT 
--       VCAST_RENESAS_SH_SIM
--       VCAST_TASKING
--       VCAST_GH_INT_178B
--       VCAST_VXWORKS
--

-- Changelog:
--      Removed VCAST_MONITOR stuff, this is really old and not used for c/c++
--      Removed VCAST_FP, a floating point to string routine.
--      For VCAST_GH_INT_178B, got rid of the prototype of sprintf
--      Removed all conditional code for: VCAST_DELPHI_NEC will not support
--      Cleaned up the way the CUSTOM_OUTPUT works so that no file edit is needed
--      Got rid of VCAST_AIM, could not see where that was used in any template
-- 
-------------------------------------------------------------------------------*/



/*-------------------------------------------------------------------------------
To make this module more readable, we have added these macro translations:
   VCAST_NO_STDIN has been replaced by VCAST_STDOUT_ONLY
   VCAST_NO_STDIN implies VCAST_STDIO
-------------------------------------------------------------------------------*/

extern int vcast_exit_flag;
#if defined (VCAST_NO_STDIN) || defined (VCAST_STDOUT_ONLY)

/* Guard Against someone setting stdout without setting stdio */
#ifndef VCAST_STDIO
#define VCAST_STDIO
#endif

#define VCAST_STDOUT_ONLY
#endif

#ifdef VCAST_BUFFER_OUTPUT 

#ifndef VCAST_STDIO
#define VCAST_STDIO
#endif
#endif

/* Legacy Option VCAST_USE_FILES is confusing in this code base */
#ifdef VCAST_USE_FILES
#ifndef VCAST_STDIO_WITH_FILES
#define VCAST_STDIO_WITH_FILES
#endif
#endif


#if !defined (VCAST_STDIO) || (defined (VCAST_STDIO) && defined(VCAST_STDIO_WITH_FILES))
/* Just for readability */
#define VCAST_USING_FILE_OPERATIONS
#endif

/*-----------------------------------------------------------------------------*/


#ifndef VCAST_GH_INT_178B
#include <stdio.h>
#endif

/* Generic Success / Failure Constants */
#define VC_OK   0
#define VC_FAIL -1


/*-------------------------------------------------------------------------------------
-- These are duplicated here (also in S2.h) so that we don't have a #include of S2 here 
------------------------------------------------------------------------------------- */

#ifndef VCAST_FILENAME_LENGTH
#define VCAST_FILENAME_LENGTH 13
#endif

/*-------------------------------------------------------------------------------------
-- This flag controls the length of all temp strings we create on the stack.
-- The point of this being user configurable is to reduce stack usage on small targets.
------------------------------------------------------------------------------------- */
#ifndef VCAST_MAX_STRING_LENGTH
#define VCAST_MAX_STRING_LENGTH 1000
#endif

/*-------------------------------------------------------------------------------
Objects and defines used for file processing.  These are used in all cases
-------------------------------------------------------------------------------*/


#define VCAST_STDOUT -1
#define VCAST_STDIN  -2
#define VCAST_STDERR -3


/* maximum number of files that can be open at any time */
#ifndef VCAST_MAX_FILES
#define VCAST_MAX_FILES 20
#endif
#ifndef VCAST_TOTAL_STRING_SIZE
#define VCAST_TOTAL_STRING_SIZE  1024
#endif

/* -------------------------------------------------------------------------------
-- We only need the file descriptor stuff when we are doing real file IO 
---------------------------------------------------------------------------------*/
#ifdef VCAST_USING_FILE_OPERATIONS

/* fpos_t is not always exchangeable with int/long */
#ifndef VCAST_TRACE32_TERM
  typedef fpos_t vcast_fpos_t;
#else
  typedef int vcast_fpos_t;
#endif

struct Vcast_File_Position{
  vcast_fpos_t position;
#ifdef VCAST_TRACE32_TERM
  int iPos;
  int fPos;
#endif  
  struct Vcast_File_Position* next;
};

struct FILES {
#ifdef VCAST_USE_SETBUF
  char fbuf[BUFSIZ];    /*buffer*/  
#endif
  FILE *fp;
  char name[VCAST_FILENAME_LENGTH];
  vcast_fpos_t start_pos;
  struct Vcast_File_Position* position_list;
  
#ifdef VCAST_TRACE32_TERM
  int handle;
  int iPos;
  int fPos;
#endif
  
};


struct FILES vcast_file_descriptor[VCAST_MAX_FILES];

#endif




/*-------------------------------------------------------------------------------
Compiler Specific Stuff
-------------------------------------------------------------------------------*/


#ifdef VCAST_USE_GH_SYSCALL
#ifdef __cplusplus
extern "C" int __ghs_syscall(int, ...);
#else
extern int __ghs_syscall(int, ...);
#endif
#endif



#ifdef VCAST_RENESAS_SH_SIM
#include "folder_path.h"
FILE *renesas_stdout_file;
#endif

#ifdef VCAST_PARADIGM
extern void dprintf (char *format,...);
#endif

#ifdef VCAST_KEIL_ARM_STM32_TGT
extern void dprintf (const char *s, ...);
#endif

#ifdef VCAST_PARADIGM_SC520
#ifdef __cplusplus
extern "C" {
#endif 

#include "586.h"
#include "SER1.h"

#ifdef __cplusplus
}
#endif 
extern COM ser1_com;
#endif


#ifdef VCAST_VXWORKS
#include <fcntl.h>
#include <unistd.h>
#endif


/*----------------------------------------------------------------------------
End Compiler Specific Stuff
------------------------------------------------------------------------------*/

/* TBD: Do we want to change this to the ccast_io.h */
#include "S0000007.h"
#include "VCAST_STRING.h"
#include "vcast_basics.h"

/* Main Extern C for the whole file */
#ifdef __cplusplus
extern "C" {
#endif

#ifdef VCAST_TRACE32_TERM
#include "VCAST_TRACE32_TERM.c"
#endif

/* -----------------------------------------------------------------------------
-- TBD: Investigate as part of case: 24422
-------------------------------------------------------------------------------*/
#ifndef VCAST_STDIO

static void vectorcast_save_position(
    struct FILES * vcast_file_ptr, 
    vcast_fpos_t vcast_position);
static void vectorcast_restore_position(struct FILES * vcast_file_ptr);

#endif


/* These declarations allow a debug script to capture each line of output from the harness */
/* To use this technique, set a breakpoint on the "vcastHaltDebugger" function, and capture the contents of the */
/* string: vcast_SingleLineOfOutput */
#ifdef VCAST_OUTPUT_VIA_DEBUGGER
void          vcastHaltDebugger();
unsigned long vcastSingleLineOfOutput_Length;
char          vcastSingleLineOfOutput[VCAST_MAX_STRING_LENGTH];
#endif


extern vCAST_boolean vCAST_FULL_STRINGS;




/* ----------------------------------------------------------------------------
-- In the NO_MALLOC case, we use these data structures for our file operations
-------------------------------------------------------------------------------*/
#ifndef VCAST_NO_MALLOC
#ifdef VCAST_FREE_HARNESS_DATA
struct VCAST_Allocated_Data *VCAST_allocated_data_list = VCAST_NULL;

void VCAST_Add_Allocated_Data(void * vcast_data_ptr){
  struct VCAST_Allocated_Data *new_data_ptr =  (struct VCAST_Allocated_Data *) VCAST_malloc(sizeof(struct VCAST_Allocated_Data ));
  new_data_ptr->next = VCAST_NULL;
  new_data_ptr->allocated = vcast_data_ptr;
  if (VCAST_allocated_data_list == VCAST_NULL) 
    VCAST_allocated_data_list = new_data_ptr;
  else {
    struct VCAST_Allocated_Data *current_data_ptr = VCAST_allocated_data_list;
    while (current_data_ptr->next != VCAST_NULL)
      current_data_ptr = current_data_ptr->next;

    current_data_ptr->next = new_data_ptr;
  }
}
#endif
#endif


#ifdef VCAST_STDIO

/*-------------------------------------------------------------------------------
-- VCAST_STDIO, VCAST_STDOUT_ONLY, and VCAST_BUFFER_OUTPUT ...
-- 
-- The following section defines all of the functions used for the various
-- flavors of standard I/O
-------------------------------------------------------------------------------*/



/*---------------------------------------------------------------------------
-- Data And Functions used in VCAST_STDIO mode  for InputDataArray(ida) File System Abstraction
-- For details on the design for this, please refer to this page:
-- https://vectorcast.atlassian.net/wiki/display/IDCT/Pointer+Math+Details
----------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------
-- Strings are stored as a tuple of length and the string itself
----------------------------------------------------------------------------*/

static unsigned int vcastLengthBytes = 1;

/*---------------------------------------------------------------------------
--  Pointer  To Beginning of String Area 
----------------------------------------------------------------------------*/
static const char *inputDataArrayStart;
/*---------------------------------------------------------------------------
--  Current Cumulative Offset into inputDataArray
----------------------------------------------------------------------------*/
static unsigned int currentIndexIntoDataArray = 0;

/* -------------------------------------------------------------------------
-- definition for stdin data size type
----------------------------------------------------------------------------*/
#ifndef VCAST_STDIN_DATA_SIZE_T
#define VCAST_STDIN_DATA_SIZE_T unsigned short
#endif

/*---------------------------------------------------------------------------
-- Data  for File Table Abstraction - inputDataArrayTOC
-- In STDOUTONLY mode, FileTable is prebuilt as  inputDataArrayTOC
-- It is constructed in non STDOUTONLY mode
----------------------------------------------------------------------------*/

struct FileData {
  char    name[VCAST_FILENAME_LENGTH]; 
  VCAST_STDIN_DATA_SIZE_T   offset;
  VCAST_STDIN_DATA_SIZE_T   len;
};
/* The below pointer points to the beginning of FileTable */
struct FileData *vcast_file_table;

/* Variable holds value for Maximum Number of Files  */
int NumFileTableEntries = VCAST_MAX_FILES;

/* The following table holds the current file offset from the start of a file */
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
struct FileLocation{
  VCAST_STDIN_DATA_SIZE_T offset;
  struct FileLocation *next; /* File location prior to the current file open */
};

struct FileLocation* vcast_file_offsets[VCAST_MAX_FILES];
#else
VCAST_STDIN_DATA_SIZE_T vcast_file_offsets[VCAST_MAX_FILES];
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */

/* counter used to give files unique ids */
int fileIdCounter = 0;

/*---------------------------------------------------------------------------
-- The files listed below are opened for write only and do not have
-- an associated entry in inputDataArrayTOC 
----------------------------------------------------------------------------*/
const char * const vcast_internal_file_names[] = {
#if defined (VCAST_FILE_INDEX)
         "1",
         "2",
         "3",
         "4",
         "5",
         "6",
         "VCAST.END"
#else
         "ASCIIRES.DAT",
         "EXPECTED.DAT",
         "TEMP_DIF.DAT",
         "TESTINSS.DAT",
         "THISTORY.DAT",
         "USERDATA.DAT",
         "VCAST.END"
#endif
                   };
#define VCAST_MAX_INTERNAL_FILES 7
/*---------------------------------------------------------------------------
-- End of Data  in VCAST_STDIO mode  for File Table Abstraction
----------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------
--  Functions for String File Operations - Get String and Add String
----------------------------------------------------------------------------*/
/**************************************************************************
Function: readLengthAndStringFromInputDataArray
Parameters: offset - Offset from the begining of inputDataArray
            len    - Length of String
Description: Functions reads length bytes at specified offset from
             the begining of inputDataArray and also returns a 
             pointer to the begining of string following the length
             Bytes
*************************************************************************/
static const char *readLengthAndStringFromInputDataArray(unsigned int currentOffset, unsigned int *len) {
   unsigned char hb = 0;
   unsigned char lb;
   /* Position the pointer at specified offset from the begining of inputDataArray */
   char* ptr = (char*)(inputDataArrayStart + currentOffset);
   /* Read the lower length byte */
   lb = *ptr++;
   if(vcastLengthBytes ==2){
      /* Read the higher length byte */
      hb = *ptr++;
   }
   /* set the length as two byte value obtained by left shifting higher byte and
      the OR'ing the lower byte  */
   *len = hb;
   *len = *len << 8;
   *len |= lb;
   return  (ptr);
}

/**************************************************************************
Function: GetFileOffSets
Parameters: fileData - the FileData structure to get a line from
            InitialOffset - Initial Offset in the InputDataArrayTOC
            Current Offset- Offset Within a file
Description: This function Sets A file's initial offset and current offset
             within the file
*************************************************************************/
int  GetFileOffsets(struct FileData * file, unsigned int *initialOffset,
                    VCAST_STDIN_DATA_SIZE_T *currentOffset)
{
  unsigned int lindex;
  /* Get The index into Current Offset Table for Files */
  lindex = file - vcast_file_table;
  *initialOffset = file->offset;
  /* Get The current offset for the current file */
  if (lindex < NumFileTableEntries){
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
    if (vcast_file_offsets[lindex]){
      *currentOffset = vcast_file_offsets[lindex]->offset;
    } else {
      *currentOffset = 0;
    }
#else
    *currentOffset = vcast_file_offsets[lindex];
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */
  } else {
    return VC_FAIL;
  }
  return VC_OK;
}
/**************************************************************************
Function: SetFileOffSet
Parameters: fileData - the FileData structure to get a line from
            Current Offset- Offset Within a file
Description: This function sets current offset within a file. 
*************************************************************************/
void  SetFileOffset(struct FileData * fileData, 
                    VCAST_STDIN_DATA_SIZE_T currentOffset)
{
  unsigned int lindex;
  /* Get The index into Current Offset Table for Files */
  lindex = fileData - vcast_file_table;
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
  if (vcast_file_offsets[lindex] == (struct FileLocation *)0){
    vcast_file_offsets[lindex] = (struct FileLocation *)VCAST_malloc(sizeof(struct FileLocation));
    vcast_file_offsets[lindex]->next = (struct FileLocation *)0;
  }
  vcast_file_offsets[lindex]->offset = currentOffset;
#else
  vcast_file_offsets[lindex] = currentOffset;
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */
}


/*---------------------------------------------------------------------------
-- Data and Routines  for Allocating and Releasing Memory for File System 
-- Data Structure
----------------------------------------------------------------------------*/
#define DEFAULT_FILESYSTEM_SIZE 2048
/* Var to hold File Table ENtries to be read in from VCAST_STDIN.DAT File */
unsigned int vcastNumStdioFiles     = 0;
/* Var to hold File System Size to be read in from VCAST_STDIN.DAT File */
unsigned int vcastFileSystemSize    = 0;

/**************************************************************************
Function: VCAST_ida_allocate_memory
Description: This function allocates memory for input strings if
             VCAST_STDOUT_ONLY is not defined
*************************************************************************/
void VCAST_ida_allocate_memory()
{
#ifndef VCAST_STDOUT_ONLY
   /*Allocate space for strings if VCAST_STDOUT_ONLY is false 
   */
   inputDataArrayStart = (char *) VCAST_malloc(vcastFileSystemSize);
#endif
                                           
}
/**************************************************************************
Function: VCAST_ida_release_memory
Description: This function releases memory for input strings if
             VCAST_STDOUT_ONLY is not defined
*************************************************************************/
void VCAST_ida_release_memory()
{
#ifndef VCAST_STDOUT_ONLY
   inputDataArrayStart = 0;
   if (vcast_file_table!= NULL)
      VCAST_free(vcast_file_table);
   vcast_file_table  = NULL;
#endif
}
/**************************************************************************
Function: vcast_get_internal_fileid
Description: This function returns the file descriptors for internal files 
             opened with write only mode
*************************************************************************/
int vcast_get_internal_fileid(char *filename)
{
   int i;
   for (i=0; i < VCAST_MAX_INTERNAL_FILES; i++) {
   
      if(!VCAST_strcmp(filename,(char*)vcast_internal_file_names[i]))
         return(i+NumFileTableEntries);
   }
   return VC_FAIL;
}

/*---------------------------------------------------------------------------
-- End of Data And Functions used in VCAST_STDIO mode  for inputDataArray File System Abstraction
----------------------------------------------------------------------------*/

/*-------------------------------------------------------------------------------
--                    VCAST_BUFFER_OUTPUT Storage
--
-- Storage used to buffer output when we are in VCAST_BUFFER_OUTPUT mode
-- The default size for the output buffer is 20,000.  The user can over-ride
-- this by setting the macro VCAST_OUTPUT_BUFFER_SIZE
-------------------------------------------------------------------------------*/
#ifdef VCAST_BUFFER_OUTPUT

#ifndef VCAST_OUTPUT_BUFFER_SIZE
#define VCAST_OUTPUT_BUFFER_SIZE 20000
#endif

char vcast_output_buffer[VCAST_OUTPUT_BUFFER_SIZE];
int  vcast_output_buffer_end = 0;

void vcast_clear_output_buffer (void) {
   VCAST_memset(vcast_output_buffer, 0, sizeof(vcast_output_buffer));
   vcast_output_buffer_end = 0;
   }

#endif
/*-----------------------------------------------------------------------------*/

/* See the header comment in this file for the usage of these objects */
#ifdef VCAST_STDIO_WITH_FILES
   struct FILES vcast_stdin_file;
   struct FILES vcast_stdout_file;
#endif

/* buffer used to store string until it can be printed */
char totalStringToPrint[VCAST_MAX_STRING_LENGTH]; 

/* string used to separate file name from contents of line */
#define VCAST_FILE_NAME_SEPARATOR ":"

/* string that denotes the beginning of a new file */
#define VCAST_FILE_BEGINNING_MARKER "*FILE:"

/* string that denotes the end of an input sequence */
#define VCAST_INPUT_ENDING_MARKER "*END"


/*---------------------------------------------------------------------------
-- Functions used in VCAST_STDIO mode _AND_ in VCAST_STDOUT_ONLY mode
----------------------------------------------------------------------------*/

/*--------------------------------------------------------------------------*/
/*---------------------------------------------------------------------------*/
int StartsNewFile(const char * line){
  int fileBeginningMarkerLength = VCAST_strlen(VCAST_FILE_BEGINNING_MARKER);
  if (VCAST_strncmp(line, (char *)VCAST_FILE_BEGINNING_MARKER, fileBeginningMarkerLength) == 0)
    return vCAST_true;
  
  return vCAST_false;
}
/*-----------------------------------------------------------------------------*/
/*-----------------------------------------------------------------------------*/
int EndOfInput (const char * line){
  int endofInputLength = VCAST_strlen(VCAST_INPUT_ENDING_MARKER);
  if (VCAST_strncmp(line, (char *)VCAST_INPUT_ENDING_MARKER, endofInputLength) == 0) {
     return vCAST_true;
     }
  else {
     return vCAST_false;
     }
}

/**************************************************************************
Function: GetCurrentLine
Parameters: fileData - the FileData structure to get a line from
            uline    -  Buffer to which data should be read into
            maxlen   -  Max length of buffer to be read into
Description: This function returns the line at the current location in
the FileData structure.
  *************************************************************************/
char * GetCurrentLine(struct FileData * fileData, char *uline, int maxlen){
  const char *line;
  VCAST_STDIN_DATA_SIZE_T lCurrentOffset;
  unsigned int lInitialOffset ;
  char *uptr = uline;
  unsigned int len;
  unsigned int limit;

  if (fileData == (struct FileData *)NULL)
    return (char *) NULL;
  {
    /* Get Offset for start of file and Offset within file */
    if(GetFileOffsets(fileData, &lInitialOffset, &lCurrentOffset) < 0)
      return (char *) NULL; 
  };
  /* Get A pointer to the Next Line */
  line = readLengthAndStringFromInputDataArray(lInitialOffset+lCurrentOffset, &len);
  if (line == (char *)NULL){
    return (char *) NULL; 
  }
  /* Copy it to User Buffer */
  if(len >= maxlen)
    limit = maxlen - 1;
  else
    limit = len;
  while (limit){
      *uptr++=*line++;
      limit--;
  }
  /* Put String Terminator */
  *uptr = '\0';
  /* if the current line signals the start of another file */
  if (StartsNewFile(uline) || EndOfInput(uline) ){
    /* this is really the end of file */
    return (char *) NULL;
  }
  return ((char *)uline);
}

/**************************************************************************
Function: AdvanceCurrentLine
Parameters: fileData - the FileData structure to update
Description: This function advances the position in the file by one line.
  *************************************************************************/
void AdvanceCurrentLine(struct FileData * fileData){

  unsigned int len;
  const char *str;
  VCAST_STDIN_DATA_SIZE_T lCurrentOffset ;
  unsigned int lInitialOffset ;
  if (fileData != (struct FileData *)NULL) {
      /* Get Offset for start of file and Offset within file */
      if (GetFileOffsets(fileData, &lInitialOffset, &lCurrentOffset) < 0)
         return;
      str = readLengthAndStringFromInputDataArray(lInitialOffset+lCurrentOffset , &len);
      /* update offset by adding the length of string and size of storage for length
      */
      lCurrentOffset += (len + vcastLengthBytes );
      SetFileOffset(fileData, lCurrentOffset);
  }
}

/**************************************************************************
Function: ResetCurrentLine
Parameters: fileData - the FileData structure to reset
Description: This function takes a FileData structure and sets its current
line pointer to the location where it was before the current file open.
  *************************************************************************/
void ResetCurrentLine(struct FileData * fileData){
  if (fileData != (struct FileData *)NULL) {
    VCAST_STDIN_DATA_SIZE_T lCurrentOffset;
    unsigned int lInitialOffset;
    unsigned int file_index = fileData - vcast_file_table;
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
    struct FileLocation * location = vcast_file_offsets[file_index];
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */
    /* Validate FileData */
    if(GetFileOffsets(fileData, &lInitialOffset, &lCurrentOffset) < 0) {
      return ;
    };
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
    if (location){
      vcast_file_offsets[file_index] = location->next;
      VCAST_free(location);
    }
#else
    /* Reset offset within file to zero */
    SetFileOffset(fileData, 0);
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */
  }
}

struct FileData* GetFile(int fpn);



/*---------------------------------------------------------------------------
-- Functions used _ONLY_ in VCAST_STDOUT_ONLY mode
-----------------------------------------------------------------------------*/

#ifdef VCAST_STDOUT_ONLY
#ifndef VCAST_INPUT_DATA_ARRAY_ATTRIBUTE
#define VCAST_INPUT_DATA_ARRAY_ATTRIBUTE
#endif

#ifdef __cplusplus
extern "C" const char* const VCAST_INPUT_DATA_ARRAY_ATTRIBUTE inputDataArray;
extern "C" const struct FileData inputDataArrayTOC[];
extern "C" const int isTwoByteLen ;
extern "C" const int NumInputFiles ;
#else
extern const char * const VCAST_INPUT_DATA_ARRAY_ATTRIBUTE inputDataArray;
extern  const struct FileData inputDataArrayTOC[];
extern  const int isTwoByteLen ;
extern  const int NumInputFiles ;
#endif

/*-----------------------------------------------------------------------------
-- End of Functions used _ONLY_ in VCAST_STDOUT_ONLY mode
-----------------------------------------------------------------------------*/

#else

/*-----------------------------------------------------------------------------
--Functions used in VCAST_STDIO mode but _NOT_ VCAST_STDOUT_ONLY mode
-----------------------------------------------------------------------------*/

/**************************************************************************
Function: NewFileData
Parameters: name - name of the file
Description: This function is used to create and initialize a new FileData
structure.                                  
 *************************************************************************/
struct FileData * NewFileData(char * name){
  /* Get Next  Available file table entry */
  struct FileData * newFileDataLink = (struct FileData *) &(vcast_file_table[fileIdCounter]);

  /* Initialize Name Field */
  VCAST_memset(newFileDataLink->name, '\0', VCAST_FILENAME_LENGTH);
  VCAST_strcpy(newFileDataLink->name, name);

  /* Initialize Initial Offset */
  newFileDataLink->offset = currentIndexIntoDataArray;

  /* Initialize Len Field */
  newFileDataLink->len = 0;

  /* increment the global counter so the next file has a different
     identification number */
  fileIdCounter++;
  return newFileDataLink;
}

/**************************************************************************
Function: AddNewFile
Parameters: name - name of the file
Description: This function makes a new FileData structure and adds it to 
the global list.                           
 *************************************************************************/
struct FileData * AddNewFile(char * name){
  struct FileData * newFileDataLink = NewFileData(name);
  return newFileDataLink;
}
/**************************************************************************
Function: AddLineToInputDataArray
Parameters: name - line - contents of the line read
                   lineLen - Length of the line
                   offset  - Offset at  which the line needs to be added
Description: This function Appends a line to the inputDataArray
 *************************************************************************/
static void  AddLineToInputDataArray(const char *lineContents, 
                                    int lineLen, 
                                    int currentOffset){
  unsigned char lb;
  unsigned char hb;
  char *linePtr = (char *)lineContents;
  char *ptr = (char*)(inputDataArrayStart + currentOffset);

  /* Store Lower byte of length at lower address */
  lb = lineLen & 0x00ff;
  hb = lineLen >> 8;
  *ptr++ = lb;
  
  /* Store Higher byte of length at higher address */
  if(vcastLengthBytes==2) {
     *ptr++ = hb;
     }
     
  /* Store the string */
  while(lineLen--) {
   *ptr++ = *linePtr++;
   }
  
}
/**************************************************************************
Function: AddNewLine
Parameters: fileData - FileData structure that is getting the line
            lineContents - line of a file that is to be stored
Description: This function adds a line to a FileData structure.
 *************************************************************************/
void AddNewLine(struct FileData * fileData, 
                char * lineContents, 
                VCAST_STDIN_DATA_SIZE_T offset){
  VCAST_STDIN_DATA_SIZE_T lCurrentOffset ;
  unsigned int lInitialOffset ;
  int lineLen = VCAST_strlen( lineContents ) ; 
  fileData->len += (lineLen + vcastLengthBytes);
  /*  Validate FileData */
  if(GetFileOffsets(fileData, &lInitialOffset, &lCurrentOffset) < 0) {
    return ;
  };

  /* Add the line to input Data Array at offset computed above */
  AddLineToInputDataArray(lineContents, lineLen, offset + fileData->offset);
  /* Update the cumulative offset from start of InputData Array */
  currentIndexIntoDataArray += (lineLen + vcastLengthBytes) ;
}

#endif  /* !VCAST_STDOUT_ONLY */


/*-----------------------------------------------------------------------------
-- Functions used in VCAST_STDIO mode _AND_ in VCAST_STDOUT_ONLY mode
--   these are here because they rely on types from above
-----------------------------------------------------------------------------*/


char * GetFileName(struct FileData * fileData) {
  if (fileData != (struct FileData *)NULL)
    return fileData->name;
  
  return ((char *)"");
}

/**************************************************************************
Function: GetIdNumber
Parameters: fileData - Pointer to FileData Structure
Description: This function retrieves the FileId for a Given File Data Structure
 *************************************************************************/

int GetIdNumber(struct FileData * fileData){
int lindex = VC_FAIL;
  /* FileId is the index into the file table TOC */
  if (fileData  != (struct FileData *)NULL) {
    lindex = fileData - vcast_file_table;
    if ((lindex > 0) && (lindex < NumFileTableEntries))
      return lindex;
  }
  return lindex;
}




/**************************************************************************
Function: GetFileFromName
Parameters: fileName - name of file to find
Description: This function retrieves the FileData structure with the 
matching name. File Data is present for files in inputDataTOC which
are files opened in Read mode and contains offsets. The Files open
only for writing  do not have FileData and their names are constructed
using internal file names 
 *************************************************************************/
struct FileData * GetFileFromName(char * fileName){
  /* Index to the begining of File Table */
  int index = 0;
  /* Iterate from beginning to end for matching file name */
  for (index=0;index<NumFileTableEntries;index++){
    if (VCAST_strcmp(vcast_file_table[index].name, fileName) == 0)
      return (&vcast_file_table[index]);
  }
  return (struct FileData *)NULL;
}

/**************************************************************************
Function: GetFile
Parameters: fpn - file identification number
Description: This function retrieves the FileData structure with the 
matching identification number.                          
 *************************************************************************/
struct FileData * GetFile(int fpn){
  /* Return File Table entry if fpn falls within the range of the file table */
  if ((fpn >=0) && (fpn < NumFileTableEntries)) {
      return( &vcast_file_table[fpn]);
  }

  return (struct FileData *) NULL;
}

#endif  /* VCAST_STDIO */

/*-----------------------------------------------------------------------------
-- End of VCAST_STDIO and STDOUT_ONLY Functions
-----------------------------------------------------------------------------*/



/* -------------------------------------------------------------------------------
-- Function Prototypes for local static functions
----------------------------------------------------------------------------------*/
static void vectorcast_local_print_string (int fpn, const char *str);
static void vectorcast_read_from_std_in (char *str);
int vcast_check_for_dump_buffer (void);

/* prototype for S3.c function */
void vCAST_END(void);




#ifdef VCAST_USING_FILE_OPERATIONS
static FILE* vectorcast_get_fd ( int fpn );
#endif


/* -------------------------------------------------------------------------------
-- External API
-- This section contains all of the functions for the external API (functions defined in ccast_io.h
-------------------------------------------------------------------------------*/


void vectorcast_fprint_string (int fpn, const char *vcast_str) {
   vectorcast_local_print_string (fpn, vcast_str);
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_string_with_length (int fpn, const char *vcast_str, int length) {
  if(vCAST_FULL_STRINGS){
     int i;
     char mystr[2] = {0, 0};
     for (i = 0; i < length; i++){
        mystr[0] = vcast_str[i];
        vectorcast_local_print_string (fpn, mystr);
     }
  } else {
   vectorcast_local_print_string (fpn, vcast_str);
  }
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_string_with_cr (int fpn, const char *vcast_str) {
   vectorcast_local_print_string (fpn, vcast_str);
   vectorcast_local_print_string (fpn, "\n");
}

/* ------------------------------------------------------------------------------- */
void vectorcast_print_string (const char *vcast_str) {
   vectorcast_fprint_string (VCAST_STDOUT, vcast_str); 
}

/* ------------------------------------------------------------------------------- */
/* if we can use sprintf, use it */
/* ------------------------------------------------------------------------------- */

#ifndef VCAST_NO_SPRINTF 
void vcast_signed_to_string ( char vcDest[],
                              VCAST_SIGNED_CONVERSION_TYPE vcSrc )
{
   sprintf ( vcDest, VCAST_LONGEST_INT_FORMAT, vcSrc );
}
void vcast_unsigned_to_string ( char vcDest[],
                                VCAST_UNSIGNED_CONVERSION_TYPE vcSrc )
{
   sprintf ( vcDest, VCAST_LONGEST_UINT_FORMAT, vcSrc );
}

#ifndef VCAST_CHAR_HEX_FORMAT
#define VCAST_CHAR_HEX_FORMAT "%x"
#endif

#ifndef VCAST_CHAR_OCT_FORMAT
#define VCAST_CHAR_OCT_FORMAT "%o"
#endif

void vcast_char_to_based_string ( char vcDest[],
                                  unsigned char vcSrc,
                                  unsigned vcUseHex )
{
  /* An explicit cast is necessary on vcSrc since some compilers, such as
     Keil c51, do not promote the type automatically. */
   if ( vcUseHex )
      sprintf ( vcDest, "\\x" VCAST_CHAR_HEX_FORMAT, (unsigned)vcSrc );
   else
      sprintf ( vcDest, "\\" VCAST_CHAR_OCT_FORMAT, (unsigned)vcSrc );
}


/* ------------------------------------------------------------------------------- */
/* we can't use sprintf => convert */
/* ------------------------------------------------------------------------------- */
#else

VCAST_SIGNED_CONVERSION_TYPE vcAbs ( VCAST_SIGNED_CONVERSION_TYPE vcNum )
{
   if ( vcNum < 0 )
      return 0 - vcNum;
   else
      return vcNum;
}

void vcast_signed_to_string ( char vcDest[],
                              VCAST_SIGNED_CONVERSION_TYPE vcSrc )
{
  vectorcast_signed_to_string(vcDest, vcSrc);
}

#define VCAST_DECIMAL_DIGIT(u) ('0'+u)
void vcast_unsigned_to_string ( char vcDest[],
                                VCAST_UNSIGNED_CONVERSION_TYPE vcSrc )
{
   char vcBackwards[VCAST_MAX_STRING_LENGTH];
   unsigned vcCount = 0;
   unsigned vcI;
   VCAST_UNSIGNED_CONVERSION_TYPE vcValue = vcSrc;
   VCAST_UNSIGNED_CONVERSION_TYPE vcRem;

   vcCount = 0;
   while ( vcValue >= 10 ) {
      vcRem = vcValue % 10;
      vcBackwards[vcCount++] = VCAST_DECIMAL_DIGIT(vcRem);
      vcValue = vcValue / 10;
   }
   vcBackwards[vcCount] = VCAST_DECIMAL_DIGIT(vcValue);
   /* vcI already initialized (based on sign) */
   for ( vcI=0; vcI<=vcCount; vcI++ )
      vcDest[vcI] = vcBackwards[vcCount-vcI];
   vcDest[vcI] = 0;
   
}

void vcast_char_to_based_string ( char vcDest[],
                                  unsigned char vcSrc,
                                  unsigned vcUseHex )
{
   char vcBackwards[VCAST_MAX_STRING_LENGTH];
   unsigned vcCount = 0;
   unsigned vcFirst;
   unsigned vcI;
   unsigned char vcValue = vcSrc;
   unsigned char vcRem;

   unsigned vcBase;

   if ( vcUseHex ) {
      vcBase = 16;
      vcDest[0] = '\\';
      vcDest[1] = 'x';
      vcFirst = 2;
   } else {
      vcBase = 8;
      vcDest[0] = '\\';
      vcFirst = 1;
   }

   vcCount = 0;
   while ( vcValue >= vcBase ) {
      vcRem = vcValue % vcBase;
      vcBackwards[vcCount++] = VCAST_DECIMAL_DIGIT(vcRem);
      vcValue = vcValue / vcBase;
   }
   vcBackwards[vcCount] = VCAST_DECIMAL_DIGIT(vcValue);
   /* vcI already initialized (based on sign) */
   for ( vcI=0; vcI<=vcCount; vcI++ )
      vcDest[vcFirst+vcI] = vcBackwards[vcCount-vcI];
   vcDest[vcFirst+vcI] = 0;
}

#endif
/* ------------------------------------------------------------------------------- */
/* end VCAST_NO_SPRINTF */
/* ------------------------------------------------------------------------------- */

void vectorcast_print_signed ( int vcast_fpn,
                               VCAST_SIGNED_CONVERSION_TYPE vcast_value ) {

   char local_string[VCAST_MAX_STRING_LENGTH];
   vcast_signed_to_string ( local_string, vcast_value );
   vectorcast_local_print_string (vcast_fpn, local_string);
}

/* ------------------------------------------------------------------------------- */
void vectorcast_print_unsigned ( int vcast_fpn,
                                 VCAST_UNSIGNED_CONVERSION_TYPE vcast_value ) {

   char local_string[VCAST_MAX_STRING_LENGTH];
   vcast_unsigned_to_string ( local_string, vcast_value );
   vectorcast_local_print_string (vcast_fpn, local_string);
   }

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_short     (int vcast_fpn, short vcast_value ) {
   vectorcast_print_signed ( vcast_fpn, vcast_value );
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_integer   (int vcast_fpn, int vcast_value ) {
   vectorcast_print_signed ( vcast_fpn, vcast_value );
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_long      (int vcast_fpn, long vcast_value ) {
   vectorcast_print_signed ( vcast_fpn, vcast_value );
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_long_long (int vcast_fpn, VCAST_LONGEST_INT vcast_value ) {
   vectorcast_print_signed ( vcast_fpn, vcast_value );
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_unsigned_short (int vcast_fpn,
                                       unsigned short vcast_value ) {
   vectorcast_print_unsigned ( vcast_fpn, vcast_value );
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_unsigned_integer (int vcast_fpn, 
                                         unsigned int vcast_value ) {
   vectorcast_print_unsigned ( vcast_fpn, vcast_value );
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_unsigned_long (int vcast_fpn, 
                                      unsigned long vcast_value ) {
   vectorcast_print_unsigned ( vcast_fpn, vcast_value );
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_unsigned_long_long (int vcast_fpn, 
                                           unsigned VCAST_LONGEST_INT vcast_value ) {
   vectorcast_print_unsigned ( vcast_fpn, vcast_value );
}



/* ------------------------------------------------------------------------------- */
/* character */
void vectorcast_fprint_char   (int fpn, char vcast_str) {
   char local_string[2];
   local_string[0] = vcast_str;
   local_string[1] = 0;
   vectorcast_fprint_string_with_cr (fpn, local_string);
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_char_hex ( int fpn, char vcast_value ) {
   char local_string[5];
   vcast_char_to_based_string ( local_string, vcast_value, 1 );
   vectorcast_fprint_string_with_cr (fpn, local_string);  
}

/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_char_octl ( int fpn, char vcast_value ) {
   char local_string[5];
   vcast_char_to_based_string ( local_string, vcast_value, 0 );
   vectorcast_fprint_string_with_cr (fpn, local_string);
}


/* ------------------------------------------------------------------------------- */
void vectorcast_fprint_long_float (int fpn, vCAST_long_double vcast_value) {
   char local_string[VCAST_MAX_STRING_LENGTH];
   vcast_float_to_string( local_string, vcast_value );
   vectorcast_local_print_string (fpn, local_string);
}


#ifdef VCAST_PROBE_POINTS_AVAILABLE
void vcast_probe_print (const char *S)
{
   vectorcast_write_to_std_out(S);
}

extern int VCAST_EXP_FILE;

void vcast_probe_assert (const char *msg, int condition)
{
    if (!VCAST_EXP_FILE)
      return;

    if (condition)
      vectorcast_fprint_string (VCAST_EXP_FILE, "<match>|<<assert>>|");
    else
      vectorcast_fprint_string (VCAST_EXP_FILE, "[fail]|<<assert>>|");

    if (msg)
      vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, msg);
    else
      vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, "(description not provided)");

    vCAST_SET_HISTORY( 1015, 999 );
}
#endif

/*------------------------------------------------------------------------------- 
-- Simple Versions of EXTERNAL API Functions
--   This section of the file contains the processing for API functions that are conditional
--   based on the IO type being used.  In all cases, these are implemented by a common function
--   that performs the FILE IO style processing, with conditionally compiled calls to functions
--   that implement specific IO mechanism.  For example, the first function: 
--   "vectorcast_initialize_io" has conditionally compilation to call the STD IO version of
--   this function called: "vectorcast_initialize_io_stdio"
--
--   To keep this section of the file clean, we put forward declarations in for the 
--   functions that are IO specific.
---------------------------------------------------------------------------------*/


/*------------------------------------------------------------------------------- 
-- vectorcast_initialize_io
---------------------------------------------------------------------------------*/
static void vectorcast_initialize_io_stdio (int inst_status, int inst_fd);

void vectorcast_initialize_io (int inst_status, int inst_fd) {
#if !defined (VCAST_STDIO)
   int i;
   for(i=0 ; i < VCAST_MAX_FILES; i ++ ) {
     /* This check is so that we do not blast the inst data
	    that may have been captured prior to this call */
     if (!(inst_status == 1 && inst_fd == i)){
        VCAST_memset(&vcast_file_descriptor[i],0,sizeof(struct FILES));
     }
   }
   
#else
   vectorcast_initialize_io_stdio (inst_status, inst_fd);
#endif

}



/* ------------------------------------------------------------------------------- 
-- vectorcast_write_vcast_end
---------------------------------------------------------------------------------*/
void vectorcast_write_vcast_end (void) {

  int vCAST_END_FILE;
  int retCode;
  
  vCAST_END_FILE = vectorcast_fopen  ( (char *)"VCAST.END", (char *)"w");
  vectorcast_fprint_string ( vCAST_END_FILE, "END\n");
  vectorcast_fclose ( vCAST_END_FILE );
  
  /* we don't care if the dump was done or not, so ignore return code */
  retCode = vcast_check_for_dump_buffer();

}  
/*-------------------------------------------------------------------------------*/



/*------------------------------------------------------------------------------- 
-- vectorcast_local_print_string
---------------------------------------------------------------------------------*/
static void vectorcast_local_print_string_stdio (int fpn, const char *str);

static void vectorcast_local_print_string (int fpn, const char *str) {
#ifdef VCAST_TRACE32_TERM
   if (fpn == VCAST_STDOUT)
   {
       TRACE32_TERM_Puts( (char *) str );
   }
   else
   {
       TRACE32_TERM_WriteFile(vcast_file_descriptor[fpn].handle, (char *) str, VCAST_strlen(str));
   }
#else
#if !defined (VCAST_STDIO)
   fprintf (vectorcast_get_fd(fpn), VCAST_PRINTF_STRING, str);
#else
   vectorcast_local_print_string_stdio (fpn, str);
#endif
#endif

}



/* -------------------------------------------------------------------------------
-- vectorcast_readline: Reads a line from an open file stream.  This line can
-- have an arbitrary length.  The result of this function is a pointer to a
-- NULL-terminated string which does *not* include the trailing newline
-- character.  The string will need to be freed when you are done with it.  
-- Return failure condition if the line we read is too long 
 ---------------------------------------------------------------------------------*/
int vectorcast_readline(char *vcast_buf, int fpn) {
   char *s;
   int vcLen;
   int retVal = 0;

   VCAST_memset(vcast_buf, '\0', VCAST_MAX_STRING_LENGTH);
   s = vectorcast_fgets(vcast_buf, VCAST_MAX_STRING_LENGTH, fpn);
   vcLen = VCAST_strlen ( vcast_buf );

#ifdef VCAST_TRACE32_TERM
   if (vcLen == 0)  /* EOF */
      return 0;
#endif

   /* if the last character is not a newline, then the line is too long */
   if ( vcLen > 0 && vcast_buf[vcLen-1] != '\n' )
      retVal = 1;

   /* Remove trailing newline */
   if (VCAST_strlen(vcast_buf)>0 && vcast_buf[VCAST_strlen(vcast_buf)-1] == '\n')
      vcast_buf[VCAST_strlen(vcast_buf)-1] = 0;

   return retVal;
}
/*-------------------------------------------------------------------------------*/



/*------------------------------------------------------------------------------- 
---------------------------------------------------------------------------------*/
int vectorcast_fflush(int fpn) {
#if defined (VCAST_NO_STD_FILES) || defined (VCAST_NO_FFLUSH) || defined (VCAST_STDIO) || defined (VCAST_TRACE32_TERM)
   return VC_OK;
#else
   return fflush(vectorcast_get_fd(fpn));
#endif

}
/* ------------------------------------------------------------------------------*/ 



/* ------------------------------------------------------------------------------- 
---------------------------------------------------------------------------------*/
int vectorcast_feof(int fpn) {
   int i;

#ifdef VCAST_TRACE32_TERM
  return TRACE32_TERM_EOF(vcast_file_descriptor[fpn].handle);
#endif

#ifdef VCAST_STDIO
   /* GetCurrentLine requires a user specified
      buffer */
   char line[VCAST_MAX_STRING_LENGTH];
   struct FileData * file = GetFile(fpn);
   if (GetCurrentLine(file, line, VCAST_MAX_STRING_LENGTH) != 0)
     i = 0;
   else
     i = 1;

#else
   if (fpn != -1)
      i = feof(vectorcast_get_fd(fpn));
   else
      i = 1;
#endif /* !defined (VCAST_STDIO) */
   return i;
}
/*-------------------------------------------------------------------------------*/




/* -------------------------------------------------------------------------------
-- External API End
-------------------------------------------------------------------------------*/



/*-------------------------------------------------------------------------------
---------------------------------------------------------------------------------
-- Functions that are specific to a particular IO configuration.
---------------------------------------------------------------------------------
-------------------------------------------------------------------------------*/

#ifdef VCAST_USING_FILE_OPERATIONS

/*-------------------------------------------------------------------------------
-------------------------------------------------------------------------------*/

FILE* vectorcast_get_fd ( int fpn ) {

   if ( fpn == VCAST_STDOUT )
      return stdout;
   else 
     if ( fpn == VCAST_STDIN )
       return stdin;
     else 
       if ( fpn == VCAST_STDERR )
     return stderr;
       else 
     if ( (fpn < 0) || (fpn>=VCAST_MAX_FILES) )
       return 0;
     else
       return vcast_file_descriptor[fpn].fp;
}


/*-------------------------------------------------------------------------------
-- This function works around bugs in the old Tornado versions of vxWorks
-- For the PC simulator, fopen for WRITE did not work properly
-------------------------------------------------------------------------------*/
void targetSpecificFileOpen (struct FILES* fd, char* filename, char *mode) {

#if defined (VCAST_FILE_PREFIX)
   char tmp_string[VCAST_MAX_STRING_LENGTH];

#define QUOTE1(x) QUOTE2(x)
#define QUOTE2(x) #x
   
   VCAST_strcpy ( tmp_string, QUOTE1(VCAST_FILE_PREFIX));
   VCAST_strcat ( tmp_string, filename );
   
   fd->fp = fopen(tmp_string, mode);
#else /* !defined (VCAST_FILE_PREFIX) */
#ifdef VCAST_TRACE32_TERM
   
  switch (mode[0])
  {
   case 'a':
   case 'A':
     fd->handle = TRACE32_TERM_OpenFile(filename, 8);
     if (fd->handle == -1) 
       TRACE32_TERM_Puts( "failed to write file\r\n" );
     break;
   case 'w':
   case 'W':
     fd->handle = TRACE32_TERM_OpenFile(filename, 4);
     if (fd->handle == -1) 
       TRACE32_TERM_Puts( "failed to write file\r\n" );
     break;
   case 'r':
   case 'R':
     fd->handle = TRACE32_TERM_OpenFile(filename, 0);
     break;

   default:
     break;
  };

  fd->fp = (FILE *) 0xffff;  
#else /* !defined (VCAST_TRACE32_TERM) */
   fd->fp = fopen(filename, mode);  
#endif /* VCAST_TRACE32_TERM */
#endif /* We are isomg the file prefix */

#ifdef VCAST_USE_SETBUF
   /* needed for some INTEGRITY targets to speed up IO */
   setbuf (*fd.fp, *fd.fbuf);
#elif defined(VCAST_USE_SETLINEBUF) || defined(VCAST_USE_SETVBUF)
   /* use if you expect harness to be killed before C library buffers can be flushed */
   setvbuf( fd->fp, NULL, _IOLBF, 0 );
#endif

}

#endif /* VCAST_USING_FILE_OPERATIONS */


/*------------------------------------------------------------------------------- 
-- vectorcast_initialize_io_stdio
---------------------------------------------------------------------------------*/
#ifdef VCAST_STDIO

static void vectorcast_initialize_io_stdio (int inst_status, int inst_fd)
{

   struct FileData * currentFile = 0;
   char currentLine[VCAST_MAX_STRING_LENGTH];
   int len;
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
   int file_index;
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */

   /* Some of the target compilers do not by default initialize
      global scope variables, so we do this explicitly here */
   fileIdCounter = 0;
   currentIndexIntoDataArray = 0;
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
   for (file_index = 0; file_index < VCAST_MAX_FILES; ++file_index){
     vcast_file_offsets[file_index] = (struct FileLocation*)0;
   } 
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */
#ifdef VCAST_BUFFER_OUTPUT
   vcast_clear_output_buffer();

#endif

   /* Allocate Space for InputDatarrayTOC
    * Allocate Space for InputDataArray
    * Allocate Space for Storing FileTable Offsets
    */
#ifdef VCAST_STDOUT_ONLY
   inputDataArrayStart = inputDataArray;
   len = VCAST_strlen(inputDataArrayStart);
   vcast_file_table = (struct FileData *) inputDataArrayTOC;
   if (isTwoByteLen == 1) 
      vcastLengthBytes = 2; 
   else 
      vcastLengthBytes = 1; 
   NumFileTableEntries = NumInputFiles;
#else

#ifdef VCAST_STDIO_WITH_FILES
   targetSpecificFileOpen (&vcast_stdin_file, "VCAST_STDIN.DAT", "r");
   targetSpecificFileOpen (&vcast_stdout_file, "VCAST_STDOUT.DAT", "w");
#endif

   /* for Non STDOUT_ONLY case, length prefix is two bytes */
   vcastLengthBytes = 2; 
   VCAST_memset(currentLine, '\0', VCAST_MAX_STRING_LENGTH);
   vectorcast_read_from_std_in(currentLine);
   len = VCAST_strlen(currentLine);
   vcastNumStdioFiles = (len==0) ? VCAST_MAX_FILES : VCAST_atoi(currentLine);
   /* ------------------------------------------------------
    -- NOTE: atoi does not detect errors.  So, we have to check
    --  for correct range during usage.
    ------------------------------------------------------ */
   vcast_file_table = (struct FileData *)VCAST_malloc(sizeof(struct FileData) * (vcastNumStdioFiles));
   NumFileTableEntries = vcastNumStdioFiles;

   /* Clear the current line buffer */
   VCAST_memset(currentLine, '\0', VCAST_MAX_STRING_LENGTH);
   vectorcast_read_from_std_in(currentLine);
   len = VCAST_strlen(currentLine);
   vcastFileSystemSize = (len==0) ?
                         DEFAULT_FILESYSTEM_SIZE :
                         VCAST_atoi(currentLine);
   VCAST_ida_allocate_memory();
#endif
   
  /* Allocate memory for event history file so error message can be
     displayed if program runs out of heap. */
     
#ifdef VCAST_RENESAS_SH_SIM
   renesas_stdout_file = fopen(VCAST_STDOUT_FOLDER,"w");
#endif

   /* Clear the current line buffer */
   VCAST_memset(currentLine, '\0', VCAST_MAX_STRING_LENGTH);
   vectorcast_read_from_std_in(currentLine);

   /* TracePoint #1 (ccast_io ) ########################################
   Validate that CurrentLine has the correct data.
   
   Here is an example: If you are at the very first breakpoint here, 
   and you are in VCAST_STDOUT mode, then currentLine should contain
   the very first line from the file: VCAST_STDIN.DAT, probably a line
   that looks like this: "*FILE:TESTORDR.DAT"     
   */
#ifdef VCAST_TEST_HARNESS_TRACE
{
static int vcastFirstReadOfstdin = 1;
if (vcastFirstReadOfstdin) {
   vcastFirstReadOfstdin = 0;
   vectorcast_write_to_std_out ("TracePoint B7: #1: First Read of stdin, ");
   vectorcast_write_to_std_out ("we read: ");
   vectorcast_write_to_std_out ( currentLine );
   vectorcast_write_to_std_out (" \n");
   }
}
#endif

#ifndef VCAST_STDOUT_ONLY
 {
   VCAST_STDIN_DATA_SIZE_T offset = 0;
   while ((len=VCAST_strlen(currentLine)) > 0){

     /* check for the end of input marker */
     if (VCAST_strncmp(currentLine, (char *)VCAST_INPUT_ENDING_MARKER, VCAST_strlen(VCAST_INPUT_ENDING_MARKER)) == 0){
       AddNewLine(currentFile, currentLine, offset);
       break;
     }

     if (StartsNewFile(currentLine)) {
          /* get name of file */
          char fileName[VCAST_FILENAME_LENGTH];
          int j = 0;
          int i;
          int fileBeginningMarkerLength = VCAST_strlen(VCAST_FILE_BEGINNING_MARKER);
          VCAST_memset(fileName, '\0', VCAST_FILENAME_LENGTH);
          for (i = fileBeginningMarkerLength; i < VCAST_strlen(currentLine); i++){

             /* new line is not part of file name so don't add it*/
             if (currentLine[i] == '\n')
                break;

             fileName[j] = currentLine[i];
             j++;
             
          } /* for */
          currentFile = AddNewFile(fileName);
          AddNewLine(currentFile, currentLine, 0);
          /* Update offset for file */
          offset = len + vcastLengthBytes;
     }
     else {
        if (currentFile != 0){
           /* replace carriage return since this is a line from a file */
           VCAST_strcat(currentLine, "\n");
           AddNewLine(currentFile, currentLine, offset);
           /* Update offset for file */
           offset += len + vcastLengthBytes+1;
        } 
     }
   
    /* get next line */
    VCAST_memset(currentLine, '\0', VCAST_MAX_STRING_LENGTH);
    vectorcast_read_from_std_in(currentLine);
   }
 }
#endif /* !defined (VCAST_STDIN) */

   /* TracePoint #2 (ccast_io ) ########################################
   Validate that all of the input data has been translated into 
   the file pointer structures. 
   
   Look at the global object: allFilesLink.  This is a linked list of 
   file data, use the debugger to browse through the list of structures, 
   via the next pointers, there should be 5 or 6 files and each file 
   should have some data.
   */

#ifdef VCAST_TEST_HARNESS_TRACE
vectorcast_write_to_std_out ("TracePoint B7: #2: Virtual File Structure Validation, ");
vectorcast_write_to_std_out ("The file offset table has the following data:\n");
vectorcast_write_to_std_out ("   Filename, Offset, Length\n");

{
struct FileData* localPtr = vcast_file_table;
int    i = 0;
char   local_string[VCAST_MAX_STRING_LENGTH];
while (i < NumFileTableEntries) {
   vectorcast_write_to_std_out ("   ");
   vectorcast_write_to_std_out (localPtr->name);
   vectorcast_write_to_std_out (", ");
   vcast_unsigned_to_string ( local_string, localPtr->offset);
   vectorcast_write_to_std_out (local_string);
   vectorcast_write_to_std_out (", ");
   vcast_unsigned_to_string ( local_string, localPtr->len);
   vectorcast_write_to_std_out (local_string);
   vectorcast_write_to_std_out ("\n");
   i++;
   localPtr++;
   }
}
vectorcast_write_to_std_out ("\n");
#endif

   /* ensure that printing buffer is clear */
   VCAST_memset(totalStringToPrint, '\0', VCAST_MAX_STRING_LENGTH);
}
#endif /* STDIO Version of initialize_io */
/* ------------------------------------------------------------------------------*/ 


/*------------------------------------------------------------------------------- 
-- vectorcast_local_print_string_stdio
---------------------------------------------------------------------------------*/
#ifdef VCAST_STDIO

static void vectorcast_local_print_string_stdio (int fpn, const char *str) {

  int lastCharacterIndex;
  int string_length = VCAST_strlen(str);

  /* if this is the beginning of a line */
  if (VCAST_strlen(totalStringToPrint) == 0) {
  
      struct FileData * fileData = GetFile(fpn);
      const char *filename ;
      if(fileData!=NULL) {
         filename = GetFileName(fileData);
      }
      else{ 
         /* File does not have an entry. May be a file opened for write */
         int lindex = fpn - NumFileTableEntries;
         if ((lindex >=0) && (lindex < VCAST_MAX_INTERNAL_FILES )) {
            filename = vcast_internal_file_names[lindex];
         }
         else {
            /* fpn out of range  */
            filename = (char*)NULL;
         }
      }
      /* Print newline since source code being tested may have just
       written to stdout. */

#if defined (__HC12__) && defined (__PRODUCT_HICROSS_PLUS__)
      vectorcast_write_to_std_out ("\r\n");
#else
      vectorcast_write_to_std_out ("\n");
#endif

      /* start the line with the file name */
      if (filename && VCAST_strlen(filename) > 0) {
         VCAST_strcpy(totalStringToPrint, filename);
         VCAST_strcat(totalStringToPrint, VCAST_FILE_NAME_SEPARATOR);
      }
  }
  
  if (VCAST_strlen(totalStringToPrint) + string_length >= VCAST_MAX_STRING_LENGTH) {
    vectorcast_write_to_std_out (totalStringToPrint);
    VCAST_memset(totalStringToPrint, '\0', VCAST_MAX_STRING_LENGTH);
    if (str[string_length - 1] == '\n') {
      vectorcast_write_to_std_out (str);
    } else {
      int vcast_index;
      for (vcast_index = 0; 
           vcast_index < string_length - 1; 
           vcast_index++){
        totalStringToPrint[0] = str[vcast_index];
        vectorcast_write_to_std_out(totalStringToPrint);
      }
      totalStringToPrint[0] = str[string_length - 1];
    }
  } else {
    /* add the string to the current contents of the line */
    VCAST_strcat(totalStringToPrint, str);

    /* if the string ends in a new line, print it */
    lastCharacterIndex = VCAST_strlen(totalStringToPrint) - 1;
    if (totalStringToPrint[lastCharacterIndex] == '\n'){

       vectorcast_write_to_std_out (totalStringToPrint);

       /* clear the buffer so it can be used for the next line */
       VCAST_memset(totalStringToPrint, '\0', VCAST_MAX_STRING_LENGTH);
    }
  }
}

#endif /* !defined (VCAST_STDIO) */
/* ------------------------------------------------------------------------------*/ 


#ifdef VCAST_COSMIC

/**************************************************************************
For Cosmic, we define an overload for putchar, and then we use a debug 
script to set a watchpoint on the label: VCAST_OUTCH.  When
the watchpoint is triggered, we clock out one character of data from the 
harness.  Unfortunately, the different versions of the debugger work
slightly differently.
**************************************************************************/

#ifndef VCAST_COSMIC_PUTCHAR_RETURN
#define VCAST_COSMIC_PUTCHAR_RETURN int
#endif

/* This is needed for the H12z compiler, that uses int for both params */
#ifndef VCAST_COSMIC_PUTCHAR_C
#define VCAST_COSMIC_PUTCHAR_C char
#endif

/* Variable watchpoint used for PPC */
unsigned int VCAST_COSMIC_OUTPUT_FLAG = 0;
char VCAST_COSMIC_OUTPUT_CHAR;

/* I have no idea why, but when I changed the parameter name to 'c' this did not
   work, so put it back to the old 'character' name */
VCAST_COSMIC_PUTCHAR_RETURN putchar(volatile VCAST_COSMIC_PUTCHAR_C character)
{
/* Label watchpoint used for HC12x */
   VCAST_COSMIC_OUTPUT_CHAR = character;
   VCAST_COSMIC_OUTPUT_FLAG++;
VCAST_OUTCH:
   return (character);
}
#endif


/* ------------------------------------------------------------------------------- 
-- vectorcast_write_to_std_out
---------------------------------------------------------------------------------*/

#if defined (VCAST_CUSTOM_OUTPUT)
/* Add a prototype for the user's function */
void VCAST_CUSTOM_OUTPUT (const char* S);
#endif

/* This function will check if the user has selected the 
   VCAST_DUMP_BUFFER option.  If they have, it will dump
   the buffer, clear the buffer, and return 1.
   
   Otherwise it will return 0
*/
int  vcast_check_for_dump_buffer (void) {

#if defined (VCAST_BUFFER_OUTPUT) && defined (VCAST_DUMP_BUFFER)

/* If we are buffering the output, _and_ want to dump it  */
/* This is useful on some targets, where a single "write" */
/* is faster than multiple writes */

#if defined (VCAST_USE_GH_SYSCALL)
    __ghs_syscall(0x40001, 1, vcast_output_buffer, vcast_output_buffer_end);
#elif defined (VCAST_CUSTOM_OUTPUT)
    VCAST_CUSTOM_OUTPUT (vcast_output_buffer);
#else
    printf ("%s\n", vcast_output_buffer);
#endif
    /* we clear the buffer in case this is an intermediate dump */
    vcast_clear_output_buffer ();
    return 1;
#else
   /* Not buffering, or dump option not selected */
   return 0;
#endif   /* VCAST_DUMP_BUFFER */

}

void vectorcast_write_to_std_out (const char *s) {
#if defined (VCAST_CUSTOM_OUTPUT)
   VCAST_CUSTOM_OUTPUT (s);
#elif defined (VCAST_STDIO_WITH_FILES)

   fprintf (vcast_stdout_file.fp, VCAST_PRINTF_STRING, s);

#elif defined (VCAST_BUFFER_OUTPUT)

#if defined (VCAST_OUTPUT_VIA_DEBUGGER)
    if ((vcast_output_buffer_end + VCAST_strlen (s)) >= VCAST_OUTPUT_BUFFER_SIZE)
    {
        vcastHaltDebugger();
        vcast_clear_output_buffer ();
    }
#endif

   /* If the buffer is full ... */
   if (!vcast_exit_flag &&
       (vcast_output_buffer_end + VCAST_strlen (s)) >= VCAST_OUTPUT_BUFFER_SIZE) {
      if (VCAST_strlen (s) >= VCAST_OUTPUT_BUFFER_SIZE){
         const char *vcast_cur = "vcast_output_buffer too small";
         int vcast_pos = 0;
         for (; *vcast_cur; ++vcast_cur) {
            vcast_output_buffer[vcast_pos++] = *vcast_cur;
         }
         vCAST_END();
      } else if (vcast_check_for_dump_buffer ()==0) {
         /* if dump is not selected, we cannot go on ... */
         const char *vcast_cur = "vcast_output_buffer overflow";
         int vcast_pos = 0;
         for (; *vcast_cur; ++vcast_cur) {
            vcast_output_buffer[vcast_pos++] = *vcast_cur;
         }
         vCAST_END();
      }
   }
   if (!vcast_exit_flag){
      VCAST_strcat(vcast_output_buffer, s);
      vcast_output_buffer_end = vcast_output_buffer_end + VCAST_strlen (s);
   }

#elif (VCAST_USE_GH_SYSCALL)

   int  Len = VCAST_strlen (s);
   char Str[VCAST_MAX_STRING_LENGTH];
   VCAST_strcpy (Str, s);

   /* we hardcode stdout (1) as the file handle */
   __ghs_syscall(0x40001, 1, Str, Len);

#elif defined (VCAST_PARADIGM)
   dprintf (VCAST_PRINTF_STRING, s);

#elif defined (VCAST_PARADIGM_SC520)
   COM * c1;
   int i, len;
   c1 = &ser1_com;

   len = VCAST_strlen(s);
   for( i=0; i<len; ++i ) {
      putser1( s[i], c1 );
   }
  
#elif defined (VCAST_RENESAS_SH_SIM)
   fprintf(renesas_stdout_file, s);
   
/***************************************************************************/
/* In the case of NEC 3619 on simulator, buffer capture is the method used */
/* The integration still uses VCAST_NEC_V850, but it is never used because */
/* the buffer capture is on                                                */
/***************************************************************************/
#elif defined (VCAST_NEC_V850)
   sendKLine(s);

#elif defined (VCAST_OUTPUT_VIA_DEBUGGER)
   vcastSingleLineOfOutput_Length = VCAST_strlen(s);
   VCAST_strcpy(vcastSingleLineOfOutput, s);
   vcastHaltDebugger();

#elif defined (VCAST_PIC24_TGT)
   int i, len;
   len = VCAST_strlen(s);
   for( i=0; i<len; ++i ) {
      UART2PutChar( s[i] );
   }

#elif defined (VCAST_KEIL_ARM_STM32_TGT)
   dprintf (VCAST_PRINTF_STRING, s);
 
 
#else

   printf (VCAST_PRINTF_STRING,s); 
   
#endif
}
/*------------------------------------------------------------------------------*/



/*------------------------------------------------------------------------------- 
---------------------------------------------------------------------------------*/
void StripReturn(char * line){
  int i;
  int lineLength = VCAST_strlen(line) - 1;
  for (i = lineLength; i >= 0; i--){
    if ((line[i] == '\n')||(line[i] == '\r'))
      line[i] = '\0';
    else
      break;
  }
}
/*-------------------------------------------------------------------------------*/



/*------------------------------------------------------------------------------- 
---------------------------------------------------------------------------------*/

char* vcast_get_filename(enum vcast_env_file_kind kind)
{
   switch (kind) {
#if defined (VCAST_STDIO) && defined (VCAST_FILE_INDEX)
      case VCAST_ASCIIRES_DAT:
         return ((char *)"1");
      case VCAST_EXPECTED_DAT:
         return ((char *)"2");
      case VCAST_TEMP_DIF_DAT:
         return ((char *)"3");
      case VCAST_TESTINSS_DAT:
         return ((char *)"4");
      case VCAST_THISTORY_DAT:
         return ((char *)"5");
      case VCAST_USERDATA_DAT:
         return ((char *)"6");
#else
      case VCAST_ASCIIRES_DAT:
         return ((char *)"ASCIIRES.DAT");
      case VCAST_EXPECTED_DAT:
         return ((char *)"EXPECTED.DAT");
      case VCAST_TEMP_DIF_DAT:
         return ((char *)"TEMP_DIF.DAT");
      case VCAST_TESTINSS_DAT:
         return ((char *)"TESTINSS.DAT");
      case VCAST_THISTORY_DAT:
         return ((char *)"THISTORY.DAT");
      case VCAST_USERDATA_DAT:
         return ((char *)"USERDATA.DAT");
#endif
      /* should never get here but some compilers
         require a return path for every possibility */
      default:
         return ((char *)"");
   }
} 
/*-------------------------------------------------------------------------------*/


/*------------------------------------------------------------------------------- 
-- vectorcast_read_from_std_in
---------------------------------------------------------------------------------*/
#if defined (VCAST_TASKING)
static void vectorcast_read_from_std_in_tasking (char *str);
#endif

/* Add a prototype for the user's function.  We define this
   to match the prototype for the stdio.h function "gets"
*/
#if defined (VCAST_CUSTOM_INPUT)
char* VCAST_CUSTOM_INPUT (char* S);  
#endif


static void vectorcast_read_from_std_in (char *str) {

   char local_string[VCAST_MAX_STRING_LENGTH];


#if defined (VCAST_CUSTOM_INPUT)
   VCAST_CUSTOM_INPUT (local_string);
   VCAST_strcpy (str, local_string);
   
   
#elif defined (VCAST_TASKING)
   vectorcast_read_from_std_in_tasking (str);

#elif defined (VCAST_STDOUT_ONLY)
   /* this function is not called in STDOUT mode */
   
#elif defined (VCAST_NO_STD_FILES)
   gets ( local_string );
   VCAST_strcpy (str, local_string);

#elif defined (VCAST_STDIO_WITH_FILES)
   fgets (local_string, VCAST_MAX_STRING_LENGTH, vcast_stdin_file.fp);
   VCAST_strcpy (str, local_string); 

#else
   fgets (local_string, VCAST_MAX_STRING_LENGTH, stdin); 
   VCAST_strcpy (str, local_string);
   
#endif

#ifdef VCAST_READ_LF
   /* Read off the extra LF */
   fgets (local_string, VCAST_MAX_STRING_LENGTH, stdin);
#endif
   StripReturn(str);
}
/*-------------------------------------------------------------------------------*/


/*------------------------------------------------------------------------------- 
-- vectorcast_read_from_std_in (TASKING ONLY)
---------------------------------------------------------------------------------*/
#ifdef VCAST_TASKING
static void vectorcast_read_from_std_in_tasking (char *str) {

   char local_string[VCAST_MAX_STRING_LENGTH];
   char newChar;
   int  newIndex;

   newIndex = 0;
   do {
      newChar = fgetc( stdin );

      local_string[newIndex] = newChar;
      local_string[++newIndex] = '\0';

   } while( newChar != '\n' && newChar != '\r' &&
          newIndex < VCAST_MAX_STRING_LENGTH-1 );
   VCAST_strcpy (str, local_string);
}
#endif /* VCAST_TASKING */
/*-------------------------------------------------------------------------------*/


/* ------------------------------------------------------------------------------- 
-- vectorcast_fopen
 ---------------------------------------------------------------------------------*/
int vectorcast_fopen( char *filename,  char *mode) {

#ifdef VCAST_TRACE32_TERM
  /* int ii; PJB CHECK - Unused? 
     char bbuffer[VCAST_MAX_STRING_LENGTH]; PJB CHECK - Unused? */
#endif

#ifdef VCAST_STDIO
  VCAST_STDIN_DATA_SIZE_T lCurrentOffset;
  unsigned int lInitialOffset;
  struct FileData * fileLink = 0;
  char * line;
  unsigned int len;
  if (VCAST_strcmp(mode, "r") == 0){
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
    unsigned int file_index;
    struct FileLocation *location;
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */

    /* look up file in list */
    fileLink = GetFileFromName(filename);
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
    file_index = fileLink - vcast_file_table;
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */
    /* Get Offset for start of file and Offset within file */
    if(GetFileOffsets(fileLink,&lInitialOffset,&lCurrentOffset) < 0){
      return VC_FAIL;
    };
    line = (char *)readLengthAndStringFromInputDataArray(lInitialOffset, &len);
#if defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus)
    location = vcast_file_offsets[file_index];
    vcast_file_offsets[file_index] 
      = (struct FileLocation *)VCAST_malloc(sizeof(struct FileLocation));
    vcast_file_offsets[file_index]->next = location;
#endif /* defined(VCAST_ALWAYS_DO_STUB_PROCESSING_IN_TI) && defined(__cplusplus) */
    SetFileOffset(fileLink, len + vcastLengthBytes);
    return GetIdNumber(fileLink);
  }
  else {
    if ( (VCAST_strcmp(mode, "w") == 0) ||
         (VCAST_strcmp(mode, "a") == 0) ){
      /* Return Descriptors for one of internal files */
      return (vcast_get_internal_fileid(filename));
    }
  }    
#else  /* not VCAST_STDIO */
   int i, j;

   /* Investigate as part of case: 24422 */
   if ((VCAST_strcmp(filename, "") != 0) && mode[0] == 'r'){
     for(i=0 ; i < VCAST_MAX_FILES-1; i ++ ) 
       {
#ifdef VCAST_TRACE32_TERM
         if (vcast_file_descriptor[i].handle && 
#else
         if (vcast_file_descriptor[i].fp && 
#endif
             VCAST_strcmp(vcast_file_descriptor[i].name, filename) == 0)
           {
             vcast_fpos_t vcast_pos;
#ifdef VCAST_TRACE32_TERM
             int vcast_pos_result = 0;

             vcast_pos = TRACE32_TERM_TellFile(vcast_file_descriptor[i].handle, &vcast_file_descriptor[i].iPos, &vcast_file_descriptor[i].fPos);

             if (vcast_pos == -1)
               vcast_pos_result = 1;

#else /* !defined (VCAST_TRACE32_TERM) */
             int vcast_pos_result = fgetpos (vcast_file_descriptor[i].fp, &vcast_pos);
#endif
             if (vcast_pos_result){
               vectorcast_print_string("Could not get file position for: ");
               vectorcast_print_string(filename);
               vectorcast_print_string("\n");
               vectorcast_fflush(VCAST_STDOUT);
             } else {
               vectorcast_save_position(&vcast_file_descriptor[i], vcast_pos);
             } /* if */
#ifdef VCAST_TRACE32_TERM
             vcast_pos_result = TRACE32_TERM_SeekFile(vcast_file_descriptor[i].handle, vcast_file_descriptor[i].start_pos, vcast_file_descriptor[i].iPos, vcast_file_descriptor[i].fPos);
#else
             vcast_pos_result = fsetpos (vcast_file_descriptor[i].fp, &vcast_file_descriptor[i].start_pos);
#endif             
             if (vcast_pos_result){
               vectorcast_print_string("Could not reset file position for: ");
               vectorcast_print_string(filename);
               vectorcast_print_string("\n");
               vectorcast_fflush(VCAST_STDOUT);
             } /* if */
             return i;
           } /* if */
       } /* for */
   } /* if */

   /* Back to back 20 iteration loops each time we call this */
   for(i=0 ; i < VCAST_MAX_FILES-1; i ++ ) 
   {
      if (vcast_file_descriptor[i].fp == 0)
      {
         j = i;
         i = VCAST_MAX_FILES;
       }
   }

   targetSpecificFileOpen (&vcast_file_descriptor[j], filename, mode);  

   /* If there is an error opening the file */
   if(vcast_file_descriptor[j].fp == 0) {
       vectorcast_print_string("Could not open file: ");
       vectorcast_print_string(filename);
       vectorcast_print_string("\n");
       vectorcast_fflush(VCAST_STDOUT);
       j = VC_FAIL;
    } 
    else if ((VCAST_strcmp(filename, "") != 0) && mode[0] == 'r') {
       VCAST_strcpy ( vcast_file_descriptor[j].name, filename);
#ifdef VCAST_TRACE32_TERM
       TRACE32_TERM_TellFile(vcast_file_descriptor[j].handle, &vcast_file_descriptor[j].iPos, &vcast_file_descriptor[j].fPos);
#else
       fgetpos (vcast_file_descriptor[j].fp, &vcast_file_descriptor[j].start_pos);
#endif
   }
   return j;
#endif /* !defined (VCAST_STDIO) */

  return VC_OK;
}

/* ------------------------------------------------------------------------------- 
-- vectorcast_fopen END
---------------------------------------------------------------------------------*/



/* ------------------------------------------------------------------------------- 
---------------------------------------------------------------------------------*/

/* ------------------------------------------------------------------------------- 
-- vectorcast_fclose
---------------------------------------------------------------------------------*/
void vectorcast_fclose(int fpn) {

#ifdef VCAST_STDIO
  struct FileData * file = GetFile(fpn);
  ResetCurrentLine(file);
#else
   if (vcast_file_descriptor[fpn].position_list != VCAST_NULL){
     vectorcast_restore_position(&vcast_file_descriptor[fpn]);
   } else {
#ifdef VCAST_TRACE32_TERM
   TRACE32_TERM_CloseFile(vcast_file_descriptor[fpn].handle);
   vcast_file_descriptor[fpn].handle = 0;
#else
   fclose(vectorcast_get_fd(fpn));
#endif
   vcast_file_descriptor[fpn].fp = (FILE *)VCAST_NULL;
   } /* if */
#endif /* !defined (VCAST_STDIO) */
}
/*------------------------------------------------------------------------------*/



/* ------------------------------------------------------------------------------- 
-- vectorcast_fgets
---------------------------------------------------------------------------------*/
char *vectorcast_fgets (char *line, int maxline, int fpn) {
#ifdef VCAST_STDIO
  /* check for end of file */
  if (vectorcast_feof(fpn))
    return (char *)NULL;
  else{
    struct FileData * file = GetFile(fpn);
    GetCurrentLine(file, line, maxline);
    AdvanceCurrentLine(file);
    return line;
  }
#else
#ifdef VCAST_TRACE32_TERM
  int lPos;

  lPos = TRACE32_TERM_ReadLineBigger(vcast_file_descriptor[fpn].handle, line, TRACE32_MAX_STRING_LENGTH);

  if (lPos == -1)
    TRACE32_TERM_Puts( "failed to read file\r\n" );

  if (lPos == 0)
    return NULL; /* EOF */

  return line;
#else /* !defined (VCAST_TRACE32_TERM) */
  char *rv = fgets(line, maxline, vectorcast_get_fd(fpn));
  /* rv will be NULL if an error occurs */
  if (rv){
    int len = VCAST_strlen(line);
    if (len >= 2 && line[len-1] == '\n' && line[len-2] == '\r'){
      line[len-2] = '\n';
      line[len-1] = '\0';
    }
  }
  return rv;
#endif /* !defined (VCAST_TRACE32_TERM) */
#endif /* !defined (VCAST_STDIO) */
}


/* ------------------------------------------------------------------------------- 
-- vectorcast_terminate_io
---------------------------------------------------------------------------------*/
void vectorcast_terminate_io (void){
#ifndef VCAST_NO_MALLOC
#ifdef VCAST_FREE_HARNESS_DATA
  struct VCAST_Allocated_Data *current_data_ptr = VCAST_allocated_data_list;
  struct VCAST_Allocated_Data *previous_data_ptr = VCAST_NULL;
  while (current_data_ptr != VCAST_NULL){
    previous_data_ptr = current_data_ptr;
    current_data_ptr = current_data_ptr->next;
    if (previous_data_ptr->allocated != VCAST_NULL) 
        VCAST_free(previous_data_ptr->allocated);
    VCAST_free(previous_data_ptr);
  }
  VCAST_allocated_data_list = VCAST_NULL;
#ifdef VCAST_SBF_UNITS_AVAILABLE
  vCAST_FREE_SBF_TABLE();
#endif
#endif
#endif

#ifdef VCAST_STDIO_WITH_FILES
  fclose (vcast_stdin_file.fp);
  fclose (vcast_stdout_file.fp);
#endif

#ifdef VCAST_RENESAS_SH_SIM
  fprintf(renesas_stdout_file,"\nVCAST.END:END");
  fclose(renesas_stdout_file);
#endif

#if defined ( VCAST_STDIO) && !defined(VCAST_STDOUT_ONLY)
#ifndef VCAST_NO_MALLOC
#ifdef VCAST_FREE_HARNESS_DATA
  VCAST_free(vcast_file_table);
#endif
#endif
#endif
}

/* ------------------------------------------------------------------------------- 
---------------------------------------------------------------------------------*/


/* ------------------------------------------------------------------------------- 
-- These two functions were added by SFC on Oct 29th 2008 in Change 41418 BugID: 8097
-- Comment: Since some compilers, such as Code Composer 55xx, cannot call 
--          fopen on an already open file, save the file position to avoid 
--          opening the file again.            
-- TBD:     Investiate as part of case: 24422
---------------------------------------------------------------------------------*/
 
#ifndef VCAST_STDIO
 static void vectorcast_save_position(struct FILES * vcast_file_ptr, 
                                       vcast_fpos_t vcast_position){
    struct Vcast_File_Position* vcast_new_file_position 
      = (struct Vcast_File_Position*)VCAST_malloc(sizeof(struct Vcast_File_Position));
    vcast_new_file_position->position = vcast_position;
    vcast_new_file_position->next = VCAST_NULL;
#ifdef VCAST_TRACE32_TERM
    vcast_new_file_position->iPos = 0;
    vcast_new_file_position->fPos = 0;
#endif
    if (vcast_file_ptr->position_list == VCAST_NULL){
      vcast_file_ptr->position_list = vcast_new_file_position;
    } else {
      struct Vcast_File_Position* vcast_current_file_position 
        = vcast_file_ptr->position_list;
      while (vcast_current_file_position->next != VCAST_NULL){
        vcast_current_file_position = vcast_current_file_position->next;
      } /* while */
      vcast_current_file_position->next = vcast_new_file_position;
    } /* if */
  } /* vectorcast_save_position */

  static void vectorcast_restore_position(struct FILES * vcast_file_ptr){
   
    struct Vcast_File_Position* vcast_current_file_position 
      = vcast_file_ptr->position_list;
    struct Vcast_File_Position* vcast_previous_file_position = VCAST_NULL; 
    while (vcast_current_file_position->next != VCAST_NULL){
      vcast_previous_file_position = vcast_current_file_position;
      vcast_current_file_position = vcast_current_file_position->next;
    } /* while */
#ifdef VCAST_TRACE32_TERM
    TRACE32_TERM_SeekFile(vcast_file_ptr->handle, vcast_current_file_position->position, vcast_current_file_position->iPos, vcast_current_file_position->fPos);
#else
    fsetpos (vcast_file_ptr->fp, &vcast_current_file_position->position);
#endif
    VCAST_free(vcast_current_file_position);
    if (vcast_previous_file_position){
      vcast_previous_file_position->next = VCAST_NULL;
    } else {
      vcast_file_ptr->position_list = VCAST_NULL;
    } /* if */
  } /* vectorcast_restore_position */
#endif
/*-------------------------------------------------------------------------------*/





/* See the notes in the file comment header for this flag */ 
#ifdef VCAST_OUTPUT_VIA_DEBUGGER
void vcastHaltDebugger(void)
{ 
   /* We reset the length to 0, this enables us to know if we
      are hitting this BP or the vCAST_END BP */
   vcastSingleLineOfOutput_Length = 0;
}
#endif

vCAST_boolean vCAST_FULL_STRINGS;             /* use full strings or not */
vCAST_boolean vCAST_HEX_NOTATION;             /* use hex notation or not */
vCAST_boolean vCAST_DO_COMBINATION_TESTING;   /* generate combination or not */


/*-------------------------------------------------------------------------------*/
/* BEGIN FLOATING POINT SUPPORT */
/*-------------------------------------------------------------------------------*/

#ifndef VCAST_NO_FLOAT

/*******************/
/* harness options */
/*******************/

/* default values */
int  VCAST_DEFAULT_FLOAT_PRECISION = 0;
int  VCAST_DEFAULT_FLOAT_FIELD_WIDTH = 0;
char VCAST_DEFAULT_FLOAT_FORMAT[VCAST_FLOAT_FORMAT_SIZE] = 
#if defined(VCAST_HAS_FLOAT128)
     /* this prints a '__float128' */
     { '%', 'Q', 'g', '\0', '\0', '\0', '\0', '\0' };
#else
#if defined(VCAST_ALLOW_LONG_DOUBLE) || !defined(VCAST_NO_LONG_DOUBLE)
     /* this prints a 'long double' */
     { '%', 'L', 'g', '\0', '\0', '\0', '\0', '\0' };
#else
     /* this prints a 'double' */
     { '%', 'g', '\0', '\0', '\0', '\0', '\0', '\0'};
#endif
#endif
/* actual values */
int  VCAST_FLOAT_PRECISION = 0;
int  VCAST_FLOAT_FIELD_WIDTH = 0;
char VCAST_FLOAT_FORMAT[VCAST_FLOAT_FORMAT_SIZE];

/* floating point special-case constants */
vCAST_long_double VCAST_globalZero = VCAST_FLOAT_ZERO;
vCAST_long_double VCAST_GET_QUIET_NAN ()
      { return VCAST_FLOAT_ZERO/VCAST_globalZero; }
vCAST_long_double VCAST_GET_NEGATIVE_INFINITY ()
      { return (-VCAST_FLOAT_ONE)/VCAST_globalZero; }
vCAST_long_double VCAST_GET_POSITIVE_INFINITY ()
      { return VCAST_FLOAT_ONE/VCAST_globalZero; }
      
#endif /* endif VCAST_NO_FLOAT */


/* 
------------------------------------------------------------------------------- 
   We only need vectorcast_float_to_string when float is defined and
   VCAST_FP or VCAST_NO_SPRINTF are defined 
------------------------------------------------------------------------------- 
*/

#ifndef VCAST_NO_FLOAT
#if defined(VCAST_FP) || defined(VCAST_NO_SPRINTF)

#define ASCII_CONVERSION 48
#define MAX_DIGITS 32
#define MAX_PRECISION 17
#define DEFAULT_PRECISION 6

#endif      /* endif defined(VCAST_FP) || defined(VCAST_NO_SPRINTF) */
#endif      /* endif VCAST_NO_FLOAT */

#if defined(VCAST_HAS_FLOAT128) && defined(VCAST_HAS_QUADMATH) && !defined(VCAST_FP) && !defined(VCAST_NO_SPRINTF)
#include <quadmath.h>
#endif /* endif defined(VCAST_HAS_FLOAT128) && defined(VCAST_HAS_QUADMATH) && !defined(VCAST_FP) && !defined(VCAST_NO_SPRINTF)*/

void vcast_float_to_string ( char *mixed_str, vCAST_long_double vcast_f ) {

#ifdef VCAST_NO_FLOAT

   /* if float not defined, then vCAST_long_double is an integer! */
   vcast_signed_to_string ( mixed_str, vcast_f );

#elif defined(VCAST_FP) || defined(VCAST_NO_SPRINTF)

   /* if we can't use sprintf, use our internal function */
   vectorcast_float_to_string ( mixed_str, vcast_f );

#else
#if defined(VCAST_HAS_FLOAT128) && defined(VCAST_HAS_QUADMATH)
   quadmath_snprintf(mixed_str, VCAST_MAX_STRING_LENGTH, VCAST_FLOAT_FORMAT, vcast_f);
#else
   /* otherwise just use sprintf */
   sprintf ( mixed_str, VCAST_FLOAT_FORMAT, vcast_f );
#endif /* endif defined(VCAST_HAS_FLOAT128) && defined(VCAST_HAS_QUADMATH) */
#endif      /* endif VCAST_NO_FLOAT */
} 

/* end FLOATING POINT SUPPORT */

#ifdef __cplusplus
} /* extern "C" */
#endif

