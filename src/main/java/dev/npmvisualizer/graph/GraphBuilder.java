package dev.npmvisualizer.graph;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Строит достижимый направленный граф итеративным обходом в глубину. */
public class GraphBuilder {
    /** Обходит зависимости без рекурсии и отмечает обратные рёбра циклов. */
    public DependencyGraph build(PackageRepository repository, String rootName)
            throws IOException, InterruptedException {
        PackageInfo root = repository.root(rootName);
        Map<String, PackageInfo> nodes = new LinkedHashMap<>();
        List<DependencyGraph.Edge> edges = new ArrayList<>();
        Set<String> active = new HashSet<>();
        Deque<Frame> stack = new ArrayDeque<>();
        enter(root, nodes, active, stack);
        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (!frame.hasNext()) {
                active.remove(frame.node.id());
                stack.pop();
                continue;
            }
            Dependency declaration = frame.next();
            PackageInfo target = repository.resolve(declaration);
            boolean backEdge = active.contains(target.id());
            edges.add(new DependencyGraph.Edge(frame.node.id(), target.id(), declaration, backEdge));
            if (!nodes.containsKey(target.id())) {
                enter(target, nodes, active, stack);
            }
        }
        return new DependencyGraph(nodes, edges);
    }

    private void enter(PackageInfo node, Map<String, PackageInfo> nodes,
                       Set<String> active, Deque<Frame> stack) {
        nodes.put(node.id(), node);
        active.add(node.id());
        stack.push(new Frame(node));
    }

    private static final class Frame {
        private final PackageInfo node;
        private int index;

        private Frame(PackageInfo node) {
            this.node = node;
        }

        private boolean hasNext() {
            return index < node.dependencies().size();
        }

        private Dependency next() {
            return node.dependencies().get(index++);
        }
    }
}
