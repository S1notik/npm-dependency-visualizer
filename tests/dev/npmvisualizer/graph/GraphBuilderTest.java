package dev.npmvisualizer.graph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки итеративного DFS, циклов и тестового репозитория. */
class GraphBuilderTest {
    @TempDir
    Path directory;

    @Test
    void detectsCyclesButDoesNotMistakeSharedDependencyForCycle() throws Exception {
        FileGraphRepository cyclic = read("A: B, C\nB: D\nC: D\nD: A\nE:\n");
        DependencyGraph graph = new GraphBuilder().build(cyclic, "A");
        assertEquals(List.of("A", "B", "D", "C"), List.copyOf(graph.nodes().keySet()));
        assertEquals(5, graph.edges().size());
        assertEquals(1, graph.edges().stream().filter(DependencyGraph.Edge::backEdge).count());
        assertTrue(graph.edges().stream().anyMatch(edge -> edge.from().equals("D") && edge.to().equals("A")
                && edge.backEdge()));
        assertFalse(graph.edges().stream().anyMatch(edge -> edge.from().equals("C") && edge.backEdge()));
    }

    @Test
    void handlesSelfCycleAndIsolatedNode() throws Exception {
        DependencyGraph self = new GraphBuilder().build(read("A: A\nB:\n"), "A");
        assertEquals(1, self.nodes().size());
        assertTrue(self.edges().get(0).backEdge());
        DependencyGraph isolated = new GraphBuilder().build(read("A: A\nB:\n"), "B");
        assertEquals(List.of("B"), List.copyOf(isolated.nodes().keySet()));
        assertTrue(isolated.edges().isEmpty());
    }

    @Test
    void handlesLongPathWithoutRecursion() throws Exception {
        int size = 20000;
        PackageRepository repository = new PackageRepository() {
            public PackageInfo root(String name) { return node(0); }
            public PackageInfo resolve(Dependency dependency) { return node(Integer.parseInt(dependency.name())); }
            private PackageInfo node(int index) {
                List<Dependency> dependencies = index + 1 == size ? List.of()
                        : List.of(new Dependency(Integer.toString(index + 1), "", false));
                return new PackageInfo(Integer.toString(index), dependencies);
            }
        };
        DependencyGraph graph = new GraphBuilder().build(repository, "0");
        assertEquals(size, graph.nodes().size());
        assertEquals(size - 1, graph.edges().size());
        assertTrue(graph.edges().stream().noneMatch(DependencyGraph.Edge::backEdge));
    }

    @Test
    void rejectsInvalidRepositoryAndMissingRoot() throws Exception {
        for (String invalid : List.of("a: B\nB:\n", "A: B\n", "A:\nA:\n", "A: B, B\nB:\n", "A: B,\nB:\n")) {
            assertThrows(IOException.class, () -> read(invalid), invalid);
        }
        FileGraphRepository repository = read("# комментарий\n\nA: B\nB:\n");
        assertThrows(IOException.class, () -> repository.root("Z"));
    }

    private FileGraphRepository read(String content) throws IOException {
        Path file = directory.resolve("repo.txt");
        Files.writeString(file, content);
        return FileGraphRepository.read(file);
    }
}
