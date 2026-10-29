package dev.npmvisualizer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки вывода и кодов завершения приложения. */
class MainTest {
    @TempDir
    Path directory;

    @Test
    void printsAllSettings() throws Exception {
        Path config = directory.resolve("config.csv");
        Files.writeString(config, "packageName,repository,testMode,asciiTree\n"
                + "lodash,https://registry.npmjs.org,false,true\n");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int code = Main.run(new String[]{config.toString()},
                new PrintStream(output), new PrintStream(errors));
        assertEquals(0, code);
        assertEquals(String.join(System.lineSeparator(), "packageName=lodash",
                "repository=https://registry.npmjs.org", "testMode=false", "asciiTree=true", ""),
                output.toString(StandardCharsets.UTF_8));
        assertEquals("", errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void reportsErrorsWithoutSettings() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int code = Main.run(new String[]{directory.resolve("missing.csv").toString()},
                new PrintStream(output), new PrintStream(errors));
        assertEquals(1, code);
        assertEquals("", output.toString(StandardCharsets.UTF_8));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Ошибка чтения CSV"));
    }

    @Test
    void rejectsExtraArguments() {
        assertEquals(1, Main.run(new String[]{"one", "two"}, System.out, System.err));
    }

    @Test
    void reportsInvalidConfiguration() throws Exception {
        Path config = directory.resolve("bad.csv");
        Files.writeString(config, "wrong,headers\n1,2");
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        assertEquals(1, Main.run(new String[]{config.toString()}, System.out, new PrintStream(errors)));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("Ошибка конфигурации"));
    }
}
