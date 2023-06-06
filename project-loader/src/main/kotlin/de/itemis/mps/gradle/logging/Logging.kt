package de.itemis.mps.gradle.logging

import org.apache.commons.logging.Log

public interface Logging {
    /**
     * Configures `de.itemis.mps` logger to log to [System.err] with the given [level]. May be called multiple times.
     */
    public fun configure(level: LogLevel)
    public fun getLogger(name: String): Log
}
