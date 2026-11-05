package dev.npmvisualizer.registry;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки адреса запроса, манифеста и ошибок реестра. */
class NpmRegistryClientTest {
    private static final String MANIFEST = """
            {"name":"@scope/demo","version":"2.1.0",
             "description":"text with \\"dependencies\\" and }",
             "other":{"dependencies":{"not-direct":"1"},"values":[null,true,3.5]},
             "dependencies":{"z":"npm:other@^2","a":"\\u005e1.0.0"},
             "optionalDependencies":{"optional":"~2"},
             "peerDependencies":{"host":"^3"},
             "devDependencies":{"tool":"^4"}}
            """;

    @Test
    void requestsLatestAndReturnsAllDeclaredGroups() throws Exception {
        AtomicReference<URI> requested = new AtomicReference<>();
        NpmRegistryClient client = new NpmRegistryClient(uri -> {
            requested.set(uri);
            return new RegistryTransport.Response(200, MANIFEST);
        });
        PackageMetadata result = client.fetch("https://registry.example/npm/", "@scope/demo");
        assertEquals("https://registry.example/npm/%40scope%2Fdemo/latest", requested.get().toASCIIString());
        assertEquals("@scope/demo", result.name());
        assertEquals("2.1.0", result.version());
        assertEquals(List.of("dependencies", "optionalDependencies", "peerDependencies", "devDependencies"),
                List.copyOf(result.groups().keySet()));
        assertEquals(Map.of("a", "^1.0.0", "z", "npm:other@^2"), result.groups().get("dependencies"));
        assertEquals(List.of("a", "z"), List.copyOf(result.groups().get("dependencies").keySet()));
        assertEquals("~2", result.groups().get("optionalDependencies").get("optional"));
    }

    @Test
    void handlesMissingOrEmptyDependencyGroups() throws Exception {
        NpmRegistryClient client = new NpmRegistryClient(uri -> new RegistryTransport.Response(200,
                "{\"name\":\"empty\",\"version\":\"1.0.0\",\"dependencies\":{}}"));
        assertTrue(client.fetch("http://registry.example", "empty").groups().isEmpty());
    }

    @Test
    void rejectsHttpAndTransportErrors() {
        NpmRegistryClient notFound = new NpmRegistryClient(uri -> new RegistryTransport.Response(404, ""));
        IOException http = assertThrows(IOException.class, () -> notFound.fetch("https://registry.example", "x"));
        assertTrue(http.getMessage().contains("404"));
        NpmRegistryClient offline = new NpmRegistryClient(uri -> { throw new IOException("offline"); });
        IOException error = assertThrows(IOException.class, () -> offline.fetch("https://registry.example", "x"));
        assertTrue(error.getMessage().contains("offline"));
    }

    @Test
    void rejectsMalformedManifestsAndInvalidRegistryUrl() {
        for (String json : List.of("not json", "[]", "{}", "{\"name\":\"x\",\"version\":\"1\",\"dependencies\":null}",
                "{\"name\":\"x\",\"version\":\"1\",\"dependencies\":{\"dep\":2}}")) {
            NpmRegistryClient client = new NpmRegistryClient(uri -> new RegistryTransport.Response(200, json));
            assertThrows(IOException.class, () -> client.fetch("https://registry.example", "x"), json);
        }
        NpmRegistryClient client = new NpmRegistryClient(uri -> new RegistryTransport.Response(200, MANIFEST));
        assertThrows(IllegalArgumentException.class, () -> client.fetch("https://registry.example/?q=x", "x"));
        assertThrows(IllegalArgumentException.class, () -> client.fetch("https://registry.example/#part", "x"));
    }
}
