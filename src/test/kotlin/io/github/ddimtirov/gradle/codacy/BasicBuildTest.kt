package io.github.ddimtirov.gradle.codacy

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BuildLogicFunctionalTest {
    @get:Rule val testProjectDir = TemporaryFolder()


    @Test fun testPlainBuild() {
        testProjectDir.newFile("build.gradle").bufferedWriter().use { it.write("""
            plugins {
                id "java"
                id "io.github.ddimtirov.codacy" version "0.1.0"
            }
            repositories.jcenter()
            dependencies { testImplementation("junit:junit:4.12") }
        """)}

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

        val result = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withPluginClasspath()
                .withArguments("test", "jacocoTestReport", "jacocoTestReportCodacyUpload", "-s")
                .build()

        assertEquals(result.task(":jacocoTestReport")!!.outcome, SUCCESS)
        assertEquals(result.task(":jacocoTestReportCodacyUpload")!!.outcome, SUCCESS)
    }
}
