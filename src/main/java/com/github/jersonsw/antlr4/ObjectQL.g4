grammar ObjectQL;

/*
 * ObjectQL Grammar
 *
 * A query language for object-oriented data structures, supporting logical operations,
 * comparisons, range queries, collection membership, text pattern matching, and user-defined functions.
 * Designed to evaluate conditions on nested objects with a simple, expressive syntax.
 *
 * Key Features:
 * - Logical operations: AND, OR with parentheses for grouping
 * - Comparisons: <, >, <=, >=, ==, != for numbers and booleans
 * - Range queries: BETWEEN for inclusive numeric ranges
 * - Collection membership: IN, NOT_IN for lists
 * - Text matching: LIKE (~), ILIKE (~~), NOT_LIKE (!~), NOT_ILIKE (!~~) with wildcards
 * - User-defined functions: extensible via identifier-based function calls (e.g., replace, min)
 * - Property paths: dot notation and array indexing (e.g., person.addresses[0].street)
 */

/*
 * Entry point for a complete query, terminated by EOF.
 */
query
    : predication EOF
    ;

/*
 * A logical predication combining conditions with AND/OR operators or parentheses.
 * Uses left recursion for efficient parsing.
 */
predication
    : LPAREN predication RPAREN                // Parentheses for grouping
    | predication andNode                      // Logical AND
    | predication orNode                       // Logical OR
    | condition                                // Base condition
    ;

/*
 * AND operator connecting two predications.
 */
andNode
    : AND predication
    ;

/*
 * OR operator connecting two predications.
 */
orNode
    : OR predication
    ;

/*
 * A single evaluable condition yielding a boolean result.
 * Supports range checks, collection membership, comparisons, text matching, booleans, and functions.
 */
condition
    : btw=betweenCond                          // Numeric range check (e.g., age >=< [18, 65])
    | in=inCond                                // Collection membership (e.g., status >+< ["active"])
    | rel=relCond                              // Numeric or boolean comparison (e.g., age > 18)
    | match=textMatchCond                      // Text pattern matching (e.g., name ~ "Jo*")
    | bool                                     // Literal true/false
    | fn=function                              // Function call returning boolean (e.g., contains(name, "John"))
    ;

/*
 * List of arguments for function calls, separated by commas.
 */
args
   : arg (COMMA arg)*                          // One or more arguments
   ;

/*
 * Individual argument for a function, can be an identifier, text, or math expression.
 */
arg
   : idtfr=identifier                          // Property path (e.g., age)
   | txt=textExpr                              // Text value (e.g., "John")
   | math=mathExpr                             // Numeric expression (e.g., 5 + 3)
   ;

/*
 * Between condition for numeric range checks.
 * Example: age >=< [18, 65] checks if age is between 18 and 65 inclusive.
 */
betweenCond
    : val=mathExpr BETWEEN LBRACKET from=mathExpr COMMA to=mathExpr RBRACKET
    ;

/*
 * IN condition for membership checks.
 * Examples:
 * - status >+< ["active", "pending"] checks if status is in the list
 * - id <> [1, 2, 3] checks if id is not in the list
 */
inCond
    : lhsText=textExpr (IN | NOT_IN) LBRACKET rhsText=stringParams RBRACKET    // Text membership
    | lhsNum=mathExpr (IN | NOT_IN) LBRACKET rhsNum=numericParams RBRACKET      // Numeric membership
    | lhs=textExpr (IN | NOT_IN) rhs=identifier                                    // Membership against identifier
    ;

/*
 * Relational condition for comparisons.
 * Examples:
 * - age > 18 (numeric comparison)
 * - isActive == true (boolean comparison)
 */
relCond
    : lhs=mathExpr opr=relOperator rhs=mathExpr                                    // Numeric comparison
    | bool_match=boolExpr                                                          // Boolean comparison
    ;

/*
 * Text match condition for pattern matching.
 * Examples:
 * - name ~ "Jo*" (case-sensitive match)
 * - name ~~ "jo*" (case-insensitive match)
 */
textMatchCond
    : lhs=textExpr opr=(LIKE | ILIKE | NOT_LIKE | NOT_ILIKE | EQUAL_TO | DIFFERENT_FROM) rhs=textExpr
    ;

/*
 * Collection of parameters for IN/NOT_IN conditions.
 * Can be text or numeric values.
 */
stringParams
    : textExpr (COMMA textExpr)*                                                   // Text collection
    ;

numericParams
    : mathExpr (COMMA mathExpr)*                                                   // Numeric collection
    ;

/*
 * Text expression evaluating to a string value.
 * Supports nested expressions, identifiers, literals, and functions.
 */
textExpr
    : LPAREN textExpr RPAREN                                                       // Parenthesized expression
    | txt=text                                                                     // Literal string (e.g., "John")
    | idtfr=identifier                                                             // Property reference (e.g., name)
    | fn=function                                                                  // Function returning text (e.g., upper("john"))
    | nil=NULL
    ;

/*
 * Boolean expression evaluating to true/false.
 * Supports nesting, comparisons, literals, functions, and identifiers.
 */
boolExpr
    : LPAREN expr=boolExpr RPAREN                                                  // Parenthesized expression
    | lhs=boolExpr opr=(EQUAL_TO | DIFFERENT_FROM) rhs=boolExpr                    // Boolean comparison
    | bool                                                                         // Literal true/false
    | fn=function                                                                  // Function returning boolean (e.g., contains(...))
    | idtfr=identifier                                                             // Property reference (e.g., isActive)
    ;

/*
 * Mathematical expression evaluating to a numeric value.
 * Supports arithmetic operations, identifiers, literals, and functions.
 */
mathExpr
    : LPAREN expr=mathExpr RPAREN                                                  // Parenthesized expression
    | lhs=mathExpr opr=aritOperator rhs=mathExpr                                   // Arithmetic operation (e.g., 5 + 3)
    | idtfr=identifier                                                             // Property reference (e.g., age)
    | num=number                                                                   // Numeric literal (e.g., 42)
    | fn=function                                                                  // Function returning number (e.g., max(1, 2))
    ;

/*
 * Function call with a name and optional arguments.
 * Example: replace(name, "Doe", "Smith") or min(1, 2, 3)
 */
function
    : name=functionName LPAREN args? RPAREN                                        // Function with optional arguments
    ;

/*
 * Function name for user-defined functions.
 * Simple identifier or namespaced (e.g., replace, math.max).
 */
functionName
    : IDENTIFIER
    ;

/*
 * Relational operators for numeric and boolean comparisons.
 */
relOperator
    : LESS_THAN                             // <
    | LESS_THAN_OR_EQUAL_TO                 // <=
    | EQUAL_TO                              // ==
    | GREATER_THAN_OR_EQUAL_TO              // >=
    | GREATER_THAN                          // >
    | DIFFERENT_FROM                        // !=
    ;

/*
 * Arithmetic operators for mathematical expressions.
 */
aritOperator
    : PLUS                                  // +
    | MINUS                                 // -
    | TIMES                                 // *
    | DIVIDE                                // /
    | MOD                                   // %
    ;

/*
 * Identifier for variables, properties, or complex paths.
 * Examples:
 * - variable
 * - object.property
 * - array[0]
 * - person.addresses[0].street
 * - customer[@main].name
 */
identifier
    : IDENTIFIER ( (LBRACKET (INSTANCE | int) RBRACKET)? '.' IDENTIFIER )* (LBRACKET (INSTANCE | int) RBRACKET)?
    ;

/*
 * Boolean literal value (true or false).
 */
bool
    : TRUE                                  // true
    | FALSE                                 // false
    ;

/*
 * Numeric literal in integer, decimal, or scientific notation formats.
 */
number
    : int                            // Integer (e.g., -42)
    | float                          // Decimal (e.g., 3.14)
    | pot                            // Scientific notation (e.g., 1.2^3)
    ;

pot
   : base=potTerm POW exponent=potTerm
   ;

potTerm
    : int
    | float
    ;

float
   : int '.' dec=NATURAL
   ;

int
   : MINUS? NATURAL
   ;

/*
 * Text literal enclosed in single or double quotes.
 */
text
    : TEXT                                 // String literal (e.g., "hello" or 'hello')
    ;

// ========= TOKEN DEFINITIONS =========

// Logical Operators (case insensitive)
AND: [A|a] [N|n] [D|d];
OR: [O|o] [R|r];

// Parentheses and Brackets
LPAREN: '(';
RPAREN: ')';
LBRACKET: '[';
RBRACKET: ']';
COMMA: ',';

// Relational Operators
EQUAL_TO: '==' | 'EQ';                          // Equal to
LESS_THAN: '<' | 'LT';                          // Less than
GREATER_THAN: '>' | 'GT';                       // Greater than
GREATER_THAN_OR_EQUAL_TO: '>=' | 'GTE';         // Greater than or equal to
LESS_THAN_OR_EQUAL_TO: '<=' | 'LTE';            // Less than or equal to
DIFFERENT_FROM: '!=' | 'NE';                    // Not equal to
BETWEEN: '>=<' | 'BETWEEN';                     // Between (inclusive)
IN: '>+<' | 'IN';                               // In collection
NOT_IN: '<>' | 'NOT IN';                        // Not in collection
LIKE: '~' | 'LIKE';                     // Text pattern match
ILIKE: '~~' | 'ILIKE';                  // Case-insensitive text match
NOT_LIKE: '!~' | 'NOT LIKE';            // Negated text pattern match
NOT_ILIKE: '!~~' | 'NOT ILIKE';         // Negated case-insensitive text match

NATURAL: DIGIT+;                                // Integer

// Arithmetic Operators
PLUS: '+';
MINUS: '-';
DIVIDE: '/';
TIMES: '*';
POW: '^';
MOD: '%';

// Boolean Literals (case insensitive)
TRUE: [Tt] [Rr] [Uu] [Ee];
FALSE: [Ff] [Aa] [Ll] [Ss] [Ee];
NULL: [Nn] [Uu] [Ll] [Ll];

// Instance Reference
INSTANCE: '@' LETTER+;                            // Instance identifier (e.g., @main)

// Identifier Format
IDENTIFIER: DOLLAR? (LETTER | '_')(LETTER | DIGIT | '_')*; // Standard identifier (e.g., $var_name)

// Text Literals
TEXT: (QUOTE '%'? (CHAR | SPACE)* '%'? QUOTE) | (DOUBLE_QUOTE '%'? (CHAR | SPACE)* '%'? DOUBLE_QUOTE); // Single or double-quoted string

// Characters Allowed in Text Literals
CHAR: LETTER | DIGIT | SIGN;

// Whitespace Handling
WHITESPACE: [ \t\r\n]+ -> skip;                   // Skip all whitespace

// Fragment Rules (used by tokens)
fragment DOT: '.';
fragment SPACE: ' ';
fragment LETTER: ('a'..'z' | 'A'..'Z');
fragment SIGN: ('.' | '+' | '(' | ')' | '/' | '%' | '#' | '\\' | '@');
fragment QUOTE: '\'';
fragment DOUBLE_QUOTE: '"';
fragment DIGIT: ('0'..'9');
fragment DOLLAR: '$';