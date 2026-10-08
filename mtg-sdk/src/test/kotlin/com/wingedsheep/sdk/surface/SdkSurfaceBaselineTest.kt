package com.wingedsheep.sdk.surface

import io.kotest.core.spec.style.FunSpec
import java.io.File

/**
 * A review prompt on every new sealed SDK leaf — not a ban on adding one.
 *
 * `src/test/resources/sdk-surface-baseline.txt` lists every concrete leaf of every sealed SDK family
 * ([SdkSurface]). A leaf the file doesn't list fails this test, and the failure prints the leaf's
 * nearest neighbours — same family, identical fields, shared name words — next to a paste-ready line.
 * Adding the line is the whole fix, but writing it means answering the question
 * `docs/sdk-design-principles.md` ("Extend before you add") asks of every new type: which two existing
 * types come closest, and why neither can take the axis?
 *
 * Lines under `[grandfathered]` are the surface as it stood when this test landed: name and family
 * only. Lines under `[added]` carry five ` — `-separated fields:
 *
 *     Name — Family — first card — Closest1, Closest2 — why neither can take the axis
 *
 * A leaf that no longer exists fails too — delete its line, it's a type the SDK lost.
 *
 * Modelled on `EffectExecutorCoverageTest` in `:rules-engine`.
 */
class SdkSurfaceBaselineTest : FunSpec({

    val baseline = Baseline.load()
    val current = SdkSurface.leaves.associateBy { it.key }

    test("every sealed SDK leaf is in the surface baseline") {
        val missing = current.keys - baseline.keys
        if (missing.isNotEmpty()) {
            error(buildString {
                appendLine("${missing.size} new sealed SDK leaf type(s). Before adding a line for each to")
                appendLine("${Baseline.PATH} under [added], check the neighbours below: could one of them")
                appendLine("take the new axis instead (docs/sdk-design-principles.md, \"Extend before you add\")?")
                for (key in missing.sorted()) {
                    val leaf = current.getValue(key)
                    appendLine()
                    appendLine("${signature(leaf)}  [${leaf.family}]")
                    val neighbours = SdkSurface.neighbours(leaf)
                    if (neighbours.isEmpty()) appendLine("    no neighbour shares a field or a name word")
                    for ((other, _) in neighbours) appendLine("    ~ ${signature(other)}")
                    val closest = neighbours.take(2).joinToString(", ") { it.first.simpleName }.ifEmpty { "<Closest1>, <Closest2>" }
                    appendLine("  line: ${leaf.name} — ${leaf.family} — <first card> — $closest — <why neither can take the axis>")
                }
            })
        }
    }

    test("the surface baseline lists no leaf that is gone") {
        val stale = baseline.keys - current.keys
        if (stale.isNotEmpty()) {
            error(
                "Leaves in ${Baseline.PATH} that no longer exist — delete these lines:\n" +
                    stale.sorted().joinToString("\n") { "  $it" }
            )
        }
    }

    test("[added] lines are sorted by name") {
        // Sorted, not appended, so parallel PRs each adding a type insert at different places in
        // the file instead of all colliding at its end.
        val names = baseline.added.map { it.substringBefore(" — ").trim() }
        if (names != names.sorted()) {
            error("Keep the [added] lines in ${Baseline.PATH} sorted by name:\n" + names.sorted().joinToString("\n") { "  $it" })
        }
    }

    test("every [added] line names the first card, two closest types and why not those") {
        val bad = baseline.added.filter { line ->
            val parts = line.split(" — ").map { it.trim() }
            parts.size < 5 ||
                parts.any { it.isEmpty() || it.startsWith("<") } ||
                parts[3].split(',').map { it.trim() }.count { it.isNotEmpty() } < 2
        }
        if (bad.isNotEmpty()) {
            error(
                "Malformed [added] lines in ${Baseline.PATH} — expected " +
                    "`Name — Family — first card — Closest1, Closest2 — why not those`:\n" +
                    bad.joinToString("\n") { "  $it" }
            )
        }
    }
})

private fun signature(leaf: SdkSurface.Leaf) =
    if (leaf.fields.isEmpty()) leaf.name else "${leaf.name}(${leaf.fields.joinToString()})"

private class Baseline(val keys: Set<String>, val added: List<String>) {
    companion object {
        const val PATH = "mtg-sdk/src/test/resources/sdk-surface-baseline.txt"

        fun load(): Baseline {
            val text = Baseline::class.java.getResource("/sdk-surface-baseline.txt")?.readText()
                ?: File("src/test/resources/sdk-surface-baseline.txt").readText()
            val keys = mutableSetOf<String>()
            val added = mutableListOf<String>()
            var section = ""
            for (raw in text.lines()) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                if (line.startsWith("[") && line.endsWith("]")) { section = line; continue }
                val parts = line.split(" — ")
                require(parts.size >= 2) { "Baseline line without a family: $line" }
                keys += "${parts[0].trim()} — ${parts[1].trim()}"
                if (section == "[added]") added += line
            }
            return Baseline(keys, added)
        }
    }
}
