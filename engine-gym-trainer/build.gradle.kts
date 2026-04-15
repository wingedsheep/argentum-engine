plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    // Self-play + MCTS over :engine-gym. Pure Kotlin library — no Spring.
    implementation(project(":engine-gym"))
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))

    implementation(libs.bundles.kotlinxEcosystem)
    implementation(kotlin("reflect"))

    testImplementation(project(":mtg-sets"))
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestProperty)
}
