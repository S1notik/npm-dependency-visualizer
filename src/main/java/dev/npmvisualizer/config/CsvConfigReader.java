package dev.npmvisualizer.config;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Читает и проверяет конфигурацию приложения из CSV-файла.
 */
public class CsvConfigReader {

    private static final Set<String> EXPECTED_HEADERS = Set.of(
            "packageName", "repository", "testMode", "asciiTree"
    );

    private static final CSVFormat FORMAT = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .get();

    /**
     * Читает единственную запись конфигурации.
     * @param path путь к CSV-файлу
     * @return проверенная конфигурация
     * @throws IOException если файл невозможно прочитать или CSV повреждён
     * @throws IllegalArgumentException если настройки некорректны
     */
    public AppConfig read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = FORMAT.parse(reader)) {
            validateHeaders(parser.getHeaderNames());
            List<CSVRecord> records = parser.getRecords();
            if (records.size() != 1) {
                throw new IllegalArgumentException(
                        "CSV должен содержать одну запись настроек"
                );
            }
            return createConfig(records.get(0));
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private void validateHeaders(List<String> headers) {
        boolean correctCount = headers.size() == EXPECTED_HEADERS.size();
        boolean correctNames = new HashSet<>(headers).equals(EXPECTED_HEADERS);
        if (!correctCount || !correctNames) {
            throw new IllegalArgumentException(
                    "CSV должен содержать столбцы без повторений: "
                            + "packageName, repository, testMode, asciiTree"
            );
        }
    }

    private AppConfig createConfig(CSVRecord record) {
        if (!record.isConsistent()) {
            throw new IllegalArgumentException(
                    "Количество значений не совпадает с количеством заголовков"
            );
        }
        String packageName = readRequired(record, "packageName");
        String repository = readRequired(record, "repository");
        boolean testMode = readBoolean(record, "testMode");
        boolean asciiTree = readBoolean(record, "asciiTree");
        validatePackageName(packageName);
        validateRepository(repository, testMode);
        return new AppConfig(packageName, repository, testMode, asciiTree);
    }

    private String readRequired(CSVRecord record, String key) {
        String value = record.get(key).strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                    "Параметр " + key + " не должен быть пустым"
            );
        }
        return value;
    }

    private boolean readBoolean(CSVRecord record, String key) {
        String value = readRequired(record, key);
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException(
                "Параметр " + key + " должен иметь значение true или false"
        );
    }

    private void validatePackageName(String packageName) {
        if (packageName.codePoints().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(
                    "Имя пакета не должно содержать пробельные символы"
            );
        }
    }

    private void validateRepository(String repository, boolean testMode) {
        if (testMode) {
            validateTestRepository(repository);
        } else {
            validateRepositoryUrl(repository);
        }
    }

    private void validateTestRepository(String repository) {
        try {
            Path path = Path.of(repository);
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new IllegalArgumentException(
                        "Тестовый репозиторий должен быть доступным для чтения файлом: "
                                + repository
                );
            }
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException(
                    "Некорректный путь к тестовому репозиторию",
                    exception
            );
        }
    }

    private void validateRepositoryUrl(String repository) {
        try {
            URI uri = new URI(repository);
            String scheme = uri.getScheme();
            boolean supportedScheme = "https".equalsIgnoreCase(scheme)
                    || "http".equalsIgnoreCase(scheme);
            if (!supportedScheme || uri.getHost() == null) {
                throw new IllegalArgumentException(
                        "repository должен содержать HTTP- или HTTPS-адрес с именем хоста"
                );
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException(
                    "Некорректный URL репозитория",
                    exception
            );
        }
    }
}