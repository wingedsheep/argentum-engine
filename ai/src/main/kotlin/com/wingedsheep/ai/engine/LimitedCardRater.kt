package com.wingedsheep.engine.ai

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Rates cards for limited (sealed/draft) play on a 0.0–5.0 scale.
 *
 * When 17Lands data is available for a card (loaded from resource files),
 * the rating is derived from real win-rate statistics. Otherwise, falls back
 * to heuristic scoring.
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

    /** Card name → 17Lands rating data, keyed by set code. Loaded lazily. */
    private val ratingsCache = mutableMapOf<String, Map<String, CardRating>>()

    /** All loaded ratings merged (name → best available rating). */
    private val allRatings: Map<String, CardRating> by lazy {
        val merged = mutableMapOf<String, CardRating>()
        val setCodes = listOf("BLB") // Add more set codes as data becomes available
        for (code in setCodes) {
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
        return ratingsCache.getOrPut(setCode) {
            val resource = LimitedCardRater::class.java.getResourceAsStream("/ratings/$setCode.json")
            if (resource != null) {
                json.decodeFromString<Map<String, CardRating>>(resource.bufferedReader().readText())
            } else {
                emptyMap()
            }
        }
    }

    fun rate(card: CardDefinition): Double {
        if (card.typeLine.isBasicLand) return 0.0
        if (card.typeLine.isLand) return 1.5 // non-basic lands are decent filler

        // Try 17Lands data first
        val dataRating = allRatings[card.name]
        if (dataRating != null) {
            val winRate = dataRating.gihWinRate ?: dataRating.winRate
            if (winRate != null && (dataRating.gameCount ?: 0) >= 50) {
                return winRateToRating(winRate)
            }
        }

        // Fall back to heuristic
        return heuristicRate(card)
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

    private fun heuristicRate(card: CardDefinition): Double {
        var rating = baseRating(card)
        rating += keywordBonus(card)
        rating += effectBonus(card)
        rating += rarityBonus(card)
        rating += curveBonus(card)
        return rating.coerceIn(0.0, 5.0)
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
    private fun keywordBonus(card: CardDefinition): Double {
        var bonus = 0.0
        val keywords = card.keywords

        // Evasion (most valuable in limited)
        if (Keyword.FLYING in keywords) bonus += 0.8
        if (Keyword.MENACE in keywords) bonus += 0.4
        if (Keyword.INTIMIDATE in keywords) bonus += 0.4
        if (Keyword.FEAR in keywords) bonus += 0.3
        if (Keyword.SHADOW in keywords) bonus += 0.5
        if (Keyword.TRAMPLE in keywords) bonus += 0.3

        // Combat keywords
        if (Keyword.FIRST_STRIKE in keywords) bonus += 0.4
        if (Keyword.DOUBLE_STRIKE in keywords) bonus += 0.8
        if (Keyword.DEATHTOUCH in keywords) bonus += 0.5
        if (Keyword.LIFELINK in keywords) bonus += 0.4
        if (Keyword.VIGILANCE in keywords) bonus += 0.2

        // Speed
        if (Keyword.HASTE in keywords) bonus += 0.3
        if (Keyword.FLASH in keywords) bonus += 0.3

        // Defense
        if (Keyword.INDESTRUCTIBLE in keywords) bonus += 0.6
        if (Keyword.HEXPROOF in keywords) bonus += 0.4
        if (Keyword.REACH in keywords) bonus += 0.2

        // Negative
        if (Keyword.DEFENDER in keywords) bonus -= 0.5

        // Landwalk (situational but free wins)
        if (Keyword.SWAMPWALK in keywords) bonus += 0.2
        if (Keyword.FORESTWALK in keywords) bonus += 0.2
        if (Keyword.ISLANDWALK in keywords) bonus += 0.2
        if (Keyword.MOUNTAINWALK in keywords) bonus += 0.2
        if (Keyword.PLAINSWALK in keywords) bonus += 0.2

        return bonus
    }

    /**
     * Bonus for spell/ability effects. Walks the effect tree to detect
     * removal, card draw, tokens, and other high-value effects.
     */
    private fun effectBonus(card: CardDefinition): Double {
        var bonus = 0.0

        // Check spell effect
        card.script.spellEffect?.let { bonus += scoreEffect(it) }

        // Check triggered abilities (ETB, death triggers, etc.)
        for (ability in card.script.triggeredAbilities) {
            bonus += scoreEffect(ability.effect) * 0.8 // slightly less than spell since conditional
        }

        // Check activated abilities
        for (ability in card.script.activatedAbilities) {
            if (!ability.isManaAbility) {
                bonus += scoreEffect(ability.effect) * 0.6 // repeatable but costs resources
            }
        }

        return bonus
    }

    /**
     * Walk an effect tree and score it for limited value.
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
            is ExileUntilLeavesEffect -> 1.4               // O-Ring removal
            is ForceSacrificeEffect -> 1.2                  // edict
            is PutOnTopOrBottomOfLibraryEffect -> 1.0       // tuck

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

            // Keyword grants
            is GrantKeywordEffect -> 0.2

            // Control effects — premium
            is GainControlEffect -> 1.5

            // Counterspells
            is CounterEffect -> 0.8

            // Composite — sum children
            is CompositeEffect -> effect.effects.sumOf { scoreEffect(it) }.coerceAtMost(2.5)

            // Conditional / optional — discount
            is MayEffect -> scoreEffect(effect.effect) * 0.8
            is ConditionalEffect -> {
                val thenScore = scoreEffect(effect.effect)
                val elseScore = effect.elseEffect?.let { scoreEffect(it) } ?: 0.0
                (thenScore + elseScore) * 0.6
            }

            // Everything else
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
