package dev.npmvisualizer.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Направленный граф с сохранёнными объявлениями и обратными рёбрами DFS. */
public record DependencyGraph(Map<String, PackageInfo> nodes, List<Edge> edges) {
    public DependencyGraph {
        nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        edges = List.copyOf(edges);
    }

    /** Направленная связь и признак того, что она замыкает текущий путь DFS. */
    public record Edge(String from, String to, Dependency declaration, boolean backEdge) { }
}
