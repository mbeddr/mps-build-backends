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
        val parsed = ArgParser(
            arrayOf(
                "--log-level", "info"
            )
        ).parseInto(::EnvironmentArgs)

        assertEquals(Level.INFO, parsed.logLevel)
    }
}
