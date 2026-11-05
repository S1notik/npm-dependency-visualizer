package dev.npmvisualizer.registry;

import java.io.IOException;
import java.net.URI;

/** Транспорт запроса к реестру; позволяет проверять логику без доступа к сети. */
@FunctionalInterface
public interface RegistryTransport {
    /** Выполняет GET-запрос и возвращает код HTTP и тело ответа. */
    Response get(URI uri) throws IOException, InterruptedException;

    /** Данные HTTP-ответа, необходимые клиенту реестра. */
    record Response(int status, String body) { }
}
