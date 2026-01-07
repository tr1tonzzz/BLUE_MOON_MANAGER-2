package vn.bluemoon.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application logger utility
 */
public class AppLogger {
    private static final Logger logger = LoggerFactory.getLogger(AppLogger.class);

    public static void info(String message) {
        logger.info(message);
    }

    public static void info(String message, Object... args) {
        logger.info(message, args);
    }

    public static void error(String message) {
        logger.error(message);
    }

    public static void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void debug(String message) {
        logger.debug(message);
    }
}

















