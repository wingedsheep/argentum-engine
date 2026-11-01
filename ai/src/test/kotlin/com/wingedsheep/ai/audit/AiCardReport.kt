package com.wingedsheep.ai.audit

import com.wingedsheep.ai.arena.harnessRegistry
import com.wingedsheep.ai.engine.LimitedCardRater
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import java.util.Locale

/**
 * `just ai-card "<name>[|<name>…]"` — the engine AI's coverage of a few cards, in the terminal: what
 * its readers understand ([AiReadingAudit.read]), how it rates them ([AiCardRating]) and which synergy
 * axes it reads them feeding or rewarding ([SynergyAxes]). The rating and reading are the same
 * answers the Assay explorer's "AI reading" and "How the AI rates it" panels draw, for an agent or a
 * contributor without a browser — the `add-card` and `add-feature` skills' AI-coverage step.
 *
 * Skipped in the normal `:ai:test` suite; enabled by `-DaiCard=<names>`. Writes the report to
 * `-DaiCardOut` (the recipe prints it) as well as stdout.
 */
class AiCardReport : FunSpec({

    val names = System.getProperty("aiCard").orEmpty().split('|').map { it.trim() }.filter { it.isNotEmpty() }

    test("AI coverage of named cards").config(enabled = names.isNotEmpty()) {
        val registry = harnessRegistry(MtgSetCatalog.all)
        val out = StringBuilder()
        for (name in names) {
            val card = registry.getCard(name) ?: registry.getCard(name.substringBefore(" // ").trim())
            if (card == null) {
                out.appendLine("\n✕ $name — not implemented (no card by that name in the registry)")
                continue
            }
            val sets = MtgSetCatalog.all
                .filter { s -> s.cards.any { it.name == card.name } || s.printings.any { it.name == card.name } }
                .sortedBy { it.releaseDate ?: "9999" }.map { it.code }
            out.append(report(card, AiReadingAudit.read(card) { registry.getCard(it) }, AiCardRating.of(card, sets)))
        }
        val file = java.io.File(System.getProperty("aiCardOut") ?: "build/reports/ai-card.txt")
        file.parentFile?.mkdirs()
        file.writeText(out.toString().trimStart() + "\n")
        println(out)
    }
})

private fun f(x: Double) = String.format(Locale.ROOT, "%.1f", x)
private fun signed(x: Double) = (if (x >= 0) "+" else "−") + f(kotlin.math.abs(x))

private fun report(card: CardDefinition, reading: AiReadingAudit.CardReading, rating: AiCardRating.Rating): String = buildString {
    val name = card.name
    appendLine()
    appendLine("━━ $name ━━")

    val checked = reading.abilities.filter { it.checked }
    val read = checked.count { it.readFully }
    appendLine(
        if (checked.isEmpty()) "Reading   nothing for the readers to check"
        else "Reading   ${if (read == checked.size) "✓" else "✕"} $read/${checked.size} abilities read fully",
    )
    for (a in checked.filterNot { it.readFully }) {
        for (finding in a.findings.filterNot { it.read }) {
            appendLine("          ✕ ${a.ability.title}: ${finding.reason}${finding.effectType?.let { " [$it]" } ?: ""}")
        }
    }

    val e = rating.limited.explanation
    val source = when (e.source) {
        LimitedCardRater.Source.BASIC_LAND -> "basic land"
        LimitedCardRater.Source.LAND -> "flat non-basic land rating"
        LimitedCardRater.Source.WIN_RATE -> "17Lands win rate ${f(e.winRate!! * 100)}% over ${e.games} games"
        LimitedCardRater.Source.TABLE -> "${e.tableSet} pick table"
        LimitedCardRater.Source.HEURISTIC -> "effect heuristic"
    }
    appendLine("Pick      ${f(e.rating)} / 5 — $source")
    if (e.source == LimitedCardRater.Source.HEURISTIC) {
        appendLine("          ! no draft rating anywhere — the AI guesses from the effect heuristic:")
        appendLine("            " + e.heuristic.joinToString("  ") { "${it.part} ${signed(it.value)}" })
    }
    // Each set whose own table rates it gets a line; the sets that fall back share one.
    for (s in rating.limited.sets.filter { it.fromTable || it.rating == null }) {
        val how = if (s.rating == null) "land handling" else "own table"
        appendLine("          ${s.set.padEnd(5)} ${s.rating?.let(::f) ?: "—"}  $how${if (s.removal) ", removal" else ""}")
    }
    val fallback = rating.limited.sets.filter { !it.fromTable && it.rating != null }
    if (fallback.isNotEmpty()) {
        appendLine("          ${f(fallback.first().rating!!)}  not in its own table → $source: " + fallback.joinToString(" ") { it.set })
    }

    val b = rating.board
    appendLine(
        "In play   " + (b.value?.let { "${f(it)} board points  (" + b.parts.joinToString("  ") { p -> "${p.label} ${signed(p.value)}" } + ")" }
            ?: b.rule.orEmpty()),
    )
    val creature = b.kind == AiCardRating.BoardKind.CREATURE || b.kind == AiCardRating.BoardKind.VARIABLE_CREATURE
    val parts = buildList {
        if (creature) add("creature body")
        addAll(rating.intent.tags.map { it.label })
        if (rating.intent.tags.isEmpty() && reading.abilities.isNotEmpty()) add("! abilities not recognised")
        if (isEmpty()) add("nothing to read")
        if (rating.intent.speed != "Sorcery speed") add(rating.intent.speed.lowercase())
    }
    appendLine("Intent    ${parts.joinToString(", ")}")
    rating.intent.facts.forEach { appendLine("          · $it") }
    val synergy = SynergyAxes.of(card)
    appendLine(
        "Synergy   " + if (synergy.isEmpty) "none read"
        else listOfNotNull(
            synergy.feeds.takeIf { it.isNotEmpty() }?.let { "feeds " + it.joinToString(", ", transform = ::axisLabel) },
            synergy.rewards.takeIf { it.isNotEmpty() }?.let { "rewards " + it.joinToString(", ", transform = ::axisLabel) },
            synergy.tribesRewarded.takeIf { it.isNotEmpty() }?.let { "asks for " + it.joinToString(", ") { t -> t.value } },
            synergy.asks.takeIf { it.isNotEmpty() }
                ?.let { "asks for " + it.joinToString(", ") { ask -> "${ask.filter.description} (${ask.wanted.toInt()})" } },
            "payoff strength ${"%.2f".format(java.util.Locale.ROOT, synergy.strength)}×".takeIf { synergy.isPayoff },
            synergy.punishes.takeIf { it.isNotEmpty() }?.let { "shrinks " + it.keys.joinToString(", ") { t -> t.value } },
            "stands alone as an answer".takeIf { synergy.standsAlone && synergy.isPayoff },
        ).joinToString(" · "),
    )
    rating.advisor?.let { appendLine("Advisor   $it") }
}

private fun axisLabel(axis: SynergyAxis): String = axis.name.lowercase().replace('_', ' ')
