package de.srlabs.simlib;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LoggingUtils {

    private static Method m;

    static {
        try {
            m = Throwable.class.getDeclaredMethod("getStackTraceElement", int.class);
            m.setAccessible(true);
        } catch (NoSuchMethodException e) { // this should never happen really
            e.printStackTrace(System.err);
        }
    }
    
    public static String formatDebugMessage(String message) {
            String result = "[" + getClassName(1) + ", " + getMethodName(1) + "] " + message;
            return result;
    }

    private static String getMethodName(final int depth) {
        try {
            StackTraceElement element = (StackTraceElement) m.invoke(new Throwable(), depth + 1);
            return element.getMethodName();
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static String getClassName(final int depth) {
        try {
            StackTraceElement element = (StackTraceElement) m.invoke(new Throwable(), depth + 1);
            return element.getClassName();
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }
}
