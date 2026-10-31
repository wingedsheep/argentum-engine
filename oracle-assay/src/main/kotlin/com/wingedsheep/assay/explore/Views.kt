package com.wingedsheep.assay.explore

import com.wingedsheep.assay.corpus.OracleCard
import com.wingedsheep.assay.corpus.OracleFace
import com.wingedsheep.assay.corpus.SetCards
import com.wingedsheep.assay.gate.CardComparison
import com.wingedsheep.assay.gate.CardResult
import com.wingedsheep.assay.gate.DeclineKey
import com.wingedsheep.assay.gate.Differential
import com.wingedsheep.assay.gate.DifferentialReport
import com.wingedsheep.assay.gate.FinenessReport
import com.wingedsheep.assay.gate.LineResult
import com.wingedsheep.assay.gate.LineVerdict
import com.wingedsheep.assay.gate.Population
import com.wingedsheep.assay.gate.PrefixProbe
import com.wingedsheep.assay.gate.Touchstone
import com.wingedsheep.assay.grammar.Grammar
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.RuleShape
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.serialization.CardSerialization
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale

/**
 * The explorer's payloads: Assay's own result types, rendered as JSON for the page.
 *
 * A pure view layer, and pointedly a thin one. Every number here comes from [FinenessReport],
 * [Touchstone] or [Differential] rather than being recomputed — the explorer showing a different
 * fineness than `just assay-gate` prints would make both untrustworthy, and the cheapest way to
 * guarantee it cannot is for the browser to be looking at the gate's own objects.
 */
internal object Views {

    // -------------------------------------------------------------------------------------------
    // Overview
    // -------------------------------------------------------------------------------------------

    fun overview(index: AssayIndex, goldens: GoldenIndex): JsonObject = buildJsonObject {
        val report = index.report
        put("corpusFile", index.corpusFile)
        put("sweepMillis", index.sweepMillis)
        put("goldens", goldens.size)
        put("clean", report.clean)
        put("cards", report.cards)
        put("faces", report.faces)
        put("vanillaFaces", report.vanillaFaces)
        put("lineInstances", report.lineInstances)
        put("uniqueLines", report.uniqueLines)
        put("fineness", report.fineness)
        put("uniqueFineness", report.uniqueFineness)
        put("inScopeFineness", report.inScopeFineness)
        put("inScopeCards", report.inScopeCards)
        put("inScopeCovered", report.inScopeCovered)
        put("cardsCovered", report.cardsCovered)
        put("cardsRoundTripped", report.cardsRoundTripped)
        put("redundantReadingLines", report.redundantReadingLines)
        put("declineFamilies", index.families(DeclineKey.TOKEN).size)
        put("shapeFamilies", index.families(DeclineKey.SHAPE).size)
        put("tailFamilies", index.families(DeclineKey.TAIL).size)
        put("states", counts(index.stateCounts))
        put("verdicts", counts(LineVerdict.entries.associate { it.name to report.instancesByVerdict[it].orZero() }))
        put("uniqueVerdicts", counts(LineVerdict.entries.associate { it.name to report.uniqueByVerdict[it].orZero() }))
        put("glosses", counts(report.glossCounts.entries.associate { (verdict, count) -> verdict.name to count }))
        put("normalizationFailures", strings(report.normalizationFailures))
        put("restoreFailures", strings(report.restoreFailures))
        put("ambiguities", strings(report.ambiguities))
        put("mismatches", strings(report.mismatches))
        put(
            "glossDifferences",
            buildJsonArray {
                report.glossDifferences.forEach {
                    add(
                        buildJsonObject {
                            put("card", it.cardName)
                            put("keyword", it.keyword)
                            put("printed", it.printed)
                            put("regenerated", it.regenerated ?: "")
                        }
                    )
                }
            },
        )
    }

    // -------------------------------------------------------------------------------------------
    // Declines — the ranked SDK / grammar gap report
    // -------------------------------------------------------------------------------------------

    fun declines(index: AssayIndex, ranking: DeclineKey, query: String?, limit: Int): JsonObject {
        val needle = query?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val all = index.families(ranking)
        val matching = all.filter {
            needle.isEmpty() ||
                it.key.lowercase(Locale.ROOT).contains(needle) ||
                it.examples.any { line -> line.lowercase(Locale.ROOT).contains(needle) }
        }
        return buildJsonObject {
            put("ranking", ranking.name)
            put("total", all.size)
            put("familyCounts", counts(DeclineKey.entries.associate { it.name to index.families(it).size }))
            put("matching", matching.size)
            put("declinedLines", index.report.instancesByVerdict[LineVerdict.DECLINED].orZero())
            put("goldensAvailable", index.goldenNames.isNotEmpty())
            put("cardsCovered", index.report.cardsCovered)
            put("corpusCards", index.report.cards)
            // Over the *unfiltered* ranking: it answers "what does working the top of this list
            // buy", and a text filter does not change the list you would work. Absent for the token
            // ranking, deliberately — see DeclineKey.namesWork.
            put("unlockCurve", JsonArray(index.unlockCurves[ranking].orEmpty().map(::JsonPrimitive)))
            put("hasUnlocks", ranking.namesWork)
            put(
                "families",
                buildJsonArray {
                    matching.take(limit).forEach { family ->
                        add(
                            buildJsonObject {
                                put("key", family.key)
                                put("cards", family.cards)
                                put("unlocks", family.soleBlocked ?: 0)
                                put("lines", family.lines)
                                put("implemented", family.implemented)
                                put("examples", strings(family.examples))
                            }
                        )
                    }
                },
            )
        }
    }

    /**
     * One family's page. [DeclineFamily.cardNames] is uncapped in the index because the probe
     * measures over all of it; the cap is here, where it is a statement about what a page can show.
     */
    fun decline(index: AssayIndex, ranking: DeclineKey, key: String): JsonObject? {
        val family = index.decline(key, ranking) ?: return null
        return buildJsonObject {
            put("ranking", ranking.name)
            put("hasUnlocks", ranking.namesWork)
            put("key", family.key)
            put("cards", family.cards)
            put("unlocks", family.soleBlocked ?: 0)
            put("lines", family.lines)
            put("implemented", family.implemented)
            put("examples", strings(family.examples))
            put("probeFind", PrefixProbe.suggestedSpan(ranking, family.examples.firstOrNull().orEmpty(), family.key))
            put(
                "blocked",
                buildJsonArray {
                    family.cardNames.take(MAX_SHOWN_CARDS).forEach { name ->
                        add(
                            buildJsonObject {
                                put("name", name)
                                put("golden", index.hasGolden(name))
                                put("set", index.card(name)?.setCode ?: "")
                            }
                        )
                    }
                },
            )
        }
    }

    /**
     * The feasibility probe over one family, run on the request thread against the live grammar.
     *
     * Re-assaying every blocked card is the cost, and it is the right one: the alternative is
     * keeping every declined line of every card in the index so this could be answered from a
     * snapshot, which would be both larger and — the moment a rule is edited — wrong. At sweep rates
     * this is a fraction of a second for the largest family in the corpus.
     */
    fun probe(
        index: AssayIndex,
        touchstone: Touchstone,
        ranking: DeclineKey,
        key: String,
        find: String,
        replace: String,
        regex: Boolean,
    ): JsonObject {
        val family = index.decline(key, ranking)
            ?: return buildJsonObject { put("error", "no decline family \"$key\"") }
        val cards = family.cardNames.mapNotNull(index::card)
        val started = System.currentTimeMillis()
        val result = PrefixProbe.run(
            touchstone = touchstone,
            cards = cards,
            family = ranking,
            key = key,
            substitution = PrefixProbe.Substitution(find = find, replace = replace, regex = regex),
        )
        return buildJsonObject {
            result.error?.let { put("error", it) }
            put("find", find)
            put("replace", replace)
            put("regex", regex)
            put("familyLines", result.familyLines)
            put("familyLinesParsing", result.familyLinesParsing)
            put("unmatched", result.unmatched)
            put("cardsConsidered", result.cardsConsidered)
            put("cardsFinished", result.cardsFinished)
            put("soleBlocked", family.soleBlocked ?: 0)
            put("millis", System.currentTimeMillis() - started)
            put(
                "examples",
                buildJsonArray {
                    result.examples.forEach {
                        add(
                            buildJsonObject {
                                put("card", it.card)
                                put("before", it.before)
                                put("after", it.after)
                                put("parses", it.parses)
                            }
                        )
                    }
                },
            )
        }
    }

    // -------------------------------------------------------------------------------------------
    // Cards — the browsable sweep
    // -------------------------------------------------------------------------------------------

    fun cards(index: AssayIndex, filter: CardFilter, offset: Int, limit: Int, ai: AiColumn = AiColumn.NONE): JsonObject {
        val matching = index.rows.asSequence().filter(filter::accepts)
        val page = matching.drop(offset).take(limit).toList()
        return buildJsonObject {
            // Counted separately from the page so a filter that matches nothing says so, rather
            // than looking like the end of a list.
            put("total", index.rows.count(filter::accepts))
            put("offset", offset)
            // The set filter's own denominator: how many cards were *printed* in the set, against
            // however many of them the corpus reached. Without it a set with an unimplemented half
            // reads as a set the sweep lost cards from.
            put("setPrinted", filter.setCards?.size ?: 0)
            put("setUnresolved", filter.setUnresolved)
            // Whether the AI column means anything here: no provider (standalone), or a provider
            // still indexing the corpus, are both said rather than shown as an empty column.
            put("aiAvailable", ai.available)
            put("aiReady", ai.ready)
            put(
                "rows",
                buildJsonArray {
                    page.forEach { row ->
                        add(
                            buildJsonObject {
                                put("name", row.name)
                                put("set", row.setCode ?: "")
                                put("layout", row.layout)
                                put("lines", row.lines)
                                put("state", state(row))
                                put("inScope", row.inScope)
                                put("golden", index.hasGolden(row.name))
                                put("declines", strings(row.declineKeys(DeclineKey.TOKEN)))
                                if (ai.available) filter.aiReadsFully(row.name)?.let { put("aiReadsFully", it) }
                            }
                        )
                    }
                },
            )
        }
    }

    private fun state(row: CardRow) = AssayIndex.state(row)

    fun search(index: AssayIndex, query: String): JsonObject = buildJsonObject {
        put(
            "cards",
            buildJsonArray {
                index.search(query).forEach { card ->
                    add(
                        buildJsonObject {
                            put("name", card.name)
                            put("set", card.setCode ?: "")
                            put("state", index.row(card.name)?.let(::state) ?: "")
                            put("golden", index.hasGolden(card.name))
                        }
                    )
                }
            },
        )
        put(
            "declines",
            buildJsonArray {
                val needle = query.trim().lowercase(Locale.ROOT)
                index.families(DeclineKey.TOKEN).asSequence()
                    .filter { it.key.lowercase(Locale.ROOT).contains(needle) }
                    .take(6)
                    .forEach {
                        add(
                            buildJsonObject {
                                put("key", it.key)
                                put("cards", it.cards)
                            }
                        )
                    }
            },
        )
    }

    // -------------------------------------------------------------------------------------------
    // One card, assayed — the page the whole thing exists for
    // -------------------------------------------------------------------------------------------

    fun cardPage(index: AssayIndex, goldens: GoldenIndex, touchstone: Touchstone, name: String): JsonObject? {
        val card = index.card(name) ?: return null
        return assayed(touchstone.assay(card), card, index, goldens)
    }

    /**
     * The same page for text that was never printed.
     *
     * A custom card runs through the identical path — a synthetic [OracleCard] straight into
     * [Touchstone.assay] — rather than through a shortcut that parses lines directly, because the
     * interesting half is what happens *before* the grammar: normalization, self-reference
     * abstraction, reminder stripping and the invertibility check. A live parser that skipped those
     * would answer a different question than the gate does.
     */
    fun parsed(touchstone: Touchstone, request: ParseRequest): JsonObject {
        val face = OracleFace(
            name = request.name.ifBlank { "Custom Card" },
            oracleText = request.oracleText,
            typeLine = request.typeLine,
            manaCost = request.manaCost,
        )
        val card = OracleCard(
            name = face.name,
            oracleId = null,
            layout = "normal",
            setCode = null,
            scryfallKeywords = emptyList(),
            faces = listOf(face),
        )
        return assayed(touchstone.assay(card), card, index = null, goldens = null)
    }

    private fun assayed(
        result: CardResult,
        card: OracleCard,
        index: AssayIndex?,
        goldens: GoldenIndex?,
    ): JsonObject = buildJsonObject {
        put("name", card.name)
        put("set", card.setCode ?: "")
        put("layout", card.layout)
        put("scryfallKeywords", strings(card.scryfallKeywords))
        put("inScope", result.inPhase1Scope)
        put("roundTrips", result.roundTrips)
        put("covered", result.covered)
        put(
            "faces",
            buildJsonArray {
                result.faces.forEachIndexed { faceIndex, face ->
                    val source = card.faces[faceIndex]
                    add(
                        buildJsonObject {
                            put("name", face.faceName)
                            put("typeLine", source.typeLine)
                            put("manaCost", source.manaCost)
                            put("oracleText", source.oracleText)
                            put("vanilla", face.normalized.isVanilla)
                            put("normalizationHolds", face.normalizationHolds)
                            face.restoreHolds?.let { put("restoreHolds", it) }
                            put("lines", buildJsonArray { face.lines.forEach { add(line(it)) } })
                            put(
                                "glosses",
                                buildJsonArray {
                                    face.glosses.forEach {
                                        add(
                                            buildJsonObject {
                                                put("keyword", it.keyword)
                                                put("verdict", it.verdict.name)
                                                put("printed", it.printed)
                                                put("regenerated", it.regenerated ?: "")
                                            }
                                        )
                                    }
                                },
                            )
                        }
                    )
                }
            },
        )
        if (index != null && goldens != null) put("golden", golden(index, goldens, card))
    }

    private fun line(line: LineResult): JsonObject = buildJsonObject {
        put("index", line.index)
        put("text", line.line)
        put("verdict", line.verdict.name)
        put("printed", line.printed ?: "")
        put("redundantReadings", line.redundantReadings)
        line.model?.let { fragment ->
            put(
                "keywords",
                buildJsonArray {
                    fragment.keywordAbilities.forEach {
                        add(
                            buildJsonObject {
                                // Structural first, prose second: where the SDK spells one concept
                                // two ways both sides *describe* themselves identically, and only
                                // the data class shows the difference. That is how the flanking
                                // finding presented.
                                put("structure", it.toString())
                                put("description", it.description)
                            }
                        )
                    }
                },
            )
            if (fragment.script != CardScript.EMPTY) put("script", script(fragment.script))
        }
        line.decline?.let { decline ->
            put(
                "decline",
                buildJsonObject {
                    put("position", decline.position)
                    put("reason", decline.reason.name)
                    put("expected", strings(decline.expected))
                },
            )
        }
    }

    // -------------------------------------------------------------------------------------------
    // The differential, for one card and for the whole corpus
    // -------------------------------------------------------------------------------------------

    /**
     * What the differential gate says about *this* card — which is the question a card page raises
     * and the CLI can only answer by re-running the whole corpus.
     *
     * The population bucket is reported whether or not a comparison happened, because that is the
     * gate's own discipline: a card that was not compared has a *named reason*, and hiding the
     * reason is exactly how a fail-closed scope quietly becomes fail-open.
     */
    private fun golden(index: AssayIndex, goldens: GoldenIndex, card: OracleCard): JsonElement {
        val implemented = goldens.card(card.name) ?: return JsonObject(mapOf("present" to JsonPrimitive(false)))
        val comparison = Differential().compare(implemented, index.oracleJoin)
        return buildJsonObject {
            put("present", true)
            put("set", implemented.setCode)
            put("population", comparison.population.name)
            put("populationNote", populationNote(comparison.population))
            comparison.verdict?.let { put("verdict", it.name) }
            put("json", goldens.json(card.name) ?: "")
            if (comparison.onlyInText.isNotEmpty()) {
                put("onlyInText", strings(comparison.onlyInText.map { it.toString() }))
            }
            if (comparison.onlyInCard.isNotEmpty()) {
                put("onlyInCard", strings(comparison.onlyInCard.map { it.toString() }))
            }
            comparison.textScript?.let { put("textScript", script(it)) }
            comparison.cardScript?.let { put("cardScript", script(it)) }
        }
    }

    fun differential(report: DifferentialReport): JsonObject = buildJsonObject {
        put("cards", report.cards)
        put("confirmed", report.confirmed)
        put("divergent", report.divergent)
        put("agreement", report.agreement)
        put("clean", report.clean)
        put("undecodable", strings(report.undecodable))
        put(
            "populations",
            buildJsonArray {
                Population.entries.forEach { population ->
                    add(
                        buildJsonObject {
                            put("name", population.name)
                            put("count", report.byPopulation[population].orZero())
                            put("note", populationNote(population))
                        }
                    )
                }
            },
        )
        put("divergences", buildJsonArray { report.divergences.forEach { add(divergence(it)) } })
    }

    private fun divergence(comparison: CardComparison): JsonObject = buildJsonObject {
        put("name", comparison.implemented.name)
        put("set", comparison.implemented.setCode)
        put("onlyInText", strings(comparison.onlyInText.map { it.toString() }))
        put("onlyInCard", strings(comparison.onlyInCard.map { it.toString() }))
        comparison.textScript?.let { put("textScript", script(it)) }
        comparison.cardScript?.let { put("cardScript", script(it)) }
    }

    private fun populationNote(population: Population) = when (population) {
        Population.COMPARED -> "Assay read the whole card and the golden decoded."
        Population.NOT_COVERED -> "Assay does not yet read every line. Not a bug — the grammar has not reached it."
        Population.MULTI_FACE -> "Multi-face: the model splits across faces, which the comparison does not mirror yet."
        Population.SCRIPT_NOT_MODELLED ->
            "The card puts content in a CardScript slot the grammar cannot produce — typically a keyword " +
                "the SDK lowers to a triggered ability at authoring time."
        Population.LINES_DO_NOT_FOLD ->
            "Two lines are both spell effects and a CardScript has one. The card prints a sequence with no rule."
        Population.NO_ORACLE_TEXT -> "No Scryfall Oracle entry joined — a custom card, or a name the index lacks."
        Population.ORACLE_TEXT_DIFFERS ->
            "The golden was authored from different wording than Scryfall now serves. Comparing would diff two cards."
        Population.UNDECODABLE -> "The golden JSON would not decode. Always a bug."
    }

    // -------------------------------------------------------------------------------------------
    // The grammar, as wired
    // -------------------------------------------------------------------------------------------

    /**
     * Every rule reachable from [Grammar.abilityLine], flattened with its structure and its usage.
     *
     * Walked from the root rather than assembled from each family's published list, so what the page
     * shows is the grammar that *runs*. A rule sitting in a file but wired into nothing cannot
     * appear here, which is the honest behaviour — and the reverse of what a hand-maintained index
     * would do.
     */
    fun grammar(index: AssayIndex?): JsonObject {
        val nodes = LinkedHashMap<Int, Phrase<*>>()
        val group = HashMap<Int, String>()

        fun walk(phrase: Phrase<*>, groupName: String) {
            if (nodes.put(phrase.id, phrase) != null) return
            group[phrase.id] = groupName
            val shape = phrase.shape
            val childGroup = if (shape is RuleShape.Choice) phrase.name else groupName
            shape.children.forEach { walk(it, childGroup) }
        }
        walk(Grammar.abilityLine, Grammar.abilityLine.name)

        return buildJsonObject {
            put("root", Grammar.abilityLine.id)
            put("rules", buildJsonArray { nodes.values.forEach { add(rule(it, group, index)) } })
        }
    }

    private fun rule(phrase: Phrase<*>, group: Map<Int, String>, index: AssayIndex?): JsonObject = buildJsonObject {
        put("id", phrase.id)
        put("name", phrase.name)
        put("canonical", phrase.canonical)
        put("group", group[phrase.id] ?: "")
        when (val shape = phrase.shape) {
            is RuleShape.Template -> {
                put("kind", "template")
                put("template", shape.template)
                put(
                    "slots",
                    buildJsonArray {
                        shape.slots.forEach { (slotName, slot) ->
                            add(
                                buildJsonObject {
                                    put("name", slotName)
                                    put("rule", slot.id)
                                    put("ruleName", slot.name)
                                }
                            )
                        }
                    },
                )
            }

            is RuleShape.Choice -> {
                put("kind", "choice")
                put("children", buildJsonArray { shape.alternatives.forEach { add(JsonPrimitive(it.id)) } })
            }

            is RuleShape.Run -> {
                put("kind", "run")
                put("separator", shape.separator)
                put("min", shape.min)
                put("children", buildJsonArray { add(JsonPrimitive(shape.item.id)) })
            }

            is RuleShape.Alternate -> {
                put("kind", "alternate")
                put("children", buildJsonArray { add(JsonPrimitive(shape.inner.id)) })
            }

            is RuleShape.Leaf -> {
                put("kind", "leaf")
                put("pattern", shape.pattern)
            }

            RuleShape.Opaque -> put("kind", "opaque")
        }
        index?.ruleUsage?.get(phrase.id)?.let {
            put("usageLines", it.lines)
            put("usageCards", it.cards)
        }
    }

    // -------------------------------------------------------------------------------------------

    private fun script(script: CardScript) =
        JsonPrimitive(CardSerialization.json.encodeToString(CardScript.serializer(), script))

    /** A page does not need 900 card names; the family's own count above says when it is showing fewer. */
    private const val MAX_SHOWN_CARDS = 400

    private fun strings(values: List<String>) = JsonArray(values.map(::JsonPrimitive))

    private fun counts(values: Map<String, Int>) = JsonObject(values.mapValues { (_, v) -> JsonPrimitive(v) })

    private fun Int?.orZero() = this ?: 0
}

/** Whether the card table can show the AI column: a provider exists, and its corpus index is done. */
internal data class AiColumn(val available: Boolean, val ready: Boolean) {
    companion object {
        val NONE = AiColumn(available = false, ready = false)
    }
}

/** The fields the live-parse page posts. A card that was never printed, in Scryfall's own shape. */
internal data class ParseRequest(
    val name: String,
    val manaCost: String,
    val typeLine: String,
    val oracleText: String,
)

/** The card table's filters, as the query string carries them. */
internal data class CardFilter(
    val state: String?,
    /** What the user typed in the set box, kept only so the page can say which set it resolved. */
    val set: String?,
    /**
     * The cards printed in [set], resolved by [com.wingedsheep.assay.corpus.SetMembership] — `null`
     * when the code is unknown, which matches **nothing**. Filtering on [CardRow.setCode] instead
     * would silently show a quarter of Portal and call it the set.
     */
    val setCards: SetCards?,
    val query: String?,
    val scopeOnly: Boolean,
    val goldenOnly: Boolean,
    private val goldens: Set<String>,
    /** Only implemented cards the engine AI does not read fully. */
    val aiGaps: Boolean = false,
    /** The [AiReadingProvider.readsFully] answer for a card name — null when unknown. */
    val aiReadsFully: (String) -> Boolean? = { null },
) {
    /** True when a set was asked for and could not be resolved — the page says so rather than lying. */
    val setUnresolved: Boolean get() = set != null && setCards == null

    fun accepts(row: CardRow): Boolean {
        if (scopeOnly && !row.inScope) return false
        if (set != null && setCards?.contains(row.oracleId, row.name) != true) return false
        if (goldenOnly && row.name !in goldens && row.name.substringBefore(" // ") !in goldens) return false
        if (aiGaps && aiReadsFully(row.name) != false) return false
        if (query != null && !row.name.lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT))) return false
        return when (state) {
            null, "" -> true
            "vanilla" -> row.vanilla
            "round-trip" -> !row.vanilla && row.roundTrips
            "variant" -> !row.vanilla && !row.roundTrips && row.covered
            "declined" -> !row.covered
            else -> true
        }
    }
}
