package io.chronicle.platform;

public final class Text {
    private Text() {}

    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
