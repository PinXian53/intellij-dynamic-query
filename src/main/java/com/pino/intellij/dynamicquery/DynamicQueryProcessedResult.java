package com.pino.intellij.dynamicquery;

import com.pino.intellij.dynamicquery.parser.DynamicQueryProblem;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Result of turning a dynamic query into a plain one.
 *
 * <p>The generated text alone is not enough: IntelliJ reports errors at offsets inside the
 * <em>generated</em> text, and those have to be translated back to offsets inside the original
 * Java string before they can be highlighted. {@link #mappings()} carries that translation, and
 * the language injector turns each mapping into one injection shred so the platform performs the
 * translation itself.
 *
 * @param originalText  the dynamic query as written by the user
 * @param generatedText the plain query handed to the IntelliJ JPQL or SQL parser
 * @param mappings      generated-to-source offset mappings, ordered by generated offset
 * @param problems      problems of the {@code [ ... ]} layer, ordered by source offset
 */
public record DynamicQueryProcessedResult(
        String originalText,
        String generatedText,
        List<OffsetMapping> mappings,
        List<DynamicQueryProblem> problems
) {

    /** Whether the query was free of {@code [ ... ]} syntax errors. */
    public boolean isValid() {
        return problems.isEmpty();
    }

    /**
     * Translates an offset inside {@link #generatedText()} back to an offset inside
     * {@link #originalText()}, or {@code null} when the offset falls outside every mapped run.
     */
    public @Nullable Integer toSourceOffset(int generatedOffset) {
        for (OffsetMapping mapping : mappings) {
            TextRange generated = mapping.generatedRange();
            if (generatedOffset >= generated.getStartOffset() && generatedOffset <= generated.getEndOffset()) {
                return mapping.sourceRange().getStartOffset() + (generatedOffset - generated.getStartOffset());
            }
        }
        return null;
    }

    /**
     * Translates a range inside {@link #generatedText()} back to a range inside
     * {@link #originalText()}, or {@code null} when it cannot be mapped as a whole.
     *
     * <p>A range that spans several optional blocks maps to the range spanning the same blocks in
     * the source, brackets included.
     */
    public @Nullable TextRange toSourceRange(TextRange generatedRange) {
        Integer start = toSourceOffset(generatedRange.getStartOffset());
        Integer end = toSourceOffset(generatedRange.getEndOffset());
        if (start == null || end == null || end < start) {
            return null;
        }
        return new TextRange(start, end);
    }
}
