package de.itemis.mps.gradle.modelcheck

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import de.itemis.mps.gradle.junit.Failure
import de.itemis.mps.gradle.junit.Testcase
import de.itemis.mps.gradle.junit.Testsuite
import de.itemis.mps.gradle.junit.Testsuites
import jetbrains.mps.checkers.ModelCheckerBuilder
import jetbrains.mps.errors.CheckerRegistry
import jetbrains.mps.errors.MessageStatus
import jetbrains.mps.errors.item.IssueKindReportItem
import jetbrains.mps.ide.httpsupport.runtime.base.HttpSupportUtil
import jetbrains.mps.ide.modelchecker.platform.actions.UnresolvedReferencesChecker
import jetbrains.mps.progress.EmptyProgressMonitor
import jetbrains.mps.project.Project
import jetbrains.mps.smodel.SModelStereotype
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.util.CollectConsumer
import org.apache.log4j.Logger
import org.jetbrains.mps.openapi.model.SModel
import org.jetbrains.mps.openapi.module.SModule
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.min
import kotlin.test.fail

val logger = Logger.getLogger("de.itemis.mps.gradle.modelcheck")

enum class ReportFormat {
    @Deprecated(
        message = "Please use ONE_TEST_PER_MODULE_AND_MODEL instead, this doesn't report errors on modules.",
        replaceWith = ReplaceWith("ONE_TEST_PER_MODULE_AND_MODEL")
    )
    ONE_TEST_PER_MODEL,
    ONE_TEST_PER_MODULE_AND_MODEL,
    ONE_TEST_PER_FAILED_MESSAGE
}

fun printInfo(msg: String) {
    logger.info(msg)
}

fun printWarn(msg: String) {
    logger.warn(msg)
}

fun printError(msg: String) {
    logger.error(msg)
}

fun getCurrentTimeStamp(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    return df.format(Date())
}

fun printResult(item: IssueKindReportItem, project: Project, args: ModelCheckArgs) {
    val path = IssueKindReportItem.PATH_OBJECT.get(item)

    val info = ::printInfo
    val warn = if (args.warningAsError) {
        ::printError
    } else {
        ::printWarn
    }

    val err = ::printError

    val print = fun(severity: MessageStatus, msg: String) {
        when (severity) {
            MessageStatus.OK -> info(msg)
            MessageStatus.WARNING -> warn(msg)
            MessageStatus.ERROR -> err(msg)
        }
    }

    when (path) {
        is IssueKindReportItem.PathObject.ModulePathObject -> {
            val module = path.resolve(project.repository)
            print(item.severity, "${item.message}[${module.moduleName}]")
        }
        is IssueKindReportItem.PathObject.ModelPathObject -> {
            val model = path.resolve(project.repository)
            print(item.severity, "${item.message} [${model.name.longName}]")
        }
        is IssueKindReportItem.PathObject.NodePathObject -> {
            val node = path.resolve(project.repository)
            val url = HttpSupportUtil.getURL(node)
            print(item.severity, "${item.message} [$url]")
        }
        else -> print(item.severity, item.message)
    }
}


fun writeJunitXml(modules: Iterable<SModule>,
                  models: Iterable<SModel>,
                  results: Iterable<IssueKindReportItem>,
                  project: Project,
                  warnAsErrors: Boolean,
                  format: ReportFormat,
                  file: File
) {

    val allErrors = results.filter {
        it.severity == MessageStatus.ERROR || (warnAsErrors && it.severity == MessageStatus.WARNING)
    }

    val errorsPerModule = allErrors
        .filter {
            val path = IssueKindReportItem.PATH_OBJECT.get(it)
            path is IssueKindReportItem.PathObject.ModulePathObject
        }.groupBy {
            when (val path = IssueKindReportItem.PATH_OBJECT.get(it)) {
                is IssueKindReportItem.PathObject.ModulePathObject -> {
                    path.resolve(project.repository)!!
                }

                else -> fail("unexpected item type")
            }
        }

    val errorsPerModel = allErrors
        .filter {
            val path = IssueKindReportItem.PATH_OBJECT.get(it)
            path is IssueKindReportItem.PathObject.ModelPathObject || path is IssueKindReportItem.PathObject.NodePathObject
        }.groupBy {
            when (val path = IssueKindReportItem.PATH_OBJECT.get(it)) {
                is IssueKindReportItem.PathObject.ModelPathObject -> {
                    path.resolve(project.repository)!!
                }
                is IssueKindReportItem.PathObject.NodePathObject -> {
                    val node = path.resolve(project.repository)
                    node.model!!
                }
                else -> fail("unexpected item type")
            }
        }

    val xmlMapper = XmlMapper()

    @Suppress("DEPRECATION")
    when (format) {
        ReportFormat.ONE_TEST_PER_MODEL -> {
            val message = "The option `--result-format model` is deprecated as it doesn't report module level errors, " +
                    "please use `--result-format module-and-model` instead."
            printError(message)
            System.err.println(message)

            val testcases = oneTestCasePerModel(models, errorsPerModel, project)
            val testsuite = Testsuite(name = "model checking",
                failures = allErrors.size,
                testcases = testcases,
                tests = models.count(),
                timestamp = getCurrentTimeStamp())
            xmlMapper.writeValue(file, testsuite)
        }

        ReportFormat.ONE_TEST_PER_MODULE_AND_MODEL -> {
            val moduleTestcases = oneTestCasePerModule(modules, errorsPerModule, project)
            val modelTestcases = oneTestCasePerModel(models, errorsPerModel, project)
            val testcases = moduleTestcases + modelTestcases
            val testsuite = Testsuite(name = "model checking",
                failures = allErrors.size,
                testcases = testcases,
                tests = modules.count() + models.count(),
                timestamp = getCurrentTimeStamp())
            xmlMapper.writeValue(file, testsuite)
        }

        ReportFormat.ONE_TEST_PER_FAILED_MESSAGE -> {
            val testsuits = models.mapIndexed { i: Int, mdl: SModel ->
                val errorsInModel = errorsPerModel[mdl] ?: emptyList()
                val effectiveErrorsInModel = errorsInModel.map { item -> oneTestCasePerMessage(item, mdl, project) }
                    // some issues are reported multiple times per node for some reason -> filter out such issues
                    .distinctBy { "${it.classname}.${it.name}" }
                    // some build servers doesn't process/group tests per package/class -> provide default sort order
                    .sortedBy { it.classname }
                Testsuite(name = mdl.name.simpleName,
                    pkg = mdl.name.namespace,
                    failures = effectiveErrorsInModel.size,
                    id = i,
                    tests = effectiveErrorsInModel.size,
                    timestamp = getCurrentTimeStamp(),
                    testcases = effectiveErrorsInModel)
            }
            xmlMapper.writeValue(file, Testsuites(testsuits))
        }
    }


}

private fun oneTestCasePerMessage(item: IssueKindReportItem, model: SModel, project: Project): Testcase {
    // replace also ':', as otherwise the string before could be recognized as class name
    val testCaseName = item.message.replace(Regex("[:\\s]"), "_").substring(0, min(item.message.length, 120))
    return when (val path = IssueKindReportItem.PATH_OBJECT.get(item)) {
        is IssueKindReportItem.PathObject.ModelPathObject -> {
            val message = "${item.message} [${model.name.longName}]"
            val className = model.name.longName
            Testcase(
                name = testCaseName,
                classname = className,
                failure = Failure(message = message, type = item.issueKind.toString()),
                time = 0
            )
        }
        is IssueKindReportItem.PathObject.NodePathObject -> {
            val node = path.resolve(project.repository)
            val url = HttpSupportUtil.getURL(node)
            val message = "${item.message} [$url]"
            val className = node.containingRoot.presentation + "." + node.nodeId
            Testcase(
                name = testCaseName,
                classname = className,
                failure = Failure(message = message, type = item.issueKind.toString()),
                time = 0
            )
        }
        else -> fail("unexpected issue kind")
    }
}


private fun oneTestCasePerModel(models: Iterable<SModel>, errorsPerModel: Map<SModel, List<IssueKindReportItem>>, project: Project): List<Testcase> {
    return models.map {
        val errors = errorsPerModel.getOrDefault(it, emptyList())
        fun reportItemToContent(s: Failure, item: IssueKindReportItem): Failure {
            return when (val path = IssueKindReportItem.PATH_OBJECT.get(item)) {
                is IssueKindReportItem.PathObject.ModelPathObject -> {
                    val model = path.resolve(project.repository)!!
                    val message = "${item.message} [${model.name.longName}]"
                    Failure(
                        message = "${s.message}\n $message",
                        type = s.type
                    )
                }
                is IssueKindReportItem.PathObject.NodePathObject -> {
                    val node = path.resolve(project.repository)
                    val url = HttpSupportUtil.getURL(node)
                    val message = "${item.message} [$url]"
                    Failure(
                        message = "${s.message}\n $message",
                        type = s.type
                    )
                }
                else -> fail("unexpected issue kind")
            }
        }

        val accumulatedFailure = errors.fold(Failure(message = "", type = "model checking"), ::reportItemToContent)

        Testcase(
            name = it.name.simpleName,
            classname = it.name.longName,
            failure = if (errors.isEmpty()) null else accumulatedFailure,
            time = 0
        )
    }
}

private fun oneTestCasePerModule(modules: Iterable<SModule>, errorsPerModule: Map<SModule, List<IssueKindReportItem>>, project: Project): List<Testcase> {
    return modules.map {
        val errors = errorsPerModule.getOrDefault(it, emptyList())
        fun reportItemToContent(s: Failure, item: IssueKindReportItem): Failure {
            return when (val path = IssueKindReportItem.PATH_OBJECT.get(item)) {
                is IssueKindReportItem.PathObject.ModulePathObject -> {
                    val module = path.resolve(project.repository)!!
                    val message = "${item.message} [${module.moduleName}]"
                    Failure(
                        message = "${s.message}\n $message",
                        type = s.type
                    )
                }
                else -> fail("unexpected issue kind")
            }
        }

        val accumulatedFailure = errors.fold(Failure(message = "", type = "model checking"), ::reportItemToContent)

        Testcase(
            name = "module ${it.moduleName!!}",
            classname = it.moduleName!!,
            failure = if (errors.isEmpty()) null else accumulatedFailure,
            time = 0
        )
    }
}

fun modelCheckProject(args: ModelCheckArgs, environment: Environment, project: Project): Boolean {
    val checkers = environment.platform.findComponent(CheckerRegistry::class.java)!!.checkers
    if (checkers.all { it !is UnresolvedReferencesChecker }) {
        checkers.add(UnresolvedReferencesChecker())
    }

    if (logger.isInfoEnabled) {
        logger.info(checkers.joinToString(prefix = "Found the following checkers in CheckerRegistry: "))
    }

    // We don't use ModelCheckerIssueFinder because it has strange dependency on the ModelCheckerSettings which we
    // want to avoid when running in headless mode
    val errorCollector = CollectConsumer<IssueKindReportItem>()

    val moduleAndModelMatcher = ModuleAndModelMatcher(args)

    val modelExtractor = object : ModelCheckerBuilder.ModelsExtractorImpl() {
        override fun includeModel(model: SModel): Boolean {
            return moduleAndModelMatcher.isModelIncluded(model)
        }
    }
    modelExtractor.includeStubs(false)
    val checker = ModelCheckerBuilder(modelExtractor).createChecker(checkers)

    val itemsToCheck = ModelCheckerBuilder.ItemsToCheck()

    project.modelAccess.runReadAction {
        if (args.models.isNotEmpty() || args.excludeModels.isNotEmpty()) {
            itemsToCheck.models.addAll(
                project.projectModulesWithGenerators
                    .filter(moduleAndModelMatcher::isModuleIncluded)
                    .flatMap { module -> module.models }
                    .filter(moduleAndModelMatcher::isModelIncluded))
        } else {
            itemsToCheck.modules.addAll(
                project.projectModulesWithGenerators
                    .filter(moduleAndModelMatcher::isModuleIncluded)
            )
        }

        checker.check(itemsToCheck, project.repository, errorCollector, EmptyProgressMonitor())

        // We need read access here to resolve the node pointers in the report items
        errorCollector.result.map { printResult(it, project, args) }

        if (args.xmlFile != null) {
            val allCheckedModules = itemsToCheck.modules
            val allCheckedModels = itemsToCheck.modules.flatMap { module ->
                module.models.filter { !SModelStereotype.isDescriptorModel(it) }
            }.union(itemsToCheck.models)
            writeJunitXml(allCheckedModules, allCheckedModels, errorCollector.result, project, args.warningAsError, args.xmlReportFormat, File(args.xmlFile!!))
        }
    }

    val minSeverity = if (args.warningAsError) MessageStatus.WARNING else MessageStatus.ERROR
    return errorCollector.result.any { it.severity >= minSeverity }
}
