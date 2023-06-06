package de.itemis.mps.gradle.logging

/**
 * Auto-detects the logging implementation that is available on the classpath. MPS 2022.2 moved to java.util.logging
 * whereas earlier versions use log4j.
 */
public fun detectLogging(): Logging =
    // The real log4j implementation contains HTMLLayout class (as opposed to log4j-over-slf4j which doesn't).
    if (isClassPresent("org.apache.log4j.HTMLLayout")) LoggingLog4j() else LoggingJul()

private fun isClassPresent(name: String): Boolean = try {
    Class.forName(name)
    true
} catch (e: ClassNotFoundException) {
    false
}
