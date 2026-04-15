package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
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
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfColorAmongEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AdditionalManaOnLandTap
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.engine.state.components.identity.ChosenColorComponent
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
    /** Per-color mana restrictions. Colors not in this map are unrestricted. */
    val colorRestrictions: Map<Color, ManaRestriction> = emptyMap()
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
 * Result of solving mana payment.
 *
 * @property sources The mana sources to tap to pay the cost
 * @property manaProduced Map of each source to the mana it will produce for this payment
 */
data class ManaSolution(
    val sources: List<ManaSource>,
    val manaProduced: Map<EntityId, ManaProduction>,
    /** Bonus mana remaining after the solver consumed some to pay the cost */
    val remainingBonusMana: Map<Color, Int> = emptyMap()
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
        // Get all untapped mana sources controlled by the player
        // Filter out restricted sources that are ineligible for the spell being cast
        val availableSources = (precomputedSources ?: findAvailableManaSources(state, playerId))
            .filter { it.entityId !in excludeSources }
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

        // Track bonus mana from auras and excess mana from multi-mana sources
        var bonusManaPool = mutableMapOf<Color, Int>()

        // Helper to update available counts when a source is used
        fun useSource(source: ManaSource, colorUsed: Color?) {
            usedSources.add(source)
            remainingSources.remove(source)
            for (color in source.producesColors) {
                availableSourcesByColor[color] = (availableSourcesByColor[color] ?: 1) - 1
            }
            // Track excess mana from multi-mana sources (e.g., Elvish Aberration produces 3 green)
            if (source.manaAmount > 1 && colorUsed != null) {
                bonusManaPool[colorUsed] = (bonusManaPool[colorUsed] ?: 0) + (source.manaAmount - 1)
            }
            // Collect bonus mana from auras attached to this source
            if (source.bonusManaPerTap > 0 && source.bonusManaColor != null) {
                bonusManaPool[source.bonusManaColor] =
                    (bonusManaPool[source.bonusManaColor] ?: 0) + source.bonusManaPerTap
            }
        }

        // Helper to spend bonus mana for a colored cost
        fun spendBonusMana(color: Color): Boolean {
            val available = bonusManaPool[color] ?: 0
            if (available > 0) {
                bonusManaPool[color] = available - 1
                return true
            }
            return false
        }

        // Helper to spend any bonus mana for generic cost
        fun spendAnyBonusMana(): Boolean {
            val entry = bonusManaPool.entries.firstOrNull { it.value > 0 } ?: return false
            bonusManaPool[entry.key] = entry.value - 1
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

        return ManaSolution(usedSources, manaProduced, bonusManaPool.filter { it.value > 0 })
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
    fun findAvailableManaSources(state: GameState, playerId: EntityId): List<ManaSource> {
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
            val allAbilities = cardDef?.script?.activatedAbilities ?: emptyList()

            // Include mana abilities granted by static effects from other permanents
            // (e.g., Clement, the Worrywort granting {T}: Add {G} or {U} to Frogs)
            val staticGrantedManaAbilities = getStaticGrantedManaAbilities(entityId, card, state)
            val manaAbilities = allAbilities.filter { it.isManaAbility } + staticGrantedManaAbilities

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
                    return@mapNotNull ManaSource(
                        entityId = entityId,
                        name = card.name,
                        producesColors = subtypeColors,
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
            // Track restrictions: if any ability is unrestricted, the source is unrestricted
            var hasUnrestrictedAbility = false
            var commonRestriction: ManaRestriction? = null
            var firstRestrictionSeen = false
            // Track per-color restrictions (for sources with mixed restricted/unrestricted abilities)
            val perColorRestrictions = mutableMapOf<Color, ManaRestriction?>()

            for (ability in manaAbilities) {
                // Detect pain cost and mana activation cost in mana abilities
                var abilityHasPainCost = false
                var abilityPainAmount = 0
                var abilityActivationManaCost = 0
                val abilityCanBeUsed = when (val cost = ability.cost) {
                    is AbilityCost.Tap -> true
                    is AbilityCost.PayLife -> {
                        abilityHasPainCost = true
                        abilityPainAmount = cost.amount
                        true
                    }
                    is AbilityCost.Composite -> {
                        var hasTap = false
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
                                else -> {}
                            }
                        }
                        hasTap // Only auto-pay tap-based abilities
                    }
                    else -> false // Skip non-tap mana abilities
                }

                if (!abilityCanBeUsed) continue

                // Check summoning sickness for creatures (non-lands)
                if (!card.typeLine.isLand && isCreature) {
                    if (hasSummoningSickness && !hasHaste) {
                        continue // Can't use this ability due to summoning sickness
                    }
                }

                if (!abilityHasPainCost) anyAbilityHasNoPainCost = true
                if (abilityHasPainCost) minPainAmount = minOf(minPainAmount, abilityPainAmount)

                // Accumulate production from effect (subtract activation mana cost for net output)
                val effectColors = mutableSetOf<Color>()
                val effectRestriction: ManaRestriction? = when (val effect = ability.effect) {
                    is AddManaEffect -> {
                        combinedColors.add(effect.color)
                        effectColors.add(effect.color)
                        val manaAmount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        val netMana = maxOf(0, manaAmount - abilityActivationManaCost)
                        maxManaAmount = maxOf(maxManaAmount, netMana)
                        effect.restriction
                    }
                    is AddColorlessManaEffect -> {
                        producesColorless = true
                        val manaAmount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        val netMana = maxOf(0, manaAmount - abilityActivationManaCost)
                        maxManaAmount = maxOf(maxManaAmount, netMana)
                        effect.restriction
                    }
                    is AddAnyColorManaEffect -> {
                        combinedColors.addAll(Color.entries)
                        effectColors.addAll(Color.entries)
                        val manaAmount = evaluateManaAmount(effect.amount, state, entityId, playerId)
                        val netMana = maxOf(0, manaAmount - abilityActivationManaCost)
                        maxManaAmount = maxOf(maxManaAmount, netMana)
                        effect.restriction
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
                            val netMana = maxOf(0, 1 - abilityActivationManaCost)
                            maxManaAmount = maxOf(maxManaAmount, netMana)
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
                            val netMana = maxOf(0, manaAmount - abilityActivationManaCost)
                            maxManaAmount = maxOf(maxManaAmount, netMana)
                        }
                        effect.restriction
                    }
                    is AddDynamicManaEffect -> {
                        combinedColors.addAll(effect.allowedColors)
                        effectColors.addAll(effect.allowedColors)
                        val manaAmount = (effect.amountSource as? DynamicAmount.Fixed)?.amount ?: 1
                        val netMana = maxOf(0, manaAmount - abilityActivationManaCost)
                        maxManaAmount = maxOf(maxManaAmount, netMana)
                        effect.restriction
                    }
                    else -> null
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
                        // Different restriction — treat as unrestricted (player can choose)
                        perColorRestrictions[color] = null
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
                    // (player can choose which ability to activate)
                    hasUnrestrictedAbility = true
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
                    colorRestrictions = restrictedColors
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
            .map { source -> augmentWithGlobalLandTapBonusMana(state, source) }
            .let { sources ->
                if (hasDampLandManaProduction(state)) applyLandManaDampening(sources) else sources
            }
    }

    /**
     * Evaluates a DynamicAmount for a mana ability, returning the actual mana count.
     * Returns 0 when the amount evaluates to zero (e.g., no creatures of the chosen type).
     */
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
                    totalBonus += amount
                    bonusColor = additionalMana.color
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
     * Augments a mana source with bonus mana from global [AdditionalManaOnLandTap]
     * abilities (e.g., Lavaleaper, Heartbeat of Spring). When any permanent on the
     * battlefield has this ability and its filter matches this source, the source
     * produces one additional mana of the same color per triggering ability.
     *
     * Only applies to lands — the oracle wording is specifically about taps for mana.
     * The bonus color is the source's first produced color (basic lands produce a
     * single color, so this is unambiguous for the canonical use case).
     */
    private fun augmentWithGlobalLandTapBonusMana(
        state: GameState,
        source: ManaSource
    ): ManaSource {
        if (!source.isLand) return source
        val landColor = source.producesColors.firstOrNull() ?: return source

        val landController = state.getEntity(source.entityId)
            ?.get<ControllerComponent>()?.playerId ?: return source

        var totalBonus = 0
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (staticAbility in cardDef.script.staticAbilities) {
                val onLandTap = staticAbility as? AdditionalManaOnLandTap ?: continue

                val filterContext = PredicateContext(controllerId = landController, sourceId = entityId)
                if (!predicateEvaluator.matches(state, source.entityId, onLandTap.filter, filterContext)) continue

                val opponentId = state.turnOrder.firstOrNull { it != landController }
                val effectContext = EffectContext(
                    sourceId = entityId,
                    controllerId = landController,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = null
                )
                val amount = dynamicAmountEvaluator.evaluate(state, onLandTap.amount, effectContext)
                if (amount > 0) totalBonus += amount
            }
        }

        return if (totalBonus > 0) {
            source.copy(
                bonusManaPerTap = source.bonusManaPerTap + totalBonus,
                bonusManaColor = source.bonusManaColor ?: landColor
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
        targetCard: CardComponent,
        state: GameState
    ): List<ActivatedAbility> {
        val result = mutableListOf<ActivatedAbility>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                if (ability is GrantActivatedAbilityToCreatureGroup) {
                    if (ability.filter.excludeSelf && permanentId == entityId) continue
                    val filter = ability.filter.baseFilter
                    val matchesAll = filter.cardPredicates.all { predicate ->
                        when (predicate) {
                            is CardPredicate.IsCreature -> targetCard.typeLine.isCreature
                            is CardPredicate.HasSubtype -> targetCard.typeLine.hasSubtype(predicate.subtype)
                            else -> true
                        }
                    }
                    if (matchesAll && ability.ability.isManaAbility) {
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

        // Fallback: check if TapPermanents mana abilities (e.g., Birchlore Rangers) provide enough extra mana.
        // These abilities tap other permanents (not the source itself) to produce mana.
        val tapPermanentsBonus = calculateTapPermanentsBonusMana(state, playerId)
        if (tapPermanentsBonus.totalMana == 0) return false

        // Allocate any-color bonus mana to the pool based on what the cost needs,
        // then re-check. This correctly handles color requirements.
        val augmentedPool = allocateAnyColorManaToPool(pool, tapPermanentsBonus.anyColorMana, cost)
            .let { p ->
                // Also add specific-color bonus mana
                tapPermanentsBonus.specificMana.entries.fold(p) { acc, (color, amount) -> acc.add(color, amount) }
            }
            .let { p ->
                // Also add colorless bonus mana
                if (tapPermanentsBonus.colorlessMana > 0) p.addColorless(tapPermanentsBonus.colorlessMana) else p
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

        // Add untapped mana sources (including bonus mana from auras and multi-mana sources)
        val sourceMana = (precomputedSources ?: findAvailableManaSources(state, playerId)).sumOf { it.manaAmount + it.bonusManaPerTap }

        // Add extra mana from TapPermanents abilities (e.g., Birchlore Rangers)
        val tapPermanentsMana = calculateTapPermanentsBonusMana(state, playerId).totalMana

        return floatingMana + sourceMana + tapPermanentsMana
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
