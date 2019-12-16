void VCAST_RUN_DATA_IF_8( int, int );
void VCAST_RUN_DATA_IF_9( int, int );
void VCAST_TI_RANGE_DATA_8(void);
void VCAST_TI_RANGE_DATA_9(void);
#include "B0000001.h"
#include "S0000002.h"
void vcast_B1_switch( int VCAST_UNIT_INDEX, int VCAST_SUB_INDEX, int VCAST_PARAM_INDEX, char *work )
{
  switch( VCAST_UNIT_INDEX ) {
    case 0:
      switch( VCAST_PARAM_INDEX ) {
        case 0:
          vCAST_SUBPROGRAM = (int)vCAST_VALUE;
          break;
        case 2:
        /* deprecated */
      break;
    case 3:
      vCAST_UNIT = (int)vCAST_VALUE;
      break;
    case 4:
      vCAST_SET_TESTCASE_CONFIGURATION_OPTIONS( VCAST_SUB_INDEX, VCAST_atoi(work), 0 );
      break;
    case 9:
      vCAST_SET_TESTCASE_OPTIONS ( work );
      break;
    default:
      vCAST_TOOL_ERROR = vCAST_true;
      break;
  } /* switch VCAST_PARAM_INDEX */
  break; /* case 0 */
case 1: /* TI RANGE DATA */
  VCAST_TI_RANGE_DATA_8();
  VCAST_TI_RANGE_DATA_9();
  break;
case 8:
  VCAST_RUN_DATA_IF_8(VCAST_SUB_INDEX, VCAST_PARAM_INDEX);
  break;
case 9:
  VCAST_RUN_DATA_IF_9(VCAST_SUB_INDEX, VCAST_PARAM_INDEX);
  break;
case 10: /* PROTOTYPES */
  switch( VCAST_SUB_INDEX ) {
    case 0: /* Defined externs */
      switch( VCAST_PARAM_INDEX ) {
        default:
          vCAST_TOOL_ERROR = vCAST_true;
          break;
      } /* switch */
      break;
        case 1:
          /* For Get_Table_Record */
          VCAST_RUN_DATA_IF_9( 7, VCAST_PARAM_INDEX );
          break;
        case 2:
          /* For Update_Table_Record */
          VCAST_RUN_DATA_IF_9( 8, VCAST_PARAM_INDEX );
          break;
    default:
      vCAST_TOOL_ERROR = vCAST_true;
      break;
  } /* switch */
  break; /* case 10 */
} /* switch */
} /* vcast_B1_switch */

int vCAST_ITERATION_COUNTER_SWITCH( int VCAST_UNIT_INDEX)
{
  return VCAST_UNIT_INDEX - 8;
} /* vCAST_ITERATION_COUNTER_SWITCH */
