/*
===============================================================================
Java Easy Bank 客戶模組 (Customer Module)
===============================================================================
*/

IF OBJECT_ID('CUSTOMER_RISK_TAG', 'U') IS NOT NULL DROP TABLE CUSTOMER_RISK_TAG;
IF OBJECT_ID('CUSTOMER_KYC', 'U') IS NOT NULL DROP TABLE CUSTOMER_KYC;
IF OBJECT_ID('CUSTOMER_AUTH', 'U') IS NOT NULL DROP TABLE CUSTOMER_AUTH;
IF OBJECT_ID('CUSTOMER_PROFILE', 'U') IS NOT NULL DROP TABLE CUSTOMER_PROFILE;
IF OBJECT_ID('CUSTOMER_APPLICATION', 'U') IS NOT NULL DROP TABLE CUSTOMER_APPLICATION;
GO