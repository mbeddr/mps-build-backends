package de.itemis.mps.gradle.junit

data class Skipped(val content: String)

/**
 * Indicates that the test errored.  An errored test is one that had an unanticipated problem. e.g., an unchecked throwable; or a problem with the implementation of the test. Contains as a text node relevant data for the error, e.g., a stack trace
 */
data class Error(
        /**
         * The error message. e.g., if a java exception is thrown, the return value of getMessage()
         */
        val message: String,
        /**
         * The type of error that occured. e.g., if a java execption is thrown the full class name of the exception.
         */
        val type: String,
        val content: String? = null
)

/**
 * Indicates that the test failed. A failure is a test which the code has explicitly failed by using the mechanisms for that purpose. e.g., via an assertEquals. Contains as a text node relevant data for the failure, e.g., a stack trace
 */
data class Failure(
        /**
         * The message specified in the assert
         */
        val message: String,
        /**
         * The type of the assert.
         */
        val type: String,
        val content: String? = null
)

data class SystemOut(val content: String)

data class SystemErr(val content: String)


data class Testcase(
        /**
         * Name of the test method
         */
        val name: String,
        /**
         * Full class name for the class the test method is in.
         */
        val classname: String,
        /**
         * Time taken (in seconds) to execute the test
         */
        val time: Int,
        val skipped: Skipped? = null,
        val error: Error? = null,
        val failure: Failure? = null
)

data class Testsuite(
        /**
         * Full class name of the test for non-aggregated testsuite documents. Class name without the package for aggregated testsuites documents
         */
        val name: String,
        /**
         * The total number of tests in the suite
         */
        val tests: Int,
        /**
         * The total number of tests in the suite that errored. An errored test is one that had an unanticipated problem. e.g., an unchecked throwable; or a problem with the implementation of the test.
         */
        val errors: Int = 0,
        /**
         * The total number of tests in the suite that failed. A failure is a test which the code has explicitly failed by using the mechanisms for that purpose. e.g., via an assertEquals
         */
        val failures: Int = 0,
        /**
         * Only required if contained in a testsuites list
         * Starts at '0' for the first testsuite and is incremented by 1 for each following testsuite
         */
        val id: Int? = null,
        /**
         * Derived from testsuite/@name in the non-aggregated documents
         */
        val pkg: String? = null,
        /**
         * The total number of ignored or skipped tests in the suite.
         */
        val skipped: Int? = null,
        /**
         * Time taken (in seconds) to execute the tests in the suite
         */
        val time: Int = 0,
        /**
         * when the test was executed. Timezone may not be specified.
         */
        val timestamp: String,
        /**
         * Host on which the tests were executed. 'localhost' should be used if the hostname cannot be determined.
         */
        val hostname: String = "localhost",
        /**
         * Properties (e.g., environment settings) set during test execution
         */
        val properties: List<Property> = emptyList(),
        val testcases: List<Testcase>,
        /**
         * Data that was written to standard out while the test was executed
         */
        val systemOut: SystemOut = SystemOut(""),
        /**
         * Data that was written to standard error while the test was executed
         */
        val systemError: SystemErr = SystemErr("")
)

data class Property(val name: String, val value: String)

data class Testsuites(val testsuites: List<Testsuite>)
