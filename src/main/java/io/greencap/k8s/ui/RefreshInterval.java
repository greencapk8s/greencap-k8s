package io.greencap.k8s.ui;

enum RefreshInterval {
    NONE("No auto refresh", 0),
    FIVE_SECONDS("5 seconds", 5),
    TEN_SECONDS("10 seconds", 10),
    THIRTY_SECONDS("30 seconds", 30),
    ONE_MINUTE("1 minute", 60);

    private final String label;
    private final int seconds;

    RefreshInterval(String label, int seconds) {
        this.label = label;
        this.seconds = seconds;
    }

    String getLabel() { return label; }
    int getSeconds() { return seconds; }
    boolean isActive() { return seconds > 0; }

    static RefreshInterval fromSeconds(int seconds) {
        for (RefreshInterval interval : values()) {
            if (interval.seconds == seconds) return interval;
        }
        return NONE;
    }
}
