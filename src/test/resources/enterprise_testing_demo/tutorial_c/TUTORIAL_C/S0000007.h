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
#ifndef __VCAST_CCAST_IO_H__
#define __VCAST_CCAST_IO_H__

/* setup for "long long" capability */
#ifdef VCAST_HAS_LONGLONG

/* setup for Microsoft "long long" */
#ifdef VCAST_MICROSOFT_LONG_LONG
#define VCAST_LONGEST_INT __int64
#define VCAST_LONGEST_INT_FORMAT "%I64d"
#define VCAST_LONGEST_UINT_FORMAT "%I64u"

/* setup for other "long long" */
#else
#define VCAST_LONGEST_INT long long

/* setup for MinGW "long long" formats */
#ifdef __MINGW32__
#define VCAST_LONGEST_INT_FORMAT "%I64d"
#define VCAST_LONGEST_UINT_FORMAT "%I64u"

/* setup for other "long long" formats */
#else
#define VCAST_LONGEST_INT_FORMAT "%lld"
#define VCAST_LONGEST_UINT_FORMAT "%llu"
#endif

/* end "long long" formats */
#endif
/* end "long long" support */

/* no "long long" support */
#else
  #define VCAST_LONGEST_INT long
  #define VCAST_LONGEST_INT_FORMAT "%ld"
  #define VCAST_LONGEST_UINT_FORMAT "%lu"
#endif
/* end "long long" check */

#ifndef VCAST_UNSIGNED_CONVERSION_TYPE
#define VCAST_UNSIGNED_CONVERSION_TYPE unsigned VCAST_LONGEST_INT
#endif
#ifndef VCAST_SIGNED_CONVERSION_TYPE
#define VCAST_SIGNED_CONVERSION_TYPE VCAST_LONGEST_INT
#endif

#include "vcast_basics.h"

#ifndef VCAST_PRINTF_INTEGER 
#define VCAST_PRINTF_INTEGER "%d"
#endif
#ifndef VCAST_PRINTF_STRING
#define VCAST_PRINTF_STRING "%s"
#endif

#ifndef VCAST_PRINTF_LONG_DOUBLE
#if defined(VCAST_HAS_FLOAT128)
#define VCAST_PRINTF_LONG_DOUBLE "Qg"
#else
#if defined(VCAST_ALLOW_LONG_DOUBLE) || !defined(VCAST_NO_LONG_DOUBLE)
#define VCAST_PRINTF_LONG_DOUBLE "Lg"
#else
#define VCAST_PRINTF_LONG_DOUBLE "g"
#endif
#endif
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define VCAST_STDOUT -1


/* ----------------------------------------------------------------------------
-- These funtions are called at the start and end of the test harenss
-- and contain conditionally compiled code to setup for the particular I/O
-- mode and in some case the particular target.
-------------------------------------------------------------------------------*/
void vectorcast_initialize_io (int inst_status, int inst_fd);
void vectorcast_terminate_io (void);
void vectorcast_write_vcast_end (void);

int  vectorcast_fflush(int fpn);

void vectorcast_fclose(int fpn);
int  vectorcast_feof(int fpn);
int  vectorcast_fopen(char *filename, char *mode);
char *vectorcast_fgets (char *line, int maxline, int fpn);


/* return failure condition if the line we read is too long */
int vectorcast_readline(char *vcast_buf, int fpn);

void vectorcast_fprint_char   (int fpn, char vcast_str);
void vectorcast_fprint_char_hex ( int fpn, char vcast_value );
void vectorcast_fprint_char_octl ( int fpn, char vcast_value );

void vectorcast_fprint_string (int fpn, const char *vcast_str);
void vectorcast_fprint_string_with_cr (int fpn, const char *vcast_str);
void vectorcast_print_string (const char *vcast_str);
void vectorcast_fprint_string_with_length(int fpn, const char *vcast_str, int length);

void vectorcast_fprint_short     (int vcast_fpn, short vcast_value );
void vectorcast_fprint_integer   (int vcast_fpn, int vcast_value );
void vectorcast_fprint_long      (int vcast_fpn, long vcast_value );
void vectorcast_fprint_long_long (int vcast_fpn, VCAST_LONGEST_INT vcast_value );

void vectorcast_fprint_unsigned_short (int vcast_fpn,
                                       unsigned short vcast_value );
void vectorcast_fprint_unsigned_integer (int vcast_fpn,
                                         unsigned int vcast_value );
void vectorcast_fprint_unsigned_long (int vcast_fpn,
                                      unsigned long vcast_value );
void vectorcast_fprint_unsigned_long_long (int vcast_fpn,
                                           unsigned VCAST_LONGEST_INT vcast_value );

void vectorcast_fprint_long_float (int fpn, vCAST_long_double);

/* numeric conversion routines */
void vcast_signed_to_string ( char vcDest[],
                              VCAST_SIGNED_CONVERSION_TYPE vcSrc );
void vcast_unsigned_to_string ( char vcDest[],
                                VCAST_UNSIGNED_CONVERSION_TYPE vcSrc );
void vcast_float_to_string ( char *mixed_str, vCAST_long_double vcast_f );


/* ----------------------------------------------------------------------------
-- API for Harness Trace Functions
-----------------------------------------------------------------------------*/

/* To write output, the normal API is: vectorcast_print_string
   vectorcast_write_to_std_out should only be used for abnormal termination
   and debug trace messages */
void vectorcast_write_to_std_out (const char *s);




/*---------------------------------------------------------------------------*/


#ifndef VCAST_CHAR_HEX_FORMAT
#define VCAST_CHAR_HEX_FORMAT "%x"
#endif
#ifndef VCAST_CHAR_OCT_FORMAT
#define VCAST_CHAR_OCT_FORMAT "%o"
#endif
void vcast_char_to_based_string ( char vcDest[],
                                  unsigned char vcSrc,
                                  unsigned vcUseHex );

/* ----------------------------------------------------------------------------
-- To Save Output Size, for some targets using stdout mode, we output a 
-- number rather than a filename.  So for example, we put out: 
-- "1: data" instead of: "ASCIIRES.DAT: data"
-- JJP TBD: Not sure why this needs to be in the header --
-------------------------------------------------------------------------------*/
enum vcast_env_file_kind
{
   VCAST_ASCIIRES_DAT = 1,
   VCAST_EXPECTED_DAT = 2,
   VCAST_TEMP_DIF_DAT = 3,
   VCAST_TESTINSS_DAT = 4,
   VCAST_THISTORY_DAT = 5,
   VCAST_USERDATA_DAT = 6
};


/* Get the name of the file */
char *vcast_get_filename(enum vcast_env_file_kind kind);




/* ----------------------------------------------------------------------------
-- Need to evaluate these items
-- JJP TBD
-------------------------------------------------------------------------------*/
#ifdef VCAST_NO_STDIN
void vectorcast_set_index(int index, int fpn);
int vectorcast_get_index(int fpn);
#endif

#ifndef VCAST_CONDITION_TYP
#define VCAST_CONDITION_TYP VCAST_LONGEST_INT
#endif


#ifndef VCAST_NO_MALLOC
#ifdef VCAST_FREE_HARNESS_DATA
struct VCAST_Allocated_Data
{
  void *allocated;
  struct VCAST_Allocated_Data *next;
};
void VCAST_Add_Allocated_Data(void * vcast_data_ptr);
extern struct VCAST_Allocated_Data *VCAST_allocated_data_list;
#ifdef VCAST_SBF_UNITS_AVAILABLE
extern void vCAST_FREE_SBF_TABLE(void);
#endif
#else
#define VCAST_Add_Allocated_Data(vcast_data_ptr)
#endif
#endif


/* -------------------------------------------------------------------------------*/
/* -------------------------------------------------------------------------------*/


/* End of File, close the Extern C block */
#ifdef __cplusplus
} /* extern "C" */
#endif

#ifndef VCAST_NULL
#ifdef __cplusplus
#define VCAST_NULL (0)
#else
#define VCAST_NULL ((void*)0)
#endif
#endif

#endif /* End of Include Guard */
