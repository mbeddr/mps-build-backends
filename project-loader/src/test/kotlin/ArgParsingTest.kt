import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.project.loader.EnvironmentArgs
import java.util.logging.Level
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ArgParsingTest {

    @Test
    fun `can parse log level`() {
        val parsed = ArgParser(arrayOf(
            "--log-level", "info")).parseInto(::EnvironmentArgs)

        assertEquals(Level.INFO, parsed.logLevel)
    }

    @Test
    fun `quiet mode is the default`() {
        val parsed = ArgParser(emptyArray()).parseInto(::EnvironmentArgs)

        assertFalse(parsed.verbose)
    }

    @Test
    fun `verbose mode can be enabled`() {
        val parsed = ArgParser(arrayOf("--verbose")).parseInto(::EnvironmentArgs)

        assertTrue(parsed.verbose)
    }

}
