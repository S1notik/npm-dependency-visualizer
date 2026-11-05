package dev.npmvisualizer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.npmvisualizer.registry.NpmRegistryClient;
import dev.npmvisualizer.registry.RegistryTransport.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки запуска, построения графа и кодов завершения приложения. */
class MainTest {
    @TempDir
    Path directory;

    @Test
    void buildsTestGraphAndMarksCycleWithoutNetwork() throws Exception {
        Path repository = directory.resolve("repo.txt");
        Files.writeString(repository, "A: B, C\nB: A\nC:\n");
        Path config = writeConfig("A," + repository + ",true,false");
        AtomicBoolean called = new AtomicBoolean();
        NpmRegistryClient client = new NpmRegistryClient(uri -> {
            called.set(true);
            return new Response(500, "");
        });
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int code = Main.run(new String[]{config.toString()}, new PrintStream(output), new PrintStream(errors), client);
        String text = output.toString(StandardCharsets.UTF_8);
        assertEquals(0, code);
        assertTrue(text.contains("Пакеты (3):"));
        assertTrue(text.contains("Связи (3):"));
        assertTrue(text.contains("B -> A (цикл)"));
        assertFalse(called.get());
        assertEquals("", errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void printsTransitiveRegistryGraph() throws Exception {
        Path config = writeConfig("root,https://registry.example,false,false");
        NpmRegistryClient client = new NpmRegistryClient(uri -> {
            String body = uri.getRawPath().equals("/root/latest")
                    ? "{\"name\":\"root\",\"version\":\"1.0.0\",\"dependencies\":{\"lib\":\"^1.0.0\"}}"
                    : "{\"versions\":{\"1.1.0\":{\"name\":\"lib\",\"version\":\"1.1.0\"}}}";
            return new Response(200, body);
        });
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int code = Main.run(new String[]{config.toString()}, new PrintStream(output), new PrintStream(errors), client);
        assertEquals(0, code);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("root@1.0.0 -> lib@1.1.0 [lib: ^1.0.0]"));
        assertEquals("", errors.toString(StandardCharsets.UTF_8));
    }

    @Test
    void reportsHttpErrorWithoutPartialOutput() throws Exception {
        Path config = writeConfig("missing,https://registry.example,false,false");
        NpmRegistryClient client = new NpmRegistryClient(uri -> new Response(404, "not found"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        assertEquals(1, Main.run(new String[]{config.toString()}, new PrintStream(output),
                new PrintStream(errors), client));
        assertEquals("", output.toString(StandardCharsets.UTF_8));
        assertTrue(errors.toString(StandardCharsets.UTF_8).contains("HTTP 404"));
    }

    @Test
    void reportsMissingConfig() {
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int code = Main.run(new String[]{directory.resolve("missing.csv").toString()},
                System.out, new PrintStream(errors));
        assertEquals(1, code);
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

    private Path writeConfig(String row) throws Exception {
        Path config = directory.resolve("config.csv");
        Files.writeString(config, "packageName,repository,testMode,asciiTree\n" + row + "\n");
        return config;
    }
}
