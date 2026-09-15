package com.pino.intellij.dynamicquery;

import com.intellij.lang.Language;
import org.jetbrains.annotations.Nullable;

/**
 * The query dialects that support the dynamic {@code [ ... ]} syntax.
 *
 * <p>Each dialect pairs an annotation with the IntelliJ language its value is written in. The
 * optional-predicate syntax is identical in both; only the language handed to the platform
 * differs, which is why everything else in this plugin is dialect-agnostic.
 *
 * <p>Adding a third dialect is a matter of adding a constant here.
 */
public enum DynamicQueryDialect {

    /** {@code @DynamicJpqlQuery} — JPQL, parsed by the bundled JPA plugin. */
    JPQL("DynamicJpqlQuery", "JPAQL"),

    /** {@code @DynamicNativeQuery} — native SQL, parsed by the bundled Database plugin. */
    NATIVE_SQL("DynamicNativeQuery", "SQL"),
    ;

    private final String annotationSimpleName;
    private final String languageId;

    DynamicQueryDialect(String annotationSimpleName, String languageId) {
        this.annotationSimpleName = annotationSimpleName;
        this.languageId = languageId;
    }

    /** Simple name of the annotation whose value is written in this dialect. */
    public String annotationSimpleName() {
        return annotationSimpleName;
    }

    /** Id of the IntelliJ language to inject, e.g. {@code JPAQL} or {@code SQL}. */
    public String languageId() {
        return languageId;
    }

    /**
     * The language to inject, or {@code null} when the IDE has no support for it.
     *
     * <p>Resolved by id rather than by a direct class reference, so a missing dialect just means
     * no injection instead of a plugin that fails to load.
     */
    public @Nullable Language findLanguage() {
        return Language.findLanguageByID(languageId);
    }

    /**
     * The dialect declared by an annotation name, which may be qualified or simple, or
     * {@code null} when the annotation is not a dynamic query annotation.
     */
    public static @Nullable DynamicQueryDialect forAnnotationName(@Nullable String annotationName) {
        if (annotationName == null) {
            return null;
        }
        String simpleName = annotationName.substring(annotationName.lastIndexOf('.') + 1);
        for (DynamicQueryDialect dialect : values()) {
            if (dialect.annotationSimpleName.equals(simpleName)) {
                return dialect;
            }
        }
        return null;
    }
}
