package com.pino.intellij.dynamicquery;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DynamicQueryDialectTest {

    @Test
    public void plainQueriesMapToJpaql() {
        assertEquals(DynamicQueryDialect.JPQL, DynamicQueryDialect.forNativeQuery(false));
        assertEquals("JPAQL", DynamicQueryDialect.JPQL.languageId());
    }

    @Test
    public void nativeQueriesMapToSql() {
        assertEquals(DynamicQueryDialect.NATIVE_SQL, DynamicQueryDialect.forNativeQuery(true));
        assertEquals("SQL", DynamicQueryDialect.NATIVE_SQL.languageId());
    }
}
