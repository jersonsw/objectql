# ObjectQL - Object Query Language

## Overview

ObjectQL is a powerful query language designed for evaluating conditions on object-oriented data structures. It provides an expressive syntax for logical operations, comparisons, range queries, collection membership, text pattern matching, and user-defined functions.

ObjectQL queries are parsed and evaluated using the `QueryEvaluatorVisitor`, which walks through the query tree and evaluates conditions dynamically. It supports complex conditions, nested structures, and property path resolution using Apache Commons BeanUtils.

## Features
- **Logical operations**: `AND`, `OR`, with parentheses for grouping
- **Comparisons**: `<`, `>`, `<=`, `>=`, `==`, `!=` for numbers and booleans
- **Range queries**: `BETWEEN` for inclusive numeric ranges
- **Collection membership**: `IN`, `NOT_IN` for lists
- **Text matching**: `LIKE (~)`, `ILIKE (~~)`, `NOT_LIKE (!~)`, `NOT_ILIKE (!~~)` with wildcards
- **User-defined functions**: Extensible via function calls (e.g., `replace`, `min`)
- **Property paths**: Dot notation and array indexing (e.g., `person.addresses[0].street`)

## Query Evaluation
Queries are evaluated using `QueryEvaluatorVisitor`, which recursively processes conditions. Key features of the evaluation process:

- **Short-circuiting**: `AND` and `OR` conditions are evaluated efficiently.
- **Property path resolution**: Supports deep property access (e.g., `user.profile.age`).
- **Error handling**: Logs errors and handles missing properties gracefully.

### Example Query on a Complex JSON Object
#### JSON Data
```json
{
  "person": {
    "id": 12345,
    "name": "Alice Johnson",
    "age": 34,
    "contact": {
      "email": "alice.johnson@example.com",
      "phones": [
        {"type": "mobile", "number": "555-1234", "active": true},
        {"type": "home", "number": "555-5678", "active": false}
      ],
      "address": {
        "street": "123 Elm Street",
        "city": "Springfield",
        "zip": "62701",
        "coordinates": {
          "lat": 39.7817,
          "lon": -89.6501
        }
      }
    },
    "orders": [
      {
        "orderId": "ORD001",
        "total": 199.95,
        "items": [
          {"product": "Laptop", "price": 149.99, "quantity": 1},
          {"product": "Mouse", "price": 24.99, "quantity": 2}
        ],
        "status": "shipped"
      },
      {
        "orderId": "ORD002",
        "total": 75.50,
        "items": [
          {"product": "Keyboard", "price": 75.50, "quantity": 1}
        ],
        "status": "pending"
      }
    ],
    "preferences": {
      "notifications": true,
      "theme": "dark"
    }
  }
}
```

#### Example Queries
```java
String query1 = "person.age > 30 AND person.contact.city == 'Springfield'";
boolean result1 = QueryEvaluator.eval(json, query1);
System.out.println("Query result: " + result1);

String query2 = "person.orders[0].status == 'shipped' AND person.orders[1].total < 100";
boolean result2 = QueryEvaluator.eval(json, query2);
System.out.println("Query result: " + result2);
```

## Built-in Functions
ObjectQL provides several built-in functions:

### String Functions
```java
visitor.registerFunction("upper", args -> ((String) args[0]).toUpperCase());
visitor.registerFunction("concat", args -> args[0] + args[1]);
```
- `replace(string, target, replacement)`: Replaces occurrences of `target` with `replacement`
- `upper(string)`: Converts a string to uppercase
- `lower(string)`: Converts a string to lowercase
- `substring(string, start, end)`: Extracts a substring
- `contains(string, substring)`: Checks if `substring` exists in `string`
- `startsWith(string, prefix)`: Checks if `string` starts with `prefix`
- `endsWith(string, suffix)`: Checks if `string` ends with `suffix`

### Numeric Functions
```java
visitor.registerFunction("max", args -> Math.max((double) args[0], (double) args[1]));
```
- `min(value1, value2, ...)`: Returns the minimum value
- `max(value1, value2, ...)`: Returns the maximum value
- `abs(number)`: Returns the absolute value
- `round(number)`: Rounds a number to the nearest integer
- `sqrt(number)`: Returns the square root

## Custom Functions
You can register your own functions to extend ObjectQL:
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
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.jersonsw</groupId>
        <artifactId>objectql</artifactId>
        <version>v1.0.1</version>
    </dependency>
</dependencies>
```

Or if you're using Gradle:
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.jersonsw:objectql:v1.0.1'
}
```

## Debugging and Logging
ObjectQL uses **SLF4J** for logging. You can enable debugging to inspect query evaluations:
```java
private static final Logger LOG = LoggerFactory.getLogger(QueryEvaluatorVisitor.class);
LOG.debug("Evaluating query: {}", query);
```
This helps diagnose issues when evaluating complex queries.

## License
This project is licensed under the MIT License.