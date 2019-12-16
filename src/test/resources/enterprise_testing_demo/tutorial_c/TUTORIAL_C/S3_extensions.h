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

#ifndef __S3_EXTENSIONS_H__
#define __S3_EXTENSIONS_H__

/*  file that contains platform/compiler specific  harness code */
#ifndef VCAST_NO_SIGNAL
#include <signal.h>
#endif

#ifdef VCAST_KEIL
#include "vcast_keil_setup.h"
#endif

/* STVD environment does not break on breakpoints, but on watches */
/* VCAST_OUTPUT_VIA_DEBUGGER needs to flag the debugger it can exit its polling loop */
int vcast_exit_flag = 0;


#ifdef VCAST_IAR_4x_TS7300
typedef struct
{
   void (*boot_sdread)();
   void (*boot_ser_puts)();
   unsigned int SystemID;
   unsigned char* DirectoryTail;
   unsigned int StartOfData;
} PARAM_SAVE;
__no_init const PARAM_SAVE __param_save;

/* external prototypes  */
void boot_sdread(unsigned int start_sector, char* buffer, int num_sectors);
void boot_ser_puts(char* buffer);

#endif

#ifdef VCAST_PIC24_LMCO_TGT
#include <intrinsics.h>
#include "io24fj256gb110.h"
#include "MemInterface.h"
#endif

#if defined (__HC12__) && defined (__PRODUCT_HICROSS_PLUS__)
#include <termio.h>
#ifdef VCAST_CW_HC12_SIM
#include <terminal.h>
#endif
#endif

#ifdef VCAST_VXWORKS_653
#include "vThreadsData.h"
#endif

#include "vcast_c_options.h"


#ifdef VCAST_PARADIGM
#define	__PASCAL 	__pascal

#if !defined(CSIM_BUILD)
#define __FAR	__far
#else
#define __FAR	
#endif

void            SerialPortBegin(void);
void Vcast_PMain(void);

extern "C"
{
      void __PASCAL __FAR HwTimerAlarmmStop(void);
};
#endif

#ifdef VCAST_PARADIGM_SC520

#ifndef _586_H_
#define _586_H_
typedef struct  {
	unsigned char ready; /* TRUE when ready */
	unsigned char baud;
	unsigned int mode; 
	unsigned char iflag;   /* interrupt status */
	unsigned char* in_buf; /* Input buffer */
	unsigned int in_tail; /* Input buffer TAIL ptr */
	unsigned int in_head; /* Input buffer HEAD ptr */
	unsigned int in_size; /* Input buffer size */
	unsigned int in_crcnt; /* Input <CR> count */
	unsigned char in_mt; /* Input buffer FLAG */
	unsigned char in_full; /* input buffer full */
	unsigned char* out_buf; /* Output buffer */
	unsigned int out_tail; /* Output buffer TAIL ptr */
	unsigned int out_head; /* Output buffer HEAD ptr */
	unsigned int out_size; /* Output buffer size */
	unsigned char out_full; /* Output buffer FLAG */
	unsigned char out_mt; /* Output buffer MT */
	unsigned char tmso;	/* transmit macro service operation */
	unsigned char rts;
	unsigned char dtr;
	unsigned char en485;
	unsigned char err;
	unsigned char node;
	unsigned char cr; /* scc CR register */
	unsigned char slave;
	unsigned int in_segm; /* input buffer segment */
	unsigned int in_offs; /* input buffer offset */
	unsigned int out_segm; /* output buffer segment */
	unsigned int out_offs; /* output buffer offset */
	unsigned char byte_delay; /* V26 macro service byte delay */
} COM;

#ifdef __cplusplus
extern "C" void sc_init(void);
extern "C" void s1_close(COM *c);
extern "C" void s1_flush(COM *c);
#include "heapsize.c"  /* allow for dynamic memory allocation */
#else
void sc_init(void);
void s1_close(COM *c);
void s1_flush(COM *c);
#endif 

#endif

#ifdef __cplusplus
extern "C" {
#endif 

#include "SER1.h"

#ifdef __cplusplus
}
#endif 

#define MAXOSIZE 1024
#define MAXISIZE 1024
unsigned char ser1_out_buf[MAXOSIZE];
unsigned char ser1_in_buf[MAXISIZE];
unsigned int delay_ctr;
extern COM ser1_com;

   COM * c1;
#ifndef SC520_BAUD
#define SC520_BAUD 8
#endif
/*    baud = 8,  19,200 baud (default)
*     baud = 9,  38,400 baud
*     baud = 10, 57,600 baud
*     baud = 11, 115,200 baud 
*/
unsigned char baud = SC520_BAUD;
#endif  /* end ifdef VCAST_PARADIGM_SC520  */


#ifdef VCAST_NO_EXIT
#ifndef VCAST_NO_SETJMP
  jmp_buf jmp_exit;
#endif
#endif

vCAST_boolean vcast_already_exited = vCAST_false;


#ifdef VCAST_BUFFER_OUTPUT
#ifndef VCAST_OUTPUT_BUFFER_SIZE
#define VCAST_OUTPUT_BUFFER_SIZE 20000
#endif
extern char vcast_output_buffer[VCAST_OUTPUT_BUFFER_SIZE];
#endif

#if defined(GRUNDIG_C51_TGT)
extern void DisableWatchdog() ;
#endif

/******************************************************************/
/* The following need to be included for NEC F3618 and F3619      */
/* Clock output settings may need to be uncommented in some cases */
/******************************************************************/
#if defined (VCAST_NEC_V850)
#include 	"define.h"
#include	"init.h"
#include 	"isr.h"
#include 	"comm.h"
#include	"macrodriver.h"
#include    "comm.h"
#define	ROOT
#include 	"main.h"
#pragma ioreg
/*#define	_CLKOUT		//Clock Output setting
// Skip - I/O port setting*/
#include    "initialize.c"
int _rcopy(unsigned long *, long);
extern unsigned long _S_romp;
#endif /* VCAST_NEC_V850 */

#if defined(VCAST_uVELOSITY)

/* In the case of Micro Velosity, we need to define a "main"  */
/* Function that sets up the VectorCAST Driver as a task, and */
/* then starts the operating system.  The OS will call    */
/* the VectorCAST driver */
#include <gh_os.h>
#include <stdio.h>

static GH_TASK vcast_driver;
static GH_STACK_AREA_DEFINE(vcast_driver_stack, 0x8000);

#endif /* VCAST_uVELOSITY */

#if defined(VCAST_THREADX)

/* In the case of ThreadX, we need to define a "main" function */
/* that starts the kernel running, and a ThreadX interface function */
/* that sets up the VectorCAST Driver as a thread, which the OS can */
/* then schedule. */
#include    "tx_api.h"
TX_THREAD vcast_driver;

#endif /* VCAST_THREADX */

#endif /* __S3_EXTENSIONS_H__ */
