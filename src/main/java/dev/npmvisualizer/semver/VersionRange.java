package dev.npmvisualizer.semver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Ограничение версий npm из сравнений, диапазонов, wildcard, ~, ^ и ||. */
public final class VersionRange {
    private static final Pattern HYPHEN = Pattern.compile("^(\\S+)\\s+-\\s+(\\S+)$");
    private static final Pattern TOKEN = Pattern.compile("^(<=|>=|<|>|=|~>|~|\\^)?(.+)$");
    private final List<ComparatorSet> alternatives;

    private VersionRange(List<ComparatorSet> alternatives) {
        this.alternatives = alternatives;
    }

    /** Разбирает выражение ограничения версий npm. */
    public static VersionRange parse(String expression) {
        List<ComparatorSet> alternatives = new ArrayList<>();
        for (String part : expression.split("\\|\\|", -1)) {
            alternatives.add(parseSet(part.strip()));
        }
        return new VersionRange(List.copyOf(alternatives));
    }

    private static ComparatorSet parseSet(String text) {
        ComparatorSet set = new ComparatorSet();
        Matcher hyphen = HYPHEN.matcher(text);
        if (hyphen.matches()) {
            addHyphen(set, PartialVersion.parse(hyphen.group(1)), PartialVersion.parse(hyphen.group(2)));
            return set;
        }
        if (text.isEmpty()) {
            return set;
        }
        String normalized = text.replaceAll("(<=|>=|<|>|=|~>|~|\\^)\\s+(?=[v0-9xX*])", "$1");
        for (String token : normalized.split("\\s+")) {
            addToken(set, token);
        }
        return set;
    }

    private static void addHyphen(ComparatorSet set, PartialVersion start, PartialVersion end) {
        set.allowPrerelease(start.lower());
        set.allowPrerelease(end.lower());
        if (start.precision() > 0) {
            set.add(Operation.GTE, start.lower());
        }
        if (end.isFull()) {
            set.add(Operation.LTE, end.lower());
        } else if (end.precision() > 0) {
            set.add(Operation.LT, beforePrereleases(end.nextPartial()));
        }
    }

    private static void addToken(ComparatorSet set, String token) {
        Matcher match = TOKEN.matcher(token);
        if (!match.matches()) {
            throw new IllegalArgumentException("некорректное ограничение версии: " + token);
        }
        String operator = match.group(1) == null ? "" : match.group(1);
        PartialVersion partial = PartialVersion.parse(match.group(2));
        set.allowPrerelease(partial.lower());
        switch (operator) {
            case "", "=" -> addExactOrPartial(set, partial);
            case ">=", ">", "<=", "<" -> addComparison(set, operator, partial);
            case "^" -> addCaret(set, partial);
            case "~", "~>" -> addTilde(set, partial);
            default -> throw new IllegalArgumentException("неизвестный оператор версии: " + operator);
        }
    }

    private static void addExactOrPartial(ComparatorSet set, PartialVersion version) {
        if (version.isFull()) {
            set.add(Operation.EQ, version.lower());
        } else if (version.precision() > 0) {
            set.add(Operation.GTE, version.lower());
            set.add(Operation.LT, beforePrereleases(version.nextPartial()));
        }
    }

    private static void addComparison(ComparatorSet set, String op, PartialVersion version) {
        if (version.precision() == 0) {
            if (op.equals(">") || op.equals("<")) {
                set.impossible = true;
            }
            return;
        }
        switch (op) {
            case ">=" -> set.add(Operation.GTE, version.lower());
            case "<" -> addLessThan(set, version);
            case ">" -> addGreaterThan(set, version);
            case "<=" -> addLessOrEqual(set, version);
            default -> throw new IllegalArgumentException("неизвестный оператор сравнения");
        }
    }

    private static void addLessThan(ComparatorSet set, PartialVersion version) {
        SemVer bound = version.isFull() ? version.lower() : beforePrereleases(version.lower());
        set.add(Operation.LT, bound);
    }

    private static void addGreaterThan(ComparatorSet set, PartialVersion version) {
        boolean full = version.isFull();
        set.add(full ? Operation.GT : Operation.GTE, full ? version.lower() : version.nextPartial());
    }

    private static void addLessOrEqual(ComparatorSet set, PartialVersion version) {
        boolean full = version.isFull();
        set.add(full ? Operation.LTE : Operation.LT,
                full ? version.lower() : beforePrereleases(version.nextPartial()));
    }

    private static void addCaret(ComparatorSet set, PartialVersion version) {
        if (version.precision() == 0) {
            return;
        }
        set.add(Operation.GTE, version.lower());
        SemVer low = version.lower();
        SemVer upper;
        if (low.major() > 0 || version.precision() == PartialVersion.MAJOR_PRECISION) {
            upper = version.nextMajor();
        } else if (low.minor() > 0 || version.precision() == PartialVersion.MINOR_PRECISION) {
            upper = version.nextMinor();
        } else {
            upper = version.nextPatch();
        }
        set.add(Operation.LT, beforePrereleases(upper));
    }

    private static void addTilde(ComparatorSet set, PartialVersion version) {
        if (version.precision() > 0) {
            set.add(Operation.GTE, version.lower());
            SemVer upper = version.precision() == PartialVersion.MAJOR_PRECISION
                    ? version.nextMajor() : version.nextMinor();
            set.add(Operation.LT, beforePrereleases(upper));
        }
    }

    private static SemVer beforePrereleases(SemVer release) {
        return new SemVer(release.major(), release.minor(), release.patch(), List.of("0"));
    }

    /** Проверяет версию с учётом ограничений на prerelease в npm. */
    public boolean includes(SemVer version) {
        return alternatives.stream().anyMatch(alternative -> alternative.matches(version));
    }

    private enum Operation { EQ, GT, GTE, LT, LTE }

    private record Comparison(Operation operation, SemVer bound) {
        private boolean matches(SemVer version) {
            int result = version.compareTo(bound);
            return switch (operation) {
                case EQ -> result == 0;
                case GT -> result > 0;
                case GTE -> result >= 0;
                case LT -> result < 0;
                case LTE -> result <= 0;
            };
        }
    }

    private static final class ComparatorSet {
        private final List<Comparison> comparisons = new ArrayList<>();
        private final List<SemVer> prereleaseBases = new ArrayList<>();
        private boolean impossible;

        private void add(Operation operation, SemVer version) {
            comparisons.add(new Comparison(operation, version));
        }

        private void allowPrerelease(SemVer version) {
            if (version.isPrerelease()) {
                prereleaseBases.add(version);
            }
        }

        private boolean matches(SemVer version) {
            if (impossible || comparisons.stream().anyMatch(comparison -> !comparison.matches(version))) {
                return false;
            }
            return !version.isPrerelease() || prereleaseBases.stream().anyMatch(version::sameBase);
        }
    }
}
