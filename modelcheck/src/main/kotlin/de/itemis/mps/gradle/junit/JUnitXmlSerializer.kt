package de.itemis.mps.gradle.junit

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Serializes JUnit test classes to XML format
 */
object JUnitXmlSerializer {

    fun toDocument(value: Any): Document =
        when (value) {
            is Testsuite -> toDocument(value)
            is Testsuites -> toDocument(value)
            else -> throw IllegalArgumentException("Unsupported argument type: " + value.javaClass)
        }

    fun toDocument(testsuites: Testsuites): Document {
        val doc = createDocument()
        val rootElement = doc.createElement("testsuites")
        doc.appendChild(rootElement)

        testsuites.testsuites.forEach { testsuite ->
            addTestsuite(doc, rootElement, testsuite)
        }

        return doc
    }

    fun toDocument(testsuite: Testsuite): Document {
        val doc = createDocument()
        val rootElement = doc.createElement("testsuite")
        doc.appendChild(rootElement)

        addTestsuiteAttributes(rootElement, testsuite)
        addTestsuiteContent(doc, rootElement, testsuite)

        return doc
    }

    private fun createDocument(): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.newDocument()
    }

    private fun addTestsuite(doc: Document, parent: Element, testsuite: Testsuite) {
        val testsuiteElement = doc.createElement("testsuite")
        parent.appendChild(testsuiteElement)

        addTestsuiteAttributes(testsuiteElement, testsuite)
        addTestsuiteContent(doc, testsuiteElement, testsuite)
    }

    private fun addTestsuiteAttributes(element: Element, testsuite: Testsuite) {
        element.setAttribute("name", testsuite.name)
        element.setAttribute("tests", testsuite.tests.toString())
        element.setAttribute("errors", testsuite.errors.toString())
        element.setAttribute("failures", testsuite.failures.toString())
        element.setAttribute("time", testsuite.time.toString())
        element.setAttribute("timestamp", testsuite.timestamp)
        element.setAttribute("hostname", testsuite.hostname)

        testsuite.id?.let { element.setAttribute("id", it.toString()) }
        testsuite.pkg?.let { element.setAttribute("package", it) }
        testsuite.skipped?.let { element.setAttribute("skipped", it.toString()) }
    }

    private fun addTestsuiteContent(doc: Document, element: Element, testsuite: Testsuite) {
        // Add properties
        val propertiesElement = doc.createElement("properties")
        element.appendChild(propertiesElement)

        testsuite.properties.forEach { property ->
            val propertyElement = doc.createElement("property")
            propertyElement.setAttribute("name", property.name)
            propertyElement.setAttribute("value", property.value)
            propertiesElement.appendChild(propertyElement)
        }

        // Add testcases
        testsuite.testcases.forEach { testcase ->
            addTestcase(doc, element, testcase)
        }

        // Add system-out
        val systemOutElement = doc.createElement("system-out")
        systemOutElement.textContent = testsuite.systemOut.content
        element.appendChild(systemOutElement)

        // Add system-err
        val systemErrElement = doc.createElement("system-err")
        systemErrElement.textContent = testsuite.systemError.content
        element.appendChild(systemErrElement)
    }

    private fun addTestcase(doc: Document, parent: Element, testcase: Testcase) {
        val testcaseElement = doc.createElement("testcase")
        parent.appendChild(testcaseElement)

        // Add attributes
        testcaseElement.setAttribute("name", testcase.name)
        testcaseElement.setAttribute("classname", testcase.classname)
        testcaseElement.setAttribute("time", testcase.time.toString())

        // Add child elements if present
        testcase.skipped?.let {
            val skippedElement = doc.createElement("skipped")
            skippedElement.textContent = it.content
            testcaseElement.appendChild(skippedElement)
        }

        testcase.error?.let {
            val errorElement = doc.createElement("error")
            errorElement.setAttribute("message", it.message)
            errorElement.setAttribute("type", it.type)
            it.content?.let { content -> errorElement.textContent = content }
            testcaseElement.appendChild(errorElement)
        }

        testcase.failure?.let {
            val failureElement = doc.createElement("failure")
            failureElement.setAttribute("message", it.message)
            failureElement.setAttribute("type", it.type)
            it.content?.let { content -> failureElement.textContent = content }
            testcaseElement.appendChild(failureElement)
        }
    }

    fun documentToByteArray(doc: Document): ByteArray {
        val outputStream = ByteArrayOutputStream()
        newTransformer().transform(DOMSource(doc), StreamResult(outputStream))
        return outputStream.toByteArray()
    }

    private fun newTransformer(): Transformer {
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        return transformer
    }
}
