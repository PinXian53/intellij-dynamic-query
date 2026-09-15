package com.pino.intellij.dynamicquery;

import com.intellij.openapi.util.TextRange;

/**
 * Maps one contiguous run of the generated query back to the dynamic query it came from.
 *
 * <p>Both ranges always have the same length: the preprocessor only ever <em>removes</em>
 * characters, it never rewrites them.
 *
 * @param sourceRange    range inside the dynamic query source
 * @param generatedRange range inside the generated query
 */
public record OffsetMapping(TextRange sourceRange, TextRange generatedRange) {

    public OffsetMapping {
        if (sourceRange.getLength() != generatedRange.getLength()) {
            throw new IllegalArgumentException(
                    "source and generated ranges must have the same length: "
                            + sourceRange + " vs " + generatedRange);
        }
    }

    /** Length of the mapped run, in characters. */
    public int length() {
        return sourceRange.getLength();
    }
}
