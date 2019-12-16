----------------------------------------------------
-- VectorCAST Test Harness Component --
-- Copyright 2019 Vector Informatik, GmbH. --
----------------------------------------------------
package body VCAST_Ada_Options is
  function VCAST_APPEND_TO_TESTINSS return boolean is
   VCAST_APPEND_TO_TESTINSS : constant boolean := FALSE;
    begin
      return VCAST_APPEND_TO_TESTINSS;
    end VCAST_APPEND_TO_TESTINSS;
  function VCAST_USE_STATIC_MEMORY return boolean is
    VCAST_USE_STATIC_MEMORY : constant boolean := FALSE;
    begin
      return VCAST_USE_STATIC_MEMORY;
    end VCAST_USE_STATIC_MEMORY;
  function VCAST_MAX_COVERED_SUBPROGRAMS return integer is
    VCAST_MAX_COVERED_SUBPROGRAMS : constant integer := 1;
    begin
      return VCAST_MAX_COVERED_SUBPROGRAMS;
    end VCAST_MAX_COVERED_SUBPROGRAMS;
  function VCAST_MAX_FILES return integer is
        VCAST_MAX_FILES : constant integer := 20;
    begin
      return VCAST_MAX_FILES;
    end VCAST_MAX_FILES;
  function VCAST_MAX_MCDC_STATEMENTS return integer is
    VCAST_MAX_MCDC_STATEMENTS : constant integer := 1;
    begin
      return VCAST_MAX_MCDC_STATEMENTS;
    end VCAST_MAX_MCDC_STATEMENTS;
  function VCAST_MAX_STRING_LENGTH return integer is
  VCAST_MAX_STRING_LENGTH : constant integer := 1000;
    begin
      return VCAST_MAX_STRING_LENGTH;
    end VCAST_MAX_STRING_LENGTH;
  function VCAST_MAX_RANGE return integer is
    VCAST_MAX_RANGE : constant integer := 20;
    begin
      return VCAST_MAX_RANGE;
    end VCAST_MAX_RANGE;
end VCAST_Ada_Options;
