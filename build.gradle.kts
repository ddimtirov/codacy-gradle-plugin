plugins {
    id("com.gradle.build-scan") version "1.8"
    id("com.gradle.plugin-publish") version "0.9.7"
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "io.github.ddimtirov"
description = "Codacy.io integration for Gradle"

repositories.jcenter()
dependencies {
    testImplementation("junit:junit:4.12")
}

pluginBundle {
    description = "Upload Jacoco coverage reports to Codacy.com"
    website = "https://github.com/ddimtirov/codacy-gradle-plugin"
    vcsUrl  = "https://github.com/ddimtirov/codacy-gradle-plugin"
    tags = listOf("codacy", "jacoco", "coverage", "quality")

    (plugins) {
        "codacyPlugin" {
            id = "io.github.ddimtirov.codacy"
            displayName = "Codacy Coverage Upload Plugin"
        }
    }
}

gradlePlugin {
    (plugins) {
        "codacyPlugin" {
            id = pluginBundle.plugins["codacyPlugin"].id
            implementationClass = "io.github.ddimtirov.gradle.codacy.CodacyPlugin"
        }
    }
}

buildScan {
    setLicenseAgreementUrl("https://gradle.com/terms-of-service")
    setLicenseAgree("yes")
}