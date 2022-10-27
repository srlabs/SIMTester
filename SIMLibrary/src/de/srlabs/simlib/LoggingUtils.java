package de.srlabs.simlib;

public class LoggingUtils {

    public static String formatDebugMessage(String message) {
        return "[" + getClassName(1) + ", " + getMethodName(1) + "] " + message;
    }

    private static String getMethodName(final int depth) {
        StackTraceElement element = (new Throwable()).getStackTrace()[depth+ 1];
        return element.getMethodName();
    }

    private static String getClassName(final int depth) {
        StackTraceElement element = (new Throwable()).getStackTrace()[depth+ 1];
        return element.getClassName();
    }
}
