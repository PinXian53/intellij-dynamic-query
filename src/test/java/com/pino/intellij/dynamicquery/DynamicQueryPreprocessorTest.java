package com.pino.intellij.dynamicquery;

import com.pino.intellij.dynamicquery.parser.DynamicQueryProblem;
import com.intellij.openapi.util.TextRange;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DynamicQueryPreprocessorTest {

    @Test
    public void plainJpqlIsUnchanged() {
        String jpql = "SELECT u FROM UserEntity u WHERE u.name = :name";

        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(jpql);

        assertEquals(jpql, result.generatedText());
        assertTrue(result.isValid());
        assertEquals(1, result.mappings().size());
    }

    @Test
    public void optionalPredicateLosesItsMarkers() {
        String result = DynamicQueryPreprocessor.processToString(
                "WHERE 1 = 1 [ AND u.name = :name ]");

        assertEquals("WHERE 1 = 1  AND u.name = :name ", result);
    }

    @Test
    public void multipleOptionalPredicates() {
        String source = """
                SELECT u FROM UserEntity u
                WHERE 1 = 1
                [ AND u.name = :name ]
                [ AND u.age = :age ]
                [ AND u.roleId IN :roleIds ]
                ORDER BY u.createdAt DESC
                """;

        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(source);

        // `\s` keeps the trailing space each removed `]` leaves behind; a text block would
        // otherwise strip it.
        assertEquals("""
                SELECT u FROM UserEntity u
                WHERE 1 = 1
                 AND u.name = :name\s
                 AND u.age = :age\s
                 AND u.roleId IN :roleIds\s
                ORDER BY u.createdAt DESC
                """, result.generatedText());
        assertTrue(result.isValid());
        assertEquals(7, result.mappings().size());
    }

    @Test
    public void joinsAndSubqueriesAreLeftAlone() {
        String source = """
                SELECT u FROM UserEntity u
                JOIN u.roles r
                WHERE EXISTS (SELECT 1 FROM AuditEntity a WHERE a.id = u.id)
                [ AND r.code = :code ]
                """;

        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(source);

        assertTrue(result.isValid());
        assertFalse(result.generatedText().contains("["));
        assertTrue(result.generatedText().contains("EXISTS (SELECT 1 FROM AuditEntity a"));
    }

    @Test
    public void nestedOptionalBlocks() {
        String result = DynamicQueryPreprocessor.processToString(
                "WHERE 1 = 1 [ AND (u.a = :a [ OR u.b = :b ]) ]");

        assertEquals("WHERE 1 = 1  AND (u.a = :a  OR u.b = :b ) ", result);
    }

    @Test
    public void bracketsInsideStringLiteralsAreNotMarkers() {
        String source = "WHERE u.code LIKE '[%' [ AND u.name = :name ]";

        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(source);

        assertEquals("WHERE u.code LIKE '[%'  AND u.name = :name ", result.generatedText());
        assertTrue(result.isValid());
    }

    @Test
    public void escapedQuoteDoesNotEndTheStringLiteral() {
        String source = "WHERE u.code = 'it''s [ok]' [ AND u.name = :name ]";

        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(source);

        assertEquals("WHERE u.code = 'it''s [ok]'  AND u.name = :name ", result.generatedText());
        assertTrue(result.isValid());
    }

    @Test
    public void emptyOptionalBlockProducesNoText() {
        assertEquals("WHERE 1 = 1 ", DynamicQueryPreprocessor.processToString("WHERE 1 = 1 []"));
    }

    @Test
    public void unclosedBracketIsReported() {
        DynamicQueryProcessedResult result =
                DynamicQueryPreprocessor.process("WHERE 1 = 1 [ AND u.name = :name");

        List<DynamicQueryProblem> problems = result.problems();
        assertEquals(1, problems.size());
        assertEquals(new TextRange(12, 13), problems.get(0).range());
        assertTrue(problems.get(0).message().contains("Unclosed"));
        // The body is still injected, so the IDE keeps checking the predicate while it is typed.
        assertEquals("WHERE 1 = 1  AND u.name = :name", result.generatedText());
    }

    @Test
    public void strayClosingBracketIsReportedAndDropped() {
        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process("WHERE 1 = 1 ]");

        assertEquals(1, result.problems().size());
        assertTrue(result.problems().get(0).message().contains("Unmatched"));
        assertEquals("WHERE 1 = 1 ", result.generatedText());
    }

    @Test
    public void nativeSqlUsesTheSameDynamicSyntax() {
        String source = """
                SELECT * FROM USERS u
                WHERE 1 = 1
                [ AND u.NAME = :name ]
                [ AND u.AGE = :age ]
                ORDER BY u.CREATED_AT DESC
                """;

        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(source);

        assertEquals("""
                SELECT * FROM USERS u
                WHERE 1 = 1
                 AND u.NAME = :name\s
                 AND u.AGE = :age\s
                ORDER BY u.CREATED_AT DESC
                """, result.generatedText());
        assertTrue(result.isValid());
    }

    @Test
    public void emptyQuery() {
        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process("");

        assertEquals("", result.generatedText());
        assertTrue(result.mappings().isEmpty());
        assertTrue(result.isValid());
    }

    @Test
    public void generatedOffsetsMapBackToTheSource() {
        String source = "WHERE 1 = 1 [ AND u.name = :name ]";
        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(source);

        int generated = result.generatedText().indexOf("u.name");
        Integer mapped = result.toSourceOffset(generated);

        assertEquals(Integer.valueOf(source.indexOf("u.name")), mapped);
    }

    @Test
    public void generatedRangesMapBackToTheSource() {
        String source = """
                WHERE 1 = 1
                [ AND u.name = :name ]
                [ AND u.age = :age ]
                """;
        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(source);

        int start = result.generatedText().indexOf("u.age");
        TextRange mapped = result.toSourceRange(new TextRange(start, start + "u.age".length()));

        assertEquals("u.age", mapped.substring(source));
    }

    @Test
    public void everyMappingKeepsTheSameTextOnBothSides() {
        String source = """
                SELECT u FROM UserEntity u
                WHERE 1 = 1
                [ AND u.name = :name ]
                ORDER BY u.createdAt DESC
                """;
        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(source);

        for (OffsetMapping mapping : result.mappings()) {
            assertEquals(
                    mapping.sourceRange().substring(source),
                    mapping.generatedRange().substring(result.generatedText()));
        }
    }

    @Test
    public void offsetOutsideAnyMappingIsUnmapped() {
        DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process("[a]");

        assertNull(result.toSourceOffset(99));
    }
}
