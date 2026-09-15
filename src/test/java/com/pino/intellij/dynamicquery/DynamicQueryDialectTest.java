package com.pino.intellij.dynamicquery;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DynamicQueryDialectTest {

    @Test
    public void jpqlAnnotationMapsToJpaql() {
        assertEquals(DynamicQueryDialect.JPQL, DynamicQueryDialect.forAnnotationName("DynamicJpqlQuery"));
        assertEquals("JPAQL", DynamicQueryDialect.JPQL.languageId());
    }

    @Test
    public void nativeAnnotationMapsToSql() {
        assertEquals(DynamicQueryDialect.NATIVE_SQL, DynamicQueryDialect.forAnnotationName("DynamicNativeQuery"));
        assertEquals("SQL", DynamicQueryDialect.NATIVE_SQL.languageId());
    }

    @Test
    public void qualifiedNamesAreRecognised() {
        assertEquals(
                DynamicQueryDialect.JPQL,
                DynamicQueryDialect.forAnnotationName("com.pino.persistence.DynamicJpqlQuery"));
        assertEquals(
                DynamicQueryDialect.NATIVE_SQL,
                DynamicQueryDialect.forAnnotationName("com.pino.persistence.DynamicNativeQuery"));
    }

    @Test
    public void otherAnnotationsDeclareNoDialect() {
        assertNull(DynamicQueryDialect.forAnnotationName("org.springframework.data.jpa.repository.Query"));
        assertNull(DynamicQueryDialect.forAnnotationName("DynamicJpqlQueries"));
        assertNull(DynamicQueryDialect.forAnnotationName(null));
    }
}
