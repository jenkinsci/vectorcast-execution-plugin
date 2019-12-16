#include "S0000009.h"
void vcast_S3_switch( int VCAST_UNIT_INDEX, int VC_SUBPROGRAM, char *VC_EVENT_FLAGS, char *VC_SLOT_DESCR ) {
  switch( VCAST_UNIT_INDEX ) {
    case 9:
      vCAST_USER_CODE_TIMER_START();
      VCAST_DRIVER_9( VC_SUBPROGRAM, VC_EVENT_FLAGS, VC_SLOT_DESCR );
      vCAST_USER_CODE_TIMER_STOP();
      break;
    default:
      vCAST_TOOL_ERROR = vCAST_true;
      break;
  } /* switch */
} /* vcast_S3_switch */
#ifdef VCAST_SBF_UNITS_AVAILABLE
void vcast_initialize_sbf_flag( int VCAST_UNIT_INDEX, int VC_SUBPROGRAM ) {
  switch( VCAST_UNIT_INDEX ) {
    case 9:
      VCAST_SBF_9( VC_SUBPROGRAM );
      break;
  } /* switch */ 
} /* vcast_initialize_sbf_flag */
#endif /* VCAST_SBF_UNITS_AVAILABLE */
