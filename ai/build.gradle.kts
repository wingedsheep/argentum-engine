plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))

    implementation(libs.bundles.kotlinxEcosystem)

    testImplementation(testFixtures(project(":rules-engine")))
    testImplementation(project(":mtg-sets"))
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestProperty)
    testImplementation(kotlin("reflect"))
}
