/* VectorCAST/Cover */
#ifndef VCAST_CONDITION_TYP
#define VCAST_CONDITION_TYP long long
#endif


#ifdef __cplusplus
extern "C" {
#endif

/*
---------------------------------------------
-- Copyright 2019 Vector Informatik, GmbH. --
---------------------------------------------
*/

/* Defined variable usage for this file:

   Variable names that are indented apply only if the enclosing variable is set

   <<no defined variables set>>    : Output is written to the file TESTINSS.DAT

   VCAST_USE_STDOUT                : Output is written to stdout using puts
      VCAST_USE_PUTCHAR            : Output is written to stdout using putchar
      VCAST_USE_GH_SYSCALL         : Output is written to stdout using the GH syscall
                                     (For Green Hills INTEGRITY 178B)

   VCAST_CUSTOM_STD_OUTPUT         : custom user code required to write Output 
                                     to stdout or serial port interface
                                   : Need to also define VCAST_USE_STDOUT in order
                                     to set the stdout flag. 

   VCAST_CUSTOM_FILE_OUTPUT        : custom user file i/o code required to write 
                                     Output to TESTINSS.DAT 

   VCAST_USE_STD_STRING            : This define turns ON the use of memset from the
                                     system header string.h.

   VCAST_USE_STATIC_MEMORY         : No malloc is available, use alternate data.
      VCAST_MAX_MCDC_STATEMENTS    : The number of MCDC statement conditions 
                                     that can be reached when malloc is not 
                                     available. 
   VCAST_MAX_COVERED_SUBPROGRAMS   : The number of subprograms that may be
                                     covered. 
   VCAST_ENABLE_DATA_CLEAR_API     : Enable this macro to add the API
                                     VCAST_CLEAR_COVERAGE_DATA, which allows
                                     you to clear the currently collected
                                     coverage data during the execution of
                                     the instrumented executable.
   VCAST_ATTRIBUTE_CODE            : Allows the user to specify an attribute
                                     that will be placed before the ascii, 
                                     binary and subprogram coverage pool 
                                     global variables. This is useful for 
                                     putting this data in specific places
                                     in memory.
   VCAST_DUMP_CALLBACK             : If this is defined to a function name, 
                                     then when the user calls 
                                     VCAST_DUMP_COVERAGE_DATA, the function
                                     this was defined to will be called. The 
                                     purpose is to allow the users main loop
                                     to be given a chance to run within a 
                                     certain time frame.
   VCAST_FLUSH_DATA                : Use the flush system call after each
                                     string is written with fprintf in
                                     VCAST_WRITE_TO_INST_FILE_COVERAGE. The
                                     default is disabled. Define to any value
                                     to enable.
   VCAST_APPEND_WIN32_PID          : Append the process id to the TESTINSS.DAT
                                     file. This uses windows specific system
                                     calls to get the pid.
   VCAST_APPEND_POSIX_PID          : Append the process id to the TESTINSS.DAT
                                     file. This uses Posix specific system
                                     calls to get the pid.
   VCAST_APPEND_SECONDS_SINCE_EPOCH: Append the number of seconds since the
                                     epoch to the TESTINSS.DAT
                                     file. This uses the C library time() call.
*/

#ifndef __C_COVER_H__
#define __C_COVER_H__

#ifdef __cplusplus
extern "C" {
#endif

#ifndef VCAST_PRINTF_INTEGER 
#define VCAST_PRINTF_INTEGER "%d"
#endif
#ifndef VCAST_PRINTF_STRING
#define VCAST_PRINTF_STRING "%s"
#endif

#ifndef VCAST_MCDC_STORAGE_TYPE
#ifdef VCAST_UNSIGNED_LONG_MCDC_STORAGE
#define VCAST_MCDC_STORAGE_TYPE unsigned long
#else 
#ifdef VCAST_HAS_LONGLONG
#ifdef VCAST_MICROSOFT_LONG_LONG
#define VCAST_MCDC_STORAGE_TYPE unsigned __int64
#else 
#define VCAST_MCDC_STORAGE_TYPE unsigned long long
#endif /* VCAST_MICROSOFT_LONG_LONG */
#else 
#define VCAST_MCDC_STORAGE_TYPE unsigned long
#endif /* VCAST_HAS_LONGLONG */
#endif /* VCAST_UNSIGNED_LONG_MCDC_STORAGE */
#endif /* VCAST_MCDC_STORAGE_TYPE */

/*************************************/
/* CUDA-specific function decorators */
#ifdef __CUDA_ARCH__
#define VCAST_CUDA_HOST_DEVICE_FUNCTION __device__ __host__
#else
#define VCAST_CUDA_HOST_DEVICE_FUNCTION
#endif
/*************************************/

/* AVL prototypes */
typedef struct vcast_mcdc_statement* VCAST_elementType;
struct AVLNode;
typedef struct AVLNode *VCAST_position;
typedef struct AVLNode *AVLTree;
VCAST_position vcast_find (VCAST_elementType VCAST_X, AVLTree VCAST_T);
AVLTree vcast_insert (VCAST_elementType VCAST_X, AVLTree VCAST_T);

struct vcast_mcdc_statement
{
  VCAST_MCDC_STORAGE_TYPE mcdc_bits;
  VCAST_MCDC_STORAGE_TYPE mcdc_bits_used;
};
typedef struct vcast_mcdc_statement *vcast_mcdc_statement_ptr;

void VCAST_REGISTER_DUMP_AT_EXIT (void);
void VCAST_CLEAR_COVERAGE_DATA (void);
void VCAST_DUMP_COVERAGE_DATA (void);

int VCAST_WRITE_FN_CALL_ID(short unit, int id);
int VCAST_WRITE_STATEMENT_ID(short unit, int id);
int VCAST_WRITE_BRANCH_ID(short unit, int id, const char* TorF);
int VCAST_WRITE_OPTIMIZED_MCDC_ID(short unit, int id, char num_conditions,
                                  VCAST_MCDC_STORAGE_TYPE mcdc_bits, 
                                  VCAST_MCDC_STORAGE_TYPE mcdc_bits_used);
int VCAST_WRITE_FUNCTION_ID(short unit, int id);

#ifdef VCAST_COVERAGE_POINTS_AS_MACROS

/*
 * The instrumentation macros provide an alternative to the
 * instrumentation point functions, which are used by default. If you
 * need to add additional statements to the instrumentation points, we
 * suggest modifying the functions, and not the macros. First, ensure
 * the "Instrument using macros in c_cover.h" option is disabled and
 * then modify the functions of the same names in the c_cover_io.c file.
 *
 * The instrumentation point macros must be expressions or a
 * comma-separated list of expressions. To use them, enable the
 * "Instrument using macros in c_cover.h" option and reinstrument all
 * source files. Modification of the macros is allowed, but very
 * difficult.  You are strongly encouranged to verify coverage data when
 * using modified macros.
 */

#if defined(VCAST_PACK_INSTRUMENTATION_STORAGE)
#define VCAST_FN_CALL_INSTRUMENTATION_POINT_REALTIME(covdata, unit, id) (\
  ((covdata[id >> 3] & (((unsigned long)1) << id % 8)) == 0) ? \
  (covdata[id >> 3] |= (((unsigned long)1) << id % 8),         \
   VCAST_WRITE_FN_CALL_ID(unit, id)) : 1                       \
)
#else
#define VCAST_FN_CALL_INSTRUMENTATION_POINT_REALTIME(covdata, unit, id) (\
  (covdata[id] == 0) ? (covdata[id] = 1, VCAST_WRITE_FN_CALL_ID(unit, id)) : 1 \
)
#endif /* VCAST_PACK_INSTRUMENTATION_STORAGE */

/*************************************/
/* Use CUDA device-side code/buffer */
#ifdef __CUDA_ARCH__
#define VCAST_FN_CALL_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) (\
    atomicOr(&(((int*)(covdata ## _device))[id >> 5]),\
                (((unsigned long)1) << (id % 32)) ) )
/* Host-side code */
#else
#if defined(VCAST_PACK_INSTRUMENTATION_STORAGE)
#define VCAST_FN_CALL_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) (\
  covdata[id >> 3] |= (((unsigned long)1) << (id % 8)) \
)
#else
#define VCAST_FN_CALL_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) (\
  covdata[id] = 1 \
)
#endif /* VCAST_PACK_INSTRUMENTATION_STORAGE */
#endif /* __CUDA_ARCH__ */
/*************************************/


#define VCAST_FN_CALL_INSTRUMENTATION_POINT_ANIMATION(unit, id) (\
  VCAST_WRITE_FN_CALL_ID(unit, id) \
)

#if !defined(VCAST_PACK_INSTRUMENTATION_STORAGE)
#define VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(covdata, unit, id) (\
  (covdata[id] == 0) ? (covdata[id] = 1, VCAST_WRITE_STATEMENT_ID(unit, id)) : 1 \
)
#else
#define VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(covdata, unit, id) (\
  ((covdata[id >> 3] & (((unsigned long)1) << id % 8)) == 0) ? \
  (covdata[id >> 3] |= (((unsigned long)1) << id % 8), \
   VCAST_WRITE_STATEMENT_ID(unit, id)) : 1 \
)
#endif

/*************************************/
/* Use CUDA device-side code/buffer */
#ifdef __CUDA_ARCH__
#define VCAST_STATEMENT_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) \
   ( atomicOr(&(((int*)(covdata ## _device))[id >> 5]), (((unsigned long)1) << (id % 32))) )
/* Host-side code */
#else
#if !defined(VCAST_PACK_INSTRUMENTATION_STORAGE)
#define VCAST_STATEMENT_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) (\
  covdata[id] = 1 \
)
#else
#define VCAST_STATEMENT_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) (\
  covdata[id >> 3] |= (((unsigned long)1) << (id % 8)) \
)
#endif
#endif /* __CUDA_ARCH__ */
/*************************************/

#define VCAST_STATEMENT_INSTRUMENTATION_POINT_ANIMATION(unit, id) (\
  VCAST_WRITE_STATEMENT_ID(unit, id) \
)

/* The first half of a byte stores the true conditions, and the second
   half stores the false conditions.  Thus, the bits are indexed as:

   id  cond  bit
   1    T     1
   2    T     2
   3    T     3
   4    T     4
   1    F     5
   2    F     6
   3    F     7
   4    F     8
*/
#if !defined(VCAST_PACK_INSTRUMENTATION_STORAGE)
#define VCAST_BRANCH_INSTRUMENTATION_POINT_REALTIME(covdata, unit, id, condition) ( \
  condition ? \
  ((covdata[id * 2] == 0) ? (covdata[id * 2] = 1, VCAST_WRITE_BRANCH_ID(unit, id, "T"), 1) : 1) \
  : \
  ((covdata[id * 2 + 1] == 0) ? (covdata[id * 2 + 1] = 1, VCAST_WRITE_BRANCH_ID(unit, id, "F"), 0) : 0) \
)
#else
#define VCAST_BRANCH_INSTRUMENTATION_POINT_REALTIME(covdata, unit, id, condition) ( \
  condition ? \
  (((covdata[id >> 2] & (((unsigned long)1) << (id % 4 << 1))) == 0) ?       \
   (covdata[id >> 2] |= (((unsigned long)1) << (id % 4 << 1)),               \
    VCAST_WRITE_BRANCH_ID(unit, id, "T"), 1) : 1)      \
  : \
  (((covdata[id >> 2] & (((unsigned long)1) << ((id % 4 << 1) + 1))) == 0) ?   \
   (covdata[id >> 2] |= (((unsigned long)1) << ((id % 4 << 1) + 1)),       \
    VCAST_WRITE_BRANCH_ID(unit, id, "F"), 0) : 0)          \
)
#endif

/*************************************/
/* Use CUDA device-side code/buffer */
#ifdef __CUDA_ARCH__
#define VCAST_BRANCH_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id, condition) \
   ( condition ? \
      (atomicOr(&(((int*)(covdata ## _device))[id >> 4]), (((unsigned long)1) << (id % 16 << 1))), 1) \
      : \
      (atomicOr(&(((int*)(covdata ## _device))[id >> 4]), (((unsigned long)1) << ((id % 16 << 1) + 1))), 0) )
/* Host-side code */
#else
#if !defined(VCAST_PACK_INSTRUMENTATION_STORAGE)
#define VCAST_BRANCH_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id, condition) ( \
  condition ? \
  ((covdata[id * 2] == 0) ? (covdata[id * 2] = 1, 1) : 1) \
  : \
  ((covdata[id * 2 + 1] == 0) ? (covdata[id * 2 + 1] = 1, 0) : 0) \
)
#else
#define VCAST_BRANCH_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id, condition) (\
  condition ? \
  (covdata[id >> 2] |= (((unsigned long)1) << (id % 4 << 1)), 1)     \
  : \
  (covdata[id >> 2] |= (((unsigned long)1) << ((id % 4 << 1) + 1)), 0)     \
)
#endif

#endif
/*************************************/
#define VCAST_BRANCH_INSTRUMENTATION_POINT_ANIMATION(unit, id, condition, onPath) ( \
  condition ? \
  (VCAST_WRITE_BRANCH_ID(unit, id, onPath ? "T" : "TX"), 1)      \
  : \
  (VCAST_WRITE_BRANCH_ID(unit, id, onPath ? "F" : "FX"), 0)      \
)

#define VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_REALTIME_0(covdata, unit, id, condition) (\
  condition ?                                                                                \
  (((covdata[id >> 2] & ((1 << ((id << 1) % 8)) | (1 << (((id << 1) + 1) % 8)))) == 0) ?     \
    (covdata[id >> 2] |= ((1 << ((id << 1) % 8)) | (1 << (((id << 1) + 1) % 8))),            \
     VCAST_WRITE_OPTIMIZED_MCDC_ID(unit, id, 'a', (VCAST_MCDC_STORAGE_TYPE)1,(VCAST_MCDC_STORAGE_TYPE)1), 1) : 1) \
      :                                                                                      \
      (((covdata[id >> 2] & ((1 << ((id << 1) % 8)) | (0 << (((id << 1) + 1) % 8)))) == 0) ? \
       (covdata[id >> 2] |= ((1 << ((id << 1) % 8)) | (0 << (((id << 1) + 1) % 8))),         \
        VCAST_WRITE_OPTIMIZED_MCDC_ID(unit, id, 'a', (VCAST_MCDC_STORAGE_TYPE)0, (VCAST_MCDC_STORAGE_TYPE)1), 0) : 0) \
)

/*************************************/
/* Use CUDA device-side code/buffer */
#ifdef __CUDA_ARCH__
#define VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_0(covdata, unit, id, condition) \
   ( condition ? \
      (atomicOr(&(((int*)(covdata ## _device))[id >> 8]), (((unsigned long)1) << (id % 32 << 1))), 1) \
      : \
      (atomicOr(&(((int*)(covdata ## _device))[id >> 8]), (((unsigned long)1) << ((id % 32 << 1) + 1))), 0) )
/* Host-side code */
#else
#define VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_0(covdata, unit, id, condition) (\
  condition ?                                                                      \
  (covdata[id >> 2] |= ((1 << ((id << 1) % 8)) | (1 << (((id << 1) + 1) % 8))), 1) \
  :                                                                                \
  (covdata[id >> 2] |= ((1 << ((id << 1) % 8)) | (0 << (((id << 1) + 1) % 8))), 0) \
)
#endif
/*************************************/

#if defined(VCAST_PACK_INSTRUMENTATION_STORAGE)
#define VCAST_FUNCTION_INSTRUMENTATION_POINT_REALTIME(covdata, unit, id) (\
  ((covdata[id >> 3] & (((unsigned long)1) << id % 8)) == 0)?\
  (covdata[id >> 3] |= (((unsigned long)1) << id % 8), \
   VCAST_WRITE_FUNCTION_ID(unit, id), 0) : 0 \
)
#else
#define VCAST_FUNCTION_INSTRUMENTATION_POINT_REALTIME(covdata, unit, id) (\
  (covdata[id] == 0) ? (covdata[id] = 1, VCAST_WRITE_FUNCTION_ID(unit, id)) : 0 \
)
#endif /* VCAST_PACK_INSTRUMENTATION_STORAGE */

/*************************************/
/* Use CUDA device-side code/buffer */
#ifdef __CUDA_ARCH__
#define VCAST_FUNCTION_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) \
   ( atomicOr(&(((int*)(covdata ## _device))[id >> 5]), \
   (((unsigned long)1) << (id % 32))) )
/* Host-side code */
#else
#if defined(VCAST_PACK_INSTRUMENTATION_STORAGE)
#define VCAST_FUNCTION_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) (\
  covdata[id >> 3] |= (((unsigned long)1) << (id % 8)) \
)
#else
#define VCAST_FUNCTION_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) (\
  covdata[id] = 1 \
)
#endif /* VCAST_PACK_INSTRUMENTATION_STORAGE */
#endif /* __CUDA_ARCH__ */
/*************************************/

#define VCAST_FUNCTION_INSTRUMENTATION_POINT_ANIMATION(unit, id) (\
  VCAST_WRITE_FUNCTION_ID(unit, id) \
)

#else

int VCAST_FN_CALL_INSTRUMENTATION_POINT_REALTIME(char* covdata, int unit, int id);
VCAST_CUDA_HOST_DEVICE_FUNCTION
int VCAST_FN_CALL_INSTRUMENTATION_POINT_BUFFERED(char* covdata, int unit, int id);
int VCAST_FN_CALL_INSTRUMENTATION_POINT_ANIMATION(int unit, int id);
int VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(char* covdata, int unit, int id);
VCAST_CUDA_HOST_DEVICE_FUNCTION
int VCAST_STATEMENT_INSTRUMENTATION_POINT_BUFFERED(char* covdata, int unit, int id);
int VCAST_STATEMENT_INSTRUMENTATION_POINT_ANIMATION(int unit, int id);
int VCAST_BRANCH_INSTRUMENTATION_POINT_REALTIME(char* covdata, int unit, int id, VCAST_CONDITION_TYP condition);
VCAST_CUDA_HOST_DEVICE_FUNCTION
int VCAST_BRANCH_INSTRUMENTATION_POINT_BUFFERED(char* covdata, int unit, int id, VCAST_CONDITION_TYP condition);
int VCAST_BRANCH_INSTRUMENTATION_POINT_ANIMATION(int unit, int id, VCAST_CONDITION_TYP condition, int onPath);
VCAST_CONDITION_TYP 
VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_REALTIME_0(char* covdata,
                                                            int unit, int id,
                                                            VCAST_CONDITION_TYP condition);
VCAST_CUDA_HOST_DEVICE_FUNCTION
VCAST_CONDITION_TYP 
VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_0(char* covdata,
                                                            int unit, int id,
                                                            VCAST_CONDITION_TYP condition);
int VCAST_FUNCTION_INSTRUMENTATION_POINT_REALTIME(char* covdata, int unit, int id);
VCAST_CUDA_HOST_DEVICE_FUNCTION
int VCAST_FUNCTION_INSTRUMENTATION_POINT_BUFFERED(char* covdata, int unit, int id);
int VCAST_FUNCTION_INSTRUMENTATION_POINT_ANIMATION(int unit, int id);

/***************************************************************************/
/* For CUDA, remap calls on device side to use device-specific buffer name */
/* See corresponding #undefs in c_cover_io.c */ 
#ifdef __CUDA_ARCH__
#define VCAST_FN_CALL_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) VCAST_FN_CALL_INSTRUMENTATION_POINT_BUFFERED(covdata ## _device, unit, id)
#define VCAST_STATEMENT_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) VCAST_STATEMENT_INSTRUMENTATION_POINT_BUFFERED(covdata ## _device, unit, id)
#define VCAST_BRANCH_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id, condition) VCAST_BRANCH_INSTRUMENTATION_POINT_BUFFERED(covdata ## _device, unit, id, condition)
#define VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_0(covdata, unit, id, condition) VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_0(covdata ## _device, unit, id, condition)
#define VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED(covdata, statement, unit, id, num_conditions, condition) VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED(covdata ## _device, statement, unit, id, num_conditions, condition)
#define VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_1(covdata, statement, unit, id, condition) VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_1(covdata ## _device, statement, unit, id, condition)
#define VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_2(covdata, statement, unit, id, condition) VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_2(covdata ## _device, statement, unit, id, condition)
#define VCAST_FUNCTION_INSTRUMENTATION_POINT_BUFFERED(covdata, unit, id) VCAST_FUNCTION_INSTRUMENTATION_POINT_BUFFERED(covdata ## _device, unit, id)
#endif
/***************************************************************************/

#endif /* VCAST_COVERAGE_POINTS_AS_MACROS */

VCAST_CUDA_HOST_DEVICE_FUNCTION
VCAST_CONDITION_TYP
VCAST_SAVE_MCDC_SUBCONDITION (struct vcast_mcdc_statement *mcdc_statement,
                              int bit_index, VCAST_CONDITION_TYP condition);

VCAST_CONDITION_TYP 
VCAST_MCDC_CONDITION_INSTRUMENTATION_POINT_REALTIME(AVLTree *vcast_mcdc_array,
                                                    struct vcast_mcdc_statement *mcdc_statement,
                                                    int unit, int id,
                                                    VCAST_CONDITION_TYP condition);

VCAST_CONDITION_TYP 
VCAST_MCDC_CONDITION_INSTRUMENTATION_POINT_ANIMATION(struct vcast_mcdc_statement *mcdc_statement,
                                                     int unit, int id, VCAST_CONDITION_TYP condition);

VCAST_CUDA_HOST_DEVICE_FUNCTION
VCAST_CONDITION_TYP 
VCAST_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED(AVLTree *vcast_mcdc_array,
                                                    struct vcast_mcdc_statement *coverage,
                                                    int unit, int id,
                                                    VCAST_CONDITION_TYP condition);

VCAST_CONDITION_TYP 
VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_REALTIME_1(char* covdata,
                                                            struct vcast_mcdc_statement *mcdc_statement,
                                                            int unit, int id,
                                                            VCAST_CONDITION_TYP condition);
VCAST_CONDITION_TYP 
VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_REALTIME(char* covdata,
                                                          struct vcast_mcdc_statement *mcdc_statement,
                                                          int unit, int id,
                                                          int num_conditions,
                                                          VCAST_CONDITION_TYP condition);

VCAST_CUDA_HOST_DEVICE_FUNCTION
VCAST_CONDITION_TYP 
VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_1(char* covdata,
                                                            struct vcast_mcdc_statement *mcdc_statement,
                                                            int unit, int id,
                                                            VCAST_CONDITION_TYP condition);
VCAST_CUDA_HOST_DEVICE_FUNCTION
VCAST_CONDITION_TYP 
VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED_2(char* covdata,
                                                            struct vcast_mcdc_statement *mcdc_statement,
                                                            int unit, int id,
                                                            VCAST_CONDITION_TYP condition);

VCAST_CUDA_HOST_DEVICE_FUNCTION
VCAST_CONDITION_TYP 
VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_BUFFERED(char* covdata,
                                                          struct vcast_mcdc_statement *mcdc_statement,
                                                          int unit, int id,
                                                          int num_conditions,
                                                          VCAST_CONDITION_TYP condition);

VCAST_CONDITION_TYP 
VCAST_OPTIMIZED_MCDC_CONDITION_INSTRUMENTATION_POINT_ANIMATION(struct vcast_mcdc_statement *mcdc_statement,
                                                           int unit, int id,
                                                           int num_conditions,
                                                           VCAST_CONDITION_TYP condition);
                                                           
                                                           
int vcastDataCoupleRead (int vc_probeIndex);
int vcastDataCoupleWrite (
       int vc_probeIndex,  
       int vc_dataType,  
       void* vc_value);   
int vcastControlCoupleCall (int vc_probeIndex);
void vcastDumpCouplingData (void);
void vcastCouplingDataInit (void);
int vcastControlCoupleTargetCall (int vc_probeIndex);
int vcastControlCoupleFptrCall (int probeIndex, void *fnCall, void *fnTarget);
int vcastControlCoupleVrtlCall (int vc_probeIndex);
                                                           
int vcast_strlen(const char *VC_S);
char * vcast_strcat (char *VC_S, const char *VC_T);
char * vcast_int_to_string (char *buf, VCAST_MCDC_STORAGE_TYPE vc_x);
void VCAST_WRITE_TO_INST_FILE_COVERAGE (const char *S, int flush);
                                                           

#ifndef VCAST_UNIT_TEST_TOOL

#ifdef VCAST_HAS_LONGLONG
  #ifdef VCAST_MICROSOFT_LONG_LONG
  #define VCAST_LONGEST_INT __int64
  #else
  #define VCAST_LONGEST_INT long long
  #endif
#else
  #define VCAST_LONGEST_INT long
#endif

#ifndef VCAST_UNSIGNED_CONVERSION_TYPE
#define VCAST_UNSIGNED_CONVERSION_TYPE unsigned VCAST_LONGEST_INT
#endif
#ifndef VCAST_SIGNED_CONVERSION_TYPE
#define VCAST_SIGNED_CONVERSION_TYPE VCAST_LONGEST_INT
#endif

/* types */
/* ifdef VCAST_NO_FLOAT */
#ifdef VCAST_NO_FLOAT
typedef long vCAST_double;
typedef long vCAST_long_double;
/* else */
#else
typedef double vCAST_double;
/* ifdef VCAST_ALLOW_LONG_DOUBLE */
#if defined(VCAST_ALLOW_LONG_DOUBLE) || !defined(VCAST_NO_LONG_DOUBLE)
typedef long double vCAST_long_double;
#else
typedef double vCAST_long_double;
#endif
/* endif VCAST_ALLOW_LONG_DOUBLE */
#endif
/* endif VCAST_NO_FLOAT */

VCAST_LONGEST_INT vcast_abs ( VCAST_LONGEST_INT vcNum );
void vectorcast_signed_to_string ( char vcDest[], VCAST_LONGEST_INT vcSrc );
int VCAST_special_compare ( char *vcDouble1, char *vcDouble2, int vcLen );
void vectorcast_strcpy ( char *VC_S, const char *VC_T );
void vectorcast_float_to_string( char *mixed_str, vCAST_long_double f );

/* This macro is always defined, because, it "tells" the c_cover_io.c 
   file that the prototypes are available to it. */
#define VCAST_PROBE_PRINT_AVAILABLE

void vcast_probe_print (const char *S);
void vcast_probe_print_int (VCAST_SIGNED_CONVERSION_TYPE i);
void vcast_probe_print_unsigned (VCAST_UNSIGNED_CONVERSION_TYPE i);
void vcast_probe_print_float (vCAST_long_double f);

#endif /* VCAST_UNIT_TEST_TOOL */

#ifdef __cplusplus
}
#endif

#endif

#ifdef __cplusplus
}
#endif
#ifdef __cplusplus
extern "C" {
#endif
extern char vcast_unit_stmt_bytes_9[];
#ifdef __cplusplus
} /* end extern */
#endif
/*vcast_header_expansion_start:C:/VCAST/2019sp3/tutorial/c/ctypes.h*/
typedef float FLOAT;
enum boolean { v_false, v_true };
enum soups { NO_SOUP, ONION, CHOWDER };
enum salads { NO_SALAD, CAESAR, GREEN };
enum entrees { NO_ENTREE, STEAK, CHICKEN, LOBSTER, PASTA };
enum desserts { NO_DESSERT, CAKE, PIE, FRUIT };
enum beverages { NO_BEVERAGE, WINE, BEER, MIXED_DRINK, SODA };
struct order_type
{
  enum soups Soup;
  enum salads Salad;
  enum entrees Entree;
  enum desserts Dessert;
  enum beverages Beverage;
};
typedef unsigned short seat_index_type;
typedef unsigned short table_index_type;
struct table_data_type
{
  enum boolean Is_Occupied;
  seat_index_type Number_In_Party;
  char Designator;
  char Wait_Person[10];
  struct order_type Order[4];
  FLOAT Check_Total;
};
typedef char name_type[32];
/*vcast_header_expansion_end*/
struct table_data_type Get_Table_Record(table_index_type Table);
void Update_Table_Record(table_index_type Table, struct table_data_type Data);
/* Allow 10 Parties to wait */
static name_type WaitingList[10];
static unsigned int WaitingListSize = 0;
static unsigned int WaitingListIndex = 0;
/* This function will add a free dessert to specific orders based on the 
   entree, salad, and beverage choice */
void Add_Included_Dessert(struct order_type* Order)
{/*vcast_internal_start*/
extern struct order_type *P_9_1_1;
extern unsigned char SBF_9_1;
if(SBF_9_1) {
  vCAST_USER_CODE_TIMER_STOP();
#define BEGINNING_OF_STUB_USER_CODE_9_1220426544
#include "vcast_configure_stub.c"
#undef BEGINNING_OF_STUB_USER_CODE_9_1220426544
  if ( vcast_is_in_driver ) {
    P_9_1_1 = Order;
    vCAST_COMMON_STUB_PROC_9( 9, 1, 2, 0 );
  } /* vcast_is_in_driver */
#define END_OF_STUB_USER_CODE_9_1220426544
#include "vcast_configure_stub.c"
#undef END_OF_STUB_USER_CODE_9_1220426544
  vCAST_USER_CODE_TIMER_START();
  return;
}
/*vcast_internal_end*/
/*vcast_internal_start*/{/*vcast_internal_end*/
  {
{
VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,0);
if(Order->Entree == STEAK &&
     Order->Salad == CAESAR &&
     Order->Beverage == MIXED_DRINK) {
    VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,1);
Order->Dessert = PIE;
  } else {
{
if((VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,2),Order->Entree == LOBSTER &&
            Order->Salad == GREEN &&
            Order->Beverage == WINE)) {
    VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,3);
Order->Dessert = CAKE;
  }}
}
}
}
/*vcast_internal_start*/}/*vcast_internal_end*/
}
int Place_Order(table_index_type Table,
                seat_index_type Seat,
                struct order_type Order)
{/*vcast_internal_start*/
extern unsigned short P_9_2_1;
extern unsigned short P_9_2_2;
extern struct order_type P_9_2_3;
extern int R_9_2;
extern unsigned char SBF_9_2;
if(SBF_9_2) {
  vCAST_USER_CODE_TIMER_STOP();
#define BEGINNING_OF_STUB_USER_CODE_9_453126248
#include "vcast_configure_stub.c"
#undef BEGINNING_OF_STUB_USER_CODE_9_453126248
  if ( vcast_is_in_driver ) {
    P_9_2_1 = Table;
    P_9_2_2 = Seat;
    P_9_2_3 = Order;
    vCAST_COMMON_STUB_PROC_9( 9, 2, 4, 0 );
  } /* vcast_is_in_driver */
#define END_OF_STUB_USER_CODE_9_453126248
#include "vcast_configure_stub.c"
#undef END_OF_STUB_USER_CODE_9_453126248
  vCAST_USER_CODE_TIMER_START();
  return R_9_2;
}
/*vcast_internal_end*/
/*vcast_internal_start*/{/*vcast_internal_end*/
  struct table_data_type Table_Data;
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,4);
Table_Data = Get_Table_Record(Table);
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,5);
Table_Data.Is_Occupied = v_true;
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,6);
Table_Data.Number_In_Party = Table_Data.Number_In_Party + 1;
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,7);
Table_Data.Order[Seat] = Order;
  /* Add a free dessert in some cases */
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,8);
Add_Included_Dessert(&Table_Data.Order[Seat]);
  {
VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,9);
switch(Order.Entree)
    {
    case NO_ENTREE :
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,10);
break;
    case STEAK :
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,11);
Table_Data.Check_Total = Table_Data.Check_Total + 14.0;
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,12);
break;
    case CHICKEN :
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,13);
Table_Data.Check_Total = Table_Data.Check_Total + 10.0;
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,14);
break;
    case LOBSTER :
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,15);
Table_Data.Check_Total = Table_Data.Check_Total + 18.0;
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,16);
break;
    case PASTA :
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,17);
Table_Data.Check_Total = Table_Data.Check_Total + 12.0;
       VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,18);
break;
    }}
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,19);
Update_Table_Record(Table, Table_Data);
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,20);
return 0;
/*vcast_internal_start*/}/*vcast_internal_end*/
}
int Clear_Table(table_index_type Table)
{/*vcast_internal_start*/
extern unsigned short P_9_3_1;
extern int R_9_3;
extern unsigned char SBF_9_3;
if(SBF_9_3) {
  vCAST_USER_CODE_TIMER_STOP();
#define BEGINNING_OF_STUB_USER_CODE_9_2460377677
#include "vcast_configure_stub.c"
#undef BEGINNING_OF_STUB_USER_CODE_9_2460377677
  if ( vcast_is_in_driver ) {
    P_9_3_1 = Table;
    vCAST_COMMON_STUB_PROC_9( 9, 3, 2, 0 );
  } /* vcast_is_in_driver */
#define END_OF_STUB_USER_CODE_9_2460377677
#include "vcast_configure_stub.c"
#undef END_OF_STUB_USER_CODE_9_2460377677
  vCAST_USER_CODE_TIMER_START();
  return R_9_3;
}
/*vcast_internal_end*/
/*vcast_internal_start*/{/*vcast_internal_end*/
  struct table_data_type Table_Data;
  seat_index_type Seat;
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,21);
Table_Data = Get_Table_Record(Table);
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,22);
Table_Data.Is_Occupied = v_false;
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,23);
Table_Data.Number_In_Party = 1;
  {
VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,24);
for (Seat=0; Seat < 4; Seat++){
      VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,25);
Table_Data.Order[Seat].Soup = NO_SOUP;
      VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,26);
Table_Data.Order[Seat].Salad = NO_SALAD;
      VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,27);
Table_Data.Order[Seat].Entree = NO_ENTREE;
      VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,28);
Table_Data.Order[Seat].Dessert = NO_DESSERT;
      VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,29);
Table_Data.Order[Seat].Beverage = NO_BEVERAGE;
   }}
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,30);
Table_Data.Check_Total = 0;
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,31);
Update_Table_Record(Table, Table_Data);
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,32);
return 0;
/*vcast_internal_start*/}/*vcast_internal_end*/
}
FLOAT Get_Check_Total(table_index_type Table)
{/*vcast_internal_start*/
extern unsigned short P_9_4_1;
extern float R_9_4;
extern unsigned char SBF_9_4;
if(SBF_9_4) {
  vCAST_USER_CODE_TIMER_STOP();
#define BEGINNING_OF_STUB_USER_CODE_9_3462039048
#include "vcast_configure_stub.c"
#undef BEGINNING_OF_STUB_USER_CODE_9_3462039048
  if ( vcast_is_in_driver ) {
    P_9_4_1 = Table;
    vCAST_COMMON_STUB_PROC_9( 9, 4, 2, 0 );
  } /* vcast_is_in_driver */
#define END_OF_STUB_USER_CODE_9_3462039048
#include "vcast_configure_stub.c"
#undef END_OF_STUB_USER_CODE_9_3462039048
  vCAST_USER_CODE_TIMER_START();
  return R_9_4;
}
/*vcast_internal_end*/
/*vcast_internal_start*/{/*vcast_internal_end*/
  struct table_data_type Table_Data;
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,33);
Table_Data = Get_Table_Record(Table);
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,34);
return (Table_Data.Check_Total);
/*vcast_internal_start*/}/*vcast_internal_end*/
}
void Add_Party_To_Waiting_List(char* Name)
{/*vcast_internal_start*/
extern char *P_9_5_1;
extern unsigned char SBF_9_5;
if(SBF_9_5) {
  vCAST_USER_CODE_TIMER_STOP();
#define BEGINNING_OF_STUB_USER_CODE_9_1361818386
#include "vcast_configure_stub.c"
#undef BEGINNING_OF_STUB_USER_CODE_9_1361818386
  if ( vcast_is_in_driver ) {
    P_9_5_1 = Name;
    vCAST_COMMON_STUB_PROC_9( 9, 5, 2, 0 );
  } /* vcast_is_in_driver */
#define END_OF_STUB_USER_CODE_9_1361818386
#include "vcast_configure_stub.c"
#undef END_OF_STUB_USER_CODE_9_1361818386
  vCAST_USER_CODE_TIMER_START();
  return;
}
/*vcast_internal_end*/
/*vcast_internal_start*/{/*vcast_internal_end*/
  int i = (VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,35),0);
  {
if((VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,36),WaitingListSize > 9))
    {
VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,37);
WaitingListSize = 0;}
}
  {
VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,38);
while(Name && *Name) {
    VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,39);
WaitingList[WaitingListSize][i++] = *Name;
    VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,40);
Name++;
  }}
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,41);
WaitingList[WaitingListSize++][i] = 0;
/*vcast_internal_start*/}/*vcast_internal_end*/
}
char* Get_Next_Party_To_Be_Seated(void)
{/*vcast_internal_start*/
extern char *R_9_6;
extern unsigned char SBF_9_6;
if(SBF_9_6) {
  vCAST_USER_CODE_TIMER_STOP();
#define BEGINNING_OF_STUB_USER_CODE_9_11444024
#include "vcast_configure_stub.c"
#undef BEGINNING_OF_STUB_USER_CODE_9_11444024
  if ( vcast_is_in_driver ) {
    vCAST_COMMON_STUB_PROC_9( 9, 6, 1, 0 );
  } /* vcast_is_in_driver */
#define END_OF_STUB_USER_CODE_9_11444024
#include "vcast_configure_stub.c"
#undef END_OF_STUB_USER_CODE_9_11444024
  vCAST_USER_CODE_TIMER_START();
  return R_9_6;
}
/*vcast_internal_end*/
/*vcast_internal_start*/{/*vcast_internal_end*/
  {
{
VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,42);
if(WaitingListIndex > 9)
    {
VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,43);
WaitingListIndex = 0;}
}
}
  VCAST_STATEMENT_INSTRUMENTATION_POINT_REALTIME(vcast_unit_stmt_bytes_9,9,44);
return WaitingList[WaitingListIndex++];
/*vcast_internal_start*/}/*vcast_internal_end*/
}
/*vcast_internal_start*/
struct order_type *P_9_1_1;
unsigned char SBF_9_1 = 0;
/*vcast_internal_end*/
/*vcast_internal_start*/
unsigned short P_9_2_1;
unsigned short P_9_2_2;
struct order_type P_9_2_3;
int R_9_2;
unsigned char SBF_9_2 = 0;
/*vcast_internal_end*/
/*vcast_internal_start*/
unsigned short P_9_3_1;
int R_9_3;
unsigned char SBF_9_3 = 0;
/*vcast_internal_end*/
/*vcast_internal_start*/
unsigned short P_9_4_1;
float R_9_4;
unsigned char SBF_9_4 = 0;
/*vcast_internal_end*/
/*vcast_internal_start*/
char *P_9_5_1;
unsigned char SBF_9_5 = 0;
/*vcast_internal_end*/
/*vcast_internal_start*/
char *R_9_6;
unsigned char SBF_9_6 = 0;
/*vcast_internal_end*/

