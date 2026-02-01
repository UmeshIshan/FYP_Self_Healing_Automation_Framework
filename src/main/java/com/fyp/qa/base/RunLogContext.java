package com.fyp.qa.base;

import java.util.function.Consumer;

public final class RunLogContext {

    private static final ThreadLocal<Consumer<String>> LOGGER = new ThreadLocal<>();

    private RunLogContext() {}

    public static void set(Consumer<String> logger) {
        LOGGER.set(logger);
    }

    public static void clear() {
        LOGGER.remove();
    }

    public static void log(String message) {
        Consumer<String> logger = LOGGER.get();
        if (logger != null) {
            logger.accept(message);
        }
    }
}
