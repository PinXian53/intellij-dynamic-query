package com.pino.intellij.dynamicquery;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParenthesizedExpression;
import org.jetbrains.annotations.Nullable;

/**
 * Decides whether a Java string is the value of a dynamic query annotation, and in which dialect
 * it is written.
 *
 * <p>Kept separate from the injector so that supporting another annotation, or renaming an
 * existing one, is a change to {@link DynamicQueryDialect} alone.
 */
public final class DynamicQueryAnnotationDetector {

    /** The attribute carrying the query. {@code null} means the shorthand {@code @A("...")}. */
    private static final String QUERY_ATTRIBUTE = "value";

    private DynamicQueryAnnotationDetector() {
    }

    /** Whether {@code expression} is (part of) the query of a dynamic query annotation. */
    public static boolean isDynamicQuery(@Nullable PsiElement expression) {
        return findDialect(expression) != null;
    }

    /**
     * The dialect {@code expression} is written in, or {@code null} when it is an ordinary string.
     */
    public static @Nullable DynamicQueryDialect findDialect(@Nullable PsiElement expression) {
        return dialectOf(findEnclosingAnnotation(expression));
    }

    /**
     * The dynamic query annotation {@code expression} belongs to, or {@code null} when it is an
     * ordinary string.
     */
    public static @Nullable PsiAnnotation findDynamicQueryAnnotation(@Nullable PsiElement expression) {
        PsiAnnotation annotation = findEnclosingAnnotation(expression);
        return dialectOf(annotation) != null ? annotation : null;
    }

    /** The dialect declared by {@code annotation}, or {@code null} when it declares none. */
    public static @Nullable DynamicQueryDialect dialectOf(@Nullable PsiAnnotation annotation) {
        // getQualifiedName() falls back to the reference text when the annotation cannot be
        // resolved, so matching on the simple name keeps injection working before the import is
        // added.
        return annotation == null ? null : DynamicQueryDialect.forAnnotationName(annotation.getQualifiedName());
    }

    /**
     * Walks up from an expression to the annotation whose query attribute contains it, stepping
     * over concatenations, parentheses and {@code {"a", "b"}} initializers on the way.
     */
    private static @Nullable PsiAnnotation findEnclosingAnnotation(@Nullable PsiElement expression) {
        if (expression == null) {
            return null;
        }

        PsiElement parent = expression.getParent();
        while (parent instanceof PsiExpression
                || parent instanceof PsiParenthesizedExpression
                || parent instanceof PsiArrayInitializerMemberValue) {
            parent = parent.getParent();
        }

        if (!(parent instanceof PsiNameValuePair pair)) {
            return null;
        }
        String attributeName = pair.getName();
        if (attributeName != null && !QUERY_ATTRIBUTE.equals(attributeName)) {
            return null;
        }
        if (!(pair.getParent() instanceof PsiAnnotationParameterList parameterList)) {
            return null;
        }
        return parameterList.getParent() instanceof PsiAnnotation annotation ? annotation : null;
    }
}
