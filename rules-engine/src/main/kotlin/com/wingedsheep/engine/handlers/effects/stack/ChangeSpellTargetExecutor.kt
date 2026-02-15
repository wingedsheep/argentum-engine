package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ChangeSpellTargetContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.sdk.scripting.ChangeSpellTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for ChangeSpellTargetEffect.
 * "If target spell has only one target and that target is a creature,
 * change that spell's target to another creature."
 *
 * Logic:
 * 1. Get the target spell from context
 * 2. Check the spell has exactly one target and that target is a creature
 * 3. If not, the effect does nothing (the "if" condition fails)
 * 4. Find all other creatures on the battlefield as legal new targets
 * 5. Present a selection decision to Meddle's controller
 * 6. Push ChangeSpellTargetContinuation
 */
class ChangeSpellTargetExecutor : EffectExecutor<ChangeSpellTargetEffect> {

    override val effectType: KClass<ChangeSpellTargetEffect> = ChangeSpellTargetEffect::class

    private val decisionHandler = DecisionHandler()

    override fun execute(
        state: GameState,
        effect: ChangeSpellTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        // 1. Get target spell
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid spell target for ChangeSpellTarget")
        }

        val spellEntity = state.getEntity(targetSpell.spellEntityId)
            ?: return ExecutionResult.error(state, "Spell not found on stack")

        // 2. Get the spell's targets
        val targetsComponent = spellEntity.get<TargetsComponent>()
        val spellTargets = targetsComponent?.targets ?: emptyList()

        // 3. Validate: spell must have exactly one target
        if (spellTargets.size != 1) {
            return ExecutionResult.success(state)
        }

        // 4. Validate: that target must be a creature on the battlefield
        val singleTarget = spellTargets.first()
        if (singleTarget !is ChosenTarget.Permanent) {
            return ExecutionResult.success(state)
        }

        val projected = StateProjector().project(state)

        // Check the target is a creature using projected types
        if (!projected.hasType(singleTarget.entityId, "CREATURE")) {
            return ExecutionResult.success(state)
        }

        // 5. Find all other creatures on the battlefield as legal new targets
        val currentTargetId = singleTarget.entityId
        val otherCreatures = state.getBattlefield()
            .filter { entityId ->
                entityId != currentTargetId && projected.hasType(entityId, "CREATURE")
            }

        if (otherCreatures.isEmpty()) {
            // No other creatures to redirect to
            return ExecutionResult.success(state)
        }

        // 6. Present selection decision to Meddle's controller
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = "Meddle",
            prompt = "Choose a new creature target",
            options = otherCreatures,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        )

        // 7. Push continuation
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
}
