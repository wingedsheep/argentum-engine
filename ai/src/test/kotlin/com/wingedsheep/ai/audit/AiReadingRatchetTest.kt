package com.wingedsheep.ai.audit

import com.wingedsheep.ai.arena.harnessRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import java.io.File
import java.util.Locale

/**
 * The AI card-reading ratchet: the full-corpus audit ([AiReadingAudit]) held against a checked-in
 * per-card baseline, `src/test/resources/ai-reading-baseline.tsv`, so coverage only moves up.
 *
 * It fails on exactly two things:
 *
 *  1. **A card the baseline knows gains a flag.** A reader lost a case, or an edit to that card now
 *     uses an effect type the AI doesn't read. This is the regression check: every per-group unread
 *     count and the unread-card total over the baselined cards can only rise this way.
 *  2. **A flag key no baselined card has** — a new `<check>: <effect type>` group. A new card brought
 *     an effect type the AI has never been asked to read, or a reader change opened a new gap.
 *
 * Everything else passes, deliberately. A card the baseline doesn't list yet (one a concurrent set
 * loop added since the last re-bless) passes when it is read fully or lands only in groups the
 * baseline already has: corpus growth into known gaps isn't a regression, and failing it would turn
 * every card PR into a baseline edit and every pair of card PRs into a merge conflict. Shrinking
 * flags and removed cards pass too, with a hint to re-bless and lock the gain in.
 *
 * Re-bless with `just ai-reading-baseline` (`-DaiReadingBaseline=<path>` rewrites the file and
 * passes). See `docs/ai/reading-audit.md`, "The ratchet".
 */
class AiReadingRatchetTest : FunSpec({

    test("no card the AI could read is lost, and no new unread effect type appears") {
        val registry = harnessRegistry(MtgSetCatalog.all)
        val current = AiReadingAudit.cardFlags(MtgSetCatalog.all.flatMap { it.cards }) { name -> registry.getCard(name) }

        System.getProperty("aiReadingBaseline")?.takeIf { it.isNotBlank() }?.let { path ->
            File(path).writeText(Baseline.render(current))
            println("Wrote the AI reading baseline (${current.size} cards, ${current.values.count { it.isEmpty() }} read fully) to $path")
            return@test
        }

        val baseline = Baseline.load()
        val diff = RatchetDiff.of(baseline, current)
        println(diff.summary(current))
        if (diff.failed) error(diff.failureMessage())
    }
})

/** Card name → flag keys, as the checked-in baseline records them. */
private object Baseline {
    const val PATH = "ai/src/test/resources/ai-reading-baseline.tsv"
    private const val RESOURCE = "/ai-reading-baseline.tsv"

    private val HEADER = """
        |# AI card-reading baseline, read by AiReadingRatchetTest. Rewrite it with `just ai-reading-baseline`;
        |# never edit it by hand. One line per card in the corpus at the last re-bless, sorted by name: the
        |# name alone when the AI's readers read the card fully, else name<TAB>flag; flag; … (the flags of
        |# ai/build/reports/ai-reading-audit/cards.tsv). No totals here on purpose: a totals line would be
        |# a merge conflict between every two re-blesses. See docs/ai/reading-audit.md, "The ratchet".
        |""".trimMargin()

    fun render(cards: Map<String, List<String>>): String = buildString {
        appendLine(HEADER)
        for ((name, keys) in cards) {
            append(name)
            if (keys.isNotEmpty()) append('\t').append(keys.sorted().joinToString("; "))
            appendLine()
        }
    }

    fun load(): Map<String, Set<String>> {
        val text = Baseline::class.java.getResource(RESOURCE)?.readText()
            ?: File("src/test/resources$RESOURCE").takeIf { it.exists() }?.readText()
            ?: error("$PATH is missing. Create it with `just ai-reading-baseline`.")
        return text.lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .associate { line ->
                val name = line.substringBefore('\t')
                val keys = if ('\t' in line) line.substringAfter('\t').split("; ").filter { it.isNotBlank() }.toSet() else emptySet()
                name to keys
            }
    }
}

private class RatchetDiff(
    /** Baselined card → the flags it gained. */
    val regressions: Map<String, Set<String>>,
    /** New group key → the cards carrying it. */
    val newGroups: Map<String, List<String>>,
    /** Cards the baseline doesn't list, flagged only in known groups — tolerated corpus growth. */
    val unbaselinedUnread: Int,
    val unbaselinedRead: Int,
    /** Baselined cards that lost at least one flag. */
    val improved: Int,
    /** Baselined cards no longer in the corpus. */
    val gone: Int,
) {
    val failed: Boolean get() = regressions.isNotEmpty() || newGroups.isNotEmpty()

    fun summary(current: Map<String, List<String>>): String {
        val read = current.values.count { it.isEmpty() }
        val pct = String.format(Locale.ROOT, "%.1f", if (current.isEmpty()) 100.0 else read * 100.0 / current.size)
        return "AI reading ratchet: $read / ${current.size} cards read fully ($pct%). " +
            "Since the baseline: ${regressions.size} card(s) gained flags, ${newGroups.size} new group(s), " +
            "$improved card(s) improved, $gone gone, $unbaselinedRead new card(s) read fully, " +
            "$unbaselinedUnread new card(s) in known groups." +
            if (!failed && (improved > 0 || gone > 0)) "\nCoverage rose: run `just ai-reading-baseline` to lock it in." else ""
    }

    fun failureMessage(): String = buildString {
        appendLine("The AI reads less of the corpus than ${Baseline.PATH} records.")
        if (regressions.isNotEmpty()) {
            appendLine()
            appendLine("${regressions.size} card(s) the baseline knows gained a flag — a reader lost a case, or the card")
            appendLine("now uses an effect type the AI doesn't read. Per group, the unread count these cards add:")
            regressions.values.flatten().groupingBy { it }.eachCount().entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .forEach { (key, n) -> appendLine("  +%-5d %s".format(n, key)) }
            appendLine("Cards (first 30):")
            regressions.entries.take(30).forEach { (name, keys) -> appendLine("  $name: ${keys.sorted().joinToString("; ")}") }
        }
        if (newGroups.isNotEmpty()) {
            appendLine()
            appendLine("${newGroups.size} new group(s) — an effect type no baselined card is flagged for:")
            newGroups.forEach { (key, cards) -> appendLine("  $key: ${cards.take(5).joinToString(", ")}${if (cards.size > 5) ", …" else ""}") }
        }
        appendLine()
        appendLine("If a reader change caused this, fix the reader. If it is expected — a card that uses an effect")
        appendLine("type the AI can't read yet — record it: run `just ai-reading-baseline` and commit")
        appendLine("${Baseline.PATH}. Its diff shows reviewers exactly which cards moved.")
        appendLine("The rules: docs/ai/reading-audit.md, \"The ratchet\".")
    }

    companion object {
        fun of(baseline: Map<String, Set<String>>, current: Map<String, List<String>>): RatchetDiff {
            val knownGroups = baseline.values.flatten().toSet()
            val regressions = sortedMapOf<String, Set<String>>()
            val newGroups = sortedMapOf<String, MutableList<String>>()
            var unbaselinedUnread = 0
            var unbaselinedRead = 0
            var improved = 0
            for ((name, keys) in current) {
                keys.filter { it !in knownGroups }.forEach { newGroups.getOrPut(it) { mutableListOf() } += name }
                val before = baseline[name]
                if (before == null) {
                    if (keys.isEmpty()) unbaselinedRead++ else unbaselinedUnread++
                    continue
                }
                val gained = keys.toSet() - before
                if (gained.isNotEmpty()) regressions[name] = gained
                if ((before - keys.toSet()).isNotEmpty()) improved++
            }
            return RatchetDiff(
                regressions = regressions,
                newGroups = newGroups,
                unbaselinedUnread = unbaselinedUnread,
                unbaselinedRead = unbaselinedRead,
                improved = improved,
                gone = (baseline.keys - current.keys).size,
            )
        }
    }
}
