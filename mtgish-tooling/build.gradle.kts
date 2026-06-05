plugins {
    id("buildsrc.convention.kotlin-jvm")
    application
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    // Pure tooling: scans repo source files as text + reads JSON. No engine/SDK classpath
    // dependency — the SDK capability registry is recovered from Kotlin *source*, not reflection,
    // exactly like the original Python spike, so this can't drift behind a stale compiled jar.
    implementation(libs.kotlinxSerialization)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}

application {
    // Single dispatch entrypoint: `probe` / `fidelity` / `autogen` subcommands mirror the
    // three original Python scripts. The justfile coverage* recipes call these.
    mainClass.set("com.wingedsheep.tooling.coverage.MainKt")
}

// Relative paths inside the tool (spike data dir, generated staging) resolve against the repo root,
// not the module dir — but the tool also discovers the repo root itself by walking up, so this is
// belt-and-suspenders for the `run` task.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
