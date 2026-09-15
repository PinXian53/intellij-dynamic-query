package com.pino.intellij.dynamicquery;

import com.pino.intellij.dynamicquery.parser.DynamicQueryLexer;
import com.pino.intellij.dynamicquery.parser.DynamicQueryToken;
import com.pino.intellij.dynamicquery.parser.DynamicQueryTokenType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class DynamicQueryLexerTest {

    @Test
    public void splitsTextAroundMarkers() {
        List<DynamicQueryToken> tokens = DynamicQueryLexer.tokenize("a[b]c");

        assertEquals(
                List.of(
                        DynamicQueryTokenType.TEXT,
                        DynamicQueryTokenType.OPTIONAL_BLOCK_START,
                        DynamicQueryTokenType.TEXT,
                        DynamicQueryTokenType.OPTIONAL_BLOCK_END,
                        DynamicQueryTokenType.TEXT),
                tokens.stream().map(DynamicQueryToken::type).toList());
    }

    @Test
    public void tokenRangesCoverTheWholeSource() {
        String source = "SELECT e FROM E e WHERE 1 = 1 [ AND e.a = :a ]";

        StringBuilder rebuilt = new StringBuilder();
        for (DynamicQueryToken token : DynamicQueryLexer.tokenize(source)) {
            assertEquals(token.text(), token.range().substring(source));
            rebuilt.append(token.text());
        }

        assertEquals(source, rebuilt.toString());
    }

    @Test
    public void emptySourceHasNoTokens() {
        assertEquals(List.of(), DynamicQueryLexer.tokenize(""));
    }
}
