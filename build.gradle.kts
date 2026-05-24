import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation(kotlin("test"))

    intellijPlatform {
        goland(providers.gradleProperty("platformVersion").get())
        bundledPlugin("org.jetbrains.plugins.go")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("platformSinceBuild")
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            create(IntelliJPlatformType.GoLand, "2025.2.6.1")
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.2.6.2")
            create(IntelliJPlatformType.GoLand, "2025.3.4.1")
            create(IntelliJPlatformType.IntellijIdea, "2025.3.5")
            create(IntelliJPlatformType.GoLand, "2026.1.1")
            create(IntelliJPlatformType.IntellijIdea, "2026.1.1")
            create(IntelliJPlatformType.IntellijIdea, "261.25134.12")
            create(IntelliJPlatformType.IntellijIdea, "262.5752.32")
            create(IntelliJPlatformType.IntellijIdea, "262.6228.19")
        }
    }
    publishing {
        token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
    }
}
