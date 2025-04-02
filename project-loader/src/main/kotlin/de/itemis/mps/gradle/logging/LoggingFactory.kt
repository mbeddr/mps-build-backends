package de.itemis.mps.gradle.logging

// java.util.logging is the only possible implementation since roughly version 2022.2.
public fun detectLogging(): Logging = LoggingJul()
