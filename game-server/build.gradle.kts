plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinPluginSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
}

dependencies {
    implementation(project(":rules-engine"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.springBootWeb)

    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestExtensionsSpring)
    testImplementation(libs.kotlinxCoroutinesTest)
}
