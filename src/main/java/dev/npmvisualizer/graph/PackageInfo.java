package dev.npmvisualizer.graph;

import java.util.List;

/** Идентификатор пакета и его прямые зависимости. */
public record PackageInfo(String id, List<Dependency> dependencies) {
    /** Сохраняет независимую неизменяемую копию зависимостей. */
    public PackageInfo {
        dependencies = List.copyOf(dependencies);
    }
}
