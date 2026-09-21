package com.pino.intellij.dynamicquery;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParenthesizedExpression;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Decides whether a Java string is a query of a {@code @DynamicQuery} annotation — its
 * {@code value} or its {@code countQuery} — and in which dialect it is written.
 *
 * <p>Kept separate from the injector so that recognising the annotation, and reading the
 * {@code nativeQuery} switch that picks the dialect, stays in one place.
 */
public final class DynamicQueryAnnotationDetector {

    /** Simple name of the annotation whose queries this plugin supports. */
    public static final String ANNOTATION_SIMPLE_NAME = "DynamicQuery";

    /** The attribute choosing between JPQL and native SQL. */
    private static final String NATIVE_QUERY_ATTRIBUTE = "nativeQuery";

    /**
     * The attributes carrying a query.
     *
     * <p>{@code value} is the query itself and {@code countQuery} the optional total-count query;
     * both are written in the same dialect, so both get the same injection and the same
     * {@code [ ... ]} handling. An unnamed pair is the shorthand {@code @DynamicQuery("...")},
     * i.e. {@code value}.
     */
    private static final Set<String> QUERY_ATTRIBUTES = Set.of("value", "countQuery");

    private DynamicQueryAnnotationDetector() {
    }

    /** Whether an annotation attribute of this name carries a query. */
    public static boolean isQueryAttribute(@Nullable String attributeName) {
        // A pair with no name is the shorthand for `value`.
        return attributeName == null || QUERY_ATTRIBUTES.contains(attributeName);
    }

    /**
     * Whether an annotation name, qualified or simple, is {@code @DynamicQuery}.
     *
     * <p>Matching on the simple name keeps injection working before the import is added, since an
     * unresolved annotation reports its reference text as its qualified name.
     */
    public static boolean isDynamicQueryAnnotationName(@Nullable String annotationName) {
        if (annotationName == null) {
            return false;
        }
        String simpleName = annotationName.substring(annotationName.lastIndexOf('.') + 1);
        return ANNOTATION_SIMPLE_NAME.equals(simpleName);
    }

    /** Whether {@code expression} is (part of) the query of a {@code @DynamicQuery}. */
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
     * The {@code @DynamicQuery} annotation {@code expression} belongs to, or {@code null} when it
     * is an ordinary string.
     */
    public static @Nullable PsiAnnotation findDynamicQueryAnnotation(@Nullable PsiElement expression) {
        PsiAnnotation annotation = findEnclosingAnnotation(expression);
        return dialectOf(annotation) != null ? annotation : null;
    }

    /**
     * The dialect declared by {@code annotation}, or {@code null} when it is not a
     * {@code @DynamicQuery}.
     */
    public static @Nullable DynamicQueryDialect dialectOf(@Nullable PsiAnnotation annotation) {
        if (annotation == null || !isDynamicQueryAnnotationName(annotation.getQualifiedName())) {
            return null;
        }
        return DynamicQueryDialect.forNativeQuery(isNativeQuery(annotation));
    }

    /**
     * The value of {@code nativeQuery}, which defaults to {@code false} — both when the attribute
     * is omitted and when its value cannot be read as a constant.
     */
    private static boolean isNativeQuery(PsiAnnotation annotation) {
        // findDeclaredAttributeValue rather than findAttributeValue: the latter needs the
        // annotation to resolve before it can report the default, and injection has to work
        // before the import is added.
        PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue(NATIVE_QUERY_ATTRIBUTE);
        if (value == null) {
            return false;
        }
        if (value instanceof PsiExpression expression) {
            Object constant = JavaPsiFacade.getInstance(expression.getProject())
                    .getConstantEvaluationHelper()
                    .computeConstantExpression(expression);
            if (constant instanceof Boolean nativeQuery) {
                return nativeQuery;
            }
        }
        // A value that is not a compile-time constant here (an unresolved reference, say): the
        // text is the best guess left.
        return "true".equals(value.getText());
    }

    /**
     * Walks up from an expression to the annotation whose query attribute ({@code value} or
     * {@code countQuery}) contains it, stepping over concatenations, parentheses and
     * {@code {"a", "b"}} initializers on the way.
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
        if (!isQueryAttribute(pair.getName())) {
            return null;
        }
        if (!(pair.getParent() instanceof PsiAnnotationParameterList parameterList)) {
            return null;
        }
        return parameterList.getParent() instanceof PsiAnnotation annotation ? annotation : null;
    }
}
