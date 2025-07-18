package de.itemis.mps.gradle.junit

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.xmlunit.validation.Languages.W3C_XML_SCHEMA_NS_URI
import org.xmlunit.validation.Validator
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamSource

class JUnitXmlSerializerTest {

    private fun String.loadResource(): URL {
        return this@JUnitXmlSerializerTest::class.java.getResource(this)!!
    }

    fun getCurrentTimeStamp(): String {
        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        return df.format(Date())
    }

    @Test
    fun `minimum test suite is valid`() {
        assertSerializesCorrectly(
            Testsuite(
                name = "my tests",
                testcases = emptyList(),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut("")
            )
        )
    }

    @Test
    fun `everything test suite is valid`() {
        assertSerializesCorrectly(
            Testsuite(
                name = "my tests",
                testcases = emptyList(),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
            )
        )
    }

    @Test
    fun `test suite with one simple test is valid`() {

        val test = Testcase(name = "my fancy test", classname = "my class name", time = 0)
        assertSerializesCorrectly(
            Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
            )
        )
    }

    @Test
    fun `test suite with one complex test with error is valid`() {

        val test = Testcase(
            name = "my fancy test",
            classname = "my class name",
            time = 0,
            error = Error(message = "my message", type = "")
        )
        assertSerializesCorrectly(
            Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
            )
        )
    }

    @Test
    fun `test suite with one complex test with error and content is valid`() {

        val test = Testcase(
            name = "my fancy test",
            classname = "my class name",
            time = 0,
            error = Error(message = "my message", type = "", content = "something")
        )
        assertSerializesCorrectly(
            Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
            )
        )
    }

    @Test
    fun `test suite with one complex test with failure is valid`() {

        val test = Testcase(
            name = "my fancy test",
            classname = "my class name",
            time = 0,
            failure = Failure(message = "my message", type = "")
        )
        assertSerializesCorrectly(
            Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
            )
        )
    }

    @Test
    fun `test suite with one complex test with failure and content is valid`() {

        val test = Testcase(
            name = "my fancy test",
            classname = "my class name",
            time = 0,
            failure = Failure(message = "my message", type = "", content = "something")
        )
        assertSerializesCorrectly(
            Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
            )
        )
    }

    @Test
    fun `test suite with one complex test with skipped is valid`() {

        val test = Testcase(
            name = "my fancy test",
            classname = "my class name",
            time = 0,
            skipped = Skipped(content = "something")
        )
        assertSerializesCorrectly(
            Testsuite(
                name = "my tests",
                testcases = listOf(test),
                tests = 0,
                timestamp = getCurrentTimeStamp(),
                systemError = SystemErr(""),
                systemOut = SystemOut(""),
                hostname = "my host",
                failures = 42,
                errors = 12,
                time = 4212,
                skipped = 32
            )
        )
    }

    @Test
    fun `test suits with one test suite is valid`() {
        val testsuite = Testsuite(
            name = "my tests",
            testcases = emptyList(),
            tests = 0,
            timestamp = getCurrentTimeStamp(),
            systemError = SystemErr(""),
            systemOut = SystemOut(""),
            id = 0,
            pkg = "something"
        )
        assertSerializesCorrectly(Testsuites(listOf(testsuite)))
    }

    @Test
    fun `test suits with test suites is valid`() {
        val testsuite = Testsuite(
            name = "my tests",
            testcases = emptyList(),
            tests = 0,
            timestamp = getCurrentTimeStamp(),
            systemError = SystemErr(""),
            systemOut = SystemOut(""),
            id = 0,
            pkg = "something"
        )
        val testsuite2 = Testsuite(
            name = "my tests 2",
            testcases = emptyList(),
            tests = 34,
            timestamp = getCurrentTimeStamp(),
            systemError = SystemErr(""),
            systemOut = SystemOut(""),
            id = 1,
            pkg = "something"
        )
        assertSerializesCorrectly(Testsuites(listOf(testsuite, testsuite2)))
    }


    @Test
    fun testsuiteByJunie() {
        val timestamp = getCurrentTimeStamp()

        val testsuite = Testsuite(
            name = "com.example.TestClass",
            tests = 3,
            errors = 1,
            failures = 1,
            timestamp = timestamp,
            testcases = listOf(
                Testcase(
                    name = "testSuccess",
                    classname = "com.example.TestClass",
                    time = 1
                ),
                Testcase(
                    name = "testError",
                    classname = "com.example.TestClass",
                    time = 2,
                    error = Error(
                        message = "Error occurred",
                        type = "java.lang.RuntimeException",
                        content = "Stack trace here"
                    )
                ),
                Testcase(
                    name = "testFailure",
                    classname = "com.example.TestClass",
                    time = 1,
                    failure = Failure(
                        message = "Assertion failed",
                        type = "org.junit.ComparisonFailure",
                        content = "Expected: <true> but was: <false>"
                    )
                )
            ),
            properties = listOf(
                Property("os.name", "Linux"),
                Property("java.version", "11")
            )
        )

        assertSerializesCorrectly(testsuite)
    }

    @Test
    fun testsuitesByJunie() {
        val timestamp = getCurrentTimeStamp()

        val testsuite1 = Testsuite(
            name = "com.example.TestClass1",
            pkg = "something",
            tests = 1,
            errors = 0,
            failures = 0,
            timestamp = timestamp,
            id = 0,
            testcases = listOf(
                Testcase(
                    name = "testMethod1",
                    classname = "com.example.TestClass1",
                    time = 1
                )
            )
        )

        val testsuite2 = Testsuite(
            name = "com.example.TestClass2",
            pkg = "something",
            tests = 1,
            errors = 0,
            failures = 0,
            timestamp = timestamp,
            id = 1,
            testcases = listOf(
                Testcase(
                    name = "testMethod2",
                    classname = "com.example.TestClass2",
                    time = 1,
                    skipped = Skipped("Test skipped")
                )
            )
        )

        assertSerializesCorrectly(Testsuites(listOf(testsuite1, testsuite2)))
    }

    private fun assertSerializesCorrectly(value: Any) {
        val doc = JUnitXmlSerializer.toDocument(value)
        val validator = Validator.forLanguage(W3C_XML_SCHEMA_NS_URI)
        validator.setSchemaSource(StreamSource("junit.xsd".loadResource().openStream(), "junit.xsd"))

        val result = validator.validateInstance(DOMSource(doc))
        Assertions.assertTrue(result.isValid) {
            buildString {
                result.problems.joinTo(this, "\n") { it.message }
                appendLine()
                appendLine("XML:")
                appendLine(JUnitXmlSerializer.documentToByteArray(doc).toString(Charsets.UTF_8))
            }
        }
    }
}
