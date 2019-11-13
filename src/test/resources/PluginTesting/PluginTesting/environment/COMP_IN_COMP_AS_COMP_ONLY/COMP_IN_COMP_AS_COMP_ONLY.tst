-- VectorCAST 19.sp1 (06/26/19)
-- Test Case Script
-- 
-- Environment    : COMP_IN_COMP_AS_COMP_ONLY
-- Unit(s) Under Test: manager
-- 
-- Script Features
TEST.SCRIPT_FEATURE:C_DIRECT_ARRAY_INDEXING
TEST.SCRIPT_FEATURE:CPP_CLASS_OBJECT_REVISION
TEST.SCRIPT_FEATURE:MULTIPLE_UUT_SUPPORT
TEST.SCRIPT_FEATURE:MIXED_CASE_NAMES
TEST.SCRIPT_FEATURE:STANDARD_SPACING_R2
TEST.SCRIPT_FEATURE:OVERLOADED_CONST_SUPPORT
TEST.SCRIPT_FEATURE:UNDERSCORE_NULLPTR
TEST.SCRIPT_FEATURE:FULL_PARAMETER_TYPES
TEST.SCRIPT_FEATURE:STRUCT_DTOR_ADDS_POINTER
TEST.SCRIPT_FEATURE:STRUCT_FIELD_CTOR_ADDS_POINTER
TEST.SCRIPT_FEATURE:STATIC_HEADER_FUNCS_IN_UUTS
TEST.SCRIPT_FEATURE:VCAST_MAIN_NOT_RENAMED
--

-- Unit: manager

-- Subprogram: (cl)Manager::PlaceOrder

-- Test Case: (cl)Manager::PlaceOrder.001
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.001
TEST.COMPOUND_ONLY
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Table:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Seat:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Order.Entree:Steak
TEST.END

-- Test Case: (cl)Manager::PlaceOrder.002
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.002
TEST.COMPOUND_ONLY
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Table:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Seat:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Order.Entree:Steak
TEST.END

-- Test Case: (cl)Manager::PlaceOrder.003
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.003
TEST.COMPOUND_ONLY
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Table:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Seat:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Order.Entree:Steak
TEST.END

-- COMPOUND TESTS

TEST.SUBPROGRAM:<<COMPOUND>>
TEST.NEW
TEST.NAME:MyCompoundCompoundOnly
TEST.COMPOUND_ONLY
TEST.SLOT: "1", "<<COMPOUND>>", "<<COMPOUND>>", "1", "MyCompoundOnly"
TEST.END
--

-- COMPOUND TESTS

TEST.SUBPROGRAM:<<COMPOUND>>
TEST.NEW
TEST.NAME:MyCompoundOnly
TEST.COMPOUND_ONLY
TEST.SLOT: "1", "manager", "(cl)Manager::PlaceOrder", "1", "(cl)Manager::PlaceOrder.001"
TEST.END
--

-- COMPOUND TESTS

TEST.SUBPROGRAM:<<COMPOUND>>
TEST.NEW
TEST.NAME:MyOuterCompound
TEST.SLOT: "1", "<<COMPOUND>>", "<<COMPOUND>>", "1", "MyCompoundCompoundOnly"
TEST.END
--
