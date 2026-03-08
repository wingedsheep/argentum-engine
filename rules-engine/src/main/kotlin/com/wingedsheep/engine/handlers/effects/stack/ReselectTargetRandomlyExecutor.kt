package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ReselectTargetRandomlyEffect
import com.wingedsheep.sdk.scripting.targets.*
import kotlin.reflect.KClass

/**
 * Executor for ReselectTargetRandomlyEffect.
 * "If it has a single target, reselect its target at random."
 *
 * Uses context.triggeringEntityId to find the spell/ability on the stack.
 * If it has exactly one target, finds all legal targets and randomly picks one.
 * If it has zero or multiple targets, does nothing.
 */
class ReselectTargetRandomlyExecutor : EffectExecutor<ReselectTargetRandomlyEffect> {

    override val effectType: KClass<ReselectTargetRandomlyEffect> = ReselectTargetRandomlyEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ReselectTargetRandomlyEffect,
        context: EffectContext
    ): ExecutionResult {
        // 1. Get the triggering spell/ability from context
        val triggeringEntityId = context.triggeringEntityId
            ?: return ExecutionResult.success(state)

        val stackEntity = state.getEntity(triggeringEntityId)
            ?: return ExecutionResult.success(state)

        // 2. Check if it has exactly one target
        val targetsComponent = stackEntity.get<TargetsComponent>()
            ?: return ExecutionResult.success(state)

        val spellTargets = targetsComponent.targets
        if (spellTargets.size != 1) {
            return ExecutionResult.success(state)
        }

        val currentTarget = spellTargets.first()
        val targetRequirements = targetsComponent.targetRequirements

        // 3. Find all legal targets
        val legalTargets = findLegalTargets(state, currentTarget, targetRequirements, context.controllerId)

        if (legalTargets.isEmpty()) {
            // No legal targets at all — keep current target
            return ExecutionResult.success(state)
        }

        // 4. Randomly pick one (may be the same as current — per ruling)
        val chosenTargetId = legalTargets.random()

        // 5. Build the new ChosenTarget based on what was chosen
        val newTarget = buildChosenTarget(state, chosenTargetId, currentTarget)
            ?: return ExecutionResult.success(state)

        // 6. Update the target on the stack entity
        val newTargetsComponent = targetsComponent.copy(
            targets = listOf(newTarget)
        )
        val newState = state.updateEntity(triggeringEntityId) { container ->
            container.with(newTargetsComponent)
        }

        // 7. Emit event for the game log
        val spellName = stackEntity.get<CardComponent>()?.name
            ?: stackEntity.get<com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent>()?.sourceName
            ?: stackEntity.get<com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent>()?.sourceName
            ?: "spell or ability"
        val oldTargetId = getTargetEntityId(currentTarget)
        val newTargetId = getTargetEntityId(newTarget)
        val oldTargetName = oldTargetId?.let { resolveEntityName(state, it) } ?: "unknown"
        val newTargetName = newTargetId?.let { resolveEntityName(state, it) } ?: "unknown"
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Grip of Chaos"

        val events = if (oldTargetId != newTargetId) {
            listOf(com.wingedsheep.engine.core.TargetReselectedEvent(
                spellOrAbilityName = spellName,
                oldTargetName = oldTargetName,
                newTargetName = newTargetName,
                sourceName = sourceName
            ))
        } else {
            emptyList()
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Find all legal targets for the spell/ability (including the current target).
     * Per ruling: "If there are multiple legal targets, it may still choose the original one."
     */
    private fun findLegalTargets(
        state: GameState,
        currentTarget: ChosenTarget,
        targetRequirements: List<TargetRequirement>,
        controllerId: EntityId
    ): List<EntityId> {
        val projected = state.projectedState
        val requirement = targetRequirements.firstOrNull()

        return when {
            requirement is AnyTarget -> {
                val permanents = state.getBattlefield().filter { entityId ->
                    projected.hasType(entityId, "CREATURE") || projected.hasType(entityId, "PLANESWALKER")
                }
                val players = state.turnOrder.filter { state.hasEntity(it) }
                permanents + players
            }

            requirement is TargetCreatureOrPlayer -> {
                val creatures = state.getBattlefield().filter { entityId ->
                    projected.hasType(entityId, "CREATURE")
                }
                val players = state.turnOrder.filter { state.hasEntity(it) }
                creatures + players
            }

            requirement is TargetOpponentOrPlaneswalker -> {
                val opponents = state.turnOrder.filter { it != controllerId && state.hasEntity(it) }
                val planeswalkers = state.getBattlefield().filter { entityId ->
                    projected.hasType(entityId, "PLANESWALKER")
                }
                opponents + planeswalkers
            }

            requirement is TargetCreatureOrPlaneswalker -> {
                state.getBattlefield().filter { entityId ->
                    projected.hasType(entityId, "CREATURE") || projected.hasType(entityId, "PLANESWALKER")
                }
            }

            requirement is TargetObject && requirement.filter.zone == Zone.BATTLEFIELD -> {
                val predContext = PredicateContext(controllerId = controllerId)
                state.getBattlefield().filter { entityId ->
                    predicateEvaluator.matchesWithProjection(
                        state, projected, entityId, requirement.filter.baseFilter, predContext
                    )
                }
            }

            requirement is TargetPlayer -> {
                state.turnOrder.filter { state.hasEntity(it) }
            }

            requirement is TargetOpponent -> {
                state.turnOrder.filter { it != controllerId && state.hasEntity(it) }
            }

            requirement is TargetSpellOrPermanent -> {
                // Spells on stack + permanents on battlefield
                state.stack + state.getBattlefield()
            }

            // Fallback: infer from current target type
            else -> findTargetsByCurrentType(state, currentTarget, projected)
        }
    }

    private fun findTargetsByCurrentType(
        state: GameState,
        currentTarget: ChosenTarget,
        projected: ProjectedState
    ): List<EntityId> {
        return when (currentTarget) {
            is ChosenTarget.Permanent -> {
                state.getBattlefield()
            }
            is ChosenTarget.Player -> {
                state.turnOrder.filter { state.hasEntity(it) }
            }
            else -> {
                val id = getTargetEntityId(currentTarget)
                if (id != null) listOf(id) else emptyList()
            }
        }
    }

    private fun buildChosenTarget(
        state: GameState,
        chosenId: EntityId,
        currentTarget: ChosenTarget
    ): ChosenTarget? {
        // Check if it's a player
        if (state.turnOrder.contains(chosenId)) {
            return ChosenTarget.Player(chosenId)
        }
        // Check if it's on the battlefield
        if (state.getBattlefield().contains(chosenId)) {
            return ChosenTarget.Permanent(chosenId)
        }
        // Check if it's on the stack
        if (state.stack.contains(chosenId)) {
            return ChosenTarget.Spell(chosenId)
        }
        // Fallback: keep structure of current target
        return when (currentTarget) {
            is ChosenTarget.Permanent -> ChosenTarget.Permanent(chosenId)
            is ChosenTarget.Player -> ChosenTarget.Player(chosenId)
            is ChosenTarget.Spell -> ChosenTarget.Spell(chosenId)
            is ChosenTarget.Card -> ChosenTarget.Card(chosenId, currentTarget.ownerId, currentTarget.zone)
        }
    }

    private fun getTargetEntityId(target: ChosenTarget): EntityId? {
        return when (target) {
            is ChosenTarget.Permanent -> target.entityId
            is ChosenTarget.Player -> target.playerId
            is ChosenTarget.Card -> target.cardId
            is ChosenTarget.Spell -> target.spellEntityId
        }
    }

    private fun resolveEntityName(state: GameState, entityId: EntityId): String {
        val entity = state.getEntity(entityId) ?: return "unknown"
        // Check if it's a card (permanent, spell, etc.)
        entity.get<CardComponent>()?.name?.let { return it }
        // Check if it's a player
        entity.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>()?.let {
            return it.name
        }
        return "unknown"
    }
}
