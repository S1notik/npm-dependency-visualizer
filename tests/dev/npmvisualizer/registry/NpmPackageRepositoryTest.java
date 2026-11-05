package dev.npmvisualizer.registry;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import dev.npmvisualizer.graph.Dependency;
import dev.npmvisualizer.graph.DependencyGraph;
import dev.npmvisualizer.graph.GraphBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки версий, npm-алиасов, необязательных зависимостей и кэша. */
class NpmPackageRepositoryTest {
    @Test
    void buildsVersionedGraphAndCachesDocuments() throws Exception {
        Map<String, Integer> requests = new HashMap<>();
        NpmRegistryClient client = new NpmRegistryClient(uri -> {
            requests.merge(uri.getRawPath(), 1, Integer::sum);
            return new RegistryTransport.Response(200, response(uri));
        });
        DependencyGraph graph = new GraphBuilder().build(new NpmPackageRepository("https://r.example", client), "root");
        assertTrue(graph.nodes().containsKey("root@1.0.0"));
        assertTrue(graph.nodes().containsKey("lib@1.9.0"));
        assertTrue(graph.nodes().containsKey("lib@2.1.0"));
        assertTrue(graph.nodes().containsKey("@scope/pkg@2.1.9"));
        assertEquals(1, requests.get("/lib"));
        assertEquals(1, requests.get("/%40scope%2Fpkg"));
        assertTrue(graph.edges().stream().anyMatch(edge -> edge.declaration().optional()
                && edge.declaration().name().equals("lib") && edge.to().equals("lib@2.1.0")));
        assertTrue(graph.edges().stream().anyMatch(edge -> edge.declaration().name().equals("alias")
                && edge.to().equals("@scope/pkg@2.1.9")));
        assertTrue(graph.edges().stream().anyMatch(edge -> edge.to().equals("root@1.0.0") && edge.backEdge()));
        assertFalse(graph.nodes().keySet().stream().anyMatch(name -> name.startsWith("dev-only@")));
    }

    @Test
    void reportsUnsupportedOrUnmatchedVersionInsteadOfInventingGraph() {
        NpmRegistryClient client = new NpmRegistryClient(uri -> new RegistryTransport.Response(200,
                "{\"versions\":{\"1.0.0\":{\"name\":\"lib\",\"version\":\"1.0.0\"}}}"));
        NpmPackageRepository repository = new NpmPackageRepository("https://r.example", client);
        assertThrows(IOException.class, () -> repository.resolve(new Dependency("lib", "^2", false)));
        assertThrows(IOException.class, () -> repository.resolve(new Dependency("lib", "file:../lib", false)));
    }

    private String response(URI uri) {
        return switch (uri.getRawPath()) {
            case "/root/latest" -> """
                    {"name":"root","version":"1.0.0",
                     "dependencies":{"alias":"npm:@scope/pkg@~2.1.0", "lib":"^1.0.0",
                                     "old":"npm:lib@^1.0.0"},
                     "optionalDependencies":{"lib":"^2.0.0"}, "devDependencies":{"dev-only":"*"}}
                    """;
            case "/lib" -> """
                    {"dist-tags":{"latest":"2.1.0"}, "versions":{
                    "1.0.0":{"name":"lib","version":"1.0.0"},
                    "1.9.0":{"name":"lib","version":"1.9.0","dependencies":{"root":"^1"}},
                    "2.1.0":{"name":"lib","version":"2.1.0"}}}
                    """;
            case "/%40scope%2Fpkg" -> """
                    {"versions":{"2.1.9":{"name":"@scope/pkg","version":"2.1.9"},
                    "2.2.0":{"name":"@scope/pkg","version":"2.2.0"}}}
                    """;
            case "/root" -> """
                    {"versions":{"1.0.0":{"name":"root","version":"1.0.0"}}}
                    """;
            default -> throw new AssertionError("Неожиданный URL: " + uri);
        };
    }
}
