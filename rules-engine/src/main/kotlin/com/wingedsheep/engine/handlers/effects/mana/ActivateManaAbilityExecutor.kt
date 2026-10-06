package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ActivateManaAbilityChoiceContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ActivateManaAbilityEffect
import kotlin.reflect.KClass

/**
 * "Its controller activates a mana ability of [ActivateManaAbilityEffect.permanent]" — an instructed
 * activation during resolution, resolving at once like any mana ability (CR 605.3b). The candidates are exactly the mana abilities the
 * controller could activate right now ([LegalActionEnumerator.enumerateManaAbilities], affordable
 * only); the activation itself runs through [ActivateAbilityHandler], so costs, colour and amount
 * choices, mana triggers and replacement effects behave as for any other activation.
 */
class ActivateManaAbilityExecutor(
    private val enumerator: () -> LegalActionEnumerator,
    private val handler: () -> ActivateAbilityHandler,
) : EffectExecutor<ActivateManaAbilityEffect> {
    override val effectType: KClass<ActivateManaAbilityEffect> = ActivateManaAbilityEffect::class

    override fun execute(state: GameState, effect: ActivateManaAbilityEffect, context: EffectContext): EffectResult {
        val sourceId = context.resolveTarget(effect.permanent, state)
            ?.takeIf { it in state.getBattlefield() } ?: return EffectResult.success(state)
        val playerId = state.projectedState.getController(sourceId) ?: return EffectResult.success(state)
        val options = ForcedManaActivation.options(state, enumerator(), playerId, sourceId)
        if (options.isEmpty()) return EffectResult.success(state)
        if (options.size == 1) return EffectResult.from(ForcedManaActivation.activate(state, handler(), options.single().second))

        val source = state.objectRef(sourceId) ?: return EffectResult.success(state)
        val name = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "this permanent"
        return EffectResult.from(state.suspendForDecision(
            question = { decisionId -> ChooseOptionDecision(
                id = decisionId, playerId = playerId,
                prompt = "Choose a mana ability of $name to activate",
                context = DecisionContext(sourceId = context.sourceId, phase = DecisionPhase.RESOLUTION),
                options = options.map { it.first },
                optionCardIds = options.indices.associateWith { listOf(sourceId) },
            ) },
            answer = ActivateManaAbilityChoiceContinuation(playerId, source, options.map { it.second }),
        ))
    }
}

/** The shared halves of an instructed mana activation: what may be activated, and activating it. */
internal object ForcedManaActivation {
    /** Each activatable mana ability of [sourceId] for [playerId], labelled for a choice prompt. */
    fun options(
        state: GameState, enumerator: LegalActionEnumerator, playerId: EntityId, sourceId: EntityId,
    ): List<Pair<String, ActivateAbility>> =
        enumerator.enumerateManaAbilities(state, playerId)
            .filter { it.affordable }
            .mapNotNull { legal -> (legal.action as? ActivateAbility)?.takeIf { it.sourceId == sourceId }?.let { legal.description to it } }
            .distinctBy { it.second.abilityId }

    /**
     * Activates [action] mid-resolution. An activation the handler refuses is one the player was
     * unable to make, so nothing happens. Priority stays where the resolving object left it: the
     * mana-ability path hands priority to the activator when its cost fired a trigger, which is
     * right for an activation at priority and wrong inside a resolution.
     */
    fun activate(state: GameState, handler: ActivateAbilityHandler, action: ActivateAbility): ExecutionResult {
        val result = handler.execute(state, action)
        if (result.error != null) return ExecutionResult.success(state)
        if (result.outcome is Outcome.Paused) return result
        val restored = result.state.copy(priorityPlayerId = state.priorityPlayerId, priorityPassedBy = state.priorityPassedBy)
        return ExecutionResult.success(restored, result.events)
    }
}
