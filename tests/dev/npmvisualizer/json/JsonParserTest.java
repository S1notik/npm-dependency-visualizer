package dev.npmvisualizer.json;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки синтаксиса JSON и обработки строк. */
class JsonParserTest {
    @Test
    void parsesNestedValuesAndEscapes() {
        Object parsed = JsonParser.parse("{\"a\":[true,false,null,-1.25e+2],\"s\":\"x\\n\\\"\\u0410\\/\\\\\"}");
        Map<?, ?> root = (Map<?, ?>) parsed;
        List<?> values = (List<?>) root.get("a");
        assertEquals(Boolean.TRUE, values.get(0));
        assertEquals(Boolean.FALSE, values.get(1));
        assertNull(values.get(2));
        assertEquals(new BigDecimal("-1.25e+2"), values.get(3));
        assertEquals("x\n\"А/\\", root.get("s"));
    }

    @Test
    void rejectsInvalidJson() {
        for (String input : List.of("", "{\"a\":1,}", "[1,]", "01", "1.", "1e+", "true false",
                "\"\\q\"", "\"\\u12X4\"", "{\"a\":null,\"a\":1}", "{\"a\":\"unterminated}")) {
            assertThrows(IllegalArgumentException.class, () -> JsonParser.parse(input), input);
        }
    }

    @Test
    void limitsExcessiveNesting() {
        String input = "[".repeat(130) + "0" + "]".repeat(130);
        assertThrows(IllegalArgumentException.class, () -> JsonParser.parse(input));
    }
}
