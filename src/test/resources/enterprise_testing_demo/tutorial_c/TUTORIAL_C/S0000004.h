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

/*************************************************************************
File : S0000004.c
Description : This file contains the declarations of functions in the 
   B0000004.c file.
***************************************************************************/
#ifndef VCAST_S0000004_C
#define VCAST_S0000004_C

#include "S0000002.h"

#ifdef __cplusplus
extern "C" {
#endif
void vCAST_INITIALIZE_PARAMETERS(void);
void vCAST_USER_CODE_INITIALIZE(int vcast_slot_index, vCAST_boolean commands_read);
void vCAST_USER_CODE_CAPTURE (void);
void vCAST_USER_CODE_CAPTURE_GLOBALS (void);

void vCAST_ONE_SHOT_INIT(void);
void vCAST_ONE_SHOT_TERM(void);

void vCAST_GLOBAL_STUB_PROCESSING(void);
void vCAST_GLOBAL_BEGINNING_OF_STUB_PROCESSING(void);

typedef enum {
   VCAST_UCT_VALUE,
   VCAST_UCT_EXPECTED,
   VCAST_UCT_EXPECTED_GLOBALS
} VCAST_USER_CODE_TYPE;

void vCAST_USER_CODE( VCAST_USER_CODE_TYPE uct, int vcast_slot_index );

#ifdef VCAST_CPP_ENVIRONMENT
extern vCAST_boolean vcast_initializing;
extern vCAST_boolean vcast_commands_read;
#endif
#ifdef __cplusplus
}
#endif
#endif

