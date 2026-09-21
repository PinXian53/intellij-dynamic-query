package com.pino.intellij.dynamicquery;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DynamicQueryAnnotationDetectorTest {

    @Test
    public void valueCarriesAQuery() {
        assertTrue(DynamicQueryAnnotationDetector.isQueryAttribute("value"));
    }

    @Test
    public void countQueryCarriesAQuery() {
        assertTrue(DynamicQueryAnnotationDetector.isQueryAttribute("countQuery"));
    }

    @Test
    public void unnamedAttributeIsTheValueShorthand() {
        assertFalse(DynamicQueryAnnotationDetector.isQueryAttribute("Value"));
        assertTrue(DynamicQueryAnnotationDetector.isQueryAttribute(null));
    }

    @Test
    public void otherAttributesCarryNoQuery() {
        assertFalse(DynamicQueryAnnotationDetector.isQueryAttribute("nativeQuery"));
        assertFalse(DynamicQueryAnnotationDetector.isQueryAttribute("countquery"));
        assertFalse(DynamicQueryAnnotationDetector.isQueryAttribute(""));
    }

    @Test
    public void simpleAndQualifiedAnnotationNamesAreRecognised() {
        assertTrue(DynamicQueryAnnotationDetector.isDynamicQueryAnnotationName("DynamicQuery"));
        assertTrue(DynamicQueryAnnotationDetector.isDynamicQueryAnnotationName(
                "com.pino.persistence.DynamicQuery"));
    }

    @Test
    public void otherAnnotationsAreNotDynamicQueries() {
        assertFalse(DynamicQueryAnnotationDetector.isDynamicQueryAnnotationName(
                "org.springframework.data.jpa.repository.Query"));
        assertFalse(DynamicQueryAnnotationDetector.isDynamicQueryAnnotationName("DynamicQueries"));
        assertFalse(DynamicQueryAnnotationDetector.isDynamicQueryAnnotationName(null));
    }
}
