package de.itemis.mps.gradle.project.loader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class PluginIdsTest {

    @field:TempDir
    lateinit var folder: File

    @Test
    fun idInLibJar() {
        val pluginDir = folder.resolve("id-in-lib-jar").apply { mkdirs() }
        writeJarWithPluginXml(pluginDir.resolve("lib/myjar.jar"), "<idea-plugin><id>jetbrains.jetpad</id></idea-plugin>")

        assertEquals("jetbrains.jetpad", readPluginId(pluginDir))
    }


    @Test
    fun noDescriptor() {
        val pluginDir = folder.resolve("no-descriptor").apply { mkdirs() }
        assertNull(readPluginId(pluginDir))
    }

    @Test
    fun idInMetaInf() {
        val pluginDir = folder.resolve("id-in-meta-inf").apply { mkdirs() }
        writeTextFile(pluginDir.resolve("META-INF/plugin.xml"), "<idea-plugin><id>jetbrains.jetpad</id></idea-plugin>")

        assertEquals("jetbrains.jetpad", readPluginId(pluginDir))
    }

    @Test
    fun idInLibJarTakesPrecedence() {
        val pluginDir = folder.resolve("lib-jar-precedence").apply { mkdirs() }
        writeJarWithPluginXml(pluginDir.resolve("lib/foo.jar"), "<idea-plugin><id>foo</id></idea-plugin>")
        writeTextFile(pluginDir.resolve("META-INF/plugin.xml"), "<idea-plugin><id>bar</id></idea-plugin>")

        assertEquals("foo", readPluginId(pluginDir))
    }

    @Test
    fun invalidXml() {
        val pluginDir = folder.resolve("invalid-xml").apply { mkdirs() }
        writeJarWithPluginXml(pluginDir.resolve("lib/foo.jar"), "INVALID")

        assertNull(readPluginId(pluginDir))
    }

    private fun writeTextFile(metaInfPluginXml: File, contents: String) {
        metaInfPluginXml.parentFile.mkdirs()
                || throw RuntimeException("Could not create parent directory for $metaInfPluginXml")

        metaInfPluginXml.writeText(contents)
    }

    private fun writeJarWithPluginXml(jarFile: File, contents: String) {
        jarFile.parentFile.mkdirs() || throw RuntimeException("Could not create parent directory for $jarFile")

        JarOutputStream(jarFile.outputStream()).use {
            it.putNextEntry(JarEntry("META-INF/plugin.xml"))
            it.write(contents.toByteArray())
        }
    }
}
