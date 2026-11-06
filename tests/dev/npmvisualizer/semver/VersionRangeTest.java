package dev.npmvisualizer.semver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки выбора версий и поддерживаемых ограничений npm SemVer. */
class VersionRangeTest {
    @Test
    void matchesCommonRangesAndBoundaries() {
        check("^1.2.3", "1.9.9", "2.0.0");
        check("^0.2.3", "0.2.9", "0.3.0");
        check("^0.0.3", "0.0.3", "0.0.4");
        check("^0.0", "0.0.99", "0.1.0");
        check("^0", "0.99.0", "1.0.0");
        check("~1.2.3", "1.2.99", "1.3.0");
        check("~1", "1.99.0", "2.0.0");
        check(">=1.2 <2", "1.9.9", "2.0.0");
        check(">1.2 <=2.0", "1.3.0", "2.1.0");
        check("1.2 - 2.3", "2.3.99", "2.4.0");
        check("1.x || >=3 <4", "3.1.0", "2.0.0");
        check("1.2", "1.2.8", "1.3.0");
        check("1.2.3+build", "1.2.3+other", "1.2.4");
        check("*", "0.0.0", "1.0.0-beta.1");
        assertFalse(VersionRange.parse(">*").includes(SemVer.parse("100.0.0")));
    }

    @Test
    void comparesExactAndPartialVersionBoundaries() {
        check(">=1.2.3", "1.2.3", "1.2.2");
        check("<1.2.3", "1.2.2", "1.2.3");
        check(">1.2.3", "1.2.4", "1.2.3");
        check("<=1.2.3", "1.2.3", "1.2.4");
        check(">=1.2", "1.2.0", "1.1.99");
        check("<1.2", "1.1.99", "1.2.0");
        check(">1.2", "1.3.0", "1.2.99");
        check("<=1.2", "1.2.99", "1.3.0");
    }

    @Test
    void prereleasesNeedMatchingExplicitBase() {
        VersionRange range = VersionRange.parse("^1.2.3-beta.2");
        assertTrue(range.includes(SemVer.parse("1.2.3-beta.3")));
        assertTrue(range.includes(SemVer.parse("1.5.0")));
        assertFalse(range.includes(SemVer.parse("1.2.3-beta.1")));
        assertFalse(range.includes(SemVer.parse("1.2.4-beta.1")));
        assertFalse(VersionRange.parse(">=1.0.0").includes(SemVer.parse("2.0.0-rc.1")));
        assertFalse(VersionRange.parse("1.2.x >=1.3.0-alpha").includes(SemVer.parse("1.3.0-alpha")));
        assertFalse(VersionRange.parse("^1.2.3 >=2.0.0-alpha").includes(SemVer.parse("2.0.0-alpha")));
        assertFalse(VersionRange.parse("<1.2 >=1.2.0-alpha").includes(SemVer.parse("1.2.0-alpha")));
    }

    @Test
    void comparesNumericAndTextPrereleaseIdentifiers() {
        List<String> order = List.of("1.0.0-alpha", "1.0.0-alpha.1", "1.0.0-alpha.beta", "1.0.0-beta",
                "1.0.0-beta.2", "1.0.0-beta.11", "1.0.0-rc.1", "1.0.0");
        for (int i = 0; i < order.size() - 1; i++) {
            assertTrue(SemVer.parse(order.get(i)).compareTo(SemVer.parse(order.get(i + 1))) < 0);
        }
        assertEquals(0, SemVer.parse("1.2.3+one").compareTo(SemVer.parse("1.2.3+two")));
    }

    @Test
    void choosesHighestMatchingVersionOrExplicitTag() {
        Map<String, Object> versions = new LinkedHashMap<>();
        for (String version : List.of("1.0.0", "1.8.0", "1.9.0-beta.1", "2.0.0", "not-semver")) {
            versions.put(version, Map.of());
        }
        Map<String, String> tags = Map.of("latest", "2.0.0", "next", "1.9.0-beta.1");
        assertEquals("1.8.0", VersionSelector.select(versions, tags, "^1.0.0"));
        assertEquals("1.9.0-beta.1", VersionSelector.select(versions, tags, "next"));
        assertEquals("2.0.0", VersionSelector.select(versions, tags, "latest"));
        assertThrows(IllegalArgumentException.class, () -> VersionSelector.select(versions, tags, "^3.0.0"));
    }

    @Test
    void usesStableTieBreakForEquivalentBuildMetadata() {
        Map<String, Object> versions = new LinkedHashMap<>();
        versions.put("1.2.3+two", Map.of());
        versions.put("1.2.3+one", Map.of());
        assertEquals("1.2.3+two", VersionSelector.select(versions, Map.of(), "1.2.3"));
    }

    @Test
    void rejectsInvalidAndUnsupportedSpecifications() {
        for (String text : List.of("file:../lib", "git+https://example/repo", "1.x.3", "1.02.3", "^oops")) {
            assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(text), text);
        }
        assertThrows(IllegalArgumentException.class, () -> SemVer.parse("1.0.0-01"));
    }

    private void check(String range, String accepted, String rejected) {
        VersionRange parsed = VersionRange.parse(range);
        assertTrue(parsed.includes(SemVer.parse(accepted)), range + " accepts " + accepted);
        assertFalse(parsed.includes(SemVer.parse(rejected)), range + " rejects " + rejected);
    }
}
