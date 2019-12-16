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

#ifndef __VCAST_B1_H__
#define __VCAST_B1_H__

#ifdef VCAST_VXWORKS
#include "vxWorks.h"
#endif
#include "S0000007.h"
#define VCAST_IN_DRIVER 1
#include "S0000002.h"
#undef VCAST_IN_DRIVER

/* "limits.h" and "float.h" has limits on base types
   If we don't use it, or some types do
   not have limits, define them here */
#ifndef VCAST_NO_LIMITS
#include <limits.h>
#ifndef VCAST_NO_FLOAT
#include <float.h>
#endif
#endif
#ifndef SCHAR_MAX
#define SCHAR_MAX 127
#endif
#ifndef UCHAR_MAX
#define UCHAR_MAX 255
#endif
#ifndef CHAR_MAX
#define CHAR_MAX UCHAR_MAX
#endif
#ifndef SHRT_MAX
#define SHRT_MAX 32767
#endif
#ifndef USHRT_MAX
#define USHRT_MAX 65535
#endif
#ifndef INT_MAX
#define INT_MAX  2147483647
#endif
#ifndef UINT_MAX
#define UINT_MAX 4294967295U
#endif
#ifndef LONG_MAX
#define LONG_MAX 2147483647L
#endif
#ifndef ULONG_MAX
#define ULONG_MAX 4294967295UL
#endif

#ifndef VCAST_NO_FLOAT
#ifndef FLT_MAX
#define FLT_MAX  3.402823466E+38F
#endif
#ifndef VCAST_FLT_MID
#define VCAST_FLT_MID 0
#endif
#ifndef DBL_MAX
#define DBL_MAX  1.7976931348623157E+308
#endif
#ifndef VCAST_DBL_MID
#define VCAST_DBL_MID 0
#endif
#ifndef LDBL_MAX
#define  LDBL_MAX 1.189731495357231765085759326628007016E+4932L
#endif
#ifndef VCAST_LDBL_MID
#define VCAST_LDBL_MID 0
#endif
#if defined(VCAST_HAS_FLOAT128)
#if defined(VCAST_HAS_QUADMATH)
#include <quadmath.h>
#endif
#ifndef FLT128_MAX
#define FLT128_MAX 1.18973149535723176508575932662800702e4932Q
#endif
#ifndef VCAST_FLT128_MID
#define VCAST_FLT128_MID 0
#endif
#endif
#endif

#ifndef SCHAR_MIN
#define SCHAR_MIN (-128)
#endif
#ifndef UCHAR_MIN
#define UCHAR_MIN 0
#endif
#ifndef CHAR_MIN
#define CHAR_MIN SCHAR_MIN
#endif
#ifndef CHAR_MIN
#define CHAR_MIN 0
#endif
#ifndef SHRT_MIN
#define SHRT_MIN (-32768)
#endif
#ifndef USHRT_MIN
#define USHRT_MIN 0
#endif
#ifndef INT_MIN
#define INT_MIN  (-2147483647-1)
#endif
#ifndef UINT_MIN
#define UINT_MIN 0
#endif
#ifndef LONG_MIN
#define LONG_MIN (-2147483647L-1L)
#endif
#ifndef ULONG_MIN
#define ULONG_MIN 0
#endif
#ifdef VCAST_HAS_LONGLONG
#ifdef VCAST_MICROSOFT_LONG_LONG
#ifndef _I64_MIN
#define _I64_MIN (-9223372036854775807i64 - 1)
#endif
#ifndef _I64_MAX
#define _I64_MAX 9223372036854775807i64
#endif
#ifndef _UI64_MAX
#define _UI64_MAX 0xffffffffffffffffui64
#endif
#else
#ifdef __GNUC__
#ifndef __LONG_LONG_MAX__
#define __LONG_LONG_MAX__ 9223372036854775807LL
#endif
#endif
#ifndef LLONG_MAX
#define LLONG_MAX 9223372036854775807LL
#endif
#ifndef LLONG_MIN
#define LLONG_MIN (-LLONG_MAX - 1LL)
#endif
#ifndef ULLONG_MAX
#define ULLONG_MAX 18446744073709551615ULL
#endif
#endif
#else
#ifndef LLONG_MAX
#define LLONG_MAX LONG_MAX
#endif
#ifndef LLONG_MIN
#define LLONG_MIN (-LONG_MAX - 1L)
#endif
#ifndef ULLONG_MAX
#define ULLONG_MAX ULONG_MAX
#endif
#endif

#ifdef __cplusplus
extern "C" {
#endif
/* FUNCTION PROTOTYPES */

void VCAST_get_indices(char *str_val, int *array_size);
#ifndef VCAST_NO_SIGNAL
void vCAST_signal(int sig);
#endif


void vcast_not_supported (void);
void vcast_get_range_value ( int *vCAST_FIRST_VAL,
                             int *vCAST_LAST_VAL,
                             int *vCAST_MORE_DATA);
int vcast_get_param (void);
int VCAST_FIND_INDEX (void);

/*------------------------------------------------------------------*/
#define settingRange()                                               \
   /* range is set when the data interface is run, not the driver */ \
((!vcast_is_in_driver) \
       /* if the current command is to get a first or last value */   \
 && ((vCAST_COMMAND == vCAST_FIRST_VAL) || (vCAST_COMMAND == vCAST_LAST_VAL)))

vCAST_double vCAST_power (short vcast_bits);
void VCAST_TI_BITFIELD ( VCAST_LONGEST_INT *vc_VAL, int Bits,  vCAST_boolean is_signed );
void VCAST_TI_STRING ( 
      char **vcast_param, 
      int vCAST_Size,
      int from_bounded_array,
      int size_of_bounded_array );
int vcast_add_to_hex(int previousNumber, char latestDigit);
char vcast_get_non_numerical_escape(char character);
int vcast_convert_size(char * input);

char * VCAST_convert(char * input);

/* ASCII value of the first char that can be displayed */
#define VCAST_FIRST_PRINTABLE 32
/* ASCII value of the last char that can be displayed */
#define VCAST_LAST_PRINTABLE 126

/**************************************************************************
Function: isUnprintable
Parameters: character - character to check
Description: This function returns true if the character it is given is
a nongraphical one. 
 *************************************************************************/
#define isUnprintable(character) \
 ((character < VCAST_FIRST_PRINTABLE)||(character > VCAST_LAST_PRINTABLE))

void vCAST_slice ( char vcast_target[], char source[], int vcast_first, int vcast_last );

vCAST_boolean vcast_proc_handles_command(int vc_m);
void VCAST_SET_GLOBAL_SIZE(unsigned int *vcast_size);
unsigned int *VCAST_GET_GLOBAL_SIZE(void);

/* EXTERNED VARIABLES */

extern int vCAST_FILE;


extern char vCAST_PARAMETER[VCAST_MAX_STRING_LENGTH];
extern char vCAST_PARAMETER_KEY[VCAST_MAX_STRING_LENGTH];

extern VCAST_LONGEST_INT vCAST_VALUE_INT;
extern VCAST_LONGEST_UNSIGNED vCAST_VALUE_UNSIGNED;
extern vCAST_long_double vCAST_VALUE;

extern int vCAST_PARAM_LENGTH;
extern int vCAST_INDEX;
extern int vCAST_DATA_FIELD;
extern int  *VCAST_index_size;
extern int  VCAST_index_count;
extern enum vCAST_COMMAND_TYPE vCAST_COMMAND;


extern vCAST_boolean vCAST_VALUE_NUL;
extern vCAST_boolean vCAST_SIZE;
extern vCAST_boolean vCAST_can_print_constructor;

struct VCAST_CSU_Data_Item
{
  void *vcast_item;
  char *vcast_command;
  struct VCAST_CSU_Data_Item *vcast_next;
};

struct VCAST_CSU_Data;

void VCAST_Add_CSU_Data (struct VCAST_CSU_Data **vcast_data, 
                         struct VCAST_CSU_Data_Item *vcast_data_item);
struct VCAST_CSU_Data_Item *VCAST_Get_CSU_Data ( 
                         struct VCAST_CSU_Data **vcast_data,
                         char *vcast_command);

#ifdef __cplusplus
}
#endif
#endif /* __VCAST_B1_H__ */
