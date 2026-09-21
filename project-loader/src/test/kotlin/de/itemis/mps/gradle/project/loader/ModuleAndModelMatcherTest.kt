package de.itemis.mps.gradle.project.loader

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModuleAndModelMatcherTest {

    @Test
    fun multipleExcludedModules() {
        val matcher = ModuleAndModelMatcher(
            modules = emptyList(), excludeModules = listOf("my.module", "another"),
            models = emptyList(), excludeModels = emptyList()
        )

        assertFalse(matcher.isModuleNameIncluded("my.module"))
        assertFalse(matcher.isModuleNameIncluded("another"))
        assertTrue(matcher.isModuleNameIncluded("foo"))
    }
}
