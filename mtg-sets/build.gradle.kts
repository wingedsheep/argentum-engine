plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":mtg-sdk"))

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}

tasks.withType<Test> {
    systemProperty("verifyImageUris", System.getProperty("verifyImageUris") ?: "false")
}
