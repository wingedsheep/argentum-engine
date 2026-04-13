plugins {
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":mtg-sdk"))
    kover(project(":rules-engine"))
    kover(project(":game-server"))
}
