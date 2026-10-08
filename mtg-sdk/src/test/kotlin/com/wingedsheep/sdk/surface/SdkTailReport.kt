package com.wingedsheep.sdk.surface

import io.kotest.core.spec.style.FunSpec
import java.io.File

/**
 * `just sdk-tail` — how long the SDK's long tail is, and which of its types look like fossils.
 *
 * Skipped unless `-DsdkTail=true`; it reads files outside this module (the card goldens and the
 * engine source), so it is a report, not a gate. Writes `mtg-sdk/build/reports/sdk-tail.md`:
 *
 * - **Per family:** how many leaves, how many no card uses, how many only one or two cards use, and
 *   how few leaves carry 90% of the uses. Usage is the number of card goldens
 *   (`mtg-sets/src/test/resources/snapshots/cards/`) whose JSON names the leaf's discriminator.
 *   A discriminator shared across families is credited to each; "used by no card" has false
 *   positives for types the engine builds at runtime or cards never serialize.
 * - **Fossil candidates, by shape:** a rarely-used leaf whose fields are a subset of (or equal to)
 *   a same-family leaf it shares a name word with — the general type may already say it.
 * - **Fossil candidates, by lowering:** a line in `rules-engine/src/main` that matches on one SDK
 *   leaf and constructs another on the same line (`is CantBeSacrificed -> … GrantKeyword(…)`) — the
 *   handler is a 1:1 translation, so cards could say the target type directly.
 *
 * Both fossil lists are heuristics to read, not verdicts. Read the report in a set-loop's finishing
 * PR; more than about one net new type per loop PR is worth a review comment.
 */
class SdkTailReport : FunSpec({

    test("write the SDK tail report").config(enabled = System.getProperty("sdkTail") == "true") {
        val report = buildReport(
            goldens = File("../mtg-sets/src/test/resources/snapshots/cards"),
            engineSource = File("../rules-engine/src/main/kotlin"),
        )
        val out = File("build/reports/sdk-tail.md")
        out.parentFile.mkdirs()
        out.writeText(report)
        // The full surface, one leaf per line with its fields — input for a baseline or a grep.
        File("build/reports/sdk-surface.txt").writeText(
            SdkSurface.leaves.joinToString("\n", postfix = "\n") { "${it.key} — ${it.fields.joinToString()}" }
        )
    }
})

private const val RARE = 2

private fun buildReport(goldens: File, engineSource: File): String {
    val leaves = SdkSurface.leaves
    val usage = cardUsage(goldens)
    fun uses(leaf: SdkSurface.Leaf) = usage[leaf.serialName] ?: 0
    val totalCards = usage[CARD_COUNT_KEY] ?: 0

    return buildString {
        appendLine("# SDK tail report")
        appendLine()
        appendLine("${leaves.size} sealed leaves in ${SdkSurface.byFamily.size} families, measured against $totalCards card goldens.")
        appendLine()
        appendLine("## Families")
        appendLine()
        appendLine("Families with at least 10 leaves, largest first.")
        appendLine()
        appendLine("| Family | Leaves | Used by no card | Used by 1–$RARE cards | Share 1–$RARE | Leaves covering 90% of uses |")
        appendLine("|---|---|---|---|---|---|")
        SdkSurface.byFamily.entries
            .filter { it.value.size >= 10 }
            .sortedByDescending { it.value.size }
            .forEach { (family, members) ->
                val counts = members.map { uses(it) }
                val unused = counts.count { it == 0 }
                val rare = counts.count { it in 1..RARE }
                val sorted = counts.sortedDescending()
                val total = sorted.sum()
                var acc = 0
                val cover90 = if (total == 0) 0 else sorted.indexOfFirst { acc += it; acc * 10 >= total * 9 } + 1
                appendLine("| `$family` | ${members.size} | $unused | $rare | ${percent(rare, members.size)} | $cover90 |")
            }
        val allRare = leaves.count { uses(it) in 1..RARE }
        appendLine()
        appendLine("Across all families: $allRare of ${leaves.size} leaves (${percent(allRare, leaves.size)}) are used by 1–$RARE cards.")

        appendLine()
        appendLine("## Fossil candidates — by shape")
        appendLine()
        appendLine("A leaf used by at most $RARE cards (counts in brackets) whose fields — at least one of them rare in its family — fit inside a same-family leaf's, sharing a name word.")
        appendLine()
        val shape = shapeCandidates(::uses)
        if (shape.isEmpty()) appendLine("None.")
        shape.forEach { appendLine(it) }

        appendLine()
        appendLine("## Fossil candidates — by lowering")
        appendLine()
        appendLine("An engine `when` arm that matches a rarely-used leaf and only builds one other leaf or `Modification` from literals: a 1:1 translation cards could spell directly.")
        appendLine()
        val lowering = loweringCandidates(engineSource, leaves, ::uses)
        if (lowering.isEmpty()) appendLine("None.")
        lowering.forEach { appendLine(it) }
    }
}

private fun shapeCandidates(uses: (SdkSurface.Leaf) -> Int): List<String> =
    SdkSurface.byFamily.values.flatMap { members ->
        // A field many of the family carry (`filter: GroupFilter`, `target: EffectTarget`) says nothing
        // about likeness; only a narrow type with a field that is rare in its family is a candidate.
        val fieldFrequency = members.flatMap { it.fields }.groupingBy { it }.eachCount()
        val common = (members.size / 20).coerceAtLeast(3)
        members.filter { narrow ->
            uses(narrow) <= RARE && narrow.fields.any { (fieldFrequency[it] ?: 0) <= common }
        }.flatMap { narrow ->
            val narrowFields = narrow.fields.toSet()
            val words = SdkSurface.nameWords(narrow)
            val wider = members.filter { general ->
                general.name != narrow.name &&
                    general.fields.toSet().containsAll(narrowFields) &&
                    words.intersect(SdkSurface.nameWords(general)).isNotEmpty() &&
                    uses(general) >= uses(narrow)
            }
            val (same, superset) = wider.partition { it.fields.size == narrow.fields.size }
            // A same-shape pair is reported once, in name order.
            same.map { general ->
                val (a, b) = listOf(narrow, general).sortedBy { it.name }
                "- `${a.name}` (${uses(a)}) and `${b.name}` (${uses(b)}) have the same fields — ${narrow.family}"
            } + listOfNotNull(superset.takeIf { it.isNotEmpty() }?.let { generals ->
                val shown = generals.sortedByDescending(uses).take(3).joinToString(", ") { "`${it.name}` (${uses(it)})" }
                val more = if (generals.size > 3) " and ${generals.size - 3} more" else ""
                "- `${narrow.name}` (${uses(narrow)}) has a subset of the fields of $shown$more — ${narrow.family}"
            })
        }
    }.distinct().sorted()

/**
 * Engine `when` arms that match a rarely-used SDK leaf and do nothing but build one other value from
 * literals: one other SDK leaf, or one `Modification`, whose arguments read nothing off the matched
 * value except its `filter`. `is CantBeSacrificed -> ContinuousEffectData(Modification.GrantKeyword(
 * CANT_BE_SACRIFICED), convertGroupFilter(ability.filter))` is the shape.
 */
private fun loweringCandidates(
    engineSource: File,
    leaves: List<SdkSurface.Leaf>,
    uses: (SdkSurface.Leaf) -> Int,
): List<String> {
    if (!engineSource.isDirectory) return listOf("_(engine source not found at ${engineSource.path})_")
    val bySimpleName = leaves.groupBy { it.simpleName }
    val armStart = Regex("""^(\s*)is\s+(?:[\w]+\.)*([A-Z]\w*)\s*->(.*)$""")
    val construct = Regex("""\b(?:Modification\.)?([A-Z]\w*)\(""")
    val packagePrefix = Regex("""\b(?:com|kotlin|java)\.(?:[a-z]\w*\.)+""")
    val memberRead = Regex("""\b[a-z]\w*\.[a-z]\w*""")
    val found = sortedSetOf<String>()
    engineSource.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
        val lines = file.readLines()
        lines.forEachIndexed { index, line ->
            val arm = armStart.find(line) ?: return@forEachIndexed
            val from = bySimpleName[arm.groupValues[2]]?.singleOrNull() ?: return@forEachIndexed
            if (uses(from) > RARE) return@forEachIndexed
            val indent = arm.groupValues[1].length
            val body = buildList {
                add(arm.groupValues[3])
                var i = index + 1
                while (i < lines.size && size <= 10) {
                    val next = lines[i]
                    val nextIndent = next.length - next.trimStart().length
                    if (next.isNotBlank() && nextIndent <= indent) break
                    add(next); i++
                }
            }
            if (body.size > 8) return@forEachIndexed
            val text = packagePrefix.replace(body.joinToString(" ") { it.trim() }, "")
            val built = construct.findAll(text).map { it.groupValues[1] }
                .filter { it != from.simpleName && (it in bySimpleName || text.contains("Modification.$it(")) }
                .filterNot { it == "ContinuousEffectData" }
                .distinct().toList()
            val to = built.singleOrNull() ?: return@forEachIndexed
            val reads = memberRead.findAll(text).map { it.value }
                .filterNot { it.endsWith(".filter") || it.endsWith(".name") }.toList()
            if (reads.isNotEmpty()) return@forEachIndexed
            val target = bySimpleName[to]?.singleOrNull()?.name ?: "Modification.$to"
            found += "- `${from.name}` (${uses(from)}) → `$target` — ${file.name}:${index + 1}"
        }
    }
    return found.toList()
}

private const val CARD_COUNT_KEY = "\u0000cards"

/** Discriminator → number of card goldens naming it at least once (plus the card total). */
private fun cardUsage(goldens: File): Map<String, Int> {
    val counts = HashMap<String, Int>()
    // Every string *value* (not key): a discriminator (`"type": "X"`) or a data object, which the
    // goldens write as a bare string (`"predicates": ["IsAttacking"]`).
    val discriminator = Regex(""""([A-Za-z][\w.]*)"(?!\s*:)""")
    var cards = 0
    goldens.listFiles { f -> f.extension == "json" }.orEmpty().forEach { file ->
        // A golden file is a run of `// Card Name` headers, each followed by that card's JSON.
        file.readText().split(Regex("""(?m)^// """)).filter { it.isNotBlank() }.forEach { block ->
            cards++
            discriminator.findAll(block).map { it.groupValues[1] }.toSet().forEach { counts.merge(it, 1, Int::plus) }
        }
    }
    counts[CARD_COUNT_KEY] = cards
    return counts
}

private fun percent(part: Int, whole: Int) = if (whole == 0) "–" else "${part * 100 / whole}%"
