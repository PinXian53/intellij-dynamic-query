package com.pino.intellij.dynamicquery.parser;

/**
 * Token kinds of the dynamic query surface syntax.
 *
 * <p>Everything that is not a dynamic query marker is plain {@link #TEXT} and is handed to the
 * IntelliJ JPQL or SQL parser untouched.
 */
public enum DynamicQueryTokenType {
    /** Plain query text, in whatever dialect the annotation declares. */
    TEXT,
    /** The {@code [} that opens an optional predicate block. */
    OPTIONAL_BLOCK_START,
    /** The {@code ]} that closes an optional predicate block. */
    OPTIONAL_BLOCK_END,
}
