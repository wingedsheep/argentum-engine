package com.wingedsheep.mtg.sets

import java.io.File

/**
 * Minimal golden-file ("approval") matcher for Kotest — the snapshot net behind Lesson 2 of
 * `backlog/phase-rs-lessons.md`.
 *
 * Compares [actual] against a committed golden resource on the test classpath. On mismatch it writes
 * the fresh output under `build/snapshots-actual/` and fails with the path, so the author can eyeball
 * the diff. To re-bless after an intentional SDK change, re-run with `-DupdateSnapshots=true`
 * (or env `UPDATE_SNAPSHOTS=1`), which rewrites the golden under `src/test/resources` and passes.
 *
 * Kotest ships no built-in approval matcher; this is the ~20-line helper the lesson calls for.
 */
object GoldenSnapshot {

    /**
     * Windows reserves the DOS device names (CON, PRN, AUX, NUL, COM1–9, LPT1–9) as filenames —
     * even with an extension — and Git for Windows refuses to check such paths out.
     */
    private val WINDOWS_RESERVED_NAMES = Regex("(?i)^(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])$")

    /**
     * [name] as a committable filename segment: unchanged unless it's a Windows-reserved device
     * name, which gets a trailing underscore (Conflux's per-set golden is `CON_.json`). Any golden
     * whose filename is minted from an arbitrary code (set code, card id, …) must go through this.
     */
    fun fileSafe(name: String): String =
        if (WINDOWS_RESERVED_NAMES.matches(name)) "${name}_" else name

    private val updateMode: Boolean =
        System.getProperty("updateSnapshots")?.toBooleanStrictOrNull() == true ||
            System.getenv("UPDATE_SNAPSHOTS").let { it != null && it.isNotBlank() && it != "0" }

    /** Assert [actual] matches the golden at `resourcePath` (relative to the test resource root). */
    fun verify(resourcePath: String, actual: String) {
        val expected = actual.trimEnd() + "\n"
        // Git may check text resources out with CRLF on Windows; generated JSON uses LF.
        val existing = javaClass.getResource("/$resourcePath")?.readText()?.replace("\r\n", "\n")

        if (updateMode) {
            val file = writeSource(resourcePath, expected)
            println("[GoldenSnapshot] re-blessed $resourcePath -> $file")
            return
        }
        if (existing == null) {
            val written = writeBuild(resourcePath, expected)
            throw AssertionError(
                "No golden for $resourcePath. Wrote actual to $written.\n" +
                    "Create it with: ./gradlew :mtg-sets:test --tests \"*CardDefinitionSnapshotTest\" -DupdateSnapshots=true",
            )
        }
        if (existing != expected) {
            val written = writeBuild(resourcePath, expected)
            throw AssertionError(
                "Snapshot mismatch for $resourcePath. Fresh output written to $written.\n" +
                    "Review the diff. If the change is intentional, re-bless with:\n" +
                    "  ./gradlew :mtg-sets:test --tests \"*CardDefinitionSnapshotTest\" -DupdateSnapshots=true",
            )
        }
    }

    private fun writeSource(resourcePath: String, content: String): File {
        val file = File(resolveDir(listOf("src/test/resources", "mtg-sets/src/test/resources")), resourcePath)
        file.parentFile.mkdirs()
        file.writeText(content)
        return file
    }

    private fun writeBuild(resourcePath: String, content: String): File {
        val file = File(resolveDir(listOf("build", "mtg-sets/build")), "snapshots-actual/$resourcePath")
        file.parentFile.mkdirs()
        file.writeText(content)
        return file
    }

    /** Gradle runs module tests with the module dir as the working dir; fall back to a repo-root path. */
    private fun resolveDir(candidates: List<String>): File =
        candidates.map(::File).firstOrNull { it.isDirectory }
            ?: File(candidates.first()).also { it.mkdirs() }
}
