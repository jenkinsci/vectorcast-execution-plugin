/***********************************************
 *      VectorCAST Test Harness Component      *
 *     Copyright 2019 Vector Informatik, GmbH.    *
 *              19.sp3 (11/13/19)              *
 ***********************************************/
#include "S0000007.h"
#include "S0000002.h"

#ifndef VCAST_NO_LIMITS
#include <limits.h>
#ifndef VCAST_NO_FLOAT
#include <float.h>
#endif
#endif
#ifndef LONG_MAX
#define LONG_MAX 2147483647L
#endif
#include "c_cover_io.c"

#ifdef VCAST_PARADIGM
extern void dprintf (char *format,...);
void            SerialPortEnd(void);
#endif

#ifdef VCAST_PARADIGM_SC520

#ifdef __cplusplus
extern "C" {
#endif 

#include "586.h"
#include "ser1.h"

#ifdef __cplusplus
}
#endif 

extern COM ser1_com;
#endif

vCAST_boolean vCAST_isUUTorGlobal(char *vcast_command);

/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0
void vCAST_GET_RANGE_OR_LIST_VAL(char *command, struct vCAST_RANGE_DATA *rd, int position);
#endif

static void vcast_get_position (const char *vcast_command, int pos, char *vdata)
{
   int vc_numPos = 0;
   int vc_begin = 0;
   int vc_cur;

   for (vc_cur = 0; vcast_command[vc_cur] != '\0'; ++vc_cur) {
      if (vcast_command[vc_cur] == '.' || vcast_command[vc_cur] == '%') {
         if (vc_numPos == pos) {
            int vc_ncur;
            /* volatile added to work-around an optimization bug in TI CC 4.1 
               see FB case: 27789 for more info */
            volatile int vc_count = 0;
            for (vc_ncur = vc_begin; vc_ncur != vc_cur; ++vc_ncur) {
               vdata[vc_count++] = vcast_command[vc_ncur];
            }
            vdata[vc_count] = '\0';
            break;
         }

         vc_numPos++;
         vc_begin = vc_cur+1;
      }
   }
}



int
vcast_get_hc_id (char *vcast_command)
{
   char vc_hc[VCAST_LARGEST_COMMAND_FIELD];
   vcast_get_position (vcast_command, 0, vc_hc);
   return VCAST_atoi (vc_hc);
}

void
vcast_get_unit_id_str (char *vcast_command, char *vcast_unit)
{
   vcast_get_position (vcast_command, 1, vcast_unit);
}

int
vcast_get_unit_id (char *vcast_command)
{
   char vc_unit[VCAST_LARGEST_COMMAND_FIELD];
   vcast_get_position (vcast_command, 1, vc_unit);
   return VCAST_atoi (vc_unit);
}

void
vcast_get_subprogram_id_str (char *vcast_command, char *vcast_subprogram)
{
   vcast_get_position (vcast_command, 2, vcast_subprogram);
}

int
vcast_get_subprogram_id (char *vcast_command)
{
   char vc_subprogram[VCAST_LARGEST_COMMAND_FIELD];
   vcast_get_position (vcast_command, 2, vc_subprogram);
   return VCAST_atoi (vc_subprogram);
}

void
vcast_get_parameter_id_str (char *vcast_command, char *vcast_subprogram)
{
   vcast_get_position (vcast_command, 3, vcast_subprogram);
}

int
vcast_get_parameter_id (char *vcast_command)
{
   char vc_parameter[VCAST_LARGEST_COMMAND_FIELD];
   vcast_get_position (vcast_command, 3, vc_parameter);
   return VCAST_atoi (vc_parameter);
}

/* The format is
 *  0.9.1.2.0
 * where 0 represents the kind of command, it's pos is 0
 * 9 represents the unit. it's positiion is 1.
 * 1 represents the subprogram/global, it's pos is 2
 * 2 represents the parameter, it's pos is 3
 * 0 is the next level, it's pos is 4
 * Be careful calling this. It currently only handles 
 * strings of size 64.
 */
int
vcast_get_nth_parameter_id (char *vcast_command, int pos)
{
   char vc_parameter[VCAST_LARGEST_COMMAND_FIELD];
   vcast_get_position (vcast_command, pos, vc_parameter);
   return VCAST_atoi (vc_parameter);
}

int vcast_get_percent_pos (char *vcast_command)
{
   int pos = 0;
   char *cur = vcast_command;

   while (cur && *cur) {
      if (*cur == '%')
         break;
      pos++;
      cur++;
   }

   return pos;
}

/**************************************************************************
Function: vCAST_slice
Parameters: vcast_target - destination of new string
      vcast_source - string to take a portion of
      vcast_first - first index of desired portion
      vcast_last - last index of desired portion
Description: This function takes a string source and copies a portion of
  it into the string vcast_target.
 *************************************************************************/
void vCAST_slice ( char vcast_target[], char vcast_source[], int vcast_first, int vcast_last )
{
  int vcast_i;
  int end_of_slice = vcast_last;

  /* make sure the last index desired is not past the end of source string */
  if (VCAST_strlen(vcast_source) < vcast_last)
    end_of_slice = VCAST_strlen(vcast_source);

  /* copy the portion of the string */
  for (vcast_i=vcast_first;vcast_i<=end_of_slice;vcast_i++)
    vcast_target[vcast_i-vcast_first] = vcast_source[vcast_i];

  /* set the character after the last coped to zero to terminate the string */
  vcast_target[vcast_i-vcast_first] = 0;
}




#if !defined(VCAST_USE_STRTOLL)
/** This function converts a string to an integer **/
VCAST_LONGEST_INT VCAST_atoi ( const char *vcast_str )
{
   VCAST_LONGEST_INT ret;
   VCAST_LONGEST_INT sgn;
   int idx;

   ret = 0;
   sgn = 1;

   idx = 0;
   while ( vcast_str[idx] != 0 )
     {
      if (vcast_str[idx] == '+')
         sgn = 1;
      else
  if (vcast_str[idx] == '-')
    sgn = -1;
  else
    if ( ( vcast_str[idx] >= '0' ) && ( vcast_str[idx] <= '9' ) )
      ret = ret * 10 + vcast_str[idx] - '0';
    else if ( vcast_str[idx] != ' ' )
      break;
      idx++;
     }

   return ( ret * sgn );
}
#endif

#ifndef VCAST_USE_STRTOULL
/** This function converts a string to an unsigned integer.  It is used by 
    the harness when strtoull is not available.  It assumes that the base
    of the unsigned integer is always ten.  **/
VCAST_LONGEST_UNSIGNED VCAST_strtoul(const char *vcast_nptr, char **vcast_endptr, int vcast_base){
  VCAST_LONGEST_UNSIGNED ret = 0;
  int idx = 0;

  while ( vcast_nptr[idx] != 0 )
    {	
      if ( ( vcast_nptr[idx] >= '0' ) && ( vcast_nptr[idx] <= '9' ) )
	ret = ret * 10 + vcast_nptr[idx] - '0';
      else if ( vcast_nptr[idx] != ' ' )
	break;
      idx++;
    }

  return ( ret);
}
#endif

#ifndef VCAST_USE_STD_STRING
/** This function copies string VC_T to string VC_S */
void VCAST_strcpy ( char *VC_S, const char *VC_T )
{
  vectorcast_strcpy(VC_S, VC_T);
}

void VCAST_unsigned_strcpy ( unsigned char *VC_S, const unsigned char *VC_T )
{
   int vcast_i = 0;
   while ( (VC_S[vcast_i] = VC_T[vcast_i]) != 0 )
      vcast_i++;
}

/** This function compares string VC_T to string VC_S */
int VCAST_strcmp ( char *VC_S, char *VC_T )
{
   int vcast_i;
   for (vcast_i=0;VC_S[vcast_i]==VC_T[vcast_i];vcast_i++)
     if ( VC_S[vcast_i] == 0 )
       return 0;
   return VC_S[vcast_i] - VC_T[vcast_i];
}

void VCAST_unsigned_strcat ( unsigned char VC_S[], const unsigned char VC_T[] )
{
   int vcast_i,vc_j;
   vcast_i = vc_j = 0;
   while ( VC_S[vcast_i] != 0 )
      vcast_i++;           /* find end of VC_S */
   while ( (VC_S[vcast_i++] = VC_T[vc_j++]) != 0 ) ; /* copy VC_T */
}

/** This function compares string VC_T to string VC_S */
int VCAST_strncmp ( const char *VC_S, const char *VC_T, int VC_N )
{
   int vcast_i;
   for (vcast_i=0;VC_S[vcast_i]==VC_T[vcast_i];vcast_i++)
      {
      if ( VC_S[vcast_i] == 0 )
         return 0;
      if ( VC_N == vcast_i+1 )
         break;
      }
   return VC_S[vcast_i] - VC_T[vcast_i];
}

/* memxxx functions used when vCAST_FULL_STRINGS is turned on.  Instead of
 * using NULL termination, they take a length, and copy all data in the
 * strings, including NULLs. */

void* VCAST_memset ( void *vcast_dest, int vcast_value, int vcast_n ) {
   typedef unsigned char vcastBYTE;
   vcastBYTE *vc_d = (vcastBYTE *) vcast_dest;
   int vcast_i;

   for (vcast_i = 0; vcast_i < vcast_n; vcast_i++)
      vc_d[vcast_i] = vcast_value;

   return vcast_dest;
}

/* Compares vcast_n bytes of vcast_s1 and vcast_s2 and returns an 
 * integer less than, equal to, or greater than zero, according to 
 * whether vcast_s1 is less than, equal to, or greater than vcast_s2 
 * when taken to be unsigned characters (bytes). */
int VCAST_memcmp(const void *vcast_s1, const void *vcast_s2, int vcast_n)
{
   typedef unsigned char vcastBYTE;
   vcastBYTE *vcast_b1 = (vcastBYTE *) vcast_s1;
   vcastBYTE *vcast_b2 = (vcastBYTE *) vcast_s2;
   int vcast_i;

   for (vcast_i = 0; vcast_i < vcast_n; vcast_i++){
      if (vcast_b1[vcast_i] < vcast_b2[vcast_i])
         return -1;
      else if (vcast_b1[vcast_i] > vcast_b2[vcast_i])
         return 1;
   }

   return 0;
}

#endif /* VCAST_USE_STD_STRING */

/* Copies vcast_n bytes from vcast_source to vcast_dest.  It returns a 
   pointer to vcast_dest */
void *VCAST_memcpy(void *vcast_dest, const void *vcast_source, int vcast_n)
{
#ifdef VCAST_USE_STD_STRING
   return memcpy(vcast_dest, vcast_source, vcast_n);
#else
   typedef unsigned char vcastBYTE;
   vcastBYTE *vc_s = (vcastBYTE *) vcast_source;
   vcastBYTE *vc_d = (vcastBYTE *) vcast_dest;
   int vcast_i;

   for (vcast_i = 0; vcast_i < vcast_n; vcast_i++)
      vc_d[vcast_i] = vc_s[vcast_i];

   return vcast_dest;
#endif
}

/** This function copies string VC_T to string VC_S */
void VCAST_strncpy ( char *VC_S, char *VC_T, int VC_N )
{
#ifdef VCAST_USE_STD_STRING
   strncpy(VC_S, VC_T, VC_N);
#else
   int VC_tmp;
   for (VC_tmp = 0; VC_tmp < VC_N && (VC_S[VC_tmp] = VC_T[VC_tmp]); VC_tmp++){}
#endif
}



void VCAST_signed_strcpy ( signed char *VC_S, const signed char *VC_T )
{
   int vcast_i = 0;
   while ( (VC_S[vcast_i] = VC_T[vcast_i]) != 0 )
      vcast_i++;
}

#ifdef VCAST_NO_MALLOC

#ifdef VCAST_EVP
__DataAlign(32)
#endif
static char vcast_heap[VCAST_MAX_HEAP_SIZE];
static char * vcast_heap_pointer = 0;
static int vcast_heap_size = 0;

/* This does NOT have to be initialized */
static int vcast_heap_allocated;

void * VCAST_malloc( unsigned int vcast_size )
{
  char * start = VCAST_NULL;
  const int pointer_size = sizeof(int*);

  if (vcast_heap_pointer == 0) {
    vcast_heap_allocated = 1;
    vcast_heap_pointer = vcast_heap;
  }

#ifdef VCAST_FOUR_BYTE_BOUNDRY_FIX
  /* Do not remove, necessary for targets that do not put
     vcast_heap on a 4 byte boundry, but require pointers
     to be on a 4 byte boundry */
  if (vcast_heap == vcast_heap_pointer) {
    int vcast_address = (int)vcast_heap_pointer;
    int val = 4 - vcast_address%4;
    vcast_heap_pointer += val;
  }
#endif

  /* Necessary for targets that require 8 byte alignment of memory
   * allocation. */
#ifdef VCAST_EIGHT_BYTE_ALIGNMENT
   {
       int byte_alignment = 8;
       int vcast_address = (int)vcast_heap_pointer;
       int vcast_address_mod = vcast_address % byte_alignment;
       if (vcast_size == byte_alignment && vcast_address_mod != 0) {
           vcast_heap_pointer += byte_alignment - vcast_address_mod;
           vcast_heap_size += byte_alignment - vcast_address_mod;
       }
   }
#endif

  vcast_size = vcast_size + (pointer_size - (vcast_size%pointer_size));

  if( vcast_size > 0 ) {
    start = vcast_heap_pointer;
    vcast_heap_pointer = start + vcast_size;
    vcast_heap_size += vcast_size;

    if( vcast_heap_size > VCAST_MAX_HEAP_SIZE ) {
      vectorcast_print_string( "VCAST_malloc() failed\n" );
      VCAST_driver_termination( 4, 1009 );
    }

  }

  return (void *)start;

}


void VCAST_free( void * vcast_aptr )
{
  /* nothing to do - VCAST_malloc is a naive implementation
      (when we're out of room, we're done) */
}

void VCAST_reset_vcast_heap () {
/* Since the VCAST_NO_MALLOC case "VCAST_free" does not really
   "free" memory, this can be used to reset the vcast_heap 
   Note that this IS NOT CURRENTLY USED by the VC harness
*/
  vcast_heap_pointer = vcast_heap;
  vcast_heap_size = 0;
}


#else /* VCAST_NO_MALLOC */

void * VCAST_malloc( unsigned int vcast_size )
{
  void * result;

  result = (void *)malloc( vcast_size );

  if( result == VCAST_NULL ) {
    vectorcast_print_string( "malloc() failed\n" );
    VCAST_driver_termination( 5, 1012 );
  }

  return result;

}

#endif /* VCAST_NO_MALLOC */

#ifndef VCAST_NO_EXIT
#ifdef __cplusplus
extern "C" {
#endif
void VCAST_exit (int vcast_status)
{
  VCAST_driver_termination (0, 1008);
}
#ifdef __cplusplus
}
#endif
#endif

/* Compares string vcast_tn to the currently executing test case name */
int VCAST_test_name_cmp(char *vcast_tn)
{
   return VCAST_strcmp(vcast_tn, vCAST_TEST_NAME);
}


#define PACKET_INDEX_LAST 100

#ifndef VCAST_NO_FLOAT
void vCAST_SET_FLOAT_FORMAT ( int   VCAST_option,
                              int   VCAST_value,
                              int  *VCAST_width,
                              int  *VCAST_precision,
                              char *VCAST_format )
{
   char vcWork[5];
        
   if ( VCAST_option == 4 && (VCAST_value >= 0 || VCAST_value < 1000))
      *VCAST_width = VCAST_value;
   else if ( VCAST_option == 1 && (VCAST_value >= 0 || VCAST_value < 18))
      *VCAST_precision = VCAST_value;

   VCAST_strcpy ( VCAST_format, "%" );
   if ( !(*VCAST_precision) && !(*VCAST_width) ) {
      VCAST_strcat ( VCAST_format, VCAST_PRINTF_LONG_DOUBLE );

   } else if ( !(*VCAST_precision) && *VCAST_width ) {
      vcast_signed_to_string ( vcWork, *VCAST_width );
      VCAST_strcat ( VCAST_format, vcWork );
      VCAST_strcat ( VCAST_format, VCAST_PRINTF_LONG_DOUBLE );

   } else if ( *VCAST_precision && !(*VCAST_width) ) {
      char vcWork[5];
      VCAST_strcat ( VCAST_format, "." );
      vcast_signed_to_string ( vcWork, *VCAST_precision );
      VCAST_strcat ( VCAST_format, vcWork );
      VCAST_strcat ( VCAST_format, VCAST_PRINTF_LONG_DOUBLE );

   } else {
      char vcWork[5];
      vcast_signed_to_string ( vcWork, *VCAST_width );
      VCAST_strcat ( VCAST_format, vcWork );
      VCAST_strcat ( VCAST_format, "." );
      vcast_signed_to_string ( vcWork, *VCAST_precision );
      VCAST_strcat ( VCAST_format, vcWork );
      VCAST_strcat ( VCAST_format, VCAST_PRINTF_LONG_DOUBLE );
   }
}
#endif /* VCAST_NO_FLOAT */ 


/* Sets option option to be the value value.  */
void vCAST_SET_TESTCASE_CONFIGURATION_OPTIONS( int VCAST_option,
                                               int VCAST_value,
                                               int VCAST_set_default) {
   /* Current options are as follows:
    *    option: 1 is VCAST_FLOAT_PRECISION
    *            VCAST_value can be any value from 0 and 17
    *              1 -> 17 is "%.Nlg"
    *              0 is the default which is "%lg"
    *    option: 2 is vCAST_FULL_STRINGS
    *             VCAST_value can be 0 -> off or 1 -> on
    *             VCAST_value is 0 on default
    *    option: 3 is vCAST_HEX_NOTATION
    *             VCAST_value can be 0 -> off or 1 -> on
    *             VCAST_value is 0 on default
    *    option: 4 is VCAST_FLOAT_FIELD_WIDTH
    *            VCAST_value can be any number >= 0
    *            when 0, no number put in the field width location
    *            otherwise, VCAST_value will be put in the field width location
    *    option: 5 is VCAST_GLOBALS_DISPLAY
    *            Controls when data is captured for global objects
    *    option: 6 is EVENT_LIMIT
    *            per-testcase override of environment option EVENT_LIMIT
    *    option: 7 is vCAST_MULTI_RETURN_SPANS_RANGE
    *    option: 8 is vCAST_MULTI_RETURN_SPANS_TESTCASE
    *    option: 9 is vCAST_MULTI_RETURN_SPANS_ITERATIONS
    *    option: 10 is vCAST_HEX_INTEGERS
    *    option: 12 is vCAST_DO_COMBINATION
    *    option: 14 is DATA_PARTITIONS
    */
    switch ( VCAST_option ) {
         case 4:  /* VCAST_FLOAT_FIELD_WIDTH */
         case 1:  /* VCAST_FLOAT_PRECISION */
		 
#ifndef VCAST_NO_FLOAT
            if ( VCAST_set_default ) {
               vCAST_SET_FLOAT_FORMAT ( VCAST_option,
                                        VCAST_value,
                                        &VCAST_DEFAULT_FLOAT_FIELD_WIDTH,
                                        &VCAST_DEFAULT_FLOAT_PRECISION,
                                        VCAST_DEFAULT_FLOAT_FORMAT );
			}
            else {
               vCAST_SET_FLOAT_FORMAT ( VCAST_option,
                                        VCAST_value,
                                        &VCAST_FLOAT_FIELD_WIDTH,
                                        &VCAST_FLOAT_PRECISION,
                                        VCAST_FLOAT_FORMAT );
            }
#endif
            break;
			
         case 2:  /* vCAST_FULL_STRINGS */
            if ( (VCAST_value == 0) || (VCAST_value == 1)) {
			   if ( VCAST_set_default ) 
                  VCAST_DEFAULT_FULL_STRINGS = VCAST_value;
			   else
                  vCAST_FULL_STRINGS= VCAST_value;
		    }
            break;
			
         case 3:  /* vCAST_HEX_NOTATION */
            if ( (VCAST_value == 0) || (VCAST_value == 1)) {
			   if ( VCAST_set_default ) 
                  VCAST_DEFAULT_HEX_NOTATION = VCAST_value;
			   else
                  vCAST_HEX_NOTATION= VCAST_value;
            }
            break;
			
         case 5: /* VCAST_GLOBALS_DISPLAY */
         case 11: /* VCAST_GLOBALS_DISPLAY */
            VCAST_GLOBALS_DISPLAY = VCAST_value;
            break;

         case 6: /* EVENT_LIMIT */
            vCAST_HIST_LIMIT = VCAST_value;
            break;
			
         case 7: /* MULTI_RETURN_SPANS_RANGE */
            vCAST_TESTCASE_OPTIONS[vCAST_MULTI_RETURN_SPANS_RANGE] =
                  VCAST_value == 1 ? vCAST_true : vCAST_false;
            break;
			
         case 9: /* MULTI_RETURN_SPANS_COMPOUND_ITERATIONS */
            vCAST_TESTCASE_OPTIONS[vCAST_MULTI_RETURN_SPANS_COMPOUND_ITERATIONS] =
                  VCAST_value == 1 ? vCAST_true : vCAST_false;
            break;
			
         case 12: /* Generate combination */
            if ( (VCAST_value == 0) || (VCAST_value == 1) ) {
			   if ( VCAST_set_default ) 
                  VCAST_DEFAULT_DO_COMBINATION = VCAST_value;
	           else		   
                  vCAST_DO_COMBINATION_TESTING = VCAST_value;
            }
            break;
			
         case 14: /* Data Partitions */
            if ( VCAST_value > 1 )
               vCAST_PARTITIONS = (vCAST_long_double)VCAST_value;
            break;
			
         default:
            break;
		
      } /* end switch */
}

void vCAST_SET_TESTCASE_OPTIONS ( char vcast_options[] )
{
   enum vCAST_testcase_options_type option_enum[3] = {
       vCAST_MULTI_RETURN_SPANS_RANGE,
       vCAST_MULTI_RETURN_SPANS_COMPOUND_ITERATIONS };

   int vcast_i;
   int vc_j = 0;

   for (vcast_i=VCAST_strlen(vcast_options)-1; vcast_i>=0; vcast_i--) {
      if (vcast_options[vcast_i] == '1')
         vCAST_TESTCASE_OPTIONS[option_enum[vc_j]] = vCAST_true;
      else
         vCAST_TESTCASE_OPTIONS[option_enum[vc_j]] = vCAST_false;

      vc_j++;
      if (vc_j > vCAST_testcase_options_count)
              break;
   }
}

/*************************************************************
-- The function vCAST_range takes the number of bits and
-- calculates the largest value that will fit
*************************************************************/
vCAST_double vCAST_power( short vcast_bits )
{
  vCAST_double ret = VCAST_FLOAT_ONE;
  short vcast_i;
  for (vcast_i=0;vcast_i<vcast_bits;vcast_i++)
    ret = VCAST_FLOAT_TWO * ret;
  return(ret);
}



/* Return 1 if is an integer, otherwise 0 */
vCAST_boolean vCAST_IS_INTEGER_RANGE (char *value)
{
   while (value && *value) {
     if ((*value >= '0' && *value <= '9') || *value == '-')
       value++;
     else
       return 0;
   }

   return 1;
}

/*-----------------------------------------------------------------*/
/*  Returns the correct value from Param at position POSITION.
    This will return the correct value if vcast_PARAM is a command
    formatted in either the form of a range, list or primitive.
    If you want this to work for other formats, this function
    must be edited. 
	
	This function gets called with a command that looks like:
	9.1.1.2%3, or 9.1.1.2%3%4%5%6%7, 0.9.2.1%#RANGE#1/4/1
	
	So it takes everything up to the first % and then appends the 
	correct value onto the end.  If it is a simple case, it is just
	the command, if it is a list, it has to grab the correct item
	from the list, if it is a RANGE, it has to compute the correct
	iteration.
	
*/
void vCAST_EXTRACT_DATA_FROM_COMMAND_LINE(char *vcast_buf, char VCAST_PARAM[], int VC_POSITION)
{
   int FIRST_MARK;
   int VC_J;
   int tempmax = VCAST_strlen(VCAST_PARAM);
  
   VCAST_memset(vcast_buf, '\0', VCAST_MAX_STRING_LENGTH);

/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0
   
   {
   /* define this here, to reduce stack usage when range processing is not used */
   char slice1[VCAST_MAX_STRING_LENGTH];

       /* Find first delimeter */
       FIRST_MARK = tempmax;
       for (VC_J=7;VC_J<=tempmax;VC_J++){
          if (VCAST_PARAM[VC_J] == '%'){
             FIRST_MARK = VC_J;
             break;
          } /* end if */
       } /* end for */

       /* slice1 now contains the parameter field of the command VCAST_PARAM */
       vCAST_slice(slice1,VCAST_PARAM,0,FIRST_MARK);

       /* traverse all range and list types */
       for (VC_J = 0;
            VC_J < VCAST_MAX_RANGE && vCAST_RANGE_COUNT[VC_J].vCAST_type != VCAST_NULL_TYPE;
            VC_J++)
       {
          /* get the object who has the same command as the one passed in */
          if(VCAST_strcmp(vCAST_RANGE_COUNT[VC_J].vCAST_COMMAND, slice1) == 0){
            VCAST_strcat(vcast_buf, vCAST_RANGE_COUNT[VC_J].vCAST_COMMAND);
            vCAST_GET_RANGE_OR_LIST_VAL(vcast_buf, &vCAST_RANGE_COUNT[VC_J], VC_POSITION-1);
            return;
          } /* end if-else */
       } /* end for */
   }

#endif /* VCAST_MAX_RANGE>0 */

   /* Neither range nor a list, just return the variable */
   VCAST_strcat(vcast_buf, VCAST_PARAM);
 
} /* vCAST_EXTRACT_DATA_FROM_COMMAND_LINE */



vCAST_long_double VCAST_itod ( char vcastStringParam[] )
{
   vCAST_long_double VCAST_number;
   vCAST_long_double VCAST_sign;
   int VCAST_idx;

   VCAST_number = VCAST_FLOAT_ZERO;
   VCAST_sign   = VCAST_FLOAT_ONE;
   VCAST_idx = 0;

   /* Advance past leading whitespace */
   while (vcastStringParam[VCAST_idx] == ' ')
            VCAST_idx++;

   while ( vcastStringParam[VCAST_idx] != 0 )
   {
      if (vcastStringParam[VCAST_idx] == '+')
         VCAST_sign = VCAST_FLOAT_ONE;
      else
         if (vcastStringParam[VCAST_idx] == '-')
         VCAST_sign = -(VCAST_FLOAT_ONE);

         else
            if ( ( vcastStringParam[VCAST_idx] >= '0' ) &&
                 ( vcastStringParam[VCAST_idx] <= '9' ) )
               VCAST_number = VCAST_number * 10 +
                              ( vcastStringParam[VCAST_idx] - '0' );
            else
               break;
      VCAST_idx++;
   }

   VCAST_number = ( VCAST_sign * VCAST_number );

   return VCAST_number;
}

/* This function takes the first number in string vcastStringParam and 
   returns it in vcastFloatParam. */
void vCAST_STR_TO_LONG_DOUBLE ( char vcastStringParam[], vCAST_long_double * vcastFloatParam)
{
#ifdef VCAST_NO_FLOAT
   *vcastFloatParam = (vCAST_long_double)VCAST_atoi (vcastStringParam);
#elif defined(VCAST_USE_STRTOD)
   *vcastFloatParam = strtod (vcastStringParam, NULL);
#elif defined(VCAST_USE_STRTOLD)
   *vcastFloatParam = strtold (vcastStringParam, NULL);
#else
   int VCAST_length;
   int VCAST_decimal = 0;
   int VCAST_exponent = 0;
   int VCAST_endOfNumber;
   int VCAST_endOfDecimal;
   int VCAST_i;

   char VCAST_work[VCAST_MAX_STRING_LENGTH];
   vCAST_long_double VCAST_right = VCAST_FLOAT_ZERO;

   vCAST_long_double VCAST_sign = VCAST_FLOAT_ONE;
   vCAST_long_double VCAST_retVal;

   /* look for decimal place and exponent */
   VCAST_length = VCAST_strlen(vcastStringParam);
   for (VCAST_i=0;VCAST_i<VCAST_length;VCAST_i++)
      if ( vcastStringParam[VCAST_i] == '.' )
         VCAST_decimal = VCAST_i;
      else if ( vcastStringParam[VCAST_i] == 'E' || vcastStringParam[VCAST_i] == 'e' )
         VCAST_exponent = VCAST_i;
      else if ( vcastStringParam[VCAST_i] == '-' )
        {
        if ( VCAST_exponent == 0 ) 
         VCAST_sign = -(VCAST_FLOAT_ONE);
        }
      else if (vcastStringParam[VCAST_i] == '+'){}
      else if ((vcastStringParam[VCAST_i] < '0') ||
               (vcastStringParam[VCAST_i] > '9'))
        /* if this is a list of values, stop at the list separator */
        VCAST_length = VCAST_i - 1;
   
   if ( VCAST_decimal > 0 )
      VCAST_endOfNumber = VCAST_decimal-1;
   else if ( VCAST_exponent > 0 )
      VCAST_endOfNumber = VCAST_exponent-1;
   else
      VCAST_endOfNumber = VCAST_length;

   if ( VCAST_exponent > 0 )
      VCAST_endOfDecimal = VCAST_exponent-1;
   else
      VCAST_endOfDecimal = VCAST_length;
           
   /* left of decimal */
   vCAST_slice ( VCAST_work, vcastStringParam, 0, VCAST_endOfNumber );
   VCAST_retVal = VCAST_itod ( VCAST_work );
   if ( VCAST_retVal < 0 )
      {
      VCAST_sign = -(VCAST_FLOAT_ONE);
      VCAST_retVal = VCAST_retVal * VCAST_sign;
      }

   /* right of decimal */
   if ( VCAST_decimal > 0 )
      {
      vCAST_slice ( VCAST_work, vcastStringParam, VCAST_decimal+1, VCAST_endOfDecimal );
      VCAST_right = VCAST_itod ( VCAST_work );
      for (VCAST_i=0;VCAST_i<VCAST_strlen(VCAST_work);VCAST_i++)
      VCAST_right = VCAST_right / VCAST_FLOAT_TEN;
      VCAST_retVal = VCAST_retVal + VCAST_right;
      }

   /* exponent */
   if ( VCAST_exponent > 0 )
      {
      vCAST_slice ( VCAST_work, vcastStringParam, VCAST_exponent+1, VCAST_length );
      VCAST_exponent = VCAST_atoi ( VCAST_work );
      if ( VCAST_exponent < 0 )
         for (VCAST_i=0;VCAST_i<-(VCAST_exponent);VCAST_i++)
            VCAST_retVal = VCAST_retVal / VCAST_FLOAT_TEN;
      else
         for (VCAST_i=0;VCAST_i<VCAST_exponent;VCAST_i++)
            VCAST_retVal = VCAST_retVal * VCAST_FLOAT_TEN;
      }

   *vcastFloatParam = VCAST_sign * VCAST_retVal;
#endif /* VCAST_NO_FLOAT */
}

void vCAST_DOUBLE_TO_STR(vCAST_long_double VC_F, char VC_S[], int VC_AS_INT)
{
   if (VC_AS_INT) {
     if (VC_F < 0) {
       vcast_signed_to_string(VC_S, (VCAST_SIGNED_CONVERSION_TYPE)VC_F);
     } else {
       vcast_unsigned_to_string(VC_S, (VCAST_UNSIGNED_CONVERSION_TYPE)VC_F);
     }
   } else {
     vcast_float_to_string ( VC_S, VC_F );
   }
}

void vCAST_ITERATION_COUNTER_RESET()
{
   int item_size, index2, index1, idx1, idx2, temp;
   /* initialize iteration counters */
   item_size = sizeof vCAST_ITERATION_COUNTERS[0][0];
   index2    = sizeof vCAST_ITERATION_COUNTERS[0] / item_size;
   temp      = (item_size * index2);
   index1    = sizeof vCAST_ITERATION_COUNTERS / temp;
   for (idx1=0;idx1<index1;idx1++)
      for (idx2=0;idx2<index2;idx2++)
         vCAST_ITERATION_COUNTERS[idx1][idx2] = 0;
}

void vCAST_RESET_ITERATION_COUNTERS(enum vCAST_testcase_options_type OPTION)
{
   if (!vCAST_TESTCASE_OPTIONS[OPTION])
      vCAST_ITERATION_COUNTER_RESET();
}

int vCAST_GET_ITERATION_COUNTER_VALUE(int UNIT, int SUB)
{
   int index = vCAST_ITERATION_COUNTER_SWITCH(UNIT);
   return vCAST_ITERATION_COUNTERS[index][SUB];
}

void vCAST_INCREMENT_ITERATION_COUNTER(int UNIT, int SUB)
{
   int index = vCAST_ITERATION_COUNTER_SWITCH(UNIT);
   vCAST_ITERATION_COUNTERS[index][SUB]++;
}

/*
 * vCAST_getListValue parses a data item from a list
 *   parameter in:
 *     vcast_list - pointer to command data, like "foo" or "%1%2"
 *     vcast_index - the item in the list that is to be returned
 *
 *   parameter out:
 *     *vcast_item - pointer to the item at vcast_index
 *     *vcast_len - length of the item
 */
#if VCAST_MAX_RANGE>0
void vCAST_getListValue(char *vcast_list, int vcast_index, char **vcast_item, int *vcast_len)
{
  *vcast_item = VCAST_NULL;
  if ( !vcast_list || !vcast_len || vcast_index < 0)
    return;

  for ( ; *vcast_list && vcast_index >= 0; --vcast_index ) {
    *vcast_len = 0;
    /* find % or end of string */
    for ( ; *vcast_list != '%'; ++vcast_list )
    {
      if ( *vcast_list == '\0' )
        return;
    }
    /* invariant: *vcast_list == '%' */
    ++vcast_list;
    *vcast_item = vcast_list;
    /* find next % or end of string */
    while ( *vcast_list != '\0' && *vcast_list != '%' )
    {
      ++vcast_list;
      ++*vcast_len;
    }
  }
}
#endif /* VCAST_MAX_RANGE>0 */



/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0
void vCAST_GET_RANGE_OR_LIST_VAL(char *command, struct vCAST_RANGE_DATA *rd, int position) {
  int  idx;
  int  cmd_len;
  char flStr[VCAST_MAX_STRING_LENGTH];
  char *list_item;
  int item_len;

  vCAST_long_double val;
  /* Did this to work around a cosmic bug */
  vCAST_long_double first_range_value = rd->vCAST_MIN;
  vCAST_long_double last_range_value = rd->vCAST_MAX;
 
  
  if(rd->vCAST_type == VCAST_RANGE_TYPE) {
    val = first_range_value + (rd->vCAST_INC * position);
    if (rd->vCAST_INC > 0){
      if (val > last_range_value) {
        /* possible for a stub which returns a range */
        val = last_range_value;
      }
    } else {
      if (val < last_range_value){
        val = last_range_value;
      }
    }
    vCAST_DOUBLE_TO_STR(val, flStr, rd->isInteger);
    VCAST_strcat(command, flStr);
  } else { /* VCAST_LIST_TYPE */
    cmd_len = VCAST_strlen(command);
    vCAST_getListValue(rd->vCAST_list, position, &list_item, &item_len);
    for(idx=0; idx<item_len; idx++) {
      command[cmd_len+idx] = list_item[idx];
    }
    command[cmd_len+item_len] = '\0';
  }
}
#endif /* VCAST_MAX_RANGE>0 */


void vCAST_EXECUTE_RANGE_COMMANDS(int iteration)
{
/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0

  int VC_I = 0;
  int index;
  char tmpStr[VCAST_MAX_STRING_LENGTH];
  
  
  for (VC_I=0; VC_I<VCAST_MAX_RANGE; VC_I++) {
    if (vCAST_RANGE_COUNT[VC_I].vCAST_type != VCAST_NULL_TYPE) {
        if(vCAST_isUUTorGlobal(vCAST_RANGE_COUNT[VC_I].vCAST_COMMAND)) {
            if (vCAST_DO_COMBINATION_TESTING) {
               index = (iteration/vCAST_RANGE_COUNT[VC_I].vCAST_COMBO_GROUPING) %
                          vCAST_RANGE_COUNT[VC_I].vCAST_NUM_VALS;
            } 
            else {
                if(iteration > vCAST_RANGE_COUNT[VC_I].vCAST_NUM_VALS-1) {
                    /* we have exhausted the values for this object, use the last */
                    index = vCAST_RANGE_COUNT[VC_I].vCAST_NUM_VALS-1;
                } 
                else {
                   index = iteration;
                }
            }
            VCAST_strcpy(tmpStr, vCAST_RANGE_COUNT[VC_I].vCAST_COMMAND);
            vCAST_GET_RANGE_OR_LIST_VAL(tmpStr, &vCAST_RANGE_COUNT[VC_I], index);
            vCAST_RUN_DATA_IF (tmpStr, vCAST_false);
        }
    }
    else {
        break;
    }
  }
  
#endif

}


/* Determines if ch is a list. It must be a null terminated string.
*/
vCAST_boolean vCAST_isList(const char *ch) {

  int numMode = 0;
  char *cur = (char *)ch;

  if(cur == VCAST_NULL)
    return 1;

  while(*cur != '\0' && numMode < 2)
    if(*cur++ == '%')
      numMode++;

  if(numMode < 2)
    return vCAST_false;
  else
    return vCAST_true;
} /* end isList */

/* Determines if ch is a range value. It must be a null terminated string.
   parameter:  ch should be pointing to a valid command
                    indexOfFirstDelimeter should be the location of the first (%) delimeter
*/
vCAST_boolean vCAST_isRange(const char *ch, int indexOfFirstDelimeter) {

  char *cur = (char *)ch;
  int length;
  char tmpStr[16];

  if(cur == VCAST_NULL || cur[indexOfFirstDelimeter] != '%')
    return vCAST_false;

  length = VCAST_strlen(ch);

  if(length >= indexOfFirstDelimeter + 7) {    /* there is enough room for #RANGE# */

    vCAST_slice(tmpStr, cur,indexOfFirstDelimeter + 1, indexOfFirstDelimeter +7);
    if(VCAST_strcmp(tmpStr, "#RANGE#") == 0)
      return vCAST_true;
  }

  return vCAST_false;
} /* end isRange */



void vCAST_FREE_RANGE_VALUES(void) {

/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0
  int index;

  for(index = 0; index < VCAST_MAX_RANGE; index++)
    if(vCAST_RANGE_COUNT[index].vCAST_type == VCAST_LIST_TYPE &&
       vCAST_RANGE_COUNT[index].vCAST_list != VCAST_NULL) {
      VCAST_free(vCAST_RANGE_COUNT[index].vCAST_COMMAND);
      VCAST_free(vCAST_RANGE_COUNT[index].vCAST_list);
      vCAST_RANGE_COUNT[index].vCAST_COMMAND = VCAST_NULL;
      vCAST_RANGE_COUNT[index].vCAST_list = VCAST_NULL;
    } else if(vCAST_RANGE_COUNT[index].vCAST_type == VCAST_RANGE_TYPE) {
      VCAST_free(vCAST_RANGE_COUNT[index].vCAST_COMMAND);
      vCAST_RANGE_COUNT[index].vCAST_COMMAND = VCAST_NULL;
    } 
#endif   /* VCAST_MAX_RANGE>0 */
      
} /* end vCAST_FREE_RANGE_VALUES */


/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0
void vCAST_GET_RANGE_VALUES(char *vcast_S,
                            struct vCAST_RANGE_DATA *vcast_range_data)
{
  int VC_I;
  int len = VCAST_strlen(vcast_S);
  char tmpStr[VCAST_MAX_STRING_LENGTH];
  int count = 0,
      index = 0;
  vCAST_boolean isInteger = 1;

  for (VC_I=0;VC_I<len;VC_I++) {
    if (vcast_S[VC_I] == '/') {
      count++;
      if (count == 1) {
        vCAST_slice(tmpStr, vcast_S, 0, VC_I-1);

        if (isInteger)
           isInteger = vCAST_IS_INTEGER_RANGE(tmpStr);

        vCAST_STR_TO_LONG_DOUBLE(tmpStr, &vcast_range_data->vCAST_MIN);
        index = VC_I+1;
      }
      else
  if (count == 2) {
    vCAST_slice(tmpStr, vcast_S, index, VC_I-1);
    if (isInteger)
       isInteger = vCAST_IS_INTEGER_RANGE(tmpStr);
    vCAST_STR_TO_LONG_DOUBLE(tmpStr, &vcast_range_data->vCAST_MAX);
    index = VC_I+1;
  }
    }
  }
  vCAST_slice(tmpStr, vcast_S, index, len-1);
  if ( VCAST_strcmp ( tmpStr, "<<PARTITION>>" ) == 0 ) {
    vcast_range_data->vCAST_INC =
          ( vcast_range_data->vCAST_MAX - vcast_range_data->vCAST_MIN ) /
          ( vCAST_PARTITIONS );
  } else {
    if (isInteger)
       isInteger = vCAST_IS_INTEGER_RANGE(tmpStr);
    vCAST_STR_TO_LONG_DOUBLE(tmpStr, &vcast_range_data->vCAST_INC);
  }

  /* If all min/max/inc were integers, use integer mode */
  /* This avoids sprintf with %f later, and uses %d instead */
  vcast_range_data->isInteger = isInteger;

  { /* calculate vCAST_NUM_VALS for RANGE */
    vCAST_long_double val = vcast_range_data->vCAST_MIN;
    if(vcast_range_data->vCAST_INC > 0) {
      while(val <= vcast_range_data->vCAST_MAX) {
        vcast_range_data->vCAST_NUM_VALS++;
        val = vcast_range_data->vCAST_MIN + (vcast_range_data->vCAST_NUM_VALS * vcast_range_data->vCAST_INC);
      }
    } else {
      while(val >= vcast_range_data->vCAST_MAX) {
        vcast_range_data->vCAST_NUM_VALS++;
        val = vcast_range_data->vCAST_MIN + (vcast_range_data->vCAST_NUM_VALS * vcast_range_data->vCAST_INC);
      }
    }
  }
}

#endif  /* VCAST_MAX_RANGE>0 */



/*  determines if a given command is on the UUT
    or on a global variable */
vCAST_boolean vCAST_isUUTorGlobal(char *vcast_command) {

   int unitId;
   int subId;

   if(vcast_command != VCAST_NULL && VCAST_strlen(vcast_command) >= 8) {
      unitId = vcast_get_unit_id(vcast_command);
      subId = vcast_get_subprogram_id(vcast_command);

#ifdef VCAST_SBF_UNITS_AVAILABLE
      if (vCAST_is_sbf(unitId, subId))
         return 0;
      else 
#endif
      {
         if ((unitId == vCAST_UNIT) || (subId == 0))
           return 1;
      }
  }
  return 0;
} /* end vCAST_isUUTorGlobal */

void vCAST_RESET_LIST_VALUES(void){

/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0

  int VC_I;
  for (VC_I=0;VC_I<VCAST_MAX_RANGE;VC_I++) {
    /* there are no more Ranges or lists, this is done */
    if(vCAST_RANGE_COUNT[VC_I].vCAST_type == VCAST_NULL_TYPE){
      break;
    /* if it is a range */
    } else if(vCAST_RANGE_COUNT[VC_I].vCAST_type == VCAST_LIST_TYPE) {   /* Its a list */
    }
  }
#endif /* VCAST_MAX_RANGE>0 */
}

#ifdef VCAST_SBF_UNITS_AVAILABLE
struct sbf_subprogram {
  int id;
  vCAST_array_boolean stubbed;
  struct sbf_subprogram *next;
};

struct sbf_unit {
  int id;
  struct sbf_subprogram *subs;
  struct sbf_unit *next;
};

static struct sbf_subprogram *vcast_alloc_subprogram(int id)
{
  struct sbf_subprogram *rtn = (struct sbf_subprogram *)VCAST_malloc(sizeof(struct sbf_subprogram));
  rtn->id = id;
  rtn->stubbed = vCAST_true;
  rtn->next = VCAST_NULL;
  return rtn;
}

static struct sbf_unit *vcast_alloc_unit(int id) {
  struct sbf_unit *rtn = (struct sbf_unit*)VCAST_malloc(sizeof(struct sbf_unit));
  rtn->id = id;
  rtn->subs = VCAST_NULL;
  rtn->next = VCAST_NULL;
  return rtn;
}

static struct sbf_unit *vcast_sbf_table;

void vCAST_MODIFY_SBF_TABLE(int sbf_unit,
                            int vcast_sub,
                            vCAST_boolean stubbed)
{
  struct sbf_unit *uiter;
  struct sbf_subprogram *siter;
  if(!vcast_sbf_table) vcast_sbf_table = vcast_alloc_unit(sbf_unit);
  uiter = vcast_sbf_table;
  while(uiter->id != sbf_unit) {
    if(!uiter->next) uiter->next = vcast_alloc_unit(sbf_unit);
    uiter = uiter->next;
  }
  siter = uiter->subs;
  if(siter) {
    while(siter->id != vcast_sub) {
      if(!siter->next) siter->next = vcast_alloc_subprogram(vcast_sub);
      siter = siter->next;
    }
  } else {
    siter = vcast_alloc_subprogram(vcast_sub);
    uiter->subs = siter;
  }
  siter->stubbed = stubbed;
}

#ifdef VCAST_SBF_UNITS_AVAILABLE
#ifdef VCAST_FREE_HARNESS_DATA
void vCAST_FREE_SBF_TABLE(void)
{
  struct sbf_unit *uiter = vcast_sbf_table;
  while(uiter != VCAST_NULL) {
    struct sbf_unit *tmpUnit = uiter;
    struct sbf_subprogram *siter = uiter->subs;
    while (siter != VCAST_NULL){
      struct sbf_subprogram *tmpSubProgram = siter;
      siter = siter->next;
      VCAST_free(tmpSubProgram);
    }
    uiter = uiter->next;
    VCAST_free(tmpUnit);
  }
}
#endif
#endif

vCAST_boolean vCAST_is_sbf(VCAST_LONGEST_INT vcast_unit, VCAST_LONGEST_INT vcast_sub)
{
  struct sbf_unit *uiter = vcast_sbf_table;
  struct sbf_subprogram *siter;
  if (!uiter) return vCAST_false;
  while(uiter->id != vcast_unit) {
    if(!uiter->next) return vCAST_false;
    uiter = uiter->next;
  }
  siter = uiter->subs;
  while(siter->id != vcast_sub) {
    if(!siter->next) return vCAST_false;
    siter = siter->next;
  }
  if((siter->id == vcast_sub) && (siter->stubbed == vCAST_true))
    return vCAST_true;

  return vCAST_false;
}

void vCAST_INITIALIZE_SBF_TABLE(void)
{
  int VC_F;
  char fileName[VCAST_FILENAME_LENGTH];
  char param[VCAST_MAX_STRING_LENGTH];
  char *work;

  vcast_sbf_table = 0;

  VCAST_strcpy(fileName, vCAST_ORDER_OBJECT.VC_N);

  VC_F = vectorcast_fopen(fileName, "r");
  while (!vectorcast_feof(VC_F)){
    vectorcast_readline(param, VC_F);
    work = &param[vcast_get_percent_pos(param)+1];
    if (work && VCAST_strcmp(work, "<<STUB>>") == 0) {
      vCAST_MODIFY_SBF_TABLE(vcast_get_unit_id(param), vcast_get_subprogram_id(param), vCAST_true);
    }
  } /* end while */
  vectorcast_fclose(VC_F);
}
#endif

void vCAST_RESET_RANGE_VALUES(void)
{
/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0

  int  VC_J;
  for (VC_J=0;VC_J<VCAST_MAX_RANGE;VC_J++) {
    vCAST_RANGE_COUNT[VC_J].vCAST_COMMAND = VCAST_NULL;
    vCAST_RANGE_COUNT[VC_J].vCAST_type = VCAST_NULL_TYPE;
    vCAST_RANGE_COUNT[VC_J].vCAST_MIN = VCAST_FLOAT_ZERO;
    vCAST_RANGE_COUNT[VC_J].vCAST_MAX = VCAST_FLOAT_ZERO;
    vCAST_RANGE_COUNT[VC_J].vCAST_INC = VCAST_FLOAT_ZERO;
    vCAST_RANGE_COUNT[VC_J].vCAST_list = VCAST_NULL;
    vCAST_RANGE_COUNT[VC_J].isInteger = 0;
    vCAST_RANGE_COUNT[VC_J].vCAST_NUM_VALS = 0;
    vCAST_RANGE_COUNT[VC_J].vCAST_COMBO_GROUPING = 1;
  }
  vCAST_HAS_RANGE = vCAST_false;
#endif   /* VCAST_MAX_RANGE>0 */
}

void vCAST_INITIALIZE_RANGE_VALUES(void)
{

/* if range processing is disabled, no need to do this stuff!*/
#if VCAST_MAX_RANGE>0

  int VC_F, last_num = 1, last_grouping = 1;
  char fileName[VCAST_FILENAME_LENGTH];
  char param[VCAST_MAX_STRING_LENGTH];
  int  paramLen = 0;
  char tmpStr[VCAST_MAX_STRING_LENGTH];
  int  VC_J;
  int  VC_COUNT = 0;
  char *CHAR_ITER;

  vCAST_NUM_RANGE_ITERATIONS = 1;

  VCAST_strcpy(fileName, vCAST_ORDER_OBJECT.VC_N);

  VC_F = vectorcast_fopen(fileName, "r");
  while (!vectorcast_feof(VC_F)){
    vectorcast_readline(param, VC_F);
    paramLen = VCAST_strlen(param);
    for (VC_J=7;VC_J<paramLen;VC_J++) {
      if (param[VC_J] == '%') {
        if (VC_COUNT<VCAST_MAX_RANGE && vCAST_isRange(param, VC_J)) {
          vCAST_RANGE_COUNT[VC_COUNT].vCAST_type = VCAST_RANGE_TYPE;
          vCAST_slice(tmpStr,param,0,VC_J);
          vCAST_RANGE_COUNT[VC_COUNT].vCAST_COMMAND =
            (char *)VCAST_malloc(sizeof(char)*VCAST_strlen(tmpStr) + 1);
          VCAST_strcpy(vCAST_RANGE_COUNT[VC_COUNT].vCAST_COMMAND, tmpStr);

          if(vCAST_isUUTorGlobal(tmpStr))
            vCAST_HAS_RANGE = vCAST_true;
          vCAST_slice(tmpStr, param, VC_J+8, paramLen);
          vCAST_GET_RANGE_VALUES(tmpStr, &vCAST_RANGE_COUNT[VC_COUNT]);
          VC_COUNT++;
          break;
        } else if(VC_COUNT < VCAST_MAX_RANGE && vCAST_isList(param)) { /* is a list */
          /* copies command string to correct struct array location */
          vCAST_RANGE_COUNT[VC_COUNT].vCAST_type = VCAST_LIST_TYPE;
          vCAST_slice(tmpStr,param,0,VC_J);
          vCAST_RANGE_COUNT[VC_COUNT].vCAST_COMMAND =
            (char *)VCAST_malloc(sizeof(char)*VCAST_strlen(tmpStr) + 1);
          VCAST_strcpy(vCAST_RANGE_COUNT[VC_COUNT].vCAST_COMMAND, tmpStr);

          if(vCAST_isUUTorGlobal(tmpStr))
            vCAST_HAS_RANGE = vCAST_true;

          vCAST_slice(tmpStr, param, VC_J, paramLen);
          vCAST_RANGE_COUNT[VC_COUNT].vCAST_list =
            (char *)VCAST_malloc(sizeof(char)*VCAST_strlen(tmpStr) + 1);
          VCAST_strcpy(vCAST_RANGE_COUNT[VC_COUNT].vCAST_list, tmpStr);
          for(CHAR_ITER=vCAST_RANGE_COUNT[VC_COUNT].vCAST_list; *CHAR_ITER != '\0';
                ++CHAR_ITER) {
            if(*CHAR_ITER == '%') {
              vCAST_RANGE_COUNT[VC_COUNT].vCAST_NUM_VALS++;
            }
          }
          VC_COUNT++;
          break;
        } else
          break;
      } /* end if */
    } /* end for */

    if (VC_COUNT == VCAST_MAX_RANGE)
      break;
  } /* end while */
  vectorcast_fclose(VC_F);
  
  /* if there is one varied object, combination testing doesn't mean anything */
  if (VC_COUNT > 1 && vCAST_DO_COMBINATION_TESTING) {
    for (VC_J=VC_COUNT-1; VC_J>=0; --VC_J) {
      if (vCAST_isUUTorGlobal(vCAST_RANGE_COUNT[VC_J].vCAST_COMMAND)) {
        vCAST_RANGE_COUNT[VC_J].vCAST_COMBO_GROUPING = last_num * last_grouping;
        last_num = vCAST_RANGE_COUNT[VC_J].vCAST_NUM_VALS;
        last_grouping = vCAST_RANGE_COUNT[VC_J].vCAST_COMBO_GROUPING;
      }
    }
    
    for (VC_J=0;VC_J<VC_COUNT;++VC_J) {
      if (vCAST_isUUTorGlobal(vCAST_RANGE_COUNT[VC_J].vCAST_COMMAND)) {
        vCAST_NUM_RANGE_ITERATIONS = vCAST_NUM_RANGE_ITERATIONS *
                                     vCAST_RANGE_COUNT[VC_J].vCAST_NUM_VALS;
      }
    }
  } else {
    for (VC_J=0;VC_J<VC_COUNT;++VC_J) {
      if (vCAST_isUUTorGlobal(vCAST_RANGE_COUNT[VC_J].vCAST_COMMAND) &&
          vCAST_RANGE_COUNT[VC_J].vCAST_NUM_VALS > vCAST_NUM_RANGE_ITERATIONS) {
        vCAST_NUM_RANGE_ITERATIONS = vCAST_RANGE_COUNT[VC_J].vCAST_NUM_VALS;
      }
    }
  }
  
#endif /* VCAST_MAX_RANGE>0 */
  
}




void vCAST_RE_OPEN_HIST_FILE (void);
void vCAST_CLOSE_HIST_FILE_FOR_EVENT (void);

#ifdef __cplusplus
void vCAST_APPEND_HISTORY_FLAG (char VC_EVENT_FLAGS[], char VC_NEXT_FLAG){
  vCAST_boolean vcast_flag_seen = vCAST_false;
  for (int vcast_flag_counter = 0; vcast_flag_counter < 4; vcast_flag_counter++){
    if (vcast_flag_seen && VC_EVENT_FLAGS[vcast_flag_counter] == ' '){
     VC_EVENT_FLAGS[vcast_flag_counter] = VC_NEXT_FLAG;
     break;
    }
    if (VC_EVENT_FLAGS[vcast_flag_counter] == VC_NEXT_FLAG){
      break;
    }
    vcast_flag_seen = vCAST_true;
  }
}
#endif
void vCAST_SET_HISTORY ( int VC_U, int VC_S )
{
   vCAST_SET_HISTORY_FLAGS ( VC_U, VC_S, "", "" );
}

void vCAST_SET_HISTORY_FLAGS ( int VC_U,
                               int VC_S,
                               char VC_EVENT_FLAGS[],
                               char VC_SLOT_DESCR[] )
{
   /* Do not store the ascii data when history limit is zero. */
   if ((vCAST_HIST_LIMIT != 0) && ((VC_U > 0) || (VC_S > 0))) {
      if (VC_U < 1000 && vCAST_HIST_INDEX <= vCAST_HIST_LIMIT)
        vCAST_STORE_ASCII_DATA (VC_U,VC_S,VC_EVENT_FLAGS);
   }

   /* if we are not printing slot data, don't print history data */
   if ( vCAST_ORDER_OBJECT.VC_PRINT_DATA[0] == 'F' )
      return;

   /* If the maximum number of events has not been reached or if 
    * final event, then write the line. */
   if ((vCAST_HIST_INDEX <= vCAST_HIST_LIMIT) ||
       (VC_U == 0 && VC_S == 0)) {

      vCAST_RE_OPEN_HIST_FILE();
      vectorcast_fprint_integer (vCAST_HIST_FILE, VC_U);
      vectorcast_fprint_string  (vCAST_HIST_FILE, ",");
      vectorcast_fprint_integer (vCAST_HIST_FILE, VC_S);
      vectorcast_fprint_string  (vCAST_HIST_FILE, " ");
      vectorcast_fprint_string (vCAST_HIST_FILE, VC_EVENT_FLAGS);
      /* print description at beginning of every slot iteration */
      if ( VC_EVENT_FLAGS[1] == 'I' && VC_SLOT_DESCR[0] != 0 ) {
        vectorcast_fprint_string (vCAST_HIST_FILE, " #" );
        vectorcast_fprint_string (vCAST_HIST_FILE, VC_SLOT_DESCR );
      }
      /* print compound for batch indicator if it's there */
      else if ( VC_SLOT_DESCR[0] == 'X' )
        vectorcast_fprint_string (vCAST_HIST_FILE, VC_SLOT_DESCR );
    
      vectorcast_fprint_string_with_cr (vCAST_HIST_FILE, "");
      vectorcast_fprint_string_with_cr ( VCAST_EXP_FILE, "-- Event" );

      vectorcast_fflush (vCAST_HIST_FILE);
      vCAST_CLOSE_HIST_FILE_FOR_EVENT ();

   } else if (vCAST_HIST_LIMIT != 0) {
      vCAST_RE_OPEN_HIST_FILE();

      vCAST_STORE_ASCII_DATA (VC_U,VC_S,VC_EVENT_FLAGS);
      if (VCAST_GLOBALS_DISPLAY != vCAST_EACH_EVENT){
        vCAST_STORE_GLOBAL_ASCII_DATA ();
      }
      vectorcast_fprint_integer (vCAST_HIST_FILE, VC_U);
      vectorcast_fprint_string  (vCAST_HIST_FILE, ",");
      vectorcast_fprint_integer (vCAST_HIST_FILE, VC_S);
      vectorcast_fprint_string  (vCAST_HIST_FILE, " ");
      vectorcast_fprint_string_with_cr (vCAST_HIST_FILE, VC_EVENT_FLAGS);

      vectorcast_fprint_string_with_cr ( VCAST_EXP_FILE, "-- Event" );

      /* If the history limit is not zero then record the fact that 
       * there are too many events */
      vectorcast_fprint_string (vCAST_HIST_FILE, "1000,888\n");
      vectorcast_fprint_string (vCAST_HIST_FILE, "0,0\n");

      /* and stop running the test case */
      vectorcast_fflush (vCAST_HIST_FILE);
      vCAST_CLOSE_HIST_FILE_FOR_EVENT ();

      vcast_is_in_driver = vCAST_false;
      VCAST_driver_termination( 0, 0 );
   }

   vCAST_HIST_INDEX = vCAST_HIST_INDEX + 1;
} /* end vCAST_SET_HISTORY */

/* Original write_to_inst_file function.  Currently used by default when
 * coverage optimizations are turned off. */
void VCAST_WRITE_TO_INST_FILE(const char VC_S[])
{
   vCAST_CREATE_INST_FILE();
   vectorcast_fprint_string_with_cr(vCAST_INST_FILE, VC_S);
}

void vCAST_SET_OUTPUT_TO_EVENT_FILE (void)
{
  vCAST_OUTPUT_FILE = vCAST_EVENT_FILE;
}

void vCAST_WRITE_END_FILE(void)
{
#ifdef VCAST_PARADIGM_SC520
  COM *c1;  
  c1 = &ser1_com;
#endif

   vectorcast_write_vcast_end ();
   vectorcast_terminate_io ();
   
#ifdef VCAST_PARADIGM
  /*  hit a few dummy dprintf's to flush serial data  */
  dprintf("                                                               \n");
  dprintf("                                                               \n");
  dprintf("                                                               \n");
  dprintf("                                                               \n");
  SerialPortEnd();
#endif
#ifdef VCAST_PARADIGM_SC520
  delay_ms(2000);
  s1_close(c1);
#endif

}

/* This function sets all of the harness options to their default value.
 * It is called before every new test case is run.
 */
void vCAST_RESET_HARNOPTS_FILE(void)
{
#ifndef VCAST_NO_FLOAT
   VCAST_strncpy(VCAST_FLOAT_FORMAT, VCAST_DEFAULT_FLOAT_FORMAT, VCAST_FLOAT_FORMAT_SIZE);
   VCAST_FLOAT_PRECISION   = VCAST_DEFAULT_FLOAT_PRECISION;
   VCAST_FLOAT_FIELD_WIDTH = VCAST_DEFAULT_FLOAT_FIELD_WIDTH;
#endif
   vCAST_FULL_STRINGS      = VCAST_DEFAULT_FULL_STRINGS;
   vCAST_HEX_NOTATION      = VCAST_DEFAULT_HEX_NOTATION;
   vCAST_DO_COMBINATION_TESTING = VCAST_DEFAULT_DO_COMBINATION;
   /* vCAST_DO_RANGE_DATA does not need to be reset. It should
    * persist, until the execution of the harness is done. */
}

/* This function is provided so that the harness can read in
 * options from the user with out having to recompile.
 * As of now, several options can be used.
 *
 * This function is called once on start up. It initialized global data that
 * is used to set the defaults after every test run. This is useful so that
 * file I/O does not become a bottleneck.
 */
void vCAST_OPEN_HARNOPTS_FILE(void)
{
   char VCAST_TEXT[VCAST_MAX_STRING_LENGTH];
   
   char VCAST_NUMBER[VCAST_LARGEST_COMMAND_FIELD];

   int  VCAST_UNIT_INDEX  = 0;
   int  VCAST_SUB_INDEX   = 0;   /* option to set */
   int  VCAST_PARAM_INDEX = 0;
   int  VCAST_VALUE       = 0;   /* value to set */
   int  VCAST_optsfd      = -1;

   if( (VCAST_optsfd = vectorcast_fopen("HARNOPTS.DAT", "r")) == -1)
      return; /* no HARNOPTS.DAT file, use default values */

   while (vectorcast_fgets ( VCAST_TEXT, VCAST_MAX_STRING_LENGTH, VCAST_optsfd )!=0) {
      VCAST_UNIT_INDEX = vcast_get_unit_id (VCAST_TEXT); 
      VCAST_SUB_INDEX = vcast_get_subprogram_id (VCAST_TEXT);
      VCAST_PARAM_INDEX = vcast_get_parameter_id (VCAST_TEXT);

      vCAST_slice(VCAST_NUMBER, VCAST_TEXT, vcast_get_percent_pos (VCAST_TEXT)+1, VCAST_strlen(VCAST_TEXT));
      VCAST_VALUE = VCAST_atoi( VCAST_NUMBER );

      /* found a config option */
      if(VCAST_UNIT_INDEX == 0) {
         if (VCAST_PARAM_INDEX == 4) {
            vCAST_SET_TESTCASE_CONFIGURATION_OPTIONS(VCAST_SUB_INDEX, VCAST_VALUE, 1);
            }
         else if (VCAST_PARAM_INDEX == 5) {
            vCAST_DO_DATA_IF = (int)VCAST_VALUE;
            }
      }
   }
   vectorcast_fclose(VCAST_optsfd);
}

/* Starting with version 5.1, the TESTORDR.DAT file is now formatted:
 * line 1: <event limit>
 * line 2: <testcase name>
 * line 3: <testcase datafile>
 * line 4: <number of iterations>
 * line 5: <slot description>
 * with lines 2 through N duplicated for each slot */
void vCAST_OPEN_TESTORDR_FILE (void)
{
   char VC_TEXT[VCAST_MAX_STRING_LENGTH];
   int  vc_x;

   vCAST_ORDER_FILE = vectorcast_fopen("TESTORDR.DAT", "r");

   /* read event limit */
   vectorcast_fgets ( VC_TEXT, VCAST_MAX_STRING_LENGTH, vCAST_ORDER_FILE );

   /* Some versions of atoi don't like the trailing newline */
   vc_x = VCAST_strlen(VC_TEXT);
   if (vc_x > 0 && VC_TEXT[vc_x-1] == '\n')
     VC_TEXT[vc_x-1] = '\0';

   vCAST_ENV_HIST_LIMIT = VCAST_atoi ( VC_TEXT );
}

struct vCAST_ORDER_ENTRY* vCAST_ORDER(void)
{
  return &vCAST_ORDER_OBJECT;
}

int VCAST_READ_TESTORDR_LINE ( char vc_line[] ) {
   char VC_TEXT[VCAST_MAX_STRING_LENGTH];
   int vc_length;

   vc_line[0] = 0;
   vectorcast_readline(VC_TEXT, vCAST_ORDER_FILE);
   vc_length = VCAST_strlen(VC_TEXT);
   if ( vc_length != 0 ) {
      if ( VC_TEXT[vc_length-1] == '\n' )
        VC_TEXT[--vc_length] = '\0';
   }
   VCAST_strcpy( vc_line, VC_TEXT );
   return vc_length;
}

vCAST_boolean vCAST_READ_NEXT_ORDER (void)
{
   char VC_TEXT[VCAST_MAX_STRING_LENGTH];

   if ( vectorcast_feof ( vCAST_ORDER_FILE ) )
      return vCAST_false;

   /* TESTCASE NAME **********************/
   if ( VCAST_READ_TESTORDR_LINE ( vCAST_ORDER_OBJECT.VC_T ) == 0 )
      return vCAST_false;

   /* TESTCASE FILE **********************/
   if ( VCAST_READ_TESTORDR_LINE ( vCAST_ORDER_OBJECT.VC_N ) == 0 )
      return vCAST_false;
   
   /* ITERATIONS *************************/
   if ( VCAST_READ_TESTORDR_LINE ( VC_TEXT ) == 0 )
      return vCAST_false;
   vCAST_ORDER_OBJECT.VC_I = VCAST_atoi ( VC_TEXT );

   /* SLOT_DESCRIPTION *************************/
   VCAST_READ_TESTORDR_LINE ( vCAST_ORDER_OBJECT.VC_SLOT_DESCR );

   /* PRINT_DATA *************************/
   VCAST_READ_TESTORDR_LINE ( vCAST_ORDER_OBJECT.VC_PRINT_DATA );

   return vCAST_true;
} /* end vCAST_READ_NEXT_ORDER */


void vCAST_READ_COMMAND_DATA_FOR_ONE_PARAM(int vcast_unit, int vcast_sub, int param)
{
   int     CMD_FILE;
   int     VC_LENGTH;
   int     lineUnit, lineSub, lineParam;
   
   char    VC_FILENAME[VCAST_FILENAME_LENGTH];
   
   char    COMMAND_PARAM[VCAST_MAX_STRING_LENGTH];
   char    vcast_buf[VCAST_MAX_STRING_LENGTH];


   VCAST_strcpy(VC_FILENAME, vCAST_ORDER_OBJECT.VC_N);
   VCAST_strcpy(vCAST_TEST_NAME,vCAST_ORDER_OBJECT.VC_T);
   CMD_FILE = vectorcast_fopen(VC_FILENAME, "r");

   while (!vectorcast_feof(CMD_FILE)){

      vectorcast_readline(COMMAND_PARAM, CMD_FILE);

      VC_LENGTH = VCAST_strlen(COMMAND_PARAM);
      if (VC_LENGTH > 7) {
         lineUnit = vcast_get_unit_id  ( COMMAND_PARAM );
         lineSub   = vcast_get_subprogram_id ( COMMAND_PARAM );
         lineParam = vcast_get_parameter_id ( COMMAND_PARAM );

         if ((lineUnit == vcast_unit) &&
             (lineSub == vcast_sub) &&
             (lineParam == param))
         {
            vCAST_EXTRACT_DATA_FROM_COMMAND_LINE(
               vcast_buf, COMMAND_PARAM, vCAST_GET_ITERATION_COUNTER_VALUE(vcast_unit, vcast_sub) + 1);

            if(VCAST_strlen(vcast_buf) != 0)
              vCAST_RUN_DATA_IF(vcast_buf, vCAST_false);
         }
      }
   } /* end while */

   vectorcast_fclose(CMD_FILE);
} /* end READ_COMMAND_DATA_FOR_ONE_PARAMETER*/



#ifdef VCAST_NO_TYPE_SUPPORT
/* In the no type support case, we generate code to setup the slot
   specific global variables that normally setup by the reading
   of the C-.DAT file.  For example, the global objects for
   vCAST_UNIT, and vCAST_SUBPROGRAM */
#endif 

int NOTvcastIsSBFObject (char* COMMAND_PARAM) {

#ifdef VCAST_SBF_UNITS_AVAILABLE
   return !(VCAST_strcmp(&COMMAND_PARAM[vcast_get_percent_pos(COMMAND_PARAM)+1], "<<STUB>>"));
#else
   return 0;
#endif
}


void vCAST_READ_COMMAND_DATA (
          int  VC_I,
          int  UN_IT,
          int  SUB_PROG,
          vCAST_boolean CALLER_IS_STUB,
          vCAST_boolean INITIALIZE_ONLY)
{
   int     CMD_FILE;
   char    VC_FILENAME[VCAST_FILENAME_LENGTH];
   
   int     VC_LENGTH;
   
   int     HC_INDEX;
   int     UNIT_INDEX;
   int     STUB_INDEX;
   int     PARAM_INDEX;
   int     SUB_INDEX;

   char    vcast_buf[VCAST_MAX_STRING_LENGTH];
   char    COMMAND_PARAM[VCAST_MAX_STRING_LENGTH];

   VCAST_strcpy(vCAST_TEST_NAME,vCAST_ORDER_OBJECT.VC_T);
#ifdef VCAST_CPP_ENVIRONMENT
#ifndef VCAST_KEIL
   vcast_should_throw_exception = 0;
#endif
#endif
   if(CALLER_IS_STUB){
      vCAST_INCREMENT_ITERATION_COUNTER(UN_IT, SUB_PROG);
      STUB_INDEX = vCAST_GET_ITERATION_COUNTER_VALUE(UN_IT, SUB_PROG);
   } else {
      STUB_INDEX = 1;
   } /* end if-else */

   if ( VC_I == -1 )
      VCAST_strcpy( VC_FILENAME, "TESTDATA.DAT" );
   else
      VCAST_strcpy( VC_FILENAME, vCAST_ORDER_OBJECT.VC_N );

   CMD_FILE = vectorcast_fopen ( VC_FILENAME, "r" );

   while (!vectorcast_feof(CMD_FILE)){

      vectorcast_readline(COMMAND_PARAM, CMD_FILE);

      VC_LENGTH = VCAST_strlen(COMMAND_PARAM);
      if (VC_LENGTH > 7){
         HC_INDEX   = vcast_get_hc_id ( COMMAND_PARAM );
         UNIT_INDEX = vcast_get_unit_id( COMMAND_PARAM);
         SUB_INDEX  = vcast_get_subprogram_id ( COMMAND_PARAM );
         PARAM_INDEX = vcast_get_parameter_id ( COMMAND_PARAM );

         /* This determines if vCAST_RUN_DATA_IF is called on this command. */
         if( ( !CALLER_IS_STUB && (UNIT_INDEX < 7 || NOTvcastIsSBFObject(COMMAND_PARAM) ||  vCAST_isUUTorGlobal(COMMAND_PARAM) ) ) ||
             ( CALLER_IS_STUB && UNIT_INDEX == UN_IT && SUB_INDEX == SUB_PROG ) ) {
             
#ifdef VCAST_CPP_ENVIRONMENT
#ifndef VCAST_KEIL
            /* only stubs may throw exceptions */
            if (CALLER_IS_STUB){
               if ((!(HC_INDEX == 0 && UNIT_INDEX == 0 && SUB_INDEX == 0)) && PARAM_INDEX == 0) {
                  vcast_should_throw_exception = 1;
                  /* find the type index of the exception being thrown */
                  vcast_exception_index = vcast_get_nth_parameter_id (COMMAND_PARAM, 4);
               } /* end if */
            } /* end if called is a stub */
#endif
#endif
            vCAST_EXTRACT_DATA_FROM_COMMAND_LINE(vcast_buf, COMMAND_PARAM, STUB_INDEX);

            if (VCAST_strlen(vcast_buf) != 0)
               vCAST_RUN_DATA_IF(vcast_buf, INITIALIZE_ONLY);
         } /* end if need to run data if */
      } /* end if VC_LENGTH > 7 */

   } /* end while */

   vectorcast_fclose ( CMD_FILE );

} /* end vCAST_READ_COMMAND_DATA */

vCAST_boolean vCAST_SHOULD_DISPLAY_GLOBALS ( int UNIT,
                                             char VC_EVENT_FLAGS[] )

{
   vCAST_boolean retVal = vCAST_false;

   if ( ( UNIT >= 1000 ) || ( UNIT == 0 ) )
      retVal = vCAST_true;
   else
   {
      switch ( VCAST_GLOBALS_DISPLAY ) {
      case vCAST_EACH_EVENT:
         retVal = vCAST_true;
         break;
      case vCAST_RANGE_ITERATION:
         if ( VC_EVENT_FLAGS[2] == 'r' ) retVal = vCAST_true;
         break;
      case vCAST_SLOT_ITERATION:
         if ( VC_EVENT_FLAGS[1] == 'i' ) retVal = vCAST_true;
         break;
      case vCAST_TESTCASE:
         if ( VC_EVENT_FLAGS[0] == 's' ) retVal = vCAST_true;
         break;
      }
   }

   return retVal;
}


void vCAST_STORE_PARAMETER_ASCII_DATA (int UN_IT, int SUB_PROG) {

   char VC_TEXT[VCAST_MAX_STRING_LENGTH];
   int  UNIT_ID;
   int  SUB_ID;
   
   vCAST_OPEN_E0_FILE();
   vCAST_OUTPUT_FILE = vCAST_EVENT_FILE; /* set output to event file */

   while (!vectorcast_feof(vCAST_E0_FILE)){
      vectorcast_readline(VC_TEXT, vCAST_E0_FILE);
      if (VCAST_strlen(VC_TEXT) > 7) {
         UNIT_ID = vcast_get_unit_id (VC_TEXT);
         SUB_ID  = vcast_get_subprogram_id (VC_TEXT);
         if (UNIT_ID > UN_IT)
            break;
         else {
            if (SUB_ID != 0 && UNIT_ID == UN_IT && SUB_ID == SUB_PROG) {
               vCAST_RUN_DATA_IF(VC_TEXT, vCAST_false);
               }
            }
        }
      }
   vectorcast_fclose ( vCAST_E0_FILE );
}


void vCAST_STORE_GLOBAL_ASCII_DATA (void)
{
   char VC_TEXT[VCAST_MAX_STRING_LENGTH];
   int  SUB_ID;

   if (!VCAST_GLOBAL_FIRST_EVENT || 
       (VCAST_GLOBALS_DISPLAY != vCAST_EACH_EVENT))
     vCAST_USER_CODE_CAPTURE_GLOBALS();

   vCAST_OPEN_E0_FILE();
   while (!vectorcast_feof(vCAST_E0_FILE)){
      vectorcast_readline(VC_TEXT, vCAST_E0_FILE);
      if (VCAST_strlen(VC_TEXT) > 7) {
         SUB_ID = vcast_get_subprogram_id (VC_TEXT);
         if (SUB_ID == 0) {
            vCAST_RUN_DATA_IF (VC_TEXT, vCAST_false);
         }
      }
   }
   vectorcast_fclose ( vCAST_E0_FILE );
   VCAST_GLOBAL_FIRST_EVENT = 0;
}



void   vCAST_STORE_ASCII_DATA ( int UN_IT,
                                int SUB_PROG,
                                char VC_EVENT_FLAGS[] )
{
   if ( vCAST_ORDER_OBJECT.VC_PRINT_DATA[0] != 'F' ) {
      vCAST_STORE_PARAMETER_ASCII_DATA (UN_IT, SUB_PROG);

      if ( vCAST_SHOULD_DISPLAY_GLOBALS ( UN_IT, VC_EVENT_FLAGS ) )
         vCAST_STORE_GLOBAL_ASCII_DATA ();
   }
   /* if flag indicates "don't print" we still need to handle global data */
   else if ( vCAST_SHOULD_DISPLAY_GLOBALS ( UN_IT, VC_EVENT_FLAGS ) ) {
      if (!VCAST_GLOBAL_FIRST_EVENT ||
          (VCAST_GLOBALS_DISPLAY != vCAST_EACH_EVENT))
        vCAST_USER_CODE_CAPTURE_GLOBALS();
  }

} /* end vCAST_STORE_ASCII_DATA */


/* READ_COMMAND_DATA_FOR_USER_GLOBALS:
 * -----------------------------------
 * Reloads any user globals which have been modified by a (void *) stub.
 * The global array vCAST_GLOBALS_TOUCHED is used to determine whether a
 * given global needs to be reloaded.  The entry in the array is reset to
 * false after reloading the value.  */

void vCAST_READ_COMMAND_DATA_FOR_USER_GLOBALS(void)
{
   int     CMD_FILE;
   char    VC_FILENAME[VCAST_FILENAME_LENGTH];
   int     VC_LENGTH;
   char    vcast_buf[VCAST_MAX_STRING_LENGTH];
   char    unit_str[5], param_str[4];
   int     param;
   char    COMMAND_PARAM[VCAST_MAX_STRING_LENGTH];

   VCAST_strcpy(VC_FILENAME, vCAST_ORDER_OBJECT.VC_N);
   VCAST_strcpy(vCAST_TEST_NAME,vCAST_ORDER_OBJECT.VC_T);
   CMD_FILE = vectorcast_fopen(VC_FILENAME, "r");

   while (!vectorcast_feof(CMD_FILE)){

      vectorcast_readline(COMMAND_PARAM, CMD_FILE);

      VC_LENGTH = VCAST_strlen(COMMAND_PARAM);
      if (VC_LENGTH > 7) {
         vcast_get_unit_id_str (COMMAND_PARAM, unit_str);
         vcast_get_parameter_id_str (COMMAND_PARAM, param_str);
         param = VCAST_atoi(param_str);

         if ((VCAST_atoi(unit_str) == 8) &&
             (param > 0 && vCAST_GLOBALS_TOUCHED[param-1]))
         {
            vCAST_EXTRACT_DATA_FROM_COMMAND_LINE(vcast_buf,
               COMMAND_PARAM, vCAST_RANGE_COUNTER);

            if(VCAST_strlen(vcast_buf) != 0)
               vCAST_RUN_DATA_IF(vcast_buf, vCAST_false);
         }
      }
   } /* end while */

   vectorcast_fclose(CMD_FILE);

   /* Reset the globals-touched array */
   for (param = 0; param < sizeof(vCAST_GLOBALS_TOUCHED)/
                           sizeof(vCAST_boolean); param++)
      vCAST_GLOBALS_TOUCHED[param] = vCAST_false;
} 


void vCAST_OPEN_E0_FILE (void)
{
  char VC_FILENAME[VCAST_FILENAME_LENGTH];
  char vcWork[7];
  int  vcI, vcLen;
  VCAST_strcpy ( VC_FILENAME, "E0000000.DAT" );
  vcast_unsigned_to_string ( vcWork, vCAST_CURRENT_SLOT+1 );
  vcLen = VCAST_strlen ( vcWork );
  for ( vcI=0; vcI<vcLen; vcI++ )
    VC_FILENAME[8-vcLen+vcI] = vcWork[vcI];
  vCAST_E0_FILE = vectorcast_fopen ( VC_FILENAME, "r" );
}


void vCAST_CREATE_EVENT_FILE (void)
{
  if (!vCAST_EVENT_FILE_OPEN)
    vCAST_EVENT_FILE = vectorcast_fopen(
      vcast_get_filename(VCAST_ASCIIRES_DAT), "w");
  vCAST_EVENT_FILE_OPEN = vCAST_true;
}

void vCAST_CLOSE_EVENT_FILE (void)
{
  vectorcast_fclose ( vCAST_EVENT_FILE );
  vCAST_EVENT_FILE_OPEN = vCAST_false;
}

void vCAST_CREATE_INST_FILE (void)
{
   if (!vCAST_INST_FILE_OPEN){
#ifdef VCAST_NO_APPEND
      vCAST_INST_FILE = vectorcast_fopen (
         vcast_get_filename(VCAST_TESTINSS_DAT), "w");
#else
      vCAST_INST_FILE = vectorcast_fopen (
         vcast_get_filename(VCAST_TESTINSS_DAT), "a");
#endif
      vCAST_INST_FILE_OPEN = vCAST_true;
   }
}

void vCAST_CLOSE_INST_FILE (void)
{
   if (vCAST_INST_FILE_OPEN){
      vectorcast_fclose ( vCAST_INST_FILE );
      vCAST_INST_FILE_OPEN = vCAST_false;
   }
}

void vCAST_CREATE_HIST_FILE (void)
{
   if (!vCAST_HIST_FILE_OPEN){
      vCAST_HIST_FILE = vectorcast_fopen (
         vcast_get_filename(VCAST_THISTORY_DAT), "w");
      vCAST_HIST_FILE_OPEN = vCAST_true;
   }
}

void vCAST_CLOSE_HIST_FILE (void)
{
   if (vCAST_HIST_FILE_OPEN){
      vectorcast_fclose ( vCAST_HIST_FILE );
      vCAST_HIST_FILE_OPEN = vCAST_false;
   }
}

/* We do this append processing so that we can minimize
   the number of open file handles at any one time.
   VisualDSP for instance has a maximum number of
   open handles that are supported.

   The problem is that for vxSim on Windows, the
   "append" does not work.  So we have a conditional
   compile to disable this close and append */

void vCAST_RE_OPEN_HIST_FILE (void)
{
#ifndef VCAST_NO_APPEND
   if (!vCAST_HIST_FILE_OPEN){
      vCAST_HIST_FILE = vectorcast_fopen (
         vcast_get_filename(VCAST_THISTORY_DAT), "a");
      vCAST_HIST_FILE_OPEN = vCAST_true;
   }
#endif
}

void vCAST_CLOSE_HIST_FILE_FOR_EVENT (void)
{
#ifndef VCAST_NO_APPEND
   if (vCAST_HIST_FILE_OPEN){
      vectorcast_fclose ( vCAST_HIST_FILE );
      vCAST_HIST_FILE_OPEN = vCAST_false;
   }
#endif
}


/* This is deprecated ... */
#ifndef VCAST_DISABLE_UC_WRITE_EXPECTED
void vCAST_UC_WRITE_EXPECTED (const char *vcast_param, const char *vcast_name, int vcast_match, const char *vcast_actual) 
{
   vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, "-- VcastExpect" );
   vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, vcast_param );
   vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, (vcast_match ? "1" : "0") );
   vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, vcast_name );
   vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, "-- VcastActual" );
   if ( vcast_actual ) {
     vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, vcast_actual );
   }
   vectorcast_fprint_string_with_cr (VCAST_EXP_FILE, "-- VcastEnd" );
}
#endif 




/* This function is used to print the "end of slot" message at the end
   of each slot. For Compound For Batch functionality with nested
   compounds, we want the descriptor at the end of each CFB slot, not 
   each child's slots.
   NOTE: We're printing the slot separator at the end of each slot
   if we're _not_ CFB, but we probably don't need to. */
void VCAST_SLOT_SEPARATOR ( int VC_EndOfSlot, char VC_SLOT_DESCR )
{
   /* This function gets called whenever we return from a UUT, so
      we need to check if the EndOfSlot flag is set. */
   if ( VC_EndOfSlot == vCAST_true ) {

      if ( (VC_SLOT_DESCR == 'Z') || /* CFB slot */
           (VC_SLOT_DESCR>='0' && VC_SLOT_DESCR<='9') ) /* not CFB */
      {
         vectorcast_fprint_string_with_cr ( VCAST_EXP_FILE, "-- Slot " );
         vectorcast_fprint_string_with_cr ( VCAST_STDOUT, "VCAST Slot is Done!" );
         vectorcast_fprint_string_with_cr ( vCAST_OUTPUT_FILE, "-- Slot " );
         /* only for CFB we need to add a separator
            to the history and coverage files */
         if ( VC_SLOT_DESCR == 'Z') {
            vCAST_RE_OPEN_HIST_FILE();
            vectorcast_fprint_string_with_cr ( vCAST_HIST_FILE, "-- Slot " );
            vCAST_CLOSE_HIST_FILE_FOR_EVENT();
            VCAST_WRITE_TO_INST_FILE ( "-- Slot" );
         } /* add separator to history file */
      } /* need separator text */
   } /* end of slot */
}


/* This is the place to initialize any global data that is declared
   in this file.  Some of the target compilers do not by default 
   initialize global scope variables, so we do this explicitly here */
void vcastInitializeB2Data (void) {
   vCAST_INST_FILE_OPEN = vCAST_false;
   vCAST_EVENT_FILE_OPEN = vCAST_false;
   vCAST_HIST_FILE_OPEN = vCAST_false;
   vCAST_TOOL_ERROR = vCAST_false;
   vCAST_HAS_RANGE = vCAST_false;
   vCAST_SKIP_ITER = vCAST_false;
   vCAST_PARTITIONS = VCAST_FLOAT_ONE;
#ifdef VCAST_NO_MALLOC
   if (vcast_heap_allocated != 1) {
      vcast_heap_pointer = 0;
      vcast_heap_size = 0;
	  }
#endif
}

void vCAST_CHECK_ROBJECT( void* vcastRobject, int vcastCombinedID )
{
   if ( vcast_is_in_driver && !vcastRobject )
      vCAST_SET_HISTORY ( 1011, vcastCombinedID );
}

