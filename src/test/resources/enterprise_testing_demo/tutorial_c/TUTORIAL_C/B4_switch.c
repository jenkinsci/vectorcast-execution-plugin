#include "S0000004.h"
void VCAST_USER_CODE_UNIT_8( VCAST_USER_CODE_TYPE, int );
void VCAST_USER_CODE_UNIT_9( VCAST_USER_CODE_TYPE, int );
void vcast_B4_switch( int VCAST_UNIT_INDEX, VCAST_USER_CODE_TYPE uct, int vcast_slot_index ) {
  switch( VCAST_UNIT_INDEX ) {
    case 0:
#if defined(VCAST_CPP_ENVIRONMENT)
      vcast_initializing = vCAST_true;
#endif
      VCAST_USER_CODE_UNIT_8( uct, vcast_slot_index );
      VCAST_USER_CODE_UNIT_9( uct, vcast_slot_index );
#if defined(VCAST_CPP_ENVIRONMENT)
      if (uct == VCAST_UCT_VALUE) {
        if (!vcast_commands_read)
          vCAST_READ_COMMAND_DATA(vcast_slot_index, 0, 0, vCAST_false, vCAST_true);
        vcast_initializing = vCAST_false;
      VCAST_USER_CODE_UNIT_8( uct, vcast_slot_index );
      VCAST_USER_CODE_UNIT_9( uct, vcast_slot_index );
        }
#endif
        break;
    case 8:
      VCAST_USER_CODE_UNIT_8( uct, vcast_slot_index );
      break;
    case 9:
      VCAST_USER_CODE_UNIT_9( uct, vcast_slot_index );
      break;
    } /* switch */
  } /* vcast_B4_switch */
