package dev.npmvisualizer.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import dev.npmvisualizer.json.JsonParser;

/** Извлекает группы зависимостей из манифеста конкретной версии npm-пакета. */
public class ManifestParser {
    private static final List<String> GROUPS = List.of(
            "dependencies", "optionalDependencies", "peerDependencies", "devDependencies"
    );

    /** Разбирает JSON и возвращает метаданные с отсортированными именами зависимостей. */
    public PackageMetadata parse(String json) {
        return parseObject(JsonParser.parse(json));
    }

    /** Извлекает данные уже разобранного объекта манифеста. */
    public PackageMetadata parseObject(Object parsed) {
        if (!(parsed instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("корнем JSON должен быть объект");
        }
        String name = requiredString(object, "name");
        String version = requiredString(object, "version");
        Map<String, Map<String, String>> groups = new LinkedHashMap<>();
        for (String group : GROUPS) {
            Map<String, String> dependencies = readGroup(object, group);
            if (!dependencies.isEmpty()) {
                groups.put(group, dependencies);
            }
        }
        return new PackageMetadata(name, version, Collections.unmodifiableMap(groups));
    }

    private String requiredString(Map<?, ?> object, String field) {
        if (!(object.get(field) instanceof String value) || value.isBlank()) {
            throw new IllegalArgumentException("поле " + field + " должно быть непустой строкой");
        }
        return value;
    }

    private Map<String, String> readGroup(Map<?, ?> object, String field) {
        if (!object.containsKey(field)) {
            return Map.of();
        }
        if (!(object.get(field) instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException("поле " + field + " должно быть объектом");
        }
        Map<String, String> result = new TreeMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String name) || !(entry.getValue() instanceof String version)) {
                throw new IllegalArgumentException("имя и версия в " + field + " должны быть строками");
            }
            result.put(name, version);
        }
        return Collections.unmodifiableMap(result);
    }
}
