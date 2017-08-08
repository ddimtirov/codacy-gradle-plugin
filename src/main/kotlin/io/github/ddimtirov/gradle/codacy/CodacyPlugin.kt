package io.github.ddimtirov.gradle.codacy

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

open class CodacyPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            val codacyExt = extensions.create("codacy", CodacyExtension::class.java, project)
            codacyExt.toolVersion = "1.0.7"

            configurations {
                "codacy" {
                    defaultDependencies { defaultDeps ->
                        defaultDeps.add(project.dependencies.create("com.codacy:codacy-coverage-reporter:${codacyExt.toolVersionProvider}"))
                    }
                }
            }

            tasks {
                "codacyUpload" {
                    group = "publishing"
                    description = "Upload all coverage reports to Codacy.com"
                }
            }
            val codacyUploadTask = tasks["codacyUpload"]

            pluginManager.apply(JacocoPlugin::class.java)
            tasks.withType(JacocoReport::class.java) { reportTask ->
                reportTask.reports.xml.isEnabled = true
                project.tasks.create("${reportTask.name}CodacyUpload", CodacyUploadTask::class.java) { uploadTask ->
                    uploadTask.group = "publishing"
                    uploadTask.description = "Upload $reportTask.name to Codacy.com"
                    uploadTask.coverageReport = reportTask.reports.xml.destination
                    uploadTask.defaultCommitUuid(codacyExt.commitUuidProvider)
                    uploadTask.defaultProjectToken(codacyExt.projectTokenProvider)
                    uploadTask.dependsOn(reportTask)
                    codacyUploadTask.dependsOn(uploadTask)
                }
            }
        }
    }
}

open class CodacyExtension(project: Project) {
    private val toolVersionState = project.property<String>()
    private val commitUuidState = project.property<String?>()
    private val projectTokenState = project.property<String?>()

    val toolVersionProvider: Provider<String> get() = toolVersionState
    val commitUuidProvider: Provider<String?> get() = commitUuidState
    val projectTokenProvider: Provider<String?> get() = projectTokenState

    var toolVersion by toolVersionState
    var commitUuid by commitUuidState
    var projectToken by projectTokenState
}

open class CodacyUploadTask @Inject constructor (val workerExecutor: WorkerExecutor): DefaultTask() {
    private val commitUuidState = project.property<String?>()
    private val projectTokenState = project.property<String?>()

    fun defaultCommitUuid(commitUuid: Provider<String?>) = commitUuidState.set(commitUuid)
    fun defaultProjectToken(projectToken: Provider<String?>) = projectTokenState.set(projectToken)

    @get:Option(option="commit-uuid", description="Commit UUID used by Codacy to track the current results. Typically inferred from the environment.")
    @get:Optional @get:Input var commitUuid by commitUuidState

    @get:Option(option="codacy-token", description="Codacy project token. Typically inferred from the environment.")
    @get:Optional @get:Input var projectToken by projectTokenState

    // TODO: is lateinit the best idiom here?
    @get:InputFile lateinit var coverageReport: File


    @TaskAction fun publishCoverageToCodacy() {
        workerExecutor.submit(CodacyUploadUow::class.java) { cfg ->
            cfg.isolationMode = IsolationMode.CLASSLOADER
            cfg.classpath = project.configurations["codacy"]
            cfg.params(coverageReport, commitUuid, projectToken)
        }
    }
}

open class CodacyUploadUow @Inject constructor (val coverageReport: File, val commitUuid: String?, val projectToken: String?) : Runnable {
    override fun run() {
        val codacyUploaderMain = Class.forName("com.codacy.CodacyCoverageReporter").getDeclaredMethod("main", Array<String>::class.java)
        val commitOpts = commitUuid?.let { arrayOf("--commitUUID", it) } ?: emptyArray()
        val tokenOpts = projectToken?.let { arrayOf("--projectToken", it) } ?: emptyArray()
        codacyUploaderMain.invoke(null,
                "-l", "Java",
                "-r", coverageReport,
                *commitOpts,
                *tokenOpts
        )
    }
}
