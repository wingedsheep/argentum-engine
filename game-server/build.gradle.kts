plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinPluginSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

dependencies {
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))
    implementation(project(":mtg-sets"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.springBootWeb)
    implementation(libs.springBootStarterDataRedis)
    implementation(libs.springdocOpenapi)
    implementation(kotlin("reflect"))

    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestExtensionsSpring)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.mockk)
}
