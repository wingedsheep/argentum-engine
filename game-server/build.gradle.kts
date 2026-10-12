import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Inject

plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinPluginSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kover)
}

dependencies {
    implementation(project(":rules-engine"))
    implementation(project(":mtg-sdk"))
    implementation(project(":mtg-sdk-tooling"))
    implementation(project(":mtg-sets"))
    implementation(project(":mtg-search"))
    implementation(project(":ai"))
    // Argentum Assay — Oracle text -> CardDefinition, for the Scenario Builder's custom-card
    // sandbox only (dev-gated; see AssayCardService). Assay stays an auditor: nothing here loads
    // the card corpus through it, and a compiled card never leaves the session that asked for it.
    implementation(project(":oracle-assay"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.springBootWeb)
    implementation(libs.springBootStarterDataRedis)
    // Accounts / persistence: PostgreSQL via Spring Data JDBC + Flyway, magic-link email.
    // All gated behind `accounts.enabled` — the DataSource/Flyway/JDBC auto-config is excluded
    // by default so the server still boots (and tests run) with no database present.
    implementation(libs.springBootStarterDataJdbc)
    implementation(libs.springBootStarterMail)
    implementation(libs.springBootFlyway)
    implementation(libs.flywayCore)
    implementation(libs.flywayPostgresql)
    runtimeOnly(libs.postgresql)
    implementation(libs.springdocOpenapi)
    implementation(kotlin("reflect"))

    // Engine scenario harness (ScenarioTestBase, GameTestDriver, TestCards) — the canonical
    // home for the static-board scenario builder; game-server's ScenarioTestBase is a shim over it.
    testImplementation(testFixtures(project(":rules-engine")))
    testImplementation(libs.springBootStarterTest)
    testImplementation(libs.kotestRunner)
    testImplementation(libs.kotestAssertions)
    testImplementation(libs.kotestExtensionsSpring)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.mockk)
    // Real-Postgres integration tests for the JDBC repositories + Flyway migration.
    // Self-skips when Docker is unavailable (see PostgresAccountsRepositoryTest).
    testImplementation(libs.testcontainersPostgresql)
    testImplementation(libs.testcontainersJunit)
}

/**
 * Bakes the engine AI changelog (`ai/CHANGELOG.md`) into `engine-ai-history.tsv` on the classpath, for
 * the admin "AI vs humans" view: each entry becomes a marker on the win-rate timeline, and is
 * credited to the first deployed build that carried it.
 *
 * Every entry is resolved to its commit on main's first-parent line — by its `#pr` (the squash's
 * `(#pr)` suffix or the merge's "Merge pull request #pr"), by an explicit sha, or, with neither, by
 * the commit that added the line — which dates it and places it on main's history. The deploy stamps
 * builds with their git sha (`APP_VERSION`), so the file also lists every first-parent sha since the
 * oldest entry: that is what places a build on the same history.
 *
 * Format, newest first: a commit carrying entries is one `sha12 TAB committer-ISO TAB pr-or-dash TAB
 * text` line per entry; any other commit is a bare `sha12`. No git (a source-only Docker build) or a
 * shallow clone writes `# unavailable` instead, and the view says so rather than showing no changes.
 */
abstract class GenerateEngineAiHistory : DefaultTask() {
    @get:Inject abstract val exec: ExecOperations
    @get:Internal abstract val repoDir: DirectoryProperty
    @get:InputFile abstract val changelog: RegularFileProperty
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    init {
        // Also an input: git history, which Gradle can't fingerprint. Rerunning is a sub-second
        // `git log`, and processResources only repacks when the file's content actually changed.
        outputs.upToDateWhen { false }
    }

    private fun git(vararg args: String): String? {
        val out = ByteArrayOutputStream()
        val result = runCatching {
            exec.exec {
                workingDir = repoDir.get().asFile
                commandLine(listOf("git") + args)
                standardOutput = out
                errorOutput = OutputStream.nullOutputStream()
                isIgnoreExitValue = true
            }
        }.getOrNull() ?: return null
        return if (result.exitValue == 0) out.toString(Charsets.UTF_8) else null
    }

    private data class Commit(val sha: String, val date: String, val pr: String?)

    @TaskAction
    fun generate() {
        val file = outputDir.get().file("engine-ai-history.tsv").asFile
        file.parentFile.mkdirs()
        val log = if (git("rev-parse", "--is-shallow-repository")?.trim() == "false") {
            git("log", "--first-parent", "--format=%H%x09%cI%x09%s")
        } else null
        if (log == null) {
            file.writeText("# unavailable\n")
            return
        }
        val squashPr = Regex("""\(#(\d+)\)\s*$""")
        val mergePr = Regex("""^Merge pull request #(\d+)""")
        val commits = log.lines().filter { it.isNotBlank() }.map { line ->
            val (sha, date, subject) = line.split('\t', limit = 3)
            Commit(sha, date, (mergePr.find(subject) ?: squashPr.find(subject))?.groupValues?.get(1))
        }
        val byPr = commits.filter { it.pr != null }.associateBy { it.pr }
        val bySha = commits.associateBy { it.sha }

        val changelogFile = changelog.get().asFile
        val relativePath = changelogFile.relativeTo(repoDir.get().asFile).invariantSeparatorsPath
        val entryLine = Regex("""^- (?:(#\d+|[0-9a-f]{7,40})\s+)?(.+)$""")
        val entries = changelogFile.readLines()
            .dropWhile { it.trim() != "## Changes" }
            .mapNotNull { entryLine.matchEntire(it.trim()) }
        val resolved = LinkedHashMap<String, MutableList<String>>()
        for (match in entries) {
            val (anchor, text) = match.destructured
            val commit = when {
                anchor.startsWith("#") -> byPr[anchor.drop(1)]
                anchor.isNotEmpty() -> commits.firstOrNull { it.sha.startsWith(anchor) }
                // No anchor: the oldest first-parent commit that added this exact line.
                else -> git("log", "--first-parent", "--format=%H", "-S", match.value, "--", relativePath)
                    ?.lines()?.lastOrNull { it.isNotBlank() }?.let { bySha[it] }
            }
            if (commit == null) {
                logger.warn("ai/CHANGELOG.md: can't place \"{}\" on main's history yet; leaving it out", match.value)
                continue
            }
            val pr = anchor.takeIf { it.startsWith("#") }?.drop(1) ?: commit.pr ?: "-"
            resolved.getOrPut(commit.sha) { mutableListOf() } +=
                "${commit.sha.take(12)}\t${commit.date}\t$pr\t${text.replace('\t', ' ')}"
        }
        val oldest = commits.indexOfLast { it.sha in resolved }
        val body = commits.take(oldest + 1).flatMap { resolved[it.sha] ?: listOf(it.sha.take(12)) }
        file.writeText("# engine-ai-history v2\n" + body.joinToString("\n") + "\n")
    }
}

val generateEngineAiHistory = tasks.register<GenerateEngineAiHistory>("generateEngineAiHistory") {
    repoDir.set(rootProject.layout.projectDirectory)
    changelog.set(rootProject.layout.projectDirectory.file("ai/CHANGELOG.md"))
    outputDir.set(layout.buildDirectory.dir("generated/engine-ai-history"))
}

sourceSets.main {
    resources.srcDir(generateEngineAiHistory)
}
