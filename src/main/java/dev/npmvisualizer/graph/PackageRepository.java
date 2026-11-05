package dev.npmvisualizer.graph;

import java.io.IOException;

/** Источник корневого пакета и пакетов, на которые указывают зависимости. */
public interface PackageRepository {
    /** Находит пакет, выбранный пользователем. */
    PackageInfo root(String name) throws IOException, InterruptedException;

    /** Находит конкретный пакет, соответствующий объявлению зависимости. */
    PackageInfo resolve(Dependency dependency) throws IOException, InterruptedException;
}
