package dev.npmvisualizer.semver;

import java.util.Map;

/** Выбирает опубликованную версию по тегу или поддерживаемому диапазону npm. */
public final class VersionSelector {
    private VersionSelector() { }

    /** Находит версию с наибольшим приоритетом, удовлетворяющую ограничению. */
    public static String select(Map<String, ?> versions, Map<String, String> tags, String requirement) {
        String spec = requirement.strip();
        if (tags.containsKey(spec)) {
            String selected = tags.get(spec);
            if (!versions.containsKey(selected)) {
                throw new IllegalArgumentException("тег " + spec + " указывает на отсутствующую версию " + selected);
            }
            return selected;
        }
        VersionRange range = VersionRange.parse(spec);
        String selected = null;
        SemVer best = null;
        for (String candidate : versions.keySet()) {
            SemVer version = parseCandidate(candidate);
            if (version == null || !range.includes(version)) {
                continue;
            }
            if (best == null || version.compareTo(best) > 0
                    || (version.compareTo(best) == 0 && candidate.compareTo(selected) > 0)) {
                best = version;
                selected = candidate;
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("нет опубликованной версии для ограничения: " + requirement);
        }
        return selected;
    }

    private static SemVer parseCandidate(String value) {
        try {
            return SemVer.parse(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
