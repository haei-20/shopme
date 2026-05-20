package com.example.gearshop.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class TableLowerCaseNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        if (name == null) return null;

        String tableName = name.getText().toLowerCase().replace("_", "");

        // quote để tránh MySQL/Aiven tự transform
        return Identifier.toIdentifier(tableName, true);
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        if (name == null) return null;

        // GIỮ NGUYÊN 100% tên cột từ Entity + quote an toàn
        return Identifier.toIdentifier(name.getText(), true);
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment context) {
        return name == null ? null : Identifier.toIdentifier(name.getText(), true);
    }

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment context) {
        return name == null ? null : Identifier.toIdentifier(name.getText(), true);
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment context) {
        return name == null ? null : Identifier.toIdentifier(name.getText(), true);
    }
}