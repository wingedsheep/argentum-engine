package com.wingedsheep.ai.engine

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.RemovalBreadth
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Rates cards for limited (sealed/draft) play on a 0.0–5.0 scale.
 *
 * Three sources, best first: real 17Lands win-rate statistics where we have them, then the
 * vendored Draftsim pick ratings (which cover far more sets), then the structural heuristic below.
 *
 * Scale:
 *   5.0 = Bomb (wins the game by itself)
 *   4.0 = Premium removal or top-tier creature
 *   3.0 = Good playable (solid curve filler, decent effect)
 *   2.0 = Filler (playable but not exciting)
 *   1.0 = Borderline (only if desperate)
 *   0.0 = Unplayable
 */
object LimitedCardRater {

    @Serializable
    private data class CardRating(
        val winRate: Double? = null,
        val gihWinRate: Double? = null,
        val gameCount: Int? = null,
        val gihGameCount: Int? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Card name → 17Lands rating data, keyed by set code. Loaded lazily.
     *
     * This object is a process-wide singleton read from every AI instance, so the
     * cache must survive concurrent access — the arena runs games on N threads.
     */
    private val ratingsCache = ConcurrentHashMap<String, Map<String, CardRating>>()

    /**
     * Which sets have a 17Lands file, read from `/ratings/_manifest.json`.
     *
     * Phase 6 replaced a hardcoded `listOf("BLB")` with this. Shipping data for another set is now
     * a JSON file plus a line in the manifest — no Kotlin change — which is the point: the list
     * used to be a literal one contributor at a time would forget to update.
     */
    private val ratedSetCodes: List<String> by lazy {
        val text = LimitedCardRater::class.java.getResourceAsStream("/ratings/_manifest.json")
            ?.bufferedReader()?.use { it.readText() }
            ?: return@lazy emptyList()
        json.decodeFromString<List<String>>(text)
    }

    /** All loaded ratings merged (name → best available rating). */
    private val allRatings: Map<String, CardRating> by lazy {
        val merged = mutableMapOf<String, CardRating>()
        for (code in ratedSetCodes) {
            loadSetRatings(code).forEach { (name, rating) ->
                // Keep the entry with higher game count
                val existing = merged[name]
                if (existing == null || (rating.gameCount ?: 0) > (existing.gameCount ?: 0)) {
                    merged[name] = rating
                }
            }
        }
        merged
    }

    private fun loadSetRatings(setCode: String): Map<String, CardRating> {
        return ratingsCache.computeIfAbsent(setCode) {
            val resource = LimitedCardRater::class.java.getResourceAsStream("/ratings/$setCode.json")
            if (resource != null) {
                json.decodeFromString<Map<String, CardRating>>(resource.bufferedReader().readText())
            } else {
                emptyMap()
            }
        }
    }

    /**
     * The Draftsim-scale pick ratings, already on this rater's 0–5 scale and covering ~83 sets
     * — `DraftsimData`'s tables, merged across every set it ships. Most are vendored from the
     * Draftsim bundle; the sets the bundle never covered are first-party tables written on
     * the same scale (see `DraftsimData`'s class doc).
     *
     * The two rating stores are *not* duplicates, which is why this consolidates by **chaining**
     * rather than by deleting one: the `ratings/` resources are raw 17Lands win-rate data (one
     * set), and the `draftai/ratings/` resources are a curated pick rating (many sets). Preferring
     * the measured win rate and falling back to the curated rating gives the rater real data on far
     * more of the catalog than either store alone, with no new files.
     */
    private val draftsimRatings: Map<String, Double> by lazy {
        DraftsimData.tablesFor(DraftsimData.ratedSetCodes()).ratings
    }

    fun rate(card: CardDefinition): Double = measured(card)?.rating ?: heuristicRate(card)

    /** Where a [rate] came from — the first of the three sources that had an answer. */
    enum class Source {
        /** Basic lands are always 0. */
        BASIC_LAND,
        /** Non-basic lands are flat filler. */
        LAND,
        /** A 17Lands game-in-hand win rate with enough games behind it. */
        WIN_RATE,
        /** A Draftsim-scale pick table, vendored or first-party. */
        TABLE,
        /** The structural effect heuristic. */
        HEURISTIC,
    }

    /** One additive term of the heuristic — what one part of the card adds to its rating. */
    data class Term(val part: String, val value: Double, val detail: String? = null)

    /**
     * [rate], with its working shown, for a developer view. [rating] is exactly [rate]'s answer;
     * [heuristic] is always the heuristic's breakdown, even when a table outranked it, so a view can
     * show where the structural estimate disagrees with the measured one.
     */
    data class Explanation(
        val rating: Double,
        val source: Source,
        /** [Source.TABLE]: the table that rated it — the first rated set, in code order. */
        val tableSet: String? = null,
        /** [Source.WIN_RATE]: the game-in-hand win rate, and the games behind it. */
        val winRate: Double? = null,
        val games: Int? = null,
        val heuristic: List<Term>,
        val heuristicRating: Double,
    )

    fun explain(card: CardDefinition): Explanation {
        val terms = heuristicTerms(card)
        val heuristic = terms.sumOf { it.value }.coerceIn(0.0, 5.0)
        val measured = measured(card)
        if (measured == null) return Explanation(heuristicRate(card), Source.HEURISTIC, heuristic = terms, heuristicRating = heuristic)
        val tableSet = if (measured.source == Source.TABLE) {
            val key = DraftsimData.nameKey(card.name)
            DraftsimData.ratedSetCodes().sorted().firstOrNull { key in DraftsimData.tablesFor(listOf(it)).ratings }
        } else null
        return measured.copy(tableSet = tableSet, heuristic = terms, heuristicRating = heuristic)
    }

    /** The land rule, the win rate or the table — whichever answers first — else null. */
    private fun measured(card: CardDefinition): Explanation? {
        fun of(rating: Double, source: Source, winRate: Double? = null, games: Int? = null) =
            Explanation(rating, source, winRate = winRate, games = games, heuristic = emptyList(), heuristicRating = 0.0)
        if (card.typeLine.isBasicLand) return of(0.0, Source.BASIC_LAND)
        if (card.typeLine.isLand) return of(1.5, Source.LAND) // non-basic lands are decent filler

        // Try 17Lands data first
        val dataRating = allRatings[card.name]
        if (dataRating != null) {
            val winRate = dataRating.gihWinRate ?: dataRating.winRate
            if (winRate != null && (dataRating.gameCount ?: 0) >= 50) {
                return of(winRateToRating(winRate), Source.WIN_RATE, winRate, dataRating.gameCount)
            }
        }

        // Then the curated pick rating, which covers far more sets than the win-rate data.
        draftsimRatings[DraftsimData.nameKey(card.name)]?.let { return of(it.coerceIn(0.0, 5.0), Source.TABLE) }
        return null
    }

    /**
     * Convert a 17Lands win rate (typically 0.40–0.65) to a 0.0–5.0 rating.
     *
     * Mapping:
     *   0.45 (bad) → 1.0
     *   0.50 (average) → 2.5
     *   0.55 (good) → 3.5
     *   0.58 (great) → 4.0
     *   0.62+ (bomb) → 4.5–5.0
     *
     * Uses a piecewise linear mapping calibrated to 17Lands Premier Draft data.
     */
    private fun winRateToRating(winRate: Double): Double {
        return when {
            winRate >= 0.62 -> 5.0
            winRate >= 0.58 -> 4.0 + (winRate - 0.58) / (0.62 - 0.58) * 1.0
            winRate >= 0.55 -> 3.5 + (winRate - 0.55) / (0.58 - 0.55) * 0.5
            winRate >= 0.50 -> 2.5 + (winRate - 0.50) / (0.55 - 0.50) * 1.0
            winRate >= 0.45 -> 1.0 + (winRate - 0.45) / (0.50 - 0.45) * 1.5
            else -> 0.5
        }.coerceIn(0.0, 5.0)
    }

    /** Heuristic base rating of a planeswalker, before its loyalty abilities and rarity. */
    private const val PLANESWALKER_BASE = 3.0

    private fun heuristicRate(card: CardDefinition): Double {
        var rating = baseRating(card)
        rating += keywordBonus(card)
        rating += effectBonus(card)
        rating += rarityBonus(card)
        rating += curveBonus(card)
        return rating.coerceIn(0.0, 5.0)
    }

    /**
     * [heuristicRate] term by term, uncapped: the base, each keyword that moves the number, each
     * effect slot, rarity and curve. Sums to [heuristicRate] before its 0–5 clamp. Not on the hot
     * path — [heuristicRate] stays the one the AI calls.
     */
    private fun heuristicTerms(card: CardDefinition): List<Term> = buildList {
        add(Term("Base", baseRating(card), baseDetail(card)))
        for (keyword in card.keywords.sortedBy { it.name }) {
            val v = KEYWORD_BONUS[keyword] ?: continue
            add(Term(keyword.displayName, v))
        }
        // The same slots [effectBonus] sums ([EffectWalker.slots]).
        fun slot(part: String, slot: EffectWalker.Slot) {
            val value = slotValue(slot)
            if (value == 0.0) return
            val breadth = narrowDiscount(slot)
            val detail = listOfNotNull(
                slot.effect.description.takeIf { it.isNotBlank() },
                "narrow removal, ×${"%.2f".format(java.util.Locale.ROOT, breadth)}".takeIf { breadth < 1.0 },
            ).joinToString(" — ").takeIf { it.isNotEmpty() }
            add(Term(part, value, detail))
        }
        // Numbered the way the AI-reading audit numbers abilities: activated abilities count the
        // mana ones [EffectWalker.slots] skips.
        val script = card.script
        val slots = EffectWalker.slots(script).iterator()
        script.spellEffect?.let { slot("Spell", slots.next()) }
        script.triggeredAbilities.indices.forEach { i -> slot("Triggered ability ${i + 1}", slots.next()) }
        script.activatedAbilities.forEachIndexed { i, a ->
            if (!a.isManaAbility) slot("Activated ability ${i + 1}", slots.next())
        }
        rarityBonus(card).takeIf { it != 0.0 }?.let { add(Term("Rarity", it, card.metadata.rarity.name.lowercase())) }
        curveBonus(card).takeIf { it != 0.0 }?.let { add(Term("Curve", it, "mana value ${card.cmc}")) }
    }

    private fun baseDetail(card: CardDefinition): String = when {
        card.typeLine.isCreature -> {
            val p = card.creatureStats?.basePower
            val t = card.creatureStats?.baseToughness
            if (p == null || t == null) "creature with variable stats"
            else "creature, ${p + t} stats for ${card.cmc.coerceAtLeast(1)} mana"
        }
        CardType.PLANESWALKER in card.typeLine.cardTypes -> "planeswalker"
        card.typeLine.isInstant -> "instant"
        card.typeLine.isSorcery -> "sorcery"
        card.typeLine.isEnchantment -> "enchantment"
        card.typeLine.isArtifact -> "artifact"
        else -> "other"
    }

    /**
     * Base rating from creature efficiency or spell type.
     */
    private fun baseRating(card: CardDefinition): Double {
        if (card.typeLine.isCreature) {
            val stats = card.creatureStats ?: return 1.5
            val power = stats.basePower ?: return 2.0 // dynamic stats are usually good
            val toughness = stats.baseToughness ?: return 2.0
            val cmc = card.cmc.coerceAtLeast(1)

            // Stat efficiency: total stats vs CMC
            val statTotal = power + toughness
            val efficiency = statTotal.toDouble() / cmc

            // Base: 2.0 for average, scale with efficiency
            // A 2/2 for 2 (efficiency=2.0) is average
            // A 3/3 for 3 (efficiency=2.0) is average
            // A 4/4 for 3 (efficiency=2.67) is good
            // A 2/1 for 1 (efficiency=3.0) is aggressive
            return when {
                efficiency >= 3.0 -> 3.0
                efficiency >= 2.5 -> 2.7
                efficiency >= 2.0 -> 2.3
                efficiency >= 1.5 -> 2.0
                else -> 1.5
            }
        }

        // A planeswalker is a repeatable engine that has to be attacked to be answered — in limited
        // nearly every one is a first-pick. Its loyalty abilities still add their own effect value
        // on top (as activated abilities), but most of what a walker does — a fresh token, a card,
        // a counter every turn while soaking damage — is the recurrence, which no single effect
        // leaf captures. Without this floor the generic "other permanent" 1.5 put a five-mana
        // rare walker below a vanilla 3/9.
        if (CardType.PLANESWALKER in card.typeLine.cardTypes) return PLANESWALKER_BASE

        // Non-creature spells
        return when {
            card.typeLine.isInstant -> 2.2   // instants are flexible
            card.typeLine.isSorcery -> 2.0
            card.typeLine.isEnchantment -> 1.8
            card.typeLine.isArtifact -> 1.5
            else -> 1.5
        }
    }

    /**
     * Bonus for combat-relevant keywords.
     */
    private fun keywordBonus(card: CardDefinition): Double =
        card.keywords.sumOf { KEYWORD_BONUS[it] ?: 0.0 }

    /** What each combat-relevant keyword adds to a limited rating; one table, read by both paths. */
    private val KEYWORD_BONUS: Map<Keyword, Double> = mapOf(
        // Evasion (most valuable in limited)
        Keyword.FLYING to 0.8,
        Keyword.MENACE to 0.4,
        Keyword.INTIMIDATE to 0.4,
        Keyword.FEAR to 0.3,
        Keyword.SHADOW to 0.5,
        Keyword.TRAMPLE to 0.3,
        // Combat keywords
        Keyword.FIRST_STRIKE to 0.4,
        Keyword.DOUBLE_STRIKE to 0.8,
        Keyword.DEATHTOUCH to 0.5,
        Keyword.LIFELINK to 0.4,
        Keyword.VIGILANCE to 0.2,
        // Speed
        Keyword.HASTE to 0.3,
        Keyword.FLASH to 0.3,
        // Defense
        Keyword.INDESTRUCTIBLE to 0.6,
        Keyword.HEXPROOF to 0.4,
        Keyword.REACH to 0.2,
        // Negative
        Keyword.DEFENDER to -0.5,
        // Landwalk (situational but free wins)
        Keyword.SWAMPWALK to 0.2,
        Keyword.FORESTWALK to 0.2,
        Keyword.ISLANDWALK to 0.2,
        Keyword.MOUNTAINWALK to 0.2,
        Keyword.PLAINSWALK to 0.2,
    )

    /**
     * Bonus for spell/ability effects. Walks the effect tree to detect
     * removal, card draw, tokens, and other high-value effects.
     *
     * The traversal — which script slots count, how much each is discounted, how gates and
     * composites fold — lives in [EffectWalker], shared with
     * [com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer]. Only the leaf scoring below is
     * this rater's own.
     */
    private fun effectBonus(card: CardDefinition): Double =
        EffectWalker.slots(card.script).sumOf(::slotValue)

    /**
     * One slot's effect value, discounted for where it lives and — when it is removal that can aim
     * at only a sliver of what an opponent plays — for how narrow it is ([narrowDiscount]).
     */
    private fun slotValue(slot: EffectWalker.Slot): Double =
        EffectWalker.fold(slot.effect, LimitedScoreFold) * slot.origin.discount * narrowDiscount(slot)

    /**
     * 1.0 for removal that can aim at most of the table ([RemovalBreadth] of [FULL_BREADTH] or more)
     * and for every non-removal slot; down to [NARROWEST_SHARE] for removal that can aim at nothing
     * an opponent is likely to have. Execute ("destroy target white creature") reaches three of the
     * eighteen probe creatures, so its slot — the removal and the card it draws — keeps under half
     * its value: in a deck that doesn't know its opponent, the card is usually a cantrip.
     */
    private fun narrowDiscount(slot: EffectWalker.Slot): Double {
        val breadth = RemovalBreadth.ofTargets(slot.targets)
        if (breadth >= FULL_BREADTH) return 1.0
        if (EffectWalker.leaves(slot.effect).none(RemovalBreadth::isRemoval)) return 1.0
        return NARROWEST_SHARE + (1 - NARROWEST_SHARE) * breadth / FULL_BREADTH
    }

    /** Removal breadth from which a slot is rated in full: Doom Blade (15/18), Smother (12/18). */
    private const val FULL_BREADTH = 0.6

    /** The share of its value a removal slot that can aim at nothing keeps. */
    private const val NARROWEST_SHARE = 0.2

    /** What one effect is worth to a limited rating — the leaf scores folded over its tree. */
    internal fun effectValue(effect: Effect): Double = EffectWalker.fold(effect, LimitedScoreFold)

    /** What a keyword adds to a limited rating, or 0 for one the table doesn't price. */
    internal fun keywordValue(keyword: Keyword): Double = KEYWORD_BONUS[keyword] ?: 0.0

    /** How a limited rating folds an effect tree. See [EffectWalker.Fold]. */
    private object LimitedScoreFold : EffectWalker.Fold<Double> {
        override fun leaf(effect: Effect): Double = scoreEffect(effect)

        /** Composite — sum children, capped so a long pipeline can't run away with the rating. */
        override fun composite(parts: List<Double>): Double = parts.sum().coerceAtMost(2.5)

        /** Conditional — discount both branches; neither is guaranteed. */
        override fun conditional(thenValue: Double, elseValue: Double?): Double =
            (thenValue + (elseValue ?: 0.0)) * 0.6

        /** Optional "may" — discount the payoff. */
        override fun may(thenValue: Double): Double = thenValue * 0.8
    }

    /**
     * Score one leaf effect for limited value.
     */
    private fun scoreEffect(effect: Effect): Double {
        return when (effect) {
            // Removal — the most valuable effect type in limited
            is MoveToZoneEffect -> when {
                effect.byDestruction -> 1.5               // destroy
                effect.destination == Zone.EXILE -> 1.6    // exile (better than destroy)
                effect.destination == Zone.HAND -> 0.8     // bounce
                effect.destination == Zone.LIBRARY -> 1.0  // tuck
                else -> 0.3
            }
            is MoveUntilSourceLeavesEffect -> when (effect.destination) {
                Zone.EXILE -> 1.4
                Zone.HAND -> 0.8
                Zone.LIBRARY -> 1.0
                else -> 0.3
            }
            is ExileUntilLeavesEffect -> 1.4               // O-Ring removal
            is ForceSacrificeEffect -> 1.2                  // edict
            is PutOnLibraryPositionOfChoiceEffect -> 1.0    // tuck

            // Damage — removal or reach
            is DealDamageEffect -> {
                val amount = (effect.amount as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 3
                when {
                    amount >= 5 -> 1.4
                    amount >= 3 -> 1.2
                    amount >= 2 -> 0.8
                    else -> 0.4
                }
            }
            is DividedDamageEffect -> 1.3
            is FightEffect -> 1.0

            // Card advantage
            is DrawCardsEffect -> {
                val count = (effect.count as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 1
                count * 0.6
            }
            is DrawUpToEffect -> effect.maxCards * 0.4

            // Tokens — board presence
            is CreateTokenEffect -> {
                val count = (effect.count as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 1
                val statValue = (effect.power + effect.toughness) / 3.0
                count * statValue * 0.5
            }

            // Stat boosts
            is ModifyStatsEffect -> {
                val p = (effect.powerModifier as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 0
                val t = (effect.toughnessModifier as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 0
                (p + t) * 0.15
            }

            // Life gain — low value in limited
            is GainLifeEffect -> 0.1
            is LoseLifeEffect -> 0.2

            // Counter manipulation
            is AddCountersEffect -> 0.4
            is AdaptEffect -> 0.4

            // Keyword grants
            is GrantKeywordEffect -> 0.2

            // Control effects — premium
            is GainControlEffect -> 1.5

            // Counterspells
            is CounterEffect -> 0.8

            // Everything else — including modal interiors and pipeline stages, which
            // [EffectWalker] deliberately does not descend into.
            else -> 0.0
        }
    }

    /**
     * Rarity bonus — rares and mythics tend to be more powerful.
     */
    private fun rarityBonus(card: CardDefinition): Double {
        return when (card.metadata.rarity) {
            Rarity.MYTHIC -> 0.8
            Rarity.RARE -> 0.5
            Rarity.UNCOMMON -> 0.2
            else -> 0.0
        }
    }

    /**
     * Mana curve bonus — 2-3 drops are the backbone of limited.
     */
    private fun curveBonus(card: CardDefinition): Double {
        if (!card.typeLine.isCreature) return 0.0
        return when (card.cmc) {
            1 -> 0.1    // aggressive but fragile
            2 -> 0.3    // 2-drops are premium
            3 -> 0.2    // solid curve
            4 -> 0.1    // top of curve
            5 -> -0.1   // expensive
            else -> -0.2 // very expensive
        }
    }
}
