import de.itemis.mps.gradle.project.loader.ModuleAndModelMatcher
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ModuleAndModelMatcherTest {

    @Test
    fun multipleExcludedModules() {
        val matcher = ModuleAndModelMatcher(modules = emptyList(), excludeModules = listOf("my.module", "another"),
            models = emptyList(), excludeModels = emptyList())

        assertFalse(matcher.isModuleNameIncluded("my.module"))
        assertFalse(matcher.isModuleNameIncluded("another"))
        assertTrue(matcher.isModuleNameIncluded("foo"))
    }
}
