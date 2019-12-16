/***********************************************
 *      VectorCAST Test Harness Component      *
 *     Copyright 2019 Vector Informatik, GmbH.    *
 *              19.sp3 (11/13/19)              *
 ***********************************************/
/***********************************************
 * VectorCAST Unit Information
 *
 * Name: User Defined Globals
 *
 * Path: C:/VCAST/2019sp3/examples/environments/enterprise_testing_demo/tutorial_c/TUTORIAL_C/S0000008.c
 *
 * Type: not-stubbed
 *
 * Unit Number: 8
 *
 ***********************************************/
#ifndef VCAST_DRIVER_ONLY
#ifndef VCAST_DONT_RENAME_EXIT
#ifdef __cplusplus
extern "C" {
#endif
void exit (int status);
#ifdef __cplusplus
}
#endif
/* used to capture the exit call */
#define exit VCAST_exit
#endif /* VCAST_DONT_RENAME_EXIT */
#endif /* VCAST_DRIVER_ONLY */
#ifndef VCAST_DRIVER_ONLY
#define VCAST_USER_GLOBALS_EXTERN
#include "S0000008.c"
#define VCAST_USER_GLOBALS_INCLUDED
#endif /* VCAST_DRIVER_ONLY */
#ifdef VCAST_HEADER_EXPANSION
#ifdef VCAST_COVERAGE
#include "S0000008_exp_inst_driver.c"
#else
#include "S0000008_expanded_driver.c"
#endif /*VCAST_COVERAGE*/
#else
#include "B0000001.h"
#include "S0000002.h"
#include "S0000004.h"
#include "S0000007.h"
#include "vcast_undef_8.h"
/* Include the file which contains function prototypes
for stub processing and value/expected user code */
#include "vcast_uc_prototypes.h"
#include "vcast_stubs_8.c"
#include "vcast_ti_decls_8.h"
void VCAST_RUN_DATA_IF_8( int VCAST_SUB_INDEX, int VCAST_PARAM_INDEX ) {
  switch ( VCAST_SUB_INDEX ) {
    case 0: /* for global objects */
      switch( VCAST_PARAM_INDEX ) {
        case 1: /* for global object VECTORCAST_INT1 */
          VCAST_TI_8_2 ( &(VECTORCAST_INT1));
          break;
        case 2: /* for global object VECTORCAST_INT2 */
          VCAST_TI_8_2 ( &(VECTORCAST_INT2));
          break;
        case 3: /* for global object VECTORCAST_INT3 */
          VCAST_TI_8_2 ( &(VECTORCAST_INT3));
          break;
        case 4: /* for global object VECTORCAST_FLT1 */
          VCAST_TI_8_3 ( &(VECTORCAST_FLT1));
          break;
        case 5: /* for global object VECTORCAST_STR1 */
          VCAST_TI_8_4 ( VECTORCAST_STR1);
          break;
        case 6: /* for global object VECTORCAST_BUFFER */
          VCAST_TI_8_5 ( VECTORCAST_BUFFER);
          break;
        default:
          vCAST_TOOL_ERROR = vCAST_true;
          break;
      } /* switch( VCAST_PARAM_INDEX ) */
      break; /* case 0 (global objects) */
    default:
      vCAST_TOOL_ERROR = vCAST_true;
      break;
  } /* switch ( VCAST_SUB_INDEX ) */
}


/* An integer */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_8_2 ( int *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_8_2 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_8_2 ( int *vcast_param ) 
{
  switch (vCAST_COMMAND) {
    case vCAST_PRINT :
      if ( vcast_param == 0)
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_integer(vCAST_OUTPUT_FILE, *vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      }
      break;
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL :
    *vcast_param = ( int  ) vCAST_VALUE_INT;
    break;
  case vCAST_FIRST_VAL :
    *vcast_param = INT_MIN;
    break;
  case vCAST_MID_VAL :
    *vcast_param = (INT_MIN / 2) + (INT_MAX / 2);
    break;
  case vCAST_LAST_VAL :
    *vcast_param = INT_MAX;
    break;
  case vCAST_MIN_MINUS_1_VAL :
    *vcast_param = INT_MIN;
    *vcast_param = *vcast_param - 1;
    break;
  case vCAST_MAX_PLUS_1_VAL :
    *vcast_param = INT_MAX;
    *vcast_param = *vcast_param + 1;
    break;
  case vCAST_ZERO_VAL :
    *vcast_param = 0;
    break;
  default:
    break;
} /* end switch */
} /* end VCAST_TI_8_2 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* A float */
#if ((defined(VCAST_NO_TYPE_SUPPORT))||(defined(VCAST_NO_FLOAT)))
void VCAST_TI_8_3 ( float *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_8_3 */
#else /*((defined(VCAST_NO_TYPE_SUPPORT))||(defined(VCAST_NO_FLOAT)))*/
void VCAST_TI_8_3 ( float *vcast_param ) 
{
  switch (vCAST_COMMAND) {
    case vCAST_PRINT :
      if ( vcast_param == 0)
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_long_float(vCAST_OUTPUT_FILE, *vcast_param);
        vectorcast_fprint_string     (vCAST_OUTPUT_FILE, "\n");
      }
      break;
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL :
    *vcast_param = ( float  ) vCAST_VALUE;
    break;
  case vCAST_FIRST_VAL :
    *vcast_param = -(FLT_MAX);
    break;
  case vCAST_MID_VAL :
    *vcast_param = VCAST_FLT_MID;
    break;
  case vCAST_LAST_VAL :
    *vcast_param = FLT_MAX;
    break;
  case vCAST_MIN_MINUS_1_VAL :
    *vcast_param = -(FLT_MAX);
    *vcast_param = *vcast_param - 1;
    break;
  case vCAST_MAX_PLUS_1_VAL :
    *vcast_param = FLT_MAX;
    *vcast_param = *vcast_param + 1;
    break;
  case vCAST_ZERO_VAL :
    *vcast_param = 0;
    break;
  case vCAST_POS_INF_VAL :
    *vcast_param = VCAST_GET_POSITIVE_INFINITY ();
    break;
  case vCAST_NEG_INF_VAL :
    *vcast_param = VCAST_GET_NEGATIVE_INFINITY ();
    break;
  case vCAST_NAN_VAL :
    *vcast_param = VCAST_GET_QUIET_NAN ();
    break;
  default:
    break;
} /* end switch */
} /* end VCAST_TI_8_3 */
#endif /*((defined(VCAST_NO_TYPE_SUPPORT))||(defined(VCAST_NO_FLOAT)))*/


/* An array */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_8_4 ( char vcast_param[8] ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_8_4 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_8_4 ( char vcast_param[8] ) 
{
  {
    int VCAST_TI_8_4_array_index = 0;
    int VCAST_TI_8_4_index = 0;
    int VCAST_TI_8_4_first, VCAST_TI_8_4_last;
    int VCAST_TI_8_4_more_data; /* true if there is more data in the current command */
    int VCAST_TI_8_4_local_field = 0;
    int VCAST_TI_8_4_value_printed = 0;
    int VCAST_TI_8_4_is_string = (VCAST_FIND_INDEX()==-1);


    vcast_get_range_value (&VCAST_TI_8_4_first, &VCAST_TI_8_4_last, &VCAST_TI_8_4_more_data);
    VCAST_TI_8_4_local_field = vCAST_DATA_FIELD;
    if ( vCAST_SIZE && (!VCAST_TI_8_4_more_data)) { /* get the size of the array */
      vectorcast_fprint_integer (vCAST_OUTPUT_FILE,8);
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n");
    } else {
      int VCAST_TI_8_4_upper = 8;
      for (VCAST_TI_8_4_array_index=0; VCAST_TI_8_4_array_index< VCAST_TI_8_4_upper; VCAST_TI_8_4_array_index++){
        if ( (VCAST_TI_8_4_index >= VCAST_TI_8_4_first) && ( VCAST_TI_8_4_index <= VCAST_TI_8_4_last)){
          if ( VCAST_TI_8_4_is_string )
            VCAST_TI_STRING ( (char**)&vcast_param, sizeof ( vcast_param ), 1,VCAST_TI_8_4_upper);
          else
            VCAST_TI_8_1 ( &(vcast_param[VCAST_TI_8_4_index]));
          VCAST_TI_8_4_value_printed = 1;
          vCAST_DATA_FIELD = VCAST_TI_8_4_local_field;
        } /* if */
        if (VCAST_TI_8_4_index >= VCAST_TI_8_4_last)
          break;
        VCAST_TI_8_4_index++;
      } /* loop */
      if ((vCAST_COMMAND == vCAST_PRINT)&&(!VCAST_TI_8_4_value_printed))
        vectorcast_fprint_string(vCAST_OUTPUT_FILE,"<<past end of array>>\n");
    } /* if */
  }
} /* end VCAST_TI_8_4 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An array */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_8_5 ( int vcast_param[4] ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_8_5 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_8_5 ( int vcast_param[4] ) 
{
  {
    int VCAST_TI_8_5_array_index = 0;
    int VCAST_TI_8_5_index = 0;
    int VCAST_TI_8_5_first, VCAST_TI_8_5_last;
    int VCAST_TI_8_5_more_data; /* true if there is more data in the current command */
    int VCAST_TI_8_5_local_field = 0;
    int VCAST_TI_8_5_value_printed = 0;


    vcast_get_range_value (&VCAST_TI_8_5_first, &VCAST_TI_8_5_last, &VCAST_TI_8_5_more_data);
    VCAST_TI_8_5_local_field = vCAST_DATA_FIELD;
    if ( vCAST_SIZE && (!VCAST_TI_8_5_more_data)) { /* get the size of the array */
      vectorcast_fprint_integer (vCAST_OUTPUT_FILE,4);
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n");
    } else {
      int VCAST_TI_8_5_upper = 4;
      for (VCAST_TI_8_5_array_index=0; VCAST_TI_8_5_array_index< VCAST_TI_8_5_upper; VCAST_TI_8_5_array_index++){
        if ( (VCAST_TI_8_5_index >= VCAST_TI_8_5_first) && ( VCAST_TI_8_5_index <= VCAST_TI_8_5_last)){
          VCAST_TI_8_2 ( &(vcast_param[VCAST_TI_8_5_index]));
          VCAST_TI_8_5_value_printed = 1;
          vCAST_DATA_FIELD = VCAST_TI_8_5_local_field;
        } /* if */
        if (VCAST_TI_8_5_index >= VCAST_TI_8_5_last)
          break;
        VCAST_TI_8_5_index++;
      } /* loop */
      if ((vCAST_COMMAND == vCAST_PRINT)&&(!VCAST_TI_8_5_value_printed))
        vectorcast_fprint_string(vCAST_OUTPUT_FILE,"<<past end of array>>\n");
    } /* if */
  }
} /* end VCAST_TI_8_5 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An integer */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_8_1 ( char *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_8_1 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_8_1 ( char *vcast_param ) 
{
  switch (vCAST_COMMAND) {
    case vCAST_PRINT :
      if ( vcast_param == 0)
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        if (vcast_is_in_driver&&(isUnprintable(*vcast_param))) {
          if (*vcast_param < 0){
            vectorcast_fprint_integer(vCAST_OUTPUT_FILE, *vcast_param);
            vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
          } else if ( vCAST_HEX_NOTATION )
            vectorcast_fprint_char_hex(vCAST_OUTPUT_FILE, *vcast_param);
          else
            vectorcast_fprint_char_octl(vCAST_OUTPUT_FILE, *vcast_param);
          } else
            vectorcast_fprint_char(vCAST_OUTPUT_FILE, *vcast_param);
        }
        break;
      case vCAST_KEEP_VAL:
        break; /* KEEP doesn't do anything */
    case vCAST_SET_VAL :
      *vcast_param = ( char  ) vCAST_VALUE_INT;
      break;
    case vCAST_FIRST_VAL :
      *vcast_param = CHAR_MIN;
      break;
    case vCAST_MID_VAL :
      *vcast_param = (CHAR_MIN/2) + (CHAR_MAX/2);
      break;
    case vCAST_LAST_VAL :
      *vcast_param = CHAR_MAX;
      break;
    case vCAST_MIN_MINUS_1_VAL :
      *vcast_param = CHAR_MIN;
      *vcast_param = *vcast_param - 1;
      break;
    case vCAST_MAX_PLUS_1_VAL :
      *vcast_param = CHAR_MAX;
      *vcast_param = *vcast_param + 1;
      break;
    case vCAST_ZERO_VAL :
      *vcast_param = 0;
      break;
    default:
      break;
  } /* end switch */
} /* end VCAST_TI_8_1 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


#ifdef VCAST_PARADIGM_ADD_SEGMENT
#pragma new_codesegment(1)
#endif
void VCAST_TI_RANGE_DATA_8 ( void ) {
#define VCAST_TI_SCALAR_TYPE "NEW_SCALAR\n"
#define VCAST_TI_ARRAY_TYPE  "NEW_ARRAY\n"
  /* Range Data for TI (scalar) VCAST_TI_8_1 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_SCALAR_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"800001\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,CHAR_MIN );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,(CHAR_MIN/2) + (CHAR_MAX/2) );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,CHAR_MAX );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  /* Range Data for TI (scalar) VCAST_TI_8_2 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_SCALAR_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"800002\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,INT_MIN );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,(INT_MIN / 2) + (INT_MAX / 2) );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,INT_MAX );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
#ifndef VCAST_NO_FLOAT
  /* Range Data for TI (scalar) VCAST_TI_8_3 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_SCALAR_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"800003\n" );
  vectorcast_fprint_long_float (vCAST_OUTPUT_FILE,-(FLT_MAX) );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_long_float (vCAST_OUTPUT_FILE,VCAST_FLT_MID );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_long_float (vCAST_OUTPUT_FILE,FLT_MAX );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
#endif
  /* Range Data for TI (array) VCAST_TI_8_4 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_ARRAY_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"100001\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,8);
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
  /* Range Data for TI (array) VCAST_TI_8_5 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_ARRAY_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"100002\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,4);
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
}
/* Include the file which contains function implementations
for stub processing and value/expected user code */
#include "USER_GLOBALS_VCAST_uc.c"

void vCAST_COMMON_STUB_PROC_8(
            int unitIndex,
            int subprogramIndex,
            int robjectIndex,
            int readEobjectData )
{
   vCAST_BEGIN_STUB_PROC_8(unitIndex, subprogramIndex);
   if ( robjectIndex )
      vCAST_READ_COMMAND_DATA_FOR_ONE_PARAM( unitIndex, subprogramIndex, robjectIndex );
   if ( readEobjectData )
      vCAST_READ_COMMAND_DATA_FOR_ONE_PARAM( unitIndex, subprogramIndex, 0 );
   vCAST_SET_HISTORY( unitIndex, subprogramIndex );
   vCAST_READ_COMMAND_DATA( vCAST_CURRENT_SLOT, unitIndex, subprogramIndex, vCAST_true, vCAST_false );
   vCAST_READ_COMMAND_DATA_FOR_USER_GLOBALS();
   vCAST_STUB_PROCESSING_8(unitIndex, subprogramIndex);
}
#endif /* VCAST_HEADER_EXPANSION */
