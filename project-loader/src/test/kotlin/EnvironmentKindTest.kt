import de.itemis.mps.gradle.project.loader.EnvironmentKind
import de.itemis.mps.gradle.project.loader.ProjectLoader
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.IdeaEnvironment
import jetbrains.mps.tool.environment.MpsEnvironment
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnvironmentKindTest {

    @Test
    fun `loads IDEA environment by default`() {
        lateinit var environmentClass: Class<in Environment>
        ProjectLoader
            .build { }
            .execute { environment -> environmentClass = environment.javaClass }

        assertTrue(IdeaEnvironment::class.java.isAssignableFrom(environmentClass)) {
            "Environment should be IDEA but was $environmentClass"
        }
    }

    @Test
    fun `can load MPS environment`() {
        lateinit var environmentClass: Class<in Environment>
        ProjectLoader
            .build { environmentKind = EnvironmentKind.MPS }
            .execute { environment -> environmentClass = environment.javaClass }

        assertTrue(MpsEnvironment::class.java.isAssignableFrom(environmentClass)) {
            "Environment should be MPS but was $environmentClass"
        }
    }
}
