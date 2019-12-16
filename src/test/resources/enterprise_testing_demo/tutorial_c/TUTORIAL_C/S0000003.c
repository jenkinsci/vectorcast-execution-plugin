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
#ifndef S0000002
#define S0000002 1
#endif
#define VCAST_DRIVER 1
#include "S0000007.h"
#include "S0000002.h"
#include "S0000009.h"
#include "S3_switch.h"
#include "c_cover.h"
#include "S3_extensions.h"


/* Prototypes for functions used only by S3 */
void vcastInitializeS2Data(void);
void vcastInitializeB1Data(void);
void vcastInitializeB2Data(void);
void vcastStartProfiling (void);
void vcastSaveProfilingData (void);



void vCAST_RE_OPEN_HIST_FILE(void);


/*************************************************************************
File : S0000003.c
Description : This file contains functions used to run test cases.  It 
  also has the main function for the UUT_INTE executable.
***************************************************************************/

#if defined (VCAST_MAIN)
int vcast_main (int argc, char **argv) {

#elif defined (VCAST_MAIN_NO_ARGS)
#if defined(__cplusplus)
extern "C" void vcast_main (void);
#endif
void vcast_main (void) {

#elif defined(VCAST_PARADIGM)
void Vcast_PMain(void) {

#elif defined(VCAST_PARADIGM_SC520) || defined(VCAST_NEC_V850)
void main(void) {

#elif defined (VCAST_INTEGRITY)
extern int INDRTConnected;
#pragma weak INDRTConnected

int main(int argc, char **argv) {

#elif defined (VCAST_COSMIC)
/* @near is not used for STM8 */
#ifdef VCAST_COSMIC_STM8
int main (int argc, char **argv) {
#else
@near int main (int argc, char **argv) {
#endif

#elif defined(VCAST_KEIL) && defined(__cplusplus)
EXTERN_C int main () {

#elif defined(VCAST_KEIL_51) || defined(VCAST_RENESAS)  || defined(VCAST_KEIL_ARM) || defined(VCAST_KEIL_ARM_STM32_TGT)
int main() {

#elif defined(VCAST_VXWORKS_653)

const unsigned char *__ctype = NULL;
#if defined(__cplusplus)
extern "C" int VCAST_MAIN (void) {
#else
extern int VCAST_MAIN (void) {
#endif
  __ctype = *(__ctypePtrGet ()); 

#elif defined(VCAST_uVELOSITY)

/* In the case of Micro Velosity, we need to define a "main"  */
/* Function that sets up the VectorCAST Driver as a task, and */
/* then starts the operating system.  The OS will call    */
/* the VectorCAST driver */

/*  Define the entry routine for our actual main                  */
/* This is what the prototype for the actual main would look like */
/* static GH_VALUE vcast_driver_task_entry(GH_ADDRESS input);     */
static GH_VALUE vcast_driver_entry(GH_ADDRESS input) {

#elif defined(VCAST_THREADX)

/* In the case of ThreadX, we need to define a "main" function */
/* that starts the kernel running, and a ThreadX interface function */
/* that sets up the VectorCAST Driver as a thread, which the OS can */
/* then schedule. */
int vcast_driver_entry(ULONG thread_input) {

#else
int main (int argc, char **argv) {
#endif

  int LOCAL_UNIT = 1;
  int VC_SUBPROGRAM = 1;
  int vc_m_INDEX      = 0;
  int VC_I, VC_J;
  struct vCAST_ORDER_ENTRY* Order;
  int VC_EndOfSlot;
  char VC_EVENT_FLAGS[5] = { ' ', ' ', ' ', ' ', 0 };


/* define configuration bits for pic24 target  */
#ifdef VCAST_PIC24_LMCO_TGT
  /* Flash Configuration Word 1 */
  __set_config_word(0x2ABFE, 0x3F7F);
  /* Flash Configuration Word 2  */
  __set_config_word(0x2ABFC, 0x12CC);
  /* Flash Configuration Word 3  */
  __set_config_word(0x2ABFA, 0xFFFF);
#endif


/*************************************************************************************/
/* This initializes the target for program execution on NEC v850 (but not 3378/3359) */
/* Init_Clock() and Init_Batt_Data_Set() may be required by your program             */
/* If it is, simply uncomment these lines                                            */
/*************************************************************************************/
#ifdef VCAST_NEC_V850
  _rcopy(&_S_romp, -1);
  __asm("di");    /* disable interrupt */
  reset_flag = RESF;
  /*Init_Clock();
    Init_Batt_Data_Set();*/
#endif


#ifdef VCAST_NO_EXIT
#ifndef VCAST_NO_SETJMP
  int jmp_exit_status = setjmp(jmp_exit);
  if( jmp_exit_status  != 0 ) {
    return ( jmp_exit_status );
  }
#endif
#endif

#ifdef GRUNDIG_C51_TGT
  DisableWatchdog();
#endif

#ifdef VCAST_PARADIGM
  HwTimerAlarmmStop(); /* stop timer 2 for ease of debugging */
PARADIGM_BEGIN:  
  SerialPortBegin();

#endif

#ifdef VCAST_PARADIGM_SC520
   /* serial port initialization for the Paradigm 586-Drive */
   sc_init();
   c1 = &ser1_com;
   s1_init(baud,ser1_in_buf,MAXISIZE,ser1_out_buf,MAXOSIZE,c1);
#endif
#if defined(VCAST_KEIL) && !defined(__cplusplus)
  vcast_keil_setup();
#endif

#ifdef VCAST_INTEGRITY
  if (&INDRTConnected && !INDRTConnected)
      HaltTask(CurrentTask());
#endif

  /* Startup all profiling processing */
  vcastStartProfiling();

/* by default, we shut off the buffering of stdout when we are in TRACE mode */
#if defined (VCAST_TEST_HARNESS_TRACE) && !defined (VCAST_NO_STD_FILES)
  /* This call will force the stdout to be unbuffered, this will get rid
  of the need to ever flush stdout, and more importantly means that calls
  to printf will not need any heap.  Which is important to be able
  to log malloc errors!  We only do this in the TRACE case since 
  never buffering might slow down test execution in the general case. */
  setbuf (stdout, VCAST_NULL);
#elif defined (VCAST_STDOUT_BUFFER_OFF)
  setbuf (stdout, VCAST_NULL);
#endif

  /* S2.h data is visibile directly in S3, so we just use a local function */
  vcastInitializeS2Data ();
  vcastInitializeB1Data ();
  vcastInitializeB2Data ();
  
  
  /* TracePoint #1 (main)  #########################################
  Within the Test Harness files there are several of these comment 
  blocks located at key debugging points to help with debugging test 
  harness execution on a new target.  Search for TracePoint to 
  find these locations.
  
  This call to vectorcast_initialize_io is a key point in the execution
  of the test harness for many target environments.  If your test harness 
  is configured for to use stdio then the allocation of the memory
  based file system occurs here.
  
  You should set a breakpoint on this function, and then step-over.  If
  you do not come back from the call, the most likely you have a stack
  or heap problem that can be diagnosed by setting breakpoints in the 
  ccast_io functions (file: B0000007.c|cpp)
  */ 

#ifdef VCAST_TEST_HARNESS_TRACE
vectorcast_write_to_std_out ("TracePoint S3: #1a, ");
vectorcast_write_to_std_out ("about to call: vectorcast_initialize_io()\n");
#endif
VCAST_TRACEPOINT_1:
  /* Initialize IO sets up the virtual stdin data in stdout mode  */
  vectorcast_initialize_io(vCAST_INST_FILE_OPEN, vCAST_INST_FILE);

#ifdef VCAST_TEST_HARNESS_TRACE
vectorcast_write_to_std_out ("TracePoint S3: #1b, ");
vectorcast_write_to_std_out ("back from call: vectorcast_initialize_io()\n");
#endif

  
#ifndef VCAST_NO_SIGNAL
#ifdef SIGSEGV
  signal ( SIGSEGV, vCAST_signal );
#endif
#ifdef SIGBUS
  signal ( SIGBUS, vCAST_signal );
#endif
#ifdef SIGILL
  signal ( SIGILL, vCAST_signal );
#endif
#ifdef SIGFPE
  signal ( SIGFPE, vCAST_signal );
#endif
#endif

  /* Initialize I/O */
#if defined (__HC12__) && defined (__PRODUCT_HICROSS_PLUS__)
  TERMIO_Init();
#ifdef VCAST_CW_HC12_SIM
  TERM_Direct(TERM_TO_BOTH, "VCAST_STDOUT.DAT");
#endif
#endif

  vCAST_ONE_SHOT_INIT();

  vCAST_OPEN_HARNOPTS_FILE(); /* set default harness options */
  if (vCAST_DO_DATA_IF) {
     vCAST_RESET_HARNOPTS_FILE();
     vCAST_OUTPUT_FILE = vectorcast_fopen (
       vcast_get_filename(VCAST_TEMP_DIF_DAT), "w" );
     /* Command to do DATA_IF processing */
     vCAST_RUN_DATA_IF("1.1.0.0%", vCAST_false);
  } else {
     vCAST_CREATE_INST_FILE();
     vCAST_CREATE_EVENT_FILE();
     vCAST_SET_OUTPUT_TO_EVENT_FILE();
     vCAST_CREATE_HIST_FILE();

     vcast_is_in_driver = vCAST_true;

     vCAST_OPEN_TESTORDR_FILE();
     
     /* TracePoint #2 (main)  ######################################### 
     If you get there, then the initialization part of the harness has
     completed.  This does not mean that all of the data is necessarily
     setup properly!  
  
     Each iteration of the while loop is a single test, or in the case
     of a compound test, a single slot.  So if "step" from here does not
     go into this loop, there was a problem in the data init.
     */      
#ifdef VCAST_TEST_HARNESS_TRACE
vectorcast_write_to_std_out ("TracePoint S3: #2, ");
vectorcast_write_to_std_out ("about to call: vCAST_READ_NEXT_ORDER()\n");
#endif
VCAST_TRACEPOINT_2:
     
     while ( vCAST_READ_NEXT_ORDER() != vCAST_false ) {  /* each slot */
       /* DECLARATION */
       vCAST_boolean commands_read = vCAST_false;

       /* reset partition count to default for each test */
       vCAST_PARTITIONS = VCAST_FLOAT_ONE;
       VC_EVENT_FLAGS[0] = 'S'; 
       VC_EVENT_FLAGS[1] = ' '; 
       VC_EVENT_FLAGS[2] = ' ';
       VC_EVENT_FLAGS[3] = ' ';
       vCAST_RESET_HARNOPTS_FILE();
       Order = vCAST_ORDER();
       
       /* TracePoint #3 (main)  ######################################### 
       Check what the "Order" structure looks like.
       For example, the field: VC_N should have the name of the .HAR file,
       something that looks like: "C-00000x.HAR"
       */
#ifdef VCAST_TEST_HARNESS_TRACE
vectorcast_write_to_std_out ("TracePoint S3: #3, ");
vectorcast_write_to_std_out ("after call to: vCAST_ORDER()\n");
#endif
VCAST_TRACEPOINT_3:

       vCAST_HIST_LIMIT = vCAST_ENV_HIST_LIMIT;
       vCAST_INITIALIZE_PARAMETERS();
#ifdef VCAST_SBF_UNITS_AVAILABLE
       vCAST_INITIALIZE_SBF_TABLE();
#endif
       vCAST_RESET_RANGE_VALUES();
       vCAST_CURRENT_SLOT = vc_m_INDEX;
       
       /* TracePoint #4a (main)  ######################################### 
       If you find that you are not getting to the function under test,
       or you get there, and the input data is not correct, you can come
       back to this point and step through the vCAST_READ_COMMAND_DATA
       */
#ifdef VCAST_TEST_HARNESS_TRACE
vectorcast_write_to_std_out ("TracePoint S3: #4a, ");
vectorcast_write_to_std_out ("about to call: vCAST_READ_COMMAND_DATA()\n");
#endif  
   
       vCAST_READ_COMMAND_DATA(vc_m_INDEX, 0, 0, vCAST_false, vCAST_false );
       vCAST_INITIALIZE_RANGE_VALUES();
       LOCAL_UNIT = vCAST_UNIT;
       VC_SUBPROGRAM = vCAST_SUBPROGRAM;
       
       /* TracePoint #4b (main)  ######################################### 
       Here, you can check that the globals: vCAST_UNIT, and vCAST_SUBPROGRAM
       are set properly.  When there is one unit under test, vCAST_UNIT will 
       generally be 9.
       */
VCAST_TRACEPOINT_4:
#ifdef VCAST_TEST_HARNESS_TRACE
vectorcast_write_to_std_out ("TracePoint S3: #4b, ");
vectorcast_write_to_std_out ("validate Global Data: vCAST_UNIT and vCAST_SUBPROGRAM: ");
{
char debugString[20];
vcast_unsigned_to_string ( debugString,vCAST_UNIT);
vectorcast_write_to_std_out  ( debugString );
vectorcast_write_to_std_out  ( " / " );
vcast_unsigned_to_string ( debugString,vCAST_SUBPROGRAM );
vectorcast_write_to_std_out  ( debugString );
vectorcast_write_to_std_out  ("\n");
}
#endif   
 
#ifdef VCAST_SBF_UNITS_AVAILABLE
       vcast_initialize_sbf_flag(LOCAL_UNIT, VC_SUBPROGRAM);
#endif
       VCAST_strcpy(vCAST_TEST_NAME,Order->VC_T);

       vCAST_OPEN_E0_FILE();
       VCAST_WRITE_TO_INST_FILE(Order->VC_N);

       for (VC_I=1;VC_I<=Order->VC_I;VC_I++) {      /* each iteration */
         VC_EVENT_FLAGS[1] = 'I'; 
         VC_EVENT_FLAGS[2] = ' '; 
         VC_EVENT_FLAGS[3] = ' ';
         vCAST_RANGE_COUNTER = 0;
         for (VC_J=0;VC_J<vCAST_NUM_RANGE_ITERATIONS;++VC_J) {   /* each range or list */
           ++vCAST_RANGE_COUNTER;
            vCAST_EXECUTE_RANGE_COMMANDS(VC_J);
            if ( vCAST_HAS_RANGE == vCAST_true )
               VC_EVENT_FLAGS[2] = 'R';
            vCAST_CURRENT_ITERATION = VC_I;

            vCAST_USER_CODE_INITIALIZE(vc_m_INDEX,  commands_read);
            commands_read = vCAST_true;

            /* TracePoint #5 (main)  ######################################### 
            This is where the actual invocation of the function under test 
            happens.  You can step into this call and through the downstream
            functions until you get to the function under test.  Or just set
            a break point on the function under test.  In either case, once
            you get to the function under test, you should verify that the 
            parameters match what was setup in the test case.
            */
#ifdef VCAST_TEST_HARNESS_TRACE
vectorcast_write_to_std_out ("TracePoint S3: #5, ");
vectorcast_write_to_std_out ("about to call: vcast_S3_switch()\n");
#endif
VCAST_TRACEPOINT_5:

            vcast_S3_switch( LOCAL_UNIT, VC_SUBPROGRAM, VC_EVENT_FLAGS, Order->VC_SLOT_DESCR );

            vCAST_USER_CODE_CAPTURE();

            VC_EVENT_FLAGS[0]=' '; VC_EVENT_FLAGS[1]=' '; VC_EVENT_FLAGS[2]='r';
            
            VC_EndOfSlot = vCAST_false;
            if ( vCAST_NUM_RANGE_ITERATIONS == VC_J+1 ) { /* "no more range" */
               VC_EVENT_FLAGS[1] = 'i';
               if  (VC_I == Order->VC_I ) {
                  VC_EVENT_FLAGS[0] = 's';
                  VC_EndOfSlot = vCAST_true;
                  }
            }
            vCAST_SET_HISTORY_FLAGS ( LOCAL_UNIT, VC_SUBPROGRAM, VC_EVENT_FLAGS, "" );
            VCAST_SLOT_SEPARATOR ( VC_EndOfSlot, Order->VC_SLOT_DESCR[0] );
            VC_EVENT_FLAGS[0]=' '; VC_EVENT_FLAGS[1]=' '; VC_EVENT_FLAGS[2]=' ';
            vCAST_RESET_ITERATION_COUNTERS(vCAST_MULTI_RETURN_SPANS_RANGE);      
         } /* For this Range */
         vCAST_RESET_ITERATION_COUNTERS(vCAST_MULTI_RETURN_SPANS_COMPOUND_ITERATIONS);
         vCAST_RANGE_COUNTER = 0;   /* sets the range values to the start */
         vCAST_RESET_LIST_VALUES(); /* sets the list back to the start */
       } /* For this Iteration */

       vCAST_FREE_RANGE_VALUES();
       vc_m_INDEX = vc_m_INDEX + 1;
       vectorcast_fclose ( vCAST_E0_FILE );
       vCAST_ITERATION_COUNTER_RESET();
     } /* For this Slot */

  }

  VCAST_driver_termination(0,0);

#if defined (VCAST_PARADIGM)
  return;
#elif defined(VCAST_PARADIGM_SC520) && defined(__cplusplus)
  return;
#elif defined (VCAST_NEC_V850)
  return;
#elif defined (VCAST_THREADX)
  return;
#elif defined (VCAST_MAIN_NO_ARGS)
  return;
#else
  return(0);
#endif

}
/* This is the common function to call when you want to exit the harness
   if we are exiting because of a problem like the event limit exceeded etc
*/
void VCAST_driver_termination(int status, int eventCode) {

/* TracePoint #6 (main)  ######################################### 
      If you get here, check the value of the "status" parameter
      The following error codes are possible:
         2 - Event limit exceeded
         4 - VC version of malloc failed
         5 - syslib malloc failed
         6 - syslib realloc failed
         7 - Output Buffer Overflow
      In the case of values 4-6 which are memory errors, we do not
      do all of the normal termination processing, we just exit
   */
VCAST_TRACEPOINT_6:
#ifdef VCAST_TEST_HARNESS_TRACE
{
char debugString[20];
vectorcast_write_to_std_out ("TracePoint S3: #6, ");
vectorcast_write_to_std_out ("in driver_termination (). Exit status is: ");
vcast_unsigned_to_string ( debugString, status);
vectorcast_write_to_std_out  ( debugString );
vectorcast_write_to_std_out  ( "\n" );
}
#endif   

/* The minimal termination does not work in "file mode" */

#if defined(VCAST_MINIMAL_TERMINATION) && (defined (VCAST_STDIO) || defined (VCAST_NO_STDIN))

    if (eventCode > 0) {
      vectorcast_fprint_integer (vCAST_HIST_FILE, eventCode);
      vectorcast_fprint_string  (vCAST_HIST_FILE, ",1\n");
    }      
    vectorcast_fprint_string (vCAST_HIST_FILE, "0,0\n");
    /* Force a new line with the initial \n */
    vectorcast_print_string ("\nVCAST.END:END\n");
    vCAST_END();
    
#ifndef VCAST_NO_EXIT
#undef exit
    exit( status );
#endif

#else 

  if (status == 7) {
     /* 7 is the output buffer over-flow and we can get into some nasty
        recursion if we even try to write the normal stuff since
        every write uses buffer.  So just end
     */
     vCAST_END();
     }
     
  else if (vCAST_DO_DATA_IF) {
    vectorcast_fclose(vCAST_OUTPUT_FILE);
    vCAST_WRITE_END_FILE();
    vCAST_END();
    }
  /* else this is a real test */
  else {
  
      /* We don't use calls to vCAST_SET_HISTORY because this can cause recursion 
         By default, we close the HIST file after each event and then open in 
         append mode for the next event.  So we have to call RE_OPEN here.
      */
      vCAST_RE_OPEN_HIST_FILE();
      if (eventCode > 0) {
          vectorcast_fprint_integer (vCAST_HIST_FILE, eventCode);
          vectorcast_fprint_string  (vCAST_HIST_FILE, ",1\n");
          vectorcast_fprint_string_with_cr ( VCAST_EXP_FILE, "-- Event" );
      }      
      vectorcast_fprint_string (vCAST_HIST_FILE, "0,0\n");
      vectorcast_fprint_string_with_cr ( VCAST_EXP_FILE, "-- Event" );
      /* if the source code being tested called exit() */
      if (eventCode == 1008) {
          vCAST_STORE_GLOBAL_ASCII_DATA ();
      }
      vectorcast_fclose(vCAST_ORDER_FILE);

#ifndef VCAST_DUMP_COVERAGE_AT_EXIT
      VCAST_DUMP_COVERAGE_DATA();
#endif /* VCAST_DUMP_COVERAGE_AT_EXIT */    

      vCAST_CLOSE_INST_FILE();
      vCAST_CLOSE_EVENT_FILE();
      vCAST_CLOSE_HIST_FILE();

     /* Exit Codes of 4,5 and 6 are malloc errors, so just exit */
     if (status >= 4 && status <= 6) {
         vectorcast_write_to_std_out ("Harness Memory Error: skipping normal termination processing!\n");
         vectorcast_write_to_std_out ("VCAST is Done!\n");
         vCAST_END();
         } 
     else {
        vCAST_ONE_SHOT_TERM();
        vcastSaveProfilingData ();
        vectorcast_write_to_std_out ("VCAST is Done!\n");
        vCAST_WRITE_END_FILE();
        vCAST_END();
        vcast_is_in_driver = vCAST_false;

#ifdef VCAST_PARADIGM_SC520
#ifndef SC520_DELAY
#define SC520_DELAY 10
#endif
         s1_flush(c1);
         delay_ctr = SC520_DELAY;
         while(delay_ctr) {delay_ctr--;}
#endif

     } /* end if status == 0 */
   } /* end if real test case */
  
#ifndef VCAST_NO_EXIT
#undef exit
   exit( status );
#else
#ifndef VCAST_NO_SETJMP
   longjmp( jmp_exit, status );
#endif
#endif

   if( status != 0 ) {
       vectorcast_print_string( "VCAST_NO_EXIT and VCAST_NO_SETJMP are set - not able to exit.\n" );
       }
#endif /*VCAST_MINIMAL_TERMINATION*/
}  /* end function */


/* This is the place to initialize any global data that is declared
   in the S2 file, which is actually instantiated in this S3 file.  
   Some of the target compilers do not by default initialize global 
   scope variables, so we do this explicitly here */
   
void vcastInitializeS2Data (void) {

    VCAST_DEFAULT_DO_COMBINATION = 0;
    VCAST_DEFAULT_FULL_STRINGS = 0;
    VCAST_DEFAULT_HEX_NOTATION = 0;
    vCAST_DO_DATA_IF = 0;
    vCAST_EVENT_FILE_OPEN = vCAST_false;
    VCAST_GLOBAL_FIRST_EVENT = 1;
    VCAST_GLOBALS_DISPLAY = 0;
    vCAST_HAS_RANGE = vCAST_false;
    vCAST_HIST_INDEX = 1;
    vCAST_HIST_FILE_OPEN = vCAST_false;
    vCAST_INST_FILE_OPEN = vCAST_false;
    vCAST_RANGE_COUNTER = 0;
    vCAST_SKIP_ITER = vCAST_false;
    vCAST_SUBPROGRAM = 1;
    vCAST_TOOL_ERROR = vCAST_false;    
    vCAST_UNIT = 9;
}



void vCAST_END(void)
{
  /* The purpose of this function is to provide a place to put a breakpoint 
     at the end of the execution of the harness.  For some debuggers, we
     place a watchpoint on the global vcast_exit_flag.
     VCAST_END_LABEL is used for Cosmic HC12
  */


  
  
VCAST_END_LABEL:
  vcast_exit_flag = 1;
  
  /* This object is used for some targets to set an object write BP */
  /* This is used for Keil and maybe others ... */
  vcast_already_exited = vCAST_true;
  

}

#if defined(VCAST_uVELOSITY)

/* In the case of Micro Velosity, we need to define a "main"  */
/* Function that sets up the VectorCAST Driver as a task, and */
/* then starts the operating system.  The OS will call    */
/* the VectorCAST driver */

#if defined(__cplusplus)
extern "C" int main(int argc, char **argv) 
#else
extern int main(int argc, char **argv) 
#endif
{
    /* Create any initial kernel objects. */
    gh_task_create(&vcast_driver, "vcast_driver", vcast_driver_entry, 0,
            vcast_driver_stack, sizeof(vcast_driver_stack), 10, GH_AUTO_START);
    /* Start uvelOSity. This call never returns. */
    uv_kernel_start();
    return 0;    
}

#endif

#if defined(VCAST_THREADX)

/* In the case of ThreadX, we need to define a "main" function */
/* that starts the kernel running, and a ThreadX interface function */
/* that sets up the VectorCAST Driver as a thread, which the OS can */
/* then schedule. */

void main() 
{
    /* Enter the ThreadX kernel. This call never returns. */
    tx_kernel_enter( );
}

/* It may be necessary to tweak this value on memory-constrained targets */
#define VCAST_THREADX_DRIVER_STACK 0x1000

/* Set up the driver as a ThreadX thread */
void tx_application_define(void *first_unused_memory)
{
    /* Create the VectorCAST Driver thread */
    tx_thread_create(&vcast_driver, "VectorCAST Driver Thread", 
            vcast_driver_entry, 0x0, 
            first_unused_memory, VCAST_THREADX_DRIVER_STACK,
            3, 3, TX_NO_TIME_SLICE, TX_AUTO_START);
}

#endif /* VCAST_THREADX */

/*****************************************************************************
 Profiling Data
 *****************************************************************************/

#if defined(VCAST_MONITOR_STACK) || defined(VCAST_MONITOR_HEAP)
#include "HeapAndStackMonitor.c"
#endif

/* Startup all profiling processing */
void vcastStartProfiling () {

/*****************************************************************************/
#if defined(VCAST_MONITOR_STACK)
   vcast_initStack();
#endif

/*****************************************************************************/
#if defined(VCAST_MONITOR_HEAP)
   vcast_initHeap();
#endif

}

/* Output all profiling data */
void vcastSaveProfilingData () {
#if defined(VCAST_MONITOR_STACK)
   int stackUsage;
#endif
#if defined(VCAST_MONITOR_HEAP)
   int heapUsage;
#endif
   int fileHandle;
   
   fileHandle = vectorcast_fopen("ProfileData.txt", "w");

/*****************************************************************************/
#if defined(VCAST_MONITOR_STACK)
   stackUsage = vcast_usedStack ();
   vectorcast_fprint_string  (fileHandle, "stack_data\n");
   vectorcast_fprint_string  (fileHandle, "size: ");
   vectorcast_fprint_integer (fileHandle, VCAST_MONITOR_STACK_SIZE);
   vectorcast_fprint_string  (fileHandle, "\n");
   vectorcast_fprint_string  (fileHandle, "unmonitored(bytes): ");
   vectorcast_fprint_integer (fileHandle, UNmonitoredStackBytes);
   vectorcast_fprint_string  (fileHandle, "\n");
   vectorcast_fprint_string  (fileHandle, "usage: ");
   vectorcast_fprint_integer (fileHandle, stackUsage);
   vectorcast_fprint_string  (fileHandle, "\n");          
   vectorcast_fprint_string  (fileHandle, "stack_data_end\n");        
#endif

/*****************************************************************************/
#if defined(VCAST_MONITOR_HEAP)
   heapUsage = vcast_usedHeap ();
   vectorcast_fprint_string  (fileHandle, "heap_data\n");
   vectorcast_fprint_string  (fileHandle, "size: ");
   vectorcast_fprint_integer (fileHandle, VCAST_MONITOR_HEAP_SIZE);
   vectorcast_fprint_string  (fileHandle, "\n");
   vectorcast_fprint_string  (fileHandle, "unmonitored(bytes): ");
   vectorcast_fprint_integer (fileHandle, UNmonitoredHeapBytes);
   vectorcast_fprint_string  (fileHandle, "\n");
   vectorcast_fprint_string  (fileHandle, "usage: ");
   vectorcast_fprint_integer (fileHandle, heapUsage);
   vectorcast_fprint_string  (fileHandle, "\n");          
   vectorcast_fprint_string  (fileHandle, "heap_data_end\n");   
#endif

   vectorcast_fclose(fileHandle);

}


