package com.wingedsheep.ai.engine

import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.engine.knowledge.AmountSign
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.DamageAmplifiers
import com.wingedsheep.ai.engine.knowledge.VehicleBody
import com.wingedsheep.ai.engine.knowledge.LossShields
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.ai.engine.knowledge.RemovalBreadth
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustBlock
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
        rating += staticBonus(card)
        rating += replacementBonus(card)
        rating += manaAbilityBonus(card)
        rating += rarityBonus(card)
        rating += curveBonus(card)
        rating += sorceryPumpPenalty(card)
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
            val value = slotValue(slot, card)
            if (value == 0.0) return
            val breadth = narrowDiscount(slot)
            val detail = listOfNotNull(
                slot.effect.description.takeIf { it.isNotBlank() },
                "activation costs ${slot.activationManaValue} mana, ×${activationDiscount(slot)}".takeIf { activationDiscount(slot) < 1.0 },
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
        script.sagaChapters.sortedBy { it.chapter }.forEach { slot("Chapter ${it.chapter}", slots.next()) }
        script.activatedAbilities.forEachIndexed { i, a ->
            if (!a.isManaAbility) slot("Activated ability ${i + 1}", slots.next())
        }
        script.staticAbilities.forEachIndexed { i, static ->
            val v = staticScore(static)
            if (v != 0.0) add(Term("Static ability ${i + 1}", v, static.description.takeIf { it.isNotBlank() }))
        }
        script.replacementEffects.forEachIndexed { i, replacement ->
            val v = replacementScore(replacement)
            if (v != 0.0) add(Term("Replacement effect ${i + 1}", v, replacement.description.takeIf { it.isNotBlank() }))
        }
        manaAbilityBonus(card).takeIf { it != 0.0 }?.let { add(Term("Mana ability", it, "a mana source that isn't a land")) }
        rarityBonus(card).takeIf { it != 0.0 }?.let { add(Term("Rarity", it, card.metadata.rarity.name.lowercase())) }
        curveBonus(card).takeIf { it != 0.0 }?.let { add(Term("Curve", it, "mana value ${card.cmc}")) }
        sorceryPumpPenalty(card).takeIf { it != 0.0 }?.let {
            add(Term("Sorcery pump", it, "temporary single-creature stats without surprise or lasting value, mana value ${card.cmc}"))
        }
    }

    /**
     * A single-creature sorcery pump spends a card before the opponent chooses blocks, and its
     * stats disappear after that fight. The generic stats sum otherwise prices a four-mana +5/+5
     * like premium removal. Discount the missing surprise and the mana committed to that one turn.
     * Only the plain pump, optionally forcing one blocker, qualifies: drawing a card, lasting
     * counters, evasion, Lure's team-wide opening and mass pumps retain their own valuations.
     */
    private fun sorceryPumpPenalty(card: CardDefinition): Double {
        if (!card.typeLine.isSorcery) return 0.0
        val effect = card.script.spellEffect ?: return 0.0
        val leaves = EffectWalker.leaves(effect)
        val pump = leaves.filterIsInstance<ModifyStatsEffect>().singleOrNull() ?: return 0.0
        if (pump.duration != Duration.EndOfTurn ||
            AmountSign.of(pump.powerModifier) !in setOf(AmountSign.GAIN, AmountSign.ZERO) ||
            AmountSign.of(pump.toughnessModifier) !in setOf(AmountSign.GAIN, AmountSign.ZERO)
        ) return 0.0
        // A group or source target is not the single targeted creature this discount describes.
        if (pump.target !is com.wingedsheep.sdk.scripting.targets.EffectTarget.BoundVariable &&
            pump.target !is com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget
        ) return 0.0
        if (card.script.targetRequirements.size != 1 || card.script.targetRequirements.single().count != 1) return 0.0
        if (leaves.any { it !== pump &&
                !(it is MustBeBlockedEffect && !it.allCreatures && it.target == pump.target)
            }) return 0.0
        return -(0.4 + 0.35 * card.cmc)
    }

    private fun baseDetail(card: CardDefinition): String = vehicleCrew(card)?.let { crew ->
        val stats = card.creatureStats!!
        "Vehicle, ${stats.basePower!! + stats.baseToughness!!} stats for ${card.cmc.coerceAtLeast(1)} mana, crew $crew"
    } ?: when {
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
     * Base rating of a body with [power]/[toughness] for [manaValue] mana: total stats per mana.
     */
    private fun statEfficiencyRating(power: Int, toughness: Int, manaValue: Int): Double {
        val cmc = manaValue.coerceAtLeast(1)

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

    /** The Crew N of a Vehicle with a printed body, or null for anything else. */
    private fun vehicleCrew(card: CardDefinition): Int? {
        if (card.typeLine.isCreature) return null
        val stats = card.creatureStats ?: return null
        if (stats.basePower == null || stats.baseToughness == null) return null
        return VehicleBody.printedCrew(card)
    }

    /**
     * Base rating from creature efficiency or spell type.
     */
    private fun baseRating(card: CardDefinition): Double {
        vehicleCrew(card)?.let { crew ->
            // A Vehicle's body, rated as the creature it becomes, less what tapping the crew costs
            // ([VehicleBody.DRAFT_CREW_PENALTY] a point) — never below a blank artifact's base.
            val stats = card.creatureStats!!
            return (statEfficiencyRating(stats.basePower!!, stats.baseToughness!!, card.cmc) -
                VehicleBody.DRAFT_CREW_PENALTY * crew).coerceAtLeast(1.5)
        }
        if (card.typeLine.isCreature) {
            val stats = card.creatureStats ?: return 1.5
            val power = stats.basePower ?: return 2.0 // dynamic stats are usually good
            val toughness = stats.baseToughness ?: return 2.0
            return statEfficiencyRating(power, toughness, card.cmc)
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
        Keyword.AFFLICT to 0.3,
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
     * this rater's own — plus opening a pipeline's "if those cards include" leaf onto both branches,
     * which [EffectWalker.leaves] keeps whole.
     */
    private fun effectBonus(card: CardDefinition): Double =
        EffectWalker.slots(card.script).sumOf { slotValue(it, card) }

    /**
     * One slot's effect value — its leaves folded, plus what its pipelines add up to — discounted
     * for where it lives and, when it is removal that can aim
     * at only a sliver of what an opponent plays — for how narrow it is ([narrowDiscount]).
     */
    private fun slotValue(slot: EffectWalker.Slot, card: CardDefinition): Double =
        (EffectWalker.fold(slot.effect, LimitedScoreFold(slot.targets, card)) + pipelineScore(slot.effect, slot.targets, slot.turnAhead, card)) * slot.origin.discount * narrowDiscount(slot) * activationDiscount(slot)

    // Four-mana activations are already slow; above that, retain only the share likely to be used.
    private fun activationDiscount(slot: EffectWalker.Slot): Double =
        if (slot.origin == EffectWalker.Origin.ACTIVATED) 4.0 / slot.activationManaValue.coerceAtLeast(4) else 1.0

    /**
     * 1.0 for removal that can aim at most of the table ([RemovalBreadth] of [FULL_BREADTH] or more)
     * and for every non-removal slot; down to [NARROWEST_SHARE] for removal that can aim at nothing
     * an opponent is likely to have — a spot spell or a sweeper ("destroy all white creatures").
     * Execute ("destroy target white creature") reaches three of the
     * eighteen probe creatures, so its slot — the removal and the card it draws — keeps under half
     * its value: in a deck that doesn't know its opponent, the card is usually a cantrip.
     */
    private fun narrowDiscount(slot: EffectWalker.Slot): Double {
        // A slot aiming only off the table (null) isn't removal to discount: the heuristic prices
        // its graveyard or hand effect as whatever it is.
        val breadth = RemovalBreadth.forLimitedSlot(slot) ?: return 1.0
        if (breadth >= FULL_BREADTH) return 1.0
        if (!RemovalBreadth.isRemoval(slot.effect)) return 1.0
        return NARROWEST_SHARE + (1 - NARROWEST_SHARE) * breadth / FULL_BREADTH
    }

    /** Removal breadth from which a slot is rated in full: Doom Blade (15/18), Smother (12/18). */
    private const val FULL_BREADTH = 0.6

    /** The share of its value a removal slot that can aim at nothing keeps. */
    private const val NARROWEST_SHARE = 0.2

    /** What one effect is worth to a limited rating — the leaf scores folded over its tree. */
    internal fun effectValue(effect: Effect): Double = EffectWalker.fold(effect, LimitedScoreFold())

    /** What a keyword adds to a limited rating, or 0 for one the table doesn't price. */
    internal fun keywordValue(keyword: Keyword): Double = KEYWORD_BONUS[keyword] ?: 0.0

    /**
     * What the card's static abilities are worth — an Aura's or Equipment's bonus, an anthem, a
     * keyword it grants, a Pacifism lock. [EffectWalker.slots] leaves statics out, so before this a
     * card whose whole text is a static scored its base alone.
     */
    private fun staticBonus(card: CardDefinition): Double = card.script.staticAbilities.sumOf { staticScore(it) }

    /**
     * One static ability, priced by [CardIntentAnalyzer]'s reading of it. A stat bonus is priced by
     * its size: on the creature it is attached to like a pump, battlefield-wide like an anthem.
     */
    private fun staticScore(static: StaticAbility): Double {
        if (static is ConditionalStaticAbility) return staticScore(static.ability) * 0.6
        // A keyword the creature grants itself is a keyword on its body, priced like a printed one.
        if (static is GrantKeyword && static.filter.scope == Scope.Self) {
            val keyword = runCatching { Keyword.valueOf(static.keyword.uppercase()) }.getOrNull()
            keyword?.let { KEYWORD_BONUS[it] }?.let { return it }
        }
        when (static) {
            is CanBlockAdditionalForCreatureGroup -> return 0.2 * static.count.coerceIn(0, 3)
            is CantBlock -> if (static.filter.scope == Scope.Self) return -0.3
            is CantAttackUnlessCoAttacker -> if (static.filter.scope == Scope.Self) return -0.3
            is CantBlockUnlessCoBlocker -> if (static.filter.scope == Scope.Self) return -0.2
            // "Attacks each combat if able" sends it into bad attacks; "blocks each combat if able"
            // into bad blocks, a milder cost since a block is usually wanted anyway.
            is MustAttack -> if (static.filter.scope == Scope.Self) return -0.2
            is MustBlock -> if (static.filter.scope == Scope.Self) return -0.1
            else -> Unit
        }
        if (static is ModifyStats) {
            val total = static.powerBonus + static.toughnessBonus
            if (total > 0) {
                return if (static.filter.scope == Scope.Battlefield) {
                    (total * ANTHEM_PER_STAT).coerceAtMost(ANTHEM_CAP)
                } else {
                    total * PUMP_PER_STAT
                }
            }
        }
        return tagValue(CardIntentAnalyzer.staticTags(static))
    }

    /**
     * Printed replacement payoffs: entering as a copy of the best creature, and multiplying
     * damage from our sources. Neither payoff is reflected in the printed body.
     */
    private fun replacementBonus(card: CardDefinition): Double =
        card.script.replacementEffects.sumOf { replacementScore(it) }

    private fun replacementScore(replacement: ReplacementEffect): Double = when {
        replacement is EntersAsCopy && replacement.copyOf == null &&
            replacement.copyFilter.cardPredicates.any { it == CardPredicate.IsCreature } ->
            // A gate (raid) means it often enters as its printed self: half the value.
            ENTRY_COPY_VALUE * (if (replacement.condition != null) 0.5 else 1.0)
        // A second life for its controller (Exquisite Archangel): about a removal spell's worth
        // of game the opponent has to win twice.
        LossShields.reads(replacement) -> LOSS_SHIELD_VALUE
        else -> DamageAmplifiers.pickValue(replacement)
    }

    /** "If you would lose the game, instead …" — the opponent has to win the game twice. */
    private const val LOSS_SHIELD_VALUE = 0.8

    /** A copy of the best creature in play, chosen as it enters — about a solid playable's worth. */
    private const val ENTRY_COPY_VALUE = 1.4

    /**
     * A non-land card with a mana ability is a mana source — [EffectWalker.slots] skips mana
     * abilities, as cost rather than payoff, so a mana rock or a dork read as [IntentTag.RAMP]
     * and was rated as its body alone.
     */
    private fun manaAbilityBonus(card: CardDefinition): Double =
        if (!card.typeLine.isLand && card.script.activatedAbilities.any { it.isManaAbility }) TAG_VALUE.getValue(IntentTag.RAMP)
        else 0.0

    /**
     * What a reading is worth when no leaf below prices the effect by its own fields: the highest
     * value among [tags]. This is what keeps the rating following the intent readers — teach
     * [CardIntentAnalyzer] a new effect and it is priced here with no second edit.
     */
    private fun tagValue(tags: Set<IntentTag>): Double = tags.maxOfOrNull { TAG_VALUE[it] ?: 0.0 } ?: 0.0

    /**
     * What the gather → move pipelines in a slot are worth. [EffectWalker] stops at pipeline stages,
     * and a gather or a move is worth nothing alone, so "destroy all creatures" and "look at the top
     * three, put one into your hand" scored zero. [CardIntentAnalyzer.pipelineTags] reads what the
     * stages add up to; this prices that reading like the leaf it stands for — a wrath or a picked
     * destroy like a destroy, a dig like one card drawn, a wish like a tutor — and graveyard targets the pipeline brings
     * onto our battlefield, however it splits them first (Boneyard Parley's piles), like the
     * recursion they are.
     */
    private fun pipelineScore(root: Effect, targets: List<TargetRequirement>, turnAhead: Boolean = false, card: CardDefinition? = null): Double {
        val reanimation = if (CardIntentAnalyzer.reanimatesChosenGraveyardTargets(root, targets)) {
            TAG_VALUE.getValue(IntentTag.RECURSION)
        } else 0.0
        // An exile into a pile the card may cast from (Maralen, Rona) is the dig it splits into two abilities.
        val pileCast = card?.let { CardIntentAnalyzer.pileCastTags(listOf(it.script) + it.cardFaces.map { face -> face.script }) }.orEmpty()
        val tags = CardIntentAnalyzer.pipelineTags(root, turnAhead, pileCast = pileCast)
        if (tags.isEmpty()) return reanimation
        var score = reanimation
        if (IntentTag.REMOVAL in tags) score += DESTROY_SCORE
        if (IntentTag.DRAW in tags) score += DRAW_PER_CARD
        // A spell cast for free off the pile this turn only (Maralen): no card banked, the cast itself.
        else if (IntentTag.FREE_SPELL in tags) score += TAG_VALUE.getValue(IntentTag.FREE_SPELL)
        if (IntentTag.GRAVEYARD_HATE in tags) score += TAG_VALUE.getValue(IntentTag.GRAVEYARD_HATE)
        // Cards taken out of another player's hand by a gather → move pipeline (Null Summoner's
        // "choose a nonland card from it; exile that card", Cranial Extraction's named search):
        // no single leaf says discard, so the pipeline reading prices it.
        if (IntentTag.DISCARD in tags) score += TAG_VALUE.getValue(IntentTag.DISCARD)
        // A wish (Burning Wish): the sideboard gather and the move to hand read as nothing alone.
        // A library search is priced at its own gather leaf, which a wish's gather isn't.
        if (IntentTag.TUTOR in tags) score += TAG_VALUE.getValue(IntentTag.TUTOR)
        return score
    }

    /** How a limited rating folds an effect tree. See [EffectWalker.Fold]. */
    private class LimitedScoreFold(
        private val targets: List<TargetRequirement> = emptyList(),
        private val source: CardDefinition? = null,
    ) : EffectWalker.Fold<Double> {
        override fun blink(exile: MoveToZoneEffect, returns: MoveToZoneEffect): Double = 0.3

        /**
         * A pipeline's "if those cards include …" branch scores as a conditional over both sides —
         * the same reading [EffectWalker.payoffLeaves] gives the intent and polarity readers.
         */
        override fun leaf(effect: Effect): Double =
            if (effect is ConditionalOnCollectionEffect) {
                conditional(
                    EffectWalker.fold(effect.ifNotEmpty, this),
                    effect.ifEmpty?.let { EffectWalker.fold(it, this) },
                )
            } else if (effect is DealDamageEffect && TargetPolarityAnalyzer.targetsOwnSide(effect.target, targets)) {
                // A toughness cost is not removal. Keep a modest penalty: survivors can pay it,
                // but it makes the cantrip less broadly usable than a free draw.
                -0.2
            } else {
                scoreEffect(effect, targets, source)
            }

        /** Composite — sum children, capped so a long pipeline can't run away with the rating. */
        override fun composite(parts: List<Double>): Double = parts.sum().coerceAtMost(COMPOSITE_CAP)

        /** Conditional — discount both branches; neither is guaranteed. */
        override fun conditional(thenValue: Double, elseValue: Double?): Double =
            (thenValue + (elseValue ?: 0.0)) * 0.6

        /** Optional "may" — discount the payoff. */
        override fun may(thenValue: Double): Double = thenValue * 0.8
    }

    /**
     * Score one leaf effect for limited value.
     */
    private fun scoreEffect(effect: Effect, targets: List<TargetRequirement>, source: CardDefinition?): Double {
        return when (effect) {
            // Mill spends no opposing board material. Give its library pressure a small term;
            // full library-depletion valuation belongs to the board evaluator.
            is GatherCardsEffect -> {
                val top = effect.source as? CardSource.TopOfLibrary
                if (top?.isMill == true) {
                    val amount = (top.count as? DynamicAmount.Fixed)?.amount ?: 4
                    (amount.coerceAtLeast(0) * 0.05).coerceAtMost(0.4)
                } else tagValue(CardIntentAnalyzer.leafTags(effect))
            }
            // Removal — the most valuable effect type in limited
            is MoveToZoneEffect -> when (CardIntentAnalyzer.graveyardMoveOf(effect, targets)) {
                // A card out of a graveyard is no permanent off the board: hate, or our own fuel.
                CardIntentAnalyzer.GraveyardMove.HATE -> TAG_VALUE.getValue(IntentTag.GRAVEYARD_HATE)
                CardIntentAnalyzer.GraveyardMove.OWN_EXILE -> 0.0
                null -> when {
                // "Exile target creature you control" into a linked pile: a stash, worth a blink.
                CardIntentAnalyzer.isOwnStash(effect, targets) -> 0.3
                effect.byDestruction -> DESTROY_SCORE     // destroy
                effect.destination == Zone.EXILE -> 1.6    // exile (better than destroy)
                effect.destination == Zone.HAND -> 0.8     // bounce
                effect.destination == Zone.LIBRARY -> 1.0  // tuck
                else -> 0.3
                }
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
            is DealDamageEffect ->
                damageScore((effect.amount as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 3)
            is DividedDamageEffect -> 1.3
            is FightEffect -> {
                // A self-fight can only kill within this creature's damage reach. A spell that
                // chooses both fighters, or a variable-power source, keeps the generic estimate.
                val selfFights = effect.target1 == EffectTarget.Self || effect.target2 == EffectTarget.Self
                val power = source?.creatureStats?.basePower
                // Extra effects or static/replacement abilities can change the fighter before
                // damage (entry counters, a pump, a transform). Keep the existing estimate unless
                // this is the simple body-plus-fight shape whose printed reach we can price.
                val simpleFighter = source != null && source.script.staticAbilities.isEmpty() &&
                    source.script.replacementEffects.isEmpty() &&
                    EffectWalker.slots(source.script).all { slot ->
                        EffectWalker.leaves(slot.effect).all { it is FightEffect }
                    }
                when {
                    !selfFights || power == null || !simpleFighter -> 1.0
                    power <= 0 -> 0.0
                    Keyword.DEATHTOUCH in source?.keywords.orEmpty() -> 1.0
                    else -> damageScore(power).coerceAtMost(1.0)
                }
            }

            // Card advantage
            is DrawCardsEffect -> {
                val count = (effect.count as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 1
                count * DRAW_PER_CARD
            }
            is DrawUpToEffect -> effect.maxCards * 0.4

            // Tokens — board presence
            is CreateTokenEffect -> {
                val count = (effect.count as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount ?: 1
                val statValue = (effect.power + effect.toughness) / 3.0
                count * statValue * 0.5
            }

            // A wrath spelled "for each creature: …" — [EffectWalker] stops at a ForEach, so price
            // the analyzer's sweeper reading of it like the pipeline spelling (see [pipelineScore]).
            is ForEachEffect ->
                if (IntentTag.SWEEPER in CardIntentAnalyzer.leafTags(effect)) DESTROY_SCORE
                // "Each opponent may search their library for a basic land" (Old-Growth Dryads):
                // the body's gain is the opponent's, so it is the card's price, not a payoff.
                else if (CardIntentAnalyzer.giftsOnlyToOpponents(effect)) -EffectWalker.fold(effect.body, LimitedScoreFold(source = source))
                // Iteration can rebind context targets; do not mistake an outer own-side
                // slot for the object visited by the loop.
                else EffectWalker.fold(effect.body, LimitedScoreFold(source = source))

            // "Choose one —" is worth its best mode(s), plus a little for the choice.
            is ModalEffect -> {
                val modes = effect.modes.map { EffectWalker.fold(it.effect, LimitedScoreFold(it.targetRequirements, source)) }.sortedDescending()
                val chosen = modes.take(effect.chooseCount.coerceAtLeast(1)).sum()
                val flexibility = if (modes.size > effect.chooseCount) MODAL_FLEXIBILITY else 0.0
                (chosen + flexibility).coerceAtMost(COMPOSITE_CAP)
            }

            // "Choose up to five {P} worth of modes": worth the distinct modes its budget buys,
            // best first, plus the choice. Repeats are left out — a second copy of a mode is
            // rarely worth the first (a second "draw for each creature that died" draws again,
            // a second sacrifice round needs creatures to eat).
            is BudgetModalEffect -> {
                var remaining = effect.budget
                var chosen = 0.0
                effect.modes.map { it.cost to EffectWalker.fold(it.effect, LimitedScoreFold(targets, source)) }
                    .sortedByDescending { it.second }
                    .forEach { (cost, value) ->
                        if (cost <= remaining && value > 0.0) { chosen += value; remaining -= cost.coerceAtLeast(0) }
                    }
                (chosen + MODAL_FLEXIBILITY).coerceAtMost(COMPOSITE_CAP)
            }

            // "Add {R} or {G}", "sacrifice an artifact or discard a card; if you do, draw two": a
            // choice the controller makes on resolution is worth its best branch, as a one-mode
            // modal is. A branch an opponent picks (a punisher) is theirs to minimise, so no
            // branch is what the card is worth.
            is ChooseActionEffect ->
                if (!EffectWalker.choosesOwnBranch(effect)) 0.0
                // A pipeline inside a branch is already priced by the slot's pipelineScore, which
                // reads through the choice (payoffLeaves).
                else effect.choices.maxOfOrNull { EffectWalker.fold(it.effect, LimitedScoreFold(targets, source)) }
                    ?.coerceAtMost(COMPOSITE_CAP) ?: 0.0

            // Card selection — scry and surveil see cards but draw none
            is ScryEffect -> (effect.count * SELECTION_PER_CARD).coerceAtMost(SELECTION_CAP)
            is SurveilEffect -> (effect.count * SELECTION_PER_CARD).coerceAtMost(SELECTION_CAP)

            // Stat boosts — or, as -N/-N on another creature, removal
            is ModifyStatsEffect -> {
                val p = (effect.powerModifier as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount
                val t = (effect.toughnessModifier as? com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed)?.amount
                // A shrink the intent reader calls removal kills like damage to toughness: -N/-N is
                // N damage, and a dynamic -X/-X (Death Wind, Defile) the unknown-X damage default.
                // Summing it as stats priced Grasp of Darkness below a vanilla instant.
                if (IntentTag.REMOVAL in CardIntentAnalyzer.leafTags(effect)) damageScore(t?.let { -it } ?: 3)
                else ((p ?: dynamicStat(effect.powerModifier)) + (t ?: dynamicStat(effect.toughnessModifier))) * 0.15
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
            is GainControlEffect -> if (effect.duration == Duration.EndOfTurn) 0.6 else 1.5

            // Counterspells
            is CounterEffect -> 0.8

            // Everything else — a tutor, a tapper, recursion, discard, a new reading: priced by what
            // the intent reader makes of it. Pipeline stages read as nothing alone (see [pipelineScore]).
            else -> tagValue(CardIntentAnalyzer.leafTags(effect))
        }
    }

    /**
     * A dynamic "+X" the sign reader can vouch for — a count, a power, double a power — priced as
     * +[DYNAMIC_STAT_GUESS], the way an unknown-X damage leaf is priced as 3. Anything else adds nothing.
     */
    private fun dynamicStat(amount: DynamicAmount): Int =
        if (AmountSign.of(amount) == AmountSign.GAIN) DYNAMIC_STAT_GUESS else 0

    /** A removal spell dealing [amount] damage, or giving -[amount]/-[amount]. */
    private fun damageScore(amount: Int): Double = when {
        amount >= 5 -> 1.4
        amount >= 3 -> 1.2
        amount >= 2 -> 0.8
        else -> 0.4
    }

    private const val DYNAMIC_STAT_GUESS = 2
    private const val DESTROY_SCORE = 1.5
    private const val COMPOSITE_CAP = 2.5
    private const val MODAL_FLEXIBILITY = 0.2
    private const val PUMP_PER_STAT = 0.15
    private const val ANTHEM_PER_STAT = 0.4
    private const val ANTHEM_CAP = 1.6

    /**
     * A reading's worth in [tagValue], sized against the leaves above that price the same effect by
     * its fields: removal by a destroy or a 3-damage burn, draw by one card, a pump by a +1/+1.
     */
    private val TAG_VALUE: Map<IntentTag, Double> = mapOf(
        IntentTag.SWEEPER to 1.5,
        IntentTag.EXILE_REMOVAL to 1.4,
        IntentTag.REMOVAL to 1.2,
        IntentTag.NEUTRALIZE to 1.0,
        IntentTag.FIGHT to 1.0,
        IntentTag.COUNTERSPELL to 0.8,
        // Turns their removal back on them or onto a sponge: a narrower counterspell, since it
        // needs a single-target spell and a legal place to send it.
        IntentTag.SPELL_REDIRECT to 0.5,
        // A second helping of a spell already cast: worth as much as the deck's best instant or
        // sorcery, and nothing without one — a little under a redirect.
        IntentTag.SPELL_COPY to 0.4,
        // A graveyard answered before it is used: real against recursion, flashback or delirium,
        // a blank against everything else — a sideboard card's worth in Limited.
        IntentTag.GRAVEYARD_HATE to 0.2,
        IntentTag.ANTHEM to 0.8,
        IntentTag.DRAW to 0.6,
        IntentTag.TUTOR to 0.6,
        IntentTag.RECURSION to 0.6,
        IntentTag.TAPPER to 0.5,
        IntentTag.TOKEN_MAKER to 0.5,
        IntentTag.DISCARD to 0.4,
        IntentTag.RAMP to 0.4,
        IntentTag.COST_REDUCER to 0.3,
        IntentTag.PUMP to 0.3,
        IntentTag.COMBAT_TRICK to 0.3,
        IntentTag.EVASION_GRANT to 0.3,
        // A first strike, a deathtouch, a forced block: it decides fights the way a pump does but
        // adds nothing to the body — a little under evasion.
        IntentTag.COMBAT_ENHANCEMENT to 0.2,
        // A free equip: worth a little on its own, more beside the Equipment it moves (the synergy
        // ask for Equipment carries that).
        IntentTag.EQUIPMENT_RELOCATION to 0.2,
        // Becoming a copy: worth the best thing on the board it can copy, which only play shows.
        IntentTag.PERMANENT_COPY to 0.2,
        IntentTag.PROTECTION to 0.3,
        IntentTag.SACRIFICE_OUTLET to 0.2,
        // A turn of no spells from the opponent: real tempo, but it answers nothing already on the
        // board — in Limited a little under a tapper.
        IntentTag.CAST_BAN to 0.3,
        // A tax slows every matching spell of theirs a turn without stopping any: real against a
        // removal-heavy hand, idle against creatures — under the ban.
        IntentTag.SPELL_TAX to 0.2,
        // A forced attack into our blockers: a combat trick the opponent walks into, but only as
        // good as the board we already have — priced like the cast ban.
        IntentTag.FORCED_ATTACK to 0.3,
        // A trigger lock shuts off enter triggers on both sides — in Limited, where most decks run
        // a few, it is a sideboard hoser more than a maindeck card.
        IntentTag.TRIGGER_LOCK to 0.1,
        // A counter lock blanks poison, -1/-1 and +1/+1 plans on both sides — a sideboard hoser
        // against a counters deck, priced like the trigger lock.
        IntentTag.COUNTER_LOCK to 0.1,
        IntentTag.TRIBAL_ENABLER to 0.1,
        // Stealing the opponent's tokens for a turn pays off only when they make some — a flash
        // response to a token spell, or a turn they'd have made Treasure or Clues anyway.
        IntentTag.TOKEN_THEFT to 0.2,
        // "That player loses the game" only pays when the creature carrying it connects — a
        // bomb's rider rather than a card on its own, so a little over a pump.
        IntentTag.GAME_WIN to 0.4,
        // A whole extra turn — another untap, draw and attack — is a Limited bomb's effect on a
        // sorcery, though usually a pricey or conditional one: a little over a counterspell.
        IntentTag.EXTRA_TURN to 1.0,
        // A spell cast for free is the mana it didn't cost — usually an attack or a cast trigger
        // away and only as good as the spells beside it: priced like ramp.
        IntentTag.FREE_SPELL to 0.4,
        IntentTag.CARD_SELECTION to 0.15,
        IntentTag.LIFEGAIN to 0.1,
    )
    private const val DRAW_PER_CARD = 0.6

    /** Scry or surveil, per card looked at: a fraction of a draw, capped well below one. */
    private const val SELECTION_PER_CARD = 0.1
    private const val SELECTION_CAP = 0.3

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
