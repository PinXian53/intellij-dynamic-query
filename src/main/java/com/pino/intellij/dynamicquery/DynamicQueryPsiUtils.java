package com.pino.intellij.dynamicquery;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPolyadicExpression;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** PSI helpers shared by the injector and the annotator. */
public final class DynamicQueryPsiUtils {

    private static final String TEXT_BLOCK_DELIMITER = "\"\"\"";

    private DynamicQueryPsiUtils() {
    }

    /** Whether the literal is a {@code String} literal (text block included). */
    public static boolean isStringLiteral(@Nullable PsiElement element) {
        return element instanceof PsiLiteralExpression literal && literal.getValue() instanceof String;
    }

    /**
     * The range of the literal's <em>content</em> within its own text, i.e. what sits between the
     * quotes.
     *
     * <p>Offsets are raw offsets into {@link PsiElement#getText()}, which is what injection shreds
     * expect; for a text block the leading indentation of each line is therefore part of the
     * range, and the platform's text escaper strips it when the injected document is built.
     *
     * @return the content range, or {@code null} for a literal that is not a well-formed string
     */
    public static @Nullable TextRange getContentRange(PsiLiteralExpression literal) {
        String text = literal.getText();
        if (text == null) {
            return null;
        }

        if (text.startsWith(TEXT_BLOCK_DELIMITER)) {
            // The content of a text block starts on the line after the opening delimiter.
            int i = TEXT_BLOCK_DELIMITER.length();
            while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t' || text.charAt(i) == '\r')) {
                i++;
            }
            if (i >= text.length() || text.charAt(i) != '\n') {
                return null;
            }
            int start = i + 1;
            int end = text.length();
            if (end - start >= TEXT_BLOCK_DELIMITER.length() && text.endsWith(TEXT_BLOCK_DELIMITER)) {
                end -= TEXT_BLOCK_DELIMITER.length();
            }
            return start <= end ? new TextRange(start, end) : null;
        }

        if (text.startsWith("\"")) {
            int end = text.length();
            if (end > 1 && text.endsWith("\"")) {
                end--;
            }
            return end >= 1 ? new TextRange(1, end) : null;
        }

        return null;
    }

    /**
     * The string literals making up an annotation value, in source order.
     *
     * <p>A bare literal yields itself; a {@code "a" + "b"} concatenation yields its operands so
     * that a query split across several literals is still injected as one JPQL fragment. Returns
     * an empty list when the expression is not a pure string constant, or when it is a literal
     * whose enclosing concatenation will be handled instead.
     */
    public static List<PsiLiteralExpression> collectStringLiterals(PsiElement context) {
        if (context instanceof PsiPolyadicExpression polyadic) {
            if (!JavaTokenType.PLUS.equals(polyadic.getOperationTokenType())) {
                return List.of();
            }
            List<PsiLiteralExpression> literals = new ArrayList<>();
            for (PsiExpression operand : polyadic.getOperands()) {
                if (!isStringLiteral(operand)) {
                    // A non-literal operand (a constant reference, say) has no text to inject
                    // into, so the concatenation as a whole is left alone.
                    return List.of();
                }
                literals.add((PsiLiteralExpression) operand);
            }
            return literals;
        }

        if (isStringLiteral(context)) {
            if (context.getParent() instanceof PsiPolyadicExpression) {
                return List.of();
            }
            return List.of((PsiLiteralExpression) context);
        }

        return List.of();
    }
}
