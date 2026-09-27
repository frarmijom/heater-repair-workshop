package com.heaterworkshop.infrastructure.persistence;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.internal.ColumnInformationImpl;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JpaRepairOrderPostgresDdlTest {
    @Test
    void addsNullableStringServiceTypeWithoutRewritingHistoricalIssueConstraint() {
        // Generate PostgreSQL DDL offline with the actual Hibernate version and entity mapping.
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", PostgreSQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build();
        try {
            var metadata = new MetadataSources(registry).addAnnotatedClass(JpaRepairOrderEntity.class)
                    .buildMetadata();
            var table = metadata.getEntityBinding(JpaRepairOrderEntity.class.getName()).getTable();
            var legacy = mock(TableInformation.class);
            when(legacy.getColumn(any(Identifier.class))).thenAnswer(invocation -> {
                Identifier id = invocation.getArgument(0);
                if (id.getText().equals("service_type")) return null;
                var column = table.getColumn(id);
                return new ColumnInformationImpl(legacy, id, column.getSqlTypeCode(metadata),
                        column.getSqlType(metadata), column.getLength() == null ? 0 : column.getLength().intValue(),
                        6, false);
            });
            var environment = registry.getService(JdbcEnvironment.class);
            var dialect = environment.getDialect();
            var sql = dialect.getTableMigrator().getSqlAlterStrings(table, metadata, legacy,
                    SqlStringGenerationContextImpl.forTests(environment));
            assertEquals(1, sql.length);
            String alteration = sql[0].toLowerCase(Locale.ROOT);
            assertTrue(alteration.contains("add column service_type varchar(32)"), alteration);
            assertFalse(alteration.contains("not null"), alteration);
            assertFalse(alteration.contains("reported_issue"), alteration);
            assertTrue(table.getColumn(Identifier.toIdentifier("reported_issue")).isNullable());
            assertTrue(table.getColumn(Identifier.toIdentifier("service_type")).isNullable());
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
