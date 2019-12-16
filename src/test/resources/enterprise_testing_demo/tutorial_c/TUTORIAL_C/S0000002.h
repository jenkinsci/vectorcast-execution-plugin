#ifndef __S0000002_C__
#define __S0000002_C__
#include "S0000008.h"

#ifdef S0000002
int vCAST_ITERATION_COUNTERS [3][9];
#else
extern int vCAST_ITERATION_COUNTERS [3][9];
#endif
#include "vcast_basics.h"

#ifdef S0000002
#ifdef __cplusplus
extern "C" {
#endif
vCAST_array_boolean vCAST_GLOBALS_TOUCHED[6] = {
      vCAST_false, vCAST_false, vCAST_false, vCAST_false, vCAST_false, 
      vCAST_false
      };
#ifdef __cplusplus
} /* extern "C" */
#endif
#else
#ifdef __cplusplus
extern "C" {
#endif
extern vCAST_array_boolean vCAST_GLOBALS_TOUCHED[6];
#ifdef __cplusplus
} /* extern "C" */
#endif
#endif
enum vCAST_testcase_options_type {
        vCAST_MULTI_RETURN_SPANS_RANGE,
        vCAST_MULTI_RETURN_SPANS_COMPOUND_ITERATIONS,
        vCAST_DISPLAY_INTEGER_RESULTS_IN_HEX,
        vCAST_DISPLAY_FULL_STRING_DATA,
        vCAST_HEX_NOTATION_FOR_UNPRINTABLE_CHARS,
        vCAST_DO_COMBINATION,
        vCAST_REFERENCED_GLOBALS,
        vCAST_FLOAT_POINT_DIGITS_OF_PRECISION,
        vCAST_FLOAT_POINT_TOLERANCE,
        vCAST_EVENT_LIMIT,
        vCAST_GLOBAL_DATA_DISPLAY,
        vCAST_EXPECTED_BEFORE_UUT_CALL,
        vCAST_DATA_PARTITIONS,
        vCAST_SHOW_ONLY_DATA_WITH_EXPECTED_RESULTS,
        vCAST_SHOW_ONLY_EVENTS_WITH_EXPECTED_RESULTS};
#define vCAST_testcase_options_count 15
enum vCAST_globals_display_type {
        vCAST_EACH_EVENT,
        vCAST_RANGE_ITERATION,
        vCAST_SLOT_ITERATION,
        vCAST_TESTCASE};
/*
---------------------------------------------
-- Copyright 2019 Vector Informatik, GmbH. --
---------------------------------------------
*/

#ifdef VCAST_NO_TYPE_SUPPORT
#define VCAST_DISABLE_TI_STRING
#define VCAST_DISABLE_TI_BITFIELD
#define VCAST_NO_FLOAT
#endif


#ifdef VCAST_VXWORKS
#include "vxWorks.h"
#endif

#ifdef VCAST_INTEGRITY
#include <INTEGRITY.h>
#endif

#include "S0000004.h"

#include "VCAST_STRING.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef VCAST_NO_SETJMP

#if defined(__TASKING__) && defined(__CP166__) && defined(__CSETJMP_H) && defined(__NAMESPACES)
  using namespace std;
#endif

#ifndef VCAST_NO_SETJMP
#ifdef S0000002
 jmp_buf VCAST_env;
#else
 extern jmp_buf VCAST_env;
#endif
#endif
#endif

#ifdef __cplusplus
}
#endif

#ifdef __MINGW32__
#ifdef VCAST_MINGW32_LONG_DOUBLE
#define VCAST_ALLOW_LONG_DOUBLE 1
#endif
#endif


/* This define use to define the temporary strings that we use
   when we are breaking down the harness commands like: "0.0.3.4%0\n"
   We use 8 because we do not expect a unit, subprogram, parameter, or field
   to be larger than 999999\n\0
*/
#ifndef VCAST_LARGEST_COMMAND_FIELD
#define VCAST_LARGEST_COMMAND_FIELD 8
#endif

#ifndef VCAST_FILENAME_LENGTH
#define VCAST_FILENAME_LENGTH 13
#endif


#ifndef VCAST_MAX_STRING_LENGTH
#define VCAST_MAX_STRING_LENGTH 1000
#endif

typedef long int vCAST_BIG_INT;
enum    vCAST_COMMAND_TYPE { vCAST_SET_VAL,
                             vCAST_PRINT,
                             vCAST_FIRST_VAL,
                             vCAST_MID_VAL,
                             vCAST_LAST_VAL,
                             vCAST_POS_INF_VAL,
                             vCAST_NEG_INF_VAL,
                             vCAST_NAN_VAL,
                             vCAST_MIN_MINUS_1_VAL,
                             vCAST_MAX_PLUS_1_VAL,
                             vCAST_ZERO_VAL,
                             vCAST_KEEP_VAL,
                             vCAST_ALLOCATE,
                             vCAST_STUB_FUNCTION,
                             vCAST_FUNCTION }; 

struct vCAST_HIST_ENTRY {
  int VC_U;
  int VC_S;
};

struct vCAST_ORDER_ENTRY {
  int  VC_I;
  char VC_N[VCAST_FILENAME_LENGTH];
  char VC_T[VCAST_MAX_STRING_LENGTH];
  char VC_SLOT_DESCR[VCAST_MAX_STRING_LENGTH];
  char VC_PRINT_DATA[VCAST_MAX_STRING_LENGTH];
};

enum VCAST_RANGE_DATA_TYPE { 
   VCAST_NULL_TYPE = 0,
   VCAST_RANGE_TYPE,
   VCAST_LIST_TYPE
};





/* If the max range is not set by the user at all
   we default to 20 */
#ifndef VCAST_MAX_RANGE
#define VCAST_MAX_RANGE 20
#endif

/* if the user explicitly sets the max range to be 0,
   it means they don't want range processing at all */
#if VCAST_MAX_RANGE>0
struct vCAST_RANGE_DATA
{
  char  *vCAST_COMMAND;                       /* command */
  enum  VCAST_RANGE_DATA_TYPE vCAST_type;     /* Determines range, list or null */
  
  /* For Range */
  vCAST_long_double vCAST_MIN;                /* Min value */
  vCAST_long_double vCAST_MAX;                /* Max value */
  vCAST_long_double vCAST_INC;                /* Increment value */

  /* For List */
  char  *vCAST_list;                          /* The actual list values */

  /* Is integer */
  int isInteger;              /* 1 if min/mid/max is integer, 0 otherwise */

  int vCAST_COMBO_GROUPING; /* Number of times to repeat a command for combination testing before resetting */
  int vCAST_NUM_VALS;
};
#endif




/* vCAST function prototypes */
#ifdef __cplusplus
extern "C" {
#endif

vCAST_double      vCAST_power (short vcast_bits);
vCAST_long_double VCAST_itod  ( char vcastStringParam[] );


void vCAST_SET_TESTCASE_CONFIGURATION_OPTIONS( int VCAST_option,int VCAST_value, int VCAST_set_default);
void vCAST_SET_TESTCASE_OPTIONS ( char vcast_options[] );
void vCAST_RUN_DATA_IF (char VCAST_PARAM[], vCAST_boolean POST_CONSTRUCTOR_USER_CODE);
void vCAST_slice (char vcast_target[], char vcast_source[], int vcast_first, int vcast_last);
void vCAST_EXTRACT_DATA_FROM_COMMAND_LINE (char *vcast_buf, char VCAST_PARAM[], int VC_POSITION);
void vCAST_STR_TO_LONG_DOUBLE(char vcastStringParam[], vCAST_long_double * vcastFloatParam);
void vCAST_DOUBLE_TO_STR (vCAST_long_double VC_F, char VC_S[], int VC_AS_INT);
void vCAST_RESET_LIST_VALUES(void);
void vCAST_ITERATION_COUNTER_RESET(void);
void vCAST_RESET_ITERATION_COUNTERS(enum vCAST_testcase_options_type );
int  vCAST_GET_ITERATION_COUNTER_VALUE(int, int);
void vCAST_INCREMENT_ITERATION_COUNTER(int, int);
void vCAST_EXECUTE_RANGE_COMMANDS (int);
int vCAST_ITERATION_COUNTER_SWITCH(int);

/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0
void vCAST_GET_RANGE_VALUES(char *vcast_S, struct vCAST_RANGE_DATA *vcast_range_data);
#endif

                              
#ifdef VCAST_SBF_UNITS_AVAILABLE
void          vCAST_MODIFY_SBF_TABLE(int sbf_unit, int vcast_sub, vCAST_boolean stubbed);
#ifdef VCAST_FREE_HARNESS_DATA
void          vCAST_FREE_SBF_TABLE(void);
#endif
void          vCAST_INITIALIZE_SBF_TABLE(void);
vCAST_boolean vCAST_is_sbf(VCAST_LONGEST_INT vcast_unit, VCAST_LONGEST_INT vcast_sub);

typedef unsigned char vcast_sbf_object_type;
void VCAST_TI_SBF_OBJECT(vcast_sbf_object_type* vcast_param);
#endif

void vCAST_RESET_RANGE_VALUES (void);
void vCAST_INITIALIZE_RANGE_VALUES (void);
void vCAST_FREE_RANGE_VALUES(void);
void vCAST_STORE_GLOBAL_ASCII_DATA (void);
void vCAST_CREATE_EVENT_FILE (void);
void vCAST_CREATE_HIST_FILE (void);
void vCAST_OPEN_HIST_FILE (void);
void vCAST_CREATE_INST_FILE (void);
int  VCAST_test_name_cmp(char *vcast_tn);
long VCAST_convert_encoded_field(const char *vcast_str);

/* Code coverage-related functions */
void vCAST_CREATE_INST_FILE (void);
void VCAST_WRITE_TO_INST_FILE (const char VC_S[]);
#include "vcast_c_options.h"

void vCAST_SET_OUTPUT_TO_EVENT_FILE (void);
void vCAST_CLOSE_INST_FILE (void);

/* Coverage data */
#define VCAST_MAX_CONDITIONS 26

void vCAST_SET_OUTPUT_TO_EVENT_FILE (void);
void vCAST_CLOSE_INST_FILE (void);
void vCAST_CLOSE_EVENT_FILE (void);
void vCAST_CLOSE_HIST_FILE (void);
void vCAST_WRITE_END_FILE(void);
void vCAST_OPEN_E0_FILE (void);
void vCAST_OPEN_HARNOPTS_FILE(void);
void vCAST_RESET_HARNOPTS_FILE(void);
void vCAST_OPEN_TESTORDR_FILE (void);
void VCAST_READ_TESTORDER_LINE ( char[] );
void vCAST_STORE_ASCII_DATA ( int, int, char[] );

vCAST_boolean vCAST_READ_NEXT_ORDER (void);
vCAST_boolean vCAST_SHOULD_DISPLAY_GLOBALS ( int, char[] );

#ifdef S0000002

/* Leave these structures at the start of this file.  It reduces memory padding */
struct vCAST_ORDER_ENTRY vCAST_ORDER_OBJECT;

#if VCAST_MAX_RANGE>0
struct vCAST_RANGE_DATA vCAST_RANGE_COUNT[VCAST_MAX_RANGE];
#endif


int  vCAST_UNIT = 9;  /* default to UUT_INDEX */
int  vCAST_SUBPROGRAM = 1;
int  vCAST_CURRENT_SLOT;
int  vCAST_CURRENT_ITERATION;
int  vCAST_HIST_INDEX = 1;
int  vCAST_HIST_LIMIT = 1000;
int  vCAST_ENV_HIST_LIMIT = 1000;
char vCAST_TEST_NAME[VCAST_MAX_STRING_LENGTH];

vCAST_long_double vCAST_PARTITIONS = VCAST_FLOAT_ONE;

/* vCAST_array_boolean is defined as a char */
vCAST_array_boolean vCAST_TESTCASE_OPTIONS[vCAST_testcase_options_count] = 
                   { vCAST_false, vCAST_false, vCAST_false, vCAST_false };


#define VCAST_STDOUT -1
#define VCAST_STDIN  -2
#define VCAST_STDERR -3

int vCAST_INST_FILE;
/* file identificastion number for the ASCIIRES.DAT file */
int vCAST_EVENT_FILE;
/* file identification number for the THISTORY.DAT file */
int vCAST_HIST_FILE;
/* TESTORDR.DAT */
int vCAST_ORDER_FILE;
/* File that tells us what to print E000000n.DAT */
int vCAST_E0_FILE;
/* file identification number for the TEMP_DIF.DAT file */
int vCAST_OUTPUT_FILE;
/* File that user can use to write output */
int vcast_user_file;
/* File for the expected user code output */ 
int VCAST_EXP_FILE;

int  vCAST_COUNT;
int  vCAST_CURRENT_COUNT;

vCAST_boolean vCAST_DO_DATA_IF = 0;
vCAST_boolean vCAST_COMMAND_IS_MIN_MAX = vCAST_false;
vCAST_boolean vcast_is_in_union = vCAST_false;
vCAST_boolean vCAST_INST_FILE_OPEN = vCAST_false;

/* true if the ASCIIRES.DAT file is open */
vCAST_boolean vCAST_EVENT_FILE_OPEN = vCAST_false;

/* true if the THISTORY.DAT file is open */
vCAST_boolean vCAST_HIST_FILE_OPEN = vCAST_false;

vCAST_boolean vCAST_TOOL_ERROR = vCAST_false;
vCAST_boolean vCAST_HAS_RANGE = vCAST_false;
vCAST_boolean vCAST_SKIP_ITER = vCAST_false;
vCAST_boolean VCAST_DEFAULT_FULL_STRINGS      = 0;
vCAST_boolean VCAST_DEFAULT_HEX_NOTATION      = 0;
vCAST_boolean VCAST_DEFAULT_DO_COMBINATION    = 0;
vCAST_boolean vcast_is_in_driver = vCAST_false;
vCAST_boolean VCAST_GLOBAL_FIRST_EVENT = 1;


int    vCAST_NUM_RANGE_ITERATIONS = 1;
int    vCAST_RANGE_COUNTER = 0;

#ifdef VCAST_CPP_ENVIRONMENT
#ifndef VCAST_KEIL
int vcast_exception_index = -1;
int vcast_should_throw_exception = 0;
#endif
#endif


/* default harness options (from HARNOPTS.DAT) */
unsigned short VCAST_GLOBALS_DISPLAY = 0; /* when to capture global data */



#else  /* S0000002 Defined */


extern int    vcast_user_file;
extern int    VCAST_EXP_FILE;

extern int    vCAST_UNIT;
extern int    vCAST_SUBPROGRAM;
extern vCAST_boolean    vCAST_DO_DATA_IF;
extern char   vCAST_TEST_NAME[];
extern int    vCAST_CURRENT_SLOT;
extern int    vCAST_CURRENT_ITERATION;
extern int    vCAST_HIST_INDEX;
extern int    vCAST_HIST_LIMIT;
extern int    vCAST_ENV_HIST_LIMIT;
extern vCAST_boolean vCAST_HAS_RANGE;
extern vCAST_boolean vCAST_SKIP_ITER;
extern int    vCAST_NUM_RANGE_ITERATIONS;
extern int    vCAST_RANGE_COUNTER;

#if VCAST_MAX_RANGE>0
extern struct vCAST_RANGE_DATA vCAST_RANGE_COUNT[];
#endif

extern struct vCAST_ORDER_ENTRY vCAST_ORDER_OBJECT;

extern vCAST_long_double vCAST_PARTITIONS;
extern vCAST_boolean vCAST_COMMAND_IS_MIN_MAX;

extern int vCAST_INST_FILE;
/* file identificastion number for the ASCIIRES.DAT file */
extern int vCAST_EVENT_FILE;
/* file identification number for the THISTORY.DAT file */
extern int vCAST_HIST_FILE;
extern int vCAST_ORDER_FILE;
extern int vCAST_E0_FILE;
/* file identification number for the TEMP_DIF.DAT file */
extern int vCAST_OUTPUT_FILE;
extern int vCAST_COUNT;
extern int vCAST_CURRENT_COUNT;
extern vCAST_array_boolean vCAST_TESTCASE_OPTIONS[vCAST_testcase_options_count];
extern vCAST_boolean vcast_is_in_union;
extern vCAST_boolean vCAST_INST_FILE_OPEN;
/* true if the ASCIIRES.DAT file is open */
extern vCAST_boolean vCAST_EVENT_FILE_OPEN;
/* true if the THISTORY.DAT file is open */
extern vCAST_boolean vCAST_HIST_FILE_OPEN;

/* default harness options (from HARNOPTS.DAT) */
extern vCAST_boolean VCAST_DEFAULT_FULL_STRINGS;
extern vCAST_boolean VCAST_DEFAULT_HEX_NOTATION;
extern vCAST_boolean VCAST_DEFAULT_DO_COMBINATION;
extern unsigned short VCAST_GLOBALS_DISPLAY; /* when to capture global data */
extern vCAST_boolean VCAST_GLOBAL_FIRST_EVENT;
#endif  /* S0000002 defined */

extern vCAST_boolean vCAST_HEX_NOTATION;  /* use hex notation or not */
extern vCAST_boolean vCAST_DO_COMBINATION_TESTING;/* generate combination or not */
struct vCAST_ORDER_ENTRY* vCAST_ORDER(void);  

#ifndef VCAST_NO_SIGNAL
void vCAST_signal(int sig);
#endif
void VCAST_driver_termination(int vcast_status, int eventCode);


int vcast_get_hc_id (char *vcast_command);
void vcast_get_unit_id_str (char *vcast_command, char *vcast_unit);
int vcast_get_unit_id (char *vcast_command);
void vcast_get_subprogram_id_str (char *vcast_command, char *vcast_subprogram);
int vcast_get_subprogram_id (char *vcast_command);
void vcast_get_parameter_id_str (char *vcast_command, char *vcast_subprogram);
int vcast_get_parameter_id (char *vcast_command);
int vcast_get_percent_pos (char *vcast_command);
void vCAST_END(void);
void VCAST_SLOT_SEPARATOR ( int VC_EndOfSlot, char VC_SLOT_DESCR );
#ifdef __cplusplus
} /* extern "C" */
#endif
#endif /*__S0000002_C__*/
