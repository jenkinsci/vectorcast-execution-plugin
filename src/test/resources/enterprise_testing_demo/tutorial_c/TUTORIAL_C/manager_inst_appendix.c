typedef int VECTORCAST_MARKER__UNIT_APPENDIX_END;
typedef int VECTORCAST_MARKER__USER_GLOBALS_START;
/*****************************************************************************

S0000008.c: This file contains the definitions of variables used in user code.

Preface all variable declarations with VCAST_USER_GLOBALS_EXTERN to ensure 

that only one definition of the variable is created in the test harness. 

*****************************************************************************/
  extern int VECTORCAST_INT1;
  extern int VECTORCAST_INT2;
  extern int VECTORCAST_INT3;
  extern float VECTORCAST_FLT1;
  extern char VECTORCAST_STR1[8];
  extern int VECTORCAST_BUFFER[4];
