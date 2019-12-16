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
#include "B0000001.h"
#include "B1_switch.h"
#include "vcast_env_defines.h"

#ifndef VCAST_NO_SIGNAL
#include <signal.h>
#endif

/* Global objects externed in B1.h */
char vCAST_PARAMETER[VCAST_MAX_STRING_LENGTH];
char vCAST_PARAMETER_KEY[VCAST_MAX_STRING_LENGTH];

VCAST_LONGEST_INT vCAST_VALUE_INT   = 0;
VCAST_LONGEST_UNSIGNED vCAST_VALUE_UNSIGNED   = 0;

vCAST_long_double vCAST_VALUE = VCAST_FLOAT_ZERO;
int vCAST_FILE;

int vCAST_PARAM_LENGTH;
int vCAST_INDEX = 0;
int vCAST_DATA_FIELD = 4;

enum vCAST_COMMAND_TYPE vCAST_COMMAND;

vCAST_boolean vCAST_VALUE_NUL = vCAST_false;
vCAST_boolean vCAST_SIZE = vCAST_false;
vCAST_boolean vCAST_can_print_constructor = vCAST_true;

int  *VCAST_index_size = VCAST_NULL;

static unsigned int *vcast_global_size = VCAST_NULL;

vCAST_boolean vcast_proc_handles_command(int vc_m)
{
   int vc_i;
   int num_fields = 1;
 
   for (vc_i=0; vc_i<vCAST_PARAM_LENGTH; vc_i++) {
      if (vCAST_PARAMETER[vc_i] == '.')
         num_fields++;
      else if (vCAST_PARAMETER[vc_i] == '%')
         break;
   }
 
   if ((num_fields - vCAST_DATA_FIELD + 1) > vc_m)
      return 0;
   else
      return 1;
}

void VCAST_SET_GLOBAL_SIZE(unsigned int *size)
{
   vcast_global_size = size;
}

unsigned int *VCAST_GET_GLOBAL_SIZE(void)
{
   return vcast_global_size;
}

/*------------------------------------------------------------------*/
int VCAST_FIND_INDEX (void)
{

  vCAST_boolean LAST_WAS_DOT  = vCAST_false;
  vCAST_boolean IS_DOT_DOT    = vCAST_false;
  int          VC_COUNTER       = 0;
  int          VC_I;

  for (VC_I=0;VC_I<vCAST_PARAM_LENGTH;VC_I++)
    {
    switch (vCAST_PARAMETER[VC_I]) {
      case '.':
        if (LAST_WAS_DOT)
          IS_DOT_DOT = vCAST_true;
        else
          LAST_WAS_DOT = vCAST_true; 
        break;
      case '%':
        return (-1);
      default:
        if ( (!IS_DOT_DOT) && (LAST_WAS_DOT) )
          {
          VC_COUNTER++;
          if (VC_COUNTER == vCAST_DATA_FIELD)
            return (VC_I);
          }
        IS_DOT_DOT = vCAST_false;
        LAST_WAS_DOT = vCAST_false; 
      }
    }
  return (-1);
} /* end VCAST_FIND_INDEX */

/*------------------------------------------------------------------*
 *
 * function:     vcast_get_param_value_with_index
 *
 * description:
 *
 * This function will get the param from VCAST_PARAMETER global based
 * on the current value of the index returned from VCAST_FIND_INDEX().
 *
 * Use this function when you want to get the current param, but DO NOT
 * want to increment vCAST_DATA_FIELD. For example, this is used when
 * trying to get range values (e.g.  "123..456").
 *
 *  VC_VALUE  is returned, which is the integer value of the param
 *            substring indexed by the current value of
 *            VCAST_DATA_FIELD global 
 *
 *  index     this points to an integer provided by the caller, which 
 *            will get updated with the a value that points to an 
 *            element in the vCAST_PARAMETER[] array that is either 
 *            the '.' or '%' or -1 if VCAST_FIND_INDEX() returned -1 
 *            (i.e. index wasn't found )
 *
 *------------------------------------------------------------------*/
int vcast_get_param_value_with_index (int *index)
{

  int vc_gp_INDEX = VCAST_FIND_INDEX();
  int VC_VALUE = 0;

  if ( vc_gp_INDEX != -1 )
  {
     while ( (vCAST_PARAMETER[vc_gp_INDEX] != '.') &&
           (vCAST_PARAMETER[vc_gp_INDEX] != '%') )
     {
        VC_VALUE = VC_VALUE * 10 + vCAST_PARAMETER[vc_gp_INDEX] - '0';
        vc_gp_INDEX = vc_gp_INDEX + 1;
     }
  }
  *index = vc_gp_INDEX;

  return (VC_VALUE);
} /* end vcast_get_param_value_with_index */

/*------------------------------------------------------------------*
 *
 * function:     vcast_get_param
 *
 * description:
 *
 * This function will return the value of the param from VCAST_PARAMETER 
 * global based on the current value of the index returned from 
 * VCAST_FIND_INDEX().
 *
 * This function will increment vCAST_DATA_FIELD to point to the next
 * param element in the VCAST_PARAMETER string
 *
 *  VC_VALUE  is returned, which is the integer value of the param
 *            substring indexed by the current value of
 *            VCAST_DATA_FIELD global 
 *
 *------------------------------------------------------------------*/
 int vcast_get_param (void)
{

  int index;
  int VC_VALUE = 0;

  VC_VALUE = vcast_get_param_value_with_index(&index);
  vCAST_DATA_FIELD++;
  return VC_VALUE;

} /* end vcast_get_param */



/*------------------------------------------------------------------*
 *
 * THIS FUNCTION IS ONLY USED BY <<MIN>> <<MID>> <<MAX>> functions
 *
 * function:      vcast_get_range_value
 *
 * description:
 *
 * This function will return the value of the first and last elements
 * of a range specification within the VCAST_PARAMETER 
 * global based on the current value of the index returned from 
 * VCAST_FIND_INDEX().
 *
 * This function will increment vCAST_DATA_FIELD to point to the next
 * param element in the VCAST_PARAMETER string
 *
 *  vCAST_FIRST_VAL this points to an integer provided by the caller, 
 *                  which will get updated with first value of the 
 *                  range 
 *
 *  vCAST_LAST_VAL  this points to an integer provided by the caller, 
 *                  which will get updated with last value of the 
 *                  range 
 *
 *  vCAST_MORE_DATA this points to an integer provided by the caller, 
 *                  which will get updated to indicate whether there
 *                  was any valid data field located in the vCAST_PARAMETER 
 *                  string based on the current value of VCAST_DATA_FIELD
 *
 *------------------------------------------------------------------*/

void vcast_get_range_value ( int *vCAST_FIRST_VAL,
                             int *vCAST_LAST_VAL,
                             int *vCAST_MORE_DATA)
{
  int vc_grv_INDEX = VCAST_FIND_INDEX();

  if (vc_grv_INDEX == -1)
     *vCAST_MORE_DATA = 0;
  else
     *vCAST_MORE_DATA = 1;

  if ((vc_grv_INDEX == -1) || ( vCAST_SIZE ) || (vCAST_PARAMETER[vc_grv_INDEX] == '*'))
  {
     *vCAST_FIRST_VAL = 0;
     *vCAST_LAST_VAL  = 0;
  }
  else
  {
     /* get the first value of the range and the updated vc_grv_INDEX will now
      * point to either '.' or '%'. Next, check for the consecutive '..' sequence, 
      * which will require getting the next param value to set the last_value of 
      * the range, otherwise last_value = first_value
      */
                            
     *vCAST_FIRST_VAL = vcast_get_param_value_with_index(&vc_grv_INDEX); 
     if ( (vCAST_PARAMETER[vc_grv_INDEX] == '.') &&
          (vCAST_PARAMETER[vc_grv_INDEX+1] == '.') )
     {
        /* update the index to point after the '..' sequence */
        vc_grv_INDEX = vc_grv_INDEX + 2;
        /* init last value before atoi conversion */
        *vCAST_LAST_VAL  = 0;   

        while ( (vCAST_PARAMETER[vc_grv_INDEX] != '.') &&
                (vCAST_PARAMETER[vc_grv_INDEX] != '%') )
        {
           *vCAST_LAST_VAL = (*vCAST_LAST_VAL * 10) + (vCAST_PARAMETER[vc_grv_INDEX] - '0');
           vc_grv_INDEX = vc_grv_INDEX + 1;
        }
     }
     else
     {
        /* if there is no range '..' specified, then first = last */
        *vCAST_LAST_VAL = *vCAST_FIRST_VAL;
     }
   }
   vCAST_DATA_FIELD++;

} /* end vcast_get_range_value */



/*------------------------------------------------------------------*/
void vcast_not_supported (void)
{
  if (vCAST_COMMAND == vCAST_PRINT)
    vectorcast_fprint_string(vCAST_OUTPUT_FILE,"*TNS*\n");
}

#ifndef VCAST_NO_SIGNAL
/*------------------------------------------------------------------*/
void vCAST_signal(int sig)
{
  int eventCode = 0;
  if ( vcast_is_in_union == vCAST_true )
    {
#ifdef SIGBUS
      signal(SIGBUS,  vCAST_signal);
#endif
#ifdef SIGSEGV
      signal(SIGSEGV, vCAST_signal);
#endif
#ifdef SIGILL
      signal(SIGILL, vCAST_signal);
#endif
#ifdef SIGFPE
      signal (SIGFPE, vCAST_signal);
#endif

#ifndef VCAST_VXWORKS
#ifndef VCAST_GH_INT_178B
#ifndef VCAST_NO_SETJMP
      longjmp(VCAST_env, sig);
#endif
#endif
#endif
      ;
    }

  else
    {
      switch ( sig )
      {
#ifdef SIGSEGV
         case SIGSEGV : 
         eventCode = 1005; 
         break;
#endif
#ifdef SIGBUS
         case SIGBUS  : 
         eventCode = 1006; 
         break;
#endif
#ifdef SIGILL
         case SIGILL: 
         eventCode = 1007; 
         break;
#endif
#ifdef SIGFPE
         case SIGFPE: 
         eventCode = 1010; 
         break;
#endif
         default      : 
         eventCode = 1007; 
         break;
      }

      vcast_is_in_driver = vCAST_false;
      VCAST_driver_termination( 1, eventCode );
    }
}
#endif

/*******************************************************************************
 * Parameters  
 *    vc_VAL 
 *       pointer to bit field value
 *    Bits 
 *       number of bits in the bit field
 *    is_signed 
 *       whether the bit field is signed
 *
 * Description 
 *    This function is used to print or set the value of a bit field member.
 ******************************************************************************/
#ifdef VCAST_DISABLE_TI_BITFIELD
void VCAST_TI_BITFIELD ( VCAST_LONGEST_INT *vc_VAL, int Bits,  vCAST_boolean is_signed ) { }
#else
void VCAST_TI_BITFIELD ( VCAST_LONGEST_INT *vc_VAL, int Bits,  vCAST_boolean is_signed )
{

   switch (vCAST_COMMAND){
   case vCAST_PRINT :
      if ( is_signed == vCAST_true )
         vectorcast_fprint_long_long    (vCAST_OUTPUT_FILE, *vc_VAL);
      else
         vectorcast_fprint_unsigned_long_long    (vCAST_OUTPUT_FILE, *vc_VAL);
      vectorcast_fprint_string  (vCAST_OUTPUT_FILE, "\n");
      break;

   case vCAST_SET_VAL :
      if ( is_signed == vCAST_true )
         *vc_VAL = (VCAST_LONGEST_INT) vCAST_VALUE;
      else
         *vc_VAL = (VCAST_LONGEST_UNSIGNED) vCAST_VALUE_UNSIGNED;
      break;

   case vCAST_FIRST_VAL :
     if (is_signed == vCAST_true){
       if (Bits == sizeof(VCAST_LONGEST_INT) * 8){
         *vc_VAL = VCAST_MIN_LONGEST_INT;
       } else {
        *vc_VAL = 0 - (VCAST_LONGEST_INT)(vCAST_power(Bits-1));
       }
     } else
        *vc_VAL = 0;
      break;

   case vCAST_MID_VAL :
      if (is_signed == vCAST_true)
      {
        VCAST_LONGEST_INT Min, Max;
        Min = 0 - (VCAST_LONGEST_INT) (vCAST_power(Bits-1));
        if (Bits == sizeof(VCAST_LONGEST_INT) * 8){
          Max = VCAST_MAX_LONGEST_INT;
        } else {
          Max = (VCAST_LONGEST_INT) (vCAST_power(Bits-1) - VCAST_FLOAT_ONE);
        }
        *vc_VAL = (Min / 2) + (Max / 2);
      }
      else
      {
        VCAST_LONGEST_UNSIGNED Max;
        if (Bits == sizeof(VCAST_LONGEST_INT) * 8){
          Max = VCAST_MAX_UNSIGNED_LONGEST_INT;
        } else {
          short vcast_i;
          Max = 1;
          for (vcast_i=0;vcast_i<Bits;vcast_i++)
            Max = 2 * Max;
          Max = Max - 1;
        }
          *vc_VAL = Max / 2;
      }

      break;

   case vCAST_LAST_VAL :
      if (is_signed == vCAST_true){
        if (Bits == sizeof(VCAST_LONGEST_INT) * 8){
          *vc_VAL = VCAST_MAX_LONGEST_INT;
        } else {
          *vc_VAL = (VCAST_LONGEST_INT) (vCAST_power(Bits-1) - VCAST_FLOAT_ONE);
        }
      } else {
        if (Bits == sizeof(VCAST_LONGEST_INT) * 8){
          *vc_VAL = VCAST_MAX_UNSIGNED_LONGEST_INT;
        } else {
          VCAST_LONGEST_UNSIGNED Max = 1;
          short vcast_i;
          for (vcast_i=0;vcast_i<Bits;vcast_i++)
            Max = 2 * Max;
          *vc_VAL = (VCAST_LONGEST_UNSIGNED) (Max - 1);
        }
      }
      break;

   default:
      break;
   }
} /* VCAST_TI_BITFIELD */
#endif /* VCAST_DISABLE_TI_BITFIELD */


/* The following five functions are all related to setting and printing char** types
   They use a lot of CODE and RAM space, so we make them optionally excludable with the
   variable VCAST_DISABLE_TI_STRING
*/

#ifdef VCAST_DISABLE_TI_STRING
void VCAST_TI_STRING (char **vcast_param, int vCAST_Size, int from_bounded_array,int size_of_bounded_array ) {
   vcast_not_supported ();
}
#else


static char vcastCommonOutputBuffer [VCAST_MAX_STRING_LENGTH];

/**************************************************************************
Function: vcast_contains_unprintable
Parameters: input - string to check
Description: This function returns true if the string it is given contains
a nongraphical character. 
 *************************************************************************/
vCAST_boolean vcast_contains_unprintable(char * input){
  int vc_i = 0;
 
  while (input[vc_i] != '\0'){
    if (isUnprintable(input[vc_i]))
      return vCAST_true;
    vc_i++;
  }

  return vCAST_false;
}


/**************************************************************************
Function: vcast_contains_unprintable_with_length
Parameters: input - string to check, length - length of string to check
Description: This function returns true if the string it is given contains
a nongraphical character. 
 *************************************************************************/
vCAST_boolean vcast_contains_unprintable_with_length(char * input, int length){
  int vc_i = 0;
 
  for (vc_i = 0; vc_i < length; vc_i++){
    if (isUnprintable(input[vc_i]))
      return vCAST_true;
  }

  return vCAST_false;
}


/**************************************************************************
Function: vcast_unconvert_char_oct
Parameters: input - character to translate into octal
Description: This function takes a character and returns the string used
to represent it in octal.  For example, an input of '_' should generate
a return of "\137".  This translation is used to
prevent nongraphical characters from creating results that span more than
one line.  This also allows nongraphical characters to be compared to
expected results since some characters such as tabs and spaces would
otherwise be indistinguishable.
 *************************************************************************/
char * vcast_unconvert_char_oct(char input){
  static char output[5];
  vcast_char_to_based_string ( output, input, vCAST_false );
  return  output;
}


/**************************************************************************
Function: vcast_unconvert_char_hex
Parameters: input - character to translate into hexadecimal
Description: This function takes a character and returns the string used
to represent it in hexadecimal.  For example, an input of '_' should generate
a return of "\x5f".  This translation is used to
prevent nongraphical characters from creating results that span more than
one line.  This also allows nongraphical characters to be compared to
expected results since some characters such as tabs and spaces would
otherwise be indistinguishable.                        
 *************************************************************************/
char * vcast_unconvert_char_hex(char input){
  static char output[5];
  vcast_char_to_based_string ( output, input, vCAST_true );
  return output;
}




/**************************************************************************
Function: vcast_unconvert_string_hex
Parameters: input - string to translate into hexadecimal
Description:  This function takes a string and returns it with all the 
characters translated into hexadecimal numbers.  This translation is used to
prevent nongraphical characters from creating results that span more than
one line.  This also allows nongraphical characters to be compared to
expected results since some characters such as tabs and spaces would
otherwise be indistinguishable.
 *************************************************************************/
char * vcast_unconvert_string_hex(char * input){
  int inputIndex;
  int inputLength;

  VCAST_memset(vcastCommonOutputBuffer, '\0', VCAST_MAX_STRING_LENGTH);
  inputLength = VCAST_strlen(input);
  for (inputIndex = 0; inputIndex < inputLength; inputIndex++)
    VCAST_strcat(vcastCommonOutputBuffer, vcast_unconvert_char_hex(input[inputIndex]));
  
  return vcastCommonOutputBuffer;
}

/**************************************************************************
Function: vcast_unconvert_string_hex_with_length
Parameters: input - string to translate into hexadecimal, 
            length - length of string to convert (may not be strlen)
Description:  This function takes a string and returns it with all the 
characters translated into hexadecimal numbers.  This translation is used to
prevent nongraphical characters from creating results that span more than
one line.  This also allows nongraphical characters to be compared to
expected results since some characters such as tabs and spaces would
otherwise be indistinguishable.
 *************************************************************************/
char * vcast_unconvert_string_hex_with_length(char * input, int length){
  int inputIndex;
 
  VCAST_memset(vcastCommonOutputBuffer, '\0', VCAST_MAX_STRING_LENGTH);
  
  for (inputIndex = 0; inputIndex < length; inputIndex++)
    VCAST_strcat(vcastCommonOutputBuffer, vcast_unconvert_char_hex(input[inputIndex]));
  
  return vcastCommonOutputBuffer;
}

/**************************************************************************
Function: vcast_unconvert_string_oct
Parameters: input - string to translate into octal
Description: This function takes a string and returns it with all the 
characters translated into octal numbers.  This translation is used to
prevent nongraphical characters from creating results that span more than
one line.  This also allows nongraphical characters to be compared to
expected results since some characters such as tabs and spaces would
otherwise be indistinguishable.
*************************************************************************/
char * vcast_unconvert_string_oct(char * input){
  int inputIndex;
  int inputLength;

  VCAST_memset(vcastCommonOutputBuffer, '\0', VCAST_MAX_STRING_LENGTH);
  inputLength = VCAST_strlen(input);
  for (inputIndex = 0; inputIndex < inputLength; inputIndex++)
    VCAST_strcat(vcastCommonOutputBuffer, vcast_unconvert_char_oct(input[inputIndex]));
  
  return vcastCommonOutputBuffer;
}

/**************************************************************************
Function: vcast_unconvert_string_oct_with_length
Parameters: input - string to translate into octal
            length - length of string to convert (may not be strlen)
Description: This function takes a string and returns it with all the 
characters translated into octal numbers.  This translation is used to
prevent nongraphical characters from creating results that span more than
one line.  This also allows nongraphical characters to be compared to
expected results since some characters such as tabs and spaces would
otherwise be indistinguishable.
*************************************************************************/
char * vcast_unconvert_string_oct_with_length(char * input, int length){
  int inputIndex;
  
  VCAST_memset(vcastCommonOutputBuffer, '\0', VCAST_MAX_STRING_LENGTH);
  
  for (inputIndex = 0; inputIndex < length; inputIndex++)
    VCAST_strcat(vcastCommonOutputBuffer, vcast_unconvert_char_oct(input[inputIndex]));
  
  return vcastCommonOutputBuffer;
}


/*******************************************************************************
 * Parameters  
 *    vcast_param 
 *       The string to operate on
 *    vCAST_Size
 *       The sizeof of the data passed in
 *    from_bounded_array
 *       1 if from an array TI, 0 otherwise
 *    size_of_bounded_array
 *       size of bounded array, -1 otherwise
 *
 * Description 
 *    This function is used to print/set/operate on a string
 ******************************************************************************/

void VCAST_TI_STRING ( 
      char **vcast_param, 
      int vCAST_Size,
      int from_bounded_array,
      int size_of_bounded_array ) {

   int szIndex = VCAST_FIND_INDEX();


   if ((vCAST_SIZE) && (szIndex == -1)){
      vectorcast_fprint_integer(vCAST_OUTPUT_FILE,vCAST_Size);
      vectorcast_fprint_string(vCAST_OUTPUT_FILE,"\n");
      return;
   } 

   switch (vCAST_COMMAND) {
      case vCAST_PRINT:
         if (vCAST_COMMAND_IS_MIN_MAX == vCAST_false){ 
            if ((*vcast_param) == 0) 
               vectorcast_fprint_string(vCAST_OUTPUT_FILE,"null\n");
            else{
               if (vCAST_FULL_STRINGS && from_bounded_array) {
                  if (vcast_contains_unprintable_with_length(*vcast_param, size_of_bounded_array)){
                     if ( vCAST_HEX_NOTATION)
                        vectorcast_fprint_string(vCAST_OUTPUT_FILE, vcast_unconvert_string_hex_with_length(*vcast_param, size_of_bounded_array));
                     else
                        vectorcast_fprint_string(vCAST_OUTPUT_FILE, vcast_unconvert_string_oct_with_length(*vcast_param, size_of_bounded_array));
           
                     vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
                  } else {
                     vectorcast_fprint_string_with_length(vCAST_OUTPUT_FILE, *vcast_param, size_of_bounded_array);
                     vectorcast_fprint_string_with_cr(vCAST_OUTPUT_FILE, "");
                  } /* end else */
               } else {
                  if (vcast_contains_unprintable(*vcast_param)){
                     if ( vCAST_HEX_NOTATION)
                        vectorcast_fprint_string(vCAST_OUTPUT_FILE, vcast_unconvert_string_hex(*vcast_param));
                     else
                        vectorcast_fprint_string(vCAST_OUTPUT_FILE, vcast_unconvert_string_oct(*vcast_param));
             
                     vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
                  } else
                     vectorcast_fprint_string_with_cr(vCAST_OUTPUT_FILE, *vcast_param);
               } /* end else */
            } /* end else */
         } /* end if */

         break;

      case vCAST_SET_VAL: {
         int size_to_copy = vcast_convert_size ( vCAST_PARAMETER ) - vCAST_INDEX;
         if ( from_bounded_array ) {
            if ( size_to_copy > size_of_bounded_array )
               size_to_copy = size_of_bounded_array;
         }

         VCAST_memcpy ( 
               (void*)*vcast_param, 
               VCAST_convert ( &vCAST_PARAMETER[vCAST_INDEX+1] ), 
               size_to_copy);

         /* If vCAST_FULL_STRINGS is set, VectorCAST prints the entire
          * bounded array. If vCAST_FULL_STRINGS is not set, VectorCAST
          * needs the null terminating string. */
         if ( from_bounded_array && !vCAST_FULL_STRINGS )
            (*vcast_param)[size_of_bounded_array-1] = '\0';
         break; 
         }

      case vCAST_FIRST_VAL:
         if ( vCAST_COMMAND_IS_MIN_MAX ) {
            vectorcast_fprint_integer(vCAST_OUTPUT_FILE,0);
            vectorcast_fprint_string_with_cr(vCAST_OUTPUT_FILE,"");
         }
         break;
 
      case vCAST_ALLOCATE:
         /* This should never be called, but we still call it ...
          * vCAST_TOOL_ERROR = vCAST_true;
          */
         break;
   
      case vCAST_LAST_VAL :
         
         if ( vCAST_COMMAND_IS_MIN_MAX ) {
            vectorcast_fprint_integer(vCAST_OUTPUT_FILE,vCAST_Size-1);
            vectorcast_fprint_string_with_cr(vCAST_OUTPUT_FILE,"");
         }
         break;
 
      default :
         break;
   } /* switch */
} /* END VCAST_TI_STRING */
#endif /* VCAST_DISABLE_TI_STRING */

#ifdef  VCAST_SBF_UNITS_AVAILABLE
void VCAST_TI_SBF_OBJECT(vcast_sbf_object_type* vcast_param)
{
  switch (vCAST_COMMAND) {
    case vCAST_PRINT :
      if ( vcast_param == 0 )
        vectorcast_fprint_string (vCAST_OUTPUT_FILE,"null\n");
      else {
        vectorcast_fprint_integer(vCAST_OUTPUT_FILE, *vcast_param);
        vectorcast_fprint_string(vCAST_OUTPUT_FILE, "\n");
      }
      break;
    case vCAST_STUB_FUNCTION :
      *vcast_param = 1;
      break;
    default:
      break;
  } /* end switch */
} /* VCAST_TI_SBF_OBJECT */
#endif /* VCAST_SBF_UNITS_AVAILABLE */

/**************************************************************************
Function: vcast_add_to_hex
Parameters: previousNumber - number so far
      latestDigit - digit to add to number
Description: This function is used in reading a hexadecimal number digit by
digit.                        
 *************************************************************************/
int vcast_add_to_hex(int previousNumber, char latestDigit){
  int newNumber = previousNumber * 16;
  int latestDigitValue = 0;
  if ((latestDigit >= '0')&&(latestDigit <= '9'))
    latestDigitValue = latestDigit - '0';
  else
    if ((latestDigit >= 'a')&&(latestDigit <= 'f'))
      latestDigitValue = latestDigit - 'a' + 10;
    else
      if ((latestDigit >= 'A')&&(latestDigit <= 'F'))
  latestDigitValue = latestDigit - 'A' + 10;

  newNumber += latestDigitValue;
  return newNumber;
}

#define add_to_octal(previousNumber, latestDigit) \
  (previousNumber * 8) + (latestDigit - '0')

/**************************************************************************
Function: is_hex
Parameters: character - char to compare
Description: This function returns true if the character is a valid 
hexadecimal digit (0-9, a-f, or A-F).
 *************************************************************************/
#define is_hex(character) \
  (((character >= '0')&&(character <= '9'))||   \
  ((character >= 'a')&&(character <= 'f'))||    \
   ((character >= 'A')&&(character <= 'F')))

/**************************************************************************
Function: is_octal
Parameters: character - char to compare
Description: This function returns true if the character is a valid octal
digit (0-7).                        
 *************************************************************************/
#define is_octal(character) \
  ((character >= '0')&&(character <= '7'))

/**************************************************************************
Function: vcast_get_non_numerical_escape
Parameters: character - character following the \
Description: This function is used in reading an escape sequence that is
not an octal or hexadecimal number.
 *************************************************************************/
 #ifndef VCAST_DISABLE_TI_STRING
char vcast_get_non_numerical_escape(char character){
  if (character == 'n')
    return '\n';
  if (character == 't')
    return '\t';
  if (character == 'v')
    return '\v';
  if (character == 'b')
    return '\b';
  if (character == 'r')
    return '\r';
  if (character == 'f')
    return '\f';
  if (character == 'a')
    return '\a';
  if (character == '\\')
    return '\\';
  if (character == '\?')
    return '\?';
  if (character == '\'')
    return '\'';
  if (character == '\"')
    return '\"';

  return '\0';
}
#endif /*VCAST_DISABLE_TI_STRING*/

#ifndef VCAST_DISABLE_TI_STRING
int vcast_convert_size(char * input){
  int inputIndex = 0;
  int outputIndex = 0;
  int length = VCAST_strlen(input);
  char currentChar;

  while (inputIndex < length){
    currentChar = input[inputIndex];
    if (currentChar == '\\'){
      inputIndex++;
      currentChar = input[inputIndex];
      if (currentChar == 'x'){
  char nextCharacter = input[inputIndex + 1];
    while(is_hex(nextCharacter)){
      inputIndex++;
      nextCharacter = input[inputIndex + 1];
    }
      }
      else
  if (is_octal(currentChar)){
    char nextCharacter = input[inputIndex + 1];
    while(is_octal(nextCharacter)){
      inputIndex++;
      nextCharacter = input[inputIndex + 1];
    }
  }
    }
 
    outputIndex++;
    inputIndex++;
  } /* while */

  return outputIndex;
}
#endif /* VCAST_DISABLE_TI_STRING */

/**************************************************************************
Function: VCAST_convert
Parameters: input - string to read
Description: This function takes a string and returns it with the escape
sequences converted into their characters.  For example, a string 
containing "\137" gets converted to "_".
*************************************************************************/
#ifndef VCAST_DISABLE_TI_STRING
char * VCAST_convert(char * input){
  static char output[VCAST_MAX_STRING_LENGTH];
  int inputIndex = 0;
  int outputIndex = 0;
  int length = VCAST_strlen(input);
  char currentChar;

  VCAST_memset(output, '\0', VCAST_MAX_STRING_LENGTH);
  while (inputIndex < length){
    currentChar = input[inputIndex];
    if (currentChar == '\\'){
      inputIndex++;
      currentChar = input[inputIndex];
      if (currentChar == 'x'){
  int hexNumber = 0;
  char nextCharacter = input[inputIndex + 1];
    while(is_hex(nextCharacter)){
      inputIndex++;
      hexNumber = vcast_add_to_hex(hexNumber, nextCharacter);
      nextCharacter = input[inputIndex + 1];
    }
    output[outputIndex] = hexNumber;
      }
      else
  if (is_octal(currentChar)){
    int octalNumber = add_to_octal(0, currentChar);
    char nextCharacter = input[inputIndex + 1];
    while(is_octal(nextCharacter)){
      inputIndex++;
      octalNumber = add_to_octal(octalNumber, nextCharacter);
      nextCharacter = input[inputIndex + 1];
    }

    output[outputIndex] = octalNumber;
  }
  else{
    output[outputIndex] = vcast_get_non_numerical_escape(currentChar);
      }
    }
    else{
      output[outputIndex] = input[inputIndex];
     
    }
    outputIndex++;
    inputIndex++;
  } /* while */

  return output;
}
#endif /*VCAST_DISABLE_TI_STRING*/




void vCAST_RUN_DATA_IF (char VCAST_PARAM[], vCAST_boolean POST_CONSTRUCTOR_USER_CODE)
{
  int  UNIT_INDEX  = 0;
  int  SUB_INDEX   = 0;
  int  PARAM_INDEX = 0;
  int VC_I;
   
  char work[VCAST_MAX_STRING_LENGTH];
  vCAST_long_double tmp;

  vCAST_DATA_FIELD  = 4;
  vCAST_VALUE       = VCAST_FLOAT_ZERO;
  vCAST_VALUE_INT   = 0;
  vCAST_TOOL_ERROR  = vCAST_false;
  vCAST_SIZE        = vCAST_false;
  vCAST_VALUE_NUL   = vCAST_false;
  vCAST_INDEX       = 0;
  VCAST_memset(vCAST_PARAMETER, '\0', VCAST_MAX_STRING_LENGTH);

  VCAST_strcpy ( vCAST_PARAMETER, VCAST_PARAM );
  vCAST_PARAM_LENGTH = VCAST_strlen(vCAST_PARAMETER);

  UNIT_INDEX = vcast_get_unit_id (vCAST_PARAMETER);
  SUB_INDEX   = vcast_get_subprogram_id (vCAST_PARAMETER);
  PARAM_INDEX = vcast_get_parameter_id ( vCAST_PARAMETER );

  {
  int tempmax = VCAST_strlen(VCAST_PARAM);
  for (VC_I=7;VC_I<=tempmax;VC_I++)
    if ( vCAST_PARAMETER[VC_I] == '%' )
      {
      vCAST_INDEX = VC_I;
      break;
      }
  }
  VCAST_memset(work, '\0', VCAST_MAX_STRING_LENGTH);
  vCAST_slice( work, vCAST_PARAMETER, (vCAST_INDEX+1), vCAST_PARAM_LENGTH);
  if (VCAST_strcmp( work, "<<null>>" ) == 0)
     vCAST_VALUE_NUL = vCAST_true;
  VCAST_memset(vCAST_PARAMETER_KEY, '\0', VCAST_MAX_STRING_LENGTH);
  vCAST_slice (vCAST_PARAMETER_KEY, vCAST_PARAMETER, 0, vCAST_INDEX);
#if defined(__cplusplus)
  if ((POST_CONSTRUCTOR_USER_CODE && 
       ((vCAST_PARAMETER[0] == '3') || (vCAST_PARAMETER[0] == '4'))) || 
      (!POST_CONSTRUCTOR_USER_CODE && (vCAST_PARAMETER[0] != '3') && 
       (vCAST_PARAMETER[0] != '4'))) {
#endif
  if (vCAST_PARAMETER[0] == '1')
    vCAST_COMMAND = vCAST_PRINT;
  else if (vCAST_PARAMETER[0] == '2')
    {
    vCAST_COMMAND = vCAST_PRINT;
    vCAST_SIZE = vCAST_true;
    }
  else if (VCAST_strncmp(work, "<<MIN>>", 7) == 0)
    vCAST_COMMAND = vCAST_FIRST_VAL;
  else if (VCAST_strncmp(work, "<<MID>>", 7) == 0)
    vCAST_COMMAND = vCAST_MID_VAL;
  else if (VCAST_strncmp(work, "<<MAX>>", 7) == 0)
    vCAST_COMMAND = vCAST_LAST_VAL;
  else if (VCAST_strncmp(work, "<<POS_INF>>", 11) == 0)
    vCAST_COMMAND = vCAST_POS_INF_VAL;
  else if (VCAST_strncmp(work, "<<NEG_INF>>", 11) == 0)
    vCAST_COMMAND = vCAST_NEG_INF_VAL;
  else if (VCAST_strncmp(work, "<<NAN>>", 7) == 0)
    vCAST_COMMAND = vCAST_NAN_VAL;
  else if (VCAST_strncmp(work, "<<MIN-1>>", 9) == 0)
    vCAST_COMMAND = vCAST_MIN_MINUS_1_VAL;
  else if (VCAST_strncmp(work, "<<MAX+1>>", 9) == 0)
    vCAST_COMMAND = vCAST_MAX_PLUS_1_VAL;
  else if (VCAST_strncmp(work, "<<ZERO>>", 8) == 0)
    vCAST_COMMAND = vCAST_ZERO_VAL;
  else if (VCAST_strncmp(work, "<<KEEP>>", 8) == 0)
    vCAST_COMMAND = vCAST_KEEP_VAL;
  else if (VCAST_strcmp(work, "<<STUB>>") == 0)
    vCAST_COMMAND = vCAST_STUB_FUNCTION;
  else if ((vCAST_PARAMETER[0] == '0')|| (vCAST_PARAMETER[0] == '4'))
    vCAST_COMMAND = vCAST_SET_VAL;
  else if ((vCAST_PARAMETER[0] == '-')|| (vCAST_PARAMETER[0] == '3'))
    vCAST_COMMAND = vCAST_ALLOCATE;
  else if (vCAST_PARAMETER[0] == '+')
    vCAST_COMMAND = vCAST_ALLOCATE;
  else if (vCAST_PARAMETER[0] == 'F')
    vCAST_COMMAND = vCAST_FUNCTION;
#if defined(__cplusplus)
      } else {
      VCAST_memset(vCAST_PARAMETER_KEY, '\0', VCAST_MAX_STRING_LENGTH);
      VCAST_memset(vCAST_PARAMETER, '\0', VCAST_MAX_STRING_LENGTH);
      return;
      }
#endif
  vCAST_slice(work, vCAST_PARAMETER, vCAST_INDEX+1, vCAST_PARAM_LENGTH);

  vCAST_STR_TO_LONG_DOUBLE(work, &tmp);
  vCAST_VALUE = tmp;
  vCAST_VALUE_INT = VCAST_atoi ( work );
  if (work && work[0] == '-') {
     vCAST_VALUE_UNSIGNED = vCAST_VALUE_INT;
  } else {
     vCAST_VALUE_UNSIGNED = VCAST_strtoul ( work, (char **)VCAST_NULL, 10 );
  }
  vcast_B1_switch( UNIT_INDEX, SUB_INDEX, PARAM_INDEX, work );
  VCAST_memset(vCAST_PARAMETER_KEY, '\0', VCAST_MAX_STRING_LENGTH);
  VCAST_memset(vCAST_PARAMETER, '\0', VCAST_MAX_STRING_LENGTH);

  if ( vCAST_TOOL_ERROR )
    {
    if ( (vCAST_COMMAND != vCAST_PRINT) && (vCAST_COMMAND != vCAST_ALLOCATE) && (!vCAST_SIZE) )
      {
      /* open TEMP_DIF.DAT, erasing everything already in it */
      vCAST_FILE = vectorcast_fopen (
         vcast_get_filename(VCAST_TEMP_DIF_DAT), "w" );
      /* write error message */
      vectorcast_fprint_string( vCAST_FILE, "##ERROR##\n");
      /* close TEMP_DIF.DAT */
      vectorcast_fclose ( vCAST_FILE );
      }
    else
      vectorcast_fprint_string(vCAST_OUTPUT_FILE,"##ERROR##\n");
    }

} /* end RUN_DATA_IF */


#ifdef VCAST_CPP_ENVIRONMENT

struct VCAST_CSU_Data
{
  struct VCAST_CSU_Data_Item *vcast_root;
  struct VCAST_CSU_Data_Item *vcast_last;
};

struct VCAST_CSU_Data *VCAST_Create_CSU_Data (void)
{
  struct VCAST_CSU_Data *vcast_data;
  vcast_data = (struct VCAST_CSU_Data*)VCAST_malloc (sizeof (struct VCAST_CSU_Data));
  vcast_data->vcast_root = 0;
  vcast_data->vcast_last = 0;
  return vcast_data;
}

void VCAST_Add_CSU_Data (struct VCAST_CSU_Data **vcast_data, 
                         struct VCAST_CSU_Data_Item *vcast_data_item)
{
  int vcast_i, vcast_counter = 0;
  int vc_length = VCAST_strlen(vcast_data_item->vcast_command);

  if (vcast_data && (*vcast_data == 0))
    *vcast_data = VCAST_Create_CSU_Data ();

  for (vcast_i = 0; vcast_i < vc_length; ++vcast_i)
  {
    if ((vcast_data_item->vcast_command)[vcast_i] == '.')
      vcast_counter++;

    if (vcast_counter == vCAST_DATA_FIELD)
    {
      (vcast_data_item->vcast_command)[vcast_i] = 0;
      break;
    }
  }

  /* The last char shouldn't be a %, that's because the strcmp
   * would only work sometimes if the % was on the end.
   * Removing the % in this case, ensures all cases don't have a
   * % on the end.
   */
  if (vcast_i == vc_length) {
     if (vcast_data_item->vcast_command[vcast_i-1] == '%') {
        vcast_data_item->vcast_command[vcast_i-1] = 0;
     }
  }

  /* Insert into the linked list */
  if ((*vcast_data)->vcast_root == 0)
  {
    (*vcast_data)->vcast_root = vcast_data_item;
    (*vcast_data)->vcast_last = vcast_data_item;
  }
  else
  {
    (*vcast_data)->vcast_last->vcast_next = vcast_data_item;
    (*vcast_data)->vcast_last = (*vcast_data)->vcast_last->vcast_next;
  }
}

struct VCAST_CSU_Data_Item *
VCAST_Get_CSU_Data (struct VCAST_CSU_Data **vcast_data, char *vcast_command)
{
  int vcast_i, vcast_counter = 0;
  struct VCAST_CSU_Data_Item *vcast_cur = 0;
  char vcast_tmp;
  int vc_length = VCAST_strlen(vcast_command);

  if (vcast_data && (*vcast_data == 0))
    *vcast_data = VCAST_Create_CSU_Data ();

  for (vcast_i = 0; vcast_i < vc_length; ++vcast_i)
  {
    if (vcast_command[vcast_i] == '.')
      vcast_counter++;

    if (vcast_counter == vCAST_DATA_FIELD ||
        vcast_i == vc_length-1)
    {
      vcast_tmp = vcast_command[vcast_i];
      vcast_command[vcast_i] = 0;
      break;
    }
  }

  vcast_command[0] = '0';

  for (vcast_cur = (*vcast_data)->vcast_root; vcast_cur != 0; vcast_cur = vcast_cur->vcast_next)
  {
    if (VCAST_strcmp (vcast_cur->vcast_command, vcast_command) == 0)
      break;
  }

  vcast_command[vcast_i] = vcast_tmp;

  return vcast_cur;
}
#endif /* VCAST_CPP_ENVIRONMENT */

/* This is the place to initialize any global data that is declared
   in this file.  Some of the target compilers do not by default 
   initialize global scope variables, so we do this explicitly here */
void vcastInitializeB1Data (void) {

 vCAST_can_print_constructor = vCAST_true;
 vCAST_DATA_FIELD = 4;
 vcast_global_size = VCAST_NULL;
 vCAST_INDEX = 0;
 VCAST_index_size = VCAST_NULL;
 vCAST_SIZE = vCAST_false;
 vCAST_VALUE = VCAST_FLOAT_ZERO;
 vCAST_VALUE_INT   = 0;
 vCAST_VALUE_NUL = vCAST_false;
 vCAST_VALUE_UNSIGNED   = 0;

}

