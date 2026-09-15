<div align="center">
    <a href="https://plugins.jetbrains.com/plugin/34291-dynamic-query">
        <img src="./src/main/resources/META-INF/pluginIcon.svg" width="280" height="280" alt="logo"/>
    </a>
</div>

<h1 align="center">Intellij Dynamic Queryp</h1>

<p align="center">
<a href="https://plugins.jetbrains.com/plugin/34291-dynamic-query"><img src="https://img.shields.io/jetbrains/plugin/r/stars/23170?style=flat-square"></a>
<a href="https://plugins.jetbrains.com/plugin/34291-dynamic-query"><img src="https://img.shields.io/jetbrains/plugin/d/34291-dynamic-query.svg?style=flat-square"></a>
<a href="https://plugins.jetbrains.com/plugin/34291-dynamic-query"><img src="https://img.shields.io/jetbrains/plugin/v/34291-dynamic-query.svg?style=flat-square"></a>
</p>

<br>

> Jetbrains Marketplace: https://plugins.jetbrains.com/plugin/34291-dynamic-query

IDE support for dynamic queries, where `[ ... ]` marks an optional predicate that the runtime
keeps or drops depending on whether its parameters are bound.

| Annotation            | Injected language | Provided by               |
|-----------------------|-------------------|---------------------------|
| `@DynamicJpqlQuery`   | `JPAQL`           | `com.intellij.javaee.jpa` |
| `@DynamicNativeQuery` | `SQL`             | `com.intellij.database`   |

```java
@DynamicJpqlQuery("""
        SELECT u FROM UserEntity u
        WHERE 1 = 1
        [ AND u.name = :name ]
        [ AND u.age = :age ]
        [ AND u.roleId IN :roleIds ]
        ORDER BY u.createdAt DESC
        """)
```

No `@Language("JPAQL")` is needed, `[` and `]` are not syntax errors, and JPQL highlighting,
entity/field resolution, completion and parameter inspections all keep working.

## How it works

The plugin is a thin adapter — it does **not** reimplement JPQL or SQL support:

```
Dynamic query ──► preprocessor ──► plain query ──► IntelliJ JPQL / SQL parser
```

The `[ ]` markers are removed by *not injecting them*. Instead of one injection shred covering the
whole string literal, `DynamicQueryLanguageInjector` registers one shred per run of plain query
text and skips the two bracket characters:

```
[ AND u.name = :name ][ AND u.age = :age ]
 └───── shred 1 ────┘  └──── shred 2 ───┘
```

Because the platform stitches the shreds together itself, offset mapping comes for free: an error
the JPQL parser reports inside the injected fragment is highlighted on the right character of the
original Java string, with no manual translation. `DynamicQueryProcessedResult` still exposes the
mapping (`toSourceOffset` / `toSourceRange`) for code that needs it outside the injection.

### Components

| Class                             | Responsibility |
| --------------------------------- | -------------- |
| `DynamicQueryDialect`             | Annotation ⇄ language pairing. Add a dialect here and nothing else changes. |
| `DynamicQueryAnnotationDetector`  | Is this Java string a dynamic query, and in which dialect? |
| `DynamicQueryLexer` / `DynamicQueryParser` | Tokenise and parse the `[ ... ]` syntax into a tree. |
| `DynamicQueryPreprocessor`        | Flatten the tree into plain query text plus `OffsetMapping`s. |
| `DynamicQueryPsiUtils`            | Literal content ranges, string-concatenation handling. |
| `DynamicQueryLanguageInjector`    | `MultiHostInjector` that turns each mapping into an injection shred. |
| `DynamicQuerySyntaxAnnotator`     | Reports unbalanced `[` / `]`. |

### What it deliberately handles

- Text blocks and ordinary string literals, including `"a" + "b"` concatenation.
- Nested optional blocks: `[ AND (u.a = :a [ OR u.b = :b ]) ]`.
- Brackets inside string literals: `WHERE u.code LIKE '[%'` is not a marker.
- Unbalanced brackets: reported as an error, while the rest of the query stays highlighted.

The plugin never looks at runtime parameter values — which branch survives is the runtime's
decision, and the IDE has to check *every* branch.

## Building

Requires JDK 25 (the JBR shipped with IntelliJ IDEA 2026.2 works) and IntelliJ IDEA **Ultimate**,
since JPQL and SQL support are Ultimate-only.

```sh
./gradlew build          # compile + tests
./gradlew buildPlugin    # build/distributions/intellij-dynamic-query-<version>.zip
./gradlew runIde         # launch a sandbox IDE with the plugin installed
```

Install the zip via *Settings → Plugins → ⚙ → Install Plugin from Disk…*.

### Configuration

`gradle.properties`:

- `localIdePath` — build against a locally installed IDE (default) instead of downloading one.
  Clear it to fall back to `platformType` / `platformVersion`.
- `pluginSinceBuild` / `javaVersion` — must match the platform being built against: `262` / `25`
  for 2026.2, `243` / `21` for 2024.3.
- `pluginGroup` / `pluginVersion` — coordinates of the built plugin.

Both dialect dependencies are declared `optional`, so the plugin still loads in an IDE that has
only one of the JPA and Database plugins and simply injects the dialect it can.

## Runtime side

This plugin only covers the IDE. Stripping the markers at runtime and deciding which predicates
survive is the job of the `@DynamicJpqlQuery` / `@DynamicNativeQuery` runtime, which is a separate
artifact. The two share only the `[ ... ]` syntax definition.

For a quick manual check, the annotations the plugin recognises are matched by simple name, so any
declaration works:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DynamicJpqlQuery {
    String value();
}
```