package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.StateProjector
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
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
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
    val bonusManaColor: Color? = null
)

/**
 * Result of solving mana payment.
 *
 * @property sources The mana sources to tap to pay the cost
 * @property manaProduced Map of each source to the mana it will produce for this payment
 */
data class ManaSolution(
    val sources: List<ManaSource>,
    val manaProduced: Map<EntityId, ManaProduction>
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
    private val cardRegistry: CardRegistry? = null,
    private val stateProjector: StateProjector = StateProjector(),
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) {

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
        xValue: Int = 0
    ): ManaSolution? {
        // Get all untapped mana sources controlled by the player
        val availableSources = findAvailableManaSources(state, playerId)

        if (availableSources.isEmpty() && cost.cmc > 0) {
            return null
        }

        // Analyze hand to inform smart tapping decisions
        val handRequirements = analyzeHandRequirements(state, playerId)

        // Count available sources per color for hand-awareness
        val availableSourcesByColor = mutableMapOf<Color, Int>()
        for (color in Color.entries) {
            availableSourcesByColor[color] = availableSources.count { it.producesColors.contains(color) }
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

                    val source = findBestSourceForColor(remainingSources, symbol.color, handRequirements, availableSourcesByColor)
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
                    val source1 = findBestSourceForColor(remainingSources, symbol.color1, handRequirements, availableSourcesByColor)
                    val source2 = findBestSourceForColor(remainingSources, symbol.color2, handRequirements, availableSourcesByColor)

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

                    val colorUsed = if (source.producesColors.contains(symbol.color1))
                        symbol.color1 else symbol.color2
                    manaProduced[source.entityId] = ManaProduction(color = colorUsed, amount = source.manaAmount)
                    useSource(source, colorUsed)
                }
                is ManaSymbol.Phyrexian -> {
                    // Try bonus mana first
                    if (spendBonusMana(symbol.color)) continue

                    // For now, always pay with mana (not life)
                    val source = findBestSourceForColor(remainingSources, symbol.color, handRequirements, availableSourcesByColor)
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

            // For generic costs, use the first available color or colorless
            val colorToUse = source.producesColors.firstOrNull()
            manaProduced[source.entityId] = if (colorToUse != null) {
                ManaProduction(color = colorToUse, amount = source.manaAmount)
            } else {
                ManaProduction(colorless = source.manaAmount)
            }
            useSource(source, colorToUse)
            genericRemaining--
        }

        return ManaSolution(usedSources, manaProduced)
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
            val cardDef = cardRegistry?.getCard(card.cardDefinitionId) ?: continue

            // Get mana cost
            val manaCost = cardDef.manaCost ?: continue

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
    internal fun findAvailableManaSources(state: GameState, playerId: EntityId): List<ManaSource> {
        // Project state once to get all keywords and projected controllers
        val projected = stateProjector.project(state)

        // Use projected controller to find all permanents controlled by this player
        // (accounts for control-changing effects like Annex)
        val battlefieldCards = projected.getBattlefieldControlledBy(playerId)

        return battlefieldCards.mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null

            // Must be untapped
            if (container.has<TappedComponent>()) return@mapNotNull null

            val card = container.get<CardComponent>() ?: return@mapNotNull null

            // Check for explicit mana abilities via CardRegistry
            val cardDef = cardRegistry?.getCard(card.cardDefinitionId)
            val allAbilities = cardDef?.script?.activatedAbilities ?: emptyList()
            val manaAbilities = allAbilities.filter { it.isManaAbility }

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
                        isCreature = isCreature,
                        hasNonManaAbilities = hasNonManaAbilities,
                        hasPainCost = false,
                        painAmount = 0,
                        canAttack = canAttack
                    )
                }
            }

            // Try to find a tap-based mana ability
            for (ability in manaAbilities) {
                // Detect pain cost in mana abilities
                var hasPainCost = false
                var painAmount = 0
                val abilityCanBeUsed = when (val cost = ability.cost) {
                    is AbilityCost.Tap -> true
                    is AbilityCost.PayLife -> {
                        hasPainCost = true
                        painAmount = cost.amount
                        true
                    }
                    is AbilityCost.Composite -> {
                        var hasTap = false
                        for (subCost in cost.costs) {
                            when (subCost) {
                                is AbilityCost.Tap -> hasTap = true
                                is AbilityCost.PayLife -> {
                                    hasPainCost = true
                                    painAmount = maxOf(painAmount, subCost.amount)
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

                // Extract production from effect
                return@mapNotNull when (val effect = ability.effect) {
                    is AddManaEffect -> {
                        val manaAmount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        ManaSource(
                            entityId = entityId,
                            name = card.name,
                            producesColors = setOf(effect.color),
                            producesColorless = false,
                            isBasicLand = isBasicLand,
                            isCreature = isCreature,
                            hasNonManaAbilities = hasNonManaAbilities,
                            hasPainCost = hasPainCost,
                            painAmount = painAmount,
                            canAttack = canAttack,
                            manaAmount = manaAmount
                        )
                    }
                    is AddColorlessManaEffect -> {
                        val manaAmount = (effect.amount as? DynamicAmount.Fixed)?.amount ?: 1
                        ManaSource(
                            entityId = entityId,
                            name = card.name,
                            producesColors = emptySet(),
                            producesColorless = true,
                            isBasicLand = isBasicLand,
                            isCreature = isCreature,
                            hasNonManaAbilities = hasNonManaAbilities,
                            hasPainCost = hasPainCost,
                            painAmount = painAmount,
                            canAttack = canAttack,
                            manaAmount = manaAmount
                        )
                    }
                    is AddAnyColorManaEffect -> ManaSource(
                        entityId = entityId,
                        name = card.name,
                        producesColors = Color.entries.toSet(),
                        producesColorless = false,
                        isBasicLand = isBasicLand,
                        isCreature = isCreature,
                        hasNonManaAbilities = hasNonManaAbilities,
                        hasPainCost = hasPainCost,
                        painAmount = painAmount,
                        canAttack = canAttack
                    )
                    else -> null // Unknown effect type, skip this ability
                }
            }

            // Fall back to land subtype logic for lands without explicit abilities
            // (lands without basic land subtypes that also have no explicit mana abilities
            // produce colorless mana, e.g., Wastes)
            if (!card.typeLine.isLand) return@mapNotNull null

            ManaSource(
                entityId = entityId,
                name = card.name,
                producesColors = emptySet(),
                producesColorless = true,
                isBasicLand = isBasicLand,
                isCreature = false,
                hasNonManaAbilities = hasNonManaAbilities,
                hasPainCost = false,
                painAmount = 0,
                canAttack = false
            )
        }.map { source -> augmentWithAuraBonusMana(state, source, playerId) }
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
        if (cardRegistry == null) return source

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
     * Finds the best source to produce a specific color.
     * Uses priority-based selection to pick the optimal source.
     */
    private fun findBestSourceForColor(
        sources: List<ManaSource>,
        color: Color,
        handRequirements: Map<Color, Int>,
        availableSourcesByColor: Map<Color, Int>
    ): ManaSource? {
        return sources
            .filter { it.producesColors.contains(color) }
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
        xValue: Int = 0
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
                colorless = poolComponent.colorless
            )
        } else {
            ManaPool()
        }

        // Pay partial from pool for the base cost
        val partialResult = pool.payPartial(cost)
        val remainingCost = partialResult.remainingCost
        val poolAfterPartial = partialResult.newPool

        // Calculate how much X can be paid from remaining pool
        val xPaidFromPool = poolAfterPartial.total.coerceAtMost(xValue)
        val xRemainingToPay = xValue - xPaidFromPool

        // If nothing remains after using pool (including X), we can pay
        if (remainingCost.isEmpty() && xRemainingToPay == 0) {
            return true
        }

        // Check if we can tap sources for the remaining cost (including remaining X)
        return solve(state, playerId, remainingCost, xRemainingToPay) != null
    }

    /**
     * Gets the total available mana for a player (floating mana + untapped sources).
     */
    fun getAvailableManaCount(state: GameState, playerId: EntityId): Int {
        // Count floating mana
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>()
        val floatingMana = if (poolComponent != null) {
            poolComponent.white + poolComponent.blue + poolComponent.black +
                poolComponent.red + poolComponent.green + poolComponent.colorless
        } else {
            0
        }

        // Add untapped mana sources (including bonus mana from auras and multi-mana sources)
        return floatingMana + findAvailableManaSources(state, playerId).sumOf { it.manaAmount + it.bonusManaPerTap }
    }
}
