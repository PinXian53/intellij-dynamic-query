package com.pino.intellij.dynamicquery.parser;

import java.util.List;

/**
 * Outcome of parsing a dynamic query: the syntax tree plus any problems of the {@code [ ... ]}
 * layer.
 */
public record DynamicQueryParseResult(List<DynamicQueryNode> nodes, List<DynamicQueryProblem> problems) {
}
