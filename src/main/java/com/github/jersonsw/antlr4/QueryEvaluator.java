package com.github.jersonsw.antlr4;

import com.github.jersonsw.antlr4.exceptions.QueryEvaluationException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.antlr.v4.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QueryEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(QueryEvaluator.class);

    public static Boolean eval(Object input, String query) {

        return eval(new QueryEvaluatorVisitor(input), query);
    }

    public static Boolean eval(QueryEvaluatorVisitor visitor, String query) {
        if (query == null || query.trim().isEmpty()) throw new IllegalArgumentException("Query cannot be null or empty");
        ObjectQLLexer lexer = new ObjectQLLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ObjectQLParser parser = new ObjectQLParser(tokens);

        SyntaxErrorListener errorListener = new SyntaxErrorListener();
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        ObjectQLParser.QueryContext tree = parser.query();

        if (errorListener.hasErrors()) {
            String errorMsg = "Failed to parse query: " + String.join("; ", errorListener.getErrors());
            LOG.error(errorMsg);
            throw new QueryEvaluationException(errorMsg);
        }

        try {
            Object result = tree.accept(visitor);
            if (!(result instanceof Boolean)) {
                String errorMsg = "Query did not return a boolean result: " + result;
                LOG.error(errorMsg);
                throw new QueryEvaluationException(errorMsg);
            }
            LOG.debug("Query '{}' evaluated to: {}", query, result);
            return (Boolean) result;
        } catch (Exception e) {
            String errorMsg = "Error evaluating query '" + query + "': " + e.getMessage();
            LOG.error(errorMsg, e);
            throw new QueryEvaluationException(errorMsg, e);
        }
    }

    private static class SyntaxErrorListener extends BaseErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer, Object offendingSymbol,
                int line, int charPositionInLine, String msg,
                RecognitionException e) {
            errors.add(String.format("Syntax error at line %d:%d - %s", line, charPositionInLine, msg));
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}