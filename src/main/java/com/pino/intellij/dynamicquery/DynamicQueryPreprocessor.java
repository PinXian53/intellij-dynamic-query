package com.pino.intellij.dynamicquery;

import com.pino.intellij.dynamicquery.parser.DynamicQueryNode;
import com.pino.intellij.dynamicquery.parser.DynamicQueryParseResult;
import com.pino.intellij.dynamicquery.parser.DynamicQueryParser;
import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a dynamic query into the plain JPQL or SQL that IntelliJ's parsers understand.
 *
 * <pre>{@code
 * SELECT u FROM UserEntity u         SELECT u FROM UserEntity u
 * WHERE 1 = 1                  ==>   WHERE 1 = 1
 * [ AND u.name = :name ]              AND u.name = :name
 * }</pre>
 *
 * <p>The optional-predicate markers are dropped; the predicate itself is kept, because the IDE has
 * to check entities, columns and parameters of <em>every</em> branch — which branches actually
 * survive is a runtime decision the IDE knows nothing about. The dynamic syntax is the same in
 * both dialects, so this class needs no knowledge of which one it is processing.
 */
public final class DynamicQueryPreprocessor {

    private DynamicQueryPreprocessor() {
    }

    /** Preprocesses {@code source} and reports how the result maps back onto it. */
    public static DynamicQueryProcessedResult process(String source) {
        DynamicQueryParseResult parsed = DynamicQueryParser.parse(source);

        StringBuilder generated = new StringBuilder(source.length());
        List<OffsetMapping> mappings = new ArrayList<>();
        flatten(parsed.nodes(), generated, mappings);

        return new DynamicQueryProcessedResult(
                source,
                generated.toString(),
                List.copyOf(mappings),
                parsed.problems());
    }

    /** Convenience for callers that only need the generated query text. */
    public static String processToString(String source) {
        return process(source).generatedText();
    }

    private static void flatten(
            List<DynamicQueryNode> nodes,
            StringBuilder generated,
            List<OffsetMapping> mappings
    ) {
        for (DynamicQueryNode node : nodes) {
            switch (node) {
                case DynamicQueryNode.TextNode text -> {
                    if (text.text().isEmpty()) {
                        continue;
                    }
                    int start = generated.length();
                    generated.append(text.text());
                    mappings.add(new OffsetMapping(
                            text.range(),
                            new TextRange(start, generated.length())));
                }
                // Markers are dropped, the body is kept.
                case DynamicQueryNode.OptionalBlock block -> flatten(block.children(), generated, mappings);
            }
        }
    }
}
