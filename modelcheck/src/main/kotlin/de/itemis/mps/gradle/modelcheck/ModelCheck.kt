package de.itemis.mps.gradle.modelcheck

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.util.BuildNumber
import de.itemis.mps.gradle.junit.Failure
import de.itemis.mps.gradle.junit.JUnitXmlSerializer
import de.itemis.mps.gradle.junit.Testcase
import de.itemis.mps.gradle.junit.Testsuite
import de.itemis.mps.gradle.junit.Testsuites
import de.itemis.mps.gradle.logging.detectLogging
import de.itemis.mps.gradle.project.loader.ModuleAndModelMatcher
import de.itemis.mps.gradle.project.loader.forceIndexing
import de.itemis.mps.gradle.project.loader.hasIndexingBug
import jetbrains.mps.checkers.ModelCheckerBuilder
import jetbrains.mps.errors.CheckerRegistry
import jetbrains.mps.errors.MessageStatus
import jetbrains.mps.errors.item.IssueKindReportItem
import jetbrains.mps.ide.httpsupport.runtime.base.HttpSupportUtil
import jetbrains.mps.ide.modelchecker.platform.actions.IdeaPlatformReadExecutor
import jetbrains.mps.progress.EmptyProgressMonitor
import jetbrains.mps.project.MPSProject
import jetbrains.mps.project.Project
import jetbrains.mps.smodel.ModelAccessBase
import jetbrains.mps.smodel.SModelStereotype
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.IdeaEnvironment
import jetbrains.mps.util.CollectConsumer
import jetbrains.mps.workbench.progress.IdeaPlatformTaskScheduler
import jetbrains.mps.workbench.progress.SystemBackgroundTaskScheduler
import org.jetbrains.mps.openapi.model.SModel
import org.jetbrains.mps.openapi.model.SNode
import org.jetbrains.mps.openapi.module.SModule
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min


val logging = detectLogging()
val logger = logging.getLogger("de.itemis.mps.gradle.modelcheck")

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

val IssueKindReportItem.path: IssueKindReportItem.PathObject
    get() = IssueKindReportItem.PATH_OBJECT.get(this)

fun IssueKindReportItem.PathObject.asModule(project: Project): SModule? =
    (this as? IssueKindReportItem.PathObject.ModulePathObject)?.resolve(project.repository)

fun IssueKindReportItem.PathObject.asModel(project: Project): SModel? =
    (this as? IssueKindReportItem.PathObject.ModelPathObject)?.resolve(project.repository)

fun IssueKindReportItem.PathObject.asNode(project: Project): SNode? =
    (this as? IssueKindReportItem.PathObject.NodePathObject)?.resolve(project.repository)

val SNode.url: String
    get() = HttpSupportUtil.getURL(this)

fun printResult(item: IssueKindReportItem, project: Project, args: ModelCheckArgs) {
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

    when (val path = item.path) {
        is IssueKindReportItem.PathObject.ModulePathObject ->
            print(item.severity, "${item.message} [${path.asModule(project)?.moduleName}]")
        is IssueKindReportItem.PathObject.ModelPathObject ->
            print(item.severity, "${item.message} [${path.asModel(project)?.name?.longName}]")
        is IssueKindReportItem.PathObject.NodePathObject ->
            print(item.severity, "${item.message} [${path.asNode(project)?.url}]")
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

    val errorsPerModel = allErrors
        .mapNotNull { error ->
            val path = error.path
            val model = path.asModel(project) ?: path.asNode(project)?.model
            model?.to(error)
        }
        .groupBy({ it.first }, { it.second })

    val modelsToReport = models.union(errorsPerModel.keys)

    val value: Any =
        @Suppress("DEPRECATION")
        when (format) {
            ReportFormat.ONE_TEST_PER_MODEL -> {
                val message =
                    "The option `--result-format model` is deprecated as it doesn't report module level errors, " +
                            "please use `--result-format module-and-model` instead."
                printError(message)
                System.err.println(message)

                Testsuite(
                    name = "model checking",
                    failures = errorsPerModel.values.sumOf { it.size },
                    testcases = oneTestCasePerModel(modelsToReport, errorsPerModel, project),
                    tests = modelsToReport.count(),
                    timestamp = getCurrentTimeStamp(),
                )
            }

            ReportFormat.ONE_TEST_PER_MODULE_AND_MODEL -> {
                val errorsPerModule = allErrors
                    .mapNotNull { error ->
                        val module = error.path.asModule(project)
                        module?.to(error)
                    }
                    .groupBy({ it.first }, { it.second })

                val modulesToReport = modules.union(errorsPerModule.keys)

                val moduleTestcases = oneTestCasePerModule(modulesToReport, errorsPerModule, project)
                val modelTestcases = oneTestCasePerModel(modelsToReport, errorsPerModel, project)
                val testcases = moduleTestcases + modelTestcases

                Testsuite(
                    name = "model checking",
                    failures = allErrors.size,
                    testcases = testcases,
                    tests = modulesToReport.count() + modelsToReport.count(),
                    timestamp = getCurrentTimeStamp()
                )
            }

            ReportFormat.ONE_TEST_PER_FAILED_MESSAGE -> {
                val testsuites = modelsToReport.mapIndexed { i: Int, mdl: SModel ->
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
                Testsuites(testsuites)
            }
        }

    file.writeBytes(JUnitXmlSerializer.documentToByteArray(JUnitXmlSerializer.toDocument(value)))
}

private fun oneTestCasePerMessage(item: IssueKindReportItem, model: SModel, project: Project): Testcase {
    // replace also ':', as otherwise the string before could be recognized as class name
    val testCaseName = item.message.replace(Regex("[:\\s]"), "_").substring(0, min(item.message.length, 120))
    return when (val path = item.path) {
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
            val message = "${item.message} [${node.url}]"
            val className = node.containingRoot.presentation + "." + node.nodeId
            Testcase(
                name = testCaseName,
                classname = className,
                failure = Failure(message = message, type = item.issueKind.toString()),
                time = 0
            )
        }
        else -> throw IllegalArgumentException("unexpected issue kind")
    }
}


private fun oneTestCasePerModel(models: Iterable<SModel>, errorsPerModel: Map<SModel, List<IssueKindReportItem>>, project: Project): List<Testcase> {
    return models.map {
        val errors = errorsPerModel.getOrDefault(it, emptyList())
        val message = buildString {
            for (item in errors) {
                when (val path = item.path) {
                    is IssueKindReportItem.PathObject.ModelPathObject -> {
                        val model = path.resolve(project.repository)!!
                        appendLine(" ${item.message} [${model.name.longName}]")
                    }
                    is IssueKindReportItem.PathObject.NodePathObject -> {
                        val node = path.resolve(project.repository)
                        appendLine(" ${item.message} [${node.url}]")
                    }
                    else -> throw IllegalArgumentException("unexpected issue kind")
                }
            }
        }

        val failure = Failure(message = message, content = message, type = "model checking")

        Testcase(
            name = it.name.simpleName,
            classname = it.name.longName,
            failure = if (errors.isEmpty()) null else failure,
            time = 0
        )
    }
}

private fun oneTestCasePerModule(modules: Iterable<SModule>, errorsPerModule: Map<SModule, List<IssueKindReportItem>>, project: Project): List<Testcase> {
    return modules.map {
        val errors = errorsPerModule.getOrDefault(it, emptyList())
        val message = buildString {
            for (item in errors) {
                when (val path = item.path) {
                    is IssueKindReportItem.PathObject.ModulePathObject -> {
                        val module = path.resolve(project.repository)!!
                        appendLine(" ${item.message} [${module.moduleName}]")
                    }
                    else -> throw IllegalArgumentException("unexpected issue kind")
                }
            }
        }

        val failure = Failure(message = message, content = message, type = "model checking")

        Testcase(
            name = "module ${it.moduleName!!}",
            classname = it.moduleName!!,
            failure = if (errors.isEmpty()) null else failure,
            time = 0
        )
    }
}

private fun ModelCheckerBuilder.setParallelTaskScheduler(project: Project) {
    try {
        setParallelTaskSchedulerV3(project)
    } catch (_: LinkageError) {
        try {
            setParallelTaskSchedulerV2(project)
        } catch (_: LinkageError) {
            try {
                setParallelTaskSchedulerV1(project)
            } catch (e: LinkageError) {
                logger.warn("Parallel model checking is not supported in this version of MPS", e)
            }
        }
    }
}

private fun ModelCheckerBuilder.setParallelTaskSchedulerV3(project: Project) {
    val executor = IdeaPlatformReadExecutor(project.repository.modelAccess)
    withTaskScheduler(IdeaPlatformTaskScheduler(project, executor))
}

private fun ModelCheckerBuilder.setParallelTaskSchedulerV2(project: Project) {
    val executor = (project.repository.modelAccess as ModelAccessBase).shareRead()
    withTaskScheduler(SystemBackgroundTaskScheduler(project, executor))
}

private fun ModelCheckerBuilder.setParallelTaskSchedulerV1(project: Project) {
    withTaskScheduler(SystemBackgroundTaskScheduler(project))
}

fun modelCheckProject(args: ModelCheckArgs, environment: Environment, project: Project): Boolean {
    val checkers = environment.platform.findComponent(CheckerRegistry::class.java)!!.checkers

    if (logger.isInfoEnabled) {
        logger.info(checkers.joinToString(prefix = "Found the following checkers in CheckerRegistry: "))
    }

    // We don't use ModelCheckerIssueFinder because it has a strange dependency on ModelCheckerSettings which we
    // want to avoid when running in headless mode
    val errorCollector = CollectConsumer<IssueKindReportItem>()

    val moduleAndModelMatcher = ModuleAndModelMatcher(args.modules, args.excludeModules, args.models, args.excludeModels)

    val modelExtractor = object : ModelCheckerBuilder.ModelsExtractorImpl() {
        override fun includeModel(model: SModel): Boolean {
            return moduleAndModelMatcher.isModelIncluded(model)
        }
    }
    modelExtractor.includeStubs(false)
    val itemsToCheck = ModelCheckerBuilder.ItemsToCheck()

    // Workaround for https://youtrack.jetbrains.com/issue/MPS-37926/Indices-not-built-properly-in-IdeaEnvironment
    if (environment is IdeaEnvironment) {
        val buildNumber = ApplicationInfo.getInstance().build
        if (shouldForceIndexing(args, buildNumber)) {
            logger.info("Forcing full indexing. Can be disabled with --force-indexing=never.")
            forceIndexing(project as MPSProject, buildNumber)
            logger.info("Full indexing complete")
        }
    }

    // In the IDEA environment run the model check in EDT to avoid interference from other code that may want to run
    // a write or read action, such as 'run in smart mode' callbacks triggered by completed indexing, context assistant
    // updates, etc.
    val runInEdt = if (environment is IdeaEnvironment) {
        { r: Runnable ->
            @Suppress("DEPRECATION") // nonModal() is not available in MPS 2022.3
            ApplicationManager.getApplication().invokeAndWait(r, ModalityState.NON_MODAL)
        }
    } else Runnable::run

    runInEdt {
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

            val checker = ModelCheckerBuilder(modelExtractor)
                .also {
                    if (args.parallel) {
                        it.setParallelTaskScheduler(project)
                    }
                }
                .createChecker(checkers)

            checker.check(itemsToCheck, project.repository, errorCollector, EmptyProgressMonitor())

            // We need read access here to resolve the node pointers in the report items
            errorCollector.result.map { printResult(it, project, args) }

            if (args.xmlFile != null) {
                val allCheckedModules = itemsToCheck.modules
                val allCheckedModels = itemsToCheck.modules.flatMap { module ->
                    module.models.filter { !SModelStereotype.isDescriptorModel(it) }
                }.union(itemsToCheck.models)
                writeJunitXml(
                    modules = allCheckedModules,
                    models = allCheckedModels,
                    results = errorCollector.result,
                    project = project,
                    warnAsErrors = args.warningAsError,
                    format = args.xmlReportFormat,
                    file = File(args.xmlFile!!)
                )
            }
        }
    }

    val minSeverity = if (args.warningAsError) MessageStatus.WARNING else MessageStatus.ERROR
    return errorCollector.result.any { it.severity >= minSeverity }
}

fun shouldForceIndexing(args: ModelCheckArgs, buildNumber: BuildNumber): Boolean {
    return args.forceIndexing ?: hasIndexingBug(buildNumber)
}
