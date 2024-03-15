package de.itemis.mps.gradle.project.loader

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

private const val Macro1 = "\${macro.path1}"
private const val Macro2 = "\${macro.path2}"
private val AllMacros = listOf(
    Macro("macro.path1", "/path/to/macro1"),
    Macro("macro.path2", "/path/to/macro2")
)

class ProjectLibrariesTest {

    @JvmField @Rule
    val folder = TemporaryFolder()

    @Test
    fun noLibrariesXml() {
        assertEquals(emptyList<String>(), getLibraries())
    }

    @Test
    fun emptyLibrariesXml() {
        writeFile(text = "<map/>")
        assertEquals(emptyList<String>(), getLibraries())
    }

    @Test
    fun librariesWithoutMacros() {
        writeFile(text = """<map>
            <Library>
                <option name="name" value="dependencies1" />
                <option name="path" value="/path/to/dependencies1" />
            </Library>
            <Library>
                <option name="name" value="dependencies2" />
                <option name="path" value="/path/to/dependencies2" />
            </Library>
        </map>""")

        assertEquals(listOf("/path/to/dependencies1", "/path/to/dependencies2"), getLibraries())
    }

    @Test
    fun macrosNotFound() {
        writeFile(text = """<map>
            <Library>
                <option name="name" value="dependencies1" />
                <option name="path" value="${Macro1}/dependencies1" />
            </Library>
            <Library>
                <option name="name" value="dependencies2" />
                <option name="path" value="${Macro2}/dependencies2" />
            </Library>
        </map>""")

        assertEquals(listOf("\${macro.path1}/dependencies1", "\${macro.path2}/dependencies2"), getLibraries())
    }

    @Test
    fun macrosFound() {
        writeFile(text = """<map>
            <Library>
                <option name="name" value="dependencies1" />
                <option name="path" value="${Macro1}/dependencies1" />
            </Library>
            <Library>
                <option name="name" value="dependencies2" />
                <option name="path" value="${Macro2}/dependencies2" />
            </Library>
        </map>""")

        assertEquals(listOf("/path/to/macro1/dependencies1", "/path/to/macro2/dependencies2"), getLibraries(AllMacros))
    }

    @Test
    fun macrosOnlyInPrefix() {
        writeFile(text = """<map>
            <Library>
                <option name="name" value="dependencies1" />
                <option name="path" value="/home/${Macro1}/dependencies1" />
            </Library>
            <Library>
                <option name="name" value="dependencies2" />
                <option name="path" value="/home/${Macro2}/dependencies2" />
            </Library>
        </map>""")

        assertEquals(listOf("/home/\${macro.path1}/dependencies1", "/home/\${macro.path2}/dependencies2"),
            getLibraries(AllMacros))
    }

    private fun getLibraries(macros: List<Macro> = emptyList()): List<String> {
        var result: List<String> = emptyList()
        findProjectLibraries(folder.root, macros) { result = it.toList() }
        return result
    }

    private fun writeFile(filePath: String = ".mps/libraries.xml", text: String) {
        val fullPath = folder.root.resolve(filePath)
        fullPath.parentFile.mkdirs()
        fullPath.writeText(text)
    }
}
