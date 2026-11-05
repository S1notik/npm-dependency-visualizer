package dev.npmvisualizer.graph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Тестовый репозиторий из текстового списка смежности с именами A-Z. */
public final class FileGraphRepository implements PackageRepository {
    private final Map<String, PackageInfo> packages;

    private FileGraphRepository(Map<String, PackageInfo> packages) {
        this.packages = packages;
    }

    /** Читает строки формата A: B, C и проверяет ссылки на все пакеты. */
    public static FileGraphRepository read(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        Map<String, PackageInfo> packages = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            parseLine(line, i + 1, packages);
        }
        checkReferences(packages);
        return new FileGraphRepository(packages);
    }

    private static void parseLine(String line, int number, Map<String, PackageInfo> packages) throws IOException {
        if (!line.matches("[A-Z]+\\s*:\\s*([A-Z]+(\\s*,\\s*[A-Z]+)*)?\\s*")) {
            throw new IOException("строка " + number + ": ожидается формат A: B, C с именами A-Z");
        }
        String[] parts = line.split(":", 2);
        String name = parts[0].strip();
        List<Dependency> dependencies = parseDependencies(parts[1]);
        if (packages.putIfAbsent(name, new PackageInfo(name, dependencies)) != null) {
            throw new IOException("строка " + number + ": пакет " + name + " описан повторно");
        }
    }

    private static List<Dependency> parseDependencies(String value) throws IOException {
        List<Dependency> result = new ArrayList<>();
        if (value.isBlank()) {
            return result;
        }
        for (String item : value.split(",")) {
            Dependency dependency = new Dependency(item.strip(), "", false);
            if (result.contains(dependency)) {
                throw new IOException("повторная зависимость " + dependency.name());
            }
            result.add(dependency);
        }
        return result;
    }

    private static void checkReferences(Map<String, PackageInfo> packages) throws IOException {
        for (PackageInfo node : packages.values()) {
            for (Dependency dependency : node.dependencies()) {
                if (!packages.containsKey(dependency.name())) {
                    throw new IOException("пакет " + node.id() + " ссылается на неописанный пакет "
                            + dependency.name());
                }
            }
        }
    }

    /** Возвращает выбранный пакет тестового репозитория. */
    @Override
    public PackageInfo root(String name) throws IOException {
        PackageInfo node = packages.get(name);
        if (node == null) {
            throw new IOException("пакет " + name + " отсутствует в тестовом репозитории");
        }
        return node;
    }

    /** Возвращает тестовый пакет по имени зависимости. */
    @Override
    public PackageInfo resolve(Dependency dependency) throws IOException {
        return root(dependency.name());
    }
}
