package dev.npmvisualizer.graph;

import java.util.List;

/** Идентификатор пакета и его прямые зависимости. */
public record PackageInfo(String id, List<Dependency> dependencies) {
    public PackageInfo {
        dependencies = List.copyOf(dependencies);
    }
}
