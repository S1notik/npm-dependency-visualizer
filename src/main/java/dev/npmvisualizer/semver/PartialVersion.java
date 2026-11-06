package dev.npmvisualizer.semver;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Частичная версия или X-range, используемая при разборе ограничения. */
final class PartialVersion {
    static final int MAJOR_PRECISION = 1;
    static final int MINOR_PRECISION = 2;
    private static final int FULL_PRECISION = 3;
    private static final String PART = "(0|[1-9][0-9]*|[xX*])";
    private static final Pattern PATTERN = Pattern.compile("^v?" + PART + "(?:\\." + PART + ")?"
            + "(?:\\." + PART + ")?(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
            + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");
    private final long[] numbers;
    private final int precision;
    private final SemVer lower;

    private PartialVersion(long[] numbers, int precision, SemVer lower) {
        this.numbers = numbers;
        this.precision = precision;
        this.lower = lower;
    }

    static PartialVersion parse(String text) {
        Matcher match = PATTERN.matcher(text);
        if (!match.matches()) {
            throw new IllegalArgumentException("неподдерживаемое ограничение версии: " + text);
        }
        long[] numbers = new long[FULL_PRECISION];
        int precision = readParts(match, numbers);
        if (match.group(4) != null && precision != FULL_PRECISION) {
            throw new IllegalArgumentException("prerelease требует полную версию: " + text);
        }
        String full = numbers[0] + "." + numbers[1] + "." + numbers[2];
        if (match.group(4) != null) {
            full += "-" + match.group(4);
        }
        return new PartialVersion(numbers, precision, SemVer.parse(full));
    }

    private static int readParts(Matcher match, long[] numbers) {
        int precision = 0;
        boolean wildcard = false;
        try {
            for (int i = 1; i <= FULL_PRECISION; i++) {
                String part = match.group(i);
                if (part == null || part.matches("[xX*]")) {
                    wildcard = true;
                } else if (wildcard) {
                    throw new IllegalArgumentException("число после wildcard в версии");
                } else {
                    numbers[i - 1] = Long.parseLong(part);
                    precision++;
                }
            }
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("слишком большое число в ограничении версии", exception);
        }
        return precision;
    }

    int precision() {
        return precision;
    }

    boolean isFull() {
        return precision == FULL_PRECISION;
    }

    SemVer lower() {
        return lower;
    }

    SemVer nextPartial() {
        return precision == MAJOR_PRECISION ? nextMajor() : nextMinor();
    }

    SemVer nextMajor() {
        return release(increment(numbers[0]), 0, 0);
    }

    SemVer nextMinor() {
        return release(numbers[0], increment(numbers[1]), 0);
    }

    SemVer nextPatch() {
        return release(numbers[0], numbers[1], increment(numbers[2]));
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            throw new IllegalArgumentException("переполнение ограничения версии");
        }
        return value + 1;
    }

    private SemVer release(long major, long minor, long patch) {
        return new SemVer(major, minor, patch, List.of());
    }
}
