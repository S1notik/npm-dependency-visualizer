package dev.npmvisualizer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;

import dev.npmvisualizer.config.AppConfig;
import dev.npmvisualizer.config.CsvConfigReader;
import dev.npmvisualizer.registry.NpmRegistryClient;
import dev.npmvisualizer.registry.PackageMetadata;

/** Точка входа приложения и обработка ошибок командной строки. */
public class Main {
    private static final int SUCCESS = 0;
    private static final int ERROR = 1;

    /** Запускает приложение и передаёт его код завершения операционной системе. */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Выполняет приложение со стандартным HTTP-клиентом. */
    public static int run(String[] args, PrintStream output, PrintStream errors) {
        return run(args, output, errors, new NpmRegistryClient());
    }

    /** Выполняет приложение с заданным клиентом реестра. */
    static int run(String[] args, PrintStream output, PrintStream errors, NpmRegistryClient client) {
        if (args.length > 1) {
            errors.println("Использование: ./run.sh [путь-к-конфигурации.csv]");
            return ERROR;
        }
        AppConfig config;
        try {
            Path path = Path.of(args.length == 0 ? "config/example.csv" : args[0]);
            config = new CsvConfigReader().read(path);
        } catch (IOException exception) {
            errors.println("Ошибка чтения CSV: " + exception.getMessage());
            return ERROR;
        } catch (IllegalArgumentException exception) {
            errors.println("Ошибка конфигурации: " + exception.getMessage());
            return ERROR;
        }
        return printDependencies(config, output, errors, client);
    }

    private static int printDependencies(AppConfig config, PrintStream output,
                                         PrintStream errors, NpmRegistryClient client) {
        if (config.isTestMode()) {
            errors.println("Тестовый репозиторий будет поддержан на этапе 3.");
            return ERROR;
        }
        try {
            PackageMetadata metadata = client.fetch(config.getRepository(), config.getPackageName());
            printMetadata(metadata, output);
            return SUCCESS;
        } catch (IOException | IllegalArgumentException exception) {
            errors.println("Ошибка получения зависимостей: " + exception.getMessage());
            return ERROR;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            errors.println("Получение зависимостей прервано.");
            return ERROR;
        }
    }

    private static void printMetadata(PackageMetadata metadata, PrintStream output) {
        output.println("Пакет: " + metadata.name() + "@" + metadata.version() + " (тег latest)");
        if (metadata.groups().isEmpty()) {
            output.println("Прямые зависимости не указаны.");
            return;
        }
        output.println("Прямые зависимости из манифеста:");
        for (Map.Entry<String, Map<String, String>> group : metadata.groups().entrySet()) {
            output.println(group.getKey() + ":");
            group.getValue().forEach((name, version) -> output.println("  " + name + " = " + version));
        }
    }
}
