plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    implementation(project(":mtg-sdk"))
    implementation(libs.classgraph)
    implementation(libs.kotlinxSerialization)

    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
}

tasks.withType<Test> {
    systemProperty("verifyImageUris", System.getProperty("verifyImageUris") ?: "false")
    // Forward the snapshot re-bless switch into the forked test JVM (see CardDefinitionSnapshotTest).
    System.getProperty("updateSnapshots")?.let { systemProperty("updateSnapshots", it) }
}

// One-shot Scryfall sync — populates legalities.json from the live Scryfall API.
// Run with: ./gradlew :mtg-sets:syncLegality
tasks.register<JavaExec>("syncLegality") {
    description = "Fetch deck-format legality for every registered card from Scryfall."
    group = "build"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.wingedsheep.mtg.sets.legality.SyncLegalitiesKt")
    workingDir = rootProject.projectDir
}

// Offline sync from a Scryfall bulk-data dump. Pass the dump path via --args.
// Run with: ./gradlew :mtg-sets:syncLegalityFromDump --args="/path/to/all-cards.json"
tasks.register<JavaExec>("syncLegalityFromDump") {
    description = "Populate legalities.json from a local Scryfall bulk-data dump."
    group = "build"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.wingedsheep.mtg.sets.legality.SyncLegalitiesFromDumpKt")
    workingDir = rootProject.projectDir
}

// Offline sync of color identities. Walks every card definition .kt file under
// mtg-sets/.../definitions/<set>/cards/ and adds or updates `colorIdentity = "..."` from a
// Scryfall bulk-data dump.
// Run with: ./gradlew :mtg-sets:syncColorIdentityFromDump --args="/path/to/all-cards.json"
tasks.register<JavaExec>("syncColorIdentityFromDump") {
    description = "Patch every card .kt file with its Scryfall color identity."
    group = "build"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.wingedsheep.mtg.sets.colors.SyncColorIdentityFromDumpKt")
    workingDir = rootProject.projectDir
}

// === mtgish auto-generator: compile-verification gate (Hybrid design) ===========================
// The Kotlin mtgish emitter writes draft cards into this isolated source set under a
// distinct `generated.<set>.cards` package (never colliding with the real definitions on the
// classpath). Gradle compiles them — so a draft that doesn't compile fails the build — and the
// verifier serialises each via the same CardExporter that produces the golden snapshots. A small
// `fidelity --gate` step then gameplay-tree diffs the serialised trees against the golden, turning the
// fidelity AUTO tier from a static prediction into a real "compiles + golden-equivalent" check.
// Run with: just coverage-verify --set POR   (or ./gradlew :mtg-sets:verifyGeneratedCards -Pset=POR)
val generatedCardsDir = layout.buildDirectory.dir("generated-cards/src")
val generatorSet = (project.findProperty("set") as String? ?: "POR").toString().uppercase()

sourceSets { create("generatedCards") }
kotlin.sourceSets.named("generatedCards") { kotlin.srcDir(generatedCardsDir) }
dependencies { "generatedCardsImplementation"(project(":mtg-sdk")) }

// Run the :mtgish-tooling CLI's emit-all on its runtime classpath (build-time tool dependency only — keeps
// :mtg-sets's main classpath free of the mtgish tooling).
val mtgishTool: Configuration by configurations.creating { isCanBeResolved = true; isCanBeConsumed = false }
dependencies { mtgishTool(project(":mtgish-tooling")) }

val emitGeneratedCards by tasks.registering(JavaExec::class) {
    description = "Emit whole-renderable cards for -Pset=CODE via the mtgish bridge."
    group = "verification"
    workingDir = rootProject.projectDir
    classpath = mtgishTool
    mainClass.set("com.wingedsheep.tooling.coverage.MainKt")
    args("autogen", "--set", generatorSet, "--emit-all", "--out", generatedCardsDir.get().asFile.absolutePath)
}
tasks.named("compileGeneratedCardsKotlin") { dependsOn(emitGeneratedCards) }

tasks.register<JavaExec>("verifyGeneratedCards") {
    description = "Compile the mtgish-generated cards and serialise them for the capability gate."
    group = "verification"
    dependsOn("compileGeneratedCardsKotlin")
    classpath = files(sourceSets["generatedCards"].output, sourceSets["main"].runtimeClasspath)
    mainClass.set("com.wingedsheep.mtg.sets.codegen.GeneratedCardVerifierKt")
    workingDir = rootProject.projectDir
    args(
        "com.wingedsheep.mtg.sets.generated.${generatorSet.lowercase()}.cards",
        layout.buildDirectory.file("generated-cards/${generatorSet.lowercase()}.generated.json")
            .get().asFile.absolutePath
    )
}
