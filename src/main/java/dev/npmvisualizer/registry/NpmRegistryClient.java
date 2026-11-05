package dev.npmvisualizer.registry;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Получает манифест npm-пакета по тегу latest из указанного реестра. */
public class NpmRegistryClient {
    private final RegistryTransport transport;

    /** Создаёт клиент, использующий стандартный HTTP-транспорт. */
    public NpmRegistryClient() {
        this(new HttpRegistryTransport());
    }

    /** Создаёт клиент с заданным транспортом. */
    public NpmRegistryClient(RegistryTransport transport) {
        this.transport = transport;
    }

    /** Запрашивает и разбирает манифест указанного пакета. */
    public PackageMetadata fetch(String repository, String packageName) throws IOException, InterruptedException {
        String body = fetchBody(repository, packageName, true);
        try {
            return new ManifestParser().parse(body);
        } catch (IllegalArgumentException exception) {
            throw new IOException("некорректный манифест npm: " + exception.getMessage(), exception);
        }
    }

    /** Запрашивает документ со всеми версиями пакета для выбора зависимости. */
    public String fetchVersions(String repository, String packageName) throws IOException, InterruptedException {
        return fetchBody(repository, packageName, false);
    }

    private String fetchBody(String repository, String packageName, boolean latest)
            throws IOException, InterruptedException {
        URI uri = createPackageUri(repository, packageName, latest);
        RegistryTransport.Response response;
        try {
            response = transport.get(uri);
        } catch (IOException exception) {
            throw new IOException("не удалось обратиться к реестру: " + exception.getMessage(), exception);
        }
        if (response.status() != 200) {
            throw new IOException("реестр вернул HTTP " + response.status() + " для пакета " + packageName);
        }
        return response.body();
    }

    private URI createPackageUri(String repository, String packageName, boolean latest) {
        URI base = URI.create(repository);
        if (base.getRawQuery() != null || base.getRawFragment() != null) {
            throw new IllegalArgumentException("URL репозитория не должен содержать query или fragment");
        }
        String prefix = base.toASCIIString();
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        String encodedName = URLEncoder.encode(packageName, StandardCharsets.UTF_8).replace("+", "%20");
        return URI.create(prefix + "/" + encodedName + (latest ? "/latest" : ""));
    }
}
