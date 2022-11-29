import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.project.loader.Args
import org.junit.Assert.assertEquals
import org.junit.Test

class ArgParsingTest {

    @Test
    fun `can parse log level`() {
        val parsed = ArgParser(arrayOf(
            "--project", "irrelevant",
            "--log-level", "info")).parseInto(::Args)

        assertEquals(LogLevel.INFO, parsed.logLevel)
    }

}
