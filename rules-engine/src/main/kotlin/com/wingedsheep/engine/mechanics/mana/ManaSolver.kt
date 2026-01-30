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
    val producesColorless: Boolean = false
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

        // Track which sources we've used
        val usedSources = mutableListOf<ManaSource>()
        val manaProduced = mutableMapOf<EntityId, ManaProduction>()
        var remainingSources = availableSources.toMutableList()

        // 1. Pay colored costs first (most constrained)
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    val source = findBestSourceForColor(remainingSources, symbol.color)
                        ?: return null // Can't pay this colored cost

                    usedSources.add(source)
                    manaProduced[source.entityId] = ManaProduction(color = symbol.color)
                    remainingSources.remove(source)
                }
                is ManaSymbol.Hybrid -> {
                    // Try first color, then second
                    val source = findBestSourceForColor(remainingSources, symbol.color1)
                        ?: findBestSourceForColor(remainingSources, symbol.color2)
                        ?: return null

                    val colorUsed = if (source.producesColors.contains(symbol.color1))
                        symbol.color1 else symbol.color2
                    usedSources.add(source)
                    manaProduced[source.entityId] = ManaProduction(color = colorUsed)
                    remainingSources.remove(source)
                }
                is ManaSymbol.Phyrexian -> {
                    // For now, always pay with mana (not life)
                    val source = findBestSourceForColor(remainingSources, symbol.color)
                        ?: return null

                    usedSources.add(source)
                    manaProduced[source.entityId] = ManaProduction(color = symbol.color)
                    remainingSources.remove(source)
                }
                is ManaSymbol.Colorless -> {
                    // Must pay with actual colorless mana (from Wastes, etc.)
                    val source = remainingSources.find { it.producesColorless }
                        ?: return null

                    usedSources.add(source)
                    manaProduced[source.entityId] = ManaProduction(colorless = 1)
                    remainingSources.remove(source)
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

            // Prefer sources that only produce one color (or colorless)
            // to preserve flexibility of multi-color lands
            val source = remainingSources.minByOrNull { it.producesColors.size }
                ?: return null

            usedSources.add(source)
            // For generic costs, use the first available color or colorless
            val colorToUse = source.producesColors.firstOrNull()
            manaProduced[source.entityId] = if (colorToUse != null) {
                ManaProduction(color = colorToUse)
            } else {
                ManaProduction(colorless = 1)
            }
            remainingSources.remove(source)
        }

        return ManaSolution(usedSources, manaProduced)
    }

    /**
     * Finds all untapped mana sources controlled by a player.
     * Supports:
     * - Basic lands and lands with basic land subtypes
     * - Non-land permanents with explicit tap mana abilities (mana dorks, mana rocks)
     * - Respects summoning sickness for creatures (unless they have haste)
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
            val manaAbilities = cardDef?.script?.activatedAbilities?.filter { it.isManaAbility } ?: emptyList()

            // Try to find a tap-based mana ability
            for (ability in manaAbilities) {
                // Only consider tap abilities for auto-pay
                if (ability.cost != AbilityCost.Tap) continue

                // Check summoning sickness for creatures (non-lands)
                if (!card.typeLine.isLand && card.typeLine.isCreature) {
                    val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                    // Use projected keywords to check for Haste (includes granted abilities)
                    val hasHaste = projected.hasKeyword(entityId, Keyword.HASTE)
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
                        producesColorless = false
                    )
                    is AddColorlessManaEffect -> ManaSource(
                        entityId = entityId,
                        name = card.name,
                        producesColors = emptySet(),
                        producesColorless = true
                    )
                    is AddAnyColorManaEffect -> ManaSource(
                        entityId = entityId,
                        name = card.name,
                        producesColors = Color.entries.toSet(),
                        producesColorless = false
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
                producesColorless = producesColorless
            )
        }
    }

    /**
     * Finds the best source to produce a specific color.
     * Prefers sources that ONLY produce that color (to preserve multi-land flexibility).
     */
    private fun findBestSourceForColor(sources: List<ManaSource>, color: Color): ManaSource? {
        // First, try to find a source that only produces this color
        val singleColorSource = sources.find {
            it.producesColors == setOf(color)
        }
        if (singleColorSource != null) return singleColorSource

        // Otherwise, any source that can produce this color
        return sources.find { it.producesColors.contains(color) }
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

        // Check if pool alone can pay
        if (pool.canPay(cost)) {
            return true
        }

        // Pay partial from pool and check if remaining can be tapped for
        val partialResult = pool.payPartial(cost)
        val remainingCost = partialResult.remainingCost

        // If nothing remains after using pool, we can pay
        if (remainingCost.isEmpty()) {
            return true
        }

        // Check if we can tap sources for the remaining cost
        return solve(state, playerId, remainingCost, xValue) != null
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
