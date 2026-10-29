package dev.npmvisualizer.config;

public class AppConfig {
    private final String packageName;
    private final String repository;
    private final boolean testMode;
    private final boolean asciiTree;

    public AppConfig(String packageName, String repository,
                     boolean testMode, boolean asciiTree) {
        this.packageName = packageName;
        this.repository = repository;
        this.testMode = testMode;
        this.asciiTree = asciiTree;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public String getRepository() {
        return repository;
    }

    public boolean isAsciiTree() {
        return asciiTree;
    }
}
