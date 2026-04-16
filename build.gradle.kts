// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}

val checkJavaEnv by tasks.registering {
    group = "verification"
    description = "Checks Java runtime before Android build tasks run"

    doLast {
        val javaHomeCandidates = listOf(
            System.getenv("JAVA_HOME"),
            providers.gradleProperty("org.gradle.java.home").orNull,
            System.getProperty("java.home")
        ).mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }

        val resolvedJavaHome = javaHomeCandidates.firstOrNull()
            ?: throw GradleException(
                "No Java runtime detected. Configure JAVA_HOME or org.gradle.java.home."
            )

        val javaExecutable = if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
            file("$resolvedJavaHome/bin/java.exe")
        } else {
            file("$resolvedJavaHome/bin/java")
        }

        if (!javaExecutable.exists()) {
            throw GradleException(
                "Invalid Java home: $resolvedJavaHome (${javaExecutable.name} not found in bin)."
            )
        }

        val source = when {
            !System.getenv("JAVA_HOME").isNullOrBlank() -> "JAVA_HOME"
            !providers.gradleProperty("org.gradle.java.home").orNull.isNullOrBlank() -> "org.gradle.java.home"
            else -> "Gradle runtime java.home"
        }
        println("Java runtime OK ($source): $resolvedJavaHome")
    }
}

gradle.projectsEvaluated {
    subprojects {
        tasks.matching { it.name == "preBuild" }
            .configureEach {
                dependsOn(rootProject.tasks.named("checkJavaEnv"))
            }
    }
}