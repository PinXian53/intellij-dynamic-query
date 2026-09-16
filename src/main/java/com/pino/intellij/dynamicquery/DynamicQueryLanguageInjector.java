package com.pino.intellij.dynamicquery;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiPolyadicExpression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Injects the right query language into the queries of a dynamic query annotation — both its
 * {@code value} and its optional {@code countQuery} — so that no {@code @Language(...)} is needed
 * on the declaration:
 *
 * <ul>
 *   <li>{@code @DynamicJpqlQuery} &rarr; JPAQL</li>
 *   <li>{@code @DynamicNativeQuery} &rarr; SQL</li>
 * </ul>
 *
 * <p>The {@code [ ... ]} markers are removed by <em>not injecting them</em>: instead of one shred
 * covering the whole string, one shred is registered per run of plain query text, skipping the two
 * bracket characters. The platform then owns the offset mapping, which means errors, completion
 * and rename land on the right character of the original Java string without this plugin
 * translating anything itself.
 *
 * <pre>{@code
 * [ AND u.name = :name ][ AND u.age = :age ]
 *  └───── shred 1 ────┘  └──── shred 2 ───┘
 * }</pre>
 */
public final class DynamicQueryLanguageInjector implements MultiHostInjector {

    @Override
    public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return List.of(PsiLiteralExpression.class, PsiPolyadicExpression.class);
    }

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        DynamicQueryDialect dialect = DynamicQueryAnnotationDetector.findDialect(context);
        if (dialect == null) {
            return;
        }
        Language language = dialect.findLanguage();
        if (language == null) {
            // The IDE has no support for this dialect (JPAQL and SQL both need IntelliJ IDEA
            // Ultimate); leaving the string alone beats injecting something nobody can parse.
            return;
        }

        List<PsiLiteralExpression> literals = DynamicQueryPsiUtils.collectStringLiterals(context);
        if (literals.isEmpty()) {
            return;
        }

        List<Shred> shreds = collectShreds(literals);
        if (shreds.isEmpty()) {
            // An empty query has nothing to inject into, and `doneInjecting` with no place is an
            // error.
            return;
        }

        registrar.startInjecting(language);
        for (Shred shred : shreds) {
            registrar.addPlace(null, null, shred.host(), shred.rangeInsideHost());
        }
        registrar.doneInjecting();
    }

    private static List<Shred> collectShreds(List<PsiLiteralExpression> literals) {
        List<Shred> shreds = new ArrayList<>();
        for (PsiLiteralExpression literal : literals) {
            if (!(literal instanceof PsiLanguageInjectionHost host) || !host.isValidHost()) {
                continue;
            }
            TextRange contentRange = DynamicQueryPsiUtils.getContentRange(literal);
            if (contentRange == null || contentRange.isEmpty()) {
                continue;
            }

            String content = contentRange.substring(literal.getText());
            DynamicQueryProcessedResult result = DynamicQueryPreprocessor.process(content);
            for (OffsetMapping mapping : result.mappings()) {
                TextRange rangeInsideHost = mapping.sourceRange().shiftRight(contentRange.getStartOffset());
                if (!rangeInsideHost.isEmpty()) {
                    shreds.add(new Shred(host, rangeInsideHost));
                }
            }
        }
        return shreds;
    }

    private record Shred(PsiLanguageInjectionHost host, TextRange rangeInsideHost) {
    }
}
