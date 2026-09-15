import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

/**
 * Building against the locally installed IDE keeps the first build fast and guarantees the
 * bundled JPA plugin matches the IDE the plugin will actually run in. Falls back to a
 * downloaded IntelliJ IDEA Ultimate when `localIdePath` is unset or missing.
 */
val localIdePath: String? = providers.gradleProperty("localIdePath").orNull
    ?.takeIf { it.isNotBlank() && file(it).exists() }

dependencies {
    intellijPlatform {
        if (localIdePath != null) {
            local(localIdePath)
        } else {
            create(
                providers.gradleProperty("platformType"),
                providers.gradleProperty("platformVersion"),
            )
        }

        // com.intellij.javaee.jpa provides the JPAQL language this plugin injects.
        bundledPlugins("com.intellij.java", "com.intellij.javaee.jpa")

        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = "Dynamic Query"
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}

val javaVersion = providers.gradleProperty("javaVersion").get().toInt()

java {
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.release = javaVersion
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnit()
}
