/* 
 * This is an auto generated file, DO NOT EDIT.
 * 
 * This file contains the instrumentation configuration and
 * coverage buffers that will be used during the execution
 * of the instrumented executable.
 * 
 * Occasionally this file needs to be edited, for example,
 * to put a storage array,
 *   char vcast_unit_stmt_bytes_1[5] = { 0 };
 * into a specific location in memory
 *   char *var = (char*)0x40000000;
 * 
 * This file is written each time the tool configuration is updated
 * or a source file instrumented. To automate editing this file,
 * navagite in the GUI to
 * Tools -> Options -> Misc -> Post-process vcast_c_options.h command
 */
/***************************************************
** VectorCAST Test Harness Component **
** Copyright 2019 Vector Informatik, GmbH. **
****************************************************/
#ifndef __VCAST_C_OPTIONS_H__
#define __VCAST_C_OPTIONS_H__
#ifndef VCAST_MAX_COVERED_SUBPROGRAMS
#define VCAST_MAX_COVERED_SUBPROGRAMS 1000
#endif
#ifndef VCAST_MAX_MCDC_STATEMENTS
#define VCAST_MAX_MCDC_STATEMENTS  1000
#endif
#define VCAST_COVERAGE_IO_REAL_TIME
#ifndef VCAST_NUMBER_SUBPROGRAM_STRUCTS
#define VCAST_NUMBER_SUBPROGRAM_STRUCTS  1
#endif
#ifndef VCAST_NUMBER_BINARY_BYTES
#define VCAST_NUMBER_BINARY_BYTES  1
#endif
#ifndef VCAST_NUMBER_AVLTREE_POINTERS
#define VCAST_NUMBER_AVLTREE_POINTERS  1
#endif
#ifndef VCAST_NUMBER_STATEMENT_STRUCTS
#define VCAST_NUMBER_STATEMENT_STRUCTS  0
#endif
#ifndef VCAST_NUMBER_BRANCH_STRUCTS
#define VCAST_NUMBER_BRANCH_STRUCTS  0
#endif
#ifndef VCAST_NUMBER_MCDC_STRUCTS
#define VCAST_NUMBER_MCDC_STRUCTS  1
#endif
#ifndef VCAST_PROBE_POINTS_AVAILABLE
#define VCAST_PROBE_POINTS_AVAILABLE 1
#endif
#ifndef VCAST_NUMBER_STATEMENT_MCDC_STRUCTS
#define VCAST_NUMBER_STATEMENT_MCDC_STRUCTS 0
#endif
#ifndef VCAST_NUMBER_STATEMENT_BRANCH_STRUCTS
#define VCAST_NUMBER_STATEMENT_BRANCH_STRUCTS 0
#endif
#ifndef VCAST_HAS_LONGLONG
#define VCAST_HAS_LONGLONG
#endif
#define VCAST_COVERAGE_TYPE_STATEMENT
#ifndef VCAST_USE_OPTIMIZED_MCDC_INSTRUMENTATION
#define VCAST_USE_OPTIMIZED_MCDC_INSTRUMENTATION
#endif
#ifndef VCAST_OPTIMIZED_MCDC_STORAGE_THRESHOLD
#define VCAST_OPTIMIZED_MCDC_STORAGE_THRESHOLD  8
#endif

#ifndef VCAST_PACK_INSTRUMENTATION_STORAGE
#define VCAST_PACK_INSTRUMENTATION_STORAGE
#endif
/* An explanation for the number of bytes required for VectorCAST/Cover.
 * 
 * This represents the maximum amount of RAM that will be required
 * to record coverage data when 100% of your application is executed.
 * It is likely that only a percentage of this storage will be 
 * required during a single program execution.
 * 
 * Please note, when using the static memory option, all of this data 
 * must be reserved in the instrumented executable through global arrays.
 * When you are not using the static memory option, this data is not 
 * reserved in the instrumented executable, and it is allocated on 
 * demand through the use of the malloc system call.
 * 
 * When instrumenting for MCDC, the size of the variables 
 * mcdc_statement_pool and avlnode_pool are controlled with the option 
 * "Maximum MC/DC expressions". The default is set to 1000, 
 * so that a large test case can execute with out fear of 
 * over flowing these buffers. The number chosen (e.g. 1000) will 
 * provide storage for that many unique MC/DC expressions.
 * 
 * For a 16 bit executable configuration:
 *   statement bit array:.................size: 1          count: 6      bytes:  6
 *   Total (no MC/DC pool storage):......................................bytes:  6 *
 * 
 * For a 32 bit executable configuration:
 *   statement bit array:.................size: 1          count: 6      bytes:  6
 *   Total (no MC/DC pool storage):......................................bytes:  6 *
 * 
 * For a 64 bit executable configuration:
 *   statement bit array:.................size: 1          count: 6      bytes:  6
 *   Total (no MC/DC pool storage):......................................bytes:  6 *
 */
#ifndef VCAST_CONDITION_TYP
#define VCAST_CONDITION_TYP long long
#endif



#endif /* __VCAST_C_OPTIONS_H__ */
#ifdef VCAST_DEFINE_COVER_VARIABLES
#ifdef __cplusplus
extern "C" {
#endif
#ifdef VCAST_COVER_SHORT_ARRAY_INIT
char vcast_unit_stmt_bytes_9[6] = { 0 };
#elif VCAST_COVER_NO_INIT
char vcast_unit_stmt_bytes_9[6];
#else
char vcast_unit_stmt_bytes_9[6] = 
  {0,0,0,0,0,0};
#endif

#ifdef VCAST_COVERAGE_IO_BUFFERED
struct vcast_unit_list {
  int vcast_unit;
  char *vcast_statement_array;
  int vcast_statement_bytes;
};

#ifndef VCAST_UNIT_LIST_VALUES_ATTR
#define VCAST_UNIT_LIST_VALUES_ATTR
#endif

VCAST_UNIT_LIST_VALUES_ATTR
static const struct vcast_unit_list vcast_unit_list_values[] = {
  {9, (char *)vcast_unit_stmt_bytes_9, sizeof(vcast_unit_stmt_bytes_9)},
  {0, 0, 0}
};

#endif
#ifdef __cplusplus
}
#endif
#endif
