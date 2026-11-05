package dev.npmvisualizer.registry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import dev.npmvisualizer.graph.Dependency;
import dev.npmvisualizer.graph.PackageInfo;
import dev.npmvisualizer.graph.PackageRepository;
import dev.npmvisualizer.semver.VersionSelector;

/** Источник узлов графа из npm-реестра с кэшем документов версий. */
public class NpmPackageRepository implements PackageRepository {
    private final String repository;
    private final NpmRegistryClient client;
    private final Map<String, PackageVersions> cache = new HashMap<>();

    /** Создаёт источник для заданного реестра и HTTP-клиента. */
    public NpmPackageRepository(String repository, NpmRegistryClient client) {
        this.repository = repository;
        this.client = client;
    }

    /** Загружает корневой пакет по тегу latest. */
    @Override
    public PackageInfo root(String name) throws IOException, InterruptedException {
        return asNode(client.fetch(repository, name));
    }

    /** Выбирает опубликованную версию, подходящую ограничению зависимости. */
    @Override
    public PackageInfo resolve(Dependency dependency) throws IOException, InterruptedException {
        Target target = Target.from(dependency);
        PackageVersions document = versions(target.name());
        try {
            String selected = VersionSelector.select(document.versions(), document.tags(), target.requirement());
            PackageMetadata metadata = new ManifestParser().parseObject(document.versions().get(selected));
            if (!metadata.name().equals(target.name()) || !metadata.version().equals(selected)) {
                throw new IllegalArgumentException("имя или версия манифеста не совпадает с выбранным пакетом");
            }
            return asNode(metadata);
        } catch (IllegalArgumentException exception) {
            throw new IOException("не удалось разрешить " + dependency.name() + " ("
                    + dependency.requirement() + "): " + exception.getMessage(), exception);
        }
    }

    private PackageVersions versions(String name) throws IOException, InterruptedException {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        String json = client.fetchVersions(repository, name);
        try {
            PackageVersions document = PackageVersions.parse(json);
            cache.put(name, document);
            return document;
        } catch (IllegalArgumentException exception) {
            throw new IOException("некорректный документ версий " + name + ": " + exception.getMessage(), exception);
        }
    }

    private PackageInfo asNode(PackageMetadata metadata) {
        Map<String, Dependency> dependencies = new TreeMap<>();
        addGroup(dependencies, metadata, "dependencies", false);
        addGroup(dependencies, metadata, "optionalDependencies", true);
        return new PackageInfo(metadata.name() + "@" + metadata.version(), new ArrayList<>(dependencies.values()));
    }

    private void addGroup(Map<String, Dependency> result, PackageMetadata metadata, String group, boolean optional) {
        metadata.groups().getOrDefault(group, Map.of()).forEach((name, requirement) ->
                result.put(name, new Dependency(name, requirement, optional)));
    }

    private record Target(String name, String requirement) {
        private static Target from(Dependency dependency) {
            String requirement = dependency.requirement().strip();
            if (!requirement.startsWith("npm:")) {
                return new Target(dependency.name(), requirement);
            }
            String alias = requirement.substring(4);
            int delimiter = alias.indexOf('@', alias.startsWith("@") ? 1 : 0);
            String name = delimiter < 0 ? alias : alias.substring(0, delimiter);
            String spec = delimiter < 0 ? "latest" : alias.substring(delimiter + 1);
            if (name.isEmpty() || spec.isEmpty()) {
                throw new IllegalArgumentException("некорректный npm-алиас: " + requirement);
            }
            return new Target(name, spec);
        }
    }
}
