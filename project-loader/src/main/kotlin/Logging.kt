import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout

public enum class LogLevel {
    ALL,
    INFO,
    WARN,
    ERROR,
    OFF
}

private fun LogLevel.toLog4j(): Level =
    when (this) {
        LogLevel.ALL -> Level.ALL
        LogLevel.INFO -> Level.INFO
        LogLevel.WARN -> Level.WARN
        LogLevel.ERROR -> Level.ERROR
        LogLevel.OFF -> Level.OFF
    }

/**
 * Adds a [ConsoleAppender] to the `de.itemis.mps` logger if [logLevel] is anything but [LogLevel.OFF]. The appender is
 * configured with the corresponding [threshold][ConsoleAppender.threshold].
 */
public fun configureLogging(logLevel: LogLevel) {
    val logger = Logger.getLogger("de.itemis.mps")
    if (logLevel != LogLevel.OFF) {
        val appender = ConsoleAppender(SimpleLayout(), "System.err")
        appender.threshold = logLevel.toLog4j()
        logger.addAppender(appender)
    }
}
