package com.github.jersonsw.antlr4;

import com.github.jersonsw.antlr4.exceptions.QueryEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the ObjectQL query evaluation library.
 * <p>
 * Verifies the functionality of {@link QueryEvaluator} and {@link QueryEvaluatorVisitor}
 * across various query types: relational conditions, range checks, collection membership,
 * text matching, logical operations, mathematical expressions, built-in functions,
 * custom functions, null handling, and error cases.
 * </p>
 */
public class QueryEvaluatorTest {
    private Map<String, Object> data; // Test data for query evaluation
    private QueryEvaluatorVisitor visitor; // Visitor instance for custom function tests

    /**
     * Sets up test data and visitor before each test.
     * Initializes a Map with sample properties to test various query scenarios.
     */
    @BeforeEach
    void setUp() {
        data = new HashMap<>();
        data.put("age", 25);              // Numeric property
        data.put("name", "John Doe");     // String property
        data.put("status", "active");     // String property for IN checks
        data.put("scores", new int[]{10, 20, 30}); // Array property
        data.put("isActive", true);       // Boolean property
        data.put("nested", Map.of("value", 42)); // Nested object
        data.put("missing", null);        // Null property for null handling
        data.put("text", "Hello World");  // String property for text functions
        visitor = new QueryEvaluatorVisitor(data);
    }

    /**
     * Tests relational conditions with numeric comparisons.
     */
    @Test
    void testQueryWithRelationalCondition() {
        assertThat(eval("age > 20")).isTrue();         // Greater than
        assertThat(eval("age <= 25")).isTrue();        // Less than or equal
        assertThat(eval("age == 25")).isTrue();        // Equal
        assertThat(eval("age != 30")).isTrue();        // Not equal
        assertThat(eval("age < 10")).isFalse();        // Less than (false case)
    }

    /**
     * Tests BETWEEN conditions for numeric range checks.
     */
    @Test
    void testQueryWithBetweenCondition() {
        assertThat(eval("age >=< [18, 65]")).isTrue(); // Within range
        assertThat(eval("age >=< [26, 65]")).isFalse(); // Below range
        assertThat(eval("missing >=< [10, 20]")).isFalse(); // Null handling
        assertThat(eval("name >=< [18, 65]")).isFalse(); // Non-numeric returns false
    }

    /**
     * Tests IN and NOT_IN conditions for collection membership.
     */
    @Test
    void testQueryWithInCondition() {
        assertThat(eval("status >+< ['active', 'pending']")).isTrue(); // IN true
        assertThat(eval("status <> ['inactive', 'pending']")).isTrue(); // NOT_IN true
        assertThat(eval("age >+< [10, 25, 30]")).isTrue(); // Numeric IN
        assertThat(eval("age <> [1, 2, 3]")).isTrue(); // Numeric NOT_IN
        assertThat(eval("missing >+< [1, 2, 3]")).isFalse(); // Null handling
    }

    /**
     * Tests text matching conditions with pattern and equality operators.
     */
    @Test
    void testQueryWithTextMatchCondition() {
        assertThat(eval("name ~ 'John*'")).isTrue();    // LIKE
        assertThat(eval("name ~~ 'john*'")).isTrue();   // ILIKE
        assertThat(eval("name !~ 'Jane*'")).isTrue();   // NOT_LIKE
        assertThat(eval("name !~~ 'JANE*'")).isTrue();  // NOT_ILIKE
        assertThat(eval("name == 'John Doe'")).isTrue(); // Equal
        assertThat(eval("name != 'Jane Doe'")).isTrue(); // Not equal
        assertThat(eval("missing ~ 'test'")).isFalse();  // Null handling
    }

    /**
     * Tests boolean comparisons.
     */
    @Test
    void testQueryWithBooleanCondition() {
        assertThat(eval("isActive == true")).isTrue();   // Equal true
        assertThat(eval("isActive != false")).isTrue();  // Not equal false
        assertThat(eval("isActive == false")).isFalse(); // Equal false
    }

    /**
     * Tests logical operators (AND, OR) with nested conditions.
     */
    @Test
    void testQueryWithLogicalOperators() {
        assertThat(eval("age > 20 AND name ~ 'John*'")).isTrue(); // AND true
        assertThat(eval("age < 20 OR status == 'active'")).isTrue(); // OR true
        assertThat(eval("(age > 30 OR name == 'Jane Doe') AND isActive")).isFalse(); // Nested false
        assertThat(eval("age > 20 AND missing == 10")).isFalse(); // Null handling
    }

    /**
     * Tests mathematical expressions with arithmetic operators.
     */
    @Test
    void testQueryWithMathExpression() {
        assertThat(eval("age + 5 > 29")).isTrue();       // Addition
        assertThat(eval("nested.value * 2 == 84")).isTrue(); // Multiplication
        assertThat(eval("10 - 5 == 5")).isTrue();        // Subtraction
        assertThat(eval("age % 10 == 5")).isTrue();      // Modulus
        assertThat(eval("2 ^ 3 == 8")).isTrue();         // Power
        assertThat(eval("missing + 5 == 5")).isFalse();  // Null handling
    }

    /**
     * Tests built-in functions provided by the visitor.
     */
    @Test
    void testBuiltInFunctions() {
        assertThat(eval("replace(name, 'Doe', 'Smith') == 'John Smith'")).isTrue();
        assertThat(eval("min(10, 20, 30) == 10")).isTrue();
        assertThat(eval("max(10, 20, 30) == 30")).isTrue();
        assertThat(eval("abs(-5) == 5")).isTrue();
        assertThat(eval("length(name) == 8")).isTrue();
        assertThat(eval("upper(name) == 'JOHN DOE'")).isTrue();
        assertThat(eval("lower(name) == 'john doe'")).isTrue();
        assertThat(eval("substring(text, 0, 5) == 'Hello'")).isTrue();
        assertThat(eval("contains(text, 'World')")).isTrue();
        assertThat(eval("startsWith(text, 'Hello')")).isTrue();
        assertThat(eval("endsWith(text, 'World')")).isTrue();
        assertThat(eval("round(3.7) == 4")).isTrue();
        assertThat(eval("ceil(3.2) == 4")).isTrue();
        assertThat(eval("floor(3.7) == 3")).isTrue();
        assertThat(eval("sqrt(16) == 4")).isTrue();
        assertThat(eval("concat('Hello', ' ', 'World') == 'Hello World'")).isTrue();
        assertThat(eval("concat('Hello', ' ', 'World') == null")).isFalse();
        assertThat(eval("replace(missing, 'a', 'b') == null")).isTrue();
    }

    /**
     * Tests registration and execution of a custom function.
     */
    @Test
    void testCustomFunction() {
        visitor.registerFunction("double", args -> {
            if (args.length != 1) throw new IllegalArgumentException("double requires 1 argument");
            if (args[0] == null) return null;
            return ((Number) args[0]).doubleValue() * 2;
        });
        assertThat(QueryEvaluator.eval(data, "double(5) == 10.0", visitor)).isTrue();
        assertThat(QueryEvaluator.eval(data, "double(age) == 50", visitor)).isTrue();
        assertThatThrownBy(() -> QueryEvaluator.eval(data, "double(1, 2)", visitor))
                .isInstanceOf(QueryEvaluationException.class)
                .hasMessageContaining("Error evaluating query")
                .hasCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Function execution failed: double");
    }

    /**
     * Tests handling of null values in various conditions.
     */
    @Test
    void testNullHandling() {
        assertThat(eval("missing > 10")).isFalse();
        assertThat(eval("missing ~ 'test'")).isFalse();
        assertThat(eval("missing + 5 == 5")).isFalse();
    }

    /**
     * Tests error handling for invalid queries.
     */
    @Test
    void testInvalidQueries() {
        assertThatThrownBy(() -> eval("age >< 10"))
                .isInstanceOf(QueryEvaluationException.class)
                .hasMessageContaining("Failed to parse query: Syntax error at line");

        assertThatThrownBy(() -> eval("unknown(5)"))
                .isInstanceOf(QueryEvaluationException.class)
                .hasMessageContaining("Error evaluating query")
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unknown function");

        assertThatThrownBy(() -> eval(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Query cannot be null or empty");
    }

    /**
     * Tests access to nested properties.
     */
    @Test
    void testNestedPropertyAccess() {
        assertThat(eval("nested.value >= 40")).isTrue();
        assertThat(eval("nested.value + 10 == 52")).isTrue();
        assertThat(eval("nested.missing == 10")).isFalse();
    }

    /**
     * Tests array indexing in property paths.
     */
    @Test
    void testArrayAccess() {
        assertThat(eval("scores[1] == 20")).isTrue();
        assertThat(eval("scores[0] + 10 == 20")).isTrue();
    }

    /**
     * Helper method to evaluate a query using the default visitor.
     *
     * @param query The ObjectQL query string.
     * @return Boolean result of the query.
     */
    private Boolean eval(String query) {
        return QueryEvaluator.eval(data, query);
    }
}