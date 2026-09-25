package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.FlippedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FlippedComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FlipEffect
import kotlin.reflect.KClass

/**
 * Executor for [FlipEffect] (CR 710).
 *
 * Gives a flip-card permanent the flipped status: its [CardComponent] becomes the flip half's
 * name, type line, rules text, P/T and abilities, while mana cost and colours stay the card's own
 * (CR 710.1c). The entity id is stable — counters, damage, attachments, controller and timestamp
 * persist — and the flip half's static/replacement effects are registered in place of the upright
 * half's, exactly as a transform re-registers a new face's.
 *
 * Silently does nothing when the target isn't a flip card, isn't on the battlefield (CR 710.1b —
 * only a permanent flips), or is already flipped (CR 710.4 — flipping is one-way).
 */
class FlipEffectExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<FlipEffect> {

    override val effectType: KClass<FlipEffect> = FlipEffect::class

    override fun execute(
        state: GameState,
        effect: FlipEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target) ?: return EffectResult.success(state)
        val (newState, event) = flipPermanent(state, cardRegistry, targetId)
            ?: return EffectResult.success(state)
        return EffectResult.success(newState, listOf(event))
    }
}

/**
 * Flip [entityId] in place, returning the new state and the [FlippedEvent], or null when there is
 * nothing to flip (not a flip card, not on the battlefield, already flipped).
 */
internal fun flipPermanent(
    state: GameState,
    cardRegistry: CardRegistry,
    entityId: EntityId
): Pair<GameState, FlippedEvent>? {
    if (entityId !in state.getBattlefield()) return null
    val container = state.getEntity(entityId) ?: return null
    if (container.get<FlippedComponent>() != null) return null
    val upright = container.get<CardComponent>() ?: return null
    val flipDef = cardRegistry.getCard(upright.cardDefinitionId)?.flipSide ?: return null
    val controllerId = container.get<ControllerComponent>()?.playerId ?: upright.ownerId ?: return null

    // CR 710.1c: the flip half replaces name, type line, text box and P/T — never mana cost or colour.
    val flipped = upright.copy(
        cardDefinitionId = flipDef.name,
        name = flipDef.name,
        typeLine = flipDef.typeLine,
        oracleText = flipDef.oracleText,
        baseStats = flipDef.creatureStats,
        baseKeywords = flipDef.keywords,
        baseFlags = flipDef.flags,
        spellEffect = flipDef.spellEffect,
        hasNonManaActivatedAbility = flipDef.hasNonManaActivatedAbility,
        hasActivatedAbility = flipDef.hasActivatedAbility,
    )

    val staticAbilityHandler = StaticAbilityHandler(cardRegistry)
    val newState = state.updateEntity(entityId) { c ->
        var updated = c
            .with(flipped)
            .with(FlippedComponent(unflippedCard = upright))
            .without<ContinuousEffectSourceComponent>()
            .without<ReplacementEffectSourceComponent>()
        updated = staticAbilityHandler.addContinuousEffectComponent(updated, flipDef)
        updated = staticAbilityHandler.addReplacementEffectComponent(updated, flipDef)
        withDfcFaceSelfRedirects(updated, flipDef)
    }
    return newState to FlippedEvent(entityId = entityId, newName = flipDef.name, controllerId = controllerId)
}
