/***********************************************
 *      VectorCAST Test Harness Component      *
 *     Copyright 2019 Vector Informatik, GmbH.    *
 *              19.sp3 (11/13/19)              *
 ***********************************************/
/*****************************************************************************
-- VectorCAST Heap and Stack Monitor
-- Copyright 2019 Vector Informatik, GmbH. --
*****************************************************************************/

/* Start of the Stack Specific Routines *************************************/
#if defined(VCAST_MONITOR_STACK)

/* We need the stack start and size defined or we cannot do anything ********/
#ifndef VCAST_MONITOR_STACK_START_ADDRESS
#error VCAST_MONITOR_STACK_START_ADDRESS is not defined
#endif
#ifndef VCAST_MONITOR_STACK_SIZE
#error VCAST_MONITOR_STACK_SIZE is not defined
#endif

/* Overlay for the stack */
unsigned char* vcast_stackOverlayArray = (unsigned char*)VCAST_MONITOR_STACK_START_ADDRESS;

/* The pattern is optional, we default to babababa */
#ifndef VCAST_MONITOR_STACK_PATTERN
#define VCAST_MONITOR_STACK_PATTERN 0xba
#endif

/* The amount of the stack that we monitor is optional, we default to 90%
   Please note you should not set this to a value much over 90% as the initial
   pattern writing could corrupt the pre-vectorcast "main" stack usage.
   For example, the vcast_main, might not get called until the RTOS has
   already used some stack.
*/
#ifndef VCAST_MONITOR_STACK_PERCENTAGE
/* 90 means that we will ignore the first 10% and monitor second 90% */
#define VCAST_MONITOR_STACK_PERCENTAGE 90
#endif


/* Stacks allocate backwards (in most cases), here is a picture
   Stack Start (*vcast_stackOverlayArray)
   ...
   ...    Monitor This Section!
   ...
   ---    Offset Based on Percentage 
   ...      
   ...    Ignore this section (allocations start down here)
   ...
   Stack End
      

   In some cases the stack grows from low to high, so this macro
   allows the user to control that case.  TI C2000 is an example 
   the default is DOWN (high to low), set this to UP to override */
   
#define VC_DOWN 0
#define VC_UP 1
#ifndef VCAST_MONITOR_STACK_DIRECTION
#define VCAST_MONITOR_STACK_DIRECTION VC_DOWN
#else
/* Whatever the user enters, treat it as up */
#define VCAST_MONITOR_STACK_DIRECTION VC_UP
#endif
      

	  


/* This is how much of the stack in bytes is NOT being monitored, the number of bytes
   in the reserved section that we are not filling and not checking  */
static unsigned int UNmonitoredStackBytes = (((VCAST_MONITOR_STACK_SIZE) * (100-VCAST_MONITOR_STACK_PERCENTAGE))/100);

/* This is how much of the stack in bytes IS being monitored */
static unsigned int monitoredStackBytes (void) {
   return ((VCAST_MONITOR_STACK_SIZE) - UNmonitoredStackBytes);
   }

/* This function returns a ptr to the lowest address of the stack to be filled and monitored */
static unsigned char* vcastStackMonitorLow (void) {
   if (VCAST_MONITOR_STACK_DIRECTION==VC_DOWN)
      return &vcast_stackOverlayArray[0];
   else
      return &vcast_stackOverlayArray[UNmonitoredStackBytes];
}
      
/* This function returns a ptr to the highest address of the stack to be filled and monitored */
static unsigned char* vcastStackMonitorHigh (void) {
   if (VCAST_MONITOR_STACK_DIRECTION==VC_DOWN)
      return &vcast_stackOverlayArray[monitoredStackBytes()-1];
   else
      return &vcast_stackOverlayArray[VCAST_MONITOR_STACK_SIZE-1];
}
      
/* This intializes a portion of the stack to VCAST_MONITOR_STACK_PATTERN */
static void vcast_initStack (void) {
   int i;
   unsigned char* Ptr = vcastStackMonitorLow();
   
   /* loop is for the maximum bytes in the monitored area,
      we loop from low memory to high, since it does not
	  matter what order we will the stack */
   for (i=0;i<monitoredStackBytes();i++) {
      *Ptr = VCAST_MONITOR_STACK_PATTERN;
      *Ptr++;
      }
}


/* This checks the stack to see how much has been used.  If less than the threshold
   has been used it returns 0, otherwise it returns the bytes used */
static unsigned int vcast_usedStack (void) {
   int i;
   int usedByteFound = 0;
   unsigned char* bottomOfStack;
   unsigned char* ptr;
   
   if (VCAST_MONITOR_STACK_DIRECTION==VC_DOWN)
      bottomOfStack = vcastStackMonitorLow();
   else
      bottomOfStack = vcastStackMonitorHigh();
       
   ptr = bottomOfStack;
   
   /* Look for the first USED cell */
   for (i=0;i<monitoredStackBytes();i++) {
      if (*ptr != VCAST_MONITOR_STACK_PATTERN) {
	     usedByteFound = 1;
         break;
		 }
      if (VCAST_MONITOR_STACK_DIRECTION==VC_DOWN)
         ptr++;
      else
         ptr--;
      }

   if (usedByteFound)
      /* if we found a used byte then i is the number of 
	     unused bytes in the monitored area */
      return ((VCAST_MONITOR_STACK_SIZE) - i);
   else
      /* otherwise, we did not use any of the monitored bytes, return 0 */
      return 0;
}
   
#endif /* End of the Stack Specific Section *********************************/


/* Start of the Heap Specific Routines *************************************/
#if defined(VCAST_MONITOR_HEAP)

/* We need the Heap start and size defined or we cannot do anything ********/
#ifndef VCAST_MONITOR_HEAP_START_ADDRESS
#error VCAST_MONITOR_HEAP_START_ADDRESS is not defined
#endif
#ifndef VCAST_MONITOR_HEAP_SIZE
#error VCAST_MONITOR_HEAP_SIZE is not defined
#endif

/* Overlay for the Heap */
unsigned char* vcast_heapOverlayArray = (unsigned char*)VCAST_MONITOR_HEAP_START_ADDRESS;

/* The pattern is optional, we default to dadadada */
#ifndef VCAST_MONITOR_HEAP_PATTERN
#define VCAST_MONITOR_HEAP_PATTERN 0xda
#endif

/* The amount of the heap that we monitor is optional, we default to 90% 
   We do this because the initial pattern writing could corrupt the 
   pre-vectorcast "main" heap usage. For example, the vcast_main, 
   might not get called until the RTOS has already used some heap */
#ifndef VCAST_MONITOR_HEAP_PERCENTAGE
#define VCAST_MONITOR_HEAP_PERCENTAGE 90
#endif




/* This is how much of the heap in bytes is NOT being monitored, the number of bytes
   in the reserved section that we are not filling and not checking  */
static unsigned int UNmonitoredHeapBytes = 
      (((VCAST_MONITOR_HEAP_SIZE) * (100-VCAST_MONITOR_HEAP_PERCENTAGE))/100);

/* This is how much of the heap in bytes IS being monitored */
static unsigned int monitoredHeapBytes (void) {
   return ((VCAST_MONITOR_HEAP_SIZE) - UNmonitoredHeapBytes);
   }

/* This intializes a portion of the heap to VCAST_MONITOR_HEAP_PATTERN */
static void vcast_initHeap (void) {
   int i;
   unsigned char* Ptr = &vcast_heapOverlayArray[UNmonitoredHeapBytes];
   
   /* Now fill the heap from the the threshold to the end */
   for (i=0;i<monitoredHeapBytes();i++) {
      *Ptr = VCAST_MONITOR_HEAP_PATTERN;
      *Ptr++;
      }
}

/* This checks from the UNmonitoredHeapBytes to the end of the
   heap to see how much has been used.  If less than the threshold
   has been used it returns 0, otherwise it returns the bytes used */
static unsigned int vcast_usedHeap (void) {
   int i;
   int usedByteFound = 0;

   /* Starting point for monitoring */
   unsigned char* Ptr =  &vcast_heapOverlayArray[(VCAST_MONITOR_HEAP_SIZE)-1];
       
   /* We need to loop backwards, because there might be holes 
      in the heap allocation */
   for (i=0;i<monitoredHeapBytes();i++) {
      if (*Ptr != VCAST_MONITOR_HEAP_PATTERN) {
	     usedByteFound = 1;
         break;
		 }
      *Ptr--;
      }

   if (usedByteFound)
      /* if we found a used byte then i is the number of 
	     unused bytes in the monitored area */     
      return ((VCAST_MONITOR_HEAP_SIZE) - i);
   else
      /* otherwise, we did not use any of the monitored bytes, return 0 */
      return 0;
}
   
#endif /* End of the Heap Specific Section *********************************/
