package dev.npmvisualizer.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Разбирает JSON-ответ реестра без сторонней библиотеки. */
public final class JsonParser {
    private static final int MAX_DEPTH = 128;
    private final String input;
    private int position;

    private JsonParser(String input) {
        this.input = input;
    }

    /** Разбирает один JSON-документ и проверяет, что после него нет данных. */
    public static Object parse(String input) {
        JsonParser parser = new JsonParser(input);
        Object value = parser.readValue(0);
        parser.skipWhitespace();
        if (parser.position != input.length()) {
            throw parser.error("лишние символы после документа");
        }
        return value;
    }

    private Object readValue(int depth) {
        if (depth > MAX_DEPTH) {
            throw error("превышена допустимая вложенность");
        }
        skipWhitespace();
        if (position >= input.length()) {
            throw error("ожидалось значение");
        }
        return switch (input.charAt(position)) {
            case '{' -> readObject(depth + 1);
            case '[' -> readArray(depth + 1);
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject(int depth) {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (consume('}')) {
            return result;
        }
        do {
            skipWhitespace();
            if (position >= input.length() || input.charAt(position) != '"') {
                throw error("ожидалось имя поля в кавычках");
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            Object value = readValue(depth);
            if (result.containsKey(key)) {
                throw error("повторяющееся поле: " + key);
            }
            result.put(key, value);
            skipWhitespace();
        } while (consume(','));
        expect('}');
        return result;
    }

    private List<Object> readArray(int depth) {
        expect('[');
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (consume(']')) {
            return result;
        }
        do {
            result.add(readValue(depth));
            skipWhitespace();
        } while (consume(','));
        expect(']');
        return result;
    }

    private String readString() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (position < input.length()) {
            char character = input.charAt(position++);
            if (character == '"') {
                return result.toString();
            }
            if (character == '\\') {
                result.append(readEscape());
            } else if (character < 0x20) {
                throw error("управляющий символ внутри строки");
            } else {
                result.append(character);
            }
        }
        throw error("незакрытая строка");
    }

    private char readEscape() {
        if (position >= input.length()) {
            throw error("незавершённая escape-последовательность");
        }
        return switch (input.charAt(position++)) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> readUnicodeEscape();
            default -> throw error("неизвестная escape-последовательность");
        };
    }

    private char readUnicodeEscape() {
        if (position + 4 > input.length()) {
            throw error("незавершённая последовательность Unicode");
        }
        int value = 0;
        for (int i = 0; i < 4; i++) {
            char character = input.charAt(position++);
            int digit = "0123456789abcdef".indexOf(Character.toLowerCase(character));
            if (digit < 0) {
                throw error("некорректная последовательность Unicode");
            }
            value = value * 16 + digit;
        }
        return (char) value;
    }

    private BigDecimal readNumber() {
        int start = position;
        consume('-');
        if (consume('0')) {
            if (hasDigit()) {
                throw error("ведущий ноль в числе");
            }
        } else {
            requireDigits();
        }
        if (consume('.')) {
            requireDigits();
        }
        if (consume('e') || consume('E')) {
            if (!consume('+')) {
                consume('-');
            }
            requireDigits();
        }
        try {
            return new BigDecimal(input.substring(start, position));
        } catch (NumberFormatException exception) {
            throw error("некорректное число");
        }
    }

    private void requireDigits() {
        if (!hasDigit()) {
            throw error("ожидалась цифра");
        }
        while (hasDigit()) {
            position++;
        }
    }

    private boolean hasDigit() {
        return position < input.length() && input.charAt(position) >= '0' && input.charAt(position) <= '9';
    }

    private Object readLiteral(String token, Object value) {
        if (!input.startsWith(token, position)) {
            throw error("неизвестное значение");
        }
        position += token.length();
        return value;
    }

    private void skipWhitespace() {
        while (position < input.length() && " \t\r\n".indexOf(input.charAt(position)) >= 0) {
            position++;
        }
    }

    private boolean consume(char character) {
        if (position < input.length() && input.charAt(position) == character) {
            position++;
            return true;
        }
        return false;
    }

    private void expect(char character) {
        if (!consume(character)) {
            throw error("ожидался символ '" + character + "'");
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " (позиция " + position + ")");
    }
}
