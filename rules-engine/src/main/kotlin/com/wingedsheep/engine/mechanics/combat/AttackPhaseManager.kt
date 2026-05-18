package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackedThisTurnComponent
import com.wingedsheep.engine.state.components.combat.PlayerAttackersThisTurnComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.mechanics.combat.rules.AttackCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.AttackDefenderRule
import com.wingedsheep.engine.mechanics.combat.rules.AttackRestrictionRule
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AttackTax

/**
 * Handles the declare attackers step of combat.
 *
 * Responsibilities:
 * - Validating individual attackers via [AttackRestrictionRule] and [AttackDefenderRule]
 * - Must-attack requirements (Taunt, Walking Desecration, Grand Melee)
 * - Attack taxes (Ghostly Prison, Windborn Muse, Whipgrass Entangler)
 * - Applying attacker components and tapping
 */
internal class AttackPhaseManager(
    private val cardRegistry: CardRegistry,
    private val attackRestrictionRules: List<AttackRestrictionRule>,
    private val attackDefenderRules: List<AttackDefenderRule>,
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
) {

    private val dynamicAmountEvaluator = com.wingedsheep.engine.handlers.DynamicAmountEvaluator()

    /**
     * Validate and declare attackers.
     *
     * @param attackers Map of attacker entity ID to defender (player or planeswalker)
     */
    fun declareAttackers(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): ExecutionResult {
        // Validate each attacker
        val projected = state.projectedState
        val opponents = state.turnOrder.filter { it != attackingPlayer }
        for ((attackerId, defenderId) in attackers) {
            val validation = validateAttacker(state, attackingPlayer, attackerId)
            if (validation != null) {
                return ExecutionResult.error(state, validation)
            }
            // Validate defender is either an opponent player or a planeswalker controlled by an opponent
            if (defenderId !in opponents) {
                if (!projected.isPlaneswalker(defenderId) || projected.getController(defenderId) in listOf(attackingPlayer)) {
                    return ExecutionResult.error(state, "Invalid attack target: must be an opponent or their planeswalker")
                }
                if (defenderId !in state.getBattlefield()) {
                    return ExecutionResult.error(state, "Planeswalker is not on the battlefield")
                }
            }
            // Check per-defender restrictions (CantAttackUnless, CantBeAttackedWithout, etc.)
            val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
            for (rule in attackDefenderRules) {
                val error = rule.check(ctx, defenderId)
                if (error != null) {
                    return ExecutionResult.error(state, error)
                }
            }
        }

        // Check must-attack requirements (Taunt)
        val mustAttackValidation = validateMustAttackRequirements(state, attackingPlayer, attackers)
        if (mustAttackValidation != null) {
            return ExecutionResult.error(state, mustAttackValidation)
        }

        // Check must-attack-this-turn requirements (Walking Desecration)
        val mustAttackThisTurnValidation = validateMustAttackThisTurnRequirements(state, attackingPlayer, attackers)
        if (mustAttackThisTurnValidation != null) {
            return ExecutionResult.error(state, mustAttackThisTurnValidation)
        }

        // Check projected must-attack requirements (Grand Melee)
        val projectedMustAttackValidation = validateProjectedMustAttackRequirements(state, attackingPlayer, attackers)
        if (projectedMustAttackValidation != null) {
            return ExecutionResult.error(state, projectedMustAttackValidation)
        }

        // Calculate (but don't pay) the attack tax. If non-zero, pause for the attacking
        // player to confirm before we tap any of their mana — otherwise auto-tapping the
        // pool would steal sources they were saving for instants/post-combat plays.
        val totalTax = calculateTotalAttackTax(state, attackingPlayer, attackers, projected)
        if (totalTax > 0) {
            return pauseForAttackTaxConfirmation(state, attackingPlayer, attackers, totalTax)
        }

        return commitAttackDeclaration(state, attackingPlayer, attackers, projected, taxEvents = emptyList())
    }

    /**
     * Apply the post-tax commitment for a declared attack: stamp [AttackingComponent],
     * tap attackers (unless vigilance), mark tracking components, and emit the
     * [AttackersDeclaredEvent]. Callable both from the synchronous (no-tax) path in
     * [declareAttackers] and from [com.wingedsheep.engine.handlers.continuations.CombatTaxContinuationResumer]
     * after the player confirms and the tax is paid.
     *
     * @param taxEvents Events from the tax payment (auto-tap [TappedEvent]s, etc.) to emit
     *   before the [AttackersDeclaredEvent].
     */
    internal fun commitAttackDeclaration(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState,
        taxEvents: List<com.wingedsheep.engine.core.GameEvent>
    ): ExecutionResult {
        var newState = state
        val tapEvents = mutableListOf<TappedEvent>()
        for ((attackerId, defenderId) in attackers) {
            val hasVigilance = projected.hasKeyword(attackerId, Keyword.VIGILANCE)
            newState = newState.updateEntity(attackerId) { container ->
                var updated = container.with(AttackingComponent(defenderId))
                if (!hasVigilance) {
                    updated = updated.with(TappedComponent)
                }
                updated
            }
            if (!hasVigilance) {
                val attackerName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                tapEvents.add(TappedEvent(attackerId, attackerName))
            }
        }

        newState = newState.updateEntity(attackingPlayer) { container ->
            var updated = container.with(AttackersDeclaredThisCombatComponent)
            if (attackers.isNotEmpty()) {
                updated = updated.with(PlayerAttackedThisTurnComponent)
                val previous = container.get<PlayerAttackersThisTurnComponent>()?.attackerIds ?: emptySet()
                updated = updated.with(PlayerAttackersThisTurnComponent(previous + attackers.keys))
            }
            updated
        }

        val attackerNames = attackers.keys.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Creature" }
        return ExecutionResult.success(
            newState,
            taxEvents + tapEvents + listOf(AttackersDeclaredEvent(attackers.keys.toList(), attackerNames, attackingPlayer))
        )
    }

    private fun pauseForAttackTaxConfirmation(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        totalTax: Int,
    ): ExecutionResult {
        val manaCost = com.wingedsheep.sdk.core.ManaCost(
            List(totalTax) { com.wingedsheep.sdk.core.ManaSymbol.generic(1) }
        )
        val manaSolver = com.wingedsheep.engine.mechanics.mana.ManaSolver(cardRegistry)
        val sources = manaSolver.findAvailableManaSources(state, attackingPlayer)
        val sourceOptions = sources.map { source ->
            com.wingedsheep.engine.core.ManaSourceOption(
                entityId = source.entityId,
                name = source.name,
                producesColors = source.producesColors,
                producesColorless = source.producesColorless,
                requiresSacrifice = source.requiresSacrifice,
                requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null,
            )
        }
        val solution = manaSolver.solve(state, attackingPlayer, manaCost)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()

        val decisionId = java.util.UUID.randomUUID().toString()
        val attackerNames = attackers.keys.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }
        val attackerListing = when (attackerNames.size) {
            0 -> "your attackers"
            1 -> attackerNames.single()
            else -> attackerNames.dropLast(1).joinToString(", ") + " and " + attackerNames.last()
        }
        val decision = com.wingedsheep.engine.core.SelectManaSourcesDecision(
            id = decisionId,
            playerId = attackingPlayer,
            prompt = "Pay {$totalTax} to attack with $attackerListing",
            context = com.wingedsheep.engine.core.DecisionContext(
                sourceId = null,
                sourceName = "Attack tax",
                phase = com.wingedsheep.engine.core.DecisionPhase.COMBAT,
            ),
            availableSources = sourceOptions,
            requiredCost = manaCost.toString(),
            autoPaySuggestion = autoPaySuggestion,
            canDecline = true,
        )
        val continuation = com.wingedsheep.engine.core.AttackTaxManaSelectionContinuation(
            decisionId = decisionId,
            attackingPlayer = attackingPlayer,
            attackers = attackers,
            manaCost = manaCost,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion,
        )
        return ExecutionResult.paused(
            state.withPendingDecision(decision).pushContinuation(continuation),
            decision,
        )
    }

    /**
     * Check if a creature passes all per-creature attack restrictions.
     * Does NOT check per-defender restrictions.
     */
    fun isValidAttacker(state: GameState, attackerId: EntityId, attackingPlayer: EntityId): Boolean {
        val projected = state.projectedState
        val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
        return attackRestrictionRules.all { it.check(ctx) == null }
    }

    /**
     * Check if a creature is restricted from attacking all opponents by per-defender rules.
     * Returns true if the creature cannot attack any opponent.
     */
    fun isRestrictedFromAllDefenders(state: GameState, attackerId: EntityId, attackingPlayer: EntityId): Boolean {
        val projected = state.projectedState
        val ctx = AttackCheckContext(state, projected, attackerId, attackingPlayer, cardRegistry)
        return attackDefenderRules.any { it.restrictsAllDefenders(ctx) }
    }

    // =========================================================================
    // Private validation
    // =========================================================================

    /**
     * Validate that a creature can attack.
     * Delegates to registered [AttackRestrictionRule] instances.
     */
    private fun validateAttacker(
        state: GameState,
        attackingPlayer: EntityId,
        attackerId: EntityId
    ): String? {
        state.getEntity(attackerId) ?: return "Attacker not found: $attackerId"
        state.getEntity(attackerId)?.get<CardComponent>() ?: return "Not a card: $attackerId"

        val ctx = AttackCheckContext(
            state = state,
            projected = state.projectedState,
            attackerId = attackerId,
            attackingPlayer = attackingPlayer,
            cardRegistry = cardRegistry
        )
        for (rule in attackRestrictionRules) {
            val error = rule.check(ctx)
            if (error != null) return error
        }
        return null
    }

    /**
     * Validate "must attack" requirements (Taunt effect).
     */
    private fun validateMustAttackRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val mustAttack = state.getEntity(attackingPlayer)?.get<MustAttackPlayerComponent>()
            ?: return null

        if (!mustAttack.activeThisTurn) {
            return null
        }

        val requiredDefender = mustAttack.defenderId
        val validAttackers = getValidAttackers(state, attackingPlayer)

        for (attackerId in validAttackers) {
            if (attackerId !in attackers.keys) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this turn (Taunt)"
            }
        }

        for ((attackerId, defenderId) in attackers) {
            if (defenderId != requiredDefender) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                val defenderName = state.getEntity(requiredDefender)?.get<CardComponent>()?.name ?: "that player"
                return "$cardName must attack $defenderName (Taunt)"
            }
        }

        return null
    }

    /**
     * Validate "must attack this turn" requirements for individual creatures.
     */
    private fun validateMustAttackThisTurnRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)

        for (attackerId in validAttackers) {
            val container = state.getEntity(attackerId) ?: continue
            if (!container.has<MustAttackThisTurnComponent>()) continue

            if (attackerId !in attackers.keys) {
                val cardName = container.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this turn"
            }
        }

        return null
    }

    /**
     * Validate projected "must attack" requirements (e.g., from Grand Melee).
     */
    private fun validateProjectedMustAttackRequirements(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>
    ): String? {
        val validAttackers = getValidAttackers(state, attackingPlayer)
        val projected = state.projectedState

        for (attackerId in validAttackers) {
            if (!projected.mustAttack(attackerId)) continue

            if (attackerId !in attackers.keys) {
                val cardName = state.getEntity(attackerId)?.get<CardComponent>()?.name ?: "Creature"
                return "$cardName must attack this combat if able"
            }
        }

        return null
    }

    /**
     * Get all creatures that can legally attack for a player.
     */
    private fun getValidAttackers(state: GameState, playerId: EntityId): List<EntityId> {
        val projected = state.projectedState

        return state.getBattlefield().filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>() ?: return@filter false

            val ctx = AttackCheckContext(state, projected, entityId, playerId, cardRegistry)

            if (attackRestrictionRules.any { it.check(ctx) != null }) return@filter false
            if (attackDefenderRules.any { it.restrictsAllDefenders(ctx) }) return@filter false

            true
        }
    }

    /**
     * Get creatures that must attack this combat (for UI pre-selection).
     * Includes creatures required by MustAttackPlayerComponent, MustAttackThisTurnComponent,
     * and projected mustAttack (e.g., from static MustAttack ability like Valley Dasher).
     */
    fun getMandatoryAttackers(state: GameState, attackingPlayer: EntityId): List<EntityId> {
        val validAttackers = getValidAttackers(state, attackingPlayer)
        val projected = state.projectedState
        val mandatory = mutableSetOf<EntityId>()

        // 1. MustAttackPlayerComponent (Taunt effect) — all valid attackers must attack
        val mustAttackPlayer = state.getEntity(attackingPlayer)?.get<MustAttackPlayerComponent>()
        if (mustAttackPlayer != null && mustAttackPlayer.activeThisTurn) {
            mandatory.addAll(validAttackers)
        }

        // 2. MustAttackThisTurnComponent (Walking Desecration) — individual creatures
        for (attackerId in validAttackers) {
            val container = state.getEntity(attackerId) ?: continue
            if (container.has<MustAttackThisTurnComponent>()) {
                mandatory.add(attackerId)
            }
        }

        // 3. Projected mustAttack (static ability like Valley Dasher, Grand Melee)
        for (attackerId in validAttackers) {
            if (projected.mustAttack(attackerId)) {
                mandatory.add(attackerId)
            }
        }

        return mandatory.toList()
    }

    // =========================================================================
    // Attack Taxes
    // =========================================================================

    /**
     * Compute the total generic-mana attack tax owed for [attackers] without paying it.
     * Used by [declareAttackers] to decide whether to pause for player confirmation
     * before tapping any mana.
     */
    internal fun calculateTotalAttackTax(
        state: GameState,
        attackingPlayer: EntityId,
        attackers: Map<EntityId, EntityId>,
        projected: ProjectedState
    ): Int {
        if (attackers.isEmpty()) return 0
        val attackersPerDefender = mutableMapOf<EntityId, Int>()
        for ((_, defenderId) in attackers) {
            val defenderPlayerId = if (state.turnOrder.contains(defenderId)) {
                defenderId
            } else {
                projected.getController(defenderId)
            }
            if (defenderPlayerId != null) {
                attackersPerDefender[defenderPlayerId] = (attackersPerDefender[defenderPlayerId] ?: 0) + 1
            }
        }

        var totalGenericTax = 0
        for ((defenderId, attackerCount) in attackersPerDefender) {
            val defenderPermanents = projected.getBattlefieldControlledBy(defenderId)
            for (entityId in defenderPermanents) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
                for (ability in cardDef.staticAbilities) {
                    if (ability is AttackTax) {
                        val ctx = com.wingedsheep.engine.handlers.EffectContext(
                            sourceId = entityId,
                            controllerId = defenderId,
                            opponentId = attackingPlayer,
                        )
                        val taxPerAttacker = maxOf(0, dynamicAmountEvaluator.evaluate(state, ability.amountPerAttacker, ctx, projected))
                        totalGenericTax += taxPerAttacker * attackerCount
                    }
                }
            }
        }

        totalGenericTax += calculatePerCreatureTax(state, attackers.keys, projected)
        return totalGenericTax
    }

    /**
     * Calculate per-creature tax from AttackBlockTaxPerCreatureType floating effects.
     */
    private fun calculatePerCreatureTax(
        state: GameState,
        creatureIds: Set<EntityId>,
        projected: ProjectedState
    ): Int {
        var totalTax = 0
        for (creatureId in creatureIds) {
            for (floatingEffect in state.floatingEffects) {
                val mod = floatingEffect.effect.modification
                if (mod !is SerializableModification.AttackBlockTaxPerCreatureType) continue
                if (creatureId !in floatingEffect.effect.affectedEntities) continue

                val creatureTypeCount = state.getBattlefield().count { entityId ->
                    projected.isCreature(entityId) && projected.hasSubtype(entityId, mod.creatureType)
                }
                val costPerCreature = ManaCost.parse(mod.manaCostPer).cmc
                totalTax += costPerCreature * creatureTypeCount
            }
        }
        return totalTax
    }
}
