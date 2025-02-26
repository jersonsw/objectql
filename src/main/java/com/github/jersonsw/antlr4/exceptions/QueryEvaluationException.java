package com.github.jersonsw.antlr4.exceptions;

public class QueryEvaluationException extends RuntimeException {
        public QueryEvaluationException(String message) {
            super(message);
        }

        public QueryEvaluationException(String message, Throwable cause) {
            super(message, cause);
        }
}