package com.organization.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("backend-debug");

    public static void logException(String context, Throwable e) {
        if (context == null || context.isEmpty()) {
            context = "exception";
        }
        LOGGER.error("{}: {}", context, e.getMessage(), e);
    }
}
