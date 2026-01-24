plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":mtg-sdk"))

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}
