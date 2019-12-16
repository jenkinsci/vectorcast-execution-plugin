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
#ifndef __VCAST_STRING_H__
#define __VCAST_STRING_H__

#ifdef VCAST_HAS_LONGLONG
#ifdef VCAST_MICROSOFT_LONG_LONG
#define VCAST_LONGEST_INT __int64
#define VCAST_LONGEST_UNSIGNED unsigned __int64
#else
#define VCAST_LONGEST_INT long long
#define VCAST_LONGEST_UNSIGNED unsigned long long
#endif
#else
#define VCAST_LONGEST_INT long
#define VCAST_LONGEST_UNSIGNED unsigned long 
#endif

#ifdef VCAST_NO_STDLIB
#define VCAST_NO_EXIT
#define VCAST_NO_MALLOC
#ifndef NULL
#define NULL 0
#endif
#else
#include <stdlib.h>
#endif

#ifndef VCAST_NO_SETJMP
#include <setjmp.h>
#endif

#ifdef VCAST_GH_INT_178B
#ifdef __cplusplus
extern "C" int sprintf (char *, const char *, ...);
#else
int sprintf (char *, const char *, ...);
#endif
#else
#if !defined(VCAST_DO_NOT_INCLUDE_STDIO)
#include <stdio.h>
#endif
#endif

#ifdef VCAST_USE_STD_STRING
#include <string.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif


#ifdef VCAST_NO_MALLOC
#ifndef VCAST_MAX_HEAP_SIZE
#define VCAST_MAX_HEAP_SIZE 0x1000
#endif
void VCAST_free (void * vcast_aptr);
#else
#define VCAST_free free
#endif
/* Wrappers for malloc and realloc are provided in B2.c */
void * VCAST_malloc (unsigned int vcast_size);


#ifdef VCAST_USE_STD_STRING
#ifdef VCAST_HAS_LONGLONG
#ifdef strtoll
#define VCAST_USE_STRTOLL
#define VCAST_atoi(value) strtoll(value, NULL, 10)
#else
VCAST_LONGEST_INT VCAST_atoi (const char *vcast_str );
#endif
#ifdef strtoull
#define VCAST_USE_STRTOULL
#define VCAST_strtoul strtoull
#else
VCAST_LONGEST_UNSIGNED VCAST_strtoul(const char *vcast_nptr, char **vcast_endptr, int vcast_base);
#endif
#else

#define VCAST_USE_STRTOLL
#define VCAST_atoi atol

#define VCAST_USE_STRTOULL
#define VCAST_strtoul strtoul
#endif

#define VCAST_strlen strlen
#define VCAST_strcpy strcpy
#define VCAST_unsigned_strcpy strcpy
#define VCAST_strcmp strcmp
#define VCAST_strcat strcat
#define VCAST_unsigned_strcat strcat
#define VCAST_strncmp strncmp
#define VCAST_memset memset
#define VCAST_memcmp memcmp
#else

VCAST_LONGEST_INT VCAST_atoi (const char *vcast_str );
VCAST_LONGEST_UNSIGNED VCAST_strtoul(const char *vcast_nptr, char **vcast_endptr, int vcast_base);
int   vcast_strlen(const char *VC_S);
void  VCAST_strcpy ( char *VC_S, const char *VC_T );
void  VCAST_unsigned_strcpy ( unsigned char *VC_S, const unsigned char *VC_T );
int   VCAST_strcmp ( char *VC_S, char *VC_T );
char* vcast_strcat (char *VC_S, const char *VC_T);
void  VCAST_unsigned_strcat ( unsigned char VC_S[], const unsigned char VC_T[] );
int   VCAST_strncmp ( const char *VC_S, const char *VC_T, int VC_N );
void *VCAST_memset ( void *vcast_dest, int vcast_value, int vcast_n );
int   VCAST_memcmp ( const void *vcast_s1, const void *vcast_s2, int vcast_n );

#define VCAST_strlen vcast_strlen
#define VCAST_strcat vcast_strcat

#endif /* VCAST_USE_STD_STRING */
int VCAST_signed_strlen (const signed char *vcast_str );
void VCAST_signed_strcpy ( signed char *VC_S, const signed char *VC_T );

#ifdef __cplusplus
}
#endif

#endif
