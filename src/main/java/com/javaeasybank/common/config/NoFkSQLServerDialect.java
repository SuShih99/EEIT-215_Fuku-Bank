package com.javaeasybank.common.config;

import org.hibernate.boot.Metadata;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;

/**
 * 自訂的 SQL Server Dialect，用於全局關閉 Foreign Key 的生成。
 * 當 hibernate.ddl-auto=create 時，將不會在資料庫中建立任何 FK 約束。
 */
public class NoFkSQLServerDialect extends SQLServerDialect {

    @Override
    public Exporter<ForeignKey> getForeignKeyExporter() {
        return new Exporter<ForeignKey>() {
            @Override
            public String[] getSqlCreateStrings(ForeignKey exportable, Metadata metadata, SqlStringGenerationContext context) {
                // 回傳空陣列，Hibernate 就不會產生 ALTER TABLE ADD CONSTRAINT 語法
                return new String[0];
            }

            @Override
            public String[] getSqlDropStrings(ForeignKey exportable, Metadata metadata, SqlStringGenerationContext context) {
                return new String[0];
            }
        };
    }
}
