package org.apache.log4j;

public class Logger {
    public static Logger getLogger(Class<?> clazz) {
        return new Logger();
    }

    public void info(Object message) {
    }
}
