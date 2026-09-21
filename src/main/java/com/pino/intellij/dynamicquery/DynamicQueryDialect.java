package com.pino.intellij.dynamicquery;

import com.intellij.lang.Language;
import org.jetbrains.annotations.Nullable;

/**
 * The query dialects that support the dynamic {@code [ ... ]} syntax.
 *
 * <p>A single annotation — {@code @DynamicQuery} — carries both: its {@code nativeQuery} attribute
 * decides which one its query is written in. The optional-predicate syntax is identical in both;
 * only the language handed to the platform differs, which is why everything else in this plugin is
 * dialect-agnostic.
 */
public enum DynamicQueryDialect {

    /** {@code nativeQuery = false} (the default) — JPQL, parsed by the bundled JPA plugin. */
    JPQL("JPAQL"),

    /** {@code nativeQuery = true} — native SQL, parsed by the bundled Database plugin. */
    NATIVE_SQL("SQL"),
    ;

    private final String languageId;

    DynamicQueryDialect(String languageId) {
        this.languageId = languageId;
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

    /** The dialect declared by a {@code nativeQuery} value. */
    public static DynamicQueryDialect forNativeQuery(boolean nativeQuery) {
        return nativeQuery ? NATIVE_SQL : JPQL;
    }
}
