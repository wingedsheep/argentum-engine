package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ChangeSpellTargetContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect
import com.wingedsheep.sdk.scripting.targets.*
import kotlin.reflect.KClass

/**
 * Executor for ChangeTargetEffect.
 * "Change the target of target spell or ability with a single target."
 *
 * Logic:
 * 1. Get the target spell/ability from context
 * 2. Check it has exactly one target — if not, the effect does nothing
 * 3. Find all legal new targets based on the spell/ability's target requirement
 * 4. Present a selection decision to the controller
 * 5. Push ChangeSpellTargetContinuation (reused)
 */
class ChangeTargetExecutor : EffectExecutor<ChangeTargetEffect> {

    override val effectType: KClass<ChangeTargetEffect> = ChangeTargetEffect::class

    private val decisionHandler = DecisionHandler()
    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: ChangeTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        // 1. Get target spell/ability
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid spell/ability target for ChangeTarget")
        }

        val stackEntity = state.getEntity(targetSpell.spellEntityId)
            ?: return ExecutionResult.error(state, "Spell/ability not found on stack")

        // 2. Get the spell/ability's targets
        val targetsComponent = stackEntity.get<TargetsComponent>()
        val spellTargets = targetsComponent?.targets ?: emptyList()

        // Must have exactly one target
        if (spellTargets.size != 1) {
            return ExecutionResult.success(state)
        }

        val currentTarget = spellTargets.first()
        val targetRequirements = targetsComponent?.targetRequirements ?: emptyList()

        // 3. Find all legal new targets based on target requirements
        val legalNewTargets = findLegalNewTargets(state, currentTarget, targetRequirements, context.controllerId)

        if (legalNewTargets.isEmpty()) {
            // No other legal targets to redirect to
            return ExecutionResult.success(state)
        }

        // 4. Present selection decision to the controller
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose a new target",
            options = legalNewTargets,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        // 5. Push continuation (reuse ChangeSpellTargetContinuation)
        val continuation = ChangeSpellTargetContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            spellEntityId = targetSpell.spellEntityId,
            sourceId = context.sourceId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * Find all legal new targets for the spell/ability, excluding the current target.
     * Uses the spell's target requirements to determine what types of entities are valid.
     */
    private fun findLegalNewTargets(
        state: GameState,
        currentTarget: ChosenTarget,
        targetRequirements: List<TargetRequirement>,
        controllerId: EntityId
    ): List<EntityId> {
        val projected = StateProjector().project(state)
        val currentTargetId = getTargetEntityId(currentTarget)

        // If we have target requirements, use the first one to determine legal targets
        val requirement = targetRequirements.firstOrNull()

        return when {
            // AnyTarget: creatures/planeswalkers on battlefield + players
            requirement is AnyTarget -> {
                val permanents = state.getBattlefield().filter { entityId ->
                    projected.hasType(entityId, "CREATURE") || projected.hasType(entityId, "PLANESWALKER")
                }
                val players = state.turnOrder.filter { state.hasEntity(it) }
                (permanents + players).filter { it != currentTargetId }
            }

            // TargetCreatureOrPlayer: creatures on battlefield + players
            requirement is TargetCreatureOrPlayer -> {
                val creatures = state.getBattlefield().filter { entityId ->
                    projected.hasType(entityId, "CREATURE")
                }
                val players = state.turnOrder.filter { state.hasEntity(it) }
                (creatures + players).filter { it != currentTargetId }
            }

            // TargetCreatureOrPlaneswalker: creatures and planeswalkers on battlefield
            requirement is TargetCreatureOrPlaneswalker -> {
                state.getBattlefield().filter { entityId ->
                    entityId != currentTargetId &&
                        (projected.hasType(entityId, "CREATURE") || projected.hasType(entityId, "PLANESWALKER"))
                }
            }

            // TargetObject with battlefield zone: use filter to match permanents
            requirement is TargetObject && requirement.filter.zone == Zone.BATTLEFIELD -> {
                val predContext = PredicateContext(controllerId = controllerId)
                state.getBattlefield().filter { entityId ->
                    entityId != currentTargetId &&
                        predicateEvaluator.matches(state, entityId, requirement.filter.baseFilter, predContext)
                }
            }

            // Fallback: infer from current target type
            else -> findTargetsByCurrentType(state, currentTarget, projected)
        }
    }

    /**
     * Fallback: find targets of the same type as the current target.
     */
    private fun findTargetsByCurrentType(
        state: GameState,
        currentTarget: ChosenTarget,
        projected: ProjectedState
    ): List<EntityId> {
        val currentTargetId = getTargetEntityId(currentTarget)
        return when (currentTarget) {
            is ChosenTarget.Permanent -> {
                state.getBattlefield().filter { it != currentTargetId }
            }
            is ChosenTarget.Player -> {
                state.turnOrder.filter { it != currentTargetId && state.hasEntity(it) }
            }
            else -> emptyList()
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
}
