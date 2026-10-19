package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChooseSpellCastThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChooseSpellCastThisTurnEffect]: offers the controller the caster's matching cast
 * records this turn, labelled with the damage each dealt, and stores the picked spell's damage.
 *
 * When every matching spell dealt the same damage the pick can't change anything, so that number
 * is stored without a prompt — as is 0 when the caster can't be resolved or cast no matching spell.
 */
class ChooseSpellCastThisTurnExecutor(
    private val predicateEvaluator: PredicateEvaluator,
) : EffectExecutor<ChooseSpellCastThisTurnEffect> {

    override val effectType: KClass<ChooseSpellCastThisTurnEffect> = ChooseSpellCastThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseSpellCastThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val casterId = context.resolvePlayerTarget(effect.caster, state)
        val predicateContext = PredicateContext.fromEffectContext(context)
        val spells = casterId?.let { state.spellsCastThisTurnByPlayer[it] }.orEmpty()
            .filter { predicateEvaluator.matchesFilter(it, effect.spellFilter, predicateContext) }

        val distinctDamage = spells.map { it.damageDealt }.distinct()
        if (distinctDamage.size <= 1) {
            return EffectResult(
                state = state,
                updatedStoredNumbers = mapOf(effect.storeDamageDealtAs to (distinctDamage.firstOrNull() ?: 0))
            )
        }

        val options = spells.map { "${it.name ?: "Spell"} — ${it.damageDealt} damage" }
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val casterName = casterId?.let { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>()?.name }
        val decision = { decisionId: String ->
            ChooseOptionDecision(
                id = decisionId,
                playerId = context.controllerId,
                prompt = "Choose one of the ${effect.spellFilter.description} spells ${casterName ?: "that player"} cast this turn",
                context = DecisionContext(
                    sourceId = context.sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                options = options
            )
        }
        val continuation = ChooseSpellCastThisTurnContinuation(
            playerId = context.controllerId,
            storeAs = effect.storeDamageDealtAs,
            damageByOption = spells.map { it.damageDealt },
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
        )
        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }
}
