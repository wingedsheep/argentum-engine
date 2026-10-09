// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
    kotlin("jvm")
    // For the `api` configuration: the split card-definition modules re-export each other along a
    // chronological chain, and `:mtg-sets` re-exports the whole chain, so consumers still see the
    // full corpus through a single `project(":mtg-sets")` dependency.
    `java-library`
}

kotlin {
    // Use a specific Java version to make it easier to work in different environments.
    jvmToolchain(21)
}

// Bound concurrent Kotlin compilations build-wide — they all share one compile-daemon heap, so
// without this the peak is workers.max × the largest source set. See KotlinCompileThrottle.
val kotlinCompileThrottle =
    gradle.sharedServices.registerIfAbsent("kotlinCompileThrottle", KotlinCompileThrottle::class) {
        maxParallelUsages.set(
            (project.findProperty("kotlinCompileParallelism") as String? ?: "2").toInt()
        )
    }

tasks.withType<KotlinCompile>().configureEach {
    usesService(kotlinCompileThrottle)
}

tasks.withType<Test>().configureEach {
    // Configure all test Gradle tasks to use JUnitPlatform.
    useJUnitPlatform()

    // Increase heap size for Spring Boot integration tests with WebSocket connections
    maxHeapSize = if (System.getProperty("eclCollect") == "true") "4g" else "2g"

    // Forward benchmark flags so benchmark tests can check them
    systemProperty("benchmark", System.getProperty("benchmark") ?: "false")
    systemProperty("benchmarkGames", System.getProperty("benchmarkGames") ?: "10")
    systemProperty("benchmarkMaxTurns", System.getProperty("benchmarkMaxTurns") ?: "50")
    systemProperty("benchmarkOutputDir", System.getProperty("benchmarkOutputDir") ?: System.getProperty("java.io.tmpdir"))
    systemProperty("benchmarkSet", System.getProperty("benchmarkSet") ?: "POR")

    // Forward per-player benchmark config
    for (prefix in listOf("p1", "p2")) {
        for (suffix in listOf("Type", "Model", "BaseUrl", "ApiKey", "DeckBuilder", "ReasoningEffort")) {
            System.getProperty("$prefix$suffix")?.let { systemProperty("$prefix$suffix", it) }
        }
    }

    // Forward the arena flags (see ai/src/test/kotlin/com/wingedsheep/ai/arena) so `just arena`
    // and `just arena-gauntlet` reach the test JVM. Both recipes also pass -Dbenchmark=true, which
    // is what relaxes TestHangGuard for a run that legitimately takes half an hour.
    for (prop in listOf(
        "arena", "arenaGauntlet", "arenaPod", "arenaBudgetScaling", "arenaTable", "arenaA", "arenaB",
        "argentum.ai.apprentice.dir",
        "eclCollect", "eclCollectGames", "eclCollectSeed", "eclCollectOutput", "eclCollectBaseDir",
        "eclCollectRunId", "eclCollectStartIndex",
        "arenaGames", "arenaSeed", "arenaSet", "arenaMaxTurns", "arenaThreads",
        "gameLog", "gameLogGames", "gameLogSeed", "gameLogAgent", "gameLogDir",
    )) {
        System.getProperty(prop)?.let { systemProperty(prop, it) }
    }

    // Forward the anti-hang guard tunables (see TestHangGuard) so they can be set from the CLI.
    // Also forward the opt-in flags for the heavy replay-divergence fuzzer (ReplayDivergenceReproTest),
    // which is skipped in CI and only runs when `runReproTests=true` is passed on the CLI.
    for (prop in listOf("testTimeoutSeconds", "testHardTimeoutSeconds", "runReproTests", "reproGames", "reproSet")) {
        System.getProperty(prop)?.let { systemProperty(prop, it) }
    }

    // Log information about all test results, not only the failed ones.
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
        showStandardStreams = System.getProperty("benchmark") == "true"
    }
}
