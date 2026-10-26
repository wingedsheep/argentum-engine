package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.sdk.scripting.effects.ReturnSelfFromZoneTransformedEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReturnSelfFromZoneTransformedEffect] — "Return this card from your graveyard
 * to the battlefield transformed" (Garland, Knight of Cornelia).
 *
 * Guards, in order:
 *  1. The source must still be in the effect's [ReturnSelfFromZoneTransformedEffect.fromZone]
 *     when the ability resolves (it may have been exiled or reanimated in response) — else no-op.
 *  2. The source must be a double-faced card. A single-faced card instructed to enter the
 *     battlefield transformed doesn't move at all (official FIN ruling), so this is a no-op,
 *     not an error.
 *
 * The face flip + zone move is the shared [returnDfcFace] helper (it flips-then-moves from
 * whatever zone the entity is currently in), always to the back face: a card in a
 * non-battlefield zone is front-face-up by definition, so "transformed" can only mean the
 * back face. It enters under its owner's control unless
 * [ReturnSelfFromZoneTransformedEffect.controllerOverride] names another player.
 */
class ReturnSelfFromZoneTransformedExecutor(
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry
) : EffectExecutor<ReturnSelfFromZoneTransformedEffect> {

    override val effectType: KClass<ReturnSelfFromZoneTransformedEffect> =
        ReturnSelfFromZoneTransformedEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnSelfFromZoneTransformedEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "ReturnSelfFromZoneTransformed has no source")
        state.getEntity(sourceId)
            ?: return EffectResult.success(state)

        // Fizzle quietly if the card already left the expected zone.
        val currentZone = state.zones.entries.firstOrNull { sourceId in it.value }?.key
            ?: return EffectResult.success(state)
        if (currentZone.zoneType != effect.fromZone) {
            return EffectResult.success(state)
        }

        // A non-DFC instructed to enter transformed stays where it is. DFC-ness comes from the
        // card definition, not the component: DoubleFacedComponent is only stamped when a DFC
        // spell resolves onto the battlefield, so a card that was discarded or milled straight
        // into the graveyard doesn't carry one yet.
        val workingState = ensureDoubleFacedComponent(state, cardRegistry, sourceId)
            ?: return EffectResult.success(state)

        // "...under your control" (Journey to Eternity): the ability's controller, not the owner.
        val controllerId = effect.controllerOverride?.let { context.resolveTarget(it, state) }

        val transition = returnDfcFace(
            zones,
            workingState, cardRegistry, sourceId, DoubleFacedComponent.Face.BACK,
            tapped = effect.tapped, controllerId = controllerId
        )
        return EffectResult.success(transition.state, transition.events)
    }
}
