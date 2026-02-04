package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AddAnyColorManaEffect
import com.wingedsheep.sdk.scripting.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.AddManaEffect

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
    val canAttack: Boolean = false
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
    private val stateProjector: StateProjector = StateProjector()
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

        // Helper to update available counts when a source is used
        fun useSource(source: ManaSource) {
            usedSources.add(source)
            remainingSources.remove(source)
            for (color in source.producesColors) {
                availableSourcesByColor[color] = (availableSourcesByColor[color] ?: 1) - 1
            }
        }

        // 1. Pay colored costs first (most constrained)
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    val source = findBestSourceForColor(remainingSources, symbol.color, handRequirements, availableSourcesByColor)
                        ?: return null // Can't pay this colored cost

                    manaProduced[source.entityId] = ManaProduction(color = symbol.color)
                    useSource(source)
                }
                is ManaSymbol.Hybrid -> {
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
                    manaProduced[source.entityId] = ManaProduction(color = colorUsed)
                    useSource(source)
                }
                is ManaSymbol.Phyrexian -> {
                    // For now, always pay with mana (not life)
                    val source = findBestSourceForColor(remainingSources, symbol.color, handRequirements, availableSourcesByColor)
                        ?: return null

                    manaProduced[source.entityId] = ManaProduction(color = symbol.color)
                    useSource(source)
                }
                is ManaSymbol.Colorless -> {
                    // Must pay with actual colorless mana (from Wastes, etc.)
                    // Sort colorless sources by priority
                    val source = remainingSources
                        .filter { it.producesColorless }
                        .minByOrNull { calculateTapPriority(it, handRequirements, availableSourcesByColor) }
                        ?: return null

                    manaProduced[source.entityId] = ManaProduction(colorless = 1)
                    useSource(source)
                }
                is ManaSymbol.Generic, is ManaSymbol.X -> {
                    // Handle in the generic pass below
                }
            }
        }

        // 2. Pay generic costs (and X)
        val genericAmount = cost.genericAmount + xValue
        repeat(genericAmount) {
            if (remainingSources.isEmpty()) {
                return null // Not enough mana
            }

            // Use priority-based selection for generic costs
            val source = remainingSources.minByOrNull { calculateTapPriority(it, handRequirements, availableSourcesByColor) }
                ?: return null

            // For generic costs, use the first available color or colorless
            val colorToUse = source.producesColors.firstOrNull()
            manaProduced[source.entityId] = if (colorToUse != null) {
                ManaProduction(color = colorToUse)
            } else {
                ManaProduction(colorless = 1)
            }
            useSource(source)
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
        val handZone = ZoneKey(playerId, ZoneType.HAND)
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
    private fun findAvailableManaSources(state: GameState, playerId: EntityId): List<ManaSource> {
        val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
        val battlefieldCards = state.getZone(battlefieldZone)

        // Project state once to get all keywords (including granted abilities)
        val projected = stateProjector.project(state)

        return battlefieldCards.mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null

            // Must be untapped
            if (container.has<TappedComponent>()) return@mapNotNull null

            // Must be controlled by the player
            val controller = container.get<ControllerComponent>()?.playerId
            if (controller != playerId) return@mapNotNull null

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
                    is AddManaEffect -> ManaSource(
                        entityId = entityId,
                        name = card.name,
                        producesColors = setOf(effect.color),
                        producesColorless = false,
                        isBasicLand = isBasicLand,
                        isCreature = isCreature,
                        hasNonManaAbilities = hasNonManaAbilities,
                        hasPainCost = hasPainCost,
                        painAmount = painAmount,
                        canAttack = canAttack
                    )
                    is AddColorlessManaEffect -> ManaSource(
                        entityId = entityId,
                        name = card.name,
                        producesColors = emptySet(),
                        producesColorless = true,
                        isBasicLand = isBasicLand,
                        isCreature = isCreature,
                        hasNonManaAbilities = hasNonManaAbilities,
                        hasPainCost = hasPainCost,
                        painAmount = painAmount,
                        canAttack = canAttack
                    )
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
            if (!card.typeLine.isLand) return@mapNotNull null

            // Determine what colors this land produces based on subtypes
            val producesColors = mutableSetOf<Color>()
            val subtypes = card.typeLine.subtypes

            // Basic land types produce their associated color
            if (subtypes.contains(Subtype.PLAINS)) producesColors.add(Color.WHITE)
            if (subtypes.contains(Subtype.ISLAND)) producesColors.add(Color.BLUE)
            if (subtypes.contains(Subtype.SWAMP)) producesColors.add(Color.BLACK)
            if (subtypes.contains(Subtype.MOUNTAIN)) producesColors.add(Color.RED)
            if (subtypes.contains(Subtype.FOREST)) producesColors.add(Color.GREEN)

            // Treat lands without basic land types as colorless producers
            // This handles generic lands, Wastes, and similar cases
            val producesColorless = producesColors.isEmpty()

            if (producesColors.isEmpty() && !producesColorless) {
                return@mapNotNull null
            }

            ManaSource(
                entityId = entityId,
                name = card.name,
                producesColors = producesColors,
                producesColorless = producesColorless,
                isBasicLand = isBasicLand,
                isCreature = false, // Falls back to land subtype logic, so not a creature
                hasNonManaAbilities = hasNonManaAbilities,
                hasPainCost = false,
                painAmount = 0,
                canAttack = false
            )
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

        // Add untapped mana sources
        return floatingMana + findAvailableManaSources(state, playerId).size
    }
}
