package de.itemis.mps.gradle.modelcheck

import de.itemis.mps.gradle.project.loader.ModuleAndModelMatcher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
