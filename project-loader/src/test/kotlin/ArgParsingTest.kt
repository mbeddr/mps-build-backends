import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.logging.LogLevel
import de.itemis.mps.gradle.project.loader.EnvironmentArgs
import org.junit.Assert.assertEquals
import org.junit.Test

class ArgParsingTest {

    @Test
    fun `can parse log level`() {
        val parsed = ArgParser(arrayOf(
            "--log-level", "info")).parseInto(::EnvironmentArgs)

        assertEquals(LogLevel.INFO, parsed.logLevel)
    }

}
