# ObjectQL - Object Query Language

ObjectQL is a powerful query language designed for evaluating conditions on object-oriented data structures. It provides an expressive syntax for logical operations, comparisons, range queries, collection membership, text pattern matching, and user-defined functions.

## Features
- Logical operations: `AND`, `OR`, with parentheses for grouping
- Comparisons: `<`, `>`, `<=`, `>=`, `==`, `!=` for numbers and booleans
- Range queries: `BETWEEN` for inclusive numeric ranges
- Collection membership: `IN`, `NOT_IN` for lists
- Text matching: `LIKE (~)`, `ILIKE (~~)`, `NOT_LIKE (!~)`, `NOT_ILIKE (!~~)` with wildcards
- User-defined functions: Extensible via function calls (e.g., `replace`, `min`)
- Property paths: Dot notation and array indexing (e.g., `person.addresses[0].street`)

## Operators
### Logical Operators
Logical operators allow combining multiple conditions:
- `AND`: Both conditions must be true (`age > 30 AND status == 'active'`)
- `OR`: At least one condition must be true (`age > 30 OR status == 'active'`)
- Parentheses `()` allow grouping complex conditions (`(age > 30 OR age < 50) AND status == 'active'`)

### Comparison Operators
- `<` (Less than): `age < 30`
- `>` (Greater than): `age > 30`
- `<=` (Less than or equal to): `age <= 30`
- `>=` (Greater than or equal to): `age >= 30`
- `==` (Equal to): `status == 'active'`
- `!=` (Not equal to): `status != 'inactive'`

### Range Queries
- `BETWEEN`: `age >=< [18, 65]` checks if `age` is between 18 and 65 (inclusive)

### Collection Membership
- `IN`: `status >+< ['active', 'pending']` checks if `status` is in the list
- `NOT_IN`: `status <> ['inactive', 'banned']` checks if `status` is not in the list

### Text Matching
- `LIKE (~)`: `name ~ 'Jo*'` matches any string starting with "Jo"
- `ILIKE (~~)`: Case-insensitive version of LIKE (`name ~~ 'jo*'`)
- `NOT_LIKE (!~)`: `name !~ 'Jo*'` ensures "Jo*" pattern is not matched
- `NOT_ILIKE (!~~)`: Case-insensitive version of NOT LIKE

## Nested Compound Conditions
You can use parentheses to create complex conditions:
```java
String complexQuery = "((age > 25 AND status == 'active') OR (age < 18 AND status == 'pending'))";
boolean complexResult = QueryEvaluator.eval(json, complexQuery);
System.out.println("Complex Query result: " + complexResult);
```
This ensures proper grouping and priority evaluation.

## Built-in Functions
ObjectQL provides several built-in functions for advanced querying:
### String Functions
- `replace(string, target, replacement)`: Replaces occurrences of `target` with `replacement`
- `upper(string)`: Converts a string to uppercase
- `lower(string)`: Converts a string to lowercase
- `substring(string, start, end)`: Extracts a substring
- `concat(string1, string2, ...)`: Concatenates multiple strings
- `contains(string, substring)`: Checks if `substring` exists in `string`
- `startsWith(string, prefix)`: Checks if `string` starts with `prefix`
- `endsWith(string, suffix)`: Checks if `string` ends with `suffix`

### Numeric Functions
- `min(value1, value2, ...)`: Returns the minimum value
- `max(value1, value2, ...)`: Returns the maximum value
- `abs(number)`: Returns the absolute value
- `round(number)`: Rounds a number to the nearest integer
- `ceil(number)`: Returns the smallest integer greater than or equal to `number`
- `floor(number)`: Returns the largest integer less than or equal to `number`
- `sqrt(number)`: Returns the square root

### Length Functions
- `length(string or array)`: Returns the length of a string or array

## Custom Functions
You can register custom functions to extend ObjectQL.
```java
QueryEvaluatorVisitor visitor = new QueryEvaluatorVisitor(inputObject);
visitor.registerFunction("double", args -> ((Number) args[0]).doubleValue() * 2);
String functionQuery = "double(age) > 50";
boolean functionResult = QueryEvaluator.eval(json, functionQuery, visitor);
System.out.println("Custom Function Query result: " + functionResult);
```

## Installation
To use ObjectQL in your Java project, add the following to your `pom.xml`:

```xml
<!-- Add JitPack repository -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<!-- Add ObjectQL dependency -->
<dependencies>
    <dependency>
        <groupId>com.github.jersonsw</groupId>
        <artifactId>objectql</artifactId>
        <version>v0.9.0-alpha</version>
    </dependency>
</dependencies>
```

Or if you're using Gradle:

```groovy
// Add JitPack repository
repositories {
    maven { url 'https://jitpack.io' }
}

// Add ObjectQL dependency
dependencies {
    implementation 'com.github.jersonsw:objectql:v0.9.0-alpha'
}
```

## Usage Examples
### Evaluating Nested JSON Objects
```java
String json = "{\"person\": {\"name\": \"John\", \"age\": 30, \"address\": {\"city\": \"New York\"}}}";
String query = "person.age > 25 AND person.address.city == 'New York'";
boolean result = QueryEvaluator.eval(json, query);
System.out.println("Query result: " + result);
```

### Evaluating Large JSON Payloads
```java
String largeJson = "{\"users\": [{\"id\":1, \"name\":\"Alice\"}, {\"id\":2, \"name\":\"Bob\"}]}";
String largeQuery = "users[0].name == 'Alice' AND users[1].name == 'Bob'";
boolean largeResult = QueryEvaluator.eval(largeJson, largeQuery);
System.out.println("Large JSON Query result: " + largeResult);
```

## Contributing
Feel free to submit issues and pull requests to improve ObjectQL.

## License
This project is licensed under the MIT License.
