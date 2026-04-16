plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kover)
    `java-test-fixtures`
}

dependencies {
    // SDK dependency - the shared contract
    implementation(project(":mtg-sdk"))

    implementation(libs.bundles.kotlinxEcosystem)

    // Shared test fixtures (GameTestDriver, TestCards) usable by :rules-engine tests
    // and by other modules via testImplementation(testFixtures(project(":rules-engine"))).
    testFixturesImplementation(project(":mtg-sdk"))
    testFixturesImplementation(project(":mtg-sets"))
    testFixturesImplementation(libs.bundles.kotlinxEcosystem)
    testFixturesImplementation(kotlin("reflect"))

    testImplementation(project(":mtg-sets"))
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestProperty)
    testImplementation(kotlin("reflect"))
}
