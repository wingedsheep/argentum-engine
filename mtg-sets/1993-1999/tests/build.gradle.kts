// Scenario tests for cards from sets released 1993–1999 — the test-side mirror of `:mtg-sets:1993-1999`.
//
// One test file per card (AGENTS.md) means this suite grows on every card PR; at ~3,000 files it
// had outgrown a single Kotlin compilation inside :rules-engine, which is what the compile daemon
// kept running out of heap on (docs/build-performance-plan.md). Sharding it by the era of the card
// under test bounds each compilation and puts a card's test in the module that mirrors the card's
// own — the tests sit in a `tests` child of the era module, so a set's PR touches one directory.
//
// This depends on the `:mtg-sets` aggregator rather than `:mtg-sets:1993-1999`: ScenarioTestBase builds
// its registry from MtgSetCatalog, so every scenario still sees the whole catalog exactly as before.
// The era only decides which module a file lives in, never what it can reach.
plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    testImplementation(project(":mtg-sdk"))
    testImplementation(project(":rules-engine"))
    // GameTestDriver / TestCards / ScenarioTestBase / TestHangGuard.
    testImplementation(testFixtures(project(":rules-engine")))
    // The whole card corpus — scenario tests import the definitions they exercise.
    testImplementation(project(":mtg-sets"))

    testImplementation(libs.kotlinxSerialization)
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}
