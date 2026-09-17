import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.project.loader.EnvironmentArgs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.logging.Level

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
