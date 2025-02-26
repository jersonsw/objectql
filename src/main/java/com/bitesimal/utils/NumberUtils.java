package com.bitesimal.utils;

public class NumberUtils {
    public static Number sum(Number a, Number b) {
        if (a instanceof Integer && b instanceof Integer) {
            return a.intValue() + b.intValue(); // Preserve Integer type
        } else if (a instanceof Double || b instanceof Double) {
            return a.doubleValue() + b.doubleValue(); // Use Double if either is Double
        } else if (a instanceof Float || b instanceof Float) {
            return a.floatValue() + b.floatValue(); // Use Float if either is Float
        } else if (a instanceof Long || b instanceof Long) {
            return a.longValue() + b.longValue(); // Use Long if either is Long
        } else if (a instanceof Short && b instanceof Short) {
            return (short) (a.shortValue() + b.shortValue()); // Preserve Short type
        } else if (a instanceof Byte && b instanceof Byte) {
            return (byte) (a.byteValue() + b.byteValue()); // Preserve Byte type
        } else {
            // Default to double for other cases
            return a.doubleValue() + b.doubleValue();
        }
    }

    public static Number subtract(Number a, Number b) {
        if (a instanceof Integer && b instanceof Integer) {
            return a.intValue() - b.intValue(); // Preserve Integer type
        } else if (a instanceof Double || b instanceof Double) {
            return a.doubleValue() - b.doubleValue(); // Use Double if either is Double
        } else if (a instanceof Float || b instanceof Float) {
            return a.floatValue() - b.floatValue(); // Use Float if either is Float
        } else if (a instanceof Long || b instanceof Long) {
            return a.longValue() - b.longValue(); // Use Long if either is Long
        } else if (a instanceof Short && b instanceof Short) {
            return (short) (a.shortValue() - b.shortValue()); // Preserve Short type
        } else if (a instanceof Byte && b instanceof Byte) {
            return (byte) (a.byteValue() - b.byteValue()); // Preserve Byte type
        } else {
            // Default to double for other cases
            return a.doubleValue() - b.doubleValue();
        }
    }

    public static Number multiply(Number a, Number b) {
        if (a instanceof Integer && b instanceof Integer) {
            return a.intValue() * b.intValue(); // Preserve Integer type
        } else if (a instanceof Double || b instanceof Double) {
            return a.doubleValue() * b.doubleValue(); // Use Double if either is Double
        } else if (a instanceof Float || b instanceof Float) {
            return a.floatValue() * b.floatValue(); // Use Float if either is Float
        } else if (a instanceof Long || b instanceof Long) {
            return a.longValue() * b.longValue(); // Use Long if either is Long
        } else if (a instanceof Short && b instanceof Short) {
            return (short) (a.shortValue() * b.shortValue()); // Preserve Short type
        } else if (a instanceof Byte && b instanceof Byte) {
            return (byte) (a.byteValue() * b.byteValue()); // Preserve Byte type
        } else {
            // Default to double for other cases
            return a.doubleValue() * b.doubleValue();
        }
    }

    public static Number divide(Number a, Number b) {
        if (a instanceof Integer && b instanceof Integer) {
            return a.intValue() / b.intValue(); // Preserve Integer type
        } else if (a instanceof Double || b instanceof Double) {
            return a.doubleValue() / b.doubleValue(); // Use Double if either is Double
        } else if (a instanceof Float || b instanceof Float) {
            return a.floatValue() / b.floatValue(); // Use Float if either is Float
        } else if (a instanceof Long || b instanceof Long) {
            return a.longValue() / b.longValue(); // Use Long if either is Long
        } else if (a instanceof Short && b instanceof Short) {
            return (short) (a.shortValue() / b.shortValue()); // Preserve Short type
        } else if (a instanceof Byte && b instanceof Byte) {
            return (byte) (a.byteValue() / b.byteValue()); // Preserve Byte type
        } else {
            // Default to double for other cases
            return a.doubleValue() / b.doubleValue();
        }
    }

    public static Number mod(Number a, Number b) {
        if (a instanceof Integer && b instanceof Integer) {
            return a.intValue() % b.intValue(); // Preserve Integer type
        } else if (a instanceof Double || b instanceof Double) {
            return a.doubleValue() % b.doubleValue(); // Use Double if either is Double
        } else if (a instanceof Float || b instanceof Float) {
            return a.floatValue() % b.floatValue(); // Use Float if either is Float
        } else if (a instanceof Long || b instanceof Long) {
            return a.longValue() % b.longValue(); // Use Long if either is Long
        } else if (a instanceof Short && b instanceof Short) {
            return (short) (a.shortValue() % b.shortValue()); // Preserve Short type
        } else if (a instanceof Byte && b instanceof Byte) {
            return (byte) (a.byteValue() % b.byteValue()); // Preserve Byte type
        } else {
            // Default to double for other cases
            return a.doubleValue() % b.doubleValue();
        }
    }

    public static Number pow(Number a, Number b) {
        if (a instanceof Integer && b instanceof Integer) {
            return Math.pow(a.intValue(), b.intValue()); // Preserve Integer type
        } else if (a instanceof Double || b instanceof Double) {
            return Math.pow(a.doubleValue(), b.doubleValue()); // Use Double if either is Double
        } else if (a instanceof Float || b instanceof Float) {
            return Math.pow(a.floatValue(), b.floatValue()); // Use Float if either is Float
        } else if (a instanceof Long || b instanceof Long) {
            return Math.pow(a.longValue(), b.longValue()); // Use Long if either is Long
        } else if (a instanceof Short && b instanceof Short) {
            return (short) Math.pow(a.shortValue(), b.shortValue()); // Preserve Short type
        } else if (a instanceof Byte && b instanceof Byte) {
            return (byte) Math.pow(a.byteValue(), b.byteValue()); // Preserve Byte type
        } else {
            // Default to double for other cases
            return Math.pow(a.doubleValue(), b.doubleValue());
        }
    }
}