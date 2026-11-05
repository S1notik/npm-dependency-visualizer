package dev.npmvisualizer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import dev.npmvisualizer.config.AppConfig;
import dev.npmvisualizer.config.CsvConfigReader;
import dev.npmvisualizer.graph.Dependency;
import dev.npmvisualizer.graph.DependencyGraph;
import dev.npmvisualizer.graph.FileGraphRepository;
import dev.npmvisualizer.graph.GraphBuilder;
import dev.npmvisualizer.graph.PackageRepository;
import dev.npmvisualizer.registry.NpmPackageRepository;
import dev.npmvisualizer.registry.NpmRegistryClient;

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
        return buildGraph(config, output, errors, client);
    }

    private static int buildGraph(AppConfig config, PrintStream output,
                                  PrintStream errors, NpmRegistryClient client) {
        try {
            PackageRepository repository = config.isTestMode()
                    ? FileGraphRepository.read(Path.of(config.getRepository()))
                    : new NpmPackageRepository(config.getRepository(), client);
            DependencyGraph graph = new GraphBuilder().build(repository, config.getPackageName());
            printGraph(graph, output);
            return SUCCESS;
        } catch (IOException | IllegalArgumentException exception) {
            errors.println("Ошибка построения графа: " + exception.getMessage());
            return ERROR;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            errors.println("Построение графа прервано.");
            return ERROR;
        }
    }

    private static void printGraph(DependencyGraph graph, PrintStream output) {
        output.println("Пакеты (" + graph.nodes().size() + "):");
        graph.nodes().keySet().forEach(name -> output.println("  " + name));
        output.println("Связи (" + graph.edges().size() + "):");
        graph.edges().forEach(edge -> output.println(formatEdge(edge)));
        long backEdges = graph.edges().stream().filter(DependencyGraph.Edge::backEdge).count();
        output.println("Обратные рёбра DFS (признаки циклов): " + backEdges);
    }

    private static String formatEdge(DependencyGraph.Edge edge) {
        Dependency dependency = edge.declaration();
        String detail = dependency.requirement().isEmpty() ? ""
                : " [" + dependency.name() + ": " + dependency.requirement() + "]";
        String optional = dependency.optional() ? " (необязательная)" : "";
        String cycle = edge.backEdge() ? " (цикл)" : "";
        return "  " + edge.from() + " -> " + edge.to() + detail + optional + cycle;
    }
}
