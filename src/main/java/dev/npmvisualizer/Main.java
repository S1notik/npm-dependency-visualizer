package dev.npmvisualizer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import dev.npmvisualizer.config.AppConfig;
import dev.npmvisualizer.config.CsvConfigReader;

public class Main {
    private static final int SUCCESS = 0;
    private static final int ERROR = 1;
    private static final int CONFIG_ARGUMENT_COUNT = 1;

    /** Запуск приложения */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /** Выполняет приложение; отдельный метод позволяет проверять вывод в тестах. */
    public static int run(String[] args, PrintStream output, PrintStream errors) {
        if (args.length > CONFIG_ARGUMENT_COUNT) {
            errors.println("Использование: ./run.sh [путь-к-конфигурации.csv]");
            return ERROR;
        }
        try {
            Path path = Path.of(args.length == 0 ? "config/example.csv" : args[0]);
            AppConfig config = new CsvConfigReader().read(path);
            printConfig(config, output);
            return SUCCESS;
        } catch (IOException exception) {
            errors.println("Ошибка чтения CSV: " + exception.getMessage());
            return ERROR;
        } catch (IllegalArgumentException exception) {
            errors.println("Ошибка конфигурации: " + exception.getMessage());
            return ERROR;
        }
    }

    private static void printConfig(AppConfig config, PrintStream output) {
        output.println("packageName=" + config.getPackageName());
        output.println("repository=" + config.getRepository());
        output.println("testMode=" + config.isTestMode());
        output.println("asciiTree=" + config.isAsciiTree());
    }
}
