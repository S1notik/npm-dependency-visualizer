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
            if (isBetter(version, candidate, best, selected)) {
                best = version;
                selected = candidate;
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("нет опубликованной версии для ограничения: " + requirement);
        }
        return selected;
    }

    private static boolean isBetter(SemVer version, String candidate, SemVer best, String selected) {
        if (best == null) {
            return true;
        }
        int comparison = version.compareTo(best);
        return comparison > 0 || (comparison == 0 && candidate.compareTo(selected) > 0);
    }

    private static SemVer parseCandidate(String value) {
        try {
            return SemVer.parse(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
