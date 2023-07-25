import de.itemis.mps.gradle.project.loader.EnvironmentKind
import de.itemis.mps.gradle.project.loader.ProjectLoader
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.IdeaEnvironment
import jetbrains.mps.tool.environment.MpsEnvironment
import org.junit.Assert
import org.junit.Test

class EnvironmentKindTest {

    @Test
    fun `loads IDEA environment by default`() {
        lateinit var environmentClass: Class<in Environment>
        ProjectLoader
            .build { }
            .execute { environment -> environmentClass = environment.javaClass }

        Assert.assertTrue(
            "Environment should be IDEA but was $environmentClass",
            IdeaEnvironment::class.java.isAssignableFrom(environmentClass)
        )
    }

    @Test
    fun `can load MPS environment`() {
        lateinit var environmentClass: Class<in Environment>
        ProjectLoader
            .build { environmentKind = EnvironmentKind.MPS }
            .execute { environment -> environmentClass = environment.javaClass }

        Assert.assertTrue(
            "Environment should be MPS but was $environmentClass",
            MpsEnvironment::class.java.isAssignableFrom(environmentClass)
        )
    }
}
