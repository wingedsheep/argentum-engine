package com.wingedsheep.engine.handlers.effects.permanent.abilities

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.KeywordGrantedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import kotlin.reflect.KClass

/**
 * Executor for GrantKeywordEffect.
 * "Target creature gains [keyword] until end of turn"
 *
 * Works for any battlefield permanent, not just creatures: combat keywords (flying, trample,
 * …) are no-ops on noncreatures, but ability flags such as `DOESNT_UNTAP` are meaningful on a
 * noncreature artifact (Phyrexian Gremlins taps a target artifact and keeps it from untapping).
 * The granted keyword lands in the projected keyword set either way; the layer that consumes it
 * decides whether it does anything. So the executor only requires that the target be a permanent
 * on the battlefield, not specifically a creature.
 *
 * The target may also be a **permanent spell on the stack** (e.g. "when you next cast a creature
 * spell this turn, it gains haste until end of turn" — Summon: Brynhildr's Gestalt Mode). A
 * permanent spell keeps its entity id as it resolves onto the battlefield (see
 * [com.wingedsheep.engine.mechanics.stack.StackResolver.resolvePermanentSpell]), and the projector
 * keeps floating effects whose affected entity is on the battlefield *or* the stack, so a keyword
 * granted while the object is on the stack applies once it becomes a permanent. On a non-permanent
 * spell the floating effect simply never has anything to apply to.
 */
class GrantKeywordExecutor : EffectExecutor<GrantKeywordEffect> {

    override val effectType: KClass<GrantKeywordEffect> = GrantKeywordEffect::class

    override fun execute(
        state: GameState,
        effect: GrantKeywordEffect,
        context: EffectContext
    ): EffectResult {
        // Resolve the target permanent
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for keyword grant")

        // Verify target still exists as a battlefield permanent
        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target permanent no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card")
        // Battlefield permanents get the keyword immediately. A permanent spell still on the stack
        // is also accepted: it keeps its entity id when it resolves, so the floating effect keyed to
        // that id starts applying the moment it becomes a permanent (used by "the next creature spell
        // you cast this turn gains …" delayed triggers).
        if (targetId !in state.getBattlefield() && targetId !in state.stack) {
            return EffectResult.error(state, "Target is no longer on the battlefield or stack")
        }

        // Create a floating effect for the keyword grant. An optional [GrantKeywordEffect.condition]
        // rides along as the floating effect's source condition, so the keyword is live only while
        // the condition holds — the durational form of a printed "as long as …" clause, and how a
        // quoted conditional ability handed out by an animate effect is modelled (Restless Spire).
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(effect.keyword),
            affectedEntities = setOf(targetId),
            duration = effect.duration,
            context = context,
            sourceCondition = effect.condition
        )

        // Emit event for visualization
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            KeywordGrantedEvent(
                targetId = targetId,
                targetName = cardComponent.name,
                keyword = effect.keyword.lowercase().replace('_', ' '),
                sourceName = sourceName
            )
        )

        return EffectResult.success(newState, events)
    }
}
