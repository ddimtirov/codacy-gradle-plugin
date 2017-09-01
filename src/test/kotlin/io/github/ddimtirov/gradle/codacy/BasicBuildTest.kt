package io.github.ddimtirov.gradle.codacy

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildLogicFunctionalTest {
    @get:Rule val testProjectDir = TemporaryFolder()
    private lateinit var projectFile: File

    @Before fun createSources() {

        testProjectDir.newFolder("src", "main", "java")
        testProjectDir.newFile("src/main/java/Hello.java").bufferedWriter().use { it.write("""
            public class Hello {
                public void hi() { System.out.println("hi"); }
            }
        """)}

        testProjectDir.newFolder("src", "test", "java")
        testProjectDir.newFile("src/test/java/HelloTest.java").bufferedWriter().use { it.write("""
            import org.junit.Test;
            public class HelloTest {
                @Test public void hiTest() { new Hello().hi(); }
            }
        """)}

        projectFile = testProjectDir.newFile("build.gradle")
        projectFile.bufferedWriter().use { it.write("""
            plugins {
                id "java"
                id "io.github.ddimtirov.codacy" version "0.1.0"
            }
            repositories.jcenter()
            dependencies { testImplementation("junit:junit:4.12") }
        """)}
    }

    @Test fun testPlainBuild() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments("test", "jacocoTestReport", "jacocoTestReportCodacyUpload", "-s")
                .build()

        assertEquals(result.task(":jacocoTestReport")!!.outcome, SUCCESS)
        assertEquals(result.task(":jacocoTestReportCodacyUpload")!!.outcome, SUCCESS)
    }

    @Test fun testFullySpecifiedBuild() {
        projectFile.appendText("""
            codacy {
                commitUuid = "abc123"
                projectToken = "my secret token"
            }
        """)
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments("test", "jacocoTestReport", "jacocoTestReportCodacyUpload", "-s")
                .build()

        assertEquals(result.task(":jacocoTestReport")!!.outcome, SUCCESS)
        assertEquals(result.task(":jacocoTestReportCodacyUpload")!!.outcome, SUCCESS)
    }

    @Test fun testCmdLineOptionsBuild() {
        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withArguments("test", "jacocoTestReport", "jacocoTestReportCodacyUpload", "-s",
                        "--commit-uuid", "abc123",
                        "--project-token", "secrettoken")
                .build()

        assertEquals(result.task(":jacocoTestReport")!!.outcome, SUCCESS)
        assertEquals(result.task(":jacocoTestReportCodacyUpload")!!.outcome, SUCCESS)
    }
}
