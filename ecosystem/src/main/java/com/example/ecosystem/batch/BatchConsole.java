package com.example.ecosystem.batch;

public final class BatchConsole {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";

    private static final boolean COLOR_ENABLED = isColorEnabled();

    private BatchConsole() {
    }

    private static boolean isColorEnabled() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        return Boolean.parseBoolean(System.getProperty("batch.console.color", "true"));
    }

    public static void header(String title) {
        String rule = "=".repeat(72);
        println("");
        println(style(CYAN + BOLD, rule));
        println(style(CYAN + BOLD, " " + title));
        println(style(CYAN + BOLD, rule));
    }

    public static void footer() {
        println(style(CYAN + BOLD, "=".repeat(72)));
        println("");
    }

    public static void line(String text) {
        println(text);
    }

    public static void tag(String tag, String message) {
        println(formatTag(tagColor(tag), tag, message));
    }

    public static void tagf(String tag, String format, Object... args) {
        tag(tag, String.format(format, args));
    }

    public static void metric(String label, String value) {
        println(String.format(" %s: %s",
                style(DIM, pad(label)),
                style(GREEN + BOLD, value)));
    }

    public static void error(String tag, String message) {
        println(formatTag(RED + BOLD, tag, message));
    }

    public static void errorf(String tag, String format, Object... args) {
        error(tag, String.format(format, args));
    }

    public static String highlight(String text) {
        return style(YELLOW + BOLD, text);
    }

    public static String success(String text) {
        return style(GREEN + BOLD, text);
    }

    private static String formatTag(String tagStyle, String tag, String message) {
        return style(tagStyle, "[" + tag + "]") + " " + message;
    }

    private static String tagColor(String tag) {
        return switch (tag) {
            case "SEED" -> YELLOW + BOLD;
            case "BATCH" -> BLUE + BOLD;
            case "BENCHMARK" -> MAGENTA + BOLD;
            default -> BOLD;
        };
    }

    private static String pad(String label) {
        return String.format("%-20s", label);
    }

    private static void println(String text) {
        System.out.println(text);
    }

    private static String style(String codes, String text) {
        if (!COLOR_ENABLED) {
            return text;
        }
        return codes + text + RESET;
    }
}
