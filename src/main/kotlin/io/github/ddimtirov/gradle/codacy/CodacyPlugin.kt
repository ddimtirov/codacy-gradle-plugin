@file:Suppress("unused")

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
            codacyExt.projectToken = null
            codacyExt.commitUuid = null

            configurations {
                "codacy" {
                    defaultDependencies {
                        add(project.dependencies.create("com.codacy:codacy-coverage-reporter:${codacyExt.toolVersion}"))
                    }
                }
            }

            val codacyUpload by tasks.creating {
                group = "publishing"
                description = "Upload all coverage reports to Codacy.com"
            }

            plugins.apply(JacocoPlugin::class.java)
            tasks.withType(JacocoReport::class.java) {
                val reportTask = this
                reports.xml.isEnabled = true
                tasks.create("${reportTask.name}CodacyUpload", CodacyUploadTask::class.java) {
                    group = "publishing"
                    description = "Upload ${reportTask.name} to Codacy.com"
                    coverageReport = reports.xml.destination
                    defaultCommitUuid(codacyExt.commitUuidProvider)
                    defaultProjectToken(codacyExt.projectTokenProvider)
                    dependsOn(reportTask)
                    codacyUpload.dependsOn(this)
                }
            }
        }
    }
}

// TODO: do we need separate state, prop and provider? What is the downside of exposing the PropertyState directly?
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

// TODO: do we need the separation of state, default-setter and delegated property? Can we make it more compact?
open class CodacyUploadTask @Inject constructor (private val workerExecutor: WorkerExecutor): DefaultTask() {
    private val commitUuidState = project.property<String?>()
    private val projectTokenState = project.property<String?>()

    fun defaultCommitUuid(commitUuid: Provider<String?>) = commitUuidState.set(commitUuid)
    fun defaultProjectToken(projectToken: Provider<String?>) = projectTokenState.set(projectToken)

    // TODO: do we really need @Option, @Optional and @Input? Especially the last can be implied?
    // FIXME: it appears that @Optional and providers don't work together well - see the BasicBuildTest
    @get:Option(option="commit-uuid", description="Commit UUID used by Codacy to track the current results. Typically inferred from the environment.")
    @get:Optional @get:Input var commitUuid by commitUuidState

    @get:Option(option="codacy-token", description="Codacy project token. Typically inferred from the environment.")
    @get:Optional @get:Input var projectToken by projectTokenState

    // TODO: is lateinit the best idiom here? Can we initialize somehow with provider/delegate?
    @get:InputFile lateinit var coverageReport: File


    @TaskAction fun publishCoverageToCodacy() {
        workerExecutor.submit(CodacyUploadUow::class.java) {
            isolationMode = IsolationMode.PROCESS
            classpath = project.configurations["codacy"]
            params(coverageReport, commitUuid, projectToken)
        }
    }
}

open class CodacyUploadUow @Inject constructor (
        private val coverageReport: File,
        private val commitUuid: String?,
        private val projectToken: String?
) : Runnable {
    override fun run() {
        val codacyUploaderMain = Class.forName("com.codacy.CodacyCoverageReporter").getDeclaredMethod("main", Array<String>::class.java)
        val commitOpts = commitUuid?.let { arrayOf("--commitUUID", it) } ?: emptyArray()
        val tokenOpts = projectToken?.let { arrayOf("--projectToken", it) } ?: emptyArray()
        codacyUploaderMain.invoke(null, arrayOf(
                "--language", "Java",
                "--coverageReport", coverageReport.canonicalPath,
                *commitOpts,
                *tokenOpts
        ))
    }
}
