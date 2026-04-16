package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import kotlin.reflect.KClass

/**
 * Executor for CopyTargetSpellEffect.
 * Copies a targeted spell on the stack, allowing the controller to choose new targets.
 *
 * Reads the targeted spell's effect and target requirements from its components on the stack,
 * then creates a copy. If the original spell has targets, prompts for new target selection
 * (reusing StormCopyTargetContinuation with remainingCopies=1).
 */
class CopyTargetSpellExecutor(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<CopyTargetSpellEffect> {

    override val effectType: KClass<CopyTargetSpellEffect> = CopyTargetSpellEffect::class

    override fun execute(
        state: GameState,
        effect: CopyTargetSpellEffect,
        context: EffectContext
    ): EffectResult {
        val spellEntityId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No target spell to copy")

        val container = state.getEntity(spellEntityId)
            ?: return EffectResult.error(state, "Target spell entity not found on stack")

        val cardComponent = container.get<CardComponent>()
        val spellEffect = cardComponent?.spellEffect
            ?: return EffectResult.error(state, "Target spell has no effect to copy")

        val spellName = cardComponent.name
        val targetsComponent = container.get<TargetsComponent>()
        val targetRequirements = targetsComponent?.targetRequirements ?: emptyList()

        val stackResolver = StackResolver(cardRegistry = cardRegistry)

        // Propagate modal info from the source spell (700.2g — copies keep the
        // same chosen modes). Targets inherit by default; a future enhancement
        // may let the copy controller re-choose per-mode targets.
        val sourceSpell = container.get<SpellOnStackComponent>()
        val inheritedChosenModes = sourceSpell?.chosenModes ?: emptyList()
        val inheritedModeTargets = sourceSpell?.modeTargetsOrdered ?: emptyList()
        val inheritedModeTargetRequirements = sourceSpell?.modeTargetRequirements ?: emptyMap()

        // If the source was modal (per-mode targets), skip flat re-targeting and
        // inherit modes/targets directly. Full per-mode retargeting is out of
        // scope for this phase.
        if (inheritedChosenModes.isNotEmpty()) {
            val copyAbility = TriggeredAbilityOnStackComponent(
                sourceId = context.sourceId ?: EntityId.generate(),
                sourceName = spellName,
                controllerId = context.controllerId,
                effect = spellEffect,
                description = "Copy of $spellName",
                chosenModes = inheritedChosenModes,
                modeTargetsOrdered = inheritedModeTargets,
                modeTargetRequirements = inheritedModeTargetRequirements
            )
            return EffectResult.from(stackResolver.putTriggeredAbility(state, copyAbility))
        }

        // If the original spell has no targets, create the copy immediately
        if (targetRequirements.isEmpty()) {
            val copyAbility = TriggeredAbilityOnStackComponent(
                sourceId = context.sourceId ?: EntityId.generate(),
                sourceName = spellName,
                controllerId = context.controllerId,
                effect = spellEffect,
                description = "Copy of $spellName"
            )
            return EffectResult.from(stackResolver.putTriggeredAbility(state, copyAbility))
        }

        // Spell has targets — prompt for new target selection
        return promptForCopyTargets(state, context, spellEntityId, spellEffect, targetRequirements, spellName)
    }

    private fun promptForCopyTargets(
        state: GameState,
        context: EffectContext,
        spellEntityId: EntityId,
        spellEffect: com.wingedsheep.sdk.scripting.effects.Effect,
        targetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        spellName: String
    ): EffectResult {
        val decisionId = "copy-spell-target-${System.nanoTime()}"

        val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
        for ((index, requirement) in targetRequirements.withIndex()) {
            val legalTargets = targetFinder.findLegalTargets(
                state, requirement, context.controllerId, context.sourceId
            )
            legalTargetsMap[index] = legalTargets
        }

        // If no legal targets for any requirement, skip copy
        val hasNoLegalTargets = legalTargetsMap.any { (_, targets) -> targets.isEmpty() }
        if (hasNoLegalTargets) {
            return EffectResult.success(state)
        }

        // Reuse StormCopyTargetContinuation with 1 copy. The resumer clones the
        // SpellOnStackComponent off this sourceId via putSpellCopy (Phase 1 of
        // spell-copies-as-spells), so it must point at the targeted spell on the
        // stack — not the trigger source (e.g., Mischievous Quanar / Naru Meha are
        // creatures with no SpellOnStackComponent).
        val continuation = StormCopyTargetContinuation(
            decisionId = decisionId,
            remainingCopies = 1,
            spellEffect = spellEffect,
            spellTargetRequirements = targetRequirements,
            spellName = spellName,
            controllerId = context.controllerId,
            sourceId = spellEntityId
        )
        val targetReqInfos = targetRequirements.mapIndexed { index, req ->
            TargetRequirementInfo(
                index = index,
                description = req.description
            )
        }

        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose targets for copy of $spellName",
            context = DecisionContext(
                phase = DecisionPhase.CASTING,
                sourceName = spellName
            ),
            targetRequirements = targetReqInfos,
            legalTargets = legalTargetsMap
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(stateWithContinuation, decision)
    }
}
