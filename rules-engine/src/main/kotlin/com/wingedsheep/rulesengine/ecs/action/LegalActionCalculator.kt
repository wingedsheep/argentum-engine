package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.ability.AbilityRegistry
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.layers.GameObjectView
import com.wingedsheep.rulesengine.ecs.layers.ModifierProvider
import com.wingedsheep.rulesengine.ecs.layers.ProjectionCache
import com.wingedsheep.rulesengine.ecs.layers.StateProjector
import com.wingedsheep.rulesengine.game.Step
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Calculates legal actions for a player in the current game state.
 *
 * This is the core component that determines what a player can do at any given moment.
 * It considers:
 * - Current phase/step
 * - Priority
 * - Available mana
 * - Cards in hand
 * - Permanents on battlefield
 * - Stack state
 *
 * Usage:
 * ```kotlin
 * val calculator = LegalActionCalculator()
 * val legalActions = calculator.calculateLegalActions(state, playerId, abilityRegistry)
 * // Present legalActions to player, player chooses one, execute it
 * ```
 */
class LegalActionCalculator {

    /**
     * Calculate all legal actions for a player in the current game state.
     *
     * @param state The current game state
     * @param playerId The player to calculate actions for
     * @param abilityRegistry Optional registry for looking up abilities
     * @param projector Optional StateProjector for using projected state (layer-modified keywords/P/T).
     *                  If not provided, a default projector with no modifiers will be used.
     * @param cache Optional ProjectionCache for caching projected views across multiple queries.
     *              When provided, dramatically improves performance for complex board states.
     * @param modifierProvider Optional ModifierProvider for collecting continuous effects.
     * @return A LegalActions object containing all available actions
     */
    fun calculateLegalActions(
        state: GameState,
        playerId: EntityId,
        abilityRegistry: AbilityRegistry? = null,
        projector: StateProjector? = null,
        cache: ProjectionCache? = null,
        modifierProvider: ModifierProvider? = null
    ): LegalActions {
        // Always can pass priority if it's this player's priority
        val hasPriority = hasActivePriority(state, playerId)

        // Use provided projector, or get one from cache, or create a default one
        val effectiveProjector = projector
            ?: cache?.getProjector(state, modifierProvider)
            ?: StateProjector(state)

        // Wrap projector in a cache-aware accessor if cache is provided
        val viewAccessor: (EntityId) -> GameObjectView? = if (cache != null) {
            { entityId -> cache.getView(state, entityId, modifierProvider) }
        } else {
            { entityId -> effectiveProjector.getView(entityId) }
        }

        return LegalActions(
            playerId = playerId,
            canPassPriority = hasPriority,
            playableLands = if (hasPriority) getPlayableLands(state, playerId) else emptyList(),
            castableSpells = if (hasPriority) getCastableSpells(state, playerId) else emptyList(),
            activatableAbilities = if (hasPriority) getActivatableAbilities(state, playerId, abilityRegistry) else emptyList(),
            declarableAttackers = getDeclarableAttackers(state, playerId, viewAccessor),
            declarableBlockers = getDeclarableBlockers(state, playerId, viewAccessor)
        )
    }

    /**
     * Check if a player currently has priority (can take actions).
     */
    private fun hasActivePriority(state: GameState, playerId: EntityId): Boolean {
        // In a two-player game, the active player has priority first
        // After they pass, the non-active player gets priority
        // For simplicity, assume the active player has priority unless specified otherwise
        return state.activePlayerId == playerId || state.priorityPlayerId == playerId
    }

    /**
     * Get all lands in hand that can be played this turn.
     */
    private fun getPlayableLands(state: GameState, playerId: EntityId): List<PlayableLand> {
        val step = state.turnState.step

        // Can only play lands during main phases on your turn
        if (state.activePlayerId != playerId) return emptyList()
        if (!step.isMainPhase) return emptyList()

        // Check if player has already played a land this turn
        val landsPlayed = state.getComponent<LandsPlayedComponent>(playerId) ?: LandsPlayedComponent()
        if (!landsPlayed.canPlayLand) return emptyList()

        // Stack must be empty to play lands
        if (state.getStack().isNotEmpty()) return emptyList()

        // Find all lands in hand
        val hand = state.getHand(playerId)
        return hand.mapNotNull { cardId ->
            val container = state.getEntity(cardId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null

            if (cardComponent.definition.isLand) {
                PlayableLand(
                    cardId = cardId,
                    cardName = cardComponent.definition.name,
                    action = PlayLand(cardId, playerId)
                )
            } else null
        }
    }

    /**
     * Get all spells in hand that can be cast.
     */
    private fun getCastableSpells(state: GameState, playerId: EntityId): List<CastableSpell> {
        val step = state.turnState.step
        val isActivePlayer = state.activePlayerId == playerId

        // Find all non-land cards in hand
        val hand = state.getHand(playerId)
        return hand.mapNotNull { cardId ->
            val container = state.getEntity(cardId) ?: return@mapNotNull null
            val cardComponent = container.get<CardComponent>() ?: return@mapNotNull null
            val def = cardComponent.definition

            // Skip lands
            if (def.isLand) return@mapNotNull null

            // Check timing restrictions
            val canCastNow = when {
                // Creatures, sorceries, enchantments, etc. - only during main phase on your turn with empty stack
                def.isCreature || def.isSorcery || def.isEnchantment || def.isArtifact -> {
                    isActivePlayer && step.isMainPhase && state.getStack().isEmpty()
                }
                // Instants - can cast anytime with priority
                def.isInstant -> true
                else -> false
            }

            if (!canCastNow) return@mapNotNull null

            // Check if player can pay the mana cost (simplified - just check total available mana)
            val availableMana = countAvailableMana(state, playerId)

            // For now, just check if total available mana is sufficient
            // A full implementation would check color requirements
            if (availableMana < def.cmc) return@mapNotNull null

            CastableSpell(
                cardId = cardId,
                cardName = def.name,
                manaCost = def.manaCost.toString(),
                isCreature = def.isCreature,
                requiresTargets = def.oracleText.contains("target"),
                action = CastSpell(
                    cardId = cardId,
                    casterId = playerId,
                    fromZone = ZoneId(ZoneType.HAND, playerId)
                )
            )
        }
    }

    /**
     * Count total available mana a player can produce by tapping lands.
     */
    private fun countAvailableMana(state: GameState, playerId: EntityId): Int {
        // Count untapped lands controlled by player
        return state.getBattlefield().count { entityId ->
            val container = state.getEntity(entityId) ?: return@count false
            val cardComponent = container.get<CardComponent>() ?: return@count false
            val controllerComponent = container.get<ControllerComponent>() ?: return@count false
            val isTapped = container.has<TappedComponent>()

            controllerComponent.controllerId == playerId &&
                cardComponent.definition.isLand &&
                !isTapped
        }
    }

    /**
     * Get all activatable abilities (mana abilities, activated abilities).
     */
    private fun getActivatableAbilities(
        state: GameState,
        playerId: EntityId,
        abilityRegistry: AbilityRegistry?
    ): List<ActivatableAbility> {
        val abilities = mutableListOf<ActivatableAbility>()

        // Find all permanents controlled by player
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val controllerComponent = container.get<ControllerComponent>() ?: continue
            if (controllerComponent.controllerId != playerId) continue

            val cardComponent = container.get<CardComponent>() ?: continue
            val isTapped = container.has<TappedComponent>()

            // Mana abilities from lands (tap for mana)
            if (cardComponent.definition.isLand && !isTapped) {
                abilities.add(
                    ActivatableAbility(
                        sourceId = entityId,
                        sourceName = cardComponent.definition.name,
                        description = "Tap for mana",
                        isManaAbility = true,
                        action = ActivateManaAbility(
                            sourceEntityId = entityId,
                            abilityIndex = 0,
                            playerId = playerId
                        )
                    )
                )
            }

            // Other activated abilities would be looked up from abilityRegistry
            // For now, we just handle basic mana abilities
        }

        return abilities
    }

    /**
     * Get all creatures that can be declared as attackers.
     * Uses the view accessor to get layer-modified keywords and P/T values.
     *
     * @param viewAccessor Function to get projected views, allowing cache usage
     */
    private fun getDeclarableAttackers(
        state: GameState,
        playerId: EntityId,
        viewAccessor: (EntityId) -> GameObjectView?
    ): List<DeclarableAttacker> {
        val step = state.turnState.step

        // Can only declare attackers during declare attackers step on your turn
        if (state.activePlayerId != playerId) return emptyList()
        if (step != Step.DECLARE_ATTACKERS) return emptyList()

        // Find all creatures controlled by player that can attack
        return state.getBattlefield().mapNotNull { entityId ->
            // Get the projected view for layer-modified state
            val view = viewAccessor(entityId) ?: return@mapNotNull null

            // Must be a creature controlled by player (using projected values)
            if (!view.isCreature) return@mapNotNull null
            if (view.controllerId != playerId) return@mapNotNull null

            // Must not be tapped
            if (view.isTapped) return@mapNotNull null

            // Must not have summoning sickness (unless has haste) - use projected keywords
            if (view.hasSummoningSickness) {
                if (!view.hasKeyword(Keyword.HASTE)) return@mapNotNull null
            }

            // Must not have defender - use projected keywords
            if (view.hasKeyword(Keyword.DEFENDER)) {
                return@mapNotNull null
            }

            // Must not already be attacking
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            if (container.has<AttackingComponent>()) return@mapNotNull null

            DeclarableAttacker(
                creatureId = entityId,
                creatureName = view.name,
                power = view.power ?: 0,
                toughness = view.toughness ?: 0,
                action = DeclareAttacker(entityId, playerId)
            )
        }
    }

    /**
     * Get all creatures that can be declared as blockers.
     * Uses the view accessor to get layer-modified keywords and check evasion abilities.
     *
     * @param viewAccessor Function to get projected views, allowing cache usage
     */
    private fun getDeclarableBlockers(
        state: GameState,
        playerId: EntityId,
        viewAccessor: (EntityId) -> GameObjectView?
    ): List<DeclarableBlocker> {
        val step = state.turnState.step

        // Can only declare blockers during declare blockers step when you're defending
        if (step != Step.DECLARE_BLOCKERS) return emptyList()

        // Check if this player is the defending player
        val combat = state.combat ?: return emptyList()
        if (combat.defendingPlayer != playerId) return emptyList()

        // Get all attackers that can be blocked (with their projected views)
        val attackerViews = state.getBattlefield().mapNotNull { entityId ->
            val container = state.getEntity(entityId) ?: return@mapNotNull null
            if (!container.has<AttackingComponent>()) return@mapNotNull null
            val view = viewAccessor(entityId) ?: return@mapNotNull null
            entityId to view
        }

        // Find all creatures controlled by player that can block
        return state.getBattlefield().flatMap { blockerId ->
            // Get the projected view for layer-modified state
            val blockerView = viewAccessor(blockerId) ?: return@flatMap emptyList()

            // Must be a creature controlled by player (using projected values)
            if (!blockerView.isCreature) return@flatMap emptyList<DeclarableBlocker>()
            if (blockerView.controllerId != playerId) return@flatMap emptyList<DeclarableBlocker>()

            // Must not be tapped
            if (blockerView.isTapped) return@flatMap emptyList<DeclarableBlocker>()

            // Must not already be blocking
            val container = state.getEntity(blockerId) ?: return@flatMap emptyList<DeclarableBlocker>()
            if (container.has<BlockingComponent>()) return@flatMap emptyList<DeclarableBlocker>()

            // Check if this creature can block (cantBlock restriction from continuous effects)
            if (blockerView.cantBlock) return@flatMap emptyList<DeclarableBlocker>()

            // For each attacker this creature could block, create a blocker option
            attackerViews.mapNotNull { (attackerId, attackerView) ->
                // Check flying/reach evasion (Rule 509.1b)
                // A creature with flying can only be blocked by creatures with flying or reach
                if (attackerView.hasKeyword(Keyword.FLYING) && !blockerView.canBlockFlying) {
                    return@mapNotNull null
                }

                // Check shadow evasion (if implemented)
                // A creature with shadow can only be blocked by creatures with shadow,
                // and can only block creatures with shadow
                if (attackerView.hasKeyword(Keyword.SHADOW) && !blockerView.hasKeyword(Keyword.SHADOW)) {
                    return@mapNotNull null
                }
                if (blockerView.hasKeyword(Keyword.SHADOW) && !attackerView.hasKeyword(Keyword.SHADOW)) {
                    return@mapNotNull null
                }

                DeclarableBlocker(
                    blockerId = blockerId,
                    blockerName = blockerView.name,
                    attackerId = attackerId,
                    action = DeclareBlocker(blockerId, attackerId, playerId)
                )
            }
        }
    }
}

/**
 * Represents all legal actions available to a player.
 */
data class LegalActions(
    val playerId: EntityId,
    val canPassPriority: Boolean,
    val playableLands: List<PlayableLand>,
    val castableSpells: List<CastableSpell>,
    val activatableAbilities: List<ActivatableAbility>,
    val declarableAttackers: List<DeclarableAttacker>,
    val declarableBlockers: List<DeclarableBlocker>
) {
    /**
     * Check if the player has any actions available.
     */
    fun hasActions(): Boolean =
        canPassPriority ||
            playableLands.isNotEmpty() ||
            castableSpells.isNotEmpty() ||
            activatableAbilities.isNotEmpty() ||
            declarableAttackers.isNotEmpty() ||
            declarableBlockers.isNotEmpty()

    /**
     * Get all actions as a flat list.
     */
    fun allActions(): List<GameAction> = buildList {
        if (canPassPriority) add(PassPriority(playerId))
        addAll(playableLands.map { it.action })
        addAll(castableSpells.map { it.action })
        addAll(activatableAbilities.map { it.action })
        addAll(declarableAttackers.map { it.action })
        addAll(declarableBlockers.map { it.action })
    }
}

/**
 * A land that can be played from hand.
 */
data class PlayableLand(
    val cardId: EntityId,
    val cardName: String,
    val action: PlayLand
)

/**
 * A spell that can be cast from hand.
 */
data class CastableSpell(
    val cardId: EntityId,
    val cardName: String,
    val manaCost: String,
    val isCreature: Boolean,
    val requiresTargets: Boolean,
    val action: CastSpell
)

/**
 * An ability that can be activated.
 */
data class ActivatableAbility(
    val sourceId: EntityId,
    val sourceName: String,
    val description: String,
    val isManaAbility: Boolean,
    val action: GameAction
)

/**
 * A creature that can be declared as an attacker.
 */
data class DeclarableAttacker(
    val creatureId: EntityId,
    val creatureName: String,
    val power: Int,
    val toughness: Int,
    val action: DeclareAttacker
)

/**
 * A creature that can be declared as a blocker for a specific attacker.
 */
data class DeclarableBlocker(
    val blockerId: EntityId,
    val blockerName: String,
    val attackerId: EntityId,
    val action: DeclareBlocker
)
