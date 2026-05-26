/*
===============================================================================
Java Easy Bank 系統權限模組 (Auth Module)
===============================================================================
*/

IF OBJECT_ID('AUTH_ACTION_LOG', 'U') IS NOT NULL DROP TABLE AUTH_ACTION_LOG;
IF OBJECT_ID('AUTH_LOGIN_LOG', 'U') IS NOT NULL DROP TABLE AUTH_LOGIN_LOG;
IF OBJECT_ID('AUTH_EMP', 'U') IS NOT NULL DROP TABLE AUTH_EMP;
IF OBJECT_ID('AUTH_ROLE', 'U') IS NOT NULL DROP TABLE AUTH_ROLE;
IF OBJECT_ID('AUTH_DEPT', 'U') IS NOT NULL DROP TABLE AUTH_DEPT;
GO
