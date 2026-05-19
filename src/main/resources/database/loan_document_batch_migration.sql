-- Idempotent SQL Server migration for the loan document batch fields.
-- Run this once on databases that existed before document_batch_type/document_batch_no were added.

IF COL_LENGTH('dbo.loan_application', 'current_supplement_batch_no') IS NULL
BEGIN
    ALTER TABLE dbo.loan_application ADD current_supplement_batch_no INT NULL;
END
GO

UPDATE dbo.loan_application
SET current_supplement_batch_no = 0
WHERE current_supplement_batch_no IS NULL;
GO

ALTER TABLE dbo.loan_application ALTER COLUMN current_supplement_batch_no INT NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
    JOIN sys.columns c
      ON c.object_id = dc.parent_object_id
     AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID(N'dbo.loan_application')
      AND c.name = N'current_supplement_batch_no'
)
BEGIN
    ALTER TABLE dbo.loan_application
        ADD CONSTRAINT DF_loan_application_current_supplement_batch_no
        DEFAULT 0 FOR current_supplement_batch_no;
END
GO

IF COL_LENGTH('dbo.loan_document', 'document_batch_type') IS NULL
BEGIN
    ALTER TABLE dbo.loan_document ADD document_batch_type NVARCHAR(20) NULL;
END
GO

IF COL_LENGTH('dbo.loan_document', 'document_batch_no') IS NULL
BEGIN
    ALTER TABLE dbo.loan_document ADD document_batch_no INT NULL;
END
GO

IF COL_LENGTH('dbo.loan_document', 'submitted_at') IS NULL
BEGIN
    ALTER TABLE dbo.loan_document ADD submitted_at DATETIME2 NULL;
END
GO

UPDATE dbo.loan_document
SET document_batch_type = N'INITIAL'
WHERE document_batch_type IS NULL;
GO

UPDATE dbo.loan_document
SET document_batch_no = 0
WHERE document_batch_no IS NULL;
GO

ALTER TABLE dbo.loan_document ALTER COLUMN document_batch_type NVARCHAR(20) NOT NULL;
GO

ALTER TABLE dbo.loan_document ALTER COLUMN document_batch_no INT NOT NULL;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
    JOIN sys.columns c
      ON c.object_id = dc.parent_object_id
     AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID(N'dbo.loan_document')
      AND c.name = N'document_batch_type'
)
BEGIN
    ALTER TABLE dbo.loan_document
        ADD CONSTRAINT DF_loan_document_batch_type
        DEFAULT N'INITIAL' FOR document_batch_type;
END
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
    JOIN sys.columns c
      ON c.object_id = dc.parent_object_id
     AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID(N'dbo.loan_document')
      AND c.name = N'document_batch_no'
)
BEGIN
    ALTER TABLE dbo.loan_document
        ADD CONSTRAINT DF_loan_document_batch_no
        DEFAULT 0 FOR document_batch_no;
END
GO
