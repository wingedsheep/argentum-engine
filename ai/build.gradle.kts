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
