package com.pino.intellij.dynamicquery.highlight;

import com.pino.intellij.dynamicquery.DynamicQueryAnnotationDetector;
import com.pino.intellij.dynamicquery.DynamicQueryPreprocessor;
import com.pino.intellij.dynamicquery.DynamicQueryProcessedResult;
import com.pino.intellij.dynamicquery.DynamicQueryPsiUtils;
import com.pino.intellij.dynamicquery.parser.DynamicQueryProblem;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * Reports unbalanced {@code [ ]} in a dynamic query.
 *
 * <p>Without this, a stray bracket would silently change which predicates get injected, and the
 * user would only see a puzzling JPQL or SQL error somewhere else in the query — or none at all.
 */
public final class DynamicQuerySyntaxAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!DynamicQueryPsiUtils.isStringLiteral(element)) {
            return;
        }
        PsiLiteralExpression literal = (PsiLiteralExpression) element;
        if (!DynamicQueryAnnotationDetector.isDynamicQuery(literal)) {
            return;
        }
        TextRange contentRange = DynamicQueryPsiUtils.getContentRange(literal);
        if (contentRange == null) {
            return;
        }

        DynamicQueryProcessedResult result =
                DynamicQueryPreprocessor.process(contentRange.substring(literal.getText()));
        int literalStart = literal.getTextRange().getStartOffset() + contentRange.getStartOffset();

        for (DynamicQueryProblem problem : result.problems()) {
            holder.newAnnotation(HighlightSeverity.ERROR, problem.message())
                    .range(problem.range().shiftRight(literalStart))
                    .create();
        }
    }
}
