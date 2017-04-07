package io.github.ddimtirov.gradle.codacy

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.tasks.options.Option
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.testing.jacoco.plugins.JacocoPlugin
import org.gradle.testing.jacoco.tasks.JacocoReport

// TODO: deploy descriptors, test, doc, publish
class CodacyPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def codacyExt = project.extensions.create('codacy', CodacyExtension)
        def codacyClasspath = project.configurations.create('codacy') { c ->
            c.defaultDependencies { dependencies ->
                dependencies << project.dependencies.create(
                        group: 'com.codacy',
                        name: 'codacy-coverage-reporter',
                        version: codacyExt.toolVersion
                )
            }
        }

        def codacyUploadTask = project.tasks.create('codacyUpload') {
            it.group = 'publishing'
            it.description = 'Upload all coverage reports to Codacy.com'
        }

        project.pluginManager.apply(JacocoPlugin)
        project.tasks.all { Task task ->
            if (task instanceof JacocoReport) {
                def reportTask = task as JacocoReport
                reportTask.reports.xml.enabled = true
                project.tasks.create("${task.name}CodacyUpload", CodacyUploadTask) {
                    it.group = 'publishing'
                    it.description = "Upload $task.name to Codacy.com"
                    it.coverageReport = { reportTask.reports.xml.destination }
                    it.classpath codacyClasspath
                    it.dependsOn task
                    codacyUploadTask.dependsOn it
                }
            }
        }
    }
}

class CodacyExtension {
    def toolVersion = '1.0.7'
}

class CodacyUploadTask extends DefaultTask {
    @Option(option='commit-uuid', description = 'Commit UUID used by Codacy to track the current results. Typically inferred from the environment.')
    @Optional @Input commitUuid

    @InputFile coverageReport
    @Classpath classpath

    @TaskAction publishCoverageToCodacy() {
        project.javaexec {
            it.main = "com.codacy.CodacyCoverageReporter"
            classpath = project.configurations.codacy
            it.classpath classpath
            it.args "-l", "Java"
            it.args "-r", project.file(coverageReport)
            if (commitUuid) it.args '--commitUUID', commitUuid
        }
    }
}
