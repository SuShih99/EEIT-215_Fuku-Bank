CREATE TABLE black_list
(
    id         bigint IDENTITY (1, 1) NOT NULL,
    list_type  varchar(20)            NOT NULL,
    list_value varchar(100)           NOT NULL,
    source     varchar(50),
    reason     varchar(255),
    status     bit                    NOT NULL,
    expires_at datetime,
    created_at datetime,
    updated_at datetime,
    CONSTRAINT pk_black_list PRIMARY KEY (id)
)
GO

CREATE NONCLUSTERED INDEX idx_bl_lookup ON black_list (list_type, list_value)
GO
CREATE TABLE black_list
(
    id         bigint IDENTITY (1, 1) NOT NULL,
    list_type  varchar(20)            NOT NULL,
    list_value varchar(100)           NOT NULL,
    source     varchar(50),
    reason     varchar(255),
    status     bit                    NOT NULL,
    expires_at datetime,
    created_at datetime,
    updated_at datetime,
    CONSTRAINT pk_black_list PRIMARY KEY (id)
)
GO

CREATE NONCLUSTERED INDEX idx_bl_lookup ON black_list (list_type, list_value)
GO