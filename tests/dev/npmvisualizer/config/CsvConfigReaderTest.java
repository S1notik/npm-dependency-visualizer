package dev.npmvisualizer.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки формата CSV и всех параметров конфигурации. */
class CsvConfigReaderTest {
    private static final String HEADER = "packageName,repository,testMode,asciiTree\n";
    @TempDir
    Path directory;

    private AppConfig read(String csv) throws IOException {
        Path file = directory.resolve("config.csv");
        Files.writeString(file, csv);
        return new CsvConfigReader().read(file);
    }

    @Test
    void readsConfigWithReorderedHeadersAndBlankLines() throws IOException {
        AppConfig config = read("asciiTree,testMode,repository,packageName\n\n"
                + "true,false,https://registry.npmjs.org,lodash\n\n");
        assertEquals("lodash", config.getPackageName());
        assertEquals("https://registry.npmjs.org", config.getRepository());
        assertFalse(config.isTestMode());
        assertTrue(config.isAsciiTree());
    }

    @Test
    void readsQuotedPathWithCommaAndEscapedQuote() throws IOException {
        Path repository = directory.resolve("test,repo\".txt");
        Files.writeString(repository, "A B");
        String quoted = "\"" + repository.toString().replace("\"", "\"\"") + "\"";
        AppConfig config = read(HEADER + "A," + quoted + ",true,false\n");
        assertEquals(repository.toString(), config.getRepository());
        assertTrue(config.isTestMode());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "", "packageName,repository,testMode,asciiTree\n",
        "packageName,repository,testMode,unknown\nlodash,https://example.org,false,true",
        "packageName,repository,testMode,testMode\nlodash,https://example.org,false,true",
        "packageName,repository,testMode,asciiTree,extra\nlodash,https://example.org,false,true,x"
    })
    void rejectsInvalidStructure(String csv) {
        assertThrows(IllegalArgumentException.class, () -> read(csv));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ",https://example.org,false,true", "bad name,https://example.org,false,true",
        "lodash,,false,true", "lodash,not-a-url,false,true",
        "lodash,ftp://example.org,false,true", "lodash,https:///missing,false,true",
        "lodash,https://example.org,tru,true", "lodash,https://example.org,false,yes",
        "lodash,https://example.org,,true", "lodash,https://example.org,false,",
        "lodash,missing-repository.txt,true,false",
        "lodash,https://example.org,false", "lodash,https://example.org,false,true,extra",
        "lodash,https://example.org,false,true\nlodash,https://example.org,false,true"
    })
    void rejectsInvalidValues(String row) {
        assertThrows(IllegalArgumentException.class, () -> read(HEADER + row));
    }

    @Test
    void rejectsDirectoryAsRepository() {
        assertThrows(IllegalArgumentException.class,
                () -> read(HEADER + "A," + directory + ",true,false"));
    }

    @Test
    void rejectsMalformedCsv() {
        assertThrows(IOException.class, () -> read(HEADER + "\"unclosed"));
    }

    @Test
    void reportsMissingConfig() {
        assertThrows(IOException.class,
                () -> new CsvConfigReader().read(directory.resolve("missing.csv")));
    }
}
