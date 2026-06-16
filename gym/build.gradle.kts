plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    // Wraps the rules engine with a stateful RL/MCTS-friendly environment.
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))
    implementation(project(":ai"))

    implementation(libs.bundles.kotlinxEcosystem)
    // :ai's deck generation (SealedDeckGenerator → Draftsim autobuilder) logs via slf4j. We don't
    // compile against it — the dependency is transitive — but the API must be on the runtime
    // classpath, which propagates to non-Spring consumers like :gym-trainer. Spring consumers
    // (:gym-server) already supply a binding.
    runtimeOnly(libs.slf4jApi)

    testImplementation(project(":mtg-sets"))
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestProperty)
    testImplementation(kotlin("reflect"))
}
