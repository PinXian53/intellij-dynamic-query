package com.pino.intellij.dynamicquery.parser;

import com.intellij.openapi.util.TextRange;

import java.util.List;

/**
 * Node of the dynamic query syntax tree.
 *
 * <p>The tree exists so that the dynamic syntax stays a real (if tiny) language rather than a
 * textual search-and-replace: new constructs can be added as new node kinds without touching the
 * injector.
 */
public sealed interface DynamicQueryNode {

    /** Range of this node inside the dynamic query source. */
    TextRange range();

    /** A run of plain query text that is passed to the IntelliJ parser as-is. */
    record TextNode(TextRange range, String text) implements DynamicQueryNode {
    }

    /**
     * An optional predicate, i.e. {@code [ AND u.name = :name ]}.
     *
     * @param openMarker  range of the {@code [}, or {@code null} when it was never opened
     * @param closeMarker range of the {@code ]}, or {@code null} when the block was left unclosed
     */
    record OptionalBlock(
            TextRange range,
            TextRange openMarker,
            TextRange closeMarker,
            List<DynamicQueryNode> children
    ) implements DynamicQueryNode {
    }
}
