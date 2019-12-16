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
#ifndef S3_SWITCH_H
#define S3_SWITCH_H   

void vcast_S3_switch( int, int, char *, char * );

#ifdef VCAST_SBF_UNITS_AVAILABLE
void vcast_initialize_sbf_flag( int, int );
#endif /* VCAST_SBF_UNITS_AVAILABLE */

#endif /* S3_SWITCH_H */
