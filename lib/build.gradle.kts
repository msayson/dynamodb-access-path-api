plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // Apply the JaCoCo plugin for code coverage reports.
    jacoco

    // Apply the detekt static analysis plugin for Kotlin code.
    // See: https://detekt.dev/docs/1.23.8/gettingstarted/gradle
    id("io.gitlab.arturbosch.detekt") version "1.23.8"

    // Apply the maven-publish plugin for publishing artifacts to a Maven repository.
    `maven-publish`

    // Apply the signing plugin for signing artifacts before publishing.
    signing
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // AWS SDK for Kotlin - DynamoDB client
    implementation("aws.sdk.kotlin:dynamodb:1.6.80")

    // Coroutines for suspend support in resolver and tests
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")

    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mocking library for Kotlin with JUnit 5 support
    testImplementation("io.mockk:mockk:1.14.9")

    // Testcontainers for local integration testing.
    // See: https://java.testcontainers.org/quickstart/junit_5_quickstart/
    testImplementation("org.testcontainers:localstack:1.21.4")
    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.3")

    // Detekt dependencies for static code analysis.
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

jacoco {
    toolVersion = "0.8.10"
}

detekt {
    // Version of detekt to use. For a list of available versions,
    // see https://github.com/detekt/detekt/releases.
    toolVersion = "1.23.8"

    // Directory where detekt will search for source files.
    source.setFrom("src/main/kotlin", "src/test/kotlin")

    // Specify custom detekt config file for overriding lint rules.
    config.setFrom("$projectDir/config/detekt.yml")

    // Builds the AST in parallel, can speed up builds in larger projects.
    parallel = true

    // Adds debug output during task execution. `false` by default.
    debug = false
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("localIntegTest")
    }

    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.95".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "21"

    reports {
        html.required.set(true) // observe findings in your browser with structure and code snippets
        txt.required.set(true) // similar to the console output, contains issue signature to manually edit baseline files
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
    jvmTarget = "21"
}

// Define task to run local integ tests
val localIntegTest = tasks.register<Test>("localIntegTest") {
    description = "Runs local integration tests."
    group = "verification"

    useJUnitPlatform {
        includeTags("localIntegTest")
    }

    shouldRunAfter(tasks.test)

    onlyIf {
        System.getProperty("runLocalIntegTests") == "true"
    }

    // Disable JaCoCo for integ tests
    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        isEnabled = false
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "io.github.msayson"
            artifactId = "dynamodb-access-path"
            version = "0.1.0"

            pom {
                name.set("dynamodb-access-path")
                description.set("A Kotlin library that determines optimal DynamoDB access paths based on table metadata.")
                url.set("https://github.com/msayson/dynamodb-access-path")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("msayson")
                        name.set("Mark Sayson")
                        url.set("https://github.com/msayson")
                    }
                }

                scm {
                    url.set("https://github.com/msayson/dynamodb-access-path")
                    connection.set("scm:git:https://github.com/msayson/dynamodb-access-path.git")
                    developerConnection.set("scm:git:ssh://git@github.com:msayson/dynamodb-access-path.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "CentralPortal"
            url = uri("https://central.sonatype.com/api/v1/publisher/upload")

            credentials {
                username = findProperty("centralUsername") as String?
                password = findProperty("centralPassword") as String?
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
