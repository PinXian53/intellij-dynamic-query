package com.pino.intellij.dynamicquery.parser;

import com.intellij.openapi.util.TextRange;

/**
 * A dynamic query syntax problem, reported at a range inside the dynamic query source.
 *
 * <p>These are problems of the {@code [ ... ]} layer only. Everything about the query itself is
 * left to IntelliJ's own JPQL and SQL inspections.
 */
public record DynamicQueryProblem(TextRange range, String message) {
}
