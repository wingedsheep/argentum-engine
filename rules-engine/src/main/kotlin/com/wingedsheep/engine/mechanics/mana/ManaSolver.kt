package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorLandsCouldProduceEffect
import com.wingedsheep.sdk.scripting.effects.LandControllerScope
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
import com.wingedsheep.engine.state.components.identity.ChosenCreatureTypeComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Represents the mana production capability of a source.
 */
data class ManaSource(
    val entityId: EntityId,
    val name: String,
    /** Colors this source can produce (empty means colorless-only) */
    val producesColors: Set<Color>,
    /** Whether this source can produce colorless mana */
    val producesColorless: Boolean = false,
    /** Whether this is a basic land (Plains, Island, Swamp, Mountain, Forest) */
    val isBasicLand: Boolean = false,
    /** Whether this source is a land (any land type) */
    val isLand: Boolean = false,
    /** Whether this source is a creature */
    val isCreature: Boolean = false,
    /** Whether this source has non-mana activated abilities (utility land/creature) */
    val hasNonManaAbilities: Boolean = false,
    /** Whether tapping this source costs life (pain land) */
    val hasPainCost: Boolean = false,
    /** Amount of life paid when tapping (for pain lands) */
    val painAmount: Int = 0,
    /** Whether this creature can attack (no summoning sickness or has haste) */
    val canAttack: Boolean = false,
    /** Amount of mana this source produces per tap (e.g., 3 for Elvish Aberration) */
    val manaAmount: Int = 1,
    /** Extra mana produced per tap from auras like Elvish Guidance */
    val bonusManaPerTap: Int = 0,
    /** Color of the bonus mana */
    val bonusManaColor: Color? = null,
    /** Mana spending restriction (e.g., "only for instant/sorcery"). Null = unrestricted. */
    val restriction: ManaRestriction? = null,
    /**
     * Per-color spell riders attached to mana this source produces (e.g. Cavern of
     * Souls' colored mana carries [ManaSpellRider.MakesSpellUncounterable]). Tracked
     * per-color so a fused source with mixed rider/non-rider abilities (e.g. an
     * uncounterable colored ability alongside a plain colorless ability) attributes
     * riders only to the color the rider-bearing ability actually produces.
     */
    val colorRiders: Map<Color, Set<ManaSpellRider>> = emptyMap(),
    /** Per-color mana restrictions. Colors not in this map are unrestricted. */
    val colorRestrictions: Map<Color, ManaRestriction> = emptyMap(),
    /**
     * True when this source has multiple mana abilities with mutually-different
     * restrictions (e.g. Steelswarm Operator's two abilities), so the cached aggregate
     * collapses to "unrestricted" and is only correct without a spell/ability payment
     * context. The solver re-runs [findAvailableManaSources] with the context when any
     * cached source carries this flag.
     */
    val hasContextSensitiveAbilities: Boolean = false,
    /**
     * Additional mana cost required (beyond tapping) to produce each color.
     * Entries reflect the *cheapest* ability producing that color on this permanent.
     * For filter lands like Hidden Grotto ({1}, {T}: Add one mana of any color),
     * every color maps to 1 because the only ability producing colors costs {1}.
     * Colors not present in this map can be produced for free (or not at all).
     */
    val colorActivationManaCost: Map<Color, Int> = emptyMap(),
    /**
     * Tapping this source also requires sacrificing it (e.g. Treasure tokens —
     * "{T}, Sacrifice this artifact: Add one mana of any color"). The auto-pay
     * solver (`solve()`) refuses to pick these because silently sacrificing a
     * permanent would surprise the player; manual mana-source selection menus
     * may offer them so the choice is explicit.
     */
    val requiresSacrifice: Boolean = false,
    /**
     * Tapping this source also requires tapping another permanent (e.g. Springleaf
     * Drum — "{T}, Tap an untapped creature you control: Add one mana of any color").
     * Auto-pay refuses to pick these because silently tapping someone else's permanent
     * choice would surprise the player; manual mana-source selection menus offer the
     * source and the resumer prompts for the secondary tap target. Null when no such
     * sub-cost is present.
     */
    val tapPermanentsSubCost: TapPermanentsSubCost? = null
) {
    /**
     * Returns the set of colors this source can produce for a given spell context.
     * Filters out colors whose restriction is not satisfied.
     */
    fun availableColorsFor(spellContext: SpellPaymentContext?): Set<Color> {
        if (colorRestrictions.isEmpty() || spellContext == null) return producesColors
        return producesColors.filter { color ->
            val restriction = colorRestrictions[color]
            restriction == null || restriction.isSatisfiedBy(spellContext)
        }.toSet()
    }
}

/**
 * Secondary tap-permanents sub-cost attached to a tap-based mana ability
 * (e.g. Springleaf Drum's "Tap an untapped creature you control").
 *
 * Mirrors [com.wingedsheep.sdk.scripting.AbilityCost.TapPermanents] but lives on
 * [ManaSource] so consumers don't need to re-resolve the ability's cost shape.
 */
data class TapPermanentsSubCost(
    val count: Int,
    val filter: GameObjectFilter,
    val excludeSelf: Boolean
)

/**
 * Result of solving mana payment.
 *
 * @property sources The mana sources to tap to pay the cost
 * @property manaProduced Map of each source to the mana it will produce for this payment
 */
data class ManaSolution(
    val sources: List<ManaSource>,
    val manaProduced: Map<EntityId, ManaProduction>,
    /**
     * Bonus mana remaining after the solver consumed some to pay the cost. Entries that
     * came from a restricted mana ability retain the restriction so callers can
     * preserve it when adding the leftover to the player's pool — losing the
     * restriction would let an artifact-only or creature-spell-only mana be spent
     * arbitrarily on the next action.
     */
    val remainingBonusMana: List<BonusManaEntry> = emptyList(),
    /**
     * Spell riders consumed by this solution — the union of riders attached to the
     * specific (source, color) slots actually tapped (e.g. Cavern of Souls' colored
     * ability contributes [ManaSpellRider.MakesSpellUncounterable] when tapped for
     * a color, but its colorless `{T}: Add {C}` ability does not).
     */
    val consumedRiders: Set<ManaSpellRider> = emptySet()
)

/**
 * A unit of bonus mana that wasn't consumed by the current solve. Carries the source's
 * mana restriction (when any) so the caller can route it back into the floating pool
 * via [ManaPool.addRestricted] instead of [ManaPool.add].
 */
data class BonusManaEntry(
    val color: Color,
    val amount: Int = 1,
    val restriction: ManaRestriction? = null,
)

/**
 * The mana a single source produces for a payment.
 */
data class ManaProduction(
    val color: Color? = null,
    val amount: Int = 1,
    val colorless: Int = 0
)

/**
 * Solves mana payment by finding which lands/sources to tap for AutoPay.
 *
 * The solver uses a greedy algorithm:
 * 1. Pay colored costs first, using sources that ONLY produce that color when possible
 * 2. Pay colorless costs with sources that only produce colorless
 * 3. Pay generic costs with any remaining sources
 *
 * This heuristic preserves flexibility by saving multi-color lands for later.
 *
 * @param cardRegistry Optional registry to look up card definitions for mana abilities.
 *                     When provided, non-land permanents with mana abilities can be used as sources.
 */
class ManaSolver(
    private val cardRegistry: CardRegistry,
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) {

    private val predicateEvaluator = PredicateEvaluator()
    private val conditionEvaluator = ConditionEvaluator()

    /**
     * Finds a valid set of mana sources to pay the cost.
     *
     * @param state The current game state
     * @param playerId The player who needs to pay
     * @param cost The mana cost to pay
     * @param xValue The value of X (for X-cost spells)
     * @return A solution describing which sources to tap, or null if the cost cannot be paid
     */
    fun solve(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        xValue: Int = 0,
        excludeSources: Set<EntityId> = emptySet(),
        spellContext: SpellPaymentContext? = null,
        precomputedSources: List<ManaSource>? = null
    ): ManaSolution? {
        // Get all untapped mana sources controlled by the player.
        //
        // The cached `precomputedSources` is built without a payment context, so for
        // sources that have multiple mana abilities with mismatched restrictions
        // (e.g. Steelswarm Operator) the cached aggregate collapses to "unrestricted"
        // and over-states what the source can produce for a specific spend. When such
        // a source is present and a context is provided, re-run findAvailableManaSources
        // with the context to filter abilities accurately. Otherwise reuse the cache.
        val cachedSources = precomputedSources
        val needsContextRebuild = spellContext != null && (
            cachedSources == null ||
                cachedSources.any { it.hasContextSensitiveAbilities }
            )
        val rawSources = if (needsContextRebuild) {
            findAvailableManaSources(state, playerId, spellContext)
        } else {
            cachedSources ?: findAvailableManaSources(state, playerId)
        }
        val availableSources = rawSources
            .filter { it.entityId !in excludeSources }
            // Auto-pay must not silently sacrifice permanents (e.g. Treasure tokens).
            // The bonus-mana accounting in canPay() still counts these via
            // calculateSacrificeSelfBonusMana(), but the solver itself never picks them.
            .filter { !it.requiresSacrifice }
            // Same rule for composite Tap+TapPermanents sources (Springleaf Drum) — the
            // resumer must prompt the player to pick which creature gets tapped, so the
            // auto-pay solver refuses to silently consume the choice. canPay() accounts
            // for these via calculateCompositeTapPermanentsBonusMana().
            .filter { it.tapPermanentsSubCost == null }
            .filter { source ->
                if (source.restriction == null || spellContext == null) true
                else source.restriction.isSatisfiedBy(spellContext)
            }

        if (availableSources.isEmpty() && cost.cmc > 0) {
            return null
        }

        // Analyze hand to inform smart tapping decisions
        val handRequirements = analyzeHandRequirements(state, playerId)

        // Count available sources per color for hand-awareness (respecting per-color restrictions)
        val availableSourcesByColor = mutableMapOf<Color, Int>()
        for (color in Color.entries) {
            availableSourcesByColor[color] = availableSources.count { it.availableColorsFor(spellContext).contains(color) }
        }

        // Track which sources we've used
        val usedSources = mutableListOf<ManaSource>()
        val manaProduced = mutableMapOf<EntityId, ManaProduction>()
        var remainingSources = availableSources.toMutableList()

        // Track bonus mana from auras and excess mana from multi-mana sources. The list
        // preserves the originating restriction (if any) per entry so unconsumed bonus
        // mana retains its restriction when it lands in the player's pool.
        val bonusManaPool = mutableListOf<BonusManaEntry>()

        // Helper to update available counts when a source is used
        fun useSource(source: ManaSource, colorUsed: Color?) {
            usedSources.add(source)
            remainingSources.remove(source)
            for (color in source.producesColors) {
                availableSourcesByColor[color] = (availableSourcesByColor[color] ?: 1) - 1
            }
            // Track excess mana from multi-mana sources (e.g., Elvish Aberration produces 3 green).
            // Inherit the source's restriction for that color so leftover restricted mana
            // remains restricted in the pool.
            if (source.manaAmount > 1 && colorUsed != null) {
                val restrictionForExcess = source.colorRestrictions[colorUsed] ?: source.restriction
                bonusManaPool.add(BonusManaEntry(colorUsed, source.manaAmount - 1, restrictionForExcess))
            }
            // Collect bonus mana from auras attached to this source (no restriction —
            // the aura grants extra mana on top of the source's printed ability).
            if (source.bonusManaPerTap > 0 && source.bonusManaColor != null) {
                bonusManaPool.add(BonusManaEntry(source.bonusManaColor, source.bonusManaPerTap, null))
            }
        }

        // Helper to spend one bonus mana of a specific color for a colored cost. The
        // restriction was already checked when the source was admitted into
        // `availableSources`, so any matching-color entry is eligible for this payment.
        // Consumption is FIFO over `bonusManaPool` (insertion order = tap order); for the
        // current solve any order is correct, and the choice affects only which
        // restrictions land back in [ManaSolution.remainingBonusMana] for the caller.
        fun spendBonusMana(color: Color): Boolean {
            val idx = bonusManaPool.indexOfFirst { it.color == color && it.amount > 0 }
            if (idx < 0) return false
            val entry = bonusManaPool[idx]
            bonusManaPool[idx] = entry.copy(amount = entry.amount - 1)
            return true
        }

        // Helper to spend one bonus mana of any color for a generic cost. Same FIFO
        // policy as [spendBonusMana].
        fun spendAnyBonusMana(): Boolean {
            val idx = bonusManaPool.indexOfFirst { it.amount > 0 }
            if (idx < 0) return false
            val entry = bonusManaPool[idx]
            bonusManaPool[idx] = entry.copy(amount = entry.amount - 1)
            return true
        }

        // 1. Pay colored costs first (most constrained)
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    // Try bonus mana first
                    if (spendBonusMana(symbol.color)) continue

                    val source = findBestSourceForColor(remainingSources, symbol.color, handRequirements, availableSourcesByColor, spellContext)
                        ?: return null // Can't pay this colored cost

                    manaProduced[source.entityId] = ManaProduction(color = symbol.color, amount = source.manaAmount)
                    useSource(source, symbol.color)

                    // Check if the bonus mana from this source can pay remaining colored costs
                    // (handled naturally on next iteration via spendBonusMana)
                }
                is ManaSymbol.Hybrid -> {
                    // Try bonus mana first
                    if (spendBonusMana(symbol.color1)) continue
                    if (spendBonusMana(symbol.color2)) continue

                    // Try first color, then second - use priority to pick the best
                    val source1 = findBestSourceForColor(remainingSources, symbol.color1, handRequirements, availableSourcesByColor, spellContext)
                    val source2 = findBestSourceForColor(remainingSources, symbol.color2, handRequirements, availableSourcesByColor, spellContext)

                    val source = when {
                        source1 == null && source2 == null -> return null
                        source1 == null -> source2!!
                        source2 == null -> source1
                        else -> {
                            // Pick the source with lower priority (tap it first)
                            val priority1 = calculateTapPriority(source1, handRequirements, availableSourcesByColor)
                            val priority2 = calculateTapPriority(source2, handRequirements, availableSourcesByColor)
                            if (priority1 <= priority2) source1 else source2
                        }
                    }

                    val availableColors = source.availableColorsFor(spellContext)
                    val colorUsed = if (availableColors.contains(symbol.color1))
                        symbol.color1 else symbol.color2
                    manaProduced[source.entityId] = ManaProduction(color = colorUsed, amount = source.manaAmount)
                    useSource(source, colorUsed)
                }
                is ManaSymbol.Phyrexian -> {
                    // Try bonus mana first
                    if (spendBonusMana(symbol.color)) continue

                    // For now, always pay with mana (not life)
                    val source = findBestSourceForColor(remainingSources, symbol.color, handRequirements, availableSourcesByColor, spellContext)
                        ?: return null

                    manaProduced[source.entityId] = ManaProduction(color = symbol.color, amount = source.manaAmount)
                    useSource(source, symbol.color)
                }
                is ManaSymbol.Colorless -> {
                    // Must pay with actual colorless mana (from Wastes, etc.)
                    // Sort colorless sources by priority
                    val source = remainingSources
                        .filter { it.producesColorless }
                        .minByOrNull { calculateTapPriority(it, handRequirements, availableSourcesByColor) }
                        ?: return null

                    manaProduced[source.entityId] = ManaProduction(colorless = source.manaAmount)
                    useSource(source, null)
                }
                is ManaSymbol.Generic, is ManaSymbol.X -> {
                    // Handle in the generic pass below
                }
            }
        }

        // 1b. Pay the internal mana cost of any ability we committed to activate above
        //     (e.g., Hidden Grotto's "{1}, {T}: Add one mana of any color" — producing
        //     the colored mana requires {1} from another source). These extra sources
        //     are tapped but their production is NOT added to manaProduced — it is
        //     consumed by the ability's activation cost rather than flowing into the
        //     spell's payment pool. Excess mana from multi-mana sources does still
        //     flow to the bonus pool and remains available for the generic pass.
        var activationCostRemaining = 0
        for (used in usedSources) {
            val produced = manaProduced[used.entityId] ?: continue
            val color = produced.color ?: continue
            activationCostRemaining += used.colorActivationManaCost[color] ?: 0
        }
        while (activationCostRemaining > 0) {
            if (spendAnyBonusMana()) {
                activationCostRemaining--
                continue
            }
            if (remainingSources.isEmpty()) return null
            val source = remainingSources.minByOrNull {
                calculateTapPriority(it, handRequirements, availableSourcesByColor)
            } ?: return null
            // Tap for activation cost; attribute any excess to bonus pool.
            val excessColor = source.producesColors.firstOrNull()
            useSource(source, excessColor)
            activationCostRemaining--
        }

        // 2. Pay generic costs (and X), using bonus mana first
        // xValue here is the total extra generic mana needed for X (callers handle XX multiplication)
        var genericRemaining = cost.genericAmount + xValue

        while (genericRemaining > 0) {
            // Try to spend bonus mana first
            if (spendAnyBonusMana()) {
                genericRemaining--
                continue
            }

            if (remainingSources.isEmpty()) {
                return null // Not enough mana
            }

            // Check if single-mana sources alone can cover the remaining generic cost.
            // If not, prefer multi-mana sources for efficiency (fewer taps overall).
            val singleManaCount = remainingSources.count { it.manaAmount == 1 }
            val needMultiMana = singleManaCount < genericRemaining

            val source = if (needMultiMana) {
                // Not enough single-mana sources — prefer multi-mana for efficiency
                remainingSources.minByOrNull { source ->
                    val basePriority = calculateTapPriority(source, handRequirements, availableSourcesByColor)
                    val savedTaps = minOf(source.manaAmount, genericRemaining) - 1
                    basePriority - savedTaps * 25
                }
            } else {
                // Enough single-mana sources — use normal priority (preserve multi-mana creatures for attacks)
                remainingSources.minByOrNull { calculateTapPriority(it, handRequirements, availableSourcesByColor) }
            } ?: return null

            // For generic costs, prefer unrestricted colors, then colorless
            val colorToUse = source.availableColorsFor(spellContext).firstOrNull()
                ?: source.producesColors.firstOrNull()
            manaProduced[source.entityId] = if (colorToUse != null) {
                ManaProduction(color = colorToUse, amount = source.manaAmount)
            } else {
                ManaProduction(colorless = source.manaAmount)
            }
            useSource(source, colorToUse)
            genericRemaining--
        }

        val consumedRiders: Set<ManaSpellRider> = usedSources.flatMapTo(mutableSetOf()) { source ->
            val color = manaProduced[source.entityId]?.color ?: return@flatMapTo emptySet()
            source.colorRiders[color] ?: emptySet()
        }
        return ManaSolution(usedSources, manaProduced, bonusManaPool.filter { it.amount > 0 }, consumedRiders)
    }

    /**
     * Calculates the tap priority for a mana source (lower = tap first).
     *
     * Priority order (tap first to last):
     * 1. Basic lands (priority ~0-1)
     * 2. Non-basic single-color lands without abilities (~2)
     * 3. Dual/tri-lands without abilities (~3-5)
     * 4. Utility lands with non-mana abilities (~10-14)
     * 5. Pain lands (~16+)
     * 6. Mana creatures that can attack (~20+)
     * 7. Five-color lands (~25+)
     */
    private fun calculateTapPriority(
        source: ManaSource,
        handRequirements: Map<Color, Int>,
        availableSourcesByColor: Map<Color, Int>
    ): Int {
        var priority = 0

        // Base: color flexibility (0-10 based on color count)
        priority += when (source.producesColors.size) {
            0 -> 1      // Colorless-only
            1 -> 0      // Single color - tap first
            2 -> 3      // Dual land
            3 -> 4      // Tri-land
            4 -> 5      // Four-color
            else -> 10  // Five-color - tap last
        }

        // Prefer basics (+2 penalty for non-basics)
        if (!source.isBasicLand && source.producesColors.isNotEmpty()) {
            priority += 2
        }

        // Preserve utility lands (+10 for non-mana abilities)
        if (source.hasNonManaAbilities) {
            priority += 10
        }

        // Avoid pain lands (+15 + pain amount)
        if (source.hasPainCost) {
            priority += 15 + source.painAmount
        }

        // Preserve attackers (+20 for creatures that can attack)
        if (source.isCreature && source.canAttack) {
            priority += 20
        }

        // Hand awareness: penalize tapping sources for colors we need in hand
        // but have limited supply of
        for (color in source.producesColors) {
            val required = handRequirements[color] ?: 0
            val available = availableSourcesByColor[color] ?: 0

            // If tapping this would leave us short for hand cards, add penalty
            if (required > 0 && available <= required) {
                priority += 5  // Medium penalty - still allow if necessary
            }
        }

        return priority
    }

    /**
     * Analyzes cards in hand and returns required color counts.
     * Returns map of Color -> minimum sources needed to cast the most color-demanding card of that color.
     */
    private fun analyzeHandRequirements(state: GameState, playerId: EntityId): Map<Color, Int> {
        val handZone = ZoneKey(playerId, Zone.HAND)
        val handCards = state.getZone(handZone)

        val colorRequirements = mutableMapOf<Color, Int>()

        for (entityId in handCards) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            // Get mana cost
            val manaCost = cardDef.manaCost

            // Count colored symbols per color in this card's cost
            val cardColorCounts = mutableMapOf<Color, Int>()
            for (symbol in manaCost.symbols) {
                when (symbol) {
                    is ManaSymbol.Colored -> {
                        cardColorCounts[symbol.color] = (cardColorCounts[symbol.color] ?: 0) + 1
                    }
                    // Hybrid symbols don't strictly require either color
                    else -> {}
                }
            }

            // Update max requirements per color
            for ((color, count) in cardColorCounts) {
                val current = colorRequirements[color] ?: 0
                colorRequirements[color] = maxOf(current, count)
            }
        }

        return colorRequirements
    }

    /**
     * Finds all untapped mana sources controlled by a player.
     * Supports:
     * - Basic lands and lands with basic land subtypes
     * - Non-land permanents with explicit tap mana abilities (mana dorks, mana rocks)
     * - Respects summoning sickness for creatures (unless they have haste)
     *
     * Populates smart-tapping metadata for each source:
     * - isBasicLand: true for basic land types
     * - isCreature: true for creatures
     * - hasNonManaAbilities: true if the source has activated abilities that aren't mana abilities
     * - hasPainCost/painAmount: true if the mana ability costs life
     * - canAttack: true for creatures that can attack (no summoning sickness or has haste)
     */
    fun findAvailableManaSources(
        state: GameState,
        playerId: EntityId,
        spellContext: SpellPaymentContext? = null,
    ): List<ManaSource> {
        // Project state once to get all keywords and projected controllers
        val projected = state.projectedState

        // Use projected controller to find all permanents controlled by this player
        // (accounts for control-changing effects like Annex)
        val battlefieldCards = projected.getBattlefieldControlledBy(playerId)

        return battlefieldCards.mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null

            // Must be untapped
            if (container.has<TappedComponent>()) return@mapNotNull null

            val card = container.get<CardComponent>() ?: return@mapNotNull null

            // Check for explicit mana abilities via CardRegistry
            val cardDef = cardRegistry.getCard(card.cardDefinitionId)
            // Suppress the card's own activated abilities when projection has stripped them
            // (e.g., Noggle the Mind / Humility / Deep Freeze). Granted abilities are kept.
            val allAbilities = if (cardDef == null || projected.hasLostAllAbilities(entityId)) emptyList()
                else cardDef.script.activatedAbilities

            // Include mana abilities granted by static effects from other permanents
            // (e.g., Clement, the Worrywort granting {T}: Add {G} or {U} to Frogs)
            val staticGrantedManaAbilities = getStaticGrantedManaAbilities(entityId, state)
            val rawManaAbilities = allAbilities.filter { it.isManaAbility } + staticGrantedManaAbilities

            // When a spell/ability payment context is provided, drop mana abilities whose
            // restriction is incompatible. Otherwise the combiner below would treat a
            // source with two mutually-exclusive restricted abilities (e.g. Steelswarm
            // Operator's "spells only" + "abilities only" variants of
            // CardTypeSpellsOrAbilitiesOnly) as unrestricted and over-produce mana for the
            // actual spend.
            val manaAbilities = if (spellContext != null) {
                rawManaAbilities.filter { ability ->
                    val r = extractManaRestriction(ability.effect)
                    r == null || r.isSatisfiedBy(spellContext)
                }
            } else {
                rawManaAbilities
            }

            // Detect non-mana activated abilities (utility land/creature)
            val hasNonManaAbilities = allAbilities.any { !it.isManaAbility }

            // Creature and attack capability detection
            val isCreature = card.typeLine.isCreature
            val hasSummoningSickness = container.has<SummoningSicknessComponent>()
            val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
            val canAttack = isCreature && (!hasSummoningSickness || hasHaste)

            // Basic land detection
            val isBasicLand = card.typeLine.isBasicLand

            // For lands: check projected basic land subtypes first (Rule 305.7)
            // Basic land types grant intrinsic mana abilities, and type-changing effects
            // like Sea's Claim change what mana a land produces
            if (card.typeLine.isLand) {
                val projectedSubtypes = projected.getSubtypes(entityId)
                val subtypeColors = mutableSetOf<Color>()
                if (projectedSubtypes.contains("Plains")) subtypeColors.add(Color.WHITE)
                if (projectedSubtypes.contains("Island")) subtypeColors.add(Color.BLUE)
                if (projectedSubtypes.contains("Swamp")) subtypeColors.add(Color.BLACK)
                if (projectedSubtypes.contains("Mountain")) subtypeColors.add(Color.RED)
                if (projectedSubtypes.contains("Forest")) subtypeColors.add(Color.GREEN)

                if (subtypeColors.isNotEmpty()) {
                    // An attached aura may override the produced mana color
                    // (e.g., Shimmerwilds Growth on a Mountain with Blue chosen → produces {U}).
                    val overrideColor = findEnchantedLandManaColorOverride(state, entityId)
                    val effectiveColors = if (overrideColor != null) setOf(overrideColor) else subtypeColors
                    return@mapNotNull ManaSource(
                        entityId = entityId,
                        name = card.name,
                        producesColors = effectiveColors,
                        producesColorless = false,
                        isBasicLand = isBasicLand,
                        isLand = true,
                        isCreature = isCreature,
                        hasNonManaAbilities = hasNonManaAbilities,
                        hasPainCost = false,
                        painAmount = 0,
                        canAttack = canAttack
                    )
                }
            }

            // Collect all tap-based mana abilities to build a combined ManaSource
            val combinedColors = mutableSetOf<Color>()
            var producesColorless = false
            var maxManaAmount = 1
            var anyAbilityHasNoPainCost = false
            var minPainAmount = Int.MAX_VALUE
            // Track which accepted abilities required sacrificing the source (e.g. Treasure).
            // The source is marked `requiresSacrifice` only when every accepted mana ability
            // requires sacrifice — if any accepted ability is non-sac, prefer that path.
            var anyAcceptedWithSac = false
            var anyAcceptedWithoutSac = false
            // Mirror of anyAcceptedWith[out]Sac for composite tap+TapPermanents abilities
            // (Springleaf Drum). The source surfaces a non-null `tapPermanentsSubCost` only
            // when every accepted mana ability requires the secondary tap.
            var anyAcceptedWithTapPermanents = false
            var anyAcceptedWithoutTapPermanents = false
            var firstAcceptedTapPermanentsSubCost: TapPermanentsSubCost? = null
            // Track restrictions: if any ability is unrestricted, the source is unrestricted
            var hasUnrestrictedAbility = false
            var commonRestriction: ManaRestriction? = null
            var firstRestrictionSeen = false
            // Set when two abilities on this source have different non-null restrictions —
            // the cached aggregate then mis-represents what the source can produce for a
            // specific context, and the solver re-runs us with a context to disambiguate.
            var hasMixedRestrictions = false
            // Track per-color restrictions (for sources with mixed restricted/unrestricted abilities)
            val perColorRestrictions = mutableMapOf<Color, ManaRestriction?>()
            // Track the minimum mana-cost-to-activate per color (cheapest ability producing it)
            val perColorActivationCost = mutableMapOf<Color, Int>()
            // Spell riders contributed per color by abilities on this source (e.g.
            // Cavern of Souls' "Add one mana of any color" carries
            // MakesSpellUncounterable on every color it can produce, while its
            // plain `{T}: Add {C}` ability contributes nothing).
            val perColorRiders = mutableMapOf<Color, MutableSet<ManaSpellRider>>()

            for (ability in manaAbilities) {
                // Skip abilities whose activation restrictions aren't satisfied
                // (e.g., Lys Alana Dignitary's "only if there is an Elf card in your graveyard").
                if (!activationRestrictionsSatisfied(state, playerId, entityId, ability)) {
                    continue
                }

                // Detect pain cost and mana activation cost in mana abilities
                var abilityHasPainCost = false
                var abilityPainAmount = 0
                var abilityActivationManaCost = 0
                var abilityRequiresSacrifice = false
                var abilityTapPermanentsSubCost: TapPermanentsSubCost? = null
                val abilityCanBeUsed = when (val cost = ability.cost) {
                    is AbilityCost.Tap -> true
                    is AbilityCost.PayLife -> {
                        abilityHasPainCost = true
                        abilityPainAmount = cost.amount
                        true
                    }
                    is AbilityCost.Composite -> {
                        var hasTap = false
                        var hasUnsupportedSubCost = false
                        for (subCost in cost.costs) {
                            when (subCost) {
                                is AbilityCost.Tap -> hasTap = true
                                is AbilityCost.PayLife -> {
                                    abilityHasPainCost = true
                                    abilityPainAmount = maxOf(abilityPainAmount, subCost.amount)
                                }
                                is AbilityCost.Mana -> {
                                    abilityActivationManaCost += subCost.cost.cmc
                                }
                                // SacrificeSelf (Treasure: "{T}, Sacrifice this artifact: Add …").
                                // Auto-tap won't pick these (filtered in solve()), but they appear
                                // in `findAvailableManaSources` so manual-selection UIs can offer
                                // them; selecting one triggers an explicit sacrifice in the resumer.
                                is AbilityCost.SacrificeSelf -> abilityRequiresSacrifice = true
                                // TapPermanents as a sub-cost (Springleaf Drum:
                                // "{T}, Tap an untapped creature you control: Add …"). Same
                                // treatment as SacrificeSelf — auto-pay refuses to silently
                                // consume the secondary tap target; manual menus offer the
                                // source and the resumer prompts for the creature.
                                is AbilityCost.TapPermanents -> {
                                    abilityTapPermanentsSubCost = TapPermanentsSubCost(
                                        count = subCost.count,
                                        filter = subCost.filter,
                                        excludeSelf = subCost.excludeSelf
                                    )
                                }
                                // Other choice costs (Forage, sacrifice-something-else, etc.) still
                                // require explicit ActivateAbility entry.
                                else -> hasUnsupportedSubCost = true
                            }
                        }
                        val tapPermSubCost = abilityTapPermanentsSubCost
                        if (hasTap && !hasUnsupportedSubCost && tapPermSubCost != null) {
                            // Verify enough untapped non-source permanents are available to satisfy
                            // the secondary tap. If not, this ability is not usable right now.
                            if (!hasEnoughTapTargets(state, playerId, entityId, tapPermSubCost)) {
                                abilityTapPermanentsSubCost = null
                                false
                            } else {
                                true
                            }
                        } else {
                            hasTap && !hasUnsupportedSubCost
                        }
                    }
                    else -> false // Skip non-tap mana abilities
                }

                if (!abilityCanBeUsed) continue

                if (abilityRequiresSacrifice) anyAcceptedWithSac = true else anyAcceptedWithoutSac = true
                if (abilityTapPermanentsSubCost != null) {
                    anyAcceptedWithTapPermanents = true
                    if (firstAcceptedTapPermanentsSubCost == null) {
                        firstAcceptedTapPermanentsSubCost = abilityTapPermanentsSubCost
                    }
                } else {
                    anyAcceptedWithoutTapPermanents = true
                }

                // Check summoning sickness for creatures (non-lands)
                if (!card.typeLine.isLand && isCreature) {
                    if (hasSummoningSickness && !hasHaste) {
                        continue // Can't use this ability due to summoning sickness
                    }
                }

                if (!abilityHasPainCost) anyAbilityHasNoPainCost = true
                if (abilityHasPainCost) minPainAmount = minOf(minPainAmount, abilityPainAmount)

                // Accumulate production from effect.
                // Note: maxManaAmount tracks the GROSS mana produced per tap (not net of
                // activation cost). The solver accounts for ability activation mana costs
                // separately via colorActivationManaCost / colorlessActivationManaCost,
                // tapping additional sources to cover them.
                val effectColors = mutableSetOf<Color>()
                val manaEffect = when (val effect = ability.effect) {
                    is CompositeEffect -> effect.effects.firstOrNull {
                        it is AddManaEffect ||
                            it is AddColorlessManaEffect ||
                            it is AddAnyColorManaEffect ||
                            it is AddAnyColorManaSpendOnChosenTypeEffect ||
                            it is AddManaOfColorAmongEffect ||
                            it is AddManaOfColorLandsCouldProduceEffect ||
                            it is AddManaOfChosenColorEffect ||
                            it is AddDynamicManaEffect
                    } ?: effect
                    else -> effect
                }
                val effectRestriction: ManaRestriction? = when (val effect = manaEffect) {
                    is AddManaEffect -> {
                        combinedColors.add(effect.color)
                        effectColors.add(effect.color)
                        val manaAmount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        maxManaAmount = maxOf(maxManaAmount, manaAmount)
                        effect.restriction
                    }
                    is AddColorlessManaEffect -> {
                        producesColorless = true
                        val manaAmount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        maxManaAmount = maxOf(maxManaAmount, manaAmount)
                        effect.restriction
                    }
                    is AddAnyColorManaEffect -> {
                        combinedColors.addAll(Color.entries)
                        effectColors.addAll(Color.entries)
                        val manaAmount = evaluateManaAmount(effect.amount, state, entityId, playerId)
                        maxManaAmount = maxOf(maxManaAmount, manaAmount)
                        effect.restriction
                    }
                    is AddAnyColorManaSpendOnChosenTypeEffect -> {
                        val chosenType = state.getEntity(entityId)
                            ?.get<ChosenCreatureTypeComponent>()?.creatureType
                        if (chosenType != null) {
                            combinedColors.addAll(Color.entries)
                            effectColors.addAll(Color.entries)
                            val manaAmount = evaluateManaAmount(effect.amount, state, entityId, playerId)
                            maxManaAmount = maxOf(maxManaAmount, manaAmount)
                            if (effect.riders.isNotEmpty()) {
                                for (color in Color.entries) {
                                    perColorRiders.getOrPut(color) { mutableSetOf() }.addAll(effect.riders)
                                }
                            }
                            ManaRestriction.SubtypeSpellsOrAbilitiesOnly(chosenType, effect.creatureOnly)
                        } else null
                    }
                    is AddManaOfColorAmongEffect -> {
                        // Determine available colors from matching permanents
                        val projected = state.projectedState
                        val predCtx = PredicateContext(controllerId = playerId)
                        for (bfId in state.getBattlefield()) {
                            if (predicateEvaluator.matchesWithProjection(state, projected, bfId, effect.filter, predCtx)) {
                                val colors = projected.getColors(bfId)
                                for (colorName in colors) {
                                    Color.entries.find { it.name == colorName }?.let {
                                        combinedColors.add(it)
                                        effectColors.add(it)
                                    }
                                }
                            }
                        }
                        if (combinedColors.isNotEmpty()) {
                            maxManaAmount = maxOf(maxManaAmount, 1)
                        }
                        effect.restriction
                    }
                    is AddManaOfColorLandsCouldProduceEffect -> {
                        val projected = state.projectedState
                        val targetPlayers: Set<EntityId> = when (effect.scope) {
                            LandControllerScope.YOU -> setOf(playerId)
                            LandControllerScope.OPPONENTS -> state.turnOrder.filter { it != playerId }.toSet()
                            LandControllerScope.ANY -> state.turnOrder.toSet()
                        }
                        val landIds = state.getBattlefield().filter { permId ->
                            val c = state.getEntity(permId) ?: return@filter false
                            val cc = c.get<CardComponent>() ?: return@filter false
                            cc.typeLine.isLand && projected.getController(permId) in targetPlayers
                        }
                        val producible = LandManaColorInspector
                            .colorsLandsCouldProduce(state, projected, landIds, cardRegistry)
                        combinedColors.addAll(producible)
                        effectColors.addAll(producible)
                        if (producible.isNotEmpty()) {
                            maxManaAmount = maxOf(maxManaAmount, 1)
                        }
                        effect.restriction
                    }
                    is AddManaOfChosenColorEffect -> {
                        val chosenColor = state.getEntity(entityId)
                            ?.get<ChosenColorComponent>()?.color
                        if (chosenColor != null) {
                            combinedColors.add(chosenColor)
                            effectColors.add(chosenColor)
                            val manaAmount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                            maxManaAmount = maxOf(maxManaAmount, manaAmount)
                        }
                        effect.restriction
                    }
                    is AddDynamicManaEffect -> {
                        combinedColors.addAll(effect.allowedColors)
                        effectColors.addAll(effect.allowedColors)
                        val manaAmount = (effect.amountSource as? DynamicAmount.Fixed)?.amount ?: 1
                        maxManaAmount = maxOf(maxManaAmount, manaAmount)
                        effect.restriction
                    }
                    else -> null
                }

                // Record the cheapest activation mana cost per color this ability produces.
                for (color in effectColors) {
                    val existing = perColorActivationCost[color]
                    perColorActivationCost[color] = if (existing == null) abilityActivationManaCost
                    else minOf(existing, abilityActivationManaCost)
                }

                // Track per-color restrictions: null means unrestricted
                for (color in effectColors) {
                    val existing = perColorRestrictions[color]
                    if (existing == null && color in perColorRestrictions) {
                        // Already have an unrestricted ability for this color — stays unrestricted
                    } else if (effectRestriction == null) {
                        // This ability is unrestricted for this color
                        perColorRestrictions[color] = null
                    } else if (existing == null && color !in perColorRestrictions) {
                        // First ability for this color — record its restriction
                        perColorRestrictions[color] = effectRestriction
                    } else if (existing != null && existing != effectRestriction) {
                        // Different restriction — collapse for the cached aggregate
                        // (player can choose which ability to activate) and flag this
                        // source as context-sensitive so solve() re-runs us with the
                        // payment context.
                        perColorRestrictions[color] = null
                        hasMixedRestrictions = true
                    }
                }

                // Track restriction for the combined source
                if (effectRestriction == null) {
                    hasUnrestrictedAbility = true
                } else if (!firstRestrictionSeen) {
                    commonRestriction = effectRestriction
                    firstRestrictionSeen = true
                } else if (commonRestriction != effectRestriction) {
                    // Different restrictions across abilities — treat as unrestricted
                    // (player can choose which ability to activate); flag for the
                    // context-aware re-solve.
                    hasUnrestrictedAbility = true
                    hasMixedRestrictions = true
                }
            }

            // If we found any usable mana abilities, return the combined source
            if (combinedColors.isNotEmpty() || producesColorless) {
                // If any ability has no pain cost, the source is not a pain source
                val hasPainCost = !anyAbilityHasNoPainCost && minPainAmount < Int.MAX_VALUE
                val painAmount = if (hasPainCost) minPainAmount else 0

                // Determine combined restriction: unrestricted if any ability is unrestricted
                val sourceRestriction = if (hasUnrestrictedAbility) null else commonRestriction

                // Build the per-color restrictions map (only include restricted colors)
                val restrictedColors = perColorRestrictions
                    .filter { (_, restriction) -> restriction != null }
                    .mapValues { (_, restriction) -> restriction!! }

                // Only record activation costs > 0 (the default is "free to produce").
                val colorActivationCosts = perColorActivationCost
                    .filter { (_, cost) -> cost > 0 }

                // Mark the source as sacrifice-required only when every accepted ability
                // demanded sacrifice. If any non-sac ability was accepted, that path is
                // preferred and the source is offered without sacrifice.
                val requiresSacrifice = anyAcceptedWithSac && !anyAcceptedWithoutSac

                // Same rule for the tap-another-permanent sub-cost: surface it only when
                // every accepted ability requires the secondary tap. If any plain mana
                // ability was accepted, that path is preferred.
                val tapPermanentsSubCost = if (anyAcceptedWithTapPermanents && !anyAcceptedWithoutTapPermanents) {
                    firstAcceptedTapPermanentsSubCost
                } else {
                    null
                }

                return@mapNotNull ManaSource(
                    entityId = entityId,
                    name = card.name,
                    producesColors = combinedColors,
                    producesColorless = producesColorless,
                    isBasicLand = isBasicLand,
                    isLand = card.typeLine.isLand,
                    isCreature = isCreature,
                    hasNonManaAbilities = hasNonManaAbilities,
                    hasPainCost = hasPainCost,
                    painAmount = painAmount,
                    canAttack = canAttack,
                    manaAmount = maxManaAmount,
                    restriction = sourceRestriction,
                    colorRiders = perColorRiders.mapValues { (_, v) -> v.toSet() },
                    colorRestrictions = restrictedColors,
                    colorActivationManaCost = colorActivationCosts,
                    requiresSacrifice = requiresSacrifice,
                    hasContextSensitiveAbilities = hasMixedRestrictions,
                    tapPermanentsSubCost = tapPermanentsSubCost,
                )
            }

            // Fall back to land subtype logic for lands without explicit abilities
            // (lands without basic land subtypes that also have no explicit mana abilities
            // produce colorless mana, e.g., Wastes)
            // Skip lands that have non-mana activated abilities but no mana abilities
            // (e.g., fetch lands like Windswept Heath)
            if (!card.typeLine.isLand) return@mapNotNull null
            if (allAbilities.isNotEmpty() && manaAbilities.isEmpty()) return@mapNotNull null

            ManaSource(
                entityId = entityId,
                name = card.name,
                producesColors = emptySet(),
                producesColorless = true,
                isBasicLand = isBasicLand,
                isLand = true,
                isCreature = false,
                hasNonManaAbilities = hasNonManaAbilities,
                hasPainCost = false,
                painAmount = 0,
                canAttack = false
            )
        }.map { source -> augmentWithAuraBonusMana(state, source, playerId) }
            .map { source -> augmentWithSourceTapBonusMana(state, source, playerId) }
            .let { sources ->
                if (hasDampLandManaProduction(state)) applyLandManaDampening(sources) else sources
            }
    }

    /**
     * Returns true when every [ActivationRestriction] on the given mana ability is currently
     * satisfied for the controller. Mirrors `CastPermissionUtils.checkActivationRestriction` but
     * is inlined here so the auto-tap solver doesn't need to depend on the legalactions module.
     */
    private fun activationRestrictionsSatisfied(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        ability: ActivatedAbility
    ): Boolean {
        if (ability.restrictions.isEmpty()) return true
        return ability.restrictions.all {
            checkActivationRestriction(state, playerId, sourceId, ability, it)
        }
    }

    private fun checkActivationRestriction(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        ability: ActivatedAbility,
        restriction: ActivationRestriction
    ): Boolean = when (restriction) {
        is ActivationRestriction.AnyPlayerMay -> true
        is ActivationRestriction.OnlyDuringYourTurn -> state.activePlayerId == playerId
        is ActivationRestriction.BeforeStep -> state.step.ordinal < restriction.step.ordinal
        is ActivationRestriction.DuringPhase -> state.phase == restriction.phase
        is ActivationRestriction.DuringStep -> state.step == restriction.step
        is ActivationRestriction.OnlyIfCondition -> {
            val opponentId = state.turnOrder.firstOrNull { it != playerId }
            val context = EffectContext(
                sourceId = sourceId,
                controllerId = playerId,
                opponentId = opponentId,
                targets = emptyList(),
                xValue = 0
            )
            conditionEvaluator.evaluate(state, restriction.condition, context)
        }
        is ActivationRestriction.OncePerTurn -> {
            val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
            tracker == null || !tracker.hasActivated(ability.id)
        }
        is ActivationRestriction.Once -> {
            val tracker = state.getEntity(sourceId)?.get<AbilityActivatedEverComponent>()
            tracker == null || !tracker.hasActivated(ability.id)
        }
        is ActivationRestriction.All -> restriction.restrictions.all {
            checkActivationRestriction(state, playerId, sourceId, ability, it)
        }
    }

    /**
     * Evaluates a DynamicAmount for a mana ability, returning the actual mana count.
     * Returns 0 when the amount evaluates to zero (e.g., no creatures of the chosen type).
     */
    /**
     * Extract the [ManaRestriction] (if any) attached to the mana-producing effect of a
     * mana ability. Used to filter abilities by spell/ability payment context before
     * combining multiple abilities on the same source.
     */
    private fun extractManaRestriction(effect: com.wingedsheep.sdk.scripting.effects.Effect): ManaRestriction? {
        val manaEffect = when (effect) {
            is CompositeEffect -> effect.effects.firstOrNull {
                it is AddManaEffect ||
                    it is AddColorlessManaEffect ||
                    it is AddAnyColorManaEffect ||
                    it is AddManaOfColorAmongEffect ||
                    it is AddManaOfColorLandsCouldProduceEffect ||
                    it is AddManaOfChosenColorEffect ||
                    it is AddDynamicManaEffect
            } ?: effect
            else -> effect
        }
        return when (manaEffect) {
            is AddManaEffect -> manaEffect.restriction
            is AddColorlessManaEffect -> manaEffect.restriction
            is AddAnyColorManaEffect -> manaEffect.restriction
            is AddManaOfColorAmongEffect -> manaEffect.restriction
            is AddManaOfColorLandsCouldProduceEffect -> manaEffect.restriction
            is AddManaOfChosenColorEffect -> manaEffect.restriction
            is AddDynamicManaEffect -> manaEffect.restriction
            // AddAnyColorManaSpendOnChosenTypeEffect derives its restriction at resolution
            // time from the source's ChosenCreatureTypeComponent, so we don't pre-filter it.
            else -> null
        }
    }

    private fun evaluateManaAmount(
        amount: DynamicAmount,
        state: GameState,
        sourceId: EntityId,
        playerId: EntityId
    ): Int {
        if (amount is DynamicAmount.Fixed) return amount.amount
        val opponentId = state.turnOrder.firstOrNull { it != playerId }
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = playerId,
            opponentId = opponentId,
            targets = emptyList(),
            xValue = null
        )
        return maxOf(0, dynamicAmountEvaluator.evaluate(state, amount, context))
    }

    /**
     * Returns the color an attached aura's [OverrideEnchantedLandManaColor] ability
     * forces the source land to produce, or `null` if no override applies.
     * Mirrors [ActivateAbilityHandler]'s version — both must agree or mana solving
     * desynchronises from mana ability resolution.
     */
    private fun findEnchantedLandManaColorOverride(
        state: GameState,
        sourceId: EntityId
    ): Color? {
        var override: Color? = null
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val attachedTo = container.get<AttachedToComponent>()
            if (attachedTo?.targetId != sourceId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val o = staticAbility as? com.wingedsheep.sdk.scripting.OverrideEnchantedLandManaColor ?: continue
                override = o.color
                    ?: container.get<ChosenColorComponent>()?.color
                    ?: continue
            }
        }
        return override
    }

    /**
     * Checks if a mana source has auras attached with AdditionalManaOnTap
     * and augments the source with bonus mana information.
     */
    private fun augmentWithAuraBonusMana(
        state: GameState,
        source: ManaSource,
        playerId: EntityId
    ): ManaSource {
        var totalBonus = 0
        var bonusColor: Color? = null

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val attachedTo = container.get<AttachedToComponent>()
            if (attachedTo?.targetId != source.entityId) continue

            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (staticAbility in cardDef.script.staticAbilities) {
                val additionalMana = staticAbility as? AdditionalManaOnTap ?: continue

                val landController = state.getEntity(source.entityId)
                    ?.get<ControllerComponent>()?.playerId ?: playerId
                val opponentId = state.turnOrder.firstOrNull { it != landController }

                val context = EffectContext(
                    sourceId = entityId,
                    controllerId = landController,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = null
                )

                val amount = dynamicAmountEvaluator.evaluate(state, additionalMana.amount, context)
                if (amount > 0) {
                    // Resolve the color: null means "read the aura's chosen color".
                    // If no color is chosen (shouldn't happen in practice), skip.
                    val manaColor = additionalMana.color
                        ?: container.get<ChosenColorComponent>()?.color
                        ?: continue
                    totalBonus += amount
                    bonusColor = manaColor
                }
            }
        }

        return if (totalBonus > 0) {
            source.copy(bonusManaPerTap = totalBonus, bonusManaColor = bonusColor)
        } else {
            source
        }
    }

    /**
     * Augments a mana source with bonus mana from [AdditionalManaOnSourceTap]
     * statics anywhere on the battlefield. Covers both flavors:
     *  - Lavaleaper: filter = BasicLand, color = null (mirror produced color)
     *  - Badgermole Cub: filter = Creature.youControl(), color = GREEN
     *
     * Filter matching uses projected state so animated creature-lands and typeshifted
     * lands are recognised under their projected types. The static-ability source's
     * controller is also read from projected state and used as the "you" perspective
     * for the filter's controller predicate, so the "you tap" form transfers correctly
     * across control-changing effects.
     *
     * The bonus color, when mirroring, is the source's first produced color — basic
     * lands produce a single color so this is unambiguous for the canonical case;
     * multi-color producers fall back to the first listed color (the auto-tap solver
     * already preferentially picks single-color sources for colored slots).
     */
    private fun augmentWithSourceTapBonusMana(
        state: GameState,
        source: ManaSource,
        tappingPlayerId: EntityId
    ): ManaSource {
        var totalBonus = 0
        var bonusColor: Color? = null

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (staticAbility in cardDef.script.staticAbilities) {
                val onSourceTap = staticAbility as? AdditionalManaOnSourceTap ?: continue

                val staticController = state.projectedState.getController(entityId) ?: continue

                // Filter from the static-ability controller's perspective — see
                // AdditionalManaOnSourceTap kdoc.
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (!predicateEvaluator.matchesWithProjection(
                        state, state.projectedState, source.entityId, onSourceTap.sourceFilter, filterContext
                    )) continue

                val opponentId = state.turnOrder.firstOrNull { it != tappingPlayerId }
                val effectContext = EffectContext(
                    sourceId = entityId,
                    controllerId = tappingPlayerId,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = null
                )
                val amount = dynamicAmountEvaluator.evaluate(state, onSourceTap.amount, effectContext)
                if (amount <= 0) continue

                // Resolve the bonus color: explicit color wins; null means mirror the source's produced color.
                val resolvedColor = onSourceTap.color ?: source.producesColors.firstOrNull() ?: continue
                totalBonus += amount
                bonusColor = bonusColor ?: resolvedColor
            }
        }

        return if (totalBonus > 0) {
            source.copy(
                bonusManaPerTap = source.bonusManaPerTap + totalBonus,
                bonusManaColor = source.bonusManaColor ?: bonusColor
            )
        } else {
            source
        }
    }

    /**
     * Get mana abilities granted to an entity by static abilities on battlefield permanents.
     * E.g., Clement, the Worrywort grants "{T}: Add {G} or {U}" to Frog creatures.
     */
    private fun getStaticGrantedManaAbilities(
        entityId: EntityId,
        state: GameState
    ): List<ActivatedAbility> {
        val result = mutableListOf<ActivatedAbility>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                if (ability is GrantActivatedAbility &&
                    ability.filter.scope is com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield) {
                    if (ability.filter.excludeSelf && permanentId == entityId) continue
                    val granterController = state.projectedState.getController(permanentId) ?: continue
                    val matches = predicateEvaluator.matchesWithProjection(
                        state,
                        state.projectedState,
                        entityId,
                        ability.filter.baseFilter,
                        PredicateContext(controllerId = granterController, sourceId = permanentId)
                    )
                    if (matches && ability.ability.isManaAbility) {
                        result.add(ability.ability)
                    }
                }
            }
        }

        return result
    }

    /**
     * Check if any permanent on the battlefield has DampLandManaProduction.
     */
    private fun hasDampLandManaProduction(state: GameState): Boolean {
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                if (cardDef.script.staticAbilities.any { it is DampLandManaProduction }) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Apply Damping Sphere mana dampening to land sources.
     * Lands that would produce 2+ total mana are converted to produce 1 colorless instead.
     */
    private fun applyLandManaDampening(sources: List<ManaSource>): List<ManaSource> {
        return sources.map { source ->
            // Only dampen lands; non-land mana sources (mana dorks, mana rocks) are unaffected
            val totalMana = source.manaAmount + source.bonusManaPerTap
            if (source.isLand && totalMana >= 2) {
                source.copy(
                    producesColors = emptySet(),
                    producesColorless = true,
                    manaAmount = 1,
                    bonusManaPerTap = 0,
                    bonusManaColor = null
                )
            } else {
                source
            }
        }
    }

    /**
     * Finds the best source to produce a specific color.
     * Uses priority-based selection to pick the optimal source.
     * Respects per-color mana restrictions when a spell context is provided.
     */
    private fun findBestSourceForColor(
        sources: List<ManaSource>,
        color: Color,
        handRequirements: Map<Color, Int>,
        availableSourcesByColor: Map<Color, Int>,
        spellContext: SpellPaymentContext? = null
    ): ManaSource? {
        return sources
            .filter { it.availableColorsFor(spellContext).contains(color) }
            .minByOrNull { calculateTapPriority(it, handRequirements, availableSourcesByColor) }
    }

    /**
     * Checks if a player can pay a mana cost (from floating mana pool + auto-pay).
     * Considers floating mana first, then checks if remaining can be paid by tapping sources.
     */
    fun canPay(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        xValue: Int = 0,
        excludeSources: Set<EntityId> = emptySet(),
        spellContext: SpellPaymentContext? = null,
        precomputedSources: List<ManaSource>? = null
    ): Boolean {
        // Get the player's floating mana pool
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>()
        val pool = if (poolComponent != null) {
            ManaPool(
                white = poolComponent.white,
                blue = poolComponent.blue,
                black = poolComponent.black,
                red = poolComponent.red,
                green = poolComponent.green,
                colorless = poolComponent.colorless,
                restrictedMana = poolComponent.restrictedMana
            )
        } else {
            ManaPool()
        }

        // Pay partial from pool for the base cost
        val partialResult = pool.payPartial(cost, spellContext)
        val remainingCost = partialResult.remainingCost
        val poolAfterPartial = partialResult.newPool

        // Calculate how much X mana is needed (multiply by X symbol count for XX costs)
        val xSymbolCount = cost.xCount.coerceAtLeast(1)
        val totalXMana = xValue * xSymbolCount
        val xPaidFromPool = poolAfterPartial.total.coerceAtMost(totalXMana)
        val xRemainingToPay = totalXMana - xPaidFromPool

        // If nothing remains after using pool (including X), we can pay
        if (remainingCost.isEmpty() && xRemainingToPay == 0) {
            return true
        }

        // Check if we can tap sources for the remaining cost (including remaining X)
        if (solve(state, playerId, remainingCost, xRemainingToPay, excludeSources, spellContext, precomputedSources) != null) return true

        // Fallback: check if "extras" — mana abilities the auto-tap solver doesn't pick — can
        // cover the remaining cost. Two flavors:
        //  1. TapPermanents (e.g. Birchlore Rangers): tap *other* permanents to produce mana.
        //  2. SacrificeSelf (e.g. Treasure tokens): a tap+sacrifice mana ability. The solver
        //     refuses to auto-tap these because paying SacrificeSelf in the auto-pay flow would
        //     mean silently losing the permanent; the player must opt-in by activating the
        //     ability directly. But the spell is still *affordable* — we just need to know it.
        val bonus = calculateTapPermanentsBonusMana(state, playerId)
            .plus(calculateSacrificeSelfBonusMana(state, playerId))
            .plus(calculateCompositeTapPermanentsBonusMana(state, playerId))
        if (bonus.totalMana == 0) return false

        // Allocate any-color bonus mana to the pool based on what the cost needs,
        // then re-check. This correctly handles color requirements.
        val augmentedPool = allocateAnyColorManaToPool(pool, bonus.anyColorMana, cost)
            .let { p ->
                // Also add specific-color bonus mana
                bonus.specificMana.entries.fold(p) { acc, (color, amount) -> acc.add(color, amount) }
            }
            .let { p ->
                // Also add colorless bonus mana
                if (bonus.colorlessMana > 0) p.addColorless(bonus.colorlessMana) else p
            }
        val augmentedResult = augmentedPool.payPartial(cost)
        val augmentedRemaining = augmentedResult.remainingCost
        val augmentedPoolAfter = augmentedResult.newPool

        val augmentedXPaid = augmentedPoolAfter.total.coerceAtMost(totalXMana)
        val augmentedXRemaining = totalXMana - augmentedXPaid

        if (augmentedRemaining.isEmpty() && augmentedXRemaining == 0) return true
        return solve(state, playerId, augmentedRemaining, augmentedXRemaining, excludeSources, spellContext, precomputedSources) != null
    }

    /**
     * Gets the total available mana for a player (floating mana + untapped sources).
     */
    fun getAvailableManaCount(state: GameState, playerId: EntityId, precomputedSources: List<ManaSource>? = null): Int {
        // Count floating mana
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>()
        val floatingMana = if (poolComponent != null) {
            poolComponent.white + poolComponent.blue + poolComponent.black +
                poolComponent.red + poolComponent.green + poolComponent.colorless
        } else {
            0
        }

        // Add untapped mana sources (including bonus mana from auras and multi-mana sources).
        // Sacrifice-self sources (treasures) and composite tap+TapPermanents sources
        // (Springleaf Drum) are counted below via their dedicated bonus helpers, so skip
        // them here to avoid double-counting.
        val sourceMana = (precomputedSources ?: findAvailableManaSources(state, playerId))
            .filter { !it.requiresSacrifice && it.tapPermanentsSubCost == null }
            .sumOf { it.manaAmount + it.bonusManaPerTap }

        // Add extra mana from "extras" abilities the solver doesn't pick:
        //  - TapPermanents (e.g., Birchlore Rangers)
        //  - Tap+SacrificeSelf mana abilities (e.g., Treasure tokens)
        //  - Composite Tap+TapPermanents mana abilities (e.g., Springleaf Drum)
        val extrasMana = calculateTapPermanentsBonusMana(state, playerId).totalMana +
            calculateSacrificeSelfBonusMana(state, playerId).totalMana +
            calculateCompositeTapPermanentsBonusMana(state, playerId).totalMana

        return floatingMana + sourceMana + extrasMana
    }

    /**
     * Bonus mana available from TapPermanents mana abilities.
     */
    internal data class TapPermanentsBonusMana(
        val anyColorMana: Int = 0,
        val specificMana: Map<Color, Int> = emptyMap(),
        val colorlessMana: Int = 0
    ) {
        val totalMana: Int get() = anyColorMana + specificMana.values.sum() + colorlessMana

        operator fun plus(other: TapPermanentsBonusMana): TapPermanentsBonusMana {
            val mergedSpecific = buildMap {
                putAll(specificMana)
                for ((color, amount) in other.specificMana) {
                    merge(color, amount, Int::plus)
                }
            }
            return TapPermanentsBonusMana(
                anyColorMana = anyColorMana + other.anyColorMana,
                specificMana = mergedSpecific,
                colorlessMana = colorlessMana + other.colorlessMana
            )
        }
    }

    /**
     * Calculates extra mana available from TapPermanents mana abilities (e.g., Birchlore Rangers).
     *
     * These abilities tap other permanents (not the source itself) to produce mana.
     * Only counts activations using permanents that are NOT already regular mana sources,
     * so this represents genuinely "extra" mana that the solver doesn't know about.
     */
    internal fun calculateTapPermanentsBonusMana(
        state: GameState,
        playerId: EntityId
    ): TapPermanentsBonusMana {
        val projected = state.projectedState
        val battlefieldCards = projected.getBattlefieldControlledBy(playerId)
        val regularSourceIds = findAvailableManaSources(state, playerId).map { it.entityId }.toSet()

        var anyColorTotal = 0
        val specificColorTotal = mutableMapOf<Color, Int>()
        var colorlessTotal = 0

        // Track which non-source permanents have already been "consumed" by a TapPermanents activation
        val consumedIds = mutableSetOf<EntityId>()

        for (entityId in battlefieldCards) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            // Skip own abilities that have been stripped by a continuous effect (Humility, Noggle the Mind, etc.)
            if (projected.hasLostAllAbilities(entityId)) continue

            for (ability in cardDef.script.activatedAbilities) {
                if (!ability.isManaAbility) continue
                val tapCost = ability.cost as? AbilityCost.TapPermanents ?: continue

                // Find untapped permanents matching the filter that are NOT regular mana sources
                // and haven't been consumed by another TapPermanents activation.
                // Note: TapPermanents doesn't use the {T} symbol, so summoning sickness doesn't apply.
                val context = PredicateContext(controllerId = playerId)
                val matchingNonSources = battlefieldCards.filter { targetId ->
                    targetId !in regularSourceIds &&
                    targetId !in consumedIds &&
                    state.getEntity(targetId)?.has<TappedComponent>() == false &&
                    predicateEvaluator.matchesWithProjection(state, projected, targetId, tapCost.filter, context)
                }

                val activationCount = matchingNonSources.size / tapCost.count
                if (activationCount == 0) continue

                // Mark consumed permanents
                val toConsume = matchingNonSources.take(activationCount * tapCost.count)
                consumedIds.addAll(toConsume)

                // Accumulate mana production
                when (val effect = ability.effect) {
                    is AddAnyColorManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        anyColorTotal += activationCount * amount
                    }
                    is AddAnyColorManaSpendOnChosenTypeEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        anyColorTotal += activationCount * amount
                    }
                    is AddManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        specificColorTotal[effect.color] =
                            (specificColorTotal[effect.color] ?: 0) + activationCount * amount
                    }
                    is AddColorlessManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        colorlessTotal += activationCount * amount
                    }
                    else -> {}
                }
            }
        }

        return TapPermanentsBonusMana(anyColorTotal, specificColorTotal, colorlessTotal)
    }

    /**
     * Calculates extra mana available from tap+SacrificeSelf mana abilities (e.g. Treasure tokens,
     * "{T}, Sacrifice this artifact: Add one mana of any color").
     *
     * The auto-tap solver refuses to pick these sources because the SacrificeSelf sub-cost can't
     * be silently paid by the auto-pay flow — the player has to activate the ability directly so
     * the sacrifice is explicit. But the spell is still *affordable* when the player has these
     * permanents available, so `canPay` and `getAvailableManaCount` must count their production.
     */
    internal fun calculateSacrificeSelfBonusMana(
        state: GameState,
        playerId: EntityId
    ): TapPermanentsBonusMana {
        val projected = state.projectedState
        val battlefieldCards = projected.getBattlefieldControlledBy(playerId)

        var anyColorTotal = 0
        val specificColorTotal = mutableMapOf<Color, Int>()
        var colorlessTotal = 0

        for (entityId in battlefieldCards) {
            val container = state.getEntity(entityId) ?: continue

            // Already tapped → can't pay the {T} sub-cost.
            if (container.has<TappedComponent>()) continue

            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            // Own abilities stripped by Humility / similar — skip the printed mana ability.
            if (projected.hasLostAllAbilities(entityId)) continue

            // Summoning sickness applies to non-land creatures (Rule 302.1) — they can't tap unless they have haste.
            val isCreature = card.typeLine.isCreature
            if (!card.typeLine.isLand && isCreature) {
                val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
                if (hasSummoningSickness && !hasHaste) continue
            }

            for (ability in cardDef.script.activatedAbilities) {
                if (!ability.isManaAbility) continue
                val composite = ability.cost as? AbilityCost.Composite ?: continue
                val hasTap = composite.costs.any { it is AbilityCost.Tap }
                val hasSacSelf = composite.costs.any { it is AbilityCost.SacrificeSelf }
                if (!hasTap || !hasSacSelf) continue

                // Skip abilities that bundle a mana sub-cost — the player can still afford them
                // when the pool has mana, but counting their net production here would
                // double-count and complicate color resolution. Treasure / Food (mana) / etc.
                // have a flat tap+sac shape; we cover the canonical case.
                if (composite.costs.any { it is AbilityCost.Mana }) continue

                // Honor activation restrictions (e.g. "only during your turn").
                if (!activationRestrictionsSatisfied(state, playerId, entityId, ability)) continue

                when (val effect = ability.effect) {
                    is AddAnyColorManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        anyColorTotal += amount
                    }
                    is AddManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        specificColorTotal[effect.color] =
                            (specificColorTotal[effect.color] ?: 0) + amount
                    }
                    is AddColorlessManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        colorlessTotal += amount
                    }
                    else -> {}
                }
            }
        }

        return TapPermanentsBonusMana(anyColorTotal, specificColorTotal, colorlessTotal)
    }

    /**
     * Whether enough untapped permanents matching [subCost] exist on the battlefield to
     * pay the secondary tap of a composite Tap+TapPermanents mana ability (Springleaf Drum).
     *
     * The source's own entity is always excluded — even when [TapPermanentsSubCost.excludeSelf]
     * is false, the source is tapped by the {T} sub-cost and so can't also satisfy the
     * "tap another permanent" half. The filter is matched via the projected state so
     * type-changing effects (Mistform Elemental, etc.) are honored.
     */
    private fun hasEnoughTapTargets(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        subCost: TapPermanentsSubCost
    ): Boolean {
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId)
        val matches = projected.getBattlefieldControlledBy(playerId).count { targetId ->
            targetId != sourceId &&
                state.getEntity(targetId)?.has<TappedComponent>() == false &&
                predicateEvaluator.matchesWithProjection(state, projected, targetId, subCost.filter, context)
        }
        return matches >= subCost.count
    }

    /**
     * Bonus mana available from composite Tap+TapPermanents mana abilities (Springleaf Drum:
     * "{T}, Tap an untapped creature you control: Add one mana of any color").
     *
     * `solve()` refuses to auto-tap these sources because the secondary tap requires player
     * input, but `canPay` and `getAvailableManaCount` must still count their production so
     * a ward (or other "counter unless pays") doesn't short-circuit to countered when the
     * player would actually be able to pay manually.
     *
     * Each Springleaf-like source contributes one activation, sharing a single pool of
     * untapped non-source creatures across all such activations to avoid double-counting.
     */
    internal fun calculateCompositeTapPermanentsBonusMana(
        state: GameState,
        playerId: EntityId
    ): TapPermanentsBonusMana {
        val projected = state.projectedState
        val battlefieldCards = projected.getBattlefieldControlledBy(playerId)

        var anyColorTotal = 0
        val specificColorTotal = mutableMapOf<Color, Int>()
        var colorlessTotal = 0

        val consumedIds = mutableSetOf<EntityId>()

        for (entityId in battlefieldCards) {
            val container = state.getEntity(entityId) ?: continue
            if (container.has<TappedComponent>()) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (projected.hasLostAllAbilities(entityId)) continue

            val isCreature = card.typeLine.isCreature
            if (!card.typeLine.isLand && isCreature) {
                val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
                if (hasSummoningSickness && !hasHaste) continue
            }

            for (ability in cardDef.script.activatedAbilities) {
                if (!ability.isManaAbility) continue
                val composite = ability.cost as? AbilityCost.Composite ?: continue
                val hasTap = composite.costs.any { it is AbilityCost.Tap }
                val tapPermanentsCost = composite.costs
                    .firstOrNull { it is AbilityCost.TapPermanents } as? AbilityCost.TapPermanents
                if (!hasTap || tapPermanentsCost == null) continue
                // Skip composites that also bundle SacrificeSelf or a mana sub-cost — those are
                // handled by other helpers (calculateSacrificeSelfBonusMana) and would
                // double-count or complicate color resolution here.
                if (composite.costs.any { it is AbilityCost.SacrificeSelf || it is AbilityCost.Mana }) continue

                if (!activationRestrictionsSatisfied(state, playerId, entityId, ability)) continue

                val context = PredicateContext(controllerId = playerId)
                val matchingTapTargets = battlefieldCards.filter { targetId ->
                    targetId != entityId &&
                        targetId !in consumedIds &&
                        state.getEntity(targetId)?.has<TappedComponent>() == false &&
                        predicateEvaluator.matchesWithProjection(state, projected, targetId, tapPermanentsCost.filter, context)
                }
                if (matchingTapTargets.size < tapPermanentsCost.count) continue

                consumedIds.addAll(matchingTapTargets.take(tapPermanentsCost.count))

                when (val effect = ability.effect) {
                    is AddAnyColorManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        anyColorTotal += amount
                    }
                    is AddManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        specificColorTotal[effect.color] =
                            (specificColorTotal[effect.color] ?: 0) + amount
                    }
                    is AddColorlessManaEffect -> {
                        val amount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        colorlessTotal += amount
                    }
                    else -> {}
                }
                // Each source contributes one activation per canPay query — multiple
                // mana abilities on the same Springleaf-like permanent are uncommon and
                // would share the {T} cost anyway.
                break
            }
        }

        return TapPermanentsBonusMana(anyColorTotal, specificColorTotal, colorlessTotal)
    }

    /**
     * Allocates any-color bonus mana to the pool based on what the cost needs.
     * Adds mana to colors where there's a deficit relative to the cost's colored requirements,
     * then adds the rest as colorless (usable for generic costs).
     */
    private fun allocateAnyColorManaToPool(pool: ManaPool, anyColorCount: Int, cost: ManaCost): ManaPool {
        if (anyColorCount == 0) return pool

        // Determine colored mana needed from the cost
        val colorNeeds = mutableMapOf<Color, Int>()
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> colorNeeds[symbol.color] = (colorNeeds[symbol.color] ?: 0) + 1
                is ManaSymbol.Phyrexian -> colorNeeds[symbol.color] = (colorNeeds[symbol.color] ?: 0) + 1
                is ManaSymbol.Hybrid -> {
                    // For hybrid, add to both colors (overestimates but correct for affordability)
                    colorNeeds[symbol.color1] = (colorNeeds[symbol.color1] ?: 0) + 1
                    colorNeeds[symbol.color2] = (colorNeeds[symbol.color2] ?: 0) + 1
                }
                else -> {}
            }
        }

        var result = pool
        var remaining = anyColorCount

        // Allocate to colors where pool is deficient, starting with the biggest deficit
        for ((color, needed) in colorNeeds.entries.sortedByDescending { it.value }) {
            val poolHas = result.get(color)
            val deficit = needed - poolHas
            if (deficit > 0) {
                val toAdd = minOf(remaining, deficit)
                result = result.add(color, toAdd)
                remaining -= toAdd
                if (remaining == 0) break
            }
        }

        // Remaining any-color mana goes to colorless (usable for generic costs)
        if (remaining > 0) {
            result = result.addColorless(remaining)
        }

        return result
    }
}
