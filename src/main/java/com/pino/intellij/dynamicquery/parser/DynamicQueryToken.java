package com.pino.intellij.dynamicquery.parser;

import com.intellij.openapi.util.TextRange;

/**
 * A single dynamic query token.
 *
 * @param type  the token kind
 * @param range the range inside the dynamic query source the token was read from
 * @param text  the token text, equal to {@code range.substring(source)}
 */
public record DynamicQueryToken(DynamicQueryTokenType type, TextRange range, String text) {
}
