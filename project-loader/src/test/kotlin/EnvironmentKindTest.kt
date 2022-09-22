import de.itemis.mps.gradle.project.loader.EnvironmentKind
import de.itemis.mps.gradle.project.loader.executeWithEnvironment
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.MpsEnvironment
import org.junit.Assert
import org.junit.Test
import java.io.File

class EnvironmentKindTest {

    @Test
    fun `can load MPS environment`() {
        lateinit var environmentClass: Class<in Environment>
        executeWithEnvironment(EnvironmentKind.MPS) { environment ->
            environmentClass = environment.javaClass
        }

        Assert.assertTrue(
            "Environment should be MPS but was $environmentClass",
            MpsEnvironment::class.java.isAssignableFrom(environmentClass)
        )
    }

}
