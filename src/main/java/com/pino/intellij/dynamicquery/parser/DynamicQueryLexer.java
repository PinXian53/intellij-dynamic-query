package com.pino.intellij.dynamicquery.parser;

import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a dynamic query into {@link DynamicQueryToken}s.
 *
 * <p>Only {@code [} and {@code ]} are meaningful; every other character accumulates into a
 * {@link DynamicQueryTokenType#TEXT} token. Brackets inside a string literal
 * ({@code WHERE u.code LIKE '[%'}) are <em>not</em> treated as markers, so such queries keep
 * working. JPQL and SQL agree on single-quoted literals with {@code ''} as the escape, so one
 * lexer serves both dialects.
 */
public final class DynamicQueryLexer {

    public static final char OPTIONAL_BLOCK_START = '[';
    public static final char OPTIONAL_BLOCK_END = ']';
    private static final char STRING_QUOTE = '\'';

    private DynamicQueryLexer() {
    }

    public static List<DynamicQueryToken> tokenize(String source) {
        List<DynamicQueryToken> tokens = new ArrayList<>();
        int length = source.length();
        int textStart = 0;
        int i = 0;
        boolean inStringLiteral = false;

        while (i < length) {
            char c = source.charAt(i);

            if (inStringLiteral) {
                if (c == STRING_QUOTE) {
                    // '' is an escaped quote inside a string literal.
                    if (i + 1 < length && source.charAt(i + 1) == STRING_QUOTE) {
                        i++;
                    } else {
                        inStringLiteral = false;
                    }
                }
                i++;
                continue;
            }

            if (c == STRING_QUOTE) {
                inStringLiteral = true;
                i++;
                continue;
            }

            if (c == OPTIONAL_BLOCK_START || c == OPTIONAL_BLOCK_END) {
                addText(tokens, source, textStart, i);
                DynamicQueryTokenType type = c == OPTIONAL_BLOCK_START
                        ? DynamicQueryTokenType.OPTIONAL_BLOCK_START
                        : DynamicQueryTokenType.OPTIONAL_BLOCK_END;
                tokens.add(new DynamicQueryToken(type, new TextRange(i, i + 1), String.valueOf(c)));
                i++;
                textStart = i;
                continue;
            }

            i++;
        }

        addText(tokens, source, textStart, length);
        return tokens;
    }

    private static void addText(List<DynamicQueryToken> tokens, String source, int start, int end) {
        if (start >= end) {
            return;
        }
        TextRange range = new TextRange(start, end);
        tokens.add(new DynamicQueryToken(DynamicQueryTokenType.TEXT, range, range.substring(source)));
    }
}
