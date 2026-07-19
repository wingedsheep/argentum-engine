package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReturnSelfFromExileTransformedEffect] (CR 702.167a).
 *
 * The source has been exiled by the paired [com.wingedsheep.sdk.scripting.AbilityCost.Craft]
 * cost. This executor:
 *  1. Flips the source's [DoubleFacedComponent] to its back face and rewrites
 *     [CardComponent] to the back face's printed characteristics.
 *  2. Re-attaches the [CraftedFromExiledComponent] recorded on the source during cost
 *     payment — preserved across the exile sojourn — so it survives
 *     [com.wingedsheep.engine.handlers.effects.ZoneTransitionService.applyBattlefieldEntry]'s
 *     general battlefield-entry strip (Rule 400.7).
 *  3. Moves the source from EXILE → BATTLEFIELD under its owner's control via the standard
 *     zone-transition pipeline (ETB triggers, static-ability registration, etc.).
 *
 * Does **not** emit a `TransformedEvent` — CR 701.27a defines transforming as turning over
 * a permanent that is already on the battlefield. Craft's "return ... transformed" produces
 * a new battlefield object with its back face up; no transform action occurs, so triggers
 * keyed on `Triggers.TransformsToBack` / `TransformsToFront` must not fire. ETB triggers on
 * the back face are dispatched normally by the `ZoneChangeEvent` emitted from the move.
 */
class ReturnSelfFromExileTransformedExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<ReturnSelfFromExileTransformedEffect> {

    override val effectType: KClass<ReturnSelfFromExileTransformedEffect> =
        ReturnSelfFromExileTransformedEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnSelfFromExileTransformedEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
            ?: return EffectResult.error(state, "Craft effect has no source")
        val container = state.getEntity(sourceId)
            ?: return EffectResult.error(state, "Craft source not found")
        container.get<DoubleFacedComponent>()
            ?: return EffectResult.error(state, "Craft source is not a double-faced permanent")

        val materials = container.get<CraftedFromExiledComponent>()

        // Flip to the back face (CR 702.167a always returns transformed) and move EXILE →
        // BATTLEFIELD under owner's control. The shared helper handles the face swap + zone move
        // (it is also used by the FIN Dominant exile-and-return-transformed effect).
        val transition = returnDfcFace(
            state, cardRegistry, sourceId, DoubleFacedComponent.Face.BACK
        )
        var newState = transition.state

        // Re-attach CraftedFromExiledComponent: ZoneTransitionService.applyBattlefieldEntry
        // strips it as part of the Rule 400.7 "new object" cleanup (alongside
        // LinkedExileComponent), so this is the deliberate re-establishment of the materials
        // link on the craft-return entry path.
        if (materials != null && newState.getEntity(sourceId) != null) {
            newState = newState.updateEntity(sourceId) { c -> c.with(materials) }
        }

        return EffectResult.success(newState, transition.events)
    }
}
