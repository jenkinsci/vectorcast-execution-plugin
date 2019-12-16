/***********************************************
 *      VectorCAST Test Harness Component      *
 *     Copyright 2019 Vector Informatik, GmbH.    *
 *              19.sp3 (11/13/19)              *
 ***********************************************/
/***********************************************
 * VectorCAST Unit Information
 *
 * Name: manager
 *
 * Path: C:/VCAST/2019sp3/tutorial/c/manager.c
 *
 * Type: stub-by-function
 *
 * Unit Number: 9
 *
 ***********************************************/
#ifndef VCAST_DRIVER_ONLY
/* Include the file which contains function prototypes
for stub processing and value/expected user code */
#include "vcast_uc_prototypes.h"
#include "vcast_basics.h"
/* STUB_DEPENDENCY_USER_CODE */
/* STUB_DEPENDENCY_USER_CODE_END */
#else
#include "vcast_env_defines.h"
#define __VCAST_BASICS_H__
#endif /* VCAST_DRIVER_ONLY */
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
#define VCAST_HEADER_EXPANSION
#ifdef VCAST_COVERAGE
#include "manager_inst_prefix.c"
#else
#include "manager_vcast_prefix.c"
#endif
#ifdef VCAST_COVERAGE
/* If coverage is enabled, include the instrumented UUT */
#include "manager_inst.c"
#else
/* If coverage is not enabled, include the original UUT */
#include "manager_vcast.c"
#endif
#ifdef VCAST_COVERAGE
#include "manager_inst_appendix.c"
#else
#include "manager_vcast_appendix.c"
#endif
#endif /* VCAST_DRIVER_ONLY */
#include "manager_driver_prefix.c"
#ifdef VCAST_HEADER_EXPANSION
#ifdef VCAST_COVERAGE
#include "manager_exp_inst_driver.c"
#else
#include "manager_expanded_driver.c"
#endif /*VCAST_COVERAGE*/
#else
#include "S0000009.h"
#include "vcast_undef_9.h"
/* Include the file which contains function prototypes
for stub processing and value/expected user code */
#include "vcast_uc_prototypes.h"
#include "vcast_stubs_9.c"
/* begin declarations of inlined friends */
/* end declarations of inlined friends */
void VCAST_DRIVER_9( int VC_SUBPROGRAM, char *VC_EVENT_FLAGS, char *VC_SLOT_DESCR ) {
#ifdef VCAST_SBF_UNITS_AVAILABLE
  vCAST_MODIFY_SBF_TABLE(9, VC_SUBPROGRAM, vCAST_false);
#endif
  switch( VC_SUBPROGRAM ) {
    case 0:
      vCAST_SET_HISTORY_FLAGS ( 9, 0, VC_EVENT_FLAGS, VC_SLOT_DESCR );
      break;
    case 1: {
      /* void Add_Included_Dessert(struct order_type * Order) */
      vCAST_SET_HISTORY_FLAGS ( 9, 1, VC_EVENT_FLAGS, VC_SLOT_DESCR );
      ( Add_Included_Dessert(
        ( P_9_1_1 ) ) );
      break; }
    case 2: {
      /* int Place_Order(table_index_type Table, seat_index_type Seat, struct order_type Order) */
      vCAST_SET_HISTORY_FLAGS ( 9, 2, VC_EVENT_FLAGS, VC_SLOT_DESCR );
      R_9_2 = 
      ( Place_Order(
        ( P_9_2_1 ),
        ( P_9_2_2 ),
        ( P_9_2_3 ) ) );
      break; }
    case 3: {
      /* int Clear_Table(table_index_type Table) */
      vCAST_SET_HISTORY_FLAGS ( 9, 3, VC_EVENT_FLAGS, VC_SLOT_DESCR );
      R_9_3 = 
      ( Clear_Table(
        ( P_9_3_1 ) ) );
      break; }
    case 4: {
      /* FLOAT Get_Check_Total(table_index_type Table) */
      vCAST_SET_HISTORY_FLAGS ( 9, 4, VC_EVENT_FLAGS, VC_SLOT_DESCR );
      R_9_4 = 
      ( Get_Check_Total(
        ( P_9_4_1 ) ) );
      break; }
    case 5: {
      /* void Add_Party_To_Waiting_List(char * Name) */
      vCAST_SET_HISTORY_FLAGS ( 9, 5, VC_EVENT_FLAGS, VC_SLOT_DESCR );
      ( Add_Party_To_Waiting_List(
        ( P_9_5_1 ) ) );
      break; }
    case 6: {
      /* char *Get_Next_Party_To_Be_Seated(void) */
      vCAST_SET_HISTORY_FLAGS ( 9, 6, VC_EVENT_FLAGS, VC_SLOT_DESCR );
      R_9_6 = 
      ( Get_Next_Party_To_Be_Seated( ) );
      break; }
    default:
      vectorcast_print_string("ERROR: Internal Tool Error\n");
      break;
  } /* switch */
}

void VCAST_SBF_9( int VC_SUBPROGRAM ) {
  switch( VC_SUBPROGRAM ) {
    case 1: {
      SBF_9_1 = 0;
      break; }
    case 2: {
      SBF_9_2 = 0;
      break; }
    case 3: {
      SBF_9_3 = 0;
      break; }
    case 4: {
      SBF_9_4 = 0;
      break; }
    case 5: {
      SBF_9_5 = 0;
      break; }
    case 6: {
      SBF_9_6 = 0;
      break; }
    default:
      break;
  } /* switch */
}
#include "vcast_ti_decls_9.h"
void VCAST_RUN_DATA_IF_9( int VCAST_SUB_INDEX, int VCAST_PARAM_INDEX ) {
  switch ( VCAST_SUB_INDEX ) {
    case 0: /* for global objects */
      switch( VCAST_PARAM_INDEX ) {
        case 1: /* for global object WaitingList */
          VCAST_TI_9_15 ( WaitingList);
          break;
        case 2: /* for global object WaitingListSize */
          VCAST_TI_9_3 ( &(WaitingListSize));
          break;
        case 3: /* for global object WaitingListIndex */
          VCAST_TI_9_3 ( &(WaitingListIndex));
          break;
        default:
          vCAST_TOOL_ERROR = vCAST_true;
          break;
      } /* switch( VCAST_PARAM_INDEX ) */
      break; /* case 0 (global objects) */
    case 7: /* function Get_Table_Record */
      switch ( VCAST_PARAM_INDEX ) {
        case 1:
          VCAST_TI_9_17 ( &(P_10_1_1));
          break;
        case 2:
          VCAST_TI_9_9 ( &(R_10_1));
          break;
      } /* switch ( VCAST_PARAM_INDEX ) */
      break; /* function Get_Table_Record */
    case 8: /* function Update_Table_Record */
      switch ( VCAST_PARAM_INDEX ) {
        case 1:
          VCAST_TI_9_17 ( &(P_10_2_1));
          break;
        case 2:
          VCAST_TI_9_9 ( &(P_10_2_2));
          break;
      } /* switch ( VCAST_PARAM_INDEX ) */
      break; /* function Update_Table_Record */
    case 1: /* function Add_Included_Dessert */
      switch ( VCAST_PARAM_INDEX ) {
        case 1:
          VCAST_TI_9_1 ( &(P_9_1_1));
          break;
        case 2:
          VCAST_TI_SBF_OBJECT( &SBF_9_1 );
          break;
      } /* switch ( VCAST_PARAM_INDEX ) */
      break; /* function Add_Included_Dessert */
    case 2: /* function Place_Order */
      switch ( VCAST_PARAM_INDEX ) {
        case 1:
          VCAST_TI_9_17 ( &(P_9_2_1));
          break;
        case 2:
          VCAST_TI_9_17 ( &(P_9_2_2));
          break;
        case 3:
          VCAST_TI_9_8 ( &(P_9_2_3));
          break;
        case 4:
          VCAST_TI_8_2 ( &(R_9_2));
          break;
        case 5:
          VCAST_TI_SBF_OBJECT( &SBF_9_2 );
          break;
      } /* switch ( VCAST_PARAM_INDEX ) */
      break; /* function Place_Order */
    case 3: /* function Clear_Table */
      switch ( VCAST_PARAM_INDEX ) {
        case 1:
          VCAST_TI_9_17 ( &(P_9_3_1));
          break;
        case 2:
          VCAST_TI_8_2 ( &(R_9_3));
          break;
        case 3:
          VCAST_TI_SBF_OBJECT( &SBF_9_3 );
          break;
      } /* switch ( VCAST_PARAM_INDEX ) */
      break; /* function Clear_Table */
    case 4: /* function Get_Check_Total */
      switch ( VCAST_PARAM_INDEX ) {
        case 1:
          VCAST_TI_9_17 ( &(P_9_4_1));
          break;
        case 2:
          VCAST_TI_8_3 ( &(R_9_4));
          break;
        case 3:
          VCAST_TI_SBF_OBJECT( &SBF_9_4 );
          break;
      } /* switch ( VCAST_PARAM_INDEX ) */
      break; /* function Get_Check_Total */
    case 5: /* function Add_Party_To_Waiting_List */
      switch ( VCAST_PARAM_INDEX ) {
        case 1:
          VCAST_TI_9_13 ( &(P_9_5_1));
          break;
        case 2:
          VCAST_TI_SBF_OBJECT( &SBF_9_5 );
          break;
      } /* switch ( VCAST_PARAM_INDEX ) */
      break; /* function Add_Party_To_Waiting_List */
    case 6: /* function Get_Next_Party_To_Be_Seated */
      switch ( VCAST_PARAM_INDEX ) {
        case 1:
          VCAST_TI_9_13 ( &(R_9_6));
          break;
        case 2:
          VCAST_TI_SBF_OBJECT( &SBF_9_6 );
          break;
      } /* switch ( VCAST_PARAM_INDEX ) */
      break; /* function Get_Next_Party_To_Be_Seated */
    default:
      vCAST_TOOL_ERROR = vCAST_true;
      break;
  } /* switch ( VCAST_SUB_INDEX ) */
}


/* An array */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_15 ( char vcast_param[10][32] ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_15 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_15 ( char vcast_param[10][32] ) 
{
  {
    int VCAST_TI_9_15_array_index = 0;
    int VCAST_TI_9_15_index = 0;
    int VCAST_TI_9_15_first, VCAST_TI_9_15_last;
    int VCAST_TI_9_15_more_data; /* true if there is more data in the current command */
    int VCAST_TI_9_15_local_field = 0;
    int VCAST_TI_9_15_value_printed = 0;


    vcast_get_range_value (&VCAST_TI_9_15_first, &VCAST_TI_9_15_last, &VCAST_TI_9_15_more_data);
    VCAST_TI_9_15_local_field = vCAST_DATA_FIELD;
    if ( vCAST_SIZE && (!VCAST_TI_9_15_more_data)) { /* get the size of the array */
      vectorcast_fprint_integer (vCAST_OUTPUT_FILE,10);
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
      vectorcast_fprint_integer (vCAST_OUTPUT_FILE,32);
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n");
    } else {
      int VCAST_TI_9_15_upper = 10;
      for (VCAST_TI_9_15_array_index=0; VCAST_TI_9_15_array_index< VCAST_TI_9_15_upper; VCAST_TI_9_15_array_index++){
        if ( (VCAST_TI_9_15_index >= VCAST_TI_9_15_first) && ( VCAST_TI_9_15_index <= VCAST_TI_9_15_last)){
          VCAST_TI_9_19 ( vcast_param[VCAST_TI_9_15_index]);
          VCAST_TI_9_15_value_printed = 1;
          vCAST_DATA_FIELD = VCAST_TI_9_15_local_field;
        } /* if */
        if (VCAST_TI_9_15_index >= VCAST_TI_9_15_last)
          break;
        VCAST_TI_9_15_index++;
      } /* loop */
      if ((vCAST_COMMAND == vCAST_PRINT)&&(!VCAST_TI_9_15_value_printed))
        vectorcast_fprint_string(vCAST_OUTPUT_FILE,"<<past end of array>>\n");
    } /* if */
  }
} /* end VCAST_TI_9_15 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An integer */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_3 ( unsigned *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_3 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_3 ( unsigned *vcast_param ) 
{
  switch (vCAST_COMMAND) {
    case vCAST_PRINT :
      if ( vcast_param == 0)
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_unsigned_integer(vCAST_OUTPUT_FILE, *vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      }
      break;
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL :
    *vcast_param = ( unsigned  ) vCAST_VALUE_UNSIGNED;
    break;
  case vCAST_FIRST_VAL :
    *vcast_param = UINT_MIN;
    break;
  case vCAST_MID_VAL :
    *vcast_param = (UINT_MIN / 2) + (UINT_MAX / 2);
    break;
  case vCAST_LAST_VAL :
    *vcast_param = UINT_MAX;
    break;
  case vCAST_MIN_MINUS_1_VAL :
    *vcast_param = UINT_MIN;
    *vcast_param = *vcast_param - 1;
    break;
  case vCAST_MAX_PLUS_1_VAL :
    *vcast_param = UINT_MAX;
    *vcast_param = *vcast_param + 1;
    break;
  case vCAST_ZERO_VAL :
    *vcast_param = 0;
    break;
  default:
    break;
} /* end switch */
} /* end VCAST_TI_9_3 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An integer */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_17 ( unsigned short *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_17 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_17 ( unsigned short *vcast_param ) 
{
  switch (vCAST_COMMAND) {
    case vCAST_PRINT :
      if ( vcast_param == 0)
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_unsigned_short(vCAST_OUTPUT_FILE, *vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      }
      break;
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL :
    *vcast_param = ( unsigned short  ) vCAST_VALUE_INT;
    break;
  case vCAST_FIRST_VAL :
    *vcast_param = USHRT_MIN;
    break;
  case vCAST_MID_VAL :
    *vcast_param = (USHRT_MIN / 2) + (USHRT_MAX / 2);
    break;
  case vCAST_LAST_VAL :
    *vcast_param = USHRT_MAX;
    break;
  case vCAST_MIN_MINUS_1_VAL :
    *vcast_param = USHRT_MIN;
    *vcast_param = *vcast_param - 1;
    break;
  case vCAST_MAX_PLUS_1_VAL :
    *vcast_param = USHRT_MAX;
    *vcast_param = *vcast_param + 1;
    break;
  case vCAST_ZERO_VAL :
    *vcast_param = 0;
    break;
  default:
    break;
} /* end switch */
} /* end VCAST_TI_9_17 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* A struct */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_9 ( struct table_data_type *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_9 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_9 ( struct table_data_type *vcast_param ) 
{
  {
    switch ( vcast_get_param () ) { /* Choose field member */
      /* Setting member variable vcast_param->Is_Occupied */
      case 1: { 
        VCAST_TI_9_10 ( &(vcast_param->Is_Occupied));
        break; /* end case 1*/
      } /* end case */
      /* Setting member variable vcast_param->Number_In_Party */
      case 2: { 
        VCAST_TI_9_17 ( &(vcast_param->Number_In_Party));
        break; /* end case 2*/
      } /* end case */
      /* Setting member variable vcast_param->Designator */
      case 3: { 
        VCAST_TI_8_1 ( &(vcast_param->Designator));
        break; /* end case 3*/
      } /* end case */
      /* Setting member variable vcast_param->Wait_Person */
      case 4: { 
        VCAST_TI_9_18 ( vcast_param->Wait_Person);
        break; /* end case 4*/
      } /* end case */
      /* Setting member variable vcast_param->Order */
      case 5: { 
        VCAST_TI_9_11 ( vcast_param->Order);
        break; /* end case 5*/
      } /* end case */
      /* Setting member variable vcast_param->Check_Total */
      case 6: { 
        VCAST_TI_8_3 ( &(vcast_param->Check_Total));
        break; /* end case 6*/
      } /* end case */
      default:
        vCAST_TOOL_ERROR = vCAST_true;
    } /* end switch */ 
  }
} /* end VCAST_TI_9_9 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* A pointer */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_1 ( struct order_type **vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_1 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_1 ( struct order_type **vcast_param ) 
{
  {
    int VCAST_TI_9_1_index;
    if (((*vcast_param) == 0) && (vCAST_COMMAND != vCAST_ALLOCATE)){
      if ( vCAST_COMMAND == vCAST_PRINT )
        vectorcast_fprint_string(vCAST_OUTPUT_FILE,"null\n");
    } else {
      if ( (vCAST_COMMAND_IS_MIN_MAX == vCAST_true) &&( VCAST_FIND_INDEX() < 0 ) ) {
        switch ( vCAST_COMMAND ) {
          case vCAST_PRINT     :
            vectorcast_fprint_string(vCAST_OUTPUT_FILE,"0\n");
          case vCAST_FIRST_VAL :
          case vCAST_LAST_VAL  :
          case vCAST_POS_INF_VAL  :
          case vCAST_NEG_INF_VAL  :
          case vCAST_NAN_VAL  :
            break;
          default :
            vCAST_TOOL_ERROR = vCAST_true;
        }
      } else {
        if (vCAST_COMMAND == vCAST_ALLOCATE && vcast_proc_handles_command(1)) {
          int VCAST_TI_9_1_array_size = (int) vCAST_VALUE;
          if (VCAST_FIND_INDEX() == -1) {
            void **VCAST_TI_9_1_memory_ptr = (void**)vcast_param;
            *VCAST_TI_9_1_memory_ptr = (void*)VCAST_malloc(VCAST_TI_9_1_array_size*(sizeof(struct order_type )));
            VCAST_memset((void*)*vcast_param, 0x0, VCAST_TI_9_1_array_size*(sizeof(struct order_type )));
#ifndef VCAST_NO_MALLOC
            VCAST_Add_Allocated_Data(*VCAST_TI_9_1_memory_ptr);
#endif
          }
        } else if (vCAST_VALUE_NUL == vCAST_true && vcast_proc_handles_command(1)) {
          if (VCAST_FIND_INDEX() == -1)
            *vcast_param = 0;
        } else {
          VCAST_TI_9_1_index = vcast_get_param();
          VCAST_TI_9_8 ( &((*vcast_param)[VCAST_TI_9_1_index]));
        }
      }
    }
  }
} /* end VCAST_TI_9_1 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* A struct */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_8 ( struct order_type *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_8 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_8 ( struct order_type *vcast_param ) 
{
  {
    switch ( vcast_get_param () ) { /* Choose field member */
      /* Setting member variable vcast_param->Soup */
      case 1: { 
        VCAST_TI_9_12 ( &(vcast_param->Soup));
        break; /* end case 1*/
      } /* end case */
      /* Setting member variable vcast_param->Salad */
      case 2: { 
        VCAST_TI_9_5 ( &(vcast_param->Salad));
        break; /* end case 2*/
      } /* end case */
      /* Setting member variable vcast_param->Entree */
      case 3: { 
        VCAST_TI_9_2 ( &(vcast_param->Entree));
        break; /* end case 3*/
      } /* end case */
      /* Setting member variable vcast_param->Dessert */
      case 4: { 
        VCAST_TI_9_7 ( &(vcast_param->Dessert));
        break; /* end case 4*/
      } /* end case */
      /* Setting member variable vcast_param->Beverage */
      case 5: { 
        VCAST_TI_9_6 ( &(vcast_param->Beverage));
        break; /* end case 5*/
      } /* end case */
      default:
        vCAST_TOOL_ERROR = vCAST_true;
    } /* end switch */ 
  }
} /* end VCAST_TI_9_8 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* A pointer */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_13 ( char **vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_13 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_13 ( char **vcast_param ) 
{
  {
    int VCAST_TI_9_13_index;
    if (((*vcast_param) == 0) && (vCAST_COMMAND != vCAST_ALLOCATE)){
      if ( vCAST_COMMAND == vCAST_PRINT )
        vectorcast_fprint_string(vCAST_OUTPUT_FILE,"null\n");
    } else {
      if ( (vCAST_COMMAND_IS_MIN_MAX == vCAST_true) &&( VCAST_FIND_INDEX() < 0 ) ) {
        switch ( vCAST_COMMAND ) {
          case vCAST_PRINT     :
            vectorcast_fprint_string(vCAST_OUTPUT_FILE,"0\n");
          case vCAST_FIRST_VAL :
          case vCAST_LAST_VAL  :
          case vCAST_POS_INF_VAL  :
          case vCAST_NEG_INF_VAL  :
          case vCAST_NAN_VAL  :
            break;
          default :
            vCAST_TOOL_ERROR = vCAST_true;
        }
      } else {
        if (vCAST_COMMAND == vCAST_ALLOCATE && vcast_proc_handles_command(1)) {
          int VCAST_TI_9_13_array_size = (int) vCAST_VALUE;
          if (VCAST_FIND_INDEX() == -1) {
            void **VCAST_TI_9_13_memory_ptr = (void**)vcast_param;
            *VCAST_TI_9_13_memory_ptr = (void*)VCAST_malloc(VCAST_TI_9_13_array_size*(sizeof(char )));
            VCAST_memset((void*)*vcast_param, 0x0, VCAST_TI_9_13_array_size*(sizeof(char )));
#ifndef VCAST_NO_MALLOC
            VCAST_Add_Allocated_Data(*VCAST_TI_9_13_memory_ptr);
#endif
          }
        } else if (vCAST_VALUE_NUL == vCAST_true && vcast_proc_handles_command(1)) {
          if (VCAST_FIND_INDEX() == -1)
            *vcast_param = 0;
        } else {
          if (VCAST_FIND_INDEX() == -1 )
            VCAST_TI_STRING ( (char**)vcast_param, sizeof ( vcast_param ), 0,-1);
          else {
            VCAST_TI_9_13_index = vcast_get_param();
            VCAST_TI_8_1 ( &((*vcast_param)[VCAST_TI_9_13_index]));
          }
        }
      }
    }
  }
} /* end VCAST_TI_9_13 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An array */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_19 ( char vcast_param[32] ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_19 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_19 ( char vcast_param[32] ) 
{
  {
    int VCAST_TI_9_19_array_index = 0;
    int VCAST_TI_9_19_index = 0;
    int VCAST_TI_9_19_first, VCAST_TI_9_19_last;
    int VCAST_TI_9_19_more_data; /* true if there is more data in the current command */
    int VCAST_TI_9_19_local_field = 0;
    int VCAST_TI_9_19_value_printed = 0;
    int VCAST_TI_9_19_is_string = (VCAST_FIND_INDEX()==-1);


    vcast_get_range_value (&VCAST_TI_9_19_first, &VCAST_TI_9_19_last, &VCAST_TI_9_19_more_data);
    VCAST_TI_9_19_local_field = vCAST_DATA_FIELD;
    if ( vCAST_SIZE && (!VCAST_TI_9_19_more_data)) { /* get the size of the array */
      vectorcast_fprint_integer (vCAST_OUTPUT_FILE,32);
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n");
    } else {
      int VCAST_TI_9_19_upper = 32;
      for (VCAST_TI_9_19_array_index=0; VCAST_TI_9_19_array_index< VCAST_TI_9_19_upper; VCAST_TI_9_19_array_index++){
        if ( (VCAST_TI_9_19_index >= VCAST_TI_9_19_first) && ( VCAST_TI_9_19_index <= VCAST_TI_9_19_last)){
          if ( VCAST_TI_9_19_is_string )
            VCAST_TI_STRING ( (char**)&vcast_param, sizeof ( vcast_param ), 1,VCAST_TI_9_19_upper);
          else
            VCAST_TI_8_1 ( &(vcast_param[VCAST_TI_9_19_index]));
          VCAST_TI_9_19_value_printed = 1;
          vCAST_DATA_FIELD = VCAST_TI_9_19_local_field;
        } /* if */
        if (VCAST_TI_9_19_index >= VCAST_TI_9_19_last)
          break;
        VCAST_TI_9_19_index++;
      } /* loop */
      if ((vCAST_COMMAND == vCAST_PRINT)&&(!VCAST_TI_9_19_value_printed))
        vectorcast_fprint_string(vCAST_OUTPUT_FILE,"<<past end of array>>\n");
    } /* if */
  }
} /* end VCAST_TI_9_19 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An enumeration */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_10 ( enum boolean *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_10 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_10 ( enum boolean *vcast_param ) 
{
  switch ( vCAST_COMMAND ) {
    case vCAST_PRINT: {
      if ( vcast_param == 0 )
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_long_long(vCAST_OUTPUT_FILE, (VCAST_LONGEST_INT)*vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      } /* end else */
      } /* end vCAST_PRINT block */
      break; /* end case vCAST_PRINT */
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL:
    *vcast_param = (enum boolean ) vCAST_VALUE_INT;
    break;
  case vCAST_FIRST_VAL:
    *vcast_param = v_false;
    break; /* end case vCAST_FIRST_VAL */
  case vCAST_LAST_VAL:
    *vcast_param = v_true;
    break; /* end case vCAST_LAST_VAL */
  default:
    vCAST_TOOL_ERROR = vCAST_true;
    break; /* end case default */
} /* end switch */
} /* end VCAST_TI_9_10 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An array */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_18 ( char vcast_param[10] ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_18 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_18 ( char vcast_param[10] ) 
{
  {
    int VCAST_TI_9_18_array_index = 0;
    int VCAST_TI_9_18_index = 0;
    int VCAST_TI_9_18_first, VCAST_TI_9_18_last;
    int VCAST_TI_9_18_more_data; /* true if there is more data in the current command */
    int VCAST_TI_9_18_local_field = 0;
    int VCAST_TI_9_18_value_printed = 0;
    int VCAST_TI_9_18_is_string = (VCAST_FIND_INDEX()==-1);


    vcast_get_range_value (&VCAST_TI_9_18_first, &VCAST_TI_9_18_last, &VCAST_TI_9_18_more_data);
    VCAST_TI_9_18_local_field = vCAST_DATA_FIELD;
    if ( vCAST_SIZE && (!VCAST_TI_9_18_more_data)) { /* get the size of the array */
      vectorcast_fprint_integer (vCAST_OUTPUT_FILE,10);
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n");
    } else {
      int VCAST_TI_9_18_upper = 10;
      for (VCAST_TI_9_18_array_index=0; VCAST_TI_9_18_array_index< VCAST_TI_9_18_upper; VCAST_TI_9_18_array_index++){
        if ( (VCAST_TI_9_18_index >= VCAST_TI_9_18_first) && ( VCAST_TI_9_18_index <= VCAST_TI_9_18_last)){
          if ( VCAST_TI_9_18_is_string )
            VCAST_TI_STRING ( (char**)&vcast_param, sizeof ( vcast_param ), 1,VCAST_TI_9_18_upper);
          else
            VCAST_TI_8_1 ( &(vcast_param[VCAST_TI_9_18_index]));
          VCAST_TI_9_18_value_printed = 1;
          vCAST_DATA_FIELD = VCAST_TI_9_18_local_field;
        } /* if */
        if (VCAST_TI_9_18_index >= VCAST_TI_9_18_last)
          break;
        VCAST_TI_9_18_index++;
      } /* loop */
      if ((vCAST_COMMAND == vCAST_PRINT)&&(!VCAST_TI_9_18_value_printed))
        vectorcast_fprint_string(vCAST_OUTPUT_FILE,"<<past end of array>>\n");
    } /* if */
  }
} /* end VCAST_TI_9_18 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An array */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_11 ( struct order_type vcast_param[4] ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_11 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_11 ( struct order_type vcast_param[4] ) 
{
  {
    int VCAST_TI_9_11_array_index = 0;
    int VCAST_TI_9_11_index = 0;
    int VCAST_TI_9_11_first, VCAST_TI_9_11_last;
    int VCAST_TI_9_11_more_data; /* true if there is more data in the current command */
    int VCAST_TI_9_11_local_field = 0;
    int VCAST_TI_9_11_value_printed = 0;


    vcast_get_range_value (&VCAST_TI_9_11_first, &VCAST_TI_9_11_last, &VCAST_TI_9_11_more_data);
    VCAST_TI_9_11_local_field = vCAST_DATA_FIELD;
    if ( vCAST_SIZE && (!VCAST_TI_9_11_more_data)) { /* get the size of the array */
      vectorcast_fprint_integer (vCAST_OUTPUT_FILE,4);
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
      vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n");
    } else {
      int VCAST_TI_9_11_upper = 4;
      for (VCAST_TI_9_11_array_index=0; VCAST_TI_9_11_array_index< VCAST_TI_9_11_upper; VCAST_TI_9_11_array_index++){
        if ( (VCAST_TI_9_11_index >= VCAST_TI_9_11_first) && ( VCAST_TI_9_11_index <= VCAST_TI_9_11_last)){
          VCAST_TI_9_8 ( &(vcast_param[VCAST_TI_9_11_index]));
          VCAST_TI_9_11_value_printed = 1;
          vCAST_DATA_FIELD = VCAST_TI_9_11_local_field;
        } /* if */
        if (VCAST_TI_9_11_index >= VCAST_TI_9_11_last)
          break;
        VCAST_TI_9_11_index++;
      } /* loop */
      if ((vCAST_COMMAND == vCAST_PRINT)&&(!VCAST_TI_9_11_value_printed))
        vectorcast_fprint_string(vCAST_OUTPUT_FILE,"<<past end of array>>\n");
    } /* if */
  }
} /* end VCAST_TI_9_11 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An enumeration */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_12 ( enum soups *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_12 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_12 ( enum soups *vcast_param ) 
{
  switch ( vCAST_COMMAND ) {
    case vCAST_PRINT: {
      if ( vcast_param == 0 )
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_long_long(vCAST_OUTPUT_FILE, (VCAST_LONGEST_INT)*vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      } /* end else */
      } /* end vCAST_PRINT block */
      break; /* end case vCAST_PRINT */
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL:
    *vcast_param = (enum soups ) vCAST_VALUE_INT;
    break;
  case vCAST_FIRST_VAL:
    *vcast_param = NO_SOUP;
    break; /* end case vCAST_FIRST_VAL */
  case vCAST_LAST_VAL:
    *vcast_param = CHOWDER;
    break; /* end case vCAST_LAST_VAL */
  default:
    vCAST_TOOL_ERROR = vCAST_true;
    break; /* end case default */
} /* end switch */
} /* end VCAST_TI_9_12 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An enumeration */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_5 ( enum salads *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_5 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_5 ( enum salads *vcast_param ) 
{
  switch ( vCAST_COMMAND ) {
    case vCAST_PRINT: {
      if ( vcast_param == 0 )
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_long_long(vCAST_OUTPUT_FILE, (VCAST_LONGEST_INT)*vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      } /* end else */
      } /* end vCAST_PRINT block */
      break; /* end case vCAST_PRINT */
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL:
    *vcast_param = (enum salads ) vCAST_VALUE_INT;
    break;
  case vCAST_FIRST_VAL:
    *vcast_param = NO_SALAD;
    break; /* end case vCAST_FIRST_VAL */
  case vCAST_LAST_VAL:
    *vcast_param = GREEN;
    break; /* end case vCAST_LAST_VAL */
  default:
    vCAST_TOOL_ERROR = vCAST_true;
    break; /* end case default */
} /* end switch */
} /* end VCAST_TI_9_5 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An enumeration */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_2 ( enum entrees *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_2 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_2 ( enum entrees *vcast_param ) 
{
  switch ( vCAST_COMMAND ) {
    case vCAST_PRINT: {
      if ( vcast_param == 0 )
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_long_long(vCAST_OUTPUT_FILE, (VCAST_LONGEST_INT)*vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      } /* end else */
      } /* end vCAST_PRINT block */
      break; /* end case vCAST_PRINT */
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL:
    *vcast_param = (enum entrees ) vCAST_VALUE_INT;
    break;
  case vCAST_FIRST_VAL:
    *vcast_param = NO_ENTREE;
    break; /* end case vCAST_FIRST_VAL */
  case vCAST_LAST_VAL:
    *vcast_param = PASTA;
    break; /* end case vCAST_LAST_VAL */
  default:
    vCAST_TOOL_ERROR = vCAST_true;
    break; /* end case default */
} /* end switch */
} /* end VCAST_TI_9_2 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An enumeration */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_7 ( enum desserts *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_7 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_7 ( enum desserts *vcast_param ) 
{
  switch ( vCAST_COMMAND ) {
    case vCAST_PRINT: {
      if ( vcast_param == 0 )
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_long_long(vCAST_OUTPUT_FILE, (VCAST_LONGEST_INT)*vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      } /* end else */
      } /* end vCAST_PRINT block */
      break; /* end case vCAST_PRINT */
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL:
    *vcast_param = (enum desserts ) vCAST_VALUE_INT;
    break;
  case vCAST_FIRST_VAL:
    *vcast_param = NO_DESSERT;
    break; /* end case vCAST_FIRST_VAL */
  case vCAST_LAST_VAL:
    *vcast_param = FRUIT;
    break; /* end case vCAST_LAST_VAL */
  default:
    vCAST_TOOL_ERROR = vCAST_true;
    break; /* end case default */
} /* end switch */
} /* end VCAST_TI_9_7 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


/* An enumeration */
#if (defined(VCAST_NO_TYPE_SUPPORT))
void VCAST_TI_9_6 ( enum beverages *vcast_param ) 
{
  /* User code: type is not supported */
  vcast_not_supported();
} /* end VCAST_TI_9_6 */
#else /*(defined(VCAST_NO_TYPE_SUPPORT))*/
void VCAST_TI_9_6 ( enum beverages *vcast_param ) 
{
  switch ( vCAST_COMMAND ) {
    case vCAST_PRINT: {
      if ( vcast_param == 0 )
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_long_long(vCAST_OUTPUT_FILE, (VCAST_LONGEST_INT)*vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      } /* end else */
      } /* end vCAST_PRINT block */
      break; /* end case vCAST_PRINT */
    case vCAST_KEEP_VAL:
      break; /* KEEP doesn't do anything */
  case vCAST_SET_VAL:
    *vcast_param = (enum beverages ) vCAST_VALUE_INT;
    break;
  case vCAST_FIRST_VAL:
    *vcast_param = NO_BEVERAGE;
    break; /* end case vCAST_FIRST_VAL */
  case vCAST_LAST_VAL:
    *vcast_param = SODA;
    break; /* end case vCAST_LAST_VAL */
  default:
    vCAST_TOOL_ERROR = vCAST_true;
    break; /* end case default */
} /* end switch */
} /* end VCAST_TI_9_6 */
#endif /*(defined(VCAST_NO_TYPE_SUPPORT))*/


#ifdef VCAST_PARADIGM_ADD_SEGMENT
#pragma new_codesegment(1)
#endif
void VCAST_TI_RANGE_DATA_9 ( void ) {
#define VCAST_TI_SCALAR_TYPE "NEW_SCALAR\n"
#define VCAST_TI_ARRAY_TYPE  "NEW_ARRAY\n"
  /* Range Data for TI (scalar) VCAST_TI_9_3 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_SCALAR_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"900003\n" );
  vectorcast_fprint_unsigned_integer (vCAST_OUTPUT_FILE,UINT_MIN );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_unsigned_integer (vCAST_OUTPUT_FILE,(UINT_MIN / 2) + (UINT_MAX / 2) );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_unsigned_integer (vCAST_OUTPUT_FILE,UINT_MAX );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  /* Range Data for TI (scalar) VCAST_TI_8_2 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_SCALAR_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"900004\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,INT_MIN );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,(INT_MIN / 2) + (INT_MAX / 2) );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,INT_MAX );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  /* Range Data for TI (array) VCAST_TI_9_11 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_ARRAY_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"100003\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,4);
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
  /* Range Data for TI (scalar) VCAST_TI_8_1 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_SCALAR_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"900014\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,CHAR_MIN );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,(CHAR_MIN/2) + (CHAR_MAX/2) );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,CHAR_MAX );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  /* Range Data for TI (array) VCAST_TI_9_15 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_ARRAY_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"100004\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,10);
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,32);
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
#ifndef VCAST_NO_FLOAT
  /* Range Data for TI (scalar) VCAST_TI_8_3 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_SCALAR_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"900015\n" );
  vectorcast_fprint_long_float (vCAST_OUTPUT_FILE,-(FLT_MAX) );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_long_float (vCAST_OUTPUT_FILE,VCAST_FLT_MID );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_long_float (vCAST_OUTPUT_FILE,FLT_MAX );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
#endif
  /* Range Data for TI (scalar) VCAST_TI_9_17 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_SCALAR_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"900016\n" );
  vectorcast_fprint_unsigned_short (vCAST_OUTPUT_FILE,USHRT_MIN );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_unsigned_short (vCAST_OUTPUT_FILE,(USHRT_MIN / 2) + (USHRT_MAX / 2) );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  vectorcast_fprint_unsigned_short (vCAST_OUTPUT_FILE,USHRT_MAX );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"\n" );
  /* Range Data for TI (array) VCAST_TI_9_18 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_ARRAY_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"100005\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,10);
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
  /* Range Data for TI (array) VCAST_TI_9_19 */
  vectorcast_fprint_string (vCAST_OUTPUT_FILE, VCAST_TI_ARRAY_TYPE );
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"100006\n" );
  vectorcast_fprint_integer (vCAST_OUTPUT_FILE,32);
  vectorcast_fprint_string (vCAST_OUTPUT_FILE,"%%\n");
}
/* Include the file which contains function implementations
for stub processing and value/expected user code */
#include "manager_uc.c"

void vCAST_COMMON_STUB_PROC_9(
            int unitIndex,
            int subprogramIndex,
            int robjectIndex,
            int readEobjectData )
{
   vCAST_BEGIN_STUB_PROC_9(unitIndex, subprogramIndex);
   if ( robjectIndex )
      vCAST_READ_COMMAND_DATA_FOR_ONE_PARAM( unitIndex, subprogramIndex, robjectIndex );
   if ( readEobjectData )
      vCAST_READ_COMMAND_DATA_FOR_ONE_PARAM( unitIndex, subprogramIndex, 0 );
   vCAST_SET_HISTORY( unitIndex, subprogramIndex );
   vCAST_READ_COMMAND_DATA( vCAST_CURRENT_SLOT, unitIndex, subprogramIndex, vCAST_true, vCAST_false );
   vCAST_READ_COMMAND_DATA_FOR_USER_GLOBALS();
   vCAST_STUB_PROCESSING_9(unitIndex, subprogramIndex);
}
#endif /* VCAST_HEADER_EXPANSION */
