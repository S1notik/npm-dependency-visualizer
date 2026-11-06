package dev.npmvisualizer.semver;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Версия SemVer с правильным сравнением предварительных выпусков. */
public record SemVer(long major, long minor, long patch, List<String> prerelease) implements Comparable<SemVer> {
    private static final Pattern FULL = Pattern.compile(
            "^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");

    public SemVer {
        prerelease = List.copyOf(prerelease);
    }

    /** Разбирает полную версию вида 1.2.3, включая prerelease и build metadata. */
    public static SemVer parse(String value) {
        Matcher matcher = FULL.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("некорректная версия SemVer: " + value);
        }
        try {
            List<String> pre = matcher.group(4) == null ? List.of() : List.of(matcher.group(4).split("\\."));
            validatePrerelease(pre);
            return new SemVer(Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2)),
                    Long.parseLong(matcher.group(3)), pre);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("слишком большое число в версии: " + value, exception);
        }
    }

    private static void validatePrerelease(List<String> identifiers) {
        for (String identifier : identifiers) {
            if (isNumber(identifier) && identifier.length() > 1 && identifier.startsWith("0")) {
                throw new IllegalArgumentException("ведущий ноль в prerelease: " + identifier);
            }
        }
    }

    /** Сообщает, относится ли версия к предварительному выпуску. */
    public boolean isPrerelease() {
        return !prerelease.isEmpty();
    }

    /** Сравнивает только тройку major.minor.patch. */
    public boolean sameBase(SemVer other) {
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    /** Сравнивает версии согласно правилам приоритета SemVer. */
    @Override
    public int compareTo(SemVer other) {
        int result = Long.compare(major, other.major);
        if (result == 0) {
            result = Long.compare(minor, other.minor);
        }
        if (result == 0) {
            result = Long.compare(patch, other.patch);
        }
        return result != 0 ? result : comparePrerelease(other);
    }

    private int comparePrerelease(SemVer other) {
        if (prerelease.isEmpty() || other.prerelease.isEmpty()) {
            return Boolean.compare(prerelease.isEmpty(), other.prerelease.isEmpty());
        }
        int limit = Math.min(prerelease.size(), other.prerelease.size());
        for (int i = 0; i < limit; i++) {
            int result = compareIdentifier(prerelease.get(i), other.prerelease.get(i));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    private int compareIdentifier(String left, String right) {
        boolean leftNumber = isNumber(left);
        boolean rightNumber = isNumber(right);
        if (leftNumber && rightNumber) {
            int length = Integer.compare(left.length(), right.length());
            return length != 0 ? length : left.compareTo(right);
        }
        if (leftNumber != rightNumber) {
            return leftNumber ? -1 : 1;
        }
        return left.compareTo(right);
    }

    private static boolean isNumber(String value) {
        return value.chars().allMatch(character -> character >= '0' && character <= '9');
    }
}
