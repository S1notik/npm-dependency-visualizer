package dev.npmvisualizer.registry;

import java.util.Map;

/** Имя, версия и сгруппированные прямые зависимости версии пакета. */
public record PackageMetadata(String name, String version, Map<String, Map<String, String>> groups) { }
