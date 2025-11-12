plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.jmh)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.serialization.json)
    implementation(libs.koin.core)
    
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.mockk)
    
    // JMH benchmarking
    jmh(libs.jmh.core)
    jmh(libs.jmh.generator)
}

tasks.withType<Test> {
    useJUnitPlatform()
    
    // Enable parallel test execution
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
    
    // Configure test reports
    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }
    
    // Configure test logging
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

// JMH configuration
jmh {
    iterations = 3
    warmupIterations = 2
    fork = 1
    timeUnit = "ms"
    resultFormat = "JSON"
    resultsFile = project.layout.buildDirectory.file("reports/jmh/results.json")
}
