package de.itemis.mps.gradle.modelcheck

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.intellij.openapi.application.ApplicationManager
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.junit.Failure
import de.itemis.mps.gradle.junit.Testcase
import de.itemis.mps.gradle.junit.Testsuite
import de.itemis.mps.gradle.junit.Testsuites
import de.itemis.mps.gradle.project.loader.Args
import de.itemis.mps.gradle.project.loader.executeWithProject
import jetbrains.mps.checkers.*
import jetbrains.mps.errors.CheckerRegistry
import jetbrains.mps.errors.MessageStatus
import jetbrains.mps.errors.item.IssueKindReportItem
import jetbrains.mps.ide.MPSCoreComponents
import jetbrains.mps.ide.httpsupport.runtime.base.HttpSupportUtil
import jetbrains.mps.ide.modelchecker.platform.actions.UnresolvedReferencesChecker
import jetbrains.mps.progress.EmptyProgressMonitor
import jetbrains.mps.project.Project
import jetbrains.mps.project.validation.StructureChecker
import jetbrains.mps.smodel.SModelStereotype
import jetbrains.mps.typesystemEngine.checker.NonTypesystemChecker
import jetbrains.mps.typesystemEngine.checker.TypesystemChecker
import jetbrains.mps.util.CollectConsumer
import org.apache.log4j.Logger
import org.jetbrains.mps.openapi.model.SModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.test.fail

fun main(args: Array<String>) = mainBody("modelcheck") {

    val parsed = ArgParser(args).parseInto(::ModelCheckArgs)
    var hasErrors = true
    try {
        hasErrors = executeWithProject(parsed) { project -> modelCheckProject(parsed, project) }
    } catch (ex: java.lang.Exception) {
        logger.fatal("error model checking", ex)
    } catch (t: Throwable) {
        logger.fatal("error model checking", t)
    }

    if (hasErrors && !parsed.dontFailOnError) {
        System.exit(-1)
    }

    System.exit(0)

}
