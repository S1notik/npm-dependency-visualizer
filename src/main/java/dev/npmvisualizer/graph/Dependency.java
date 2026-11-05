package dev.npmvisualizer.graph;

/** Прямое объявление зависимости в источнике данных. */
public record Dependency(String name, String requirement, boolean optional) { }
