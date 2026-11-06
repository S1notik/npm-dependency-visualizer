package dev.npmvisualizer.graph;

import java.io.IOException;

/** Источник корневого пакета и пакетов, на которые указывают зависимости. */
public interface PackageRepository {
    PackageInfo root(String name) throws IOException, InterruptedException;

    PackageInfo resolve(Dependency dependency) throws IOException, InterruptedException;
}
