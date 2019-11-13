-- VectorCAST 19.sp1 (06/26/19)
-- Test Case Script
-- 
-- Environment    : SIMPLE_TEST
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

-- Subprogram: <<INIT>>

-- Test Case: <<INIT>>.001
TEST.SUBPROGRAM:<<INIT>>
TEST.NEW
TEST.NAME:<<INIT>>.001
TEST.AUTOMATIC_INITIALIZATION
TEST.VALUE:manager.<<GLOBAL>>.(cl).Manager.Manager.<<constructor>>.Manager().<<call>>:0
TEST.END

-- Unit: manager

-- Subprogram: (cl)Manager::PlaceOrder

-- Test Case: (cl)Manager::PlaceOrder.001
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.001
TEST.COMPOUND_ONLY
TEST.END

-- Test Case: (cl)Manager::PlaceOrder.Steak.BadCF
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.Steak.BadCF
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Table:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Seat:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Order.Entree:Steak
TEST.FLOW
  manager.cpp.(cl)Manager::PlaceOrder
  uut_prototype_stubs.DataBase::GetTableRecord
  manager.cpp.(cl)Manager::AddIncludedDessert
  uut_prototype_stubs.DataBase::UpdateTableRecord
  manager.cpp.(cl)Manager::PlaceOrder
TEST.END_FLOW
TEST.END

-- Test Case: (cl)Manager::PlaceOrder.Steak.BadEV
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.Steak.BadEV
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Table:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Seat:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Order.Entree:Steak
TEST.EXPECTED:uut_prototype_stubs.DataBase::UpdateTableRecord.Data[0].CheckTotal:15
TEST.END

-- Test Case: (cl)Manager::PlaceOrder.Steak.CF
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.Steak.CF
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Table:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Seat:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Order.Entree:Steak
TEST.FLOW
  manager.cpp.<<INIT>>
  manager.cpp.<<INIT>>
  manager.cpp.(cl)Manager::PlaceOrder
  uut_prototype_stubs.DataBase::GetTableRecord
  uut_prototype_stubs.DataBase::UpdateTableRecord
  manager.cpp.(cl)Manager::PlaceOrder
TEST.END_FLOW
TEST.END

-- Test Case: (cl)Manager::PlaceOrder.Steak.EV
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.Steak.EV
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Table:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Seat:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Order.Entree:Steak
TEST.EXPECTED:uut_prototype_stubs.DataBase::UpdateTableRecord.Data[0].CheckTotal:14
TEST.END

-- Test Case: (cl)Manager::PlaceOrder.Steak.NoEV
TEST.UNIT:manager
TEST.SUBPROGRAM:(cl)Manager::PlaceOrder
TEST.NEW
TEST.NAME:(cl)Manager::PlaceOrder.Steak.NoEV
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Table:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Seat:1
TEST.VALUE:manager.(cl)Manager::PlaceOrder.Order.Entree:Steak
TEST.END

-- COMPOUND TESTS

TEST.SUBPROGRAM:<<COMPOUND>>
TEST.NEW
TEST.NAME:<<COMPOUND>>.001
TEST.SLOT: "1", "manager", "(cl)Manager::PlaceOrder", "1", "(cl)Manager::PlaceOrder.Steak.NoEV"
TEST.SLOT: "2", "manager", "(cl)Manager::PlaceOrder", "1", "(cl)Manager::PlaceOrder.Steak.EV"
TEST.SLOT: "3", "manager", "(cl)Manager::PlaceOrder", "1", "(cl)Manager::PlaceOrder.Steak.BadEV"
TEST.SLOT: "4", "manager", "(cl)Manager::PlaceOrder", "1", "(cl)Manager::PlaceOrder.Steak.CF"
TEST.SLOT: "5", "manager", "(cl)Manager::PlaceOrder", "1", "(cl)Manager::PlaceOrder.Steak.BadCF"
TEST.END
--

-- COMPOUND TESTS

TEST.SUBPROGRAM:<<COMPOUND>>
TEST.NEW
TEST.NAME:<<COMPOUND>>.002
TEST.SLOT: "1", "manager", "(cl)Manager::PlaceOrder", "1", "(cl)Manager::PlaceOrder.Steak.EV"
TEST.SLOT: "2", "manager", "(cl)Manager::PlaceOrder", "1", "(cl)Manager::PlaceOrder.Steak.CF"
TEST.END
--
