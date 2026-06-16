package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellOrAbilityEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetTriggeredAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for [CopyTargetSpellOrAbilityEffect] — "copy target instant/sorcery spell, activated
 * ability, or triggered ability. You may choose new targets for the copy." (Return the Favor).
 *
 * The single target requirement can resolve to any of three stack-object kinds, so this executor
 * dispatches at resolution on the chosen entity's stack component (CR 707.10 — a copy is created on
 * the stack, controlled by the copier, inheriting cast-time choices; CR 707.10c lets the copier
 * pick new targets):
 *
 *  - **Spell** (instant/sorcery, has no ability component) → delegate to [CopyTargetSpellExecutor],
 *    which handles the modal / targeted / no-target spell-copy paths and the new-targets prompt.
 *  - **Triggered ability** → delegate to [CopyTargetTriggeredAbilityExecutor].
 *  - **Activated ability** → clone its [ActivatedAbilityOnStackComponent] here (the triggered
 *    executor only handles triggered components), re-prompting for new targets when it has any.
 *
 * Delegation reuses the existing copy machinery verbatim rather than duplicating it, so all three
 * kinds share one code path per kind.
 */
class CopyTargetSpellOrAbilityExecutor(
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<CopyTargetSpellOrAbilityEffect> {

    override val effectType: KClass<CopyTargetSpellOrAbilityEffect> =
        CopyTargetSpellOrAbilityEffect::class

    private val spellExecutor = CopyTargetSpellExecutor(cardRegistry, targetFinder)
    private val triggeredExecutor = CopyTargetTriggeredAbilityExecutor(cardRegistry, targetFinder)

    override fun execute(
        state: GameState,
        effect: CopyTargetSpellOrAbilityEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No target spell or ability to copy")

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target spell or ability entity not found on stack")

        return when {
            container.has<TriggeredAbilityOnStackComponent>() ->
                triggeredExecutor.execute(state, CopyTargetTriggeredAbilityEffect(effect.target), context)

            container.has<ActivatedAbilityOnStackComponent>() ->
                copyActivatedAbility(state, targetId, context)

            // Otherwise it's a spell on the stack (instant/sorcery). The spell executor handles
            // modal / targeted / no-target paths and the choose-new-targets prompt.
            else -> spellExecutor.execute(state, CopyTargetSpellEffect(effect.target), context)
        }
    }

    /**
     * Copy an activated ability on the stack. The copy inherits every cast-time value (X, sacrificed
     * permanents, tapped permanents) per CR 707.10 and is controlled by the copier. If the source
     * ability has targets, the copier may choose new ones (CR 707.10c).
     */
    private fun copyActivatedAbility(
        state: GameState,
        abilityEntityId: EntityId,
        context: EffectContext
    ): EffectResult {
        val container = state.getEntity(abilityEntityId)!!
        val source = container.get<ActivatedAbilityOnStackComponent>()!!
        val targetRequirements = container.get<TargetsComponent>()?.targetRequirements ?: emptyList()

        if (targetRequirements.isEmpty()) {
            val copy = source.copy(controllerId = context.controllerId)
            val stackResolver = StackResolver(cardRegistry = cardRegistry)
            // CR 707.10: a copy isn't activated, so don't emit an AbilityActivatedEvent (it would
            // re-fire "whenever you activate an ability" triggers off the copy).
            return EffectResult.from(
                stackResolver.putActivatedAbility(state, copy, emitActivationEvent = false)
            )
        }

        // The source has targets — prompt the copier to choose new ones.
        val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
        for ((index, requirement) in targetRequirements.withIndex()) {
            legalTargetsMap[index] = targetFinder.findLegalTargets(
                state, requirement, context.controllerId, context.sourceId
            )
        }
        // If any requirement has no legal targets, the copy can't be legally placed — skip it.
        if (legalTargetsMap.any { (_, targets) -> targets.isEmpty() }) {
            return EffectResult.success(state)
        }

        val decisionId = "copy-activated-ability-target-${System.nanoTime()}"
        val continuation = CopyActivatedAbilityTargetContinuation(
            decisionId = decisionId,
            abilityEntityId = abilityEntityId,
            controllerId = context.controllerId,
            targetRequirements = targetRequirements
        )
        val targetReqInfos = targetRequirements.mapIndexed { index, req ->
            TargetRequirementInfo(index = index, description = req.description)
        }
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = "Choose new targets for copy of ${source.sourceName}'s ability",
            context = DecisionContext(
                phase = DecisionPhase.CASTING,
                sourceName = source.sourceName,
                effectHint = "Copy of activated ability"
            ),
            targetRequirements = targetReqInfos,
            legalTargets = legalTargetsMap
        )
        val stateWithContinuation = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)
        return EffectResult.paused(stateWithContinuation, decision)
    }
}
