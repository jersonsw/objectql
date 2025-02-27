package com.github.jersonsw.antlr4;

import com.github.jersonsw.utils.NumberUtils;
import com.google.gson.Gson;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Visitor implementation for evaluating ObjectQL queries against an object.
 * <p>
 * This class processes queries defined by the ObjectQL grammar, evaluating conditions such as
 * logical operations (AND, OR), numeric comparisons (e.g., <, >, ≤, ≥, ==, ≠),
 * text matching, range checks, and user-defined functions. It uses Apache Commons BeanUtils to
 * access nested properties and SLF4J for logging.
 * </p>
 * <p>
 * Built-in functions include string manipulation (e.g., replace, upper), numeric operations
 * (e.g., min, max, sqrt), and text utilities (e.g., contains, startsWith). Users can extend
 * functionality by registering additional custom functions via the {@link #registerFunction(String, Function)}
 * method. All functions are user-defined and identified by simple names without special prefixes.
 * </p>
 *
 * @see ObjectQLParser
 * @see QueryEvaluator
 */
public class QueryEvaluatorVisitor implements ObjectQLVisitor<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(QueryEvaluatorVisitor.class);
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[\\d+]"); // Simpler pattern to detect indexing
    private final Object obj; // The object to evaluate queries against
    private final Map<String, Pattern> patternCache = new HashMap<>(); // Cache for regex patterns in text matching
    private final Map<String, Function<Object[], Object>> functionRegistry = new HashMap<>(); // Registry for user-defined functions

    private static final Gson GSON = new Gson();

    public QueryEvaluatorVisitor(String obj){
        this(castInput(obj));
    }

    /**
     * Constructs a new visitor with the target object to evaluate queries against.
     * Initializes a comprehensive set of built-in functions for common operations.
     *
     * @param input The object to query (e.g., Map, POJO, or deserialized JSON). Can be null,
     *            in which case property lookups will return null.
     */
    public QueryEvaluatorVisitor(Object input) {
        if (input == null) throw new IllegalArgumentException("Input cannot be null");

        this.obj = input;
        // Register built-in functions for string manipulation
        registerFunction("replace", args -> {
            if (args.length != 3)
                throw new IllegalArgumentException("replace requires 3 arguments: string, target, replacement");
            if (args[0] == null || args[1] == null || args[2] == null) return null;
            return ((String) args[0]).replaceAll((String) args[1], (String) args[2]);
        });
        registerFunction("upper", args -> {
            if (args.length != 1) throw new IllegalArgumentException("upper requires 1 argument: string");
            if (args[0] == null) return null;
            return ((String) args[0]).toUpperCase();
        });
        registerFunction("lower", args -> {
            if (args.length != 1) throw new IllegalArgumentException("lower requires 1 argument: string");
            if (args[0] == null) return null;
            return ((String) args[0]).toLowerCase();
        });
        registerFunction("substring", args -> {
            if (args.length < 2 || args.length > 3)
                throw new IllegalArgumentException("substring requires 2 or 3 arguments: string, start, [end]");
            if (args[0] == null || args[1] == null) return null;
            String str = (String) args[0];
            int start = ((Number) args[1]).intValue();
            int end = args.length == 3 && args[2] != null ? ((Number) args[2]).intValue() : str.length();
            return str.substring(start, Math.min(end, str.length()));
        });
        registerFunction("concat", args -> {
            if (args.length < 1) throw new IllegalArgumentException("concat requires at least 1 argument: strings");
            return Arrays.stream(args).filter(Objects::nonNull)
                    .map(Object::toString).reduce("", String::concat);
        });

        // Register built-in functions for numeric operations
        registerFunction("min", args -> {
            if (args.length < 1) throw new IllegalArgumentException("min requires at least 1 argument: numbers");
            return Arrays.stream(args).filter(Objects::nonNull)
                    .mapToDouble(a -> ((Number) a).doubleValue()).min()
                    .orElseThrow(() -> new IllegalArgumentException("No valid numbers provided to min"));
        });
        registerFunction("max", args -> {
            if (args.length < 1) throw new IllegalArgumentException("max requires at least 1 argument: numbers");
            return Arrays.stream(args).filter(Objects::nonNull)
                    .mapToDouble(a -> ((Number) a).doubleValue()).max()
                    .orElseThrow(() -> new IllegalArgumentException("No valid numbers provided to max"));
        });
        registerFunction("abs", args -> {
            if (args.length != 1) throw new IllegalArgumentException("abs requires 1 argument: number");
            if (args[0] == null) return null;
            return Math.abs(((Number) args[0]).doubleValue());
        });
        registerFunction("round", args -> {
            if (args.length != 1) throw new IllegalArgumentException("round requires 1 argument: number");
            if (args[0] == null) return null;
            return Math.round(((Number) args[0]).doubleValue());
        });
        registerFunction("ceil", args -> {
            if (args.length != 1) throw new IllegalArgumentException("ceil requires 1 argument: number");
            if (args[0] == null) return null;
            return Math.ceil(((Number) args[0]).doubleValue());
        });
        registerFunction("floor", args -> {
            if (args.length != 1) throw new IllegalArgumentException("floor requires 1 argument: number");
            if (args[0] == null) return null;
            return Math.floor(((Number) args[0]).doubleValue());
        });
        registerFunction("sqrt", args -> {
            if (args.length != 1) throw new IllegalArgumentException("sqrt requires 1 argument: number");
            if (args[0] == null) return null;
            return Math.sqrt(((Number) args[0]).doubleValue());
        });

        // Register built-in functions for text utilities
        registerFunction("length", args -> {
            if (args.length != 1) throw new IllegalArgumentException("length requires 1 argument: string or array");
            if (args[0] == null) return null;
            return args[0] instanceof String ? ((String) args[0]).length() : ((Object[]) args[0]).length;
        });
        registerFunction("contains", args -> {
            if (args.length != 2)
                throw new IllegalArgumentException("contains requires 2 arguments: string, substring");
            if (args[0] == null || args[1] == null) return false;
            return ((String) args[0]).contains((String) args[1]);
        });
        registerFunction("startsWith", args -> {
            if (args.length != 2) throw new IllegalArgumentException("startsWith requires 2 arguments: string, prefix");
            if (args[0] == null || args[1] == null) return false;
            return ((String) args[0]).startsWith((String) args[1]);
        });
        registerFunction("endsWith", args -> {
            if (args.length != 2) throw new IllegalArgumentException("endsWith requires 2 arguments: string, suffix");
            if (args[0] == null || args[1] == null) return false;
            return ((String) args[0]).endsWith((String) args[1]);
        });
    }

    /**
     * Registers a custom function for use in queries.
     * <p>
     * Allows users to extend the query language with their own functions. The function name must be a valid
     * identifier as per the ObjectQL grammar, and the implementation must handle an array of arguments,
     * returning a result compatible with the query context (e.g., Number, String, Boolean).
     * </p>
     * <p>
     * Example:
     * <pre>
     * visitor.registerFunction("double", args -> ((Number) args[0]).doubleValue() * 2);
     * </pre>
     * This enables queries like "double(age) > 50" to double the value of age and compare it.
     * </p>
     *
     * @param name The function name (e.g., "double"). Must not be null or empty.
     * @param fn   The function implementation accepting an array of arguments. Must not be null.
     * @throws IllegalArgumentException if name or fn is null or if name is empty.
     */
    public void registerFunction(String name, Function<Object[], Object> fn) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Function name must not be null or empty");
        }
        if (fn == null) {
            throw new IllegalArgumentException("Function implementation must not be null");
        }
        functionRegistry.put(name, fn);
        LOG.debug("Registered function: {}", name);
    }

    /**
     * Evaluates the top-level query rule, initiating the parsing process.
     * <p>
     * This is the entry point for query evaluation, delegating to the predication rule.
     * </p>
     *
     * @param ctx The query context from the parser, representing the full query.
     * @return Boolean result of the query evaluation.
     * @throws IllegalArgumentException if the query is invalid or lacks a predication.
     */
    @Override
    public Boolean visitQuery(ObjectQLParser.QueryContext ctx) {
        LOG.debug("Evaluating query: {}", ctx.getText());
        ObjectQLParser.PredicationContext predication = ctx.predication();
        if (predication != null) {
            return (Boolean) predication.accept(this);
        }
        throw new IllegalArgumentException("Query is invalid or empty");
    }

    /**
     * Evaluates a predication, combining conditions with logical operators (AND/OR).
     * <p>
     * Handles nested predications and short-circuits null values to false for safe evaluation.
     * Supports recursive evaluation of AND and OR operations.
     * </p>
     *
     * @param ctx The predication context from the parser.
     * @return Boolean result of the predication evaluation, or false if null.
     */
    @Override
    public Boolean visitPredication(ObjectQLParser.PredicationContext ctx) {
        LOG.debug("Evaluating predication: {}", ctx.getText());
        // Base case: single condition
        if (ctx.condition() != null) {
            return (Boolean) ctx.condition().accept(this);
        }
        ObjectQLParser.PredicationContext predication = ctx.predication();
        if (predication == null) return false; // No predication to evaluate

        Boolean left = (Boolean) predication.accept(this);
        if (left == null) {
            return false; // Null-safe fallback
        }
        // Combine with AND or OR if present
        if (ctx.andNode() != null) {
            Boolean right = (Boolean) ctx.andNode().accept(this);
            return left && (right != null ? right : false);
        }
        if (ctx.orNode() != null) {
            Boolean right = (Boolean) ctx.orNode().accept(this);
            return left || (right != null ? right : false);
        }
        return left;
    }

    /**
     * Evaluates an AND node, combining the left predication with another via logical AND.
     * <p>
     * Recursively evaluates the right-hand predication and applies the AND operation.
     * </p>
     *
     * @param ctx The AND node context from the parser.
     * @return Boolean result of the AND operation.
     * @throws IllegalArgumentException if the right predication is invalid or missing.
     */
    @Override
    public Boolean visitAndNode(ObjectQLParser.AndNodeContext ctx) {
        if (ctx.predication() == null || ctx.predication().isEmpty()) {
            throw new IllegalArgumentException("Invalid AND condition: " + ctx.getText());
        }
        return (Boolean) ctx.predication().accept(this);
    }

    /**
     * Evaluates an OR node, combining the left predication with another via logical OR.
     * <p>
     * Recursively evaluates the right-hand predication and applies the OR operation.
     * </p>
     *
     * @param ctx The OR node context from the parser.
     * @return Boolean result of the OR operation.
     * @throws IllegalArgumentException if the right predication is invalid or missing.
     */
    @Override
    public Boolean visitOrNode(ObjectQLParser.OrNodeContext ctx) {
        if (ctx.predication() == null || ctx.predication().isEmpty()) {
            throw new IllegalArgumentException("Invalid OR condition: " + ctx.getText());
        }
        return (Boolean) ctx.predication().accept(this);
    }

    /**
     * Evaluates a single condition, delegating to specific condition types.
     * <p>
     * Supports range checks (BETWEEN), membership tests (IN/NOT_IN), numeric comparisons
     * (<, >, ≤, ≥, ==, ≠), text matching, boolean literals, and function calls.
     * Each condition type is processed by its respective visitor method.
     * </p>
     *
     * @param ctx The condition context from the parser.
     * @return Boolean result of the condition evaluation.
     * @throws IllegalArgumentException if the condition type is unrecognized.
     */
    @Override
    public Boolean visitCondition(ObjectQLParser.ConditionContext ctx) {
        LOG.debug("Evaluating condition: {}", ctx.getText());
        if (ctx.btw != null) return (Boolean) ctx.btw.accept(this); // Range check
        if (ctx.in != null) return (Boolean) ctx.in.accept(this);   // Membership test
        if (ctx.rel != null) return (Boolean) ctx.rel.accept(this); // Comparison
        if (ctx.match != null) return (Boolean) ctx.match.accept(this); // Text matching
        if (ctx.bool() != null) return (Boolean) ctx.bool().accept(this);   // Boolean literal
        if (ctx.fn != null) return (Boolean) ctx.fn.accept(this);       // Function call
        throw new IllegalArgumentException("Unrecognized condition: " + ctx.getText());
    }

    /**
     * Evaluates a list of arguments for a function call.
     * <p>
     * Each argument is processed recursively, and the results are collected into an array for
     * passing to the function implementation.
     * </p>
     *
     * @param ctx The args context from the parser.
     * @return Array of evaluated argument values (e.g., Number, String).
     */
    @Override
    public Object[] visitArgs(ObjectQLParser.ArgsContext ctx) {
        return ctx.arg().stream().map(arg -> arg.accept(this)).toArray();
    }

    /**
     * Evaluates a single function argument, resolving to its value.
     * <p>
     * Arguments can be identifiers (property paths), text expressions, or mathematical expressions,
     * each processed by their respective visitor methods.
     * </p>
     *
     * @param ctx The arg context from the parser.
     * @return The evaluated argument value (e.g., Number, String).
     * @throws IllegalArgumentException if the argument type is invalid.
     */
    @Override
    public Object visitArg(ObjectQLParser.ArgContext ctx) {
        if (ctx.idtfr != null) return ctx.idtfr.accept(this); // Property value
        if (ctx.txt != null) return ctx.txt.accept(this);     // Text literal or expression
        if (ctx.math != null) return ctx.math.accept(this);   // Numeric expression
        throw new IllegalArgumentException("Invalid function argument: " + ctx.getText());
    }

    /**
     * Evaluates a BETWEEN condition to check if a value falls within a numeric range (inclusive).
     * <p>
     * Returns false if any operand (value, from, to) is null, ensuring safe evaluation without
     * NullPointerExceptions. Throws an exception if operands are not numeric.
     * </p>
     *
     * @param ctx The betweenCond context from the parser.
     * @return True if the value is within the range (inclusive), false otherwise (including null cases).
     * @throws IllegalArgumentException if the value, from, or to operands are not numeric.
     */
    @Override
    public Boolean visitBetweenCond(ObjectQLParser.BetweenCondContext ctx) {
        Object valObj = ctx.val.accept(this);
        Object fromObj = ctx.from.accept(this);
        Object toObj = ctx.to.accept(this);

        if (valObj == null || fromObj == null || toObj == null) return false; // Null-safe handling
        if (!(valObj instanceof Number val) || !(fromObj instanceof Number from) || !(toObj instanceof Number to)) {
            throw new IllegalArgumentException("BETWEEN requires numeric values: " + ctx.getText());
        }
        double valD = val.doubleValue();
        double fromD = from.doubleValue();
        double toD = to.doubleValue();
        return valD >= fromD && valD <= toD;
    }

    /**
     * Evaluates an IN or NOT_IN condition to test collection membership.
     * <p>
     * Supports both numeric and text collections. Returns false if the left-hand side or collection
     * is null, ensuring safe evaluation. The collection must be an array or list of compatible types.
     * </p>
     *
     * @param ctx The inCond context from the parser.
     * @return True if the condition is satisfied (IN: present, NOT_IN: absent), false otherwise (including null cases).
     * @throws IllegalArgumentException if the collection type is invalid or operands mismatch.
     */
    @Override
    public Boolean visitInCond(ObjectQLParser.InCondContext ctx) {
        if (ctx.lhsNum != null) { // Numeric IN/NOT_IN
            Object lhsObj = ctx.lhsNum.accept(this);
            if (lhsObj == null) return false;
            if (!(lhsObj instanceof Number lhs)) {
                throw new IllegalArgumentException("IN lhs must be numeric: " + ctx.getText());
            }
            Object[] rhs = (Object[]) ctx.rhsNum.accept(this);
            if (rhs == null) return false;
            Set<Number> rhsSet = new HashSet<>(Arrays.asList(Arrays.copyOf(rhs, rhs.length, Number[].class)));
            boolean isPresent = rhsSet.contains(lhs);
            return (ctx.IN() != null) == isPresent;
        }
        if (ctx.lhsText != null || ctx.lhs != null) { // Text IN/NOT_IN
            Object lhsObj = ctx.lhsText != null ? ctx.lhsText.accept(this) : ctx.lhs.accept(this);
            if (lhsObj == null) return false;

            String lhs = lhsObj.toString();
            Object rhsObj = ctx.lhsText != null ? ctx.rhsText.accept(this) : ctx.rhs.accept(this);
            if (rhsObj == null) return false;

            Set<Object> rhsSet;
            if (rhsObj instanceof String[]) {
                rhsSet = new HashSet<>(Arrays.asList((String[]) rhsObj));
            } else if (rhsObj instanceof Object[]) {
                rhsSet = Arrays.stream(((Object[]) rhsObj))
                        .map(Object::toString)
                        .collect(Collectors.toSet());
            } else {
                throw new IllegalArgumentException("IN/NOT_IN rhs must be a collection: " + ctx.getText());
            }

            boolean isPresent = rhsSet.contains(lhs);

            return (ctx.IN() != null) == isPresent;
        }
        throw new IllegalArgumentException("Invalid IN condition: " + ctx.getText());
    }

    /**
     * Evaluates a relational condition, comparing numeric or boolean values.
     * <p>
     * For numeric comparisons, supports <, >, ≤, ≥, ==, ≠ operators. For boolean comparisons,
     * delegates to boolExpr handling. Returns false if either operand is null.
     * </p>
     *
     * @param ctx The relCond context from the parser.
     * @return Boolean result of the comparison.
     * @throws IllegalArgumentException if numeric operands are not Numbers or operator is unknown.
     */
    @Override
    public Boolean visitRelCond(ObjectQLParser.RelCondContext ctx) {
        if (ctx.bool_match != null) return (Boolean) ctx.bool_match.accept(this); // Boolean comparison
        Object lhsObj = ctx.lhs.accept(this);
        Object rhsObj = ctx.rhs.accept(this);

        if (lhsObj == null || rhsObj == null) return false; // Null-safe handling
        if (!(lhsObj instanceof Number lhs) || !(rhsObj instanceof Number rhs)) {
            throw new IllegalArgumentException("Relational operators require numeric values: " + ctx.getText());
        }
        return switch (ctx.opr.getText()) {
            case "==" -> lhs.doubleValue() == rhs.doubleValue();
            case "!=" -> lhs.doubleValue() != rhs.doubleValue();
            case ">" -> lhs.doubleValue() > rhs.doubleValue();
            case ">=" -> lhs.doubleValue() >= rhs.doubleValue();
            case "<" -> lhs.doubleValue() < rhs.doubleValue();
            case "<=" -> lhs.doubleValue() <= rhs.doubleValue();
            default -> throw new IllegalArgumentException("Unknown relational operator: " + ctx.opr.getText());
        };
    }

    /**
     * Evaluates a text matching condition using pattern matching or exact comparison.
     * <p>
     * Supports LIKE (~), ILIKE (~~), NOT_LIKE (!~), NOT_ILIKE (!~~) with wildcards (* for any chars,
     * ? for one char), and equality operators (==, ≠). Returns false if either operand is null.
     * </p>
     *
     * @param ctx The textMatchCond context from the parser.
     * @return Boolean result of the text match evaluation.
     * @throws IllegalArgumentException if the operator is unrecognized.
     */
    @Override
    public Boolean visitTextMatchCond(ObjectQLParser.TextMatchCondContext ctx) {
        Object lhsObj = ctx.lhs.accept(this);
        String opr = ctx.opr.getText();

        if (ctx.nilRhs != null) {
            if (opr.equals("==")) return lhsObj == null;
            if (opr.equals("!=")) return lhsObj != null;
            throw new IllegalArgumentException("Invalid operator is used to compare with a null value: " + opr);
        }

        Object rhsObj = ctx.rhs.accept(this);

        if (lhsObj == null || rhsObj == null) return false; // Null-safe handling

        String lhs = lhsObj.toString();
        String rhs = rhsObj.toString();

        // Cache regex patterns for performance with LIKE operators
        Pattern pattern = patternCache.computeIfAbsent(opr + rhs, k ->
                Pattern.compile(rhs.replace("*", ".*").replace("?", "."),
                        opr.contains("~~") ? Pattern.CASE_INSENSITIVE : 0));
        boolean matches = pattern.matcher(lhs).matches();
        return switch (opr) {
            case "==" -> lhs.equals(rhs);
            case "!=" -> !lhs.equals(rhs);
            case "~", "~~" -> matches; // LIKE and ILIKE (case-insensitive)
            case "!~", "!~~" -> !matches; // NOT_LIKE and NOT_ILIKE (case-insensitive)
            default -> throw new IllegalArgumentException("Unknown text match operator: " + opr);
        };
    }

    /**
     * Evaluates a collection of parameters for IN/NOT_IN conditions.
     * <p>
     * Processes a list of text or numeric expressions, returning them as an array for membership testing.
     * </p>
     *
     * @param ctx The paramsCollection context from the parser.
     * @return Array of evaluated parameter values (e.g., String[], Number[]).
     * @throws IllegalArgumentException if the collection type is invalid (neither text nor numeric).
     */
    @Override
    public Object[] visitParamsCollection(ObjectQLParser.ParamsCollectionContext ctx) {
        if (!ctx.mathExpr().isEmpty()) {
            return ctx.mathExpr().stream().map(expr -> expr.accept(this)).toArray(); // Numeric collection
        }
        if (!ctx.textExpr().isEmpty()) {
            return ctx.textExpr().stream().map(expr -> expr.accept(this)).toArray(); // Text collection
        }
        throw new IllegalArgumentException("Invalid collection: " + ctx.getText());
    }

    /**
     * Evaluates a text expression, resolving it to a string value.
     * <p>
     * Supports function calls (e.g., upper("john")), literal text (e.g., "hello"), property identifiers
     * (e.g., name), and nested expressions. Returns null if the result cannot be resolved.
     * </p>
     *
     * @param ctx The textExpr context from the parser.
     * @return String result of the expression, or null if unresolved.
     * @throws IllegalArgumentException if the expression type is invalid.
     */
    @Override
    public String visitTextExpr(ObjectQLParser.TextExprContext ctx) {
        Object result;
        if (ctx.fn != null) {
            result = ctx.fn.accept(this); // Function call returning a string
        } else if (ctx.txt != null) {
            result = ctx.txt.accept(this); // Literal text
        } else if (ctx.idtfr != null) {
            result = ctx.idtfr.accept(this); // Property value
        } else if (!ctx.textExpr().isEmpty()) {
            result = ctx.textExpr().accept(this); // Nested text expression
        } else {
            throw new IllegalArgumentException("Invalid text expression: " + ctx.getText());
        }
        return result != null ? result.toString() : null;
    }

    /**
     * Evaluates a boolean expression, resolving it to a true/false value.
     * <p>
     * Supports boolean literals (true/false), function calls (e.g., contains(...)), property identifiers
     * (e.g., isActive), comparisons (==, ≠), and nested expressions. Coerces non-boolean values to
     * boolean via parsing if necessary.
     * </p>
     *
     * @param ctx The boolExpr context from the parser.
     * @return Boolean result of the expression.
     * @throws IllegalArgumentException if the expression is invalid or operator is unrecognized.
     */
    @Override
    public Boolean visitBoolExpr(ObjectQLParser.BoolExprContext ctx) {
        if (ctx.bool() != null) {
            return (Boolean) ctx.bool().accept(this); // Literal true/false
        }
        if (ctx.idtfr != null) { // Property resolving to boolean
            Object val = ctx.idtfr.accept(this);
            return val instanceof Boolean ? (Boolean) val : Boolean.parseBoolean(val != null ? val.toString() : "false");
        }
        if (ctx.fn != null) {
            return (Boolean) ctx.fn.accept(this); // Function returning boolean
        }
        if (ctx.lhs != null && ctx.opr != null && ctx.rhs != null) { // Boolean comparison
            Object lhsObj = ctx.lhs.accept(this);
            Object rhsObj = ctx.rhs.accept(this);
            if (lhsObj == null || rhsObj == null) return false;
            Boolean lhs = lhsObj instanceof Boolean ? (Boolean) lhsObj : Boolean.parseBoolean(lhsObj.toString());
            Boolean rhs = rhsObj instanceof Boolean ? (Boolean) rhsObj : Boolean.parseBoolean(rhsObj.toString());
            return switch (ctx.opr.getText()) {
                case "==" -> lhs.equals(rhs);
                case "!=" -> !lhs.equals(rhs);
                default -> throw new IllegalArgumentException("Invalid boolean operator: " + ctx.opr.getText());
            };
        }
        if (ctx.expr != null) {
            return (Boolean) ctx.expr.accept(this); // Nested boolean expression
        }
        throw new IllegalArgumentException("Invalid boolean expression: " + ctx.getText());
    }

    /**
     * Resolves an identifier (property path) to a value from the target object.
     * <p>
     * Uses Apache Commons BeanUtils to access nested properties (e.g., "person.address.street")
     * and array elements (e.g., "scores[0]"). Returns null if the property is not found or inaccessible.
     * </p>
     *
     * @param ctx The identifier context from the parser.
     * @return The resolved value (e.g., Number, String, Boolean), or null if not found.
     * @throws IllegalArgumentException if the identifier is empty or malformed.
     */
    @Override
    public Object visitIdentifier(ObjectQLParser.IdentifierContext ctx) {
        if (ctx.IDENTIFIER().isEmpty()) {
            throw new IllegalArgumentException("Invalid identifier: " + ctx.getText());
        }
        String fieldPath = ctx.getText();

        try {
            // Check if the path contains an index (e.g., "[1]")
            if (INDEX_PATTERN.matcher(fieldPath).find()) {
                Object value = resolveIndexedPath(obj, fieldPath);
                LOG.debug("Resolved indexed identifier {} to {}", fieldPath, value);
                return value;
            } else {
                // Non-indexed path: use PropertyUtils directly
                Object value = PropertyUtils.getProperty(obj, fieldPath);
                System.out.printf("Testing it now: %s -> %s\n", fieldPath, value);
                LOG.debug("Resolved non-indexed identifier {} to {}", fieldPath, value);
                return value;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            LOG.debug("Identifier {} not found: {}", fieldPath, e.getMessage());
            System.out.println(e);
            return null; // Property not found or inaccessible
        } catch (Exception e) {
            LOG.debug("Error resolving identifier {}: {}", fieldPath, e.getMessage());
            throw new IllegalArgumentException("Failed to resolve identifier: " + fieldPath, e);
        }
    }

    /**
     * Resolves a property path with indexing (e.g., "scores[1]") recursively.
     *
     * @param target The object to resolve the path against (e.g., Map, POJO).
     * @param path   The property path (e.g., "scores[1]", "nested.scores[2]").
     * @return The resolved value, or null if not found or out of bounds.
     * @throws Exception if the path is invalid or indexing fails.
     */
    private Object resolveIndexedPath(Object target, String path) throws Exception {
        if (target == null || path == null || path.isEmpty()) {
            return null;
        }

        Pattern fullPattern = Pattern.compile("^(.*?)\\[(\\d+)](.*)$");
        Matcher matcher = fullPattern.matcher(path);
        if (!matcher.matches()) {
            return PropertyUtils.getProperty(target, path); // Shouldn’t happen, but fallback
        }

        String base = matcher.group(1); // e.g., "scores"
        String indexStr = matcher.group(2); // e.g., "1"
        String remainder = matcher.group(3); // e.g., "" or ".field"

        Object current = base.isEmpty() ? target : PropertyUtils.getProperty(target, base);
        if (current == null) {
            return null;
        }

        if (indexStr != null) {
            int index = Integer.parseInt(indexStr);
            switch (current) {
                case Object[] array -> {
                    if (index >= 0 && index < array.length) {
                        current = array[index];
                    } else {
                        return null; // Out of bounds
                    }
                }
                case int[] array -> {
                    if (index >= 0 && index < array.length) {
                        current = array[index]; // Auto-boxed to Integer
                    } else {
                        return null; // Out of bounds
                    }
                }
                case List<?> list -> {
                    if (index >= 0 && index < list.size()) {
                        current = list.get(index);
                    } else {
                        return null; // Out of bounds
                    }
                }
                case null, default ->
                        throw new IllegalArgumentException("Indexed property '" + base + "' is not an array or list");
            }
        }

        if (remainder != null && !remainder.isEmpty()) {
            return resolveIndexedPath(current, remainder.substring(1)); // Recurse for nested paths
        }
        return current;
    }

    /**
     * Evaluates a mathematical expression, resolving it to a numeric value.
     * <p>
     * Supports arithmetic operations (+, -, *, /, ^, %), function calls (e.g., sqrt(16)), property
     * identifiers (e.g., age), numeric literals (e.g., 42), and nested expressions. Preserves integer
     * precision where possible, falling back to double for mixed types or non-integer results.
     * </p>
     *
     * @param ctx The mathExpr context from the parser.
     * @return Number result of the expression (Integer or Double), or null if unresolved.
     * @throws IllegalArgumentException if operands are invalid or operator is unrecognized.
     */
    @Override
    public Number visitMathExpr(ObjectQLParser.MathExprContext ctx) {
        if (ctx.num != null) {
            return (Number) ctx.num.accept(this); // Numeric literal
        }
        if (ctx.idtfr != null) { // Property resolving to a number
            Object val = ctx.idtfr.accept(this);

            if (val == null) return null;
            if (val instanceof Number) return (Number) val;

            LOG.warn("Identifier must resolve to a number: " + ctx.getText());
            return null;
        }
        if (ctx.fn != null) {
            return (Number) ctx.fn.accept(this); // Function returning a number
        }
        if (ctx.lhs != null && ctx.opr != null && ctx.rhs != null) { // Arithmetic operation
            Object lhsObj = ctx.lhs.accept(this);
            Object rhsObj = ctx.rhs.accept(this);
            if (lhsObj == null || rhsObj == null) return null;
            if (!(lhsObj instanceof Number lhs) || !(rhsObj instanceof Number rhs)) {
                throw new IllegalArgumentException("Arithmetic requires numeric operands: " + ctx.getText());
            }
            String opr = ctx.opr.getText();

            return switch (opr) {
                case "+" -> NumberUtils.sum(lhs, rhs);
                case "-" -> NumberUtils.subtract(lhs, rhs);
                case "*" -> NumberUtils.multiply(lhs, rhs);
                case "/" -> NumberUtils.divide(lhs, rhs);
                case "%" -> NumberUtils.mod(lhs, rhs);
                case "^" -> NumberUtils.pow(lhs, rhs);
                default -> throw new IllegalArgumentException("Unknown arithmetic operator: " + opr);
            };
        }
        if (ctx.expr != null) {
            return (Number) ctx.expr.accept(this); // Nested math expression
        }
        throw new IllegalArgumentException("Invalid math expression: " + ctx.getText());
    }

    /**
     * Evaluates a user-defined function call identified by its name and arguments.
     * <p>
     * Executes the function from the registry, logging execution details for debugging.
     * Functions are identified by name only (e.g., "replace", "min").
     * </p>
     *
     * @param ctx The function context from the parser.
     * @return Result of the function call (e.g., Number, String, Boolean), type depends on the function's implementation.
     * @throws UnsupportedOperationException if the function name is not registered in the function registry.
     * @throws IllegalArgumentException      if the function execution fails (e.g., wrong number of arguments).
     */
    @Override
    public Object visitFunction(ObjectQLParser.FunctionContext ctx) {
        String fnName = ctx.name.accept(this).toString();
        Object[] args = ctx.args() != null ? (Object[]) ctx.args().accept(this) : new Object[0];
        Function<Object[], Object> fn = functionRegistry.get(fnName);
        if (fn == null) {
            LOG.error("Unknown function '{}' invoked in query: {}", fnName, ctx.getText());
            throw new UnsupportedOperationException("Unknown function: " + fnName);
        }
        LOG.debug("Executing function {} with args: {}", fnName, Arrays.toString(args));
        try {
            return fn.apply(args);
        } catch (Exception e) {
            LOG.error("Function '{}' failed with args {}: {}", fnName, Arrays.toString(args), e.getMessage());
            throw new IllegalArgumentException("Function execution failed: " + fnName, e);
        }
    }

    /**
     * Extracts the function name from the parse tree.
     * <p>
     * Returns the text as-is, representing a user-defined function name (e.g., "replace", "max").
     * </p>
     *
     * @param ctx The functionName context from the parser.
     * @return The function name as a string.
     */
    @Override
    public String visitFunctionName(ObjectQLParser.FunctionNameContext ctx) {
        return ctx.getText();
    }

    /**
     * Extracts the relational operator text from the parse tree.
     * <p>
     * Used in relational conditions to identify the comparison operator.
     * </p>
     *
     * @param ctx The relOperator context from the parser.
     * @return The operator as a string (e.g., "==", ">").
     */
    @Override
    public String visitRelOperator(ObjectQLParser.RelOperatorContext ctx) {
        return ctx.getText();
    }

    /**
     * Extracts the arithmetic operator text from the parse tree.
     * <p>
     * Used in mathematical expressions to identify the arithmetic operation.
     * </p>
     *
     * @param ctx The aritOperator context from the parser.
     * @return The operator as a string (e.g., "+", "*").
     */
    @Override
    public String visitAritOperator(ObjectQLParser.AritOperatorContext ctx) {
        return ctx.getText();
    }

    /**
     * Parses a boolean literal from the query text.
     * <p>
     * Converts the case-insensitive text ("true" or "false") to a Boolean value.
     * </p>
     *
     * @param ctx The boolean context from the parser.
     * @return True or false based on the literal text.
     */
    @Override
    public Boolean visitBool(ObjectQLParser.BoolContext ctx) {
        return Boolean.valueOf(ctx.getText());
    }

    /**
     * Parses a numeric literal (integer or decimal) from the query text.
     * <p>
     * Returns an Integer for whole numbers or a Double for decimals, maintaining precision where possible.
     * </p>
     *
     * @param ctx The number context from the parser.
     * @return The parsed numeric value (Integer or Double).
     * @throws IllegalArgumentException if the numeric literal is malformed.
     */
    @Override
    public Number visitNumber(ObjectQLParser.NumberContext ctx) {
        String num = ctx.getText();
        try {

            if (num.contains(".")) return Double.valueOf(num);

            return Integer.valueOf(num);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + num, e);
        }
    }

    /**
     * Parses a text literal, stripping quotes from the string.
     * <p>
     * Handles both single- and double-quoted strings as defined in the grammar, returning
     * the content without quotes. Returns an empty string if the literal is null (though rare).
     * </p>
     *
     * @param ctx The text context from the parser.
     * @return The string value without surrounding quotes.
     */
    @Override
    public String visitText(ObjectQLParser.TextContext ctx) {
        return ctx.TEXT() != null ? ctx.TEXT().toString().replaceAll("['\"]", "") : "";
    }

    @Override
    public Object visitNull(ObjectQLParser.NullContext ctx) {
        return null;
    }

    @Override
    public Object visit(ParseTree parseTree) {
        return null; // Default implementation for unhandled nodes
    }

    @Override
    public Object visitChildren(RuleNode ruleNode) {
        return null; // Default implementation for unhandled nodes
    }

    @Override
    public Object visitTerminal(TerminalNode terminalNode) {
        return null; // Default implementation for unhandled nodes
    }

    @Override
    public Object visitErrorNode(ErrorNode errorNode) {
        return null; // Default implementation for error handling
    }

    private static Object castInput(Object input){
        try {
            if (input instanceof String) return GSON.fromJson((String) input, Object.class);

            return input;
        } catch (Exception e){
            throw new IllegalArgumentException("The provided input is not an object nor a JSON string, but: \"" + input + "\"");
        }
    }
}