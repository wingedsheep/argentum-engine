plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))

    implementation(libs.bundles.kotlinxEcosystem)
    compileOnly(libs.slf4jApi)

    // slf4j is compileOnly above (the consuming runtime — game-server — supplies a binding); the
    // module's own tests exercise code paths that initialize loggers, so they need the API present.
    testImplementation(libs.slf4jApi)
    testImplementation(testFixtures(project(":rules-engine")))
    testImplementation(project(":mtg-sets"))
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestProperty)
    testImplementation(kotlin("reflect"))
}

// AI decision tests must be reproducible: `GameState.rng` seeds the rollouts and the hidden-
// information determinizations, so a scenario seeded from the clock (ScenarioTestBase's default)
// lets the same position pass on one run and fail on the next. Pin it here; a test that wants a
// particular seed still calls `withRngSeed`. Pass `-Dargentum.scenario.rngSeed=<n>` to rerun the
// suite under another seed and find assertions that only hold by luck.
tasks.withType<Test>().configureEach {
    systemProperty(
        "argentum.scenario.rngSeed",
        System.getProperty("argentum.scenario.rngSeed") ?: "1",
    )
}
