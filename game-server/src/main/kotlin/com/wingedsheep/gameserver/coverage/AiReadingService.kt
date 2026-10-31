package com.wingedsheep.gameserver.coverage

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.engine.LimitedCardRater
import com.wingedsheep.ai.engine.knowledge.CardLookup
import com.wingedsheep.assay.explore.AiAbilityView
import com.wingedsheep.assay.explore.AiBoardView
import com.wingedsheep.assay.explore.AiIntentView
import com.wingedsheep.assay.explore.AiPartView
import com.wingedsheep.assay.explore.AiPickView
import com.wingedsheep.assay.explore.AiRatingView
import com.wingedsheep.assay.explore.AiSetPickView
import com.wingedsheep.assay.explore.AiTagView
import com.wingedsheep.assay.explore.AiFindingView
import com.wingedsheep.assay.explore.AiReadingProvider
import com.wingedsheep.assay.explore.AiReadingView
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What the engine AI's static card readers understand of each card — the AI card-reading audit
 * (`docs/ai/reading-audit.md`) run inside the server, for two developer views:
 *
 * - the Argentum Assay explorer's **AI reading** panel, through [AiReadingProvider] — `:oracle-assay`
 *   cannot depend on `:ai`, so this server, which has both, supplies the readings when it mounts the
 *   explorer; and
 * - the Set Completion view's per-set **AI-reading score** ([setSummary]) and a set's unread cards
 *   grouped by the effect type the AI misses ([setDetail]).
 *
 * ## Cost
 *
 * One card is about a millisecond, so the card page reads on request. The *corpus* — every
 * implemented card, for the per-set share and the explorer's "AI gaps" filter — is several seconds,
 * so it is indexed once, on a minimum-priority daemon thread started by the first request that wants
 * it, never at boot: a server nobody opens those views on pays nothing, and the view that asked
 * renders at once and fills the number in when [setSummary] says it is ready. The definitions are
 * immutable for the life of the process, so the index never goes stale — which is why it is
 * computed rather than baked: a baked file would go stale with every card PR and churn on every
 * re-bless, while the whole corpus takes about a second to read.
 */
@Service
class AiReadingService(private val registry: CardRegistry) : AiReadingProvider {

    /** Token definitions (Role tokens) for the polarity reader, as the AI looks them up in play. */
    private val lookup: CardLookup = { name -> registry.getCard(name) }

    /**
     * One indexed card: its audit flag keys (empty when read fully) and how many of its checked
     * abilities the readers understood — the ability-weighted measure beside the per-card one.
     */
    private data class Entry(val name: String, val flags: List<String>, val abilitiesRead: Int, val abilities: Int) {
        val readFully: Boolean get() = flags.isEmpty()
    }

    /** Card name (and front-face name) → its reading. Null until indexed. */
    @Volatile private var index: Map<String, Entry>? = null
    private val started = AtomicBoolean(false)

    // ── AiReadingProvider ───────────────────────────────────────────────────

    override fun implemented(name: String): CardDefinition? = registry.getCard(name)

    override fun read(card: CardDefinition): AiReadingView = view(AiReadingAudit.read(card, lookup))

    override fun readsFully(name: String): Boolean? {
        val current = index ?: run { warm(); return null }
        return current.lookup(name)?.readFully
    }

    override fun corpusReady(): Boolean = (index != null).also { if (!it) warm() }

    /** How the AI rates [card], with one draft row per catalogued set that authors or reprints it. */
    override fun rate(card: CardDefinition): AiRatingView {
        val names = setOf(card.name, frontFace(card.name))
        val sets = MtgSetCatalog.all
            .filter { set -> set.cards.any { it.name in names } || set.printings.any { it.name in names } }
            .sortedBy { it.releaseDate ?: "9999" }
            .map { it.code }
        return ratingView(AiCardRating.of(card, sets))
    }

    // ── Set Completion ──────────────────────────────────────────────────────

    /**
     * One set's AI-reading score. The headline is [readFully] of [total]: of the cards the set
     * authors, how many the AI's readers understand completely. [abilitiesRead] of [abilities] is
     * the secondary, ability-weighted measure — a set of mostly-read cards with one gap each scores
     * low on the first and high on the second, and the difference says which kind of work it needs.
     */
    data class SetReading(val readFully: Int, val total: Int, val abilitiesRead: Int, val abilities: Int)

    /** Per-set AI-reading scores, keyed by set code — or `ready = false` while the corpus is indexing. */
    data class SetSummary(val ready: Boolean, val sets: Map<String, SetReading>)

    /** One effect-type group the AI misses in a set — an audit flag key and the set's cards carrying it. */
    data class GapGroup(val key: String, val check: String, val effectType: String, val cards: List<String>)

    /** One card the AI does not read fully, with its flag keys. */
    data class UnreadCard(val name: String, val flags: List<String>)

    /**
     * A set's AI-reading detail: its score, its unread cards, and those cards grouped by the effect
     * type the AI misses, biggest group first — the groups are what `just ai-reading-loop` works on,
     * so the top of this list is the set's actionable part. `ready = false` while indexing.
     */
    data class SetDetail(
        val ready: Boolean,
        val score: SetReading?,
        val groups: List<GapGroup>,
        val unread: List<UnreadCard>,
        /** Where the draft AI's pick ratings for this set come from; null while indexing. */
        val draft: DraftRatings? = null,
    )

    /**
     * How the draft AI rates this set's cards: [rated] of [nonland] nonland cards are in the set's own
     * pick table (none at all when [hasTable] is false); the rest are filled in by the limited rater's
     * fallback — another set's table, a 17Lands win rate, or, for [heuristic] of them, the effect
     * heuristic. [archetypes] says whether the table carries archetype tags, which switches the pick
     * scorer to its archetype-aware path.
     */
    data class DraftRatings(
        val hasTable: Boolean,
        val rated: Int,
        val nonland: Int,
        val archetypes: Boolean,
        /** Of the cards the table leaves unrated, how many no data rates anywhere — the effect heuristic's guesses. */
        val heuristic: Int,
    )

    fun setSummary(): SetSummary {
        val current = index ?: run { warm(); return SetSummary(ready = false, sets = emptyMap()) }
        val sets = MtgSetCatalog.all.associate { set -> set.code to score(entries(set, current)) }
            .filterValues { it.total > 0 }
        return SetSummary(ready = true, sets = sets)
    }

    /** The detail for set [code]; null when no such set is catalogued. */
    fun setDetail(code: String): SetDetail? {
        val set = MtgSetCatalog.byCode(code) ?: return null
        val current = index ?: run { warm(); return SetDetail(ready = false, score = null, groups = emptyList(), unread = emptyList()) }
        val entries = entries(set, current)
        val unread = entries.filterNot { it.readFully }.sortedBy { it.name }
        val groups = unread.flatMap { e -> e.flags.map { it to e.name } }
            .groupBy({ it.first }, { it.second })
            .map { (key, cards) ->
                GapGroup(key, key.substringBefore(": "), key.substringAfter(": "), cards.distinct().sorted())
            }
            .sortedWith(compareByDescending<GapGroup> { it.cards.size }.thenBy { it.key })
        return SetDetail(
            ready = true,
            score = score(entries).takeIf { it.total > 0 },
            groups = groups,
            unread = unread.map { UnreadCard(it.name, it.flags) },
            draft = draftRatings(set),
        )
    }

    private fun draftRatings(set: MtgSet): DraftRatings {
        val tables = DraftsimData.tablesFor(listOf(set.code))
        val nonland = (set.cards.map { it.name } + set.printings.map { it.name }).distinct()
            .mapNotNull { registry.getCard(it) ?: registry.getCard(frontFace(it)) }
            .distinctBy { it.name }
            .filterNot { it.typeLine.isLand }
        val unrated = nonland.filterNot { DraftsimData.nameKey(it.name) in tables.ratings }
        return DraftRatings(
            hasTable = tables.ratings.isNotEmpty(),
            rated = nonland.size - unrated.size,
            nonland = nonland.size,
            archetypes = tables.archetypes.isNotEmpty(),
            heuristic = unrated.count { LimitedCardRater.explain(it).source == LimitedCardRater.Source.HEURISTIC },
        )
    }

    /**
     * The set's authored cards and its reprint rows, by name — what the set's coverage counts.
     * Basic lands are left out: they have nothing to read, and would only inflate the score.
     */
    private fun entries(set: MtgSet, current: Map<String, Entry>): List<Entry> =
        (set.cards.map { it.name } + set.printings.map { it.name }).distinct()
            .mapNotNull { current.lookup(it) }
            .distinctBy { it.name }

    private fun score(entries: List<Entry>) = SetReading(
        readFully = entries.count { it.readFully },
        total = entries.size,
        abilitiesRead = entries.sumOf { it.abilitiesRead },
        abilities = entries.sumOf { it.abilities },
    )

    private fun Map<String, Entry>.lookup(name: String): Entry? = this[name] ?: this[frontFace(name)]

    // ── Index ───────────────────────────────────────────────────────────────

    /** Start indexing the corpus, once, in the background. */
    fun warm() {
        if (!started.compareAndSet(false, true)) return
        Thread({ build() }, "ai-reading-index").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }.start()
    }

    private fun build() {
        val started = System.nanoTime()
        val built = HashMap<String, Entry>()
        for (name in registry.allCardNames()) {
            val card = registry.getCard(name) ?: continue
            // A reader that throws on one card must not take the index down; that card reads as
            // not read, which is what a crash in the AI's reading of it would mean in play too.
            val entry = runCatching {
                val reading = AiReadingAudit.read(card, lookup)
                val checked = reading.abilities.filter { it.checked }
                Entry(card.name, reading.flags.map { it.key }, checked.count { it.readFully }, checked.size)
            }.onFailure { log.warn("AI reading failed for {}: {}", card.name, it.toString()) }
                .getOrDefault(Entry(card.name, listOf("error: the readers threw"), 0, 0))
            built[card.name] = entry
            built.putIfAbsent(frontFace(card.name), entry)
        }
        index = built
        log.info(
            "AI reading index: {} of {} cards read fully ({} ms)",
            built.values.distinctBy { it.name }.count { it.readFully }, built.values.distinctBy { it.name }.size,
            (System.nanoTime() - started) / 1_000_000,
        )
    }

    private fun view(reading: AiReadingAudit.CardReading) = AiReadingView(
        readFully = reading.readFully,
        flags = reading.flags.map { it.key },
        abilities = reading.abilities.map { ability ->
            AiAbilityView(
                kind = ability.ability.kind.name,
                title = ability.ability.title,
                face = ability.ability.face,
                text = ability.ability.text,
                checked = ability.checked,
                readFully = ability.readFully,
                findings = ability.findings.map { f ->
                    AiFindingView(
                        reader = f.check.reader,
                        check = f.check.label,
                        read = f.read,
                        reason = f.reason,
                        effectType = f.effectType,
                        step = f.step,
                        stepText = f.stepText,
                        target = f.target,
                        mode = f.mode,
                    )
                },
            )
        },
    )

    private fun ratingView(r: AiCardRating.Rating): AiRatingView {
        fun part(p: AiCardRating.Part) = AiPartView(p.label, p.value, p.detail)
        val e = r.limited.explanation
        return AiRatingView(
            pick = AiPickView(
                rating = e.rating,
                source = when (e.source) {
                    LimitedCardRater.Source.BASIC_LAND -> "basicLand"
                    LimitedCardRater.Source.LAND -> "land"
                    LimitedCardRater.Source.WIN_RATE -> "winRate"
                    LimitedCardRater.Source.TABLE -> "table"
                    LimitedCardRater.Source.HEURISTIC -> "heuristic"
                },
                tableSet = e.tableSet,
                winRate = e.winRate,
                games = e.games,
                heuristic = e.heuristic.map { AiPartView(it.part, it.value, it.detail) },
                heuristicRating = e.heuristicRating,
                sets = r.limited.sets.map { AiSetPickView(it.set, it.rating, it.fromTable, it.removal, it.archetypes) },
            ),
            board = AiBoardView(
                kind = when (r.board.kind) {
                    AiCardRating.BoardKind.CREATURE -> "creature"
                    AiCardRating.BoardKind.VARIABLE_CREATURE -> "variableCreature"
                    AiCardRating.BoardKind.PERMANENT -> "permanent"
                    AiCardRating.BoardKind.PLANESWALKER -> "planeswalker"
                    AiCardRating.BoardKind.LAND -> "land"
                    AiCardRating.BoardKind.SPELL -> "spell"
                },
                value = r.board.value,
                parts = r.board.parts.map(::part),
                rule = r.board.rule,
            ),
            intent = AiIntentView(r.intent.tags.map { AiTagView(it.key, it.label) }, r.intent.speed, r.intent.facts),
            advisor = r.advisor,
        )
    }

    private companion object {
        private val log = LoggerFactory.getLogger(AiReadingService::class.java)
    }
}

/** Strip a ` // back` suffix, matching how the coverage view keys double-faced cards. */
private fun frontFace(name: String): String = name.substringBefore(" // ").trim()
