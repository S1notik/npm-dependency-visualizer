package dev.npmvisualizer.registry;

import java.io.IOException;
import java.net.URI;

/** Транспорт запроса к реестру; позволяет проверять логику без доступа к сети. */
@FunctionalInterface
public interface RegistryTransport {
    Response get(URI uri) throws IOException, InterruptedException;

    record Response(int status, String body) { }
}
