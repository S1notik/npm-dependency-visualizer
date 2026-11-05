package dev.npmvisualizer.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.npmvisualizer.json.JsonParser;

/** Теги и манифесты версий одного пакета из документа npm-реестра. */
record PackageVersions(Map<String, String> tags, Map<String, Object> versions) {
    static PackageVersions parse(String json) {
        Object result = JsonParser.parse(json);
        if (!(result instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("документ версий должен быть JSON-объектом");
        }
        Map<String, Object> versions = readObject(object.get("versions"), "versions");
        Map<String, Object> rawTags = object.containsKey("dist-tags")
                ? readObject(object.get("dist-tags"), "dist-tags") : Map.of();
        Map<String, String> tags = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : rawTags.entrySet()) {
            if (!(entry.getValue() instanceof String version)) {
                throw new IllegalArgumentException("значение тега должно быть строкой: " + entry.getKey());
            }
            tags.put(entry.getKey(), version);
        }
        return new PackageVersions(tags, versions);
    }

    private static Map<String, Object> readObject(Object value, String name) {
        if (!(value instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("поле " + name + " должно быть объектом");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : object.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("имена полей в " + name + " должны быть строками");
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}
