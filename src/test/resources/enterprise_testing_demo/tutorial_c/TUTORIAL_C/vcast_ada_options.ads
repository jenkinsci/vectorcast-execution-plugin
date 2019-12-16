----------------------------------------------------
-- VectorCAST Test Harness Component --
-- Copyright 2019 Vector Informatik, GmbH. --
----------------------------------------------------
package VCAST_Ada_Options is
  function VCAST_APPEND_TO_TESTINSS return boolean;
  function VCAST_USE_STATIC_MEMORY return boolean;
  function VCAST_MAX_COVERED_SUBPROGRAMS return integer;
  function VCAST_MAX_FILES return integer;
  function VCAST_MAX_MCDC_STATEMENTS return integer;
  function VCAST_MAX_STRING_LENGTH return integer;
  function VCAST_MAX_RANGE return integer;
end VCAST_Ada_Options;
