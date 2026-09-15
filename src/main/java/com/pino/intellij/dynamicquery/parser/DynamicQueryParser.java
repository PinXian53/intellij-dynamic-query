package com.pino.intellij.dynamicquery.parser;

import com.intellij.openapi.util.TextRange;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Builds a {@link DynamicQueryNode} tree out of the token stream produced by
 * {@link DynamicQueryLexer}.
 *
 * <p>The parser is deliberately forgiving: unbalanced brackets are reported as
 * {@link DynamicQueryProblem}s but still produce a usable tree, so that the IDE keeps highlighting
 * the query while it is being typed.
 */
public final class DynamicQueryParser {

    private DynamicQueryParser() {
    }

    public static DynamicQueryParseResult parse(String source) {
        return parse(DynamicQueryLexer.tokenize(source), source.length());
    }

    public static DynamicQueryParseResult parse(List<DynamicQueryToken> tokens, int sourceLength) {
        List<DynamicQueryProblem> problems = new ArrayList<>();
        List<DynamicQueryNode> roots = new ArrayList<>();
        Deque<OpenBlock> stack = new ArrayDeque<>();

        for (DynamicQueryToken token : tokens) {
            List<DynamicQueryNode> target = stack.isEmpty() ? roots : stack.peek().children;

            switch (token.type()) {
                case TEXT -> target.add(new DynamicQueryNode.TextNode(token.range(), token.text()));
                case OPTIONAL_BLOCK_START -> stack.push(new OpenBlock(token.range()));
                case OPTIONAL_BLOCK_END -> {
                    if (stack.isEmpty()) {
                        // Dropped rather than re-emitted as text: a stray ']' must not leak into
                        // the query handed to the parser, or the user would see two errors for
                        // one mistake.
                        problems.add(new DynamicQueryProblem(
                                token.range(),
                                "Unmatched ']': there is no matching '[' in this dynamic query"));
                    } else {
                        OpenBlock open = stack.pop();
                        List<DynamicQueryNode> parent = stack.isEmpty() ? roots : stack.peek().children;
                        parent.add(open.close(token.range()));
                    }
                }
            }
        }

        // Anything still open ends implicitly at the end of the query.
        List<OpenBlock> unclosed = new ArrayList<>(stack);
        for (OpenBlock open : unclosed) {
            problems.add(new DynamicQueryProblem(
                    open.openMarker,
                    "Unclosed '[': this optional predicate is never closed with ']'"));
        }
        while (!stack.isEmpty()) {
            OpenBlock open = stack.pop();
            List<DynamicQueryNode> parent = stack.isEmpty() ? roots : stack.peek().children;
            parent.add(open.closeImplicitly(sourceLength));
        }

        problems.sort(Comparator.comparingInt(problem -> problem.range().getStartOffset()));
        return new DynamicQueryParseResult(List.copyOf(roots), List.copyOf(problems));
    }

    private static final class OpenBlock {
        private final TextRange openMarker;
        private final List<DynamicQueryNode> children = new ArrayList<>();

        private OpenBlock(TextRange openMarker) {
            this.openMarker = openMarker;
        }

        private DynamicQueryNode.OptionalBlock close(TextRange closeMarker) {
            return new DynamicQueryNode.OptionalBlock(
                    new TextRange(openMarker.getStartOffset(), closeMarker.getEndOffset()),
                    openMarker,
                    closeMarker,
                    List.copyOf(children));
        }

        private DynamicQueryNode.OptionalBlock closeImplicitly(int sourceLength) {
            int end = Math.max(openMarker.getEndOffset(), sourceLength);
            return new DynamicQueryNode.OptionalBlock(
                    new TextRange(openMarker.getStartOffset(), end),
                    openMarker,
                    null,
                    List.copyOf(children));
        }
    }
}
